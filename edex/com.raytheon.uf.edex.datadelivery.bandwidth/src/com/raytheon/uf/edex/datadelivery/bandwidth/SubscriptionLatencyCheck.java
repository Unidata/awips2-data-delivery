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
package com.raytheon.uf.edex.datadelivery.bandwidth;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthMap;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthRoute;
import com.raytheon.uf.common.datadelivery.event.retrieval.GenericNotifyEvent;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription.SubscriptionPriority;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataSetHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.SubscriptionHandler;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.ITimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthBucket;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.DataSetLatency;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthBucketDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.hibernate.DataSetLatencyDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalDao;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;

/**
 *
 * This class is is used to examine all of the active subscriptions in the
 * current BandwidthBucket. If a subscription is due to expire (without
 * completing its data retrieval), then a NotificationMessage needs to be sent
 * (placed on the EventBus) to the Notification Dialog that the Subscription has
 * passed its retrieval window and failed to return the requested data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Dec 01, 2014  3550     ccody     Initial creation
 * Mar 09, 2015  4242     dhladky   Data integrity exception caused by dupe key
 *                                  caused gaps in scheduling
 * May 27, 2015  4531     dhladky   Remove excessive Calendar references.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Sep 19, 2017  6415     rjpeter   Fixed calculateNewSleepTime to always be
 *                                  positive
 * Oct 10, 2017  6415     nabowle   Add retrieval latency check. Fix extendedDelayTimeFactor.
 * Oct 25, 2017  6484     tjensen   Merged SubscriptionRetrievals and
 *                                  BandwidthAllocations
 *
 * </pre>
 *
 * @author ccody
 */
public class SubscriptionLatencyCheck<T extends Time, C extends Coverage> {

    /** Status handler (logger) */
    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SubscriptionLatencyCheck.class);

    /** Default extension at 25% */
    protected static final float DEFAULT_EXTENDED_DELAY_FACTOR = .25F;

    protected static final float DEFAULT_RETRIEVAL_EXTENSION = 0.5F;

    protected static float retrievalExtensionFactor = DEFAULT_RETRIEVAL_EXTENSION;

    static {
        String val = System.getProperty(
                "datadelivery.bandwidth.retrieval.expiration.factor");
        if (val != null) {
            try {
                retrievalExtensionFactor = Float.parseFloat(val);
            } catch (NumberFormatException nfe) {
                statusHandler.warn(
                        "datadelivery.bandwidth.retrieval.expiration.factor has been incorrectly configured. Using the default value.");
            }
        }
    }

    /**
     * Subscription Latency Check processing time window. This is how long
     * (Mils) before the end of a Bandwidth Bucket that this check should
     * execute.
     */
    protected static final long PROCESSING_TIME_WINDOW = Long.getLong(
            "datadelivery.bandwidth.latency.processingWindow",
            15 * TimeUtil.MILLIS_PER_SECOND);

    /** Spring may attempt to start this more than once. */
    private volatile boolean started = false;

    /** Bandwidth Bucket Dao */
    protected final IBandwidthBucketDao bucketsDao;

    /** Bandwidth Dao */
    protected final IBandwidthDao<T, C> bandwidthDao;

    protected final RetrievalDao retrievalDao;

    /** Subscription Handler Object */
    protected final SubscriptionHandler subscriptionHandler;

    /** Data Set Handler object */
    protected final DataSetHandler dataSetHandler;

    /** Data Set Latency Handler Object */
    protected final DataSetLatencyDao dataSetLatencyDao;

    protected final Network network;

    private final SubscriptionLatencyCheckProcessor slcp;

    /** Common Simple Date Format */
    protected SimpleDateFormat sdf = new SimpleDateFormat(
            "yyyy-MM-dd_HH:mm:ss.S");

    /** Bandwidth Bucket Size Value from "datadelivery/bandwidthmap.xml" */
    protected long configFileBandwidthBucketLength = 0;

    /**
     * Value to use for Subscription Extended Latency Factor. Loaded from
     * "datadelivery/bandwidthmap.xml" as an integer percentage and divided by
     * 100.0 (25->0.25), or defaulted to 0.25. The new latency will be
     * calculated as <Original Latency> + (<Original Latency> *
     * extendedDelayTimeFactor)
     */
    protected float extendedDelayTimeFactor = DEFAULT_EXTENDED_DELAY_FACTOR;

    public enum SubAllocationStatus {

        UNKNOWN("UNKNOWN"), EXTENDED("EXTENDED"), EXPIRED("EXPIRED");

        private final String statusName;

        private SubAllocationStatus(String name) {
            statusName = name;
        }

        public String getStatus() {
            return statusName;
        }
    }

    /**
     * Create Spring persistent Bean class.
     *
     * @param bucketsDao
     *            Active instance of IBandwidthBucketDao
     * @param bandwidthDao
     *            Active instance of IBandwidthDao
     * @param subscriptionHandler
     *            Active instance of ISubscriptionHandler
     * @param dataSetHandler
     *            Active instance of IDataSetHandler
     * @param dataSetLatencyHandler
     *            Active instance of IDataSetLatencyHandler
     * @param retrievalDao
     *            Active RetrievalDao instance
     * @param network
     *            The network.
     *
     */
    public SubscriptionLatencyCheck(IBandwidthBucketDao bucketsDao,
            IBandwidthDao<T, C> bandwidthDao,
            SubscriptionHandler subscriptionHandler,
            DataSetHandler dataSetHandler, DataSetLatencyDao dataSetLatencyDao,
            RetrievalDao retrievalDao, Network network) {
        this.bucketsDao = bucketsDao;
        this.bandwidthDao = bandwidthDao;
        this.retrievalDao = retrievalDao;
        this.subscriptionHandler = subscriptionHandler;
        this.dataSetHandler = dataSetHandler;
        this.dataSetLatencyDao = dataSetLatencyDao;
        this.network = network;
        this.slcp = new SubscriptionLatencyCheckProcessor(network);
    }

    /**
     * Start the Subscription Latency Check process in its own thread.
     *
     * Allow only 1
     */
    public void start() {

        if (!started) {
            started = true;

            if (checkInitParams()) {
                slcp.start();
            }
        } else {
            statusHandler.warn(
                    "start() has already been called, ignoring further requests!");
        }
    }

    /**
     * Shutdown Subscription Latency Check process.
     *
     */
    public void shutdown() {
        statusHandler.info("shutdown() has been called ("
                + sdf.format(TimeUtil.currentTimeMillis())
                + "), ignoring further requests!");
        if (started) {
            slcp.shutdown();
            started = false;
        }

    }

    /**
     * Check initialization (Spring) parameters before starting.
     *
     * @return boolean true if all parameters have been assigned.
     */
    protected boolean checkInitParams() {

        if (this.bucketsDao == null) {
            statusHandler.error(
                    "ERROR: Unable to perform Subscription Latency Check (Spring Bean). Bandwidth Bucket DAO is null.");
            return (false);
        }

        if (this.bandwidthDao == null) {
            statusHandler.error(
                    "ERROR: Unable to perform Subscription Latency Check (Spring Bean). Bandwidth DAO is null.");
            return (false);
        }

        if (this.subscriptionHandler == null) {
            statusHandler.error(
                    "ERROR: Unable to perform Subscription Latency Check (Spring Bean). Subscription Handler is null.");
            return (false);
        }

        if (this.dataSetHandler == null) {
            statusHandler.error(
                    "ERROR: Unable to perform Subscription Latency Check (Spring Bean). Data Set Meta Data Handler is null.");
            return (false);
        }

        if (this.dataSetLatencyDao == null) {
            statusHandler.error(
                    "ERROR: Unable to perform Subscription Latency Check (Spring Bean). Data Set Latency Dao is null.");
            return (false);
        }

        return (true);
    }

    /**
     * Internal Processing Class.
     *
     * One processing thread is needed for each Network type. Each Network can
     * have its own Bandwidth Bucket Sizes, Times, Durations, etc. For
     * simplicity of timing; each Network will be processed individually.
     *
     */
    private class SubscriptionLatencyCheckProcessor extends Thread {

        private volatile boolean isProcessorShutdown = false;

        /** Bandwidth Bucket (time) length in Mils */
        protected long bandwidthBucketLength = 3 * TimeUtil.MILLIS_PER_MINUTE;

        private final Network threadNetwork;

        /**
         * SubscriptionLatencyCheckProcessor.
         *
         * Perform the Network specific processing of checking Subscription
         * retrieval and extending wait Latency.
         *
         * @param network
         *            The network that this Thread is processing latent
         *            subscriptions for.
         */
        public SubscriptionLatencyCheckProcessor(Network network) {
            super("SubscriptionLatencyCheckProcessor:" + network.name());
            this.threadNetwork = network;
        }

        /**
         * Shutdown Subscription Latency Check Processor thread.
         *
         */
        public void shutdown() {
            statusHandler.info("SubscriptionLatencyCheckProcessor for Network: "
                    + threadNetwork.name() + " shutdown() has been called ("
                    + sdf.format(TimeUtil.currentTimeMillis())
                    + "), ignoring further requests!");
            isProcessorShutdown = true;
        }

        /**
         * Perform process timing initialization.
         *
         * Retrieve values for Bandwidth Bucket start times and Bandwidth Bucket
         * duration. Write an event log entry if the Registry and config values
         * differ.
         *
         */
        protected void initialize() {
            // Get Bandwidth Bucket Gap and Subscription Extension Factor
            // from config datadelivery/bandwidthmap.xml file
            this.bandwidthBucketLength = retrieveBandwidthMapData();

            // Compute value from Registry (bandwidthbucket.bucketstarttime
            // database table)
            long now = TimeUtil.currentTimeMillis();
            BandwidthBucket currentBandwidthBucket = bucketsDao
                    .getBucketContainingTime(now, this.threadNetwork);
            if (currentBandwidthBucket == null) {
                statusHandler
                        .info("Unable to retrieve BandwidthBucket for timestamp "
                                + now + " and Network  " + this.threadNetwork
                                + " Unable to compare Registry Bandwidth Bucket size agaist bandwidthmap.xml definition.");
                return;
            }
            long currentBandwidthBucketTime = currentBandwidthBucket
                    .getBucketStartTime();
            long nextWindow = now + bandwidthBucketLength;
            BandwidthBucket nextBandwidthBucket = bucketsDao
                    .getBucketContainingTime(nextWindow, this.threadNetwork);
            if (nextBandwidthBucket == null) {
                statusHandler
                        .info("Unable to retrieve BandwidthBucket for timestamp "
                                + nextWindow + " (next Bucket) and Network "
                                + this.threadNetwork
                                + " Unable to compare Registry Bandwidth Bucket size agaist bandwidthmap.xml definition.");
                return;
            }
            long nextBandwidthBucketTime = nextBandwidthBucket
                    .getBucketStartTime();

            long registryBandwidthBucketLength = nextBandwidthBucketTime
                    - currentBandwidthBucketTime;
            if (configFileBandwidthBucketLength != registryBandwidthBucketLength) {
                statusHandler
                        .info("Configured (bandwidthmap.xml) Bandwidth Bucket Size ("
                                + configFileBandwidthBucketLength
                                + " mils) differs from persistance record bandwidthbucket.bucketstarttime latency ("
                                + registryBandwidthBucketLength
                                + " mils). SubscriptionLatencyCheck is using ("
                                + registryBandwidthBucketLength + " mils).");
                bandwidthBucketLength = registryBandwidthBucketLength;
            }
        }

        @Override
        public void run() {

            initialize();

            long waitTime = 0;
            // Don't start immediately. Wait until next Bandwidth Bucket.
            try {
                waitTime = calculateNewSleepTime(0);
                statusHandler.debug("Sleeping (" + waitTime
                        + " mils) until next Bandwidth Bucket.");
                Thread.sleep(waitTime);
            } catch (InterruptedException e1) {
                // ignore
            }

            while (!this.isProcessorShutdown) {
                ITimer timer = TimeUtil.getTimer();
                timer.start();

                try {
                    statusHandler.info("Checking subscription latencies...");
                    waitTime = checkSubscriptionLatency();
                } catch (Throwable e) {
                    // so thread doesn't die
                    statusHandler.error(
                            "Unable to perform Subscription Latency Check.", e);
                }
                timer.stop();
                statusHandler.info("Checking subscription latency took "
                        + timer.getElapsedTime() + "ms.");
                try {
                    if ((!this.isProcessorShutdown) && (waitTime >= 0)) {
                        statusHandler.debug("SubscriptionLatencyCheckProcessor "
                                + threadNetwork.name() + " Sleeping ("
                                + waitTime
                                + " mils) until next Bandwidth Bucket.");
                        Thread.sleep(waitTime);

                    }
                } catch (InterruptedException e1) {
                    // ignore
                }
            }
        }

        /**
         * Main processing method.
         *
         * Retrieve and check through all Scheduled/Active Bandwidth Allocation
         * objects for:
         * <p>
         * <list>
         * <li>Allocations that are about to expire (i.e. Scheduled in the
         * Current Bandwidth Bucket but are not scheduled in the next.)</li>
         * </list>
         * <p>
         * For each expiring Bandwidth Allocation:
         * <p>
         * Check to see if Data Set Data has been retrieved for the Allocation.
         * Check to see if its Latency has already been extended. If not:
         * <p>
         * Extend the Latency by 1/4 of its original value and flag the
         * Allocation as having been extended.
         * <p>
         * Update the Subscription (This will in turn trigger the scheduling and
         * Bandwidth Allocation.
         * <p>
         * Send (and store) Notification of the changes.
         * <p>
         *
         * @return Wait time (mils) before starting next execution
         */
        protected long checkSubscriptionLatency() {

            if (this.isProcessorShutdown) {
                return (-1);
            }

            long scheduledNextBandwidthBucketTime;

            // Create a single list of Bandwidth Allocation objects that need to
            // be processed.
            List<BandwidthAllocation> rescheduleBandwidthAllocationList = new ArrayList<>();
            scheduledNextBandwidthBucketTime = buildRescheduleBucketAllocationList(
                    rescheduleBandwidthAllocationList);

            // At this point; we have a list of Bandwidth Allocation objects
            // meeting the following criteria:
            // 1. They are SubscriptionRetieval objects.
            // 2. The Data Set for the Bandwidth Allocation has not arrived
            // (i.e. still in the Current Bandwidth Allocation list)
            // 3. There is not a Bandwidth Allocation in the Next Bandwidth
            // Bucket.

            // Create/Update DataSetLatency objects to extend retrieval time.
            // Scheduler has been modified to check for DataSetLatency objects
            // prior to removing them from Retrieval.
            List<SubscriptionLatencyData> subscriptionLatencyList = buildSubscriptionLatencyData(
                    rescheduleBandwidthAllocationList);

            processSubscriptionLatencyDataList(subscriptionLatencyList);

            // Send messages to Notification for the Subscriptions that have
            // been extended or expired.
            generateAndSendExtendedNotification(subscriptionLatencyList);
            generateAndSendExpiredNotification(subscriptionLatencyList);

            checkRetrievalLatency();

            return calculateNewSleepTime(scheduledNextBandwidthBucketTime);
        }

        /**
         * Build a list of Bandwidth Allocation objects for subscriptions that
         * expire in the current bandwidth bucket.
         *
         * This method ALSO returns the timestamp of the start of the subsequent
         * (the next, next Bandwidth Bucket start time).
         * <p>
         *
         * @param rescheduleBandwidthAllocationList
         *            An empty, but allocated, List to place expiring Bandwidth
         *            Allocation objects into
         * @return Timestamp of the subsequent Bandwidth bucket start time
         */

        protected long buildRescheduleBucketAllocationList(
                List<BandwidthAllocation> rescheduleBandwidthAllocationList) {

            // Set bucket time to a default value.
            long scheduledNextBandwidthBucketTime = TimeUtil.currentTimeMillis()
                    + bandwidthBucketLength;

            // Calculate Bandwidth Bucket Window times
            long now = TimeUtil.currentTimeMillis();
            BandwidthBucket currentBandwidthBucket = bucketsDao
                    .getBucketContainingTime(now, threadNetwork);
            long nextWindow = now + bandwidthBucketLength;
            BandwidthBucket nextBandwidthBucket = bucketsDao
                    .getBucketContainingTime(nextWindow, threadNetwork);
            long oneMore = nextWindow + bandwidthBucketLength;
            BandwidthBucket oneMoreBandwidthBucket = bucketsDao
                    .getBucketContainingTime(oneMore, threadNetwork);
            if ((currentBandwidthBucket == null)
                    || (nextBandwidthBucket == null)
                    || (oneMoreBandwidthBucket == null)) {
                statusHandler
                        .warn("Unable to retrieve necessary Bandwidth Bucket data for Network: "
                                + this.threadNetwork
                                + " Unable to perform Subscription Latency Check. Waiting for next Bandwidth Bucket time window");
                return (scheduledNextBandwidthBucketTime);
            }
            long currentBandwidthBucketTime = currentBandwidthBucket
                    .getBucketStartTime();

            long nextBandwidthBucketTime = nextBandwidthBucket
                    .getBucketStartTime();

            // Save Next Start Bandwidth Bucket Time to schedule next execution
            scheduledNextBandwidthBucketTime = nextBandwidthBucketTime;

            long oneMoreBandwidthBucketTime = oneMoreBandwidthBucket
                    .getBucketStartTime();

            List<Long> bandwidthIdList = new ArrayList<>(3);
            bandwidthIdList.add(new Long(currentBandwidthBucketTime));
            bandwidthIdList.add(new Long(nextBandwidthBucketTime));
            bandwidthIdList.add(new Long(oneMoreBandwidthBucketTime));
            List<BandwidthAllocation> allBandwidthAllocationList = bandwidthDao
                    .getBandwidthAllocations(threadNetwork, bandwidthIdList);

            Map<String, BandwidthAllocation> currentBwBktBandwidthAllocationMap = new HashMap<>();
            Map<String, BandwidthAllocation> nextBwBktBandwidthAllocationMap = new HashMap<>();

            for (BandwidthAllocation bandwidthAllocation : allBandwidthAllocationList) {
                RetrievalStatus retrievalStatus = bandwidthAllocation
                        .getStatus();
                if (RetrievalStatus.SCHEDULED == retrievalStatus) {
                    Long baId = bandwidthAllocation.getIdentifier();
                    long baBandwidthBucket = bandwidthAllocation
                            .getBandwidthBucket();
                    String subscriptionId = bandwidthAllocation.getSubscriptionId();

                    if ((baBandwidthBucket >= currentBandwidthBucketTime)
                            && (baBandwidthBucket < nextBandwidthBucketTime)) {
                        // Allocation is in the current Bandwidth Bucket
                        currentBwBktBandwidthAllocationMap.put(subscriptionId,
                                bandwidthAllocation);
                    } else if ((baBandwidthBucket >= nextBandwidthBucketTime)
                            && (baBandwidthBucket < oneMoreBandwidthBucketTime)) {
                        // Allocation is in the next Bandwidth Bucket
                        nextBwBktBandwidthAllocationMap.put(subscriptionId,
                                bandwidthAllocation);
                    } else if (baBandwidthBucket < currentBandwidthBucketTime) {
                        // This allocation should have been removed.
                        // Record it for tracking purposes
                        statusHandler
                                .info("Bandwidth Allocation: Bandwidth Bucket set earlier than most current Bandwidth Bucket. Id: "
                                        + baId
                                        + "  Assigned Bandwidth Bucket Time: "
                                        + sdf.format(bandwidthAllocation
                                                .getBandwidthBucket())
                                        + "  for Subscription: "
                                        + subscriptionId);
                    } else {
                        // Outside of range. Do nothing.
                    }
                } else {
                    // Not a SubscriptionRetrieval Object. Ignore.
                }
            }

            for (Entry<String, BandwidthAllocation> entry : currentBwBktBandwidthAllocationMap
                    .entrySet()) {

                if (!nextBwBktBandwidthAllocationMap
                        .containsKey(entry.getKey())) {
                    /*
                     * An Allocation in the Current Map does NOT exist in the
                     * Next Bandwidth Allocation Map The Subscription is
                     * expiring in this allocation.
                     */
                    rescheduleBandwidthAllocationList.add(entry.getValue());
                }
            }

            return (scheduledNextBandwidthBucketTime);
        }

        /**
         * Build Subscription Latency Data List.
         *
         * Build a list of Subscription Latency Data objects to evaluate whether
         * to extend or expire Subscriptions.
         *
         * @param rescheduleBandwidthAllocationList
         *            A List of relevant BandwidthAllocation objects to evaluate
         * @return List of SubscriptionLatencyData objects
         */
        protected List<SubscriptionLatencyData> buildSubscriptionLatencyData(
                List<BandwidthAllocation> rescheduleBandwidthAllocationList) {

            if ((rescheduleBandwidthAllocationList == null)
                    || (rescheduleBandwidthAllocationList.isEmpty())) {
                return (null);
            }

            List<SubscriptionLatencyData> subLatencyDataList = new ArrayList<>(
                    rescheduleBandwidthAllocationList.size());

            for (BandwidthAllocation bandwidthAllocation : rescheduleBandwidthAllocationList) {

                SubscriptionLatencyData subscriptionLatencyData = new SubscriptionLatencyData();
                subLatencyDataList.add(subscriptionLatencyData);

                subscriptionLatencyData.setSubscriptionLatency(
                        bandwidthAllocation.getSubscriptionLatency());

                long baseRefTimeLong = bandwidthAllocation
                        .getBaseReferenceTime().getTime();
                subscriptionLatencyData.setBaseRefTimestamp(baseRefTimeLong);

                String dataSetName = null;
                String dataSetProviderName = null;
                String subscriptionName = null;
                try {
                    Subscription sub = subscriptionHandler
                            .getById(bandwidthAllocation.getSubscriptionId());
                    subscriptionName = sub.getName();
                    dataSetName = sub.getDataSetName();
                    dataSetProviderName = sub.getProvider();
                } catch (RegistryHandlerException e) {
                    statusHandler.error("Unable to query Subscription with id: "
                            + bandwidthAllocation.getSubscriptionId(), e);
                    continue;
                }
                subscriptionLatencyData.setSubscriptionName(subscriptionName);
                subscriptionLatencyData.setDataSetName(dataSetName);
                subscriptionLatencyData
                        .setDataSetProviderName(dataSetProviderName);
                try {
                    DataSet<T, C> dataSet = dataSetHandler.getByNameAndProvider(
                            dataSetName, dataSetProviderName);
                    subscriptionLatencyData.availabilityOffset = dataSet
                            .getAvailabilityOffset();
                    DataSetLatency dataSetLatency = null;
                    dataSetLatency = dataSetLatencyDao
                            .getByDataSetNameAndProvider(dataSetName,
                                    dataSetProviderName);
                    subscriptionLatencyData.setDataSetLatency(dataSetLatency);
                } catch (RegistryHandlerException rhe) {
                    statusHandler.error("Unable to query DataSet Name: "
                            + dataSetName + " Provider: " + dataSetProviderName,
                            rhe);
                }
            }

            return (subLatencyDataList);
        }

        /**
         * Evaluate Subscription latency times of Data Set Latency objects to
         * determine whether a Subscription should be extended or allowed to
         * expire.
         *
         * @param subLatencyDataList
         *            A List of relevant SubscriptionLatencyData objects to
         *            evaluate
         */
        protected void processSubscriptionLatencyDataList(
                List<SubscriptionLatencyData> subLatencyDataList) {

            if ((subLatencyDataList == null)
                    || (subLatencyDataList.isEmpty())) {
                return;
            }

            long now = TimeUtil.currentTimeMillis();

            for (SubscriptionLatencyData subLatencyData : subLatencyDataList) {
                processSubscriptionLatencyData(now, subLatencyData);
            }
        }

        /**
         * Check the given subscription to see if it should be expired or
         * extended. The latency of a subscription is extended (and its
         * scheduled BandwidthAllocation scheduling extended if the following
         * conditions are met:
         * <p>
         * 1. A BandwidthAllociation for the Subscription exists in the Current
         * BandwidthBucket, but not in the Next Bandwidth Bucket.
         * <p>
         * 2. The Data Set Meta Data for the Subscription for the current set of
         * BandwidthAllocations have been received.
         * <p>
         * 3. The Subscription Latency has not already been extended for the
         * current set of BandwidthAllocations.
         * <p>
         * <p>
         * If these criteria are met:
         * <p>
         * 1. An extended latency is computed (usually 25% increase in the
         * original Subscription Latency).
         * <p>
         * 2. A Notification Message of "SubscriptionExtended" is dispatched.
         * <p>
         * 3. The BandwidthManager will extend the latency of the Subscription.
         * <p>
         * 4. A Notification Message of "SubscriptionExpired" is dispatched for
         * Subscriptions that will be expired.
         *
         * @param now
         *            A single System Timestamp to use
         * @param subLatencyData
         *            The SubscriptionLatencyData object to be evaluated for
         *            latency.
         */
        protected void processSubscriptionLatencyData(long now,
                SubscriptionLatencyData subLatencyData) {
            DataSetLatency dataSetLatency = subLatencyData.getDataSetLatency();
            if (dataSetLatency != null) {
                // Data Set has been extended before.
                // Determine if it was it recent enough to be relevant
                long dataSetLatencyBaseRefTimestamp = dataSetLatency
                        .getBaseRefTimestamp();
                long bandwidthAllocationBaseRefTime = subLatencyData
                        .getBaseRefTimestamp();

                // If the Base Ref Timestamp of the Data Set Latency is before
                // the Base Reference Time of the Current Bandwidth Allocation;
                // then the Data Set Latency object refers to a prior
                // allocation.
                // This Data Set Latency data is not referencing THIS
                // allocation.
                if (dataSetLatencyBaseRefTimestamp < bandwidthAllocationBaseRefTime) {
                    // This can be extended.
                    subLatencyData.setSubAllocationStatus(
                            SubAllocationStatus.EXTENDED);
                } else {
                    // This can NOT be extended.
                    subLatencyData.setSubAllocationStatus(
                            SubAllocationStatus.EXPIRED);
                }
            } else {
                // There is no DataSetLatency data for the Data Set
                // for the BandwidthAllocation object's Subscription.
                // This can be extended.
                subLatencyData
                        .setSubAllocationStatus(SubAllocationStatus.EXTENDED);
            }

            if (subLatencyData
                    .getSubAllocationStatus() == SubAllocationStatus.EXTENDED) {
                dataSetLatency = getDataSetLatency(now, subLatencyData);
                // need transaction safety here
                if (dataSetLatency != null) {
                    DataSetLatency dataSetLatencyOld = dataSetLatencyDao
                            .getByDataSetNameAndProvider(
                                    dataSetLatency.getDataSetName(),
                                    dataSetLatency.getProviderName());
                    if (dataSetLatencyOld != null) {
                        dataSetLatencyDao.delete(dataSetLatencyOld);
                    }

                    dataSetLatencyDao.create(dataSetLatency);
                }
            } else {
                // Delete the existing DataSetLatency record
                dataSetLatencyDao.delete(dataSetLatency);
            }
        }

        /**
         * Build DataSetLatency from SubscriptionLatencyData data.
         *
         * @param now
         *            GMT System time
         * @param subLatencyData
         *            Internal data holder object
         * @return DataSetLatency new hibernate object
         */
        protected DataSetLatency getDataSetLatency(long now,
                SubscriptionLatencyData subLatencyData) {

            DataSetLatency dataSetLatency = subLatencyData.getDataSetLatency();

            if (dataSetLatency == null) {
                dataSetLatency = new DataSetLatency();
                dataSetLatency.setDataSetName(subLatencyData.getDataSetName());
                dataSetLatency.setProviderName(
                        subLatencyData.getDataSetProviderName());
                subLatencyData.setDataSetLatency(dataSetLatency);
            }
            // Set a NEW Base Ref Timestamp (to refer to the current set of
            // BandwidthAllocations for the Subscription
            dataSetLatency
                    .setBaseRefTimestamp(subLatencyData.getBaseRefTimestamp());

            // Calculate the new extended Latency
            long oldLatencyMilsL = subLatencyData.getSubscriptionLatency()
                    * TimeUtil.MILLIS_PER_MINUTE;
            int oldLatencyMils = (int) oldLatencyMilsL;

            int additionalWaitMils = Math
                    .round(oldLatencyMils * extendedDelayTimeFactor);
            int newLatencyMils = oldLatencyMils + additionalWaitMils;
            long newLatencyMinL = newLatencyMils / TimeUtil.MILLIS_PER_MINUTE;
            int newLatencyMin = (int) newLatencyMinL;
            dataSetLatency.setExtendedLatency(newLatencyMin);

            return (dataSetLatency);
        }

        /**
         * Generate and send notification message for all Subscriptions with
         * extended latencies. Send 1 notification for all Subscriptions
         * extended for the current Bandwidth Bucket.
         *
         * @param subLatencyDataList
         *            List of SubscriptionLatencyData objects
         */
        protected void generateAndSendExtendedNotification(
                List<SubscriptionLatencyData> subLatencyDataList) {
            if ((subLatencyDataList == null)
                    || (subLatencyDataList.isEmpty())) {
                return;
            }

            boolean doSendComment = false;
            StringBuilder sb = new StringBuilder();
            sb.append(
                    "The Latency time for the following Subscriptions has been extended:");
            for (SubscriptionLatencyData subLatencyData : subLatencyDataList) {
                if (subLatencyData
                        .getSubAllocationStatus() == SubAllocationStatus.EXTENDED) {
                    doSendComment = true;
                    sb.append("\n\t");
                    sb.append(subLatencyData.getSubscriptionName());
                    sb.append(" New Latency: ");
                    sb.append(subLatencyData.getSubscriptionLatency());
                    sb.append(" Data Set Name: ");
                    sb.append(subLatencyData.getDataSetName());
                    sb.append(" Provider: ");
                    sb.append(subLatencyData.getDataSetProviderName());
                    long availabilityOffset = subLatencyData
                            .getAvailabilityOffset();
                    long oldLatencyMils = subLatencyData
                            .getSubscriptionLatency()
                            * TimeUtil.MILLIS_PER_MINUTE;
                    if (availabilityOffset > oldLatencyMils) {
                        // The Data Set Availability offset is GREATER than the
                        // Subscription Latency.
                        sb.append("\n\t\tData Set Availability Offset : ");
                        sb.append(TimeUtil.prettyDuration(availabilityOffset));
                        sb.append(" is greater than Subscription Latency: ");
                        sb.append(TimeUtil.prettyDuration(oldLatencyMils));
                    }
                }
                sb.append(
                        "\nData Set Meta Data for these Subscriptions has been received, but the corresponding data is still missing.");
            }

            if (doSendComment) {
                sendNotification("SubscriptionExtended", sb.toString());
            }
        }

        /**
         * Generate and send notification message for all Subscriptions with
         * expiring latencies. Send 1 notification for all Subscriptions that
         * will expire at the end of the current Bandwidth Bucket.
         *
         * @param subLatencyDataList
         *            List of SubscriptionLatencyData objects.
         */
        protected void generateAndSendExpiredNotification(
                List<SubscriptionLatencyData> subLatencyDataList) {
            if ((subLatencyDataList == null)
                    || (subLatencyDataList.isEmpty())) {
                return;
            }

            boolean doSendComment = false;
            StringBuilder sb = new StringBuilder();
            sb.append(
                    "The extended Latency time for the following Subscriptions has expired:");
            for (SubscriptionLatencyData subLatencyData : subLatencyDataList) {
                if (subLatencyData
                        .getSubAllocationStatus() == SubAllocationStatus.EXPIRED) {
                    doSendComment = true;
                    sb.append("\n\t");
                    sb.append(subLatencyData.getSubscriptionName());
                    sb.append(" Data Set Name: ");
                    sb.append(subLatencyData.getDataSetName());
                    sb.append(" Provider: ");
                    sb.append(subLatencyData.getDataSetName());
                    long availabilityOffset = subLatencyData
                            .getAvailabilityOffset();
                    long oldLatencyMils = subLatencyData
                            .getSubscriptionLatency()
                            * TimeUtil.MILLIS_PER_MINUTE;
                    if (availabilityOffset > oldLatencyMils) {
                        // The Data Set Availability offset is GREATER than the
                        // Subscription Latency.
                        sb.append("\n\t\tData Set Availability Offset : ");
                        sb.append(TimeUtil.prettyDuration(availabilityOffset));
                        sb.append(" is greater than Subscription Latency: ");
                        sb.append(TimeUtil.prettyDuration(oldLatencyMils));
                    }
                }
                sb.append(
                        "\nData Set Meta Data for these Subscriptions has been received, but the corresponding data is still missing.");
            }

            if (doSendComment) {
                sendNotification("SubscriptionExpired", sb.toString());
            }
        }

        /**
         * Check for pending Retrievals that have exceeded their
         * latencyExpireTime, updating their priority or failing them, as
         * appropriate.
         */
        private void checkRetrievalLatency() {
            List<RetrievalRequestRecord> expiredRetrievals;
            try {
                expiredRetrievals = retrievalDao
                        .getExpiredSubscriptionRetrievals();
                List<RetrievalRequestRecord> extensions = new ArrayList<>();
                List<RetrievalRequestRecord> failures = new ArrayList<>();
                for (RetrievalRequestRecord rrr : expiredRetrievals) {
                    /*
                     * If the retrieval has expired, lower its priority. If the
                     * priority drops too low, mark the retrieval as failed.
                     * Otherwise, increase the expiration time and update the
                     * retrieval with the new priority and time.
                     *
                     * The priorities are extended beyond the normal priority
                     * levels so that even the lowest priority retrievals have a
                     * second window of opportunity to be fulfilled.
                     */
                    int priority = rrr.getPriority() + 1;
                    boolean extend = priority <= SubscriptionPriority
                            .values().length + 1;

                    if (extend) {
                        int expireTimeout = rrr.getLatencyMinutes();
                        expireTimeout *= retrievalExtensionFactor;
                        Calendar expireCal = TimeUtil
                                .newGmtCalendar(rrr.getLatencyExpireTime());
                        expireCal.add(Calendar.MINUTE, expireTimeout);
                        rrr.setLatencyExpireTime(expireCal.getTime());
                        rrr.setPriority(priority);
                    } else {
                        rrr.setState(RetrievalRequestRecord.State.FAILED);
                    }
                    try {
                        retrievalDao.update(rrr);
                        if (extend) {
                            extensions.add(rrr);
                        } else {
                            failures.add(rrr);
                        }
                    } catch (Exception e) {
                        statusHandler.warn(
                                "Unable to update an expired retrieval.", e);
                    }
                }

                sendRetrievalNotifications(true, extensions);
                sendRetrievalNotifications(false, failures);
            } catch (DataAccessLayerException e) {
                statusHandler.warn("Unable to check for expired retrievals.",
                        e);
            }
        }

        private void sendRetrievalNotifications(boolean extended,
                List<RetrievalRequestRecord> retrievals) {
            if (retrievals == null || retrievals.isEmpty()) {
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm zzz");

            /*
             * Specific retrieval url's aren't known until the provider-specific
             * request is built. This causes retrievals for different parameters
             * for the same DSMD url to generate the same notification message.
             * Only one of each duplicative message will be included in the
             * notification.
             */
            StringBuilder sb = new StringBuilder()
                    .append("The following Subscription Retrievals have ")
                    .append(extended ? "been extended" : "expired").append(":");
            Set<String> retrievalSet = new HashSet<>();
            for (RetrievalRequestRecord rrr : retrievals) {
                StringBuilder rsb = new StringBuilder().append("\n\t")
                        .append(rrr.getSubscriptionName())
                        .append(" Data Set Name: ").append(rrr.getDataSetName())
                        .append(" Provider: ").append(rrr.getProvider());
                if (extended) {
                    rsb.append(" New Expiration Time: ")
                            .append(sdf.format(rrr.getLatencyExpireTime()))
                            .append(" New Priority: ")
                            .append(rrr.getPriority());
                } else {
                    rsb.append(" Expired At: ")
                            .append(sdf.format(rrr.getLatencyExpireTime()));
                }
                retrievalSet.add(rsb.toString());
            }
            for (String s : retrievalSet) {
                sb.append(s);
            }
            sendNotification(
                    extended ? "RetrievalExtended" : "RetrievalExpired",
                    sb.toString());
        }

        /**
         * Retrieve BandwidthMap file data.
         *
         * Retrieve Bandwidth Bucket Size and Subscription Latency Extend factor
         * (percentage) from "datadelivery/bandwidthmap.xml" file. Class
         * attributes configFileBandwidthBucketLength and
         * extendedDelayTimeFactor are also retrieved from the BandwidthMap
         * file.
         *
         * @return Bandwidth Update Latency in milliseconds
         */
        protected long retrieveBandwidthMapData() {
            // Default Bandwidth Bucket size
            long bucketSizeMils = 3 * TimeUtil.MILLIS_PER_MINUTE;
            long bucketSizeMinutes = 0;

            // Default extension at 25%
            float extendedDelayFactor = DEFAULT_EXTENDED_DELAY_FACTOR;

            IPathManager pm = PathManagerFactory.getPathManager();
            File bandwidthFile = pm
                    .getStaticFile("datadelivery/bandwidthmap.xml");
            if (bandwidthFile != null) {

                BandwidthMap copyOfBandwidthMap = BandwidthMap
                        .load(bandwidthFile);
                if (copyOfBandwidthMap != null) {
                    // Retrieve BucketSizeMils
                    BandwidthRoute route = copyOfBandwidthMap
                            .getRoute(this.threadNetwork);
                    if (route != null) {
                        bucketSizeMinutes = route.getBucketSizeMinutes();
                        if (bucketSizeMinutes > 0) {
                            bucketSizeMils = bucketSizeMinutes
                                    * TimeUtil.MILLIS_PER_MINUTE;
                        }

                        configFileBandwidthBucketLength = bucketSizeMils;
                    }

                    int tempExtendedDelayFactor = copyOfBandwidthMap
                            .getExtendedLatencyFactor();
                    if (tempExtendedDelayFactor > 0) {
                        extendedDelayFactor = tempExtendedDelayFactor / 100.0F;
                    }
                    extendedDelayTimeFactor = extendedDelayFactor;
                }
            }

            return (bucketSizeMils);
        }

        /**
         * Calculate how long this process should sleep.
         *
         * Ideally this (Spring started) process sleeps until
         * PROCESSING_TIME_WINDOW before the next:
         * <p>
         * Bandwidth Bucket Start time + Bandwidth Bucket Length is reached. The
         * time between "now" and 15 seconds before the start of the next
         * Bandwidth Bucket window is computed dynamically.
         *
         * @param nextBandwidthBucketStartTime
         *            System start time of the Next Bandwidth Bucket
         * @return Number of mils to sleep until the start of the next
         *         processing cycle.
         */
        protected long calculateNewSleepTime(
                long nextBandwidthBucketStartTime) {
            long now = TimeUtil.currentTimeMillis();
            long newSleepTimeMillis = this.bandwidthBucketLength;

            // advance nextBandwidthBucketStartTime if necessary
            if (nextBandwidthBucketStartTime < now) {
                long additionalBuckets = ((now - nextBandwidthBucketStartTime)
                        / bandwidthBucketLength) + 1;
                nextBandwidthBucketStartTime += additionalBuckets
                        * bandwidthBucketLength;
            }

            newSleepTimeMillis = nextBandwidthBucketStartTime - now
                    - PROCESSING_TIME_WINDOW;

            // common use case, usually takes less than
            // PROCESSING_TIME_WINDOW to do the latency check
            while (newSleepTimeMillis <= 0) {
                newSleepTimeMillis += bandwidthBucketLength;
            }

            return newSleepTimeMillis;

        }

        /**
         * Send Event to create (and store) a NotificationRecord containing
         * information about which Subscriptions have been Extended or Expired.
         * <p>
         * Note: Spring and hibernate balk when attempting to store the
         * NotificationRecord directly.
         * <p>
         *
         * @param category
         *            "SubscriptionExtended" or "SubscriptionExpired"
         * @param notificationString
         *            Subscription Detail data
         */
        protected void sendNotification(String category,
                String notificationString) {

            GenericNotifyEvent genericNotifyEvent = new GenericNotifyEvent(
                    notificationString);
            genericNotifyEvent.setCategory(category);
            genericNotifyEvent.setPriority(3);
            genericNotifyEvent.setOwner("System");

            // Put notification on the EventBus
            EventBus.publish(genericNotifyEvent);
        }

    }

    /**
     * Internal data class.
     *
     * Amalgamated: BandwidthAllocation, RetrievalSubscription, Subscription,
     * DataSet, and DataSetLatency data.
     */
    protected class SubscriptionLatencyData {
        // From BandwidthAllocation/BandwidthSubscription
        protected String subscriptionName;

        protected long baseRefTime;

        protected int subscriptionLatency;

        // From Data Set
        protected String dataSetName;

        protected String dataSetProviderName;

        protected int availabilityOffset;

        protected SubAllocationStatus subAllocationStatus;

        // Data Set Latency object (if one exists)
        protected DataSetLatency dataSetLatency;

        /**
         * Default constructor.
         */
        public SubscriptionLatencyData() {
            dataSetLatency = null;
            subAllocationStatus = SubAllocationStatus.UNKNOWN;
            subscriptionName = null;
            baseRefTime = 0;
            subscriptionLatency = 0;
            dataSetName = null;
            dataSetProviderName = null;
        }

        /**
         * Get subscriptionName value.
         *
         * Derived from BandwidthAllocation/BandwidthSubscription
         *
         * @return subscriptionName
         */
        public String getSubscriptionName() {
            return (subscriptionName);
        }

        /**
         * Set subscriptionName value.
         *
         * Derived from BandwidthAllocation/BandwidthSubscription
         *
         * @param value
         */
        public void setSubscriptionName(String value) {
            subscriptionName = value;
        }

        /**
         * Get baseRefTime value.
         *
         * Derived from BandwidthAllocation/BandwidthSubscription
         *
         * @return baseRefTime
         */
        public long getBaseRefTimestamp() {
            return (baseRefTime);
        }

        /**
         * Set baseRefTime value.
         *
         * Derived from BandwidthAllocation/BandwidthSubscription
         *
         * @param value
         */
        public void setBaseRefTimestamp(long value) {
            baseRefTime = value;
        }

        /**
         * Get subscriptionLatency value in Minutes.
         *
         * Derived from BandwidthAllocation/BandwidthSubscription
         *
         * @return subscriptionLatency
         */
        public int getSubscriptionLatency() {
            return (subscriptionLatency);
        }

        /**
         * Set subscriptionLatency value (in Minutes).
         *
         * Derived from BandwidthAllocation/BandwidthSubscription
         *
         * @param value
         */
        public void setSubscriptionLatency(int value) {
            subscriptionLatency = value;
        }

        /**
         * Get dataSetName value.
         *
         * Derived from DataSet
         *
         * @return dataSetName
         */
        public String getDataSetName() {
            return (dataSetName);
        }

        /**
         * Set dataSetName value.
         *
         * Derived from DataSet
         *
         * @param value
         */
        public void setDataSetName(String value) {
            dataSetName = value;
        }

        /**
         * Get dataSetProviderName value.
         *
         * Derived from DataSet
         *
         * @return dataSetProviderName
         */
        public String getDataSetProviderName() {
            return (dataSetProviderName);
        }

        /**
         * Set dataSetProviderName value.
         *
         * Derived from DataSet
         *
         * @param value
         */
        public void setDataSetProviderName(String value) {
            dataSetProviderName = value;
        }

        /**
         * Get availabilityOffset value.
         *
         * Derived from DataSet
         *
         * @return availabilityOffset
         */
        public int getAvailabilityOffset() {
            return (availabilityOffset);
        }

        /**
         * Set availabilityOffset value.
         *
         * Derived from DataSet
         *
         * @param value
         */
        public void setDataSetProviderName(int value) {
            availabilityOffset = value;
        }

        /**
         * Get subAllocationStatus value.
         *
         * Derived from DataSetLatency
         *
         * @return subAllocationStatus
         */
        public SubAllocationStatus getSubAllocationStatus() {
            return (subAllocationStatus);
        }

        /**
         * Set subAllocationStatus value.
         *
         * Derived from DataSetLatency
         *
         * @param value
         */
        public void setSubAllocationStatus(SubAllocationStatus value) {
            subAllocationStatus = value;
        }

        /**
         * Get dataSetLatency value.
         *
         * Derived from DataSetLatency
         *
         * @return dataSetLatency
         */
        public DataSetLatency getDataSetLatency() {
            return (dataSetLatency);
        }

        /**
         * Set dataSetLatency value.
         *
         * Derived from DataSetLatency
         *
         * @param value
         */
        public void setDataSetLatency(DataSetLatency value) {
            dataSetLatency = value;
        }
    }

}
