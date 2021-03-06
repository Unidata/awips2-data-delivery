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
 * Mar 14, 2012 00357      dhladky     Initial creation
 * Jun 12, 2012 00609      djohnson    Update path to crawl script.
 * Aug 06, 2012 01022      djohnson    Launch the crawler in the same JVM.
 * Oct 28, 2013 2361       dhladky     Fixed up JAXBManager.
 * Jun 14, 2014 3120       dhladky     Changed method name so it's more flexible
 * Jul 19, 2017 6178       tgurney     Remove call to main()
 *
 * </pre>
 *
 * @author dhladky
 */

public class MainSequenceCrawlLauncher extends CrawlLauncher {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(MainSequenceCrawlLauncher.class);

    private final List<HarvesterJobController<MainSequenceCrawlLauncher>> harvesterJobs = new ArrayList<>();

    public MainSequenceCrawlLauncher() {
        harvesterJobs.clear();
        init();
    }

    @Override
    public void addHarvesterJobs(String providerName, Agent agent) {
        getHarvesterJobs().add(
                new HarvesterJobController<>(providerName + "-" + getType(),
                        ((CrawlAgent) agent).getMainScan(),
                        MainSequenceCrawlLauncher.class));
        statusHandler.handle(Priority.INFO,
                "Added " + providerName + " Main Scan entry.");
    }

    @Override
    public void launch(String providerName) {
        try {
            // if many, start many
            for (LocalizationFile lf : getLocalizedFiles()) {
                HarvesterConfig hc = HarvesterConfigurationManager
                        .getHarvesterFile(lf.getFile());
                if (hc.getProvider().getName().equals(providerName)) {
                    if (hc.getAgent() != null) {
                        // we only want crawler types for CrawlerMetadata
                        Agent agent = hc.getAgent();
                        if (agent instanceof CrawlAgent) {
                            MainSequenceCrawler
                                    .run(lf.getFile().getAbsolutePath());
                        }
                    }
                }
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Crawler failed to initialize!", e);
        }
    }

    public List<HarvesterJobController<MainSequenceCrawlLauncher>> getHarvesterJobs() {
        return harvesterJobs;
    }

    @Override
    public String getType() {
        return "main";
    }

}
