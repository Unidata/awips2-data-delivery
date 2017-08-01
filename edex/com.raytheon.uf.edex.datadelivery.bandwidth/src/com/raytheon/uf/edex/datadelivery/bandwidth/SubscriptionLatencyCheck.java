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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthMap;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthRoute;
import com.raytheon.uf.common.datadelivery.event.retrieval.GenericNotifyEvent;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataSetHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.SubscriptionHandler;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthBucket;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.DataSetLatency;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthBucketDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrieval;
import com.raytheon.uf.edex.datadelivery.bandwidth.hibernate.DataSetLatencyDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;

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
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 01, 2014 3550       ccody       Initial creation
 * Mar 09, 2015 4242       dhladky     Data integrity exception caused by dupe key
 *                                     ~ caused gaps in scheduling
 * May 27, 2015 4531       dhladky     Remove excessive Calendar references.
 * Mar 16, 2016 3919       tjensen     Cleanup unneeded interfaces
 *
 * </pre>
 *
 * @author ccody
 * @version 1.0
 */
public class SubscriptionLatencyCheck<T extends Time, C extends Coverage> {

    /** Status handler (logger) */
    protected final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SubscriptionLatencyCheck.class);

    /** Default extension at 25% (percentage as integer) */
    protected static final int DEFAULT_EXTENDED_DELAY_FACTOR = 25;

    /** Spring may attempt to start this more than once. */
    private volatile boolean started = false;

    /** Bandwidth Bucket Dao */
    protected final IBandwidthBucketDao bucketsDao;

    /** Bandwidth Dao */
    protected final IBandwidthDao<T, C> bandwidthDao;

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
     * "datadelivery/bandwidthmap.xml" or defaulted to 25 (25% = 0.25)
     * Calculated as <Original Latency> + (<Original Latency> *
     * extendedDelayTimeFactor)
     */
    protected int extendedDelayTimeFactor = DEFAULT_EXTENDED_DELAY_FACTOR;

    public enum SubAllocationStatus {

        UNKNOWN("UNKNOWN"), EXTENDED("EXTENDED"), EXPIRED("EXPIRED");

        private final String statusName;

        private SubAllocationStatus(String name) {
            statusName = name;
        }

        public String getStatus() {
            return statusName;
        }
    };

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
     *
     */
    public SubscriptionLatencyCheck(IBandwidthBucketDao bucketsDao,
            IBandwidthDao<T, C> bandwidthDao,
            SubscriptionHandler subscriptionHandler,
            DataSetHandler dataSetHandler, DataSetLatencyDao dataSetLatencyDao,
            Network network) {

        this.bucketsDao = bucketsDao;
        this.bandwidthDao = bandwidthDao;
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

        /**
         * Subscription Latency Check processing time window. This is how long
         * (Mils) before the end of a Bandwidth Bucket that this check should
         * execute.
         */
        protected long processingTimeWindow = 15 * TimeUtil.MILLIS_PER_SECOND;

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
                try {
                    waitTime = checkSubscriptionLatency();
                } catch (Throwable e) {
                    // so thread doesn't die
                    statusHandler.error(
                            "Unable to perform Subscription Latency Check.", e);
                }
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

            long waitTime = calculateNewSleepTime(
                    scheduledNextBandwidthBucketTime);

            return (waitTime);
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
                if ((bandwidthAllocation instanceof SubscriptionRetrieval)
                        && (RetrievalStatus.SCHEDULED == retrievalStatus)) {
                    Long baId = bandwidthAllocation.getIdentifier();
                    long baBandwidthBucket = bandwidthAllocation
                            .getBandwidthBucket();
                    SubscriptionRetrieval subscriptionRetrieval = (SubscriptionRetrieval) bandwidthAllocation;
                    BandwidthSubscription bandwidthSubscription = subscriptionRetrieval
                            .getBandwidthSubscription();
                    String subscriptionName = bandwidthSubscription.getName();

                    if ((baBandwidthBucket >= currentBandwidthBucketTime)
                            && (baBandwidthBucket < nextBandwidthBucketTime)) {
                        // Allocation is in the current Bandwidth Bucket
                        currentBwBktBandwidthAllocationMap.put(subscriptionName,
                                bandwidthAllocation);
                    } else if ((baBandwidthBucket >= nextBandwidthBucketTime)
                            && (baBandwidthBucket < oneMoreBandwidthBucketTime)) {
                        // Allocation is in the next Bandwidth Bucket
                        nextBwBktBandwidthAllocationMap.put(subscriptionName,
                                bandwidthAllocation);
                    } else if (baBandwidthBucket < currentBandwidthBucketTime) {
                        // This allocation should have been removed.
                        // Record it for tracking purposes
                        SubscriptionRetrieval subRetrieval = (SubscriptionRetrieval) bandwidthAllocation;
                        statusHandler
                                .info("Bandwidth Allocation: Bandwidth Bucket set earlier than most current Bandwidth Bucket. Id: "
                                        + baId
                                        + "  Assigned Bandwidth Bucket Time: "
                                        + sdf.format(bandwidthAllocation
                                                .getBandwidthBucket())
                                        + "  for Subscription: "
                                        + bandwidthSubscription.getRegistryId()
                                        + "  for Data Set Name: "
                                        + bandwidthSubscription
                                                .getDataSetName());
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

                if (bandwidthAllocation instanceof SubscriptionRetrieval) {
                    SubscriptionRetrieval subRetrieval = (SubscriptionRetrieval) bandwidthAllocation;
                    SubscriptionLatencyData subscriptionLatencyData = new SubscriptionLatencyData();
                    subLatencyDataList.add(subscriptionLatencyData);

                    BandwidthSubscription bandwidthSubscription = subRetrieval
                            .getBandwidthSubscription();

                    subscriptionLatencyData.setSubscriptionLatency(
                            subRetrieval.getSubscriptionLatency());
                    subscriptionLatencyData.setSubscriptionName(
                            bandwidthSubscription.getName());
                    long baseRefTimeLong = bandwidthSubscription
                            .getBaseReferenceTime().getTime();
                    subscriptionLatencyData
                            .setBaseRefTimestamp(baseRefTimeLong);

                    String dataSetName = bandwidthSubscription.getDataSetName();
                    String dataSetProviderName = bandwidthSubscription
                            .getProvider();
                    subscriptionLatencyData.setDataSetName(dataSetName);
                    subscriptionLatencyData
                            .setDataSetProviderName(dataSetProviderName);
                    DataSet<T, C> dataSet = null;
                    try {
                        dataSet = dataSetHandler.getByNameAndProvider(
                                dataSetName, dataSetProviderName);
                    } catch (RegistryHandlerException rhe) {
                        statusHandler.error(
                                "Unable to query DataSet Name: " + dataSetName
                                        + " Provider: " + dataSetProviderName,
                                rhe);
                    }
                    if (dataSet != null) {
                        subscriptionLatencyData.availabilityOffset = dataSet
                                .getAvailabilityOffset();
                        DataSetLatency dataSetLatency = null;
                        dataSetLatency = dataSetLatencyDao
                                .getByDataSetNameAndProvider(dataSetName,
                                        dataSetProviderName);
                        subscriptionLatencyData
                                .setDataSetLatency(dataSetLatency);
                    } else {
                        // We have a data set name but NO actual data set.
                        // Treat as having no Data Set
                    }
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
                    .round(oldLatencyMils * (extendedDelayTimeFactor / 100));
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
            int extendedDelayFactor = DEFAULT_EXTENDED_DELAY_FACTOR;

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
                        extendedDelayFactor = tempExtendedDelayFactor;
                    }
                    extendedDelayTimeFactor = extendedDelayFactor;
                }
            }

            return (bucketSizeMils);
        }

        /**
         * Calculate how long this process should sleep.
         *
         * Ideally this (Spring started) process sleeps until 15 seconds
         * (this.processingTimeWindow) the next:
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

            long newSleepTimeMillis = 0;
            long now = TimeUtil.currentTimeMillis();
            if (nextBandwidthBucketStartTime == 0) {
                // Startup. Just wait a full bandwidthBucketLength before
                // starting
                newSleepTimeMillis = this.bandwidthBucketLength;
            } else {
                if (now < (nextBandwidthBucketStartTime
                        - this.processingTimeWindow)) {
                    newSleepTimeMillis = nextBandwidthBucketStartTime
                            + this.bandwidthBucketLength
                            - this.processingTimeWindow - now;
                    if (newSleepTimeMillis < 0) {
                        newSleepTimeMillis = 0;
                    }
                } else {
                    // Notify that it took longer than the allotted time window
                    // to complete. Skip a cycle
                    statusHandler
                            .info("SubscriptionLatencyCheck took longer than usual to complete."
                                    + " Skipping a Bandwidth Bucket cycle to avoid stacking up.");
                    newSleepTimeMillis = (nextBandwidthBucketStartTime
                            - this.processingTimeWindow)
                            + (this.bandwidthBucketLength * 2) - now;
                }
            }
            return (newSleepTimeMillis);
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
