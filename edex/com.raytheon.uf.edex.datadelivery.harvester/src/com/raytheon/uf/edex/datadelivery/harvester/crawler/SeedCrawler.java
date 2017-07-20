package com.raytheon.uf.edex.datadelivery.harvester.crawler;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.annotations.VisibleForTesting;
import com.raytheon.uf.common.comm.ProxyConfiguration;
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
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.edex.datadelivery.harvester.crawler.MainSequenceCrawler.ModelCrawlConfiguration;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

/**
 * Crawl for new models / datasets
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 4, 2012  1038       dhladky     Initial creation
 * 6/18/2014    1712        bphillip    Updated Proxy configuration
 * Jul 14, 2017 6178       tgurney     Clean up unneeded code
 *
 * </pre>
 *
 * @author dhladky
 */
public class SeedCrawler extends Crawler {

    protected static final String CONFIG_FILE_PREFIX = StringUtil.join(
            new String[] { "datadelivery", "harvester" }, File.separatorChar);

    protected static final String CONFIG_FILE_SUFFIX = "harvester.xml";

    public static void run(final String configFilePath) throws Exception {
        final File configFile = new File(configFilePath);
        final HarvesterConfig config = readConfig(configFile);
        final String providerName = config.getProvider().getName();

        Runnable runWithinLock = () -> {
            statusHandler.info("~~~~~~~~~~~~~~~~" + providerName
                    + " Seed Search Crawler~~~~~~~~~~~~~~~~~~");
            SeedCrawler crawl = new SeedCrawler(config);
            crawl.setAgent((CrawlAgent) config.getAgent());
            crawl.setProviderName(providerName);
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
    public SeedCrawler(HarvesterConfig config) {
        super(config);
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

    private void processCollections(HarvesterConfig hconfig,
            Map<String, ProtoCollection> collections, Provider provider,
            CrawlAgent agent) {

        ArrayList<Collection> newCollections = new ArrayList<>();
        // start processing the proto collections into real collections
        for (Entry<String, ProtoCollection> entry : collections.entrySet()) {
            ProtoCollection pc = entry.getValue();
            try {
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
                statusHandler.error("Error parsing proto-collection '"
                        + pc.getCollectionName() + "' from "
                        + provider.getName(), e);
            }
        }

        statusHandler.info("Processing complete: " + newCollections.size()
                + " new collections, " + collections.size()
                + " total collections found");

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

    private void saveNewConfig(HarvesterConfig hconfig, String provider) {
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext lc = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.SITE);
        String fileName = CONFIG_FILE_PREFIX + IPathManager.SEPARATOR + provider
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
     * Performs the actual crawls based on the collection of
     * {@link ModelCrawlConfiguration} objects.
     *
     * @param configurations
     *            the configuration objects
     */
    @VisibleForTesting
    private void performSeedCrawl() {

        Provider provider = hconfig.getProvider();
        CrawlAgent agent = (CrawlAgent) hconfig.getAgent();

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
        if (agent.getCollection() != null) {
            for (Collection coll : agent.getCollection()) {
                // compare to see if it is missing updates and such
                if (agent.isMature(coll.getName())) {
                    knownCollections.add(coll.getSeedUrl());
                }
            }
        }

        // SeedLinkStore seedLinks = new SeedLinkStore();
        HashMap<String, ProtoCollection> collections = new HashMap<>();

        ArrayList<WebCrawler> webCrawlers = new ArrayList<>(1);
        webCrawlers.add(new SeedHarvester(searchUrl, agent.getSearchKey(),
                collections, knownCollections, agent.getIgnore()));

        statusHandler.info("Starting crawl...");
        // start the crawling, blocks thread till finished
        crawlcontroller.start(webCrawlers, webCrawlers.size());
        crawlcontroller.Shutdown();
        statusHandler.info("Finished crawl...");

        processCollections(hconfig, collections, provider, agent);

    }

}
