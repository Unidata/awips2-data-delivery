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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.CollectionUtils;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.raytheon.uf.common.datadelivery.bandwidth.BandwidthRequest;
import com.raytheon.uf.common.datadelivery.bandwidth.BandwidthRequest.RequestType;
import com.raytheon.uf.common.datadelivery.bandwidth.ProposeScheduleResponse;
import com.raytheon.uf.common.datadelivery.event.retrieval.SubscriptionStatusEvent;
import com.raytheon.uf.common.datadelivery.event.status.DataDeliverySystemStatusDefinition;
import com.raytheon.uf.common.datadelivery.event.status.DataDeliverySystemStatusEvent;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataDeliveryRegistryObjectTypes;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.GriddedDataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.PDADataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.PointDataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.PointTime;
import com.raytheon.uf.common.datadelivery.registry.RecurringSubscription;
import com.raytheon.uf.common.datadelivery.registry.SharedSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataSetMetaDataHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.SubscriptionHandler;
import com.raytheon.uf.common.datadelivery.service.SendToServerSubscriptionNotificationService;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.registry.ebxml.RegistryUtil;
import com.raytheon.uf.common.registry.ebxml.encoder.RegistryEncoders;
import com.raytheon.uf.common.registry.ebxml.encoder.RegistryEncoders.Type;
import com.raytheon.uf.common.registry.event.InsertRegistryEvent;
import com.raytheon.uf.common.registry.event.RegistryEvent;
import com.raytheon.uf.common.registry.event.RemoveRegistryEvent;
import com.raytheon.uf.common.registry.event.UpdateRegistryEvent;
import com.raytheon.uf.common.registry.handler.IRegistryObjectHandler;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.time.util.IPerformanceTimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.common.util.IFileModifiedWatcher;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDbInit;
import com.raytheon.uf.edex.datadelivery.bandwidth.hibernate.ISubscriptionFinder;
import com.raytheon.uf.edex.datadelivery.bandwidth.notification.BandwidthEventBus;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalPlan;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.SubscriptionRetrievalAgent;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.UnscheduledAllocationReport;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthDaoUtil;
import com.raytheon.uf.edex.registry.ebxml.util.RegistryIdUtil;

/**
 * Implementation of {@link BandwidthManager} that isolates EDEX specific
 * functionality. This keeps things out of the {@link InMemoryBandwidthManager}
 * that could interfere with garbage collection/threading concerns.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 10, 2013  2106     djohnson  Extracted from {@link BandwidthManager}.
 * Jul 11, 2013  2106     djohnson  Look up subscription from the handler
 *                                  directly.
 * Jul 19, 2013  2209     dhladky   Fixed un-serialized subscription for
 *                                  pointData.
 * Sep 13, 2013  2267     bgonzale  Check for no subscription retrieval
 *                                  attribute found.
 * Sep 16, 2013  2383     bgonzale  Add exception information for no
 *                                  subscription found. Add throws to
 *                                  updatePointDataSetMetaData.
 * Oct 01, 2013  1797     dhladky   Time and GriddedTime separation
 * Oct 10, 2013  1797     bgonzale  Refactored registry Time objects.
 * Oct 23, 2013  2385     bphillip  Change schedule method to scheduleAdhoc
 * Nov 04, 2013  2506     bgonzale  Added removeBandwidthSubscriptions method.
 *                                  Added subscriptionNotificationService field.
 *                                  Send notifications.
 * Nov 15, 2013  2545     bgonzale  Added check for subscription events before
 *                                  sending notifications.  Republish dataset
 *                                  metadata registry insert and update events
 *                                  as dataset metadata events.
 * Jan 13, 2014  2679     dhladky   Small Point data updates.
 * Jan 14, 2014  2692     dhladky   AdhocSubscription handler
 * Jan 20, 2014  2398     dhladky   Fixed rescheduling beyond active
 *                                  period/expired window.
 * Jan 24, 2014  2709     bgonzale  Changed parameter to shouldScheduleForTime
 *                                  to a Calendar.
 * Jan 29, 2014  2636     mpduff    Scheduling refactor.
 * Jan 30, 2014  2686     dhladky   refactor of retrieval.
 * Feb 06, 2014  2636     bgonzale  Added initializeScheduling method that uses
 *                                  the in-memory bandwidth manager to perform
 *                                  the scheduling initialization because of
 *                                  efficiency.
 * Feb 11, 2014  2771     bgonzale  Use Data Delivery ID instead of Site.
 * Feb 10, 2014  2636     mpduff    Pass Network map to be scheduled.
 * Feb 21, 2014  2636     dhladky   Try catch to keep MaintTask from dying.
 * Mar 31, 2014  2889     dhladky   Added username for notification center
 *                                  tracking.
 * Apr 09, 2014  3012     dhladky   Range the queries for metadata checks, adhoc
 *                                  firing prevention.
 * Apr 22, 2014  2992     dhladky   Added IdUtil for siteList
 * May 22, 2014  2808     dhladky   schedule unscheduled when a sub is
 *                                  deactivated
 * Jul 28, 2014  2752     dhladky   Fixed bad default user for registry.
 * Oct 08, 2014  2746     ccody     Relocated registryEventListener to
 *                                  EdexBandwidthManager super class
 * Oct 15, 2014  3664     ccody     Add notification event for unscheduled
 *                                  Subscriptions at startup
 * Oct 12, 2014  3707     dhladky   Changed the way gridded subscriptions are
 *                                  triggerd for retrieval.
 * Oct 28, 2014  2748     ccody     Add notification event for Subscription
 *                                  modifications. Add Thread.sleep to mitigate
 *                                  registry update race condition.
 * Jan 20, 2015  2414     dhladky   Refactored and better documented, fixed
 *                                  event handling, fixed race conditions.
 * Jan 15, 2015  3884     dhladky   Removed shutdown and shutdown internal
 *                                  methods.
 * Jan 30, 2015  2746     dhladky   Handling special cases in notification
 *                                  message routing.
 * Mar 08, 2015  3950     dhladky   Bandwidth change better handled.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * May 10, 2016  4493     dhladky   Deleted un-needed methods/vars
 * May 27, 2016  4531     dhladky   Remove excessive Calendar references.
 * Jun 09, 2016  4047     dhladky   Performance improvement on startup, initial
 *                                  startup scheduling async now.
 * Aug 09, 2016  5771     rjpeter   Allow concurrent event processing
 * Apr 05, 2017  1045     tjensen   Update for moving datasets
 * Aug 02, 2017  6186     rjpeter   Moved intersection logic to DataSetMetaData,
 *                                  refactored dataSetMetaData processing
 * Oct 25, 2017  6484     tjensen   Merged SubscriptionRetrievals and
 *                                  BandwidthAllocations
 * Dec 12, 2017  6522     mapeters  Add thread-based logging for retrieval
 * Feb 02, 2018  6471     tjensen   Added UnscheduledAllocationReports
 *
 * </pre>
 *
 * @author djohnson
 */
public abstract class EdexBandwidthManager<T extends Time, C extends Coverage>
        extends BandwidthManager<T, C> {

    /** pattern to id RAP and RAP_F datasets */
    private static final Pattern RAP_PATTERN = Pattern
            .compile(".*rap_f\\d\\d$");

    /** handler for DataSetMeataData objects from the registry **/
    private final DataSetMetaDataHandler dataSetMetaDataHandler;

    /** handler for Subscription objects from the registry **/
    private final SubscriptionHandler subscriptionHandler;

    /** The scheduler for retrievals **/
    private final ScheduledExecutorService scheduler;

    /** Notification handler for sending messages from BWM to CAVE/EDEX **/
    private final SendToServerSubscriptionNotificationService subscriptionNotificationService;

    /** Interface for finding subscriptions **/
    private final ISubscriptionFinder<Subscription<T, C>> findSubscriptionsStrategy;

    /**
     * Update the BWM if any of it's config files change
     */
    private final Runnable watchForConfigFileChanges = new Runnable() {

        private final IFileModifiedWatcher fileModifiedWatcher = FileUtil
                .getFileModifiedWatcher(
                        EdexBandwidthContextFactory.getBandwidthMapConfig());

        @Override
        public void run() {
            if (fileModifiedWatcher.hasBeenModified()) {
                bandwidthMapConfigurationUpdated();
            }
        }
    };

    /**
     * Primary spring utilized constructor
     *
     * @param dbInit
     * @param bandwidthDao
     * @param retrievalManager
     * @param bandwidthDaoUtil
     * @param subscriptionNotificationService
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public EdexBandwidthManager(IBandwidthDbInit dbInit,
            IBandwidthDao<T, C> bandwidthDao, RetrievalManager retrievalManager,
            BandwidthDaoUtil<T, C> bandwidthDaoUtil, RegistryIdUtil idUtil,
            SubscriptionRetrievalAgent retrievalAgent,
            DataSetMetaDataHandler dataSetMetaDataHandler,
            SubscriptionHandler subscriptionHandler,
            SendToServerSubscriptionNotificationService subscriptionNotificationService,
            ISubscriptionFinder findSubscriptionsStrategy) {
        super(dbInit, bandwidthDao, retrievalManager, bandwidthDaoUtil, idUtil,
                retrievalAgent);

        this.dataSetMetaDataHandler = dataSetMetaDataHandler;
        this.subscriptionHandler = subscriptionHandler;
        this.subscriptionNotificationService = subscriptionNotificationService;
        this.findSubscriptionsStrategy = findSubscriptionsStrategy;

        // schedule maintenance tasks
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Signals the bandwidth map localization file is updated, perform a
     * reinitialize operation.
     */
    private void bandwidthMapConfigurationUpdated() {
        BandwidthRequest<T, C> request = new BandwidthRequest<>();
        request.setRequestType(RequestType.REINITIALIZE);

        try {
            handleRequest(request);
        } catch (Exception e) {
            logger.error("Error while reinitializing the bandwidth manager.",
                    e);
        }
    }

    /**
     * Reset specifically the EdexBandwidthManager.
     *
     * This method is abstract here so that the handleRequest implemented in the
     * abstract BandwidthManager class is able to make a call the instance of
     * the EdexBandwidthManager so that it can "reshuffle the deck" of Retrieval
     * Plans, etc.
     * <p>
     * The EdexBandwidthManageris the only BandwidthManager class that needs to
     * be able to reset itself in response to operations from within the
     * abstract BandwidthManager.handleRequest method processing. Presently,
     * this method must be implemented but empty in all other implementations.
     *
     * @see com.raytheon.uf.edex.datadelivery.bandwidth.BandwidthManager.
     *      resetBandwidthManager
     */
    @Override
    protected void resetBandwidthManager(Network requestNetwork,
            String resetReasonMessage) {

        Map<Network, List<Subscription<T, C>>> networkToSubscriptionSetMap = null;
        List<Subscription<T, C>> subscriptionList = null;
        /** Essentially re-schedule everything and restart retrieval manager. */

        if (findSubscriptionsStrategy != null) {
            try {
                networkToSubscriptionSetMap = findSubscriptionsStrategy
                        .findSubscriptionsToSchedule();
            } catch (Exception ex) {
                logger.error(
                        "Error occurred searching for subscriptions. Falling back to backup files.",
                        ex);
                networkToSubscriptionSetMap = null;
            }
        }

        if ((networkToSubscriptionSetMap != null)
                && (!networkToSubscriptionSetMap.isEmpty())) {

            // Deal with retrieval restart first
            try {
                /**
                 * restart RetrievalManager.
                 */
                retrievalManager.restart();

            } catch (Exception e) {
                logger.error("Can't restart Retrieval Manager! ", e);
            }

            logger.info(
                    "EdexBandwidthManager: Rescheduling Subscriptions for Bandwidth Manager reset: "
                            + resetReasonMessage);

            // Reactivate Subscriptions
            String networkName;

            for (Network network : networkToSubscriptionSetMap.keySet()) {

                networkName = network.name();
                subscriptionList = networkToSubscriptionSetMap.get(network);

                if (subscriptionList != null) {
                    for (Subscription<T, C> subscription : subscriptionList) {
                        if (isSubscriptionManagedLocally(subscription)
                                && subscription.isActive()) {
                            try {
                                logger.info("\tRe-scheduling subscription: "
                                        + subscription.getName());
                                /*
                                 * subscriptionUpdated() both removes the old
                                 * allocations and re-schedules. It's the most
                                 * efficient option for resetting BWM
                                 */
                                List<UnscheduledAllocationReport> unscheduledList = subscriptionUpdated(
                                        subscription);
                                logSubscriptionListUnscheduled(networkName,
                                        unscheduledList);
                            } catch (Exception ex) {
                                logger.error(
                                        "Error occurred restarting EdexBandwidthManager for Network: "
                                                + network + " Subscription: "
                                                + subscription.getName(),
                                        ex);
                            }
                        }
                    }
                }
            }
        } else {
            logger.error(
                    "Error occurred restarting EdexBandwidthManager: unable to find subscriptions to reschedule");
            return;
        }

        logger.info("Restored Subscriptions for Bandwidth Manager Reset.");

        // Create system status event
        Calendar now = TimeUtil.newGmtCalendar();
        DataDeliverySystemStatusEvent event = new DataDeliverySystemStatusEvent();
        event.setName(requestNetwork.name());
        event.setDate(now);
        event.setSystemType("Bandwidth");
        event.setStatus(DataDeliverySystemStatusDefinition.RESTART);

        EventBus.publish(event);
    }

    // ******************** Subscription Related Methods **************

    /**
     * Schedule the list of subscriptions. This list is generally passed from
     * the in memory BWM and is processed here for scheduling (for real) and DB
     * persistence.
     *
     * @param subscriptions
     *            the subscriptions
     * @return the set of unscheduled allocation reports
     */
    @Override
    protected Set<UnscheduledAllocationReport> scheduleSubscriptions(
            List<Subscription<T, C>> insubscriptions) {

        /**
         * This returns an empty unscheduled list by design. The actual storage
         * is moved asynchronously to this process. We handle the processing of
         * unscheduled there and then. This saves time at start-up that was
         * eaten up waiting for the BWM keeping the JVM initialization from
         * completing.
         */
        if (insubscriptions != null) {
            for (Subscription<T, C> sub : orderSubscriptionsByPriority(
                    insubscriptions)) {
                BandwidthEventBus.publish(sub);
            }
        }

        return new TreeSet<>();
    }

    /**
     * Process the subscription scheduling asynchronously for applying proposed
     * subscriptions. Reads subs off the BandwidthEventBus placed by
     * scheduleSubscriptions() method.
     *
     * @param persistSub
     */
    @Subscribe
    public void persistScheduledSubscription(Subscription<T, C> persistSub) {

        logger.info("Persisting Subscription: " + persistSub.getName()
                + " status: " + persistSub.getStatus().name());
        subscriptionUpdated(persistSub);

        /**
         * We don't do anything with the unscheduled allocations at this point.
         * We leave them to be cycled by the maintenance task (proposal flags
         * then anyway). Also, do not send out a SubscriptionEvent, proposal has
         * that covered.
         */
    }

    /**
     * Give a listing of the unscheduled subscriptions.
     *
     * @param subscriptionNetwork
     * @param unscheduledList
     */
    private void logSubscriptionListUnscheduled(String subscriptionNetwork,
            List<UnscheduledAllocationReport> unscheduledList) {
        if (unscheduledList != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("The following subscriptions for Network: ");
            sb.append(subscriptionNetwork);
            sb.append(" remain unscheduled after refresh:\n");
            BandwidthAllocation ba = null;
            for (int i = 0; i < unscheduledList.size(); i++) {
                ba = unscheduledList.get(i).getUnscheduled();
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(ba.getId());
            }
            logger.info(sb.toString());
        }

    }

    /**
     * Get the spring injected subscription object handler.
     *
     * @return the subscriptionHandler
     */
    public SubscriptionHandler getSubscriptionHandler() {
        return subscriptionHandler;
    }

    /**
     * Get the subscription if it is part of a client subscription request.
     *
     * @param subId
     * @return
     */
    protected Subscription<T, C> getRequestSubscription(String subId) {
        Subscription<T, C> subscription = null;
        if (requestSubscriptions != null) {
            if (requestSubscriptions.containsKey(subId)) {
                subscription = requestSubscriptions.get(subId);
            }
        }
        return subscription;
    }

    // *************** Registry Object Lookup Related Methods **********

    /**
     * Query the registry for any stored object by it's ID.
     *
     * @param handler
     * @param id
     * @return
     */
    protected <M> M getRegistryObjectById(IRegistryObjectHandler<M> handler,
            String id) {
        try {
            return handler.getById(id);
        } catch (RegistryHandlerException e) {
            logger.error("Error attempting to retrieve RegistryObject[" + id
                    + "] from Registry.", e);
            return null;
        }
    }

    /**
     * Gets the id'd dataSetMetaData object from the registry.
     *
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    private DataSetMetaData<T, C> getDataSetMetaData(String id) {
        return getRegistryObjectById(dataSetMetaDataHandler, id);
    }

    // ******************** BANDWIDTH BUS related methods *********************

    /*
     * These will all carry the @Subscribe notation. This means they will
     * deliver objects of their type that have been placed on the Bus.
     */

    /**
     * When a Subscription is removed from the Registry, a RemoveRegistryEvent
     * is generated and forwarded to this method to remove the necessary
     * BandwidthReservations (and perhaps redefine others).
     *
     * @param event
     */
    @SuppressWarnings("unchecked")
    @Subscribe
    @AllowConcurrentEvents
    public void subscriptionRemoved(RemoveRegistryEvent event) {
        String objectType = event.getObjectType();
        if (objectType != null) {
            // For removes we only care about Subscriptions. DSMD is un-flagged.
            if (DataDeliveryRegistryObjectTypes
                    .isRecurringSubscription(event.getObjectType())) {
                logger.info(
                        "Received Subscription removal notification for Subscription ["
                                + event.getId() + "]");

                remove(bandwidthDao
                        .getBandwidthAllocationsByRegistryId(event.getId()));

                try {
                    /*
                     * You have to construct this object from JAXB because it
                     * has been deleted by the registry by the time you get it
                     * here for BWM and Client notification.
                     */
                    Subscription<T, C> sub = (Subscription<T, C>) RegistryEncoders
                            .ofType(Type.JAXB)
                            .decodeObject(event.getRemovedObject());
                    logger.info("Subscription removed: " + event.getId());
                    /*
                     * We only care about subs that our Site ID is contained
                     * within
                     */
                    boolean isLocalOrigination = false;

                    if (sub.getOfficeIDs().contains(RegistryIdUtil.getId())) {
                        isLocalOrigination = true;
                    }

                    sendSubscriptionNotificationEvent(event, sub,
                            isLocalOrigination);

                } catch (SerializationException e) {
                    logger.error(
                            "Failed to retrieve deleted object from RemoveRegistryEvent",
                            e);
                }
            }
        }
    }

    /**
     * Listen for registry insert events necessary to drive Bandwidth
     * Management.
     *
     * @param event
     *            The <code>InsertRegistryEvent</code> Object to evaluate.
     */
    @SuppressWarnings("unchecked")
    @Subscribe
    @AllowConcurrentEvents
    public void registryEventListener(InsertRegistryEvent event) {
        final String objectType = event.getObjectType();

        // All DSMD updates go to BUS and circuit back to BWM
        if (DataDeliveryRegistryObjectTypes.DATASETMETADATA
                .equals(objectType)) {
            publishDataSetMetaDataEvent(event);
        }

        // Want ALL subs here, regardless of origination or management.
        if (DataDeliveryRegistryObjectTypes
                .isRecurringSubscription(event.getObjectType())) {

            /*
             * A subscription found in the request cache means that the update
             * was made "locally". In a local CAVE GUI.
             */
            Subscription<T, C> subscription = getRequestSubscription(
                    event.getId());
            boolean isLocalOrigination = false;

            /*
             * Non-locally effected subscriptions are queried directly from the
             * registry.
             */
            if (subscription == null) {
                subscription = getRegistryObjectById(getSubscriptionHandler(),
                        event.getId());
            } else {
                isLocalOrigination = true;
            }

            /*
             * This is to catch outside changes that affect subscriptions you
             * are subscribed too. The special case is where someone added your
             * site to a shared sub at another registry node.
             */
            if (!isLocalOrigination) {
                if (subscription.getOfficeIDs().contains(RegistryIdUtil.getId())
                        || subscription.getOriginatingSite()
                                .equals(RegistryIdUtil.getId())) {
                    isLocalOrigination = true;
                }
            }

            logger.info("Subscription Inserted: " + subscription.getName());
            sendSubscriptionNotificationEvent(event, subscription,
                    isLocalOrigination);
        }
    }

    /**
     * Listen for Registry update events. Filter for subscription specific
     * events. Sends corresponding subscription notification events.
     *
     * @param event
     */
    @SuppressWarnings("unchecked")
    @Subscribe
    @AllowConcurrentEvents
    public void registryEventListener(UpdateRegistryEvent event) {
        final String objectType = event.getObjectType();
        // All DSMD updates go to BUS and circuit back to BWM
        if (DataDeliveryRegistryObjectTypes.DATASETMETADATA
                .equals(objectType)) {
            publishDataSetMetaDataEvent(event);
        }
        // Only want Shared and Site subs here, no Pending or Adhoc's.
        if ((DataDeliveryRegistryObjectTypes.SHARED_SUBSCRIPTION
                .equals(objectType))
                || (DataDeliveryRegistryObjectTypes.SITE_SUBSCRIPTION
                        .equals(objectType))) {

            /*
             * A subscription found in the request cache means that the update
             * was made "locally". In a local CAVE GUI.
             */
            Subscription<T, C> subscription = getRequestSubscription(
                    event.getId());
            boolean isLocalOrigination = false;

            /*
             * Non-locally effected subscriptions are queried directly from the
             * registry.
             */
            if (subscription == null) {
                subscription = getRegistryObjectById(getSubscriptionHandler(),
                        event.getId());
            } else {
                isLocalOrigination = true;
            }

            /*
             * This is to catch outside changes that affect subscriptions you
             * which are subscribed and the special case where you remove
             * yourself from a shared sub.
             */
            if (!isLocalOrigination) {
                if (subscription.getOfficeIDs().contains(RegistryIdUtil.getId())
                        || subscription.getOriginatingSite()
                                .equals(RegistryIdUtil.getId())) {
                    isLocalOrigination = true;
                }
            }

            /*
             * Only subscriptions local to this BWM get processed for scheduling
             * updates.
             */
            if (isSubscriptionManagedLocally(subscription)) {
                subscriptionUpdated(subscription);
            }

            logger.info("Subscription Updated: " + subscription.getName());
            sendSubscriptionNotificationEvent(event, subscription,
                    isLocalOrigination);
        }
    }

    /**
     * Filter for DataSetMetaData Objects received from registry. Publish to
     * BandwidthEventBus for further downstream processing by specific type of
     * DataSetMetaData object.
     *
     * @param re
     */
    private void publishDataSetMetaDataEvent(RegistryEvent re) {
        final String id = re.getId();
        DataSetMetaData<T, C> dsmd = getDataSetMetaData(id);

        if (dsmd != null) {
            /*
             * TODO: A hack to prevent rap_f and rap datasets being Identified
             * as the same dataset...
             */
            Matcher matcher = RAP_PATTERN.matcher(dsmd.getUrl());
            if (matcher.matches()) {
                logger.info("Found rap_f dataset - updating dataset name from ["
                        + dsmd.getDataSetName() + "] to [rap_f]");
                dsmd.setDataSetName("rap_f");
            }

            BandwidthEventBus.publish(dsmd);

        } else {
            logger.error("No DataSetMetaData found for id [" + id + "]");
        }
    }

    /**
     * Process a general DataSetMetaData object for update.
     *
     * @param dataSetMetaData
     *            the metadadata
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected List<? extends Subscription> getSubscriptionsForDataSetMetaData(
            DataSetMetaData dataSetMetaData) throws ParseException {
        List<? extends Subscription> subscriptions = null;

        /*
         * Look for active subscriptions to the dataset.
         */
        String dataSetName = dataSetMetaData.getDataSetName();
        String url = dataSetMetaData.getUrl();

        try {

            // check to see if this site is the NCF
            if (RegistryIdUtil.getId().equals(RegistryUtil.defaultUser)) {
                subscriptions = subscriptionHandler
                        .getSharedSubscriptionHandler()
                        .getActiveByDataSetAndProvider(
                                dataSetMetaData.getDataSetName(),
                                dataSetMetaData.getProviderName());
            } else {
                subscriptions = subscriptionHandler.getSiteSubscriptionHandler()
                        .getActiveByDataSetAndProviderForSite(
                                dataSetMetaData.getDataSetName(),
                                dataSetMetaData.getProviderName(),
                                RegistryIdUtil.getId());
            }
        } catch (RegistryHandlerException e) {
            logger.error("Failed to lookup subscriptions for dataset ["
                    + dataSetName + "], url [" + url + "]", e);
            return Collections.emptyList();
        }

        if (CollectionUtil.isNullOrEmpty(subscriptions)) {
            logger.debug("No subscriptions found for dataset [" + dataSetName
                    + "], url [" + url + "]");
            return subscriptions;
        }

        Iterator<? extends Subscription> subIter = subscriptions.iterator();
        while (subIter.hasNext()) {
            Subscription<T, C> subscription = subIter.next();

            try {
                String skipReason = dataSetMetaData
                        .satisfiesSubscription(subscription);
                if (skipReason != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Skipping subscription ["
                                + subscription.getName() + "] for dataSet["
                                + dataSetName + "] url [" + url + "]:"
                                + skipReason);
                    }
                    subIter.remove();
                }
            } catch (Exception e) {
                logger.error("Error checking if dataset [" + dataSetName
                        + "] url [" + url + "] satisfies subscription ["
                        + subscription.getName() + "], skipping subscription",
                        e);
                subIter.remove();
            }
        }

        if (!subscriptions.isEmpty()) {
            logger.info("Found " + subscriptions.size()
                    + " subscriptions subscribed to dataset " + dataSetName
                    + ", url " + url + ".");
        } else if (logger.isDebugEnabled()) {
            logger.debug("No subscriptions found for dataset [" + dataSetName
                    + "], url [" + url + "]");
        }

        return subscriptions;
    }

    /**
     * Process a {@link PDADataSetMetaData} that was received from the event
     * bus.
     *
     * @param dataSetMetaData
     *            the metadadata
     * @throws ParseException
     */
    @Subscribe
    @AllowConcurrentEvents
    public void updatePDADataSetMetaData(PDADataSetMetaData dataSetMetaData)
            throws ParseException {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(RETRIEVAL_THREAD_PREFIX + oldThreadName);

        try {
            @SuppressWarnings("rawtypes")
            List<? extends Subscription> subs = getSubscriptionsForDataSetMetaData(
                    dataSetMetaData);

            if (CollectionUtil.isNullOrEmpty(subs)) {
                return;
            }

            retrievalAgent.queueRetrievals(dataSetMetaData, subs);
        } finally {
            Thread.currentThread().setName(oldThreadName);
        }
    }

    /**
     * Process a {@link GriddedDataSetMetaData} that was received from the event
     * bus.
     *
     * @param dataSetMetaData
     *            the metadadata
     * @throws ParseException
     */
    @Subscribe
    @AllowConcurrentEvents
    public void updateGriddedDataSetMetaData(
            GriddedDataSetMetaData dataSetMetaData) throws ParseException {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(RETRIEVAL_THREAD_PREFIX + oldThreadName);

        try {
            @SuppressWarnings("rawtypes")
            List<? extends Subscription> subs = getSubscriptionsForDataSetMetaData(
                    dataSetMetaData);

            if (CollectionUtil.isNullOrEmpty(subs)) {
                return;
            }

            retrievalAgent.queueRetrievals(dataSetMetaData, subs);
        } finally {
            Thread.currentThread().setName(oldThreadName);
        }
    }

    /**
     * Process a {@link PointDataSetMetaData} that was received from the event
     * bus.
     *
     * @param dataSetMetaData
     *            the metadadata
     * @throws ParseException
     */
    @SuppressWarnings("unchecked")
    @Subscribe
    @AllowConcurrentEvents
    public void updatePointDataSetMetaData(PointDataSetMetaData dataSetMetaData)
            throws ParseException {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(RETRIEVAL_THREAD_PREFIX + oldThreadName);

        try {
            @SuppressWarnings("rawtypes")
            List<? extends Subscription> subs = getSubscriptionsForDataSetMetaData(
                    dataSetMetaData);

            if (CollectionUtil.isNullOrEmpty(subs)) {
                return;
            }

            // Update the subscription to be for the time of the DSMD
            final PointTime time = dataSetMetaData.getTime();
            final Date pointTimeStart = time.getStart();
            final Date pointTimeEnd = time.getEnd();

            for (Subscription<T, C> sub : subs) {
                T subTime = sub.getTime();
                subTime.setStart(pointTimeStart);
                subTime.setEnd(pointTimeEnd);
            }

            retrievalAgent.queueRetrievals(dataSetMetaData, subs);
        } finally {
            Thread.currentThread().setName(oldThreadName);
        }
    }

    /**
     ******************************* Notification Related Methods **************************
     */

    /**
     * Determine if the subscription belongs to the local BWM.
     *
     * @param sub
     * @return
     */
    @SuppressWarnings("rawtypes")
    private static boolean isSubscriptionManagedLocally(Subscription sub) {

        boolean isLocallyManaged = false;

        if (sub instanceof SharedSubscription) {
            /*
             * This means an update has occurred on a SHARED sub. We however do
             * not want it scheduled locally. This check will catch it when the
             * update occurs on the Central Registry and update it accordingly.
             */
            isLocallyManaged = RegistryIdUtil.getId()
                    .equals(RegistryUtil.defaultUser);

        } else {
            /*
             * Local site update to a local subscription This catches non-GUI
             * related updates from other modules of DD.
             */
            isLocallyManaged = sub.getOriginatingSite()
                    .equals(RegistryIdUtil.getId());
        }

        return isLocallyManaged;
    }

    /**
     * Send out subscription notifications to listening CAVE clients and EDEX
     * components.
     *
     * @param event
     * @param sub
     */
    protected void sendSubscriptionNotificationEvent(RegistryEvent event,
            Subscription<T, C> sub, boolean isApplicableForTheLocalSite) {
        final String objectType = event.getObjectType();

        if (DataDeliveryRegistryObjectTypes
                .isRecurringSubscription(objectType)) {
            if (sub != null) {
                /*
                 * Send out a subscription update notification for CAVE clients.
                 * We want only shared subs that are locally relevant and local
                 * subs.
                 */
                if (isApplicableForTheLocalSite) {
                    switch (event.getAction()) {
                    case UPDATE:
                        subscriptionNotificationService
                                .sendUpdatedSubscriptionNotification(sub,
                                        event.getUsername());
                        break;
                    case INSERT:
                        subscriptionNotificationService
                                .sendCreatedSubscriptionNotification(sub,
                                        event.getUsername());
                        break;
                    case DELETE:
                        subscriptionNotificationService
                                .sendDeletedSubscriptionNotification(sub,
                                        event.getUsername());
                        break;
                    default:
                        logger.error("Invalid RegistryEvent action: "
                                + event.getAction());
                    }
                }
            }
        }
    }

    /**
     * Send out subscription status event to listening CAVE and EDEX components.
     * components.
     *
     * @param event
     * @param sub
     */
    protected void sendSubscriptionStatusEvent(String message,
            Subscription<T, C> sub) {
        SubscriptionStatusEvent sse = new SubscriptionStatusEvent(sub, message);
        EventBus.publish(sse);
    }

    // ******************* Scheduling Related methods *********************
    /**
     * This method is called at EDEX (hibernate) BWM initialization. It attempts
     * to schedule the subscriptions available to it. It allows adds the timer
     * tasks for the Maintenance Task which keep scheduling current.
     *
     * @param subMap
     *            <Network, List<Subscription>> subMap
     */
    @Override
    @SuppressWarnings("rawtypes")
    public List<String> initializeScheduling(
            Map<Network, List<Subscription>> subMap)
            throws SerializationException {
        List<String> unscheduledNames = new ArrayList<>(0);

        try {
            for (Network key : subMap.keySet()) {
                List<Subscription<T, C>> subscriptions = new ArrayList<>();
                // this loop is here only because of the generics mess
                for (Subscription<T, C> s : subMap.get(key)) {
                    subscriptions.add(s);
                }
                ProposeScheduleResponse response = proposeScheduleSubscriptions(
                        subscriptions);
                Set<String> unscheduled = response
                        .getUnscheduledSubscriptions();
                List<Subscription<T, C>> unScheduledSubs = new ArrayList<>();

                if (!unscheduled.isEmpty()) {
                    /*
                     * if proposed was unable to schedule some subscriptions it
                     * will schedule nothing. schedule any that can be scheduled
                     * here.
                     */
                    List<Subscription<T, C>> subsToSchedule = new ArrayList<>();
                    for (Subscription<T, C> s : subscriptions) {
                        if (!unscheduled.contains(s.getName())) {
                            subsToSchedule.add(s);
                        } else {
                            unScheduledSubs.add(s);
                        }
                    }

                    unscheduled.addAll(getUnscheduledSubNames(
                            scheduleSubscriptions(subsToSchedule)));
                    unscheduledNames.addAll(unscheduled);

                    /*
                     * Update unscheduled subscriptions to reflect reality of
                     * condition
                     */
                    if (!CollectionUtils.isEmpty(unScheduledSubs)) {
                        SubscriptionStatusEvent sse = null;
                        for (Subscription<T, C> sub : unScheduledSubs) {
                            try {
                                sub.setUnscheduled(true);
                                subscriptionHandler
                                        .update(RegistryUtil.defaultUser, sub);
                                sse = new SubscriptionStatusEvent(sub,
                                        " is unscheduled. Insufficient bandwidth at startup.");
                                EventBus.publish(sse);
                            } catch (RegistryHandlerException e) {
                                logger.error(
                                        "Unable to update subscription scheduling status: "
                                                + sub.getName(),
                                        e);
                            }
                        }
                    }
                }
            }
        } finally {

            scheduler.scheduleAtFixedRate(watchForConfigFileChanges, 1, 1,
                    TimeUnit.MINUTES);
            scheduler.scheduleAtFixedRate(new MaintenanceTask(), 30, 30,
                    TimeUnit.MINUTES);
        }
        return unscheduledNames;
    }

    /**
     * Get's the list of subscriptions to schedule by network type.
     *
     * @param network
     */
    @Override
    protected List<Subscription<T, C>> getSubscriptionsToSchedule(
            Network network) {
        List<Subscription<T, C>> subList = new ArrayList<>(0);
        try {
            Map<Network, List<Subscription<T, C>>> activeSubs = findSubscriptionsStrategy
                    .findSubscriptionsToSchedule();
            if (activeSubs.get(network) != null) {
                subList = activeSubs.get(network);
            }
        } catch (Exception e) {
            logger.error("Error retrieving subscriptions.", e);
        }

        return subList;
    }

    /**
     * For a given block of allocations. Un-schedule it's subscriptions and
     * remove it's Retrieval Attributes.
     *
     * @param List
     *            <BandwidthAllocation> unscheduled
     */
    @Override
    protected void unscheduleSubscriptionsForAllocations(
            List<BandwidthAllocation> unscheduled) {
        Set<Subscription<T, C>> subscriptions = new HashSet<>();

        for (BandwidthAllocation unscheduledAllocation : unscheduled) {

            try {
                Subscription<T, C> sub = DataDeliveryHandlers
                        .getSubscriptionHandler()
                        .getById(unscheduledAllocation.getSubscriptionId());
                if (sub != null) {
                    subscriptions.add(sub);
                }
            } catch (RegistryHandlerException e) {
                logger.error("Unable to deserialize a subscription with id "
                        + unscheduledAllocation.getSubscriptionId(), e);
                continue;
            }
        }

        for (Subscription<T, C> subscription : subscriptions) {
            boolean origSchedStatus = subscription.isUnscheduled();
            subscription.setUnscheduled(true);

            subscriptionUpdated(subscription);

            if (!origSchedStatus) {
                sendSubscriptionStatusEvent(" is unscheduled.", subscription);
            }
        }
    }

    /**
     * Try to schedule other subs when another deactivates
     *
     * @param deactivatedSubName
     */
    @SuppressWarnings({ "unchecked", "rawtypes", "unused" })
    @Override
    public void scheduleUnscheduledSubscriptions(String deactivatedSubName) {

        /*
         * With the removal of allocations, try now to add any subs that might
         * not have had room to schedule.
         */
        Map<Network, List<Subscription<T, C>>> subMap = null;
        try {
            subMap = findSubscriptionsStrategy.findSubscriptionsToSchedule();
        } catch (Exception e) {
            logger.error(
                    "Problem finding subscriptions that need to be scheduled",
                    e);
        }
        if (subMap != null) {
            logger.info("Finding any unscheduled subscriptions...");
            List<Subscription<T, C>> unschedSubs = new ArrayList<>();
            for (Network route : subMap.keySet()) {
                for (Subscription<T, C> sub : subMap.get(route)) {
                    // look for unscheduled subs, try to schedule them.
                    if (!sub.getName().equals(deactivatedSubName)
                            && ((RecurringSubscription<T, C>) sub)
                                    .shouldSchedule()
                            && sub.isUnscheduled()) {
                        unschedSubs.add(sub);
                    }
                }
            }

            if (!CollectionUtil.isNullOrEmpty(unschedSubs)) {
                boolean modIsUnSched = false;
                String msg = null;
                for (Subscription sub : unschedSubs) {

                    logger.info(
                            "Attempting to Schedule unscheduled subscription: "
                                    + sub.getName());
                    List<BandwidthAllocation> unscheduleAllocations = schedule(
                            sub);

                    /*
                     * The problem with the way this originally worked was that
                     * the info that a sub was either "scheduled", or
                     * "un-scheduled" was never persisted to the subscription in
                     * registry. That state never survived a restart and you
                     * would get unpredictable subscription scheduling each time
                     * you restarted. This way, subs that are un-scheduled and
                     * or deactivated will be clearly visible in the
                     * SubscriptionManager Dialog. Before, They existed in a
                     * limbo state in which they weren't displayed in the BUG
                     * graph but still showed as active in the
                     * SubscriptionManager presenting a confusing state.
                     */
                    try {
                        subscriptionHandler.update(RegistryUtil.registryUser,
                                sub);
                        // Send notification if its scheduling status has
                        // changed
                        modIsUnSched = sub.isUnscheduled();
                        if (modIsUnSched) {
                            msg = " is unscheduled.";
                        } else {
                            msg = " is scheduled.";
                        }
                        sendSubscriptionStatusEvent(msg, sub);

                    } catch (RegistryHandlerException e) {
                        logger.error(
                                "Couldn't update subscription state in BandwidthManager",
                                e);
                    }
                }
            } else {
                logger.info("No unscheduled subscriptions found...");
            }
        }
    }

    /**
     * Private inner work thread used to keep the RetrievalPlans up to date.
     * This fired off in a crontab every 30 minutes. What it does is keep all of
     * the subscriptions scheduled out to the Retrieval Plan maximum window
     * value. (Default is 48 hours)
     */
    private class MaintenanceTask implements Runnable {
        @Override
        public void run() {

            try {

                IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
                timer.start();
                logger.info("MaintenanceTask starting...");

                for (RetrievalPlan plan : retrievalManager.getRetrievalPlans()
                        .values()) {
                    logger.info("MaintenanceTask: " + plan.getNetwork());
                    logger.info("MaintenanceTask: planStart: "
                            + plan.getPlanStart().getTime() + " planEnd: "
                            + plan.getPlanEnd().getTime());
                    plan.resize();
                    logger.info("MaintenanceTask: resized planStart: "
                            + plan.getPlanStart().getTime() + " planEnd: "
                            + plan.getPlanEnd().getTime());

                    // Find DEFERRED Allocations and load them into the plan...
                    List<BandwidthAllocation> deferred = bandwidthDao
                            .getDeferred(plan.getNetwork(),
                                    plan.getPlanEnd().getTime());
                    if (!deferred.isEmpty()) {
                        retrievalManager.schedule(deferred);
                    }
                }

                int numSubsProcessed = 0;
                for (RetrievalPlan plan : retrievalManager.getRetrievalPlans()
                        .values()) {
                    numSubsProcessed += updateSchedule(plan.getNetwork());
                }
                timer.stop();
                logger.info("MaintenanceTask complete: " + timer.getElapsed()
                        + " - " + numSubsProcessed
                        + " Subscriptions processed.");

            } catch (Throwable t) {
                logger.error(
                        "MaintenanceTask: Subscription update scheduling has failed",
                        t);
            }
        }
    }
}
