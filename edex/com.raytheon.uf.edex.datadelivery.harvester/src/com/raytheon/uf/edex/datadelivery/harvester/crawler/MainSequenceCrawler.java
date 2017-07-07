package com.raytheon.uf.edex.datadelivery.harvester.crawler;

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

import java.io.File;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import com.google.common.annotations.VisibleForTesting;
import com.raytheon.uf.common.datadelivery.harvester.CrawlAgent;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.registry.Collection;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.ImmutableDate;
import com.raytheon.uf.common.time.util.TimeUtil;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

/**
 * Crawl for MetaData
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 20, 2011    218      dhladky     Initial creation
 * Jun 28, 2012    819      djohnson    Retrieve proxy information,
 *                                      use {@link FileLock}, add logging.
 * Jul 17, 2012    749      djohnson    Break out the use of files to communicate as a strategy.
 * Aug 06, 2012   1022      djohnson    Use ExecutorCommunicationStrategyDecorator.
 * Aug 28, 2012   1022      djohnson    Delete the crawler4j database after each run.
 * Aug 30, 2012   1123      djohnson    Use ThreadFactory that will comply with thread-based logging.
 * Sep 11, 2012   1154      djohnson    Change to create crawl configuration objects,
 *                                      then use them to perform configured crawls that can span days.
 * Oct 2, 2012    1038      dhladky     redesigned
 * 6/18/2014    1712        bphillip    Updated Proxy configuration
 * Jul 12, 2017   6178      tgurney     Updates for database-based link storage
 * </pre>
 *
 * @author dhladky
 */
public class MainSequenceCrawler extends Crawler {

    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

    private final CrawlerLinkDao crawlerLinkDao = new CrawlerLinkDao();

    /**
     * The configuration object for a crawl instance to be performed.
     */
    @VisibleForTesting
    public static class ModelCrawlConfiguration {
        private final String providerName;

        private final String modelName;

        private final String modelSubName;

        private final String searchUrl;

        private final String dateFrag;

        private final ImmutableDate date;

        private final String searchKey;

        private final int politenessDelay;

        public ModelCrawlConfiguration(String providerName, String modelName,
                String modelSubName, String searchUrl, String dateFrag,
                Date date, String searchKey, int politenessDelay) {
            this.providerName = providerName;
            this.modelName = modelName;
            this.modelSubName = modelSubName;
            this.searchUrl = searchUrl;
            this.dateFrag = dateFrag;
            this.date = new ImmutableDate(date);
            this.searchKey = searchKey;
            this.politenessDelay = politenessDelay;
        }

        /**
         * @return
         */
        public Date getDate() {
            return date;
        }

        /**
         * @return
         */
        public String getDateFrag() {
            return dateFrag;
        }

        /**
         * @return the modelName
         */
        public String getModelName() {
            return modelName;
        }

        /**
         * @return the modelSubName
         */
        public String getModelSubName() {
            return modelSubName;
        }

        /**
         * @return the politenessDelay
         */
        public int getPolitenessDelay() {
            return politenessDelay;
        }

        /**
         * @return
         */
        public String getProviderName() {
            return providerName;
        }

        /**
         * @return
         */
        public String getSearchKey() {
            return searchKey;
        }

        /**
         * @return the searchUrl
         */
        public String getSearchUrl() {
            return searchUrl;
        }
    }

    public static void run(String configFilePath) throws Exception {
        final File configFile = new File(configFilePath);
        final HarvesterConfig config = readConfig(configFile);
        final String providerName = config.getProvider().getName();

        Runnable runWithinLock = () -> {
            statusHandler.info("~~~~~~~~~~~~~~~~~" + providerName
                    + " Main Sequence Crawler~~~~~~~~~~~~~~~~~~~");
            MainSequenceCrawler crawl = new MainSequenceCrawler(config);
            crawl.setAgent((CrawlAgent) config.getAgent());
            crawl.setProviderName(providerName);
            crawl.crawl();
        };

        runIfLockCanBeAcquired(runWithinLock, providerName + "-main");
    }

    public MainSequenceCrawler(HarvesterConfig config) {
        super(config);
    }

    /**
     * DO the actual crawling of the site
     *
     * @param hconfig
     * @return
     */
    @Override
    public void crawl() {

        final List<ModelCrawlConfiguration> modelConfigurations = createModelCrawlConfigurations();
        boolean autoTrigger = false;

        if (!modelConfigurations.isEmpty()) {
            performCrawls(modelConfigurations);
        } else {
            statusHandler
                    .warn("Configuration for this provider contains no Collections. "
                            + hconfig.getProvider().getName());
            autoTrigger = true;
        }

        cleanupDatabase();

        if (autoTrigger) {
            // auto trigger a seed scan
            SeedCrawlLauncher scl = new SeedCrawlLauncher();
            scl.launch(hconfig.getProvider().getName());
        }
    }

    /**
     * Creates the list of model crawl configurations that will be performed.
     *
     * @return the list of {@link ModelCrawlConfiguration}s
     */
    @VisibleForTesting
    public List<ModelCrawlConfiguration> createModelCrawlConfigurations() {
        // The collection of configuration objects
        List<ModelCrawlConfiguration> modelConfigurations = new ArrayList<>();

        final CrawlAgent agent = (CrawlAgent) hconfig.getAgent();
        final Provider provider = hconfig.getProvider();
        final String providerName = provider.getName();
        List<Collection> collections = agent.getCollection();

        if (collections != null && !collections.isEmpty()) {
            final Date nonDateCollection = new ImmutableDate(0);

            for (Collection coll : collections) {
                // only process one's that are set to not be ignored
                if (!coll.isIgnore()) {
                    List<Date> datesToCrawl = new ArrayList<>();
                    Date date = TimeUtil.newImmutableDate();
                    long postedFileDelayMilliseconds = provider
                            .getPostedFileDelay().getMillis();

                    if (postedFileDelayMilliseconds > 0) {
                        // Check whether the posted file delay would place us in
                        // a
                        // new date
                        Date delayedDate = new ImmutableDate(
                                date.getTime() - postedFileDelayMilliseconds);

                        boolean isNewerDate = TimeUtil.isNewerDay(delayedDate,
                                date, GMT_TIME_ZONE);
                        if (isNewerDate) {
                            while (delayedDate.before(date)) {
                                datesToCrawl.add(delayedDate);
                                delayedDate = new ImmutableDate(
                                        delayedDate.getTime()
                                                + TimeUtil.MILLIS_PER_DAY);
                            }
                        }
                    }

                    if (coll.getFirstDate() != null
                            && coll.getLastDate() != null) {
                        // adds the most recent day
                        datesToCrawl.add(date);

                        // adds the last day according to seed scanning
                        datesToCrawl.add(coll.getLastDateAsDate());
                    } else {
                        // not a collection based on dates, crawl it always
                        datesToCrawl.add(nonDateCollection);
                    }

                    // sort/dup elim by format string
                    SortedSet<String> urlDates = new TreeSet<>();
                    for (Date dateToCrawl : datesToCrawl) {
                        if (dateToCrawl.equals(nonDateCollection)) {
                            urlDates.add("");
                        } else {
                            urlDates.add(coll.getUrlDate(dateToCrawl));
                        }
                    }

                    for (String urlDate : urlDates) {
                        ModelCrawlConfiguration modelConfiguration = getModelConfiguration(
                                providerName, coll, provider.getUrl(), urlDate,
                                date, agent.getSearchKey(),
                                provider.getTimeBetweenCrawlRequests());

                        modelConfigurations.add(modelConfiguration);
                    }
                }
            }
        }

        return modelConfigurations;
    }

    /**
     * Gets the frontier
     *
     * @return
     */
    @Override
    protected String getCrawlerFrontier() {
        return "/" + getProviderName() + "/main";
    }

    @Override
    protected int getMaxDepthOfCrawl() {
        return getAgent().getMaxMainDepth();
    }

    @Override
    protected int getMaxPages() {
        return getAgent().getMaxMainPages();
    }

    /**
     * Performs the actual crawls based on the collection of
     * {@link ModelCrawlConfiguration} objects.
     *
     * @param configurations
     *            the configuration objects
     */
    @VisibleForTesting
    private void performCrawls(List<ModelCrawlConfiguration> configurations) {
        CrawlConfig config = getCrawlConfig();
        CrawlAgent agent = (CrawlAgent) hconfig.getAgent();
        final int totalCount = configurations.size();

        for (int i = 0; i < totalCount; i++) {
            ModelCrawlConfiguration modelConfiguration = configurations.get(i);
            final String dateFrag = modelConfiguration.getDateFrag();
            final String modelName = modelConfiguration.getModelName();
            final String providerName = modelConfiguration.getProviderName();
            final int politenessDelay = modelConfiguration.getPolitenessDelay();

            config.setPolitenessDelay(politenessDelay);

            statusHandler.info(String.format(
                    "Starting crawl [%s/%s], provider [%s], collection [%s], model [%s], date [%s], politeness delay [%s]",
                    i + 1, totalCount, providerName, modelName,
                    modelConfiguration.getModelSubName(), dateFrag,
                    politenessDelay));

            List<CrawlerLink> links = new ArrayList<>();
            String searchUrl = modelConfiguration.getSearchUrl();

            // The crawler library objects. These must be created anew for each
            // loop, otherwise results are ignored.
            PageFetcher pageFetcher = new CrawlMonitorPageFetcher(providerName,
                    config);
            RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
            robotstxtConfig.setEnabled(agent.isUseRobots());
            RobotstxtServer robotstxtServer = new RobotstxtServer(
                    robotstxtConfig, pageFetcher);

            /*
             * Instantiate the controller for this crawl.
             */
            CrawlController crawlcontroller = null;
            try {
                crawlcontroller = new CrawlController(config, pageFetcher,
                        robotstxtServer);
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }

            /*
             * For each crawl, you need to add some seed urls. These are the
             * first URLs that are fetched and then the crawler starts following
             * links which are found in these pages
             */
            crawlcontroller.addSeed(searchUrl);

            ArrayList<WebCrawler> webCrawlers = new ArrayList<>(1);
            CrawlerLink linkTemplate = new CrawlerLink();
            linkTemplate.setProviderName(providerName);
            linkTemplate.setCollectionName(modelName);
            linkTemplate.setSubName(modelConfiguration.getModelSubName());
            webCrawlers.add(new MainSequenceHarvester(searchUrl,
                    modelConfiguration.getSearchKey(), links, agent.getIgnore(),
                    linkTemplate));

            // start the crawling, blocks thread till finished
            crawlcontroller.start(webCrawlers, webCrawlers.size());
            crawlcontroller.Shutdown();
            crawlerLinkDao.createLinks(links);
        }
    }
}
