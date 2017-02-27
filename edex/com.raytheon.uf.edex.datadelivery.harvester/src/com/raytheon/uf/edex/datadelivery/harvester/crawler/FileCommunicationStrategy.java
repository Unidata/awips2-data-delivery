/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.edex.datadelivery.harvester.crawler;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;

import com.google.common.annotations.VisibleForTesting;
import com.raytheon.uf.common.datadelivery.harvester.CrawlAgent;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfigurationManager;
import com.raytheon.uf.common.datadelivery.harvester.ProtoCollection;
import com.raytheon.uf.common.datadelivery.registry.Collection;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.common.util.file.FilenameFilters;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.Link;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.LinkStore;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.ProviderCollectionLinkStore;

/**
 * A {@link File}-based communication strategy.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 17, 2012  740      djohnson  Initial creation
 * Aug 06, 2012  1022     djohnson  Add shutdown(), write out millis with
 *                                  filename to prevent overwriting.
 * Sep 10, 2012  1154     djohnson  Use JAXB instead of thrift, allowing
 *                                  introspection of links, return files in
 *                                  ascending order.
 * Mar 14, 2013  1794     djohnson  Consolidate common FilenameFilter
 *                                  implementations.
 * Oct 28, 2013  2361     dhladky   Fixed up JAXBManager.
 * Sep 10, 2014  3581     ccody     Remove references to SerializationUtil for
 *                                  JAXB operations.
 * Dec 14, 2016  5988     tjensen   Clean up error handling for crawler
 * Feb 27, 2017  5988     tjensen   Fix issue with file save for new config
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
class FileCommunicationStrategy implements CommunicationStrategy {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(FileCommunicationStrategy.class);

    private static final String XML_EXTENSION = ".xml";

    private static final Pattern DASH_PATTERN = Pattern.compile("-");

    /** Path to crawler links directory. */
    private static final String LINKS_DIR = StringUtil.join(
            new String[] { "datadelivery", "harvester", "links" },
            File.separatorChar);

    protected static final String CONFIG_FILE_PREFIX = StringUtil.join(
            new String[] { "datadelivery", "harvester" }, File.separatorChar);

    protected static final String CONFIG_FILE_SUFFIX = "harvester.xml";

    /**
     * The directory where links files are stored for the harvester to pick up.
     */
    private final File linksDir;

    /** JAXBManager Class for marshaling and unmarshaling objects */
    private JAXBManager jaxbManager = null;

    /**
     * Constructor.
     */
    FileCommunicationStrategy() {
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext lc = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.CONFIGURED);
        LocalizationFile lf = pm.getLocalizationFile(lc, LINKS_DIR);
        linksDir = lf.getFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // TODO: Use cluster lock if data delivery every clustered, currently single
    // threaded to avoid file contention
    public synchronized ProviderCollectionLinkStore getNextLinkStore() {
        removeOldLinks();
        File[] files = readLinksDirectory();

        if (!CollectionUtil.isNullOrEmpty(files)) {

            statusHandler.info(files.length + " MetaData files found.");

            for (File file : files) {
                String fileName = file.getName();

                try {
                    String[] pieces = DASH_PATTERN.split(fileName);

                    if (pieces != null && pieces.length > 1) {

                        String providerName = pieces[0];
                        String collectionName = null;
                        if (pieces.length < 3) {
                            collectionName = pieces[1];
                        } else {
                            StringBuffer buf = new StringBuffer();
                            for (int i = 1; i < pieces.length - 1; i++) {
                                buf.append(pieces[i]);
                                if (i < pieces.length - 2) {
                                    buf.append("-");
                                }
                            }
                            collectionName = buf.toString();
                        }

                        LinkStore store = readLinks(file);
                        return new ProviderCollectionLinkStore(providerName,
                                collectionName, store);
                    }
                } finally {
                    // delete link file
                    statusHandler.info("Link File deleted " + file.getName()
                            + " Success? " + file.delete());
                }
            }
        }

        return null;
    }

    /**
     * Purge any links that have been in the directory for more than a set time
     * period. These are likely OBE at this point and we shouldn't waste time
     * trying to process them.
     */
    private void removeOldLinks() {
        if (linksDir.isDirectory()) {
            // TODO: Should this be configurable?
            int daysToKeepLinks = 7;
            File[] obeFiles = FileFilterUtils.filter(new AgeFileFilter(
                    Date.from(LocalDate.now().minusDays(daysToKeepLinks)
                            .atStartOfDay(ZoneId.of("GMT")).toInstant())),

                    linksDir);
            for (File obe : obeFiles) {
                statusHandler.info("Link File '" + obe.getName()
                        + "' is older than " + daysToKeepLinks
                        + " day old and will be deleted. Success? "
                        + obe.delete());
            }
        }
    }

    private void marshalAndWriteToFile(File destinationFile, LinkStore links)
            throws IOException, JAXBException, SerializationException {
        getJaxbManager().marshalToXmlFile(links,
                destinationFile.getAbsolutePath());
    }

    @Override
    public void processCollections(HarvesterConfig hconfig,
            Map<String, ProtoCollection> collections, Provider provider,
            CrawlAgent agent) {

        ArrayList<Collection> newCollections = new ArrayList<>();
        // start processing the proto collections into real collections
        for (Entry<String, ProtoCollection> entry : collections.entrySet()) {
            try {
                ProtoCollection pc = entry.getValue();
                // make first date, find greatest depth dates
                Collection coll = new Collection(pc.getCollectionName(),
                        pc.getSeedUrl(), pc.getDateFormatString());
                coll.setLastDate(pc.getLastDateFormatted());
                coll.setFirstDate(pc.getFirstDateFormatted());
                // TODO: figure a default data type and projection
                // strategy other than just the first one.
                coll.setDataType(
                        provider.getProviderType().get(0).getDataType());
                coll.setProjection(provider.getProjection().get(0).getType());
                coll.setPeriodicity(pc.getPeriodicity());
                coll.setUrlKey(pc.getUrlKey());
                // if we ingestNew this will be true
                if (agent.isIngestNew()) {
                    coll.setIgnore(false);
                }
                newCollections.add(coll);
                statusHandler.info(
                        "adding new Collection: " + pc.getCollectionName());
                // announce
            } catch (Exception e) {
                statusHandler.error(
                        "Error parsing proto-collections " + e.getMessage());
            }
        }

        statusHandler.info("Processing Complete: " + collections.size()
                + " collections found...");

        Date currentDate = new Date(TimeUtil.currentTimeMillis());
        // walk dates forward for all collections if necessary
        if (agent.getCollection() != null) {
            for (Collection collection : agent.getCollection()) {
                collection.updateTime(currentDate);
            }
        }

        if (!newCollections.isEmpty()) {

            if (agent.getCollection() != null) {
                ArrayList<Collection> replace = new ArrayList<>();
                // compare and process against existing collections
                for (Collection newCollection : newCollections) {
                    for (Collection collection : agent.getCollection()) {
                        if (newCollection.getName()
                                .equals(collection.getName())) {
                            // preserve parameter lookups
                            if (collection.getParameterLookup() != null) {
                                newCollection.setParameterLookup(
                                        collection.getParameterLookup());
                            }
                            // preserve level lookups
                            if (collection.getLevelLookup() != null) {
                                newCollection.setLevelLookup(
                                        collection.getLevelLookup());
                            }
                            // other ancillary things
                            newCollection.setIgnore(collection.isIgnore());
                            newCollection
                                    .setProjection(collection.getProjection());
                            newCollection.setDataType(collection.getDataType());
                            replace.add(collection);
                        }
                    }
                }
                // remove all of the replaced collections
                agent.getCollection().removeAll(replace);
                // add all new collections + replacements
                agent.getCollection().addAll(newCollections);
            } else {
                agent.setCollection(newCollections);
            }
        }

        // save the updated file
        saveNewConfig(hconfig, provider.getName());

    }

    private File[] readFilesInDir(final File dir, final String fileExtension) {
        if (dir.isDirectory()) {
            File[] files = dir
                    .listFiles(FilenameFilters.byFileExtension(fileExtension));

            // order the files in time order
            Arrays.sort(files, new Comparator<File>() {

                @Override
                public int compare(File o1, File o2) {
                    // TODO Auto-generated method stub
                    return (int) (o1.lastModified() - o2.lastModified());
                }

            });
            return files;
        }

        return null;
    }

    /**
     * Read the links from the specified file.
     * 
     * @param file
     *            the file to read
     * @return the {@link LinkStore}
     */
    private LinkStore readLinks(File file) {

        LinkStore links = null;

        try {
            links = (LinkStore) getJaxbManager()
                    .unmarshalFromXmlFile(LinkStore.class, file);
            statusHandler.info("Read linkStore for " + file);
        } catch (JAXBException | SerializationException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error unmarshalling file " + file.getAbsolutePath(), e);
        }

        return links;
    }

    /**
     * Find all files
     * 
     * @return
     */
    private File[] readLinksDirectory() {
        return readFilesInDir(linksDir, XML_EXTENSION);
    }

    @VisibleForTesting
    private void saveNewConfig(HarvesterConfig hconfig, String provider) {

        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext lc = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.SITE);
        String fileName = CONFIG_FILE_PREFIX + File.separatorChar + provider
                + "-" + CONFIG_FILE_SUFFIX;
        LocalizationFile lf = pm.getLocalizationFile(lc, fileName);
        File file = lf.getFile();

        try {
            HarvesterConfigurationManager.setHarvesterFile(hconfig, file);
            lf.save();
        } catch (Exception e) {
            statusHandler.error(
                    "Unable to recreate the " + provider
                            + "-harvester.xml configuration! Save of new collections failed",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendLinkStore(
            ProviderCollectionLinkStore providerCollectionLinkStore) {
        final String providerName = providerCollectionLinkStore
                .getProviderName();
        final String collectionName = providerCollectionLinkStore
                .getCollectionName();
        final LinkStore links = providerCollectionLinkStore.getLinkStore();

        try {
            if (!links.getLinks().isEmpty()) {
                File destinationFile = new File(linksDir,
                        providerName + CrawlMetaDataHandler.DASH
                                + collectionName + CrawlMetaDataHandler.DASH
                                + providerCollectionLinkStore.getLinkStore()
                                        .getCreationTime()
                                + XML_EXTENSION);

                // create dirs if not there
                destinationFile.getParentFile().mkdirs();
                // write it out
                marshalAndWriteToFile(destinationFile, links);

                statusHandler.info("Wrote linkStore: " + providerName + " : "
                        + collectionName + " size: "
                        + links.getLinkKeys().size());
            }

        } catch (JAXBException | SerializationException | IOException e) {
            statusHandler.error("Error writing to linkStore: " + providerName
                    + " : " + collectionName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
    }

    /**
     * Create or get JAXB Manager to marshal and unmarshal JAXB objects
     * 
     * @return
     */
    private JAXBManager getJaxbManager() throws JAXBException {

        if (this.jaxbManager == null) {
            jaxbManager = new JAXBManager(LinkStore.class, Link.class);
        }
        return this.jaxbManager;
    }

}
