package com.raytheon.uf.edex.datadelivery.harvester.crawler;

import java.util.List;

import com.raytheon.uf.common.datadelivery.harvester.Agent;
import com.raytheon.uf.common.datadelivery.harvester.CrawlAgent;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfigurationManager;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.harvester.Launcher;

/**
 * Abstract Crawl Launcher
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 4, 2012  1038      dhladky     Initial creation
 * Nov 19, 2012 1166      djohnson    Clean up JAXB representation of registry objects.
 * Oct 28, 2013 2361      dhladky     Fixed up JAXBManager.
 * Jun 14, 2014 3120      dhladky     moved down object chain
 *
 * </pre>
 *
 * @author dhladky
 * @version 1.0
 */

public abstract class CrawlLauncher extends Launcher {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(CrawlLauncher.class);

    @Override
    public abstract String getType();

    /**
     * Run this at instantiation for crawlers
     */
    @Override
    public void init() {

        try {
            if (isInitial()) {
                // if many, start many
                List<LocalizationFile> files = Launcher.getLocalizedFiles();

                if (files != null) {
                    for (LocalizationFile lf : files) {

                        HarvesterConfig hc = HarvesterConfigurationManager
                                .getHarvesterFile(lf.getFile());

                        if (hc.getAgent() != null) {
                            // we only want crawler types for CrawlerMetadata
                            Agent agent = hc.getAgent();

                            if (agent instanceof CrawlAgent) {
                                // create our quartz beans
                                setProviderName(hc.getProvider().getName());
                                addHarvesterJobs(hc.getProvider().getName(),
                                        agent);
                            }
                        }
                    }
                }
            }

            setInitial(false);

        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Crawler Cron Beans failed to initialize!", e);
        }
    }

}
