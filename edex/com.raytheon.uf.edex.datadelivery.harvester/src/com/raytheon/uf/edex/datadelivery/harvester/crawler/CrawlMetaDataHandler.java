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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.raytheon.uf.common.datadelivery.harvester.CrawlAgent;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.URLParserInfo;
import com.raytheon.uf.common.datadelivery.registry.handlers.ProviderHandler;
import com.raytheon.uf.common.datadelivery.retrieval.util.LookupManager;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.ITimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.query.DatabaseQuery;
import com.raytheon.uf.edex.datadelivery.harvester.MetaDataHandler;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IExtractMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IParseMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.Link;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.ServiceTypeFactory;
import com.raytheon.uf.edex.datadelivery.retrieval.opendap.OpenDapServiceFactory;

import opendap.dap.DAS;

/**
 * Processes links that have been found for known URLParserInfos by the crawler
 * and parses them for metadata to add to the registry.
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
 * Jul 12, 2017  6178     tgurney   Change link storage from file system to database
 * Aug 31, 2017  6430     rjpeter   Added timing information.
 * Oct 04, 2017  6465     tjensen   Get URLParserInfos from config file
 *
 * </pre>
 *
 * @author dhladky
 */

public class CrawlMetaDataHandler extends MetaDataHandler {

    private final CrawlerLinkDao crawlerLinkDao = new CrawlerLinkDao();

    public CrawlMetaDataHandler(ProviderHandler providerHandler) {
        this.providerHandler = providerHandler;
    }

    /**
     * Get next available links for a single provider/collection
     *
     * @return links
     */
    private synchronized List<CrawlerLink> getLinks() {
        return crawlerLinkDao.getLinks();
    }

    /**
     * Check the files found by the crawler
     *
     * @throws Exception
     */
    public void metaDataCheck() {
        statusHandler.info("Checking for new Crawl MetaData.....");
        hconfigs = readConfigs();
        try {
            removeOldLinks();
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, "Failed to delete old links",
                    e);
        }
        List<CrawlerLink> crawlerLinks = null;

        while ((crawlerLinks = getLinks()) != null && !crawlerLinks.isEmpty()) {
            String collectionName = crawlerLinks.get(0).getCollectionName();
            String providerName = crawlerLinks.get(0).getProviderName();
            statusHandler.info("Got " + crawlerLinks.size() + " links for "
                    + providerName + " : " + collectionName);
            if (hconfigs != null) {
                HarvesterConfig hc = hconfigs.get(providerName);
                CrawlAgent agent = (CrawlAgent) hc.getAgent();
                URLParserInfo urlParserInfo = LookupManager.getInstance()
                        .getURLParserInfoForProvider(providerName)
                        .get(collectionName);

                if (urlParserInfo != null) {
                    Provider provider = hc.getProvider();
                    OpenDapServiceFactory serviceFactory = (OpenDapServiceFactory) ServiceTypeFactory
                            .retrieveServiceFactory(provider.getServiceType());
                    serviceFactory.setProvider(provider);

                    IExtractMetaData<String, DAS> mde = serviceFactory
                            .getExtractor();

                    List<CrawlerLink> removes = new ArrayList<>();
                    for (CrawlerLink link : crawlerLinks) {

                        String url = link.getUrl();

                        try {
                            link.setMetadata(mde.extractMetaData(url));
                            mde.setDataDate();
                        } catch (Exception e) {
                            final String userFriendly = String.format(
                                    "Unable to retrieve metadata for dataset group %s: %s",
                                    collectionName, url);
                            statusHandler.error(userFriendly, e);

                            /*
                             * If we can't extract it, we can't parse it, so
                             * remove
                             */
                            removes.add(link);
                        }
                    }
                    crawlerLinks.removeAll(removes);
                    crawlerLinkDao.setAllProcessed(removes);

                    if (!crawlerLinks.isEmpty()) {
                        // now start parsing the metadata objects
                        String dataDateFormat = agent.getDateFormat();

                        IParseMetaData mdp = serviceFactory.getParser();

                        ITimer timer = TimeUtil.getTimer();
                        timer.start();
                        try {
                            List<Link> links = crawlerLinks.stream()
                                    .map(CrawlerLink::asLink)
                                    .collect(Collectors.toList());
                            mdp.parseMetaData(provider, links, urlParserInfo,
                                    dataDateFormat);
                            crawlerLinkDao.setAllProcessed(crawlerLinks);
                            statusHandler.info("Successfully processed "
                                    + links.size() + " links for "
                                    + providerName + " : " + collectionName);
                            crawlerLinkDao.createLinks(crawlerLinks);
                        } catch (Exception e) {
                            statusHandler
                                    .error("Unable to parse metadata for dataset group"
                                            + collectionName, e);
                        }
                        timer.stop();
                        statusHandler.info("Parsed and stored metadata from ["
                                + providerName + "] for model ["
                                + urlParserInfo.getName() + "] in [" + TimeUtil
                                        .prettyDuration(timer.getElapsedTime())
                                + "]");
                    } else {
                        statusHandler.info("No new data for " + providerName
                                + " : " + collectionName);
                    }
                } else {
                    statusHandler.handle(Priority.ERROR,
                            "URLParserInfo is null, please check your configuration!");
                }
            } else {
                statusHandler.handle(Priority.ERROR,
                        "Harvester Config and or store are null, please check your configuration!");
            }
        }
    }

    public void removeOldLinks() {
        List<String> providerNames = null;
        DatabaseQuery q = new DatabaseQuery(CrawlerLink.class);
        q.addDistinctParameter("providerName");
        try {
            providerNames = (List<String>) crawlerLinkDao.queryByCriteria(q);
        } catch (DataAccessLayerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Failed to query provider names", e);
            return;
        }
        for (String providerName : providerNames) {
            HarvesterConfig hc = hconfigs.get(providerName);
            if (hc != null) {
                int daysToKeepLinks = Integer.parseInt(hc.getRetention());
                crawlerLinkDao.removeOldLinks(providerName, daysToKeepLinks);
            }
        }
    }

}
