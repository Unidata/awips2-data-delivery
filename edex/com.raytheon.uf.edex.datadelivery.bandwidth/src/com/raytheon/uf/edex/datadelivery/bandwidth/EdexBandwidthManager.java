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

import static com.raytheon.uf.common.registry.ebxml.encoder.RegistryEncoders.Type.JAXB;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.CollectionUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.raytheon.uf.common.datadelivery.bandwidth.BandwidthRequest;
import com.raytheon.uf.common.datadelivery.bandwidth.BandwidthRequest.RequestType;
import com.raytheon.uf.common.datadelivery.bandwidth.ProposeScheduleResponse;
import com.raytheon.uf.common.datadelivery.event.retrieval.SubscriptionStatusEvent;
import com.raytheon.uf.common.datadelivery.event.status.DataDeliverySystemStatusDefinition;
import com.raytheon.uf.common.datadelivery.event.status.DataDeliverySystemStatusEvent;
import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
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
import com.raytheon.uf.common.datadelivery.registry.SiteSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.IAdhocSubscriptionHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.IDataSetMetaDataHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.ISubscriptionHandler;
import com.raytheon.uf.common.datadelivery.service.ISubscriptionNotificationService;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.registry.ebxml.RegistryUtil;
import com.raytheon.uf.common.registry.ebxml.encoder.RegistryEncoders;
import com.raytheon.uf.common.registry.event.InsertRegistryEvent;
import com.raytheon.uf.common.registry.event.RegistryEvent;
import com.raytheon.uf.common.registry.event.RemoveRegistryEvent;
import com.raytheon.uf.common.registry.event.UpdateRegistryEvent;
import com.raytheon.uf.common.registry.handler.IRegistryObjectHandler;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.IPerformanceTimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.common.util.IFileModifiedWatcher;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDbInit;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrieval;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrievalAttributes;
import com.raytheon.uf.edex.datadelivery.bandwidth.hibernate.ISubscriptionFinder;
import com.raytheon.uf.edex.datadelivery.bandwidth.notification.BandwidthEventBus;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalPlan;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.SubscriptionRetrievalFulfilled;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthDaoUtil;
import com.raytheon.uf.edex.registry.ebxml.util.RegistryIdUtil;

/**
 * Implementation of {@link BandwidthManager} that isolates EDEX specific
 * functionality. This keeps things out of the {@link InMemoryBandwidthManager}
 * that could interfere with garbage collection/threading concerns.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 10, 2013 2106       djohnson     Extracted from {@link BandwidthManager}.
 * Jul 11, 2013 2106       djohnson     Look up subscription from the handler directly.
 * Jul 19, 2013 2209       dhladky      Fixed un-serialized subscription for pointData.
 * Sep 13, 2013 2267       bgonzale     Check for no subscription retrieval attribute found.
 * Sep 16, 2013 2383       bgonzale     Add exception information for no subscription found.
 *                                      Add throws to updatePointDataSetMetaData.
 * Oct 1 2013   1797       dhladky      Time and GriddedTime separation
 * Oct 10, 2013 1797       bgonzale     Refactored registry Time objects.
 * 10/23/2013   2385       bphillip     Change schedule method to scheduleAdhoc
 * Nov 04, 2013 2506       bgonzale     Added removeBandwidthSubscriptions method.
 *                                      Added subscriptionNotificationService field.
 *                                      Send notifications.
 * Nov 15, 2013 2545       bgonzale     Added check for subscription events before sending
 *                                      notifications.  Republish dataset metadata registry
 *                                      insert and update events as dataset metadata events.
 * Jan 13, 2014 2679       dhladky      Small Point data updates.   
 * Jan 14, 2014 2692       dhladky      AdhocSubscription handler
 * Jan 20, 2013 2398       dhladky      Fixed rescheduling beyond active period/expired window.                                 
 * Jan 24, 2013 2709       bgonzale     Changed parameter to shouldScheduleForTime to a Calendar.
 * Jan 29, 2014 2636       mpduff       Scheduling refactor.
 * Jan 30, 2014 2686       dhladky      refactor of retrieval.
 * Feb 06, 2014 2636       bgonzale     Added initializeScheduling method that uses the in-memory
 *                                      bandwidth manager to perform the scheduling initialization
 *                                      because of efficiency.
 * Feb 11, 2014 2771       bgonzale     Use Data Delivery ID instead of Site.
 * Feb 10, 2014 2636       mpduff       Pass Network map to be scheduled.
 * Feb 21, 2014, 2636      dhladky      Try catch to keep MaintTask from dying.
 * Mar 31, 2014 2889       dhladky      Added username for notification center tracking.
 * Apr 09, 2014 3012       dhladky      Range the queries for metadata checks, adhoc firing prevention.
 * Apr 22, 2014 2992       dhladky      Added IdUtil for siteList
 * May 22, 2014 2808       dhladky      schedule unscheduled when a sub is deactivated
 * Jul 28, 2014 2752       dhladky      Fixed bad default user for registry.
 * Oct 08, 2014 2746       ccody        Relocated registryEventListener to EdexBandwidthManager super class
 * Oct 15, 2014 3664       ccody        Add notification event for unscheduled Subscriptions at startup
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
public abstract class EdexBandwidthManager<T extends Time, C extends Coverage>
        extends BandwidthManager<T, C> {

    private static final Pattern RAP_PATTERN = Pattern
            .compile(".*rap_f\\d\\d$");

    private final IDataSetMetaDataHandler dataSetMetaDataHandler;

    private final ISubscriptionHandler subscriptionHandler;

    private final IAdhocSubscriptionHandler adhocSubscriptionHandler;

    private final ScheduledExecutorService scheduler;

    private final ISubscriptionNotificationService subscriptionNotificationService;

    private final ISubscriptionFinder<Subscription<T, C>> findSubscriptionsStrategy;

    private static final String UNKNOWN = "UNKNOWN";

    @VisibleForTesting
    final Runnable watchForConfigFileChanges = new Runnable() {

        private final IFileModifiedWatcher fileModifiedWatcher = FileUtil
                .getFileModifiedWatcher(EdexBandwidthContextFactory
                        .getBandwidthMapConfig());

        @Override
        public void run() {
            if (fileModifiedWatcher.hasBeenModified()) {
                bandwidthMapConfigurationUpdated();
            }
        }
    };

    /**
     * @param dbInit
     * @param bandwidthDao
     * @param retrievalManager
     * @param bandwidthDaoUtil
     * @param subscriptionNotificationService
     */
    public EdexBandwidthManager(IBandwidthDbInit dbInit,
            IBandwidthDao<T, C> bandwidthDao,
            RetrievalManager retrievalManager,
            BandwidthDaoUtil<T, C> bandwidthDaoUtil, RegistryIdUtil idUtil,
            IDataSetMetaDataHandler dataSetMetaDataHandler,
            ISubscriptionHandler subscriptionHandler,
            IAdhocSubscriptionHandler adhocSubscriptionHandler,
            ISubscriptionNotificationService subscriptionNotificationService,
            ISubscriptionFinder findSubscriptionsStrategy) {
        super(dbInit, bandwidthDao, retrievalManager, bandwidthDaoUtil, idUtil);

        this.dataSetMetaDataHandler = dataSetMetaDataHandler;
        this.subscriptionHandler = subscriptionHandler;
        this.subscriptionNotificationService = subscriptionNotificationService;
        this.adhocSubscriptionHandler = adhocSubscriptionHandler;
        this.findSubscriptionsStrategy = findSubscriptionsStrategy;

        // schedule maintenance tasks
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public List<String> initializeScheduling(
            Map<Network, List<Subscription>> subMap)
            throws SerializationException {
        List<String> unscheduledNames = new ArrayList<String>(0);

        try {
            for (Network key : subMap.keySet()) {
                List<Subscription<T, C>> subscriptions = new ArrayList<Subscription<T, C>>();
                // this loop is here only because of the generics mess
                for (Subscription<T, C> s : subMap.get(key)) {
                    subscriptions.add(s);
                }
                ProposeScheduleResponse response = proposeScheduleSubscriptions(subscriptions);
                Set<String> unscheduled = response
                        .getUnscheduledSubscriptions();
                List<Subscription<T, C>> unScheduledSubs = new ArrayList<Subscription<T, C>>();

                if (!unscheduled.isEmpty()) {
                    // if proposed was unable to schedule some subscriptions it
                    // will schedule nothing. schedule any that can be scheduled
                    // here.
                    List<Subscription<T, C>> subsToSchedule = new ArrayList<Subscription<T, C>>();
                    for (Subscription<T, C> s : subscriptions) {
                        if (!unscheduled.contains(s.getName())) {
                            subsToSchedule.add(s);
                        } else {
                            unScheduledSubs.add(s);
                        }
                    }

                    unscheduled.addAll(scheduleSubscriptions(subsToSchedule));
                    unscheduledNames.addAll(unscheduled);

                    // Update unscheduled subscriptions to reflect reality of
                    // condition
                    if (!CollectionUtils.isEmpty(unScheduledSubs)) {
                        SubscriptionStatusEvent sse = null;
                        for (Subscription<T, C> sub : unScheduledSubs) {
                            try {
                                sub.setUnscheduled(true);
                                subscriptionHandler.update(
                                        RegistryUtil.defaultUser, sub);
                                sse = new SubscriptionStatusEvent(sub,
                                        " is unscheduled. Insufficient bandwidth at startup.");
                                EventBus.publish(sse);
                            } catch (RegistryHandlerException e) {
                                statusHandler.handle(Priority.PROBLEM,
                                        "Unable to update subscription scheduling status: "
                                                + sub.getName(), e);
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
     * @see com.raytheon.uf.edex.datadelivery.bandwidth.BandwidthManager.resetBandwidthManager
     */
    @Override
    protected void resetBandwidthManager(Network requestNetwork,
            String resetReasonMessage) {

        statusHandler
                .info("EdexBandwidthManager: Restoring Subscriptions for Bandwidth Manager Reset. START");
        Map<Network, List<Subscription<T, C>>> networkToSubscriptionSetMap = null;
        List<Subscription<T, C>> subscriptionList = null;

        if (findSubscriptionsStrategy != null) {
            try {
                networkToSubscriptionSetMap = findSubscriptionsStrategy
                        .findSubscriptionsToSchedule();
            } catch (Exception ex) {
                statusHandler
                        .error("Error occurred searching for subscriptions. Falling back to backup files.",
                                ex);
                networkToSubscriptionSetMap = null;
            }
        }

        if ((networkToSubscriptionSetMap != null)
                && (networkToSubscriptionSetMap.isEmpty() == false)) {

            // Remove ALL BANDWIDTH Subscriptions
            List<BandwidthSubscription> removeBandwidthSubscriptionList = this.bandwidthDao
                    .getBandwidthSubscriptions();
            if (removeBandwidthSubscriptionList.isEmpty() == false) {
                remove(removeBandwidthSubscriptionList);
            }

            // Restore ALL Subscriptions
            String networkName;
            for (Network network : networkToSubscriptionSetMap.keySet()) {
                networkName = network.name();
                subscriptionList = networkToSubscriptionSetMap.get(network);
                if (subscriptionList != null) {
                    for (Subscription<T, C> subscription : subscriptionList) {

                        restoreSubscription(subscription);
                        try {
                            statusHandler.info("\tScheduling: "
                                    + subscription.getName());
                            List<BandwidthAllocation> unscheduledList = schedule(subscription);
                            logSubscriptionListUnscheduled(networkName,
                                    unscheduledList);
                        } catch (Exception ex) {
                            statusHandler.error(
                                    "Error occurred restarting EdexBandwidthManager for Network: "
                                            + network + " Subscription: "
                                            + subscription.getName(), ex);
                        }
                    }
                }
            }
        } else {
            statusHandler
                    .error("Error occurred restarting EdexBandwidthManager: unable to find subscriptions to reschedule");
            return;
        }

        statusHandler
                .info("END EdexBandwidthManager: Restoring Subscriptions for Bandwidth Manager Reset.");

        // Create system status event
        Calendar now = TimeUtil.newGmtCalendar();
        DataDeliverySystemStatusEvent event = new DataDeliverySystemStatusEvent();
        event.setName(requestNetwork.name());
        event.setDate(now);
        event.setSystemType("Bandwidth");
        event.setStatus(DataDeliverySystemStatusDefinition.RESTART);

        EventBus.publish(event);
    }

    private void restoreSubscription(Subscription<T, C> subscription) {

        String subscriptionName = UNKNOWN;
        String networkName = UNKNOWN;
        try {
            subscriptionName = subscription.getName();
            Network subscriptionNetwork = subscription.getRoute();
            if (subscriptionNetwork != null) {
                networkName = subscriptionNetwork.name();
            }
            List<BandwidthAllocation> unscheduledList = null;
            statusHandler.info("\tScheduling: " + subscriptionName);
            unscheduledList = schedule(subscription);
            logSubscriptionListUnscheduled(networkName, unscheduledList);
        } catch (Exception ex) {
            statusHandler.error(
                    "Error occurred restarting EdexBandwidthManager for Network: "
                            + networkName + " Subscription: "
                            + subscriptionName, ex);
        }
    }

    private void logSubscriptionListUnscheduled(String subscriptionNetwork,
            List<BandwidthAllocation> unscheduledList) {
        if (unscheduledList != null) {
            StringBuffer sb = new StringBuffer();
            sb.append("The following subscriptions for Network: ");
            sb.append(subscriptionNetwork);
            sb.append(" remain unscheduled after refresh:\n");
            BandwidthAllocation ba = null;
            for (int i = 0; i < unscheduledList.size(); i++) {
                ba = unscheduledList.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(ba.getId());
            }
            statusHandler.info(sb.toString());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void shutdownInternal() {
        unregisterFromEventBus();
        unregisterFromBandwidthEventBus();

        try {
            scheduler.shutdownNow();
        } catch (Exception e) {
            statusHandler.handle(Priority.WARN,
                    "Unable to shutdown the scheduler.", e);
        } finally {
            // TODO: Remove this finally block when fully switched over to Java
            // 1.7. See TODO comment in the constructor.
            if (scheduler != null
                    && scheduler instanceof ScheduledThreadPoolExecutor) {
                ((ScheduledThreadPoolExecutor) scheduler).getQueue().clear();
            }
        }
        EventBus.unregister(retrievalManager);
    }

    /**
     * Unregister from the {@link EventBus}.
     */
    private void unregisterFromEventBus() {
        EventBus.unregister(this);
    }

    /**
     * Unregister from the {@link BandwidthEventBus}.
     */
    private void unregisterFromBandwidthEventBus() {
        BandwidthEventBus.unregister(this);
    }

    /**
     * The callback method for BandwidthEventBus to use to notify
     * BandwidthManager that retrievalManager has completed the retrievals for a
     * Subscription. The updated BandwidthSubscription Object is placed on the
     * BandwidthEventBus.
     * 
     * @param subscription
     *            The completed subscription.
     */
    @Subscribe
    public void subscriptionFulfilled(
            SubscriptionRetrievalFulfilled subscriptionRetrievalFulfilled) {

        statusHandler.info("subscriptionFulfilled() :: "
                + subscriptionRetrievalFulfilled.getSubscriptionRetrieval());

        SubscriptionRetrieval sr = subscriptionRetrievalFulfilled
                .getSubscriptionRetrieval();

        List<SubscriptionRetrieval> subscriptionRetrievals = bandwidthDao
                .querySubscriptionRetrievals(sr.getBandwidthSubscription());

        List<SubscriptionRetrieval> fulfilledList = new ArrayList<SubscriptionRetrieval>();

        // Look to see if all the SubscriptionRetrieval's for a subscription are
        // completed.
        for (SubscriptionRetrieval subscription : subscriptionRetrievals) {
            if (RetrievalStatus.FULFILLED.equals(subscription.getStatus())) {
                fulfilledList.add(subscription);
            }
        }

        // Remove the completed SubscriptionRetrieval Objects from the
        // plan..
        for (SubscriptionRetrieval fsr : fulfilledList) {
            RetrievalPlan plan = retrievalManager.getPlan(fsr.getNetwork());
            plan.remove(fsr);
            statusHandler.info("Removing fulfilled SubscriptionRetrieval: "
                    + fsr.getId());
        }
    }

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
            if (DataDeliveryRegistryObjectTypes.isRecurringSubscription(event
                    .getObjectType())) {
                statusHandler
                        .info("Received Subscription removal notification for Subscription ["
                                + event.getId() + "]");
                removeBandwidthSubscriptions(event.getId());

                try {
                    Subscription<T, C> sub = (Subscription<T, C>) RegistryEncoders
                            .ofType(JAXB)
                            .decodeObject(event.getRemovedObject());
                    sendSubscriptionNotificationEvent(event, sub);
                } catch (SerializationException e) {
                    statusHandler
                            .handle(Priority.PROBLEM,
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
     * @param re
     *            The <code>InsertRegistryEvent</code> Object to evaluate.
     */
    @SuppressWarnings("unchecked")
    @Subscribe
    @AllowConcurrentEvents
    public void registryEventListener(InsertRegistryEvent re) {
        final String objectType = re.getObjectType();

        if (DataDeliveryRegistryObjectTypes.DATASETMETADATA.equals(objectType)) {
            publishDataSetMetaDataEvent(re);
        }
        if (DataDeliveryRegistryObjectTypes.isRecurringSubscription(re
                .getObjectType())) {
            Subscription<T, C> sub = getRegistryObjectById(subscriptionHandler,
                    re.getId());
            sendSubscriptionNotificationEvent(re, sub);

        }
    }

    /**
     * Listen for Registry update events. Filter for subscription specific
     * events. Sends corresponding subscription notification events.
     * 
     * @param event
     */
    @Subscribe
    @AllowConcurrentEvents
    public void registryEventListener(UpdateRegistryEvent event) {
        final String objectType = event.getObjectType();
        if (DataDeliveryRegistryObjectTypes.DATASETMETADATA.equals(objectType)) {
            publishDataSetMetaDataEvent(event);
        }
        if ((DataDeliveryRegistryObjectTypes.SHARED_SUBSCRIPTION
                .equals(objectType))
                || (DataDeliveryRegistryObjectTypes.SITE_SUBSCRIPTION
                        .equals(objectType))) {
            Subscription<T, C> subscription = getRegistryObjectById(
                    getSubscriptionHandler(), event.getId());
            boolean isLocalOrigination = subscription.getOriginatingSite()
                    .equals(RegistryIdUtil.getId());

            if (isLocalOrigination) {
                subscriptionUpdated(subscription);
            } else {
                sendSubscriptionNotificationEvent(event, subscription);
            }
        }
    }

    protected void sendSubscriptionNotificationEvent(RegistryEvent event,
            Subscription<T, C> sub) {
        final String objectType = event.getObjectType();

        if (DataDeliveryRegistryObjectTypes.isRecurringSubscription(objectType)) {
            if (sub != null) {
                boolean isApplicableForTheLocalSite = sub.getOfficeIDs()
                        .contains(RegistryIdUtil.getId());
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
                        statusHandler.handle(
                                Priority.PROBLEM,
                                "Invalid RegistryEvent action: "
                                        + event.getAction());
                    }
                }
            }
        }
    }

    private void publishDataSetMetaDataEvent(RegistryEvent re) {
        final String id = re.getId();
        DataSetMetaData<T> dsmd = getDataSetMetaData(id);

        if (dsmd != null) {
            // Repost the Object to the BandwidthEventBus to free
            // the notification thread.

            // TODO: A hack to prevent rap_f and rap datasets being
            // Identified as the
            // same dataset...
            Matcher matcher = RAP_PATTERN.matcher(dsmd.getUrl());
            if (matcher.matches()) {
                statusHandler
                        .info("Found rap_f dataset - updating dataset name from ["
                                + dsmd.getDataSetName() + "] to [rap_f]");
                dsmd.setDataSetName("rap_f");
            }

            BandwidthEventBus.publish(dsmd);

        } else {
            statusHandler.error("No DataSetMetaData found for id [" + id + "]");
        }
    }

    @SuppressWarnings("unchecked")
    private DataSetMetaData<T> getDataSetMetaData(String id) {
        return getRegistryObjectById(dataSetMetaDataHandler, id);
    }

    protected static <M> M getRegistryObjectById(
            IRegistryObjectHandler<M> handler, String id) {
        try {
            return handler.getById(id);
        } catch (RegistryHandlerException e) {
            statusHandler.error("Error attempting to retrieve RegistryObject["
                    + id + "] from Registry.", e);
            return null;
        }
    }

    /**
     * @return the subscriptionHandler
     */
    public ISubscriptionHandler getSubscriptionHandler() {
        return subscriptionHandler;
    }
    
    @SuppressWarnings("unchecked")
    @Subscribe 
    public void updatePDADataSetMetaData(PDADataSetMetaData dataSetMetaData) {
        
        bandwidthDao.newBandwidthDataSetUpdate((DataSetMetaData<T>) dataSetMetaData);

        // Looking for active subscriptions to the dataset.
        try {
            @SuppressWarnings("rawtypes")
            List<Subscription> subscriptions = subscriptionHandler
                    .getActiveByDataSetAndProvider(
                            dataSetMetaData.getDataSetName(),
                            dataSetMetaData.getProviderName());

            if (subscriptions.isEmpty()) {
                return;
            }

            statusHandler
                    .info(String
                            .format("Found [%s] subscriptions that will have an "
                                    + "adhoc subscription generated and scheduled for url [%s].",
                                    subscriptions.size(),
                                    dataSetMetaData.getUrl()));

            // Create an adhoc for each one, and schedule it
            for (Subscription<T, C> subscription : subscriptions) {

                // both of these are handled identically, 
                // The only difference is logging.

                if (subscription instanceof SiteSubscription) {
                    if (subscription.getOfficeIDs().contains(
                            RegistryIdUtil.getId())) {

                        Subscription<T, C> sub = updateSubscriptionWithDataSetMetaData(
                                subscription, dataSetMetaData);
                        statusHandler
                                .info("Updating subscription metadata: "
                                        + sub.getName()
                                        + " dataSetMetadata: "
                                        + sub.getDataSetName()
                                        + " scheduling SITE subscription for retrieval.");

                        scheduleAdhoc(new AdhocSubscription<T, C>(
                                (SiteSubscription<T, C>) sub));
                    } else {
                        // Fall through! doesn't belong to this site, so we
                        // won't retrieve it.
                    }

                } else if (subscription instanceof SharedSubscription) {
                    // check to see if this site is the NCF
                    if (RegistryIdUtil.getId().equals(RegistryUtil.defaultUser)) {

                        Subscription<T, C> sub = updateSubscriptionWithDataSetMetaData(
                                subscription, dataSetMetaData);
                        statusHandler
                                .info("Updating subscription metadata: "
                                        + sub.getName()
                                        + " dataSetMetadata: "
                                        + sub.getDataSetName()
                                        + " scheduling SHARED subscription for retrieval.");
                        scheduleAdhoc(new AdhocSubscription<T, C>(
                                (SharedSubscription<T, C>) sub));
                    } else {
                     // Fall through! doesn't belong to this site, so we
                        // won't retrieve it.
                    }
                } else {
                    throw new IllegalStateException(
                            "Unexpected state: Subscription type other than Shared or Site encountered! "
                                    + subscription.getName());
                }
            }
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Failed to lookup subscriptions. dataSet: "+dataSetMetaData.getDataSetName(), e);
        }
    }

    /**
     * Process a {@link GriddedDataSetMetaData} that was received from the event
     * bus.
     * 
     * @param dataSetMetaData
     *            the metadadata
     */
    @SuppressWarnings("unchecked")
    @Subscribe
    public void updateGriddedDataSetMetaData(
            GriddedDataSetMetaData dataSetMetaData) throws ParseException {

        /*
         * Looking for active subscriptions to the dataset. #3707 Simplified the
         * triggering mechanism for Gridded subs. It all but guarantees the
         * retrieval of a given subscription subscribed to a given dataset. This
         * should be the core concept of the #2414 BandwidthManager re-design.
         */

        try {

            @SuppressWarnings("rawtypes")
            List<Subscription> subscriptions = subscriptionHandler
                    .getActiveByDataSetAndProvider(
                            dataSetMetaData.getDataSetName(),
                            dataSetMetaData.getProviderName());

            if (subscriptions.isEmpty()) {
                return;
            }

            statusHandler.info(String.format(
                    "Found [%s] subscriptions subscribed to "
                            + "this dataset, url [%s].", subscriptions.size(),
                    dataSetMetaData.getUrl()));

            // Create an adhoc for each one, and schedule it
            for (Subscription<T, C> subscription : subscriptions) {

                // both of these are handled identically,
                // The only difference is logging.

                if (subscription instanceof SiteSubscription) {
                    if (subscription.getOfficeIDs().contains(
                            RegistryIdUtil.getId())) {

                        Subscription<T, C> sub = updateSubscriptionWithDataSetMetaData(
                                subscription, dataSetMetaData);
                        statusHandler
                                .info("Updating subscription metadata: "
                                        + sub.getName()
                                        + " dataSetMetadata: "
                                        + sub.getDataSetName()
                                        + " scheduling SITE subscription for retrieval.");

                        scheduleAdhoc(new AdhocSubscription<T, C>(
                                (SiteSubscription<T, C>) sub));
                    } else {
                        // Fall through! doesn't belong to this site, so we
                        // won't retrieve it.
                    }

                } else if (subscription instanceof SharedSubscription) {
                    // check to see if this site is the NCF
                    if (RegistryIdUtil.getId().equals(RegistryUtil.defaultUser)) {

                        Subscription<T, C> sub = updateSubscriptionWithDataSetMetaData(
                                subscription, dataSetMetaData);
                        statusHandler
                                .info("Updating subscription metadata: "
                                        + sub.getName()
                                        + " dataSetMetadata: "
                                        + sub.getDataSetName()
                                        + " scheduling SHARED subscription for retrieval.");
                        scheduleAdhoc(new AdhocSubscription<T, C>(
                                (SharedSubscription<T, C>) sub));
                    } else {
                        // Fall through! doesn't belong to this site, so we
                        // won't retrieve it.
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Unexpected state: Subscription type other than Shared or Site encountered! "
                                    + subscription.getName());
                }
            }
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Failed to lookup subscriptions.", e);
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
    @Subscribe
    public void updatePointDataSetMetaData(PointDataSetMetaData dataSetMetaData)
            throws ParseException {

        final PointTime time = dataSetMetaData.getTime();
        final String providerName = dataSetMetaData.getProviderName();
        final String dataSetName = dataSetMetaData.getDataSetName();
        final Date pointTimeStart = time.getStart();
        final Date pointTimeEnd = time.getEnd();

        final SortedSet<Integer> allowedRefreshIntervals = PointTime
                .getAllowedRefreshIntervals();
        final long maxAllowedRefreshIntervalInMillis = TimeUtil.MILLIS_PER_MINUTE
                * allowedRefreshIntervals.last();
        final long minAllowedRefreshIntervalInMillis = TimeUtil.MILLIS_PER_MINUTE
                * allowedRefreshIntervals.first();

        // Find any retrievals ranging from those with the minimum refresh
        // interval to the maximum refresh interval
        final Date startDate = new Date(pointTimeStart.getTime()
                + minAllowedRefreshIntervalInMillis);
        final Date endDate = new Date(pointTimeEnd.getTime()
                + maxAllowedRefreshIntervalInMillis);

        final SortedSet<SubscriptionRetrieval> subscriptionRetrievals = bandwidthDao
                .getSubscriptionRetrievals(providerName, dataSetName,
                        RetrievalStatus.SCHEDULED, startDate, endDate);

        if (!CollectionUtil.isNullOrEmpty(subscriptionRetrievals)) {
            for (SubscriptionRetrieval retrieval : subscriptionRetrievals) {
                // Now check and make sure that at least one of the times falls
                // in their retrieval range, their latency is the retrieval
                // interval
                final int retrievalInterval = retrieval
                        .getSubscriptionLatency();

                // This is the latest time on the data we care about, once the
                // retrieval is signaled to go it retrieves everything up to
                // its start time
                final Date latestRetrievalDataTime = retrieval.getStartTime()
                        .getTime();
                // This is the earliest possible time this retrieval cares about
                final Date earliestRetrievalDataTime = new Date(
                        latestRetrievalDataTime.getTime()
                                - (TimeUtil.MILLIS_PER_MINUTE * retrievalInterval));

                // If the end is before any times we care about or the start is
                // after the latest times we care about, skip it
                if (pointTimeEnd.before(earliestRetrievalDataTime)
                        || pointTimeStart.after(latestRetrievalDataTime)) {
                    continue;
                } else {
                    statusHandler.info("Retrieval:  " + retrieval.toString()
                            + " Outside the range: MIN: "
                            + earliestRetrievalDataTime.toString() + " MAX: "
                            + latestRetrievalDataTime.toString()
                            + " \n No retrieval will be produced!");
                }

                try {
                    // Update the retrieval times on the subscription object
                    // which goes through the retrieval process
                    final SubscriptionRetrievalAttributes<T, C> subscriptionRetrievalAttributes = bandwidthDao
                            .getSubscriptionRetrievalAttributes(retrieval);
                    final Subscription<T, C> subscription = subscriptionRetrievalAttributes
                            .getSubscription();

                    if (subscription.getTime() instanceof PointTime) {
                        final PointTime subTime = (PointTime) subscription
                                .getTime();
                        subscription.setUrl(dataSetMetaData.getUrl());
                        subscription.setProvider(dataSetMetaData
                                .getProviderName());

                        subTime.setRequestStart(earliestRetrievalDataTime);
                        subTime.setRequestEnd(latestRetrievalDataTime);
                        subTime.setTimes(time.getTimes());
                        subscriptionRetrievalAttributes
                                .setSubscription(subscription);

                        bandwidthDao.update(subscriptionRetrievalAttributes);

                        // Now update the retrieval to be ready
                        retrieval.setStatus(RetrievalStatus.READY);
                        bandwidthDaoUtil.update(retrieval);
                    } else {
                        throw new IllegalArgumentException(
                                "Subscription time not PointType! "
                                        + subscription.getName());
                    }

                } catch (SerializationException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            e.getLocalizedMessage(), e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void unscheduleSubscriptionsForAllocations(
            List<BandwidthAllocation> unscheduled) {
        List<SubscriptionRetrieval> retrievals = Lists.newArrayList();
        for (BandwidthAllocation unscheduledAllocation : unscheduled) {
            if (unscheduledAllocation instanceof SubscriptionRetrieval) {
                SubscriptionRetrieval retrieval = (SubscriptionRetrieval) unscheduledAllocation;
                retrievals.add(retrieval);
            }
        }

        Set<Subscription<T, C>> subscriptions = new HashSet<Subscription<T, C>>();
        for (SubscriptionRetrieval retrieval : retrievals) {
            try {
                final SubscriptionRetrievalAttributes<T, C> sra = bandwidthDao
                        .getSubscriptionRetrievalAttributes(retrieval);
                if (sra != null) {
                    Subscription<T, C> sub = sra.getSubscription();
                    if (sub != null) {
                        subscriptions.add(sub);
                    }
                }
            } catch (SerializationException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to deserialize a subscription", e);
                continue;
            }
        }

        for (Subscription<T, C> subscription : subscriptions) {
            boolean origSchedStatus = subscription.isUnscheduled();
            subscription.setUnscheduled(true);

            subscriptionUpdated(subscription);

            if (origSchedStatus == false) {
                SubscriptionStatusEvent sse = new SubscriptionStatusEvent(
                        subscription, " is unscheduled.");
                EventBus.publish(sse);
            }
        }
    }

    /**
     * Updates a {@link Subscription) to reflect important attributes of the
     * specified {@link DataSetMetaData}.
     * 
     * @param sub
     *            the subscription
     * @param dataSetMetaData
     *            the datasetmetadata update
     * @return the subscription
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Subscription updateSubscriptionWithDataSetMetaData(
            Subscription sub, DataSetMetaData dataSetMetaData) {
        // TODO perfect candidate for the factory for time and coverage
        Time dsmdTime = dataSetMetaData.getTime();
        final Time subTime = sub.getTime();
        dsmdTime = handleCyclesAndSequences(subTime, dsmdTime);
        sub.setTime(dsmdTime);
        sub.setUrl(dataSetMetaData.getUrl());

        return sub;
    }

    /**
     * Signals the bandwidth map localization file is updated, perform a
     * reinitialize operation.
     */
    private void bandwidthMapConfigurationUpdated() {
        BandwidthRequest<T, C> request = new BandwidthRequest<T, C>();
        request.setRequestType(RequestType.REINITIALIZE);

        try {
            handleRequest(request);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error while reinitializing the bandwidth manager.", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.edex.datadelivery.bandwidth.BandwidthManager#
     * getSubscriptionsToSchedule
     * (com.raytheon.uf.common.datadelivery.registry.Network)
     */
    @Override
    protected List<Subscription<T, C>> getSubscriptionsToSchedule(
            Network network) {
        List<Subscription<T, C>> subList = new ArrayList<Subscription<T, C>>(0);
        try {
            Map<Network, List<Subscription<T, C>>> activeSubs = findSubscriptionsStrategy
                    .findSubscriptionsToSchedule();
            if (activeSubs.get(network) != null) {
                subList = activeSubs.get(network);
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error retrieving subscriptions.", e);
        }

        return subList;
    }

    /**
     * Try to schedule other subs when another deactivates
     * 
     * @param deactivatedSubName
     */
    @Override
    public void scheduleUnscheduledSubscriptions(String deactivatedSubName) {

        // With the removal of allocations, try now to add any subs that might
        // not have had room to schedule.
        Map<Network, List<Subscription<T, C>>> subMap = null;
        try {
            subMap = findSubscriptionsStrategy.findSubscriptionsToSchedule();
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Problem finding subscriptions that need to be scheduled",
                    e);
        }
        if (subMap != null) {
            statusHandler.info("Finding any unscheduled subscriptions...");
            List<Subscription<T, C>> unschedSubs = new ArrayList<Subscription<T, C>>();
            for (Network route : subMap.keySet()) {
                for (Subscription<T, C> sub : subMap.get(route)) {
                    // look for unscheduled subs, try to schedule them.
                    if (!sub.getName().equals(deactivatedSubName)
                            && ((RecurringSubscription<T, C>) sub)
                                    .shouldSchedule() && sub.isUnscheduled()) {
                        unschedSubs.add(sub);
                    }
                }
            }

            if (!CollectionUtil.isNullOrEmpty(unschedSubs)) {
                boolean modIsUnSched = false;
                String msg = null;
                for (Subscription sub : unschedSubs) {

                    statusHandler
                            .info("Attempting to Schedule unscheduled subscription: "
                                    + sub.getName());
                    List<BandwidthAllocation> unscheduleAllocations = schedule(sub);

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
                        if (modIsUnSched == true) {
                            msg = " is unscheduled.";
                        } else {
                            msg = " is scheduled.";
                        }
                        SubscriptionStatusEvent sse = new SubscriptionStatusEvent(
                                sub, msg);
                        EventBus.publish(sse);
                    } catch (RegistryHandlerException e) {
                        statusHandler
                                .handle(Priority.PROBLEM,
                                        "Couldn't update subscription state in BandwidthManager",
                                        e);
                    }
                }
            } else {
                statusHandler.info("No unscheduled subscriptions found...");
            }
        }
    }

    /**
     * Private inner work thread used to keep the RetrievalPlans up to date.
     */
    private class MaintenanceTask implements Runnable {
        @Override
        public void run() {

            try {

                IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
                timer.start();
                statusHandler.info("MaintenanceTask starting...");

                for (RetrievalPlan plan : retrievalManager.getRetrievalPlans()
                        .values()) {
                    if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                        statusHandler.info("MaintenanceTask: "
                                + plan.getNetwork());
                        statusHandler.info("MaintenanceTask: planStart: "
                                + plan.getPlanStart().getTime());
                        statusHandler.info("MaintenanceTask: planEnd: "
                                + plan.getPlanEnd().getTime());
                    }
                    plan.resize();
                    if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                        statusHandler
                                .info("MaintenanceTask: resized planStart: "
                                        + plan.getPlanStart().getTime());
                        statusHandler.info("MaintenanceTask: resized planEnd: "
                                + plan.getPlanEnd().getTime());
                        statusHandler.info("MaintenanceTask: Update schedule");
                    }
                    // Find DEFERRED Allocations and load them into the
                    // plan...
                    List<BandwidthAllocation> deferred = bandwidthDao
                            .getDeferred(plan.getNetwork(), plan.getPlanEnd());
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
                statusHandler.info("MaintenanceTask complete: "
                        + timer.getElapsed() + " - " + numSubsProcessed
                        + " Subscriptions processed.");

            } catch (Throwable t) {
                statusHandler
                        .error("MaintenanceTask: Subscription update scheduling has failed",
                                t);
            }
        }
    }
}
