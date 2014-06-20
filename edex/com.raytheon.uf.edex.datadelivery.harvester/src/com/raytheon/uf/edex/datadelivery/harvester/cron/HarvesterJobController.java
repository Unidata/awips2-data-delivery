package com.raytheon.uf.edex.datadelivery.harvester.cron;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.datadelivery.harvester.Launcher;

/**
 * Harvester Job for Quartz in code.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 04, 2012 1038       dhladky     Initial creation
 * May 20, 2013 2000       djohnson    Scheduler must now be started since quartz upgrade.
 * Jun 14, 2014 3120       dhladky     Made more abstract
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class HarvesterJobController<T extends Launcher> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(HarvesterJobController.class);

    private Class<T> clazz;

    private String name;

    public HarvesterJobController(String name, String cron, Class<T> clazz) {

        try {
            setClazz(clazz);
            setName(name);
            SchedulerFactory schedFactory = new org.quartz.impl.StdSchedulerFactory();
            Scheduler schedular = schedFactory.getScheduler();
            // get rid of any previous jobs
            // schedular.deleteJob(name, "Crawler");
            JobDetail jobDetail = null;
            try {
                jobDetail = schedular.getJobDetail(name, "Launcher");
            } catch (SchedulerException se) {
                statusHandler.info("Job doesn't exist!");
            }

            if (jobDetail != null) {
                // reschedule
                CronTrigger trigger = (CronTrigger) schedular.getTrigger(name,
                        "Launcher");
                String cronEx = trigger.getCronExpression();
                if (!cron.equals(cronEx)) {
                    trigger.setCronExpression(cron);
                    schedular.rescheduleJob(name, "Launcher", trigger);
                    statusHandler.info("Rescheduling Job: " + name);
                }
            } else {
                jobDetail = new JobDetail(name, "Launcher", clazz);
                jobDetail.getJobDataMap().put(name, "FULL");
                CronTrigger trigger = new CronTrigger(name, "Launcher");
                trigger.setCronExpression(cron);
                schedular.scheduleJob(jobDetail, trigger);
            }

            if (!schedular.isStarted()) {
                schedular.start();
            }

        } catch (Exception e) {
            statusHandler.error("Unable to schedule job: " + name + " error: "
                    + e.getMessage());
        }
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    public void setClazz(Class<T> clazz) {
        this.clazz = clazz;
    }

    public void setName(String name) {
        this.name = name;
    }

}
