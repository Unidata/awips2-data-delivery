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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.annotations.VisibleForTesting;
import com.raytheon.uf.common.comm.ProxyConfiguration;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.URLParserInfo;
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
 * This Crawler looks for new data URLParserInfo and stores the information
 * about the URLParserInfo in a configuration file. This information is then
 * used by the MainSequence Crawler to look for links to process. Uses a seed
 * harvester to gather the information and then converts them to URLParserInfos.
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
 * Oct 04, 2017  6465     tjensen   Refactor for URLParserInfos cleanup
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

    private void processURLParserInfo(
            Map<String, URLParserInfo> newURLParserInfo) {

        // start processing the URLParserInfo
        for (URLParserInfo upi : newURLParserInfo.values()) {

            // if we ingestNew this will be true
            upi.setIgnore(!agent.isIngestNew());
            statusHandler.info("adding new URLParserInfo: " + upi.getName());

        }

        statusHandler.info("Processing complete: " + newURLParserInfo.size()
                + " new URLParserInfo found");

        if (!newURLParserInfo.isEmpty()) {
            LookupManagerUtils.writeURLParserInfoUpdates(providerName,
                    newURLParserInfo);
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
            statusHandler.error(
                    "Error instantiating CrawlController. Seed crawl aborted.",
                    e);
            return;
        }

        /*
         * For each crawl, you need to add some seed urls. These are the first
         * URLs that are fetched and then the crawler starts following links
         * which are found in these pages
         */
        crawlcontroller.addSeed(searchUrl);

        /*
         * if URLParserInfos already exist for provider, we'll add those seed
         * URL's as ignores
         */
        List<String> knownURLParserInfo = new ArrayList<>();
        if (urlParserInfoMap != null) {
            for (URLParserInfo upi : urlParserInfoMap.values()) {
                knownURLParserInfo.add(upi.getSeedUrl());
            }
        }

        // Key for both maps will be the URLParserInfo name
        Map<String, URLParserInfo> newURLParserInfo = new HashMap<>();
        Map<String, Set<Date>> dates = new HashMap<>();

        ArrayList<WebCrawler> webCrawlers = new ArrayList<>(1);
        webCrawlers.add(new SeedHarvester(searchUrl, agent.getSearchKey(),
                newURLParserInfo, dates, knownURLParserInfo,
                agent.getIgnore()));

        statusHandler.info("Starting crawl...");
        // start the crawling, blocks thread till finished
        crawlcontroller.start(webCrawlers, webCrawlers.size());
        crawlcontroller.Shutdown();
        statusHandler.info("Finished crawl...");

        processURLParserInfo(newURLParserInfo);
        processDates(dates, newURLParserInfo);
    }

    private void processDates(Map<String, Set<Date>> dates,
            Map<String, URLParserInfo> newURLParserInfo) {
        for (Entry<String, Set<Date>> entry : dates.entrySet()) {
            String key = entry.getKey();
            Set<Date> dateList = entry.getValue();

            URLParserInfo upi = newURLParserInfo.get(key);
            SimpleDateFormat sdf = new SimpleDateFormat();
            sdf.applyLocalizedPattern(upi.getDateFormat());
            Set<String> dateStrings = new TreeSet<>();

            for (Date date : dateList) {
                dateStrings.add(sdf.format(date));
            }
            LookupManagerUtils.writeCrawlerDates(key, dateStrings);
        }
    }
}
