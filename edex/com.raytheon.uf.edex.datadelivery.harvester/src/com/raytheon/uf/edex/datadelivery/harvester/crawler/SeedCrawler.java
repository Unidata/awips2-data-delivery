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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.raytheon.uf.common.comm.ProxyConfiguration;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.harvester.ProtoCollection;
import com.raytheon.uf.common.datadelivery.registry.Collection;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.retrieval.util.LookupManagerUtils;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.harvester.crawler.MainSequenceCrawler.ModelCrawlConfiguration;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

/**
 * This Crawler looks for new data Collections and stores the information about
 * the Collection in a configuration file. This information is then used by the
 * MainSequence Crawler to look for links to process. Uses a seed harvester to
 * gather the ProtoCollections and then converts them to Collections.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------
 * Oct 04, 2012  1038     dhladky   Initial creation
 * Jun 18, 2014  1712     bphillip  Updated Proxy configuration
 * Jul 14, 2017  6178     tgurney   Clean up unneeded code
 * Oct 04, 2017  6465     tjensen   Refactor for collections cleanup
 *
 * </pre>
 *
 * @author dhladky
 */
public class SeedCrawler extends Crawler {

    public static void run(final String configFilePath) throws Exception {
        final File configFile = new File(configFilePath);
        final HarvesterConfig config = readConfig(configFile);
        final String providerName = config.getProvider().getName();

        Runnable runWithinLock = () -> {
            statusHandler.info("~~~~~~~~~~~~~~~~" + providerName
                    + " Seed Search Crawler~~~~~~~~~~~~~~~~~~");
            SeedCrawler crawl = new SeedCrawler(config, providerName);
            crawl.crawl();
        };

        runIfLockCanBeAcquired(runWithinLock, providerName + "-seed");
    }

    /**
     * Construct a {@link SeedCrawler} that will be initialized from the
     * specified configuration file.
     *
     * @param configFile
     *            the configuration file
     */
    public SeedCrawler(HarvesterConfig config, String providerName) {
        super(config, providerName);
        if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
            if (ProxyConfiguration.HTTP_PROXY_DEFINED) {
                statusHandler.debug(
                        String.format("proxy host:[%s]  proxy port: [%s]",
                                ProxyConfiguration.getHttpProxyHost(),
                                ProxyConfiguration.getHttpProxyPortString()));
            } else {
                statusHandler.debug("No proxy information configured.");
            }
        }
    }

    /**
     * DO the actual crawling of the site
     *
     * @param hconfig
     * @return
     */
    @Override
    public void crawl() {
        performSeedCrawl();
        cleanupDatabase();
    }

    @Override
    protected String getCrawlerFrontier() {
        return "/" + getProviderName() + "/seed";
    }

    @Override
    protected int getMaxDepthOfCrawl() {
        return getAgent().getMaxSeedDepth();
    }

    @Override
    protected int getMaxPages() {
        return getAgent().getMaxSeedPages();
    }

    private void processCollections(
            Map<String, ProtoCollection> protoCollections, Provider provider) {

        ArrayList<Collection> newCollections = new ArrayList<>();
        // start processing the proto collections into real collections
        for (ProtoCollection pc : protoCollections.values()) {
            try {
                // make first date, find greatest depth dates
                Collection coll = new Collection(pc.getCollectionName(),
                        pc.getSeedUrl(), pc.getDateFormatString());
                coll.setUrlKey(pc.getUrlKey());
                coll.setDates(pc.getSortedDateStrings());
                // if we ingestNew this will be true
                if (agent.isIngestNew()) {
                    coll.setIgnore(false);
                }
                newCollections.add(coll);
                statusHandler.info(
                        "adding new Collection: " + pc.getCollectionName());
                // announce
            } catch (Exception e) {
                statusHandler.error("Error parsing proto-collection '"
                        + pc.getCollectionName() + "' from "
                        + provider.getName(), e);
            }
        }

        statusHandler.info("Processing complete: " + newCollections.size()
                + " new collections, " + protoCollections.size()
                + " total collections found");

        if (!newCollections.isEmpty()) {
            LookupManagerUtils.writeCollectionUpdates(providerName,
                    newCollections);
        }
    }

    /**
     * Performs the actual crawls based on the collection of
     * {@link ModelCrawlConfiguration} objects.
     *
     * @param configurations
     *            the configuration objects
     */
    @VisibleForTesting
    private void performSeedCrawl() {

        Provider provider = hconfig.getProvider();

        CrawlConfig config = getCrawlConfig();
        // Do fast for seed scans, don't be polite
        config.setPolitenessDelay(10);

        statusHandler.info(String.format(
                "Starting Seed crawl provider [%s], politeness delay [%s]",
                provider.getName(), config.getPolitenessDelay()));

        String searchUrl = provider.getUrl();

        // The crawler library objects. These must be created anew for each
        // loop, otherwise results are ignored.
        PageFetcher pageFetcher = new CrawlMonitorPageFetcher(
                provider.getName(), config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        robotstxtConfig.setEnabled(agent.isUseRobots());
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig,
                pageFetcher);

        /*
         * Instantiate the controller for this crawl.
         */
        CrawlController crawlcontroller = null;
        try {
            crawlcontroller = new CrawlController(config, pageFetcher,
                    robotstxtServer);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

        /*
         * For each crawl, you need to add some seed urls. These are the first
         * URLs that are fetched and then the crawler starts following links
         * which are found in these pages
         */
        crawlcontroller.addSeed(searchUrl);

        // if collections already exist for provider, we'll add those seed URL's
        // as ignores
        List<String> knownCollections = new ArrayList<>();
        if (collections != null) {
            for (Collection coll : collections.values()) {
                knownCollections.add(coll.getSeedUrl());
            }
        }

        // SeedLinkStore seedLinks = new SeedLinkStore();
        Map<String, ProtoCollection> protoCollections = new HashMap<>();

        ArrayList<WebCrawler> webCrawlers = new ArrayList<>(1);
        webCrawlers.add(new SeedHarvester(searchUrl, agent.getSearchKey(),
                protoCollections, knownCollections, agent.getIgnore()));

        statusHandler.info("Starting crawl...");
        // start the crawling, blocks thread till finished
        crawlcontroller.start(webCrawlers, webCrawlers.size());
        crawlcontroller.Shutdown();
        statusHandler.info("Finished crawl...");

        processCollections(protoCollections, provider);
    }
}
