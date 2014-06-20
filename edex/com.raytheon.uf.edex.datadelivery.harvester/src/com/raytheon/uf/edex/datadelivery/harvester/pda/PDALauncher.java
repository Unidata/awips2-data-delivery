package com.raytheon.uf.edex.datadelivery.harvester.pda;

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
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfigurationManager;
import com.raytheon.uf.common.datadelivery.harvester.PDAAgent;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.harvester.Launcher;
import com.raytheon.uf.edex.datadelivery.harvester.cron.HarvesterJobController;

/**
 * PDA Launcher, launches the PDA harvester by sending a CSW request to the PDA provider.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * June 13, 2014 3120      dhladky     Initial creation
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */


public class PDALauncher extends Launcher {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDALauncher.class);
    
    private final List<HarvesterJobController<PDALauncher>> harvesterJobs = new ArrayList<HarvesterJobController<PDALauncher>>();
    
    public PDALauncher() {
        harvesterJobs.clear();
        init();
    }
    
    @Override
    public void addHarvesterJobs(String providerName, Agent agent) {
        getHarvesterJobs().add(
                new HarvesterJobController<PDALauncher>(providerName
                        + "-" + getType(), ((PDAAgent)agent).getMainScan(),
                        PDALauncher.class));
        statusHandler.handle(Priority.INFO, "Added/Updated " + providerName
                + " Scan entry.");
        
    }

    @Override
    public void launch(String jobName) {
        
        try {
            // Gets them in localization order, Site, Configured, then Base
            // Makes sure local changes get run first before default fall backs.
            for (LocalizationFile lf : Launcher.getLocalizedFiles()) {

                final HarvesterConfig hc = HarvesterConfigurationManager.getHarvesterFile(lf.getFile());

                if (hc.getProvider().getName().equals(providerName)) {
                    if (hc.getAgent() != null) {
                        // we only want PDA type
                        Agent agent = hc.getAgent();

                        if (agent instanceof PDAAgent) {
                            // Launch the PDACatalogHarvester
                            Runnable runner = new Runnable() {
                                @Override
                                public void run() {

                                    statusHandler.info("~~~~~~~~~~~~~~~~~ " + providerName
                                            + " Catalog Harvester ~~~~~~~~~~~~~~~~~~~");
                                    PDACatalogHarvester pch = new PDACatalogHarvester(hc);
                                    boolean status = pch.harvest();
                                    statusHandler.info("Message sent to " + providerName +": success? "+status);
                                }
                            };

                            // execute off main thread
                            runner.run();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "PDA Catalog harvester failed to initialize!", e);
        }
        
    }

    @Override
    public void init() {
        try {
            if (isInitial()) {
                // if many, start many
                List<LocalizationFile> files = Launcher
                        .getLocalizedFiles();

                if (files != null) {
                    for (LocalizationFile lf : files) {

                        HarvesterConfig hc = HarvesterConfigurationManager
                                .getHarvesterFile(lf.getFile());

                        if (hc.getAgent() != null) {
                            // we only want crawler types for CrawlerMetadata
                            Agent agent = hc.getAgent();

                            if (agent instanceof PDAAgent) {
                                // create our quartz beans
                                setProviderName(hc.getProvider().getName());
                                addHarvesterJobs(hc.getProvider().getName(),
                                        (PDAAgent) agent);
                            }
                        }
                    }
                }
            }

            setInitial(false);

        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "PDA Cron Beans failed to initialize!", e);
        }
    }
    
    public List<HarvesterJobController<PDALauncher>> getHarvesterJobs() {
        return harvesterJobs;
    }
    
    @Override
    public String getType() {
        return "pda";
    }

}
