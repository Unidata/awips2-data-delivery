package com.raytheon.uf.edex.datadelivery.harvester;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.raytheon.uf.common.datadelivery.harvester.Agent;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Abstract Launcher, used to launch harvester jobs for Data Delivery
 * Abstracted this out From the CrawlLauncher now that we have more than 1 
 * harvester at central registry that needs launching.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 14, 2014 3120      dhladky      Abstract "Launcher", abstracted out from original CrawlLauncher
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public abstract class Launcher implements Job {

    protected String providerName;

    private boolean initial = true;

    public abstract void addHarvesterJobs(String providername, Agent agent);

    public abstract void launch(String jobName);

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(Launcher.class);

    /**
     * Gets site and base level configs
     * 
     * @return
     */
    public static List<LocalizationFile> getLocalizedFiles() {
        // first get the Localization directory and find all harvester
        // configs
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext lcConfigured = pm.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.SITE);

        IPathManager pm2 = PathManagerFactory.getPathManager();
        LocalizationContext lcBase = pm2.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.BASE);

        LocalizationFile[] sitefiles = pm.listFiles(lcConfigured,
                "datadelivery/harvester", new String[] { "xml" }, false, true);

        LocalizationFile[] baseFiles = pm2.listFiles(lcBase,
                "datadelivery/harvester", new String[] { "xml" }, false, true);

        ArrayList<LocalizationFile> files = new ArrayList<LocalizationFile>();
        // collect files where there is an existing configured variation
        HashMap<String, LocalizationFile> fileMap = new HashMap<String, LocalizationFile>();

        for (LocalizationFile file : sitefiles) {
            fileMap.put(file.getName(), file);
        }

        // compare to base files
        for (LocalizationFile file : baseFiles) {
            String baseKey = file.getName();
            if (!fileMap.keySet().contains(baseKey)) {
                files.add(file);
            }
        }

        // add the configured to the base list
        for (Entry<String, LocalizationFile> entry : fileMap.entrySet()) {
            files.add(entry.getValue());
        }

        return files;
    }

    /**
     * execute the job
     */
    @Override
    public void execute(JobExecutionContext arg0) throws JobExecutionException {

        final String originalThreadName = Thread.currentThread().getName();

        try {
            Thread.currentThread().setName(
                    "launcherThreadPool-[" + originalThreadName + "]");
            String jobName = arg0.getJobDetail().getName().split("-")[0];
            launch(jobName);

        } catch (Exception e) {
            statusHandler
                    .error("Incorrect naming for job:  Should be [ProviderName-Type] ex. [SAMPLE-main].",
                            e);
        } finally {
            Thread.currentThread().setName(originalThreadName);
        }

    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    
    public abstract String getType();
        
    public abstract void init();

    public boolean isInitial() {
        return initial;
    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }

}

