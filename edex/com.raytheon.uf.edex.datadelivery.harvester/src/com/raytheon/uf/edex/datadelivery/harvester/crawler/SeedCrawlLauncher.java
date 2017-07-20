package com.raytheon.uf.edex.datadelivery.harvester.crawler;

import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.common.datadelivery.harvester.Agent;
import com.raytheon.uf.common.datadelivery.harvester.CrawlAgent;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfigurationManager;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.harvester.cron.HarvesterJobController;

/**
 * Launch crawler in the same JVM.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 4, 2012  1038      dhladky     Initial creation
 * Nov 19, 2012 1166      djohnson    Clean up JAXB representation of registry objects.
 * Oct 28, 2013 2361       dhladky     Fixed up JAXBManager.
 * Jun 14, 2014 3120       dhladky     Changed method name so it's more flexible
 * Jul 19, 2017 6178       tgurney     Remove call to main()
 *
 * </pre>
 *
 * @author dhladky
 */

public class SeedCrawlLauncher extends CrawlLauncher {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SeedCrawlLauncher.class);

    private final List<HarvesterJobController<SeedCrawlLauncher>> harvesterJobs = new ArrayList<>();

    public SeedCrawlLauncher() {
        harvesterJobs.clear();
        init();
    }

    @Override
    public void addHarvesterJobs(String providerName, Agent agent) {

        getHarvesterJobs().add(new HarvesterJobController<>(
                providerName + "-" + getType(),
                ((CrawlAgent) agent).getSeedScan(), SeedCrawlLauncher.class));
        statusHandler.handle(Priority.INFO,
                "Added/Updated " + providerName + " Seed Scan entry.");
    }

    @Override
    public void launch(String providerName) {
        try {
            for (LocalizationFile lf : getLocalizedFiles()) {
                HarvesterConfig hc = HarvesterConfigurationManager
                        .getHarvesterFile(lf.getFile());
                if (hc.getProvider().getName().equals(providerName)) {
                    if (hc.getAgent() != null) {
                        // we only want crawler types for CrawlerMetadata
                        Agent agent = hc.getAgent();
                        if (agent instanceof CrawlAgent) {
                            SeedCrawler.run(lf.getFile().getAbsolutePath());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Crawler failed to initialize!", e);
        }
    }

    public List<HarvesterJobController<SeedCrawlLauncher>> getHarvesterJobs() {
        return harvesterJobs;
    }

    @Override
    public String getType() {
        return "seed";
    }
}
