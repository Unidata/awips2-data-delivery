package com.raytheon.uf.edex.datadelivery.bandwidth.registry;

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

import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.io.NetworkTrafficListener;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkTrafficServerConnector;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthMap;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthRoute;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthBucketDao;
import com.raytheon.uf.edex.datadelivery.retrieval.handlers.IBandwidthChangedCallback;
import com.raytheon.uf.edex.registry.ebxml.web.RegistryWebServer;

/**
 * {@link RegistryBandwidthUtilizationListener} Keeps track of network traffic
 * for registry
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 06, 2013 1736       dhladky     Initial creation
 * 6/5/2014     1712       bphillip    Changed registry Jetty server class
 * Dec 01, 2015 5152       nabowle     Use custom NetworkTrafficSelectChannelConnector.
 * Jun 12, 2017 6222       tgurney     Add bandwidthChangedCallback
 * Nov 30, 2017 6529       tgurney     jetty 9.3 upgrade fix
 * 
 * </pre>
 * 
 * @author dhladky
 */

public class RegistryBandwidthUtilizationListener implements NetworkTrafficListener {

    private static final Logger logger = LoggerFactory
            .getLogger(RegistryBandwidthUtilizationListener.class);

    /* Is this registry federated or not */
    private boolean isFederated = false;

    /* Total bytes collected since last run */
    private AtomicInteger totalBytes = new AtomicInteger(0);

    /* The millis of the last run */
    private Long lastRun;

    /** network for data traffic  */
    private Network network = Network.OPSNET;

    /** size of bucket in minutes */
    private int bucketSize = 0;

    /** bucket dao class */
    private IBandwidthBucketDao bucketDao;

    /**
     * Construct the listener
     * @param server
     * @param isFederated
     * @param BandwidthMap
     */
    public RegistryBandwidthUtilizationListener(RegistryWebServer server,
            Boolean isFederated, BandwidthMap map,
            IBandwidthBucketDao bucketDao,
            IBandwidthChangedCallback bandwidthChangedCallback) {

        // We only care about OPSNET in this listener
        this.setFederated(isFederated);
        this.lastRun = TimeUtil.currentTimeMillis();
        BandwidthRoute route = map.getRoute(network);
        bandwidthChangedCallback.bandwidthChanged(route.getDefaultBandwidth(),
                network);
        this.bucketSize = route.getBucketSizeMinutes();
        this.bucketDao = bucketDao;
        String cron = getCronString(route.getBucketSizeMinutes());
        createQuartzCron(cron, network.name());

        for (Connector connector: server.getJettyServer().getConnectors()) {
            if (connector instanceof NetworkTrafficServerConnector) {
                NetworkTrafficServerConnector nconnector = ((NetworkTrafficServerConnector) connector);
                nconnector.addNetworkTrafficListener(this);
                logger.debug(nconnector.toString() + " on Network: " + network);
            }
        }
    }


    @Override
    public void opened(Socket socket) {
        logger.debug("Socket Open!");
    }

    @Override
    public void incoming(Socket socket, ByteBuffer bytes) {

        // Ignore local traffic if federated
        if (isFilteredTraffic(socket)) {
            return;
        }

        // add bytes to total
        addBytes(bytes);
    }

    @Override
    public void outgoing(Socket socket, ByteBuffer bytes) {

        // Ignore local traffic if federated
        if (isFilteredTraffic(socket)) {
            return;
        }

        // add bytes to total
        addBytes(bytes);
    }

    @Override
    public void closed(Socket socket) {
        logger.debug("Socket Closed!");
    }

    /**
     * Add the bytes
     * @param bytes
     */
    private void addBytes(ByteBuffer bytes) {
        totalBytes.getAndAdd(bytes.remaining());
    }


    public boolean isFederated() {
        return isFederated;
    }


    public void setFederated(boolean isFederated) {
        this.isFederated = isFederated;
    }

    /**
     * Filter un-needed traffic
     * @param socket
     * @return
     */
    private boolean isFilteredTraffic(Socket socket) {

        if (isFederated()) {

            InetAddress address = socket.getInetAddress();

            if (address.isLoopbackAddress()) {
                // ignore this traffic
                return true;
            }
        }

        return false;
    }

    /**
     * Get the string to submit as the cron
     * @param minutes
     * @return
     */
    private String getCronString(int minutes) {
        return new String("0 0/"+minutes+" * * * ?");
    }

    /**
     * Creates a dynamic quartz cron for the Bandwidth Utilization based on the
     * size of the Bandwidth Bucket in minutes.
     * @param cron
     * @param name
     */
    private void createQuartzCron(String cron, String name) {

        try {

            SchedulerFactory schedFactory = new org.quartz.impl.StdSchedulerFactory();
            Scheduler schedular = schedFactory.getScheduler();
            JobDetail jobDetail = null;

            try {
                jobDetail = schedular.getJobDetail(name, "BandwidthUtilization");
            } catch (SchedulerException se) {
                logger.info("Job doesn't exist!", se);
            }

            if (jobDetail != null) {
                // reschedule
                CronTrigger trigger = (CronTrigger) schedular.getTrigger(name,
                        "BandwidthUtilization");
                String cronEx = trigger.getCronExpression();
                if (!cron.equals(cronEx)) {
                    trigger.setCronExpression(cron);
                    schedular.rescheduleJob(name, "BandwidthUtilization", trigger);
                    logger.info("Rescheduling Job: " + name);
                }
            } else {
                jobDetail = new JobDetail(name, "BandwidthUtilization", BandwidthUtilizationProcessor.class);
                jobDetail.getJobDataMap().put(name, "FULL");
                // add the atomic int to the map
                jobDetail.getJobDataMap().put("totalBytes", totalBytes);
                jobDetail.getJobDataMap().put("network", network);
                jobDetail.getJobDataMap().put("lastRun", lastRun);
                jobDetail.getJobDataMap().put("bucketDao", bucketDao);
                jobDetail.getJobDataMap().put("bucketSize", bucketSize);
                CronTrigger trigger = new CronTrigger(name, "BandwidthUtilization");
                trigger.setCronExpression(cron);
                schedular.scheduleJob(jobDetail, trigger);
                logger.info("Scheduling Job: " + name);
            }

            if (!schedular.isStarted()) {
                schedular.start();
            }

        } catch (Exception e) {
            logger.error("Unable to schedule job: " + name + " error: "
                    + e);
        }
    }

}
