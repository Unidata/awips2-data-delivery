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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.raytheon.uf.common.datadelivery.harvester.CrawlAgent;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.registry.Collection;
import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Utils;
import com.raytheon.uf.common.datadelivery.registry.handlers.ProviderHandler;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.edex.datadelivery.harvester.MetaDataHandler;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IExtractMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IParseMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IServiceFactory;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.Link;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.LinkStore;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.ProviderCollectionLinkStore;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.ServiceTypeFactory;

import opendap.dap.DAS;

/**
 * Harvest MetaData
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 20, 2011  218      dhladky   Initial creation
 * Jul 17, 2012  749      djohnson  Break out the use of files to communicate as
 *                                  a strategy.
 * Jul 24, 2012  955      djohnson  Use the Abstract Factory Pattern to simplify
 *                                  service specific access.
 * Aug 30, 2012  1123     djohnson  Rename CrawlerEvent to HarvesterEvent.
 * Sep 12, 2012  1038     dhladky   Reconfigured config.
 * Oct 03, 2012  1241     djohnson  Use registry handler.
 * Nov 09, 2012  1263     dhladky   Changed to Site Level
 * Feb 05, 2013  1580     mpduff    EventBus refactor.
 * Mar 18, 2013  1802     bphillip  Modified to insert provider object after
 *                                  database is initialized
 * Jun 24, 2013  2106     djohnson  Accepts ProviderHandler as a constructor
 *                                  argument.
 * Oct 28, 2013  2361     dhladky   Fixed up JAXBManager.
 * Mar 31, 2014  2889     dhladky   Added username for notification center
 *                                  tracking.
 * Jul 08, 2014  3120     dhladky   More generics
 * Apr 12, 2015  4400     dhladky   Upgrade to DAP2 for harvesting.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Dec 14, 2016  5988     tjensen   Clean up error handling for crawler
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class CrawlMetaDataHandler extends MetaDataHandler {

    public static final String DASH = "-";

    /** Path to crawler links directory. */
    private static final String PROCESSED_DIR = StringUtil.join(
            new String[] { "datadelivery", "harvester", "processed" },
            File.separatorChar);

    private final CommunicationStrategy communicationStrategy;

    private final File timesDir;

    public CrawlMetaDataHandler(CommunicationStrategy communicationStrategy,
            ProviderHandler providerHandler) {
        this.communicationStrategy = communicationStrategy;
        this.providerHandler = providerHandler;

        IPathManager pm = PathManagerFactory.getPathManager();

        LocalizationContext lc = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.CONFIGURED);
        LocalizationFile lf = pm.getLocalizationFile(lc, PROCESSED_DIR);
        timesDir = lf.getFile();

    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private Set<String> getPreviousRun(String collectionName,
            String providerName, String dateString) {

        // default if it is brand new
        Set<String> previousRun = new HashSet<>();

        try {
            File file = getPreviousRunsFile(providerName, collectionName,
                    dateString);
            if (file != null && file.exists() && file.length() > 0) {
                previousRun = (Set<String>) SerializationUtil
                        .transformFromThrift(FileUtil.file2bytes(file));
            }
            statusHandler.info("Read previous URLs for " + providerName + " : "
                    + collectionName + " : " + dateString);
        } catch (SerializationException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        } catch (IOException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to read previous runs file!", e);
        }

        return previousRun;
    }

    private File getPreviousRunsFile(String providerName, String collectionName,
            String dateString) {

        return new File(timesDir, providerName + DASH + collectionName + DASH
                + dateString + ".bin");
    }

    /**
     * Check the files found by the crawler
     * 
     * @throws Exception
     */
    public void metaDataCheck() {

        statusHandler.info("Checking for new Crawl MetaData.....");
        hconfigs = readConfigs();

        ProviderCollectionLinkStore providerCollectionLinkStore = null;

        while ((providerCollectionLinkStore = communicationStrategy
                .getNextLinkStore()) != null) {
            String collectionName = providerCollectionLinkStore
                    .getCollectionName();
            String providerName = providerCollectionLinkStore.getProviderName();
            LinkStore store = providerCollectionLinkStore.getLinkStore();
            if (store != null && hconfigs != null) {
                Set<String> previousRun = getPreviousRun(collectionName,
                        providerName, store.getDateString());

                HarvesterConfig hc = hconfigs.get(providerName);
                CrawlAgent agent = (CrawlAgent) hc.getAgent();
                Collection collection = agent
                        .getCollectionByName(collectionName);

                if (collection != null) {

                    Provider provider = hc.getProvider();
                    IServiceFactory<String, DAS, GriddedTime, GriddedCoverage> serviceFactory = ServiceTypeFactory
                            .retrieveServiceFactory(provider);

                    IExtractMetaData<String, DAS> mde = serviceFactory
                            .getExtractor();

                    // remove previous run
                    store.getLinkKeys().removeAll(previousRun);

                    // extract metadata, process each link
                    List<String> removes = new ArrayList<>();

                    for (String linkKey : store.getLinkKeys()) {
                        Link link = store.getLink(linkKey);
                        String url = link.getUrl();
                        try {
                            link.setLinks(mde.extractMetaData(url));
                            mde.setDataDate();
                        } catch (Exception e) {
                            final String userFriendly = String.format(
                                    "Unable to retrieve metadata for dataset group %s: %s.",
                                    collectionName, url);
                            statusHandler.error(userFriendly, e);

                            // If we can't extract it, we can't parse it, so
                            // remove
                            removes.add(linkKey);
                        }
                    }

                    // remove failed entries
                    if (!removes.isEmpty()) {
                        store.getLinkKeys().removeAll(removes);
                    }

                    if (!store.getLinkKeys().isEmpty()) {
                        // now start parsing the metadata objects
                        String directoryDateFormat = collection.getDateFormat();
                        String dataDateFormat = agent.getDateFormat();
                        Date lastUpdate = null;

                        try {
                            if (!directoryDateFormat.equals("")) {
                                lastUpdate = Utils.convertDate(
                                        directoryDateFormat,
                                        store.getDateString());
                            } else {
                                // use current time
                                lastUpdate = new Date(
                                        System.currentTimeMillis());
                            }
                        } catch (ParseException e1) {
                            throw new IllegalArgumentException(
                                    "Unable to parse a date!", e1);
                        }

                        IParseMetaData mdp = serviceFactory
                                .getParser(lastUpdate);

                        try {
                            mdp.parseMetaData(provider, store, collection,
                                    dataDateFormat);
                            previousRun.addAll(store.getLinkKeys());
                        } catch (Exception e) {
                            final String userFriendly = String.format(
                                    "Unable to parse metadata for dataset group %s.",
                                    collectionName);

                            statusHandler.handle(Priority.PROBLEM, userFriendly,
                                    e);
                        }

                        writePreviousRun(previousRun, collectionName,
                                providerName, store.getDateString());
                    } else {
                        statusHandler.info("No new data for " + providerName
                                + " : " + collectionName + " : "
                                + store.getDateString());
                    }
                } else {
                    statusHandler.handle(Priority.ERROR,
                            "Collection is null, please check your configuration!");
                }
            } else {
                statusHandler.handle(Priority.ERROR,
                        "Harvester Config and or store are null, please check your configuration!");
            }
        }
    }

    /**
     * Write the previous keys
     * 
     * @return
     */
    private void writePreviousRun(Set<String> previousRun,
            String collectionName, String providerName, String dateString) {

        try {
            File file = getPreviousRunsFile(providerName, collectionName,
                    dateString);

            FileUtil.bytes2File(
                    SerializationUtil.transformToThrift(previousRun), file,
                    false);

        } catch (SerializationException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        } catch (IOException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

        statusHandler
                .info("Wrote URLs: " + providerName + " : " + collectionName
                        + " : " + dateString + " size: " + previousRun.size());
    }
}
