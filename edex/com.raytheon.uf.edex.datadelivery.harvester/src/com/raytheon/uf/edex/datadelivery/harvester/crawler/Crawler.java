package com.raytheon.uf.edex.datadelivery.harvester.crawler;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.annotations.VisibleForTesting;
import com.raytheon.uf.common.comm.ProxyConfiguration;
import com.raytheon.uf.common.datadelivery.harvester.CrawlAgent;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfigurationManager;
import com.raytheon.uf.common.datadelivery.registry.Collection;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.harvester.crawler.MainSequenceCrawler.ModelCrawlConfiguration;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;

/**
 * Crawler super class
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 4, 2012   1038      dhladky     Initial creation
 * Oct 28, 2013  2361       dhladky     Fixed up JAXBManager.
 * 6/18/2014    1712        bphillip    Updated Proxy configuration
 * Jul 14, 2017  6178      tgurney     Remove communication strategy
 * Jul 19, 2017  6178      tgurney     Remove file locking mechanism
 *
 * </pre>
 *
 * @author dhladky
 */

public abstract class Crawler {

    protected String providerName;

    protected CrawlAgent agent;

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(Crawler.class);

    protected final HarvesterConfig hconfig;

    private static final Map<String, Lock> lockMap = new HashMap<>();

    /**
     * Get the model configuration.
     *
     * @param pmodelName
     *            the primary? model name
     * @param providerUrl
     *            the provider's url
     * @param dateFrag
     *            the date fragment
     * @param date
     * @return the model configuration
     */
    public static ModelCrawlConfiguration getModelConfiguration(
            String providerName, Collection collection, String providerUrl,
            String dateFrag, Date date, String searchKey, int politenessDelay) {

        String searchUrl = getUrl(providerUrl + collection.getSeedUrl(),
                collection.getUrlKey());

        if (!"".equals(dateFrag)) {
            searchUrl = searchUrl + dateFrag + '/';
        }

        return new ModelCrawlConfiguration(providerName, collection.getName(),
                collection.getName(), searchUrl, dateFrag, date, searchKey,
                politenessDelay);
    }

    /**
     * Returns the URL to use concatenating the portions with the url separator
     * character.
     *
     * @param portions
     *            the portions of the url
     *
     * @return the url to use
     */
    protected static String getUrl(String... portions) {
        return com.raytheon.uf.common.util.StringUtil.join(portions, '/');
    }

    /**
     * Read in the config file
     *
     * @param configFile
     *            the configuration file
     *
     * @return
     */
    protected static HarvesterConfig readConfig(File configFile) {

        HarvesterConfig hc = null;

        try {
            hc = HarvesterConfigurationManager.getHarvesterFile(configFile);
        } catch (Exception e1) {
            statusHandler.handle(Priority.ERROR, e1.getLocalizedMessage(), e1);
        }

        return hc;

    }

    /**
     * @param runWithinLock
     */
    protected static void runIfLockCanBeAcquired(final Runnable runWithinLock,
            String crawlType) {
        Lock theLock = null;
        synchronized (lockMap) {
            theLock = lockMap.get(crawlType);
            if (theLock == null) {
                theLock = new ReentrantLock();
                lockMap.put(crawlType, theLock);
            }
        }
        if (theLock.tryLock()) {
            try {
                runWithinLock.run();
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Crawler [" + crawlType + "] failed", e);
            } finally {
                theLock.unlock();
            }
        } else {
            statusHandler
                    .error("Unable to acquire lock, another instance must be running! "
                            + crawlType);
        }
    }

    /**
     * Super class construct
     *
     * @param hconfig
     * @param communicationStrategy
     */
    public Crawler(HarvesterConfig hconfig) {
        this.hconfig = hconfig;
    }

    /**
     * Deletes the contents of the crawler4j database after each run.
     */
    @VisibleForTesting
    protected void cleanupDatabase() {
        try {
            // Delete the database after every run
            EnvironmentConfig envConfig = new EnvironmentConfig();
            envConfig.setAllowCreate(true);
            envConfig.setTransactional(false);
            envConfig.setLocking(false);

            File envHome = new File(
                    ((CrawlAgent) hconfig.getAgent()).getCrawlDir()
                            + getCrawlerFrontier() + "/frontier");
            Environment env = new Environment(envHome, envConfig);
            try {
                env.removeDatabase(null, "DocIDs");
            } catch (Exception e) {
                statusHandler.handle(Priority.WARN,
                        "No Database existing to be removed. "
                                + getCrawlerFrontier(),
                        e);
            }
        } catch (Throwable t) {
            statusHandler.error(
                    "Unable to remove the existing crawler database!  "
                            + "This will cause DataSet information to be missed.",
                    t);
        }
    }

    /**
     * The crawling method call
     */
    protected abstract void crawl();

    public CrawlAgent getAgent() {
        return agent;
    }

    /**
     * Creates and returns the {@link CrawlConfig} to be used for crawling the
     * remote server.
     *
     * @return the {@link CrawlConfig}
     */
    /**
     * Creates and returns the {@link CrawlConfig} to be used for crawling the
     * remote server.
     *
     * @return the {@link CrawlConfig}
     */
    protected CrawlConfig getCrawlConfig() {

        CrawlConfig config = new CrawlConfig();
        CrawlAgent agent = (CrawlAgent) hconfig.getAgent();
        hconfig.getProvider().getName();

        config.setCrawlStorageFolder(
                agent.getCrawlDir() + getCrawlerFrontier());

        /*
         * You can set the maximum crawl depth here. The default value is -1 for
         * unlimited depth
         */
        config.setMaxDepthOfCrawling(getMaxDepthOfCrawl());

        /*
         * You can set the maximum number of pages to crawl. The default value
         * is -1 for unlimited number of pages
         */
        config.setMaxPagesToFetch(getMaxPages());

        /*
         * Do you need to set a proxy? If so, you can use:
         */
        if (ProxyConfiguration.HTTP_PROXY_DEFINED) {
            config.setProxyHost(ProxyConfiguration.getHttpProxyHost());
            config.setProxyPort(ProxyConfiguration.getHttpProxyPort());
        }

        /*
         * This config parameter can be used to set your crawl to be resumable
         * (meaning that you can resume the crawl from a previously
         * interrupted/crashed crawl). Note: if you enable resuming feature and
         * want to start a fresh crawl, you need to delete the contents of
         * rootFolder manually.
         */
        config.setResumableCrawling(false);

        return config;
    }

    protected abstract String getCrawlerFrontier();

    protected abstract int getMaxDepthOfCrawl();

    protected abstract int getMaxPages();

    public String getProviderName() {
        return providerName;
    }

    public void setAgent(CrawlAgent agent) {
        this.agent = agent;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

}
