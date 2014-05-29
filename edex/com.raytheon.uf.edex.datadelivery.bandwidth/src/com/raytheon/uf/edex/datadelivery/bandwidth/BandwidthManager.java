package com.raytheon.uf.edex.datadelivery.bandwidth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.raytheon.edex.util.Util;
import com.raytheon.uf.common.auth.exception.AuthorizationException;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.datadelivery.bandwidth.BandwidthRequest;
import com.raytheon.uf.common.datadelivery.bandwidth.BandwidthRequest.RequestType;
import com.raytheon.uf.common.datadelivery.bandwidth.IProposeScheduleResponse;
import com.raytheon.uf.common.datadelivery.bandwidth.ProposeScheduleResponse;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthGraphData;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthMap;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthRoute;
import com.raytheon.uf.common.datadelivery.event.retrieval.AdhocSubscriptionRequestEvent;
import com.raytheon.uf.common.datadelivery.event.retrieval.SubscriptionRequestEvent;
import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.PointTime;
import com.raytheon.uf.common.datadelivery.registry.RecurringSubscription;
import com.raytheon.uf.common.datadelivery.registry.SiteSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.Utils.SubscriptionStatus;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.IPerformanceTimer;
import com.raytheon.uf.common.time.util.ITimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.common.util.JarUtil;
import com.raytheon.uf.common.util.LogUtil;
import com.raytheon.uf.common.util.algorithm.AlgorithmUtil;
import com.raytheon.uf.common.util.algorithm.AlgorithmUtil.IBinarySearchResponse;
import com.raytheon.uf.edex.auth.req.AbstractPrivilegedRequestHandler;
import com.raytheon.uf.edex.auth.resp.AuthorizationResponse;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.modes.EDEXModesUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthBucket;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthDataSetUpdate;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDbInit;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrieval;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrievalAttributes;
import com.raytheon.uf.edex.datadelivery.bandwidth.interfaces.BandwidthInitializer;
import com.raytheon.uf.edex.datadelivery.bandwidth.interfaces.ISubscriptionAggregator;
import com.raytheon.uf.edex.datadelivery.bandwidth.processing.BandwidthSubscriptionContainer;
import com.raytheon.uf.edex.datadelivery.bandwidth.processing.SimpleSubscriptionAggregator;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalPlan;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthDaoUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;
import com.raytheon.uf.edex.registry.ebxml.exception.EbxmlRegistryException;
import com.raytheon.uf.edex.registry.ebxml.util.RegistryIdUtil;

/**
 * Abstract {@link IBandwidthManager} implementation which provides core
 * functionality. Intentionally package-private to hide implementation details.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 07, 2012            dhladky      Initial creation
 * Aug 11, 2012 726        jspinks      Modifications for implementing bandwidth management.
 * Oct 10, 2012 0726       djohnson     Changes to support new subscription/adhoc retrievals, use enabled flag,
 *                                      add support for non-cyclic subscriptions.
 * Oct 23, 2012 1286       djohnson     Add more service requests.
 * Nov 20, 2012 1286       djohnson     Add proposing scheduling subscriptions, change some logging to debug.
 * Dec 06, 2012 1397       djohnson     Add ability to get bandwidth graph data.
 * Dec 11, 2012 1403       djohnson     Adhoc subscriptions no longer go to the registry.
 * Dec 12, 2012 1286       djohnson     Remove shutdown hook and finalize().
 * Jan 25, 2013 1528       djohnson     Compare priorities as primitive ints.
 * Jan 28, 2013 1530       djohnson     Unschedule all allocations for a subscription that does not fully schedule.
 * Jan 30, 2013 1501       djohnson     Fix broken calculations for determining required latency.
 * Feb 05, 2013 1580       mpduff       EventBus refactor.
 * Feb 14, 2013 1595       djohnson     Check with BandwidthUtil whether or not to reschedule subscriptions on update.
 * Feb 14, 2013 1596       djohnson     Do not reschedule allocations when a subscription is removed.
 * Feb 20, 2013 1543       djohnson     Add try/catch blocks during the shutdown process.
 * Feb 27, 2013 1644       djohnson     Force sub-classes to provide an implementation for how to schedule SBN routed subscriptions.
 * Mar 11, 2013 1645       djohnson     Watch configuration file for changes.
 * Mar 28, 2013 1841       djohnson     Subscription is now UserSubscription.
 * May 02, 2013 1910       djohnson     Shutdown proposed bandwidth managers in a finally.
 * May 20, 2013 1650       djohnson     Add in capability to find required dataset size.
 * Jun 03, 2013 2038       djohnson     Add base functionality to handle point data type subscriptions.
 * Jun 13, 2013 2095       djohnson     Improve bandwidth manager speed, and add performance logging.
 * Jun 18, 2013 2120       dhladky      Add times to pointtime array
 * Jun 20, 2013 1802       djohnson     Check several times for the metadata for now.
 * Jun 24, 2013 2106       djohnson     Access BandwidthBucket contents through RetrievalPlan.
 * Jul 09, 2013 2038       djohnson     Correct unregisterFromBandwidthEventBus() to actually do it.
 * Jul 10, 2013 2106       djohnson     Move EDEX instance specific code into its own class.
 * Jul 11, 2013 2106       djohnson     Propose changing available bandwidth returns subscription names.
 * Jul 18, 2013 1653       mpduff       Added case GET_SUBSCRIPTION_STATUS.
 * Aug 06, 2013 1654       bgonzale     Added SubscriptionRequestEvents.
 * Sep 11, 2013 2351       dhladky      Fixed adhoc requests for pointdata
 * Sep 17, 2013 2383       bgonzale     Reverted back to how BandwidthManager. handles
 *                                      case for no matching dataset metadata for an 
 *                                      adhoc subscription.
 * Sep 25, 2013 1797       dhladky      separated time from gridded time
 * Oct 23, 2013 2385       bphillip     Change schedule method to scheduleAdhoc
 * Oct 30, 2013 2448       dhladky      Moved methods to TimeUtil. 
 * Nov 04, 2013 2506       bgonzale     Added removeBandwidthSubscriptions method.
 * Nov 19, 2013 2545       bgonzale     changed getBandwidthGraphData to protected.
 * Dec 04, 2013 2566       bgonzale     added method to retrieve and parse spring files for a mode.
 * Dec 11, 2013 2566       bgonzale     fix spring resource resolution.
 * Dec 17, 2013 2636       bgonzale     Changed logging to differentiate the output.
 * Jan 08, 2014 2615       bgonzale     getMostRecent checks subscription time constraints before scheduling.
 *                                      handlePoint method now schedules most recent.
 * Jan 14, 2014 2692       dhladky      Bad Point scheduling final Empty list.                                   
 * Jan 14, 2014 2459       mpduff       Change to subscription status.
 * Jan 25, 2014 2636       mpduff       Don't do an initial adhoc query for a new subscription.
 * Jan 24, 2013 2709       bgonzale     Before scheduling adhoc, check if in active period window.
 * Jan 29, 2014 2636       mpduff       Scheduling refactor.
 * Jan 30, 2014 2686       dhladky      refactor of retrieval.
 * Feb 06, 2014 2636       bgonzale     fix overwrite of unscheduled subscription list.  fix scheduling
 *                                      of already scheduled BandwidthAllocations.
 * Feb 11, 2014 2771       bgonzale     Added handler for GET_DATADELIVERY_ID request.
 * Feb 10, 2014 2636       mpduff       Changed how retrieval plan is updated over time.
 * Apr 02, 2014  2810      dhladky      Priority sorting of subscriptions.
 * Apr 09, 2014 3012       dhladky      Range the querries for metadata checks to subscriptions.
 * Apr 22, 2014 2992       dhladky      Ability to get list of all registry nodes containing data.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */
public abstract class BandwidthManager<T extends Time, C extends Coverage>
        extends AbstractPrivilegedRequestHandler<BandwidthRequest<T, C>>
        implements IBandwidthManager<T, C> {

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(BandwidthManager.class);

    private static final Pattern RES_PATTERN = Pattern.compile("^res");
    
    /** used to query for registry subscription object owners **/
    private static final String objectType = "'urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:com.raytheon.uf.common.datadelivery.registry.%Subscription'";

    // Requires package access so it can be accessed from the maintenance task
    final IBandwidthDao<T, C> bandwidthDao;

    private ISubscriptionAggregator aggregator;

    private BandwidthInitializer initializer;

    protected final BandwidthDaoUtil<T, C> bandwidthDaoUtil;

    private final IBandwidthDbInit dbInit;
    
    /** used for min time range **/
    public static final String MIN_RANGE_TIME = "min";
    private final RegistryIdUtil idUtil;

    
    /** used for max time range **/
    public static final String MAX_RANGE_TIME = "max";

    // Instance variable and not static, because there are multiple child
    // implementation classes which should each have a unique prefix
    private final IPerformanceStatusHandler performanceHandler = PerformanceStatus
            .getHandler(this.getClass().getSimpleName());

    @VisibleForTesting
    final RetrievalManager retrievalManager;

    public BandwidthManager(IBandwidthDbInit dbInit,
            IBandwidthDao<T, C> bandwidthDao,
            RetrievalManager retrievalManager,
            BandwidthDaoUtil<T, C> bandwidthDaoUtil,
            RegistryIdUtil idUtil) {
        this.dbInit = dbInit;
        this.bandwidthDao = bandwidthDao;
        this.retrievalManager = retrievalManager;
        this.bandwidthDaoUtil = bandwidthDaoUtil;
        this.idUtil = idUtil;
    }

    /**
     * Get the list of mode configured spring file names for the named mode.
     * 
     * @param modeName
     *            retrieve the spring files configured for this mode
     * @return list of spring files configured for the given mode
     */
    protected static String[] getSpringFileNamesForMode(String modeName) {
        List<String> fileList = new ArrayList<String>();
        try {
            EDEXModesUtil.extractSpringXmlFiles(fileList, modeName);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to determine spring files for mode " + modeName, e);
        }

        String[] result = new String[fileList.size()];
        int i = 0;
        for (String fileName : fileList) {
            String name = RES_PATTERN.matcher(fileName).replaceFirst("");
            name = JarUtil.getResResourcePath(name);
            result[i++] = name;
            statusHandler.info("Spring file added: " + name + " for mode "
                    + modeName);
        }
        return result;
    }

    private List<BandwidthAllocation> schedule(Subscription<T, C> subscription,
            SortedSet<Integer> cycles) {
        SortedSet<Calendar> retrievalTimes = bandwidthDaoUtil
                .getRetrievalTimes(subscription, cycles);

        return scheduleSubscriptionForRetrievalTimes(subscription,
                retrievalTimes);
    }

    /**
     * Schedules a subscription that specifies a retrieval interval, rather than
     * cycle times.
     * 
     * @param subscription
     *            the subscription
     * @param retrievalInterval
     *            the retrieval interval
     * @return the list of unscheduled subscriptions
     */
    private List<BandwidthAllocation> schedule(Subscription<T, C> subscription,
            int retrievalInterval) {
        SortedSet<Calendar> retrievalTimes = bandwidthDaoUtil
                .getRetrievalTimes(subscription, retrievalInterval);

        return scheduleSubscriptionForRetrievalTimes(subscription,
                retrievalTimes);
    }

    /**
     * Schedule the given subscription for the specified retrieval times.
     * 
     * @param subscription
     *            the subscription
     * @param retrievalTimes
     *            the retrieval times
     * @return the unscheduled subscriptions
     */
    private List<BandwidthAllocation> scheduleSubscriptionForRetrievalTimes(
            Subscription<T, C> subscription, SortedSet<Calendar> retrievalTimes) {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        if (retrievalTimes.isEmpty()) {
            return Collections.emptyList();
        }

        List<BandwidthAllocation> unscheduled = Lists.newArrayList();

        final int numberOfRetrievalTimes = retrievalTimes.size();
        List<BandwidthSubscription> newSubscriptions = Lists
                .newArrayListWithCapacity(numberOfRetrievalTimes);
        statusHandler.info("Scheduling subscription " + subscription.getName());

        for (Calendar retrievalTime : retrievalTimes) {
            if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                statusHandler.info("Scheduling subscription ["
                        + subscription.getName()
                        + String.format(
                                "] retrievalTime [%1$tY%1$tm%1$td%1$tH%1$tM",
                                retrievalTime) + "]");
            }

            // Add the current subscription to the ones BandwidthManager already
            // knows about.
            newSubscriptions.add(BandwidthUtil
                    .getSubscriptionDaoForSubscription(subscription,
                            retrievalTime));
        }
        timer.lap("createBandwidthSubscriptions");

        bandwidthDao.storeBandwidthSubscriptions(newSubscriptions);
        timer.lap("storeBandwidthSubscriptions");

        unscheduled.addAll(aggregate(new BandwidthSubscriptionContainer(
                subscription, newSubscriptions)));
        timer.lap("aggregate");

        timer.stop();
        timer.logLaps("scheduleSubscriptionForRetrievalTimes() subscription ["
                + subscription.getName() + "] retrievalTimes ["
                + retrievalTimes.size() + "]", performanceHandler);

        return unscheduled;
    }

    protected List<BandwidthAllocation> schedule(
            Subscription<T, C> subscription, BandwidthSubscription dao) {
        Calendar retrievalTime = dao.getBaseReferenceTime();

        // Retrieve all the current subscriptions by provider, dataset name and
        // base time.
        List<BandwidthSubscription> subscriptions = bandwidthDao
                .getBandwidthSubscriptions(dao.getProvider(),
                        dao.getDataSetName(), retrievalTime);

        statusHandler.info("Scheduling subscription ["
                + dao.getName()
                + String.format(
                        "] baseReferenceTime %1$tY%1$tm%1$td%1$tH%1$tM",
                        retrievalTime));

        return aggregate(new BandwidthSubscriptionContainer(subscription,
                subscriptions));
    }

    /**
     * Aggregate subscriptions for a given base time and dataset.
     * 
     * @param bandwidthSubscriptions
     *            A List of SubscriptionDaos that have the same base time and
     *            dataset.
     */
    private List<BandwidthAllocation> aggregate(
            BandwidthSubscriptionContainer bandwidthSubscriptions) {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        List<SubscriptionRetrieval> retrievals = getAggregator().aggregate(
                bandwidthSubscriptions);
        timer.lap("aggregator");
        if (CollectionUtil.isNullOrEmpty(retrievals)) {
            return new ArrayList<BandwidthAllocation>(0);
        }

        /*
         * Create a separate list of BandwidthReservations to schedule as the
         * aggregation process may return all subsumedSubscriptionRetrievalsfor
         * the specified Subscription.
         */
        List<SubscriptionRetrieval> reservations = new ArrayList<SubscriptionRetrieval>();
        for (SubscriptionRetrieval retrieval : retrievals) {
            /*
             * New RetrievalRequests will be marked as "PROCESSING" we need to
             * make new BandwidthReservations for these SubscriptionRetrievals.
             */
            /*
             * TODO: How to process "rescheduled" RetrievalRequests in the case
             * where subscription aggregation has determined that an existing
             * subscription has now be subsumed or altered to accommodate a new
             * super set of subscriptions...
             */
            if ((retrieval.getStatus().equals(RetrievalStatus.RESCHEDULE) || retrieval
                    .getStatus().equals(RetrievalStatus.PROCESSING))
                    && !retrieval.isSubsumed()) {

                BandwidthSubscription bandwidthSubscription = retrieval
                        .getBandwidthSubscription();
                Calendar retrievalTime = bandwidthSubscription
                        .getBaseReferenceTime();
                Calendar startTime = TimeUtil.newGmtCalendar(retrievalTime
                        .getTime());
                
                startTime.add(Calendar.MINUTE,
                        retrieval.getDataSetAvailablityDelay());
                int maxLatency = retrieval.getSubscriptionLatency();
                retrieval.setStartTime(startTime);

                Calendar endTime = TimeUtil.newGmtCalendar();
                endTime.setTimeInMillis(startTime.getTimeInMillis());

                if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                    statusHandler.debug("Adding latency minutes of ["
                            + maxLatency
                            + "] to start time of "
                            + String.format("[%1$tY%1$tm%1$td%1$tH%1$tM]",
                                    startTime));
                }

                endTime.add(Calendar.MINUTE, maxLatency);
                retrieval.setEndTime(endTime);

                // Add SubscriptionRetrieval to the list to schedule..
                reservations.add(retrieval);
            }
        }
        timer.lap("creating retrievals");

        for (SubscriptionRetrieval retrieval : reservations) {
            BandwidthSubscription bandwidthSubscription = retrieval
                    .getBandwidthSubscription();
            if (bandwidthSubscription.isCheckForDataSetUpdate()) {
                // Check to see if the data subscribed to is available..
                // if so, mark the status of the BandwidthReservation as
                // READY.
                List<BandwidthDataSetUpdate> z = bandwidthDao
                        .getBandwidthDataSetUpdate(
                                bandwidthSubscription.getProvider(),
                                bandwidthSubscription.getDataSetName(),
                                bandwidthSubscription.getBaseReferenceTime());
                if (!z.isEmpty()) {
                    retrieval.setStatus(RetrievalStatus.READY);
                }
            }
        }
        timer.lap("checking if ready");

        bandwidthDao.store(reservations);
        timer.lap("storing retrievals");

        List<SubscriptionRetrievalAttributes<T, C>> attributes = Lists
                .newArrayListWithCapacity(reservations.size());
        for (SubscriptionRetrieval retrieval : reservations) {
            final SubscriptionRetrievalAttributes<T, C> attribute = new SubscriptionRetrievalAttributes<T, C>();
            try {
                attribute.setSubscription(bandwidthSubscriptions.subscription);
            } catch (SerializationException e) {
                throw new IllegalStateException(
                        "Unable to serialize the subscription, these retrievals will not be processed!");
            }
            attribute.setSubscriptionRetrieval(retrieval);
            attributes.add(attribute);
        }

        bandwidthDao.storeSubscriptionRetrievalAttributes(attributes);
        timer.lap("storing retrieval attributes");

        List<BandwidthAllocation> unscheduled = (reservations.isEmpty()) ? Collections
                .<BandwidthAllocation> emptyList() : retrievalManager
                .schedule(reservations);
        timer.lap("scheduling retrievals");

        timer.stop();
        final int numberOfBandwidthSubscriptions = bandwidthSubscriptions.newSubscriptions
                .size();
        timer.logLaps("aggregate() bandwidthSubscriptions ["
                + numberOfBandwidthSubscriptions + "]", performanceHandler);

        return unscheduled;
    }

    @Override
    public List<BandwidthAllocation> schedule(Subscription<T, C> subscription) {

        List<BandwidthAllocation> unscheduled = Collections.emptyList();
        if (subscription instanceof RecurringSubscription) {
            if (!((RecurringSubscription<T, C>) subscription).shouldSchedule()) {
                return unscheduled;
            }
        }
        
        final DataType dataSetType = subscription.getDataSetType();
        switch (dataSetType) {
        case GRID:
            unscheduled = handleGridded(subscription);
            break;
        case POINT:
            unscheduled = handlePoint(subscription);
            break;
        default:
            throw new IllegalArgumentException(
                    "The BandwidthManager doesn't know how to treat subscriptions with data type ["
                            + dataSetType + "]!");
        }

        unscheduleSubscriptionsForAllocations(unscheduled);

        return unscheduled;
    }
    
    /**
     * Update the retrieval plan for this subscription.
     * 
     * @param subscription
     *            The subscription that needs its scheduling updated
     */
    private void updateSchedule(Subscription subscription) {
        final DataType dataSetType = subscription.getDataSetType();
        switch (dataSetType) {
        case GRID:
            updateGriddedSchedule(subscription);
            break;
        case POINT:
            updatePointSchedule(subscription);
            break;
        default:
            throw new IllegalArgumentException(
                    "The BandwidthManager doesn't know how to treat subscriptions with data type ["
                            + dataSetType + "]!");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.edex.datadelivery.bandwidth.IBandwidthManager#
     * updateSchedule(com.raytheon.uf.common.datadelivery.registry.Network)
     */
    @Override
    public int updateSchedule(Network network) {
        List<Subscription> subsToSchedule = getSubscriptionsToSchedule(network);
        if (CollectionUtil.isNullOrEmpty(subsToSchedule)) {
            return 0;
        }

        for (Subscription subscription : subsToSchedule) {
            updateSchedule(subscription);
        }

        return subsToSchedule.size();
    }

    /**
     * Get the subscriptions to schedule for the given network.
     * 
     * @param network
     *            The network
     * @return List of subscriptions for the network
     */
    protected abstract List<Subscription> getSubscriptionsToSchedule(
            Network network);

    /**
     * Unschedules all subscriptions the allocations are associated to.
     * 
     * @param unscheduled
     *            the unscheduled allocations
     */
    protected abstract void unscheduleSubscriptionsForAllocations(
            List<BandwidthAllocation> unscheduled);

    /**
     * {@inheritDoc}
     * 
     * @return
     */
    @Override
    public List<BandwidthAllocation> scheduleAdhoc(
            AdhocSubscription<T, C> subscription) {
        return scheduleAdhoc(subscription, BandwidthUtil.now());
    }

    /**
     * {@inheritDoc}
     * 
     * @return
     */
    @Override
    public List<BandwidthAllocation> scheduleAdhoc(
            AdhocSubscription<T, C> subscription, Calendar now) {

        List<BandwidthSubscription> subscriptions = new ArrayList<BandwidthSubscription>();
        // Store the AdhocSubscription with a base time of now..
        subscriptions.add(bandwidthDao.newBandwidthSubscription(subscription,
                now));
        /**
         * This check allows for retrieval of data older than current for grid.
         * This is not allowed for pointdata types, they must grab current URL
         * and time.
         */
        AdhocSubscription<T, C> subscriptionUpdated = bandwidthDaoUtil
                .setAdhocMostRecentUrlAndTime(subscription, true);
        if (subscriptionUpdated != null) {
            subscription = subscriptionUpdated;
        }

        // Use SimpleSubscriptionAggregator (i.e. no aggregation) to generate a
        // SubscriptionRetrieval for this AdhocSubscription
        SimpleSubscriptionAggregator a = new SimpleSubscriptionAggregator(
                bandwidthDao);
        List<BandwidthAllocation> reservations = new ArrayList<BandwidthAllocation>();
        List<SubscriptionRetrieval> retrievals = a
                .aggregate(new BandwidthSubscriptionContainer(subscription,
                        subscriptions));

        for (SubscriptionRetrieval retrieval : retrievals) {
            retrieval.setStartTime(now);
            Calendar endTime = TimeUtil.newCalendar(now);
            endTime.add(Calendar.MINUTE, retrieval.getSubscriptionLatency());
            retrieval.setEndTime(endTime);
            // Store the SubscriptionRetrieval - retrievalManager expects
            // the BandwidthAllocations to already be stored.
            bandwidthDao.store(retrieval);
            reservations.add(retrieval);
        }

        List<BandwidthAllocation> unscheduled = retrievalManager
                .schedule(reservations);

        // Now iterate the SubscriptionRetrievals and for the
        // Allocations that were scheduled, set the status to READY
        // and notify the retrievalManager.
        for (SubscriptionRetrieval retrieval : retrievals) {

            if (retrieval.getStatus().equals(RetrievalStatus.SCHEDULED)) {
                SubscriptionRetrievalAttributes<T, C> attributes = new SubscriptionRetrievalAttributes<T, C>();
                attributes.setSubscriptionRetrieval(retrieval);

                try {
                    attributes.setSubscription(subscription);

                    bandwidthDao.store(attributes);
                } catch (SerializationException e) {
                    statusHandler.error(
                            "Trapped Exception trying to schedule AdhocSubscription["
                                    + subscription.getName() + "]", e);
                    return Collections.emptyList();
                }

                retrieval.setStatus(RetrievalStatus.READY);
                bandwidthDaoUtil.update(retrieval);
            }
        }

        return unscheduled;
    }

    /**
     * {@inheritDoc}
     * 
     * @return
     */
    @Override
    public List<BandwidthAllocation> subscriptionUpdated(
            Subscription<T, C> subscription) {
        // Since AdhocSubscription extends Subscription it is not possible to
        // separate the processing of those Objects in EventBus. So, handle the
        // case where the updated subscription is actually an AdhocSubscription
        if (subscription instanceof AdhocSubscription) {
            return adhocSubscription((AdhocSubscription<T, C>) subscription);
        }
        // Dealing with a 'normal' subscription
        else {
            // First see if BandwidthManager has seen the subscription before.
            List<BandwidthSubscription> bandwidthSubscriptions = bandwidthDao
                    .getBandwidthSubscription(subscription);

            // If BandwidthManager does not know about the subscription, and
            // it's active, attempt to add it..
            if (bandwidthSubscriptions.isEmpty()
                    && ((RecurringSubscription) subscription).shouldSchedule()
                    && !subscription.isUnscheduled()) {
                return schedule(subscription);
            } else if (subscription.getStatus() == SubscriptionStatus.DEACTIVATED
                    || subscription.isUnscheduled()) {
                // See if the subscription was deactivated or unscheduled..
                // Need to remove BandwidthReservations for this
                // subscription.
                return remove(bandwidthSubscriptions);
            } else {
                // Normal update, unschedule old allocations and create new ones
                List<BandwidthAllocation> unscheduled = remove(bandwidthSubscriptions);
                unscheduled.addAll(schedule(subscription));
                return unscheduled;
            }
        }
    }

    /**
     * Update this point subscription's schedule.
     * 
     * @param Subscription
     *            The subscription that needs its schedule updated
     */
    private void updatePointSchedule(Subscription sub) {
        SortedSet<Calendar> retrievalTimes = bandwidthDaoUtil
                .getRetrievalTimes(sub,
                        ((PointTime) sub.getTime()).getInterval());

        scheduleUpdates(sub, retrievalTimes);
    }

    /**
     * Update this grid subscription's schedule.
     * 
     * @param Subscription
     *            The subscription that needs its schedule updated
     */
    private void updateGriddedSchedule(Subscription sub) {
        final List<Integer> cycles = ((GriddedTime) sub.getTime())
                .getCycleTimes();

        SortedSet<Calendar> retrievalTimes = bandwidthDaoUtil
                .getRetrievalTimes(sub, Sets.newTreeSet(cycles));
        scheduleUpdates(sub, retrievalTimes);
    }

    /**
     * Schedule retrievals for this subscription for the provided retrieval
     * times.
     * 
     * @param sub
     *            The subscription
     * @param retrievalTimes
     *            The retrieval times
     */
    private void scheduleUpdates(Subscription sub,
            SortedSet<Calendar> retrievalTimes) {
        List<BandwidthSubscription> currentBandwidthSubscriptions = bandwidthDao
                .getBandwidthSubscription(sub);

        // Remove any times already associated with a retrieval allocation
        for (BandwidthSubscription bs : currentBandwidthSubscriptions) {
            retrievalTimes.remove(bs.getBaseReferenceTime());
        }

        if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
            statusHandler.info("Scheduling " + sub.getName());
            for (Calendar c : retrievalTimes) {
                statusHandler.info("Scheduling for " + c.getTime());
            }
        }

        scheduleSubscriptionForRetrievalTimes(sub, retrievalTimes);
    }

    /**
     * Handle scheduling point data type subscriptions.
     * 
     * @param subscription
     *            the subscription
     * @return the list of unscheduled subscriptions
     */
    private List<BandwidthAllocation> handlePoint(
            Subscription<T, C> subscription) {
        List<BandwidthAllocation> unscheduled = schedule(subscription,
                ((PointTime) subscription.getTime()).getInterval());
        // add an adhoc if one exists and isn't in startup mode
        if (EDEXUtil.isRunning()) {
            unscheduled.addAll(getMostRecent(subscription, false));
        }
        return unscheduled;
    }

    /**
     * Handle scheduling grid data type subscriptions.
     * 
     * @param subscription
     *            the subscription
     * @return the list of unscheduled subscriptions
     */
    private List<BandwidthAllocation> handleGridded(
            Subscription<T, C> subscription) {
        final List<Integer> cycles = ((GriddedTime) subscription.getTime())
                .getCycleTimes();
        final boolean subscribedToCycles = !CollectionUtil
                .isNullOrEmpty(cycles);

        // The subscription has cycles, so we can allocate bandwidth at
        // expected times
        List<BandwidthAllocation> unscheduled = Collections.emptyList();
        if (subscribedToCycles) {
            unscheduled = schedule(subscription, Sets.newTreeSet(cycles));
        }
        // add an adhoc if one exists and isn't in startup mode
        if (EDEXUtil.isRunning()) {
            unscheduled.addAll(getMostRecent(subscription, true));
        }   

        return unscheduled;
    }

    /**
     * Schedule the most recent dataset update if one exists.
     * @param subscription
     * @param useMostRecentDataSetUpdate
     * @return
     */
    private List<BandwidthAllocation> getMostRecent(
            Subscription<T, C> subscription, boolean useMostRecentDataSetUpdate) {
        List<BandwidthAllocation> unscheduled = Collections.emptyList();
        // Create an adhoc subscription based on the new subscription,
        // and set it to retrieve the most recent cycle (or most recent
        // url if a daily product)
        if (subscription instanceof SiteSubscription && subscription.isActive()) {
            AdhocSubscription<T, C> adhoc = new AdhocSubscription<T, C>(
                    (SiteSubscription<T, C>) subscription);
            adhoc = bandwidthDaoUtil.setAdhocMostRecentUrlAndTime(adhoc,
                    useMostRecentDataSetUpdate);

            if (adhoc == null) {
                statusHandler
                        .info(String
                                .format("There wasn't applicable most recent dataset metadata to use for new subscription [%s].  "
                                        + "No adhoc requested.",
                                        subscription.getName()));
            } else {
                RetrievalPlan plan = retrievalManager.getPlan(subscription
                        .getRoute());
                if (plan != null) {
                    Date subscriptionValidStart = subscription.calculateStart(
                            plan.getPlanStart()).getTime();
                    Date subscriptionValidEnd = subscription.calculateEnd(
                            plan.getPlanEnd()).getTime();
                    Calendar nowCalendar = TimeUtil.newCalendar();
                    Date now = nowCalendar.getTime();

                    if ((now.equals(subscriptionValidStart) || now
                            .after(subscriptionValidStart))
                            && now.before(subscriptionValidEnd)
                            && subscription.inActivePeriodWindow(nowCalendar)) {
                        unscheduled = scheduleAdhoc(adhoc);
                    } else {
                        statusHandler.info(String.format(
                                "Time frame outside of subscription active time frame [%s].  "
                                        + "No adhoc requested.",
                                subscription.getName()));
                    }
                }
            }
        } else {
            statusHandler
                    .warn("Unable to create adhoc queries for shared subscriptions at this point.  This functionality should be added in the future...");
        }
        return unscheduled;
    }

    /**
     * {@inheritDoc}
     * 
     * @return
     */
    @Override
    public List<BandwidthAllocation> adhocSubscription(
            AdhocSubscription<T, C> adhoc) {
        statusHandler.info("Scheduling adhoc subscription [" + adhoc.getName()
                + "]");
        return scheduleAdhoc(adhoc);
    }

    /**
     * Remove BandwidthSubscription's (and dependent Objects) from any
     * RetrievalPlans they are in and adjust the RetrievalPlans accordingly.
     * 
     * @param bandwidthSubscriptions
     *            The subscriptionDao's to remove.
     * @return
     */
    protected List<BandwidthAllocation> remove(
            List<BandwidthSubscription> bandwidthSubscriptions) {

        List<BandwidthAllocation> unscheduled = new ArrayList<BandwidthAllocation>();

        for (BandwidthSubscription bandwidthSubscription : bandwidthSubscriptions) {
            bandwidthDaoUtil.remove(bandwidthSubscription);
        }

        return unscheduled;
    }

    /**
     * Remove bandwidth subscriptions for the given id.
     * 
     * @param subscriptionId
     *            the bandwidth subscriptions to remove
     */
    protected void removeBandwidthSubscriptions(String subscriptionId) {
        statusHandler
                .info("Received Subscription removal notification for Subscription ["
                        + subscriptionId
                        + "], removing BandwidthSubscriptions.");
        // Need to locate and remove all BandwidthReservations for the
        // given subscription..
        List<BandwidthSubscription> l = bandwidthDao
                .getBandwidthSubscriptionByRegistryId(subscriptionId);
        if (!l.isEmpty()) {
            remove(l);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAggregator(ISubscriptionAggregator aggregator) {
        this.aggregator = aggregator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ISubscriptionAggregator getAggregator() {
        return aggregator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object handleRequest(BandwidthRequest<T, C> request)
            throws Exception {

        ITimer timer = TimeUtil.getTimer();
        timer.start();

        Object response = null;

        final Network requestNetwork = request.getNetwork();
        final int bandwidth = request.getBandwidth();

        final List<Subscription<T, C>> subscriptions = request
                .getSubscriptions();
        final RequestType requestType = request.getRequestType();
        switch (requestType) {
        case GET_ESTIMATED_COMPLETION:
            Subscription<T, C> adhocAsSub = null;
            if (subscriptions.size() != 1
                    || (!((adhocAsSub = subscriptions.get(0)) instanceof AdhocSubscription))) {
                throw new IllegalArgumentException(
                        "Must supply one, and only one, adhoc subscription to get the estimated completion time.");
            }
            response = getEstimatedCompletionTime((AdhocSubscription<T, C>) adhocAsSub);
            break;
        case REINITIALIZE:
            response = startNewBandwidthManager();
            break;
        case RETRIEVAL_PLAN:
            response = showRetrievalPlan(requestNetwork);
            break;
        case PROPOSE_SCHEDULE_SUBSCRIPTION:
            // SBN subscriptions must go through the NCF
            if (!subscriptions.isEmpty()
                    && Network.SBN.equals(subscriptions.get(0).getRoute())) {
                final IProposeScheduleResponse proposeResponse = proposeScheduleSbnSubscription(subscriptions);
                response = proposeResponse;
            } else {
                // OPSNET subscriptions
                response = proposeScheduleSubscriptions(subscriptions);
            }
            break;
        case SCHEDULE_SUBSCRIPTION:
            // SBN subscriptions must go through the NCF
            if (!subscriptions.isEmpty()
                    && Network.SBN.equals(subscriptions.get(0).getRoute())) {
                response = scheduleSbnSubscriptions(subscriptions);
            } else {
                // OPSNET subscriptions
                response = scheduleSubscriptions(subscriptions);
            }
            break;
        case GET_BANDWIDTH:
            RetrievalPlan b = retrievalManager.getPlan(requestNetwork);
            if (b != null) {
                response = b.getDefaultBandwidth();
            } else {
                response = null;
            }
            break;
        case PROPOSE_SET_BANDWIDTH:
            Set<String> unscheduledSubscriptions = proposeSetBandwidth(
                    requestNetwork, bandwidth);
            response = unscheduledSubscriptions;
            if (unscheduledSubscriptions.isEmpty()) {
                statusHandler
                        .info("No subscriptions will be unscheduled by changing the bandwidth for network ["
                                + requestNetwork
                                + "] to ["
                                + bandwidth
                                + "].  Applying...");
                // This is a safe operation as all subscriptions will remain
                // scheduled, just apply
                setBandwidth(requestNetwork, bandwidth);
            }

            break;
        case FORCE_SET_BANDWIDTH:
            boolean setBandwidth = setBandwidth(requestNetwork, bandwidth);
            response = setBandwidth;
            break;
        case SHOW_ALLOCATION:
            break;

        case SHOW_BUCKET:

            long bucketId = request.getId();
            RetrievalPlan plan = retrievalManager.getPlan(requestNetwork);
            BandwidthBucket bucket = plan.getBucket(bucketId);
            response = plan.showBucket(bucket);
            break;

        case SHOW_DEFERRED:
            StringBuilder sb = new StringBuilder();
            List<BandwidthAllocation> z = bandwidthDao.getDeferred(
                    requestNetwork, request.getBegin());
            for (BandwidthAllocation allocation : z) {
                sb.append(allocation).append("\n");
            }
            response = sb.toString();
            break;
        case GET_BANDWIDTH_GRAPH_DATA:
            response = getBandwidthGraphData();
            break;
        case GET_DATADELIVERY_ID:
            response = RegistryIdUtil.getId();
            break;
        case GET_DATADELIVERY_REGISTRIES:
            response = idUtil.getUniqueRegistries(objectType);
            break;
        case GET_SUBSCRIPTION_STATUS:
            Subscription<T, C> sub = null;
            if (subscriptions.size() != 1
                    || (!((sub = subscriptions.get(0)) instanceof Subscription))) {
                throw new IllegalArgumentException(
                        "Must supply one, and only one, subscription to get the status summary.");
            }
            response = bandwidthDao.getSubscriptionStatusSummary(sub);
            break;
        default:
            throw new IllegalArgumentException(
                    "Dont know how to handle request type [" + requestType
                            + "]");
        }

        // Clean up any in-memory bandwidth configuration files
        InMemoryBandwidthContextFactory.deleteInMemoryBandwidthConfigFile();

        timer.stop();

        statusHandler.info("Processed request of type [" + requestType
                + "] in [" + timer.getElapsedTime() + "] ms");

        return response;
    }

    /**
     * Schedule the SBN subscriptions.
     * 
     * @param subscriptions
     *            the subscriptions
     * @return the set of subscription names unscheduled as a result of
     *         scheduling the subscriptions
     * @throws SerializationException
     */
    protected abstract Set<String> scheduleSbnSubscriptions(
            List<Subscription<T, C>> subscriptions)
            throws SerializationException;

    /**
     * Proposes scheduling a list of subscriptions.
     * 
     * @param subscriptions
     *            the subscriptions
     * @return the response
     * @throws SerializationException
     */
    protected ProposeScheduleResponse proposeScheduleSubscriptions(
            List<Subscription<T, C>> subscriptions)
            throws SerializationException {
        final ProposeScheduleResponse proposeResponse = proposeSchedule(subscriptions);
        Set<String> subscriptionsUnscheduled = proposeResponse
                .getUnscheduledSubscriptions();
        if (subscriptionsUnscheduled.isEmpty()) {
            statusHandler
                    .info("No subscriptions will be unscheduled by scheduling subscriptions "
                            + subscriptions + ".  Applying...");
            // This is a safe operation as all subscriptions will remain
            // scheduled, just apply
            scheduleSubscriptions(subscriptions);
        } else if (subscriptions.size() == 1) {
            final Subscription<T, C> subscription = subscriptions.get(0);
            int requiredLatency = determineRequiredLatency(subscription);
            proposeResponse.setRequiredLatency(requiredLatency);
            long requiredDataSetSize = determineRequiredDataSetSize(subscription);
            proposeResponse.setRequiredDataSetSize(requiredDataSetSize);
        }
        return proposeResponse;
    }

    /**
     * Propose scheduling SBN routed subscriptions. Sub-classes must implement
     * the specific functionality.
     * 
     * @param subscriptions
     *            the subscriptions targeted at the SBN
     * @return the response
     * @throws Exception
     *             on error
     */
    protected abstract IProposeScheduleResponse proposeScheduleSbnSubscription(
            List<Subscription<T, C>> subscriptions) throws Exception;

    /**
     * Retrieve the bandwidth graph data.
     * 
     * @return the graph data
     */
    protected BandwidthGraphData getBandwidthGraphData() {
        return new BandwidthGraphDataAdapter(retrievalManager).get();
    }

    /**
     * Get the estimated completion time for an adhoc subscription.
     * 
     * @param subscription
     *            the subscription
     * @return the estimated completion time
     */
    private Date getEstimatedCompletionTime(AdhocSubscription<T, C> subscription) {
        final List<BandwidthSubscription> bandwidthSubscriptions = bandwidthDao
                .getBandwidthSubscriptionByRegistryId(subscription.getId());

        if (bandwidthSubscriptions.isEmpty()) {
            statusHandler
                    .warn("Unable to find subscriptionDaos for subscription ["
                            + subscription + "].  Returning current time.");
            return new Date();
        } else if (bandwidthSubscriptions.size() > 1) {
            statusHandler
                    .warn("Found more than one subscriptionDaos for subscription ["
                            + subscription
                            + "].  Ignoring list and using the first item.");
        }

        BandwidthSubscription bandwidthSubscription = bandwidthSubscriptions
                .get(0);
        long id = bandwidthSubscription.getId();

        final List<BandwidthAllocation> bandwidthAllocations = bandwidthDao
                .getBandwidthAllocations(id);

        if (bandwidthAllocations.isEmpty()) {
            statusHandler
                    .warn("Unable to find bandwidthAllocations for subscription ["
                            + subscription + "].  Returning current time.");
            return new Date();
        }

        Calendar latest = null;
        for (BandwidthAllocation allocation : bandwidthAllocations) {
            final Calendar endTime = allocation.getEndTime();
            if (latest == null || endTime.after(latest)) {
                latest = endTime;
            }
        }

        return latest.getTime();
    }

    /**
     * Schedule the list of subscriptions.
     * 
     * @param subscriptions
     *            the subscriptions
     * @return the set of subscription names unscheduled
     * @throws SerializationException
     */
    protected Set<String> scheduleSubscriptions(
            List<Subscription<T, C>> insubscriptions)
            throws SerializationException {
        
        Set<String> unscheduledSubscriptions = new TreeSet<String>();
        Set<BandwidthAllocation> unscheduledAllocations = new HashSet<BandwidthAllocation>();
        Map<String, SubscriptionRequestEvent> subscriptionEventsMap = new HashMap<String, SubscriptionRequestEvent>();
        
        // Order list by Subscription Priority
        // We want highest priority subscriptions scheduled first.
        List<Subscription<T, C>> subscriptions = new ArrayList<Subscription<T,C>>(insubscriptions.size());
        subscriptions.addAll(insubscriptions);
        Collections.sort(subscriptions);

        for (Subscription<T, C> subscription : subscriptions) {
            List<BandwidthAllocation> unscheduled = subscriptionUpdated(subscription);
            unscheduledAllocations.addAll(unscheduled);

            /*
             * Create a subscription event or increment an existing event's
             * count.
             */
            String key = new StringBuilder(subscription.getId())
                    .append(subscription.getOwner())
                    .append(subscription.getRoute().toString())
                    .append(subscription.getProvider()).toString();
            SubscriptionRequestEvent event = subscriptionEventsMap.get(key);
            if (event == null) {
                if (subscription instanceof AdhocSubscription) {
                    event = new AdhocSubscriptionRequestEvent();
                } else {
                    event = new SubscriptionRequestEvent();
                }
                event.setId(subscription.getId());
                event.setOwner(subscription.getOwner());
                event.setNetwork(subscription.getRoute().toString());
                event.setProvider(subscription.getProvider());
                subscriptionEventsMap.put(key, event);
            } else {
                event.incrementNumRecords();
            }
        }

        /*
         * publish the subscription events.
         */
        for (SubscriptionRequestEvent event : subscriptionEventsMap.values()) {
            EventBus.publish(event);
        }

        for (BandwidthAllocation allocation : unscheduledAllocations) {
            if (allocation instanceof SubscriptionRetrieval) {
                SubscriptionRetrieval retrieval = (SubscriptionRetrieval) allocation;
                unscheduledSubscriptions.add(retrieval
                        .getBandwidthSubscription().getName());
            }
        }

        return unscheduledSubscriptions;
    }

    /**
     * Sets the bandwidth for a network to the specified value.
     * 
     * @param requestNetwork
     *            the network
     * @param bandwidth
     *            the bandwidth
     * @return true on success, false otherwise
     * @throws SerializationException
     *             on error serializing
     */
    private boolean setBandwidth(Network requestNetwork, int bandwidth)
            throws SerializationException {
        RetrievalPlan c = retrievalManager.getPlan(requestNetwork);
        if (c != null) {
            c.setDefaultBandwidth(bandwidth);

            return startNewBandwidthManager();
        }
        return false;
    }

    /**
     * Propose scheduling the subscriptions.
     * 
     * @param subscriptions
     *            the subscriptions
     * @return the response
     * @throws SerializationException
     */
    private ProposeScheduleResponse proposeSchedule(
            List<Subscription<T, C>> subscriptions)
            throws SerializationException {
        BandwidthMap copyOfCurrentMap = BandwidthMap
                .load(EdexBandwidthContextFactory.getBandwidthMapConfig());

        Set<String> unscheduled = Collections.emptySet();
        BandwidthManager<T, C> proposedBwManager = null;
        try {
            proposedBwManager = startProposedBandwidthManager(copyOfCurrentMap);

            unscheduled = proposedBwManager
                    .scheduleSubscriptions(subscriptions);
        } finally {
            nullSafeShutdown(proposedBwManager);
        }

        final ProposeScheduleResponse proposeScheduleResponse = new ProposeScheduleResponse();
        proposeScheduleResponse.setUnscheduledSubscriptions(unscheduled);

        return proposeScheduleResponse;
    }

    /**
     * Shutdown if not null.
     * 
     * @param bandwidthManager
     *            the bandwidth manager
     */
    private void nullSafeShutdown(BandwidthManager<T, C> bandwidthManager) {
        if (bandwidthManager != null) {
            bandwidthManager.shutdown();
        }
    }

    /**
     * Propose changing a route's bandwidth to the specified amount.
     * 
     * @return the subscriptions that would be unscheduled after setting the
     *         bandwidth
     * 
     * @throws SerializationException
     */
    private Set<String> proposeSetBandwidth(Network requestNetwork,
            int bandwidth) throws SerializationException {
        BandwidthMap copyOfCurrentMap = BandwidthMap
                .load(EdexBandwidthContextFactory.getBandwidthMapConfig());
        BandwidthRoute route = copyOfCurrentMap.getRoute(requestNetwork);
        route.setDefaultBandwidth(bandwidth);

        Set<String> subscriptions = new HashSet<String>();
        BandwidthManager<T, C> proposedBwManager = null;
        try {
            proposedBwManager = startProposedBandwidthManager(copyOfCurrentMap);

            if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                statusHandler.debug("Current retrieval plan:" + Util.EOL
                        + showRetrievalPlan(requestNetwork) + Util.EOL
                        + "Proposed retrieval plan:" + Util.EOL
                        + proposedBwManager.showRetrievalPlan(requestNetwork));
            }

            List<BandwidthAllocation> unscheduledAllocations = proposedBwManager.bandwidthDao
                    .getBandwidthAllocationsInState(RetrievalStatus.UNSCHEDULED);

            if (!unscheduledAllocations.isEmpty()
                    && statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                LogUtil.logIterable(
                        statusHandler,
                        Priority.DEBUG,
                        "The following unscheduled allocations would occur with the proposed bandwidth:",
                        unscheduledAllocations);
            }

            for (BandwidthAllocation allocation : unscheduledAllocations) {
                if (allocation instanceof SubscriptionRetrieval) {
                    subscriptions.add(((SubscriptionRetrieval) allocation)
                            .getBandwidthSubscription().getName());
                }
            }

            if (!subscriptions.isEmpty()
                    && statusHandler.isPriorityEnabled(Priority.INFO)) {
                LogUtil.logIterable(
                        statusHandler,
                        Priority.INFO,
                        "The following subscriptions would not be scheduled with the proposed bandwidth:",
                        subscriptions);
            }
        } finally {
            nullSafeShutdown(proposedBwManager);
        }

        return subscriptions;
    }

    /**
     * Starts the proposed bandwidth manager and returns the reference to it.
     * 
     * @param bandwidthMap
     * 
     * @return the proposed bandwidth manager
     * @throws SerializationException
     */
    @VisibleForTesting
    BandwidthManager<T, C> startProposedBandwidthManager(
            BandwidthMap bandwidthMap) {

        InMemoryBandwidthContextFactory
                .setInMemoryBandwidthConfigFile(bandwidthMap);

        return startBandwidthManager(
                InMemoryBandwidthManager.IN_MEMORY_BANDWIDTH_MANAGER_FILES,
                true, "memory");
    }

    /**
     * Starts the new bandwidth manager.
     * 
     * @return true if the new bandwidth manager was started
     */
    private boolean startNewBandwidthManager() {
        BandwidthManager<T, C> bandwidthManager = startBandwidthManager(
                getSpringFilesForNewInstance(), false, "EDEX");

        final boolean successfullyStarted = bandwidthManager != null;
        if (successfullyStarted) {
            this.shutdown();
        } else {
            statusHandler
                    .error("The new BandwidthManager reference was null, please check the log for errors.");
        }
        return successfullyStarted;
    }

    /**
     * Starts a {@link BandwidthManager} and returns a reference to it.
     * 
     * @param springFiles
     *            the spring files to use
     * @param type
     * @return the reference to the bandwidth manager
     */
    private BandwidthManager<T, C> startBandwidthManager(
            final String[] springFiles, boolean close, String type) {
        ITimer timer = TimeUtil.getTimer();
        timer.start();

        ClassPathXmlApplicationContext ctx = null;
        try {
            ctx = new ClassPathXmlApplicationContext(springFiles,
                    EDEXUtil.getSpringContext());
            final BandwidthManager<T, C> bwManager = ctx.getBean(
                    "bandwidthManager", BandwidthManager.class);
            try {
                bwManager.initializer.executeAfterRegistryInit();
                return bwManager;
            } catch (EbxmlRegistryException e) {
                statusHandler
                        .handle(Priority.PROBLEM,
                                "Error loading subscriptions after starting the new bandwidth manager!  Returning null reference.",
                                e);
                return null;
            }
        } finally {
            if (close) {
                Util.close(ctx);
            }

            timer.stop();
            statusHandler.info("Took [" + timer.getElapsedTime()
                    + "] ms to start a new bandwidth manager of type [" + type
                    + "]");
        }
    }

    /**
     * Return the display of the retrieval plan for the network.
     * 
     * @param network
     *            the network
     * @return the plan
     */
    private String showRetrievalPlan(Network network) {
        RetrievalPlan a = retrievalManager.getPlan(network);
        return (a != null) ? a.showPlan() : "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        initializer.init(this, dbInit, retrievalManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInitializer(BandwidthInitializer initializer) {
        this.initializer = initializer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BandwidthInitializer getInitializer() {
        return initializer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthorizationResponse authorized(IUser user,
            BandwidthRequest request) throws AuthorizationException {
        return new AuthorizationResponse(true);
    }

    /**
     * {@inheritDoc}
     */
    public List<BandwidthAllocation> copyState(BandwidthManager<T, C> copyFrom) {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();
        List<BandwidthAllocation> unscheduled = Collections.emptyList();
        IBandwidthDao<T, C> fromDao = copyFrom.bandwidthDao;

        final boolean proposingBandwidthChange = retrievalManager
                .isProposingBandwidthChanges(copyFrom.retrievalManager);
        if (proposingBandwidthChange) {

            retrievalManager.initRetrievalPlans();

            // Proposing bandwidth changes requires the old way of bringing up a
            // fresh bandwidth manager and trying the change from scratch
            unscheduled = Lists.newArrayList();
            Set<String> subscriptionNames = Sets.newHashSet();
            for (BandwidthSubscription subscription : fromDao
                    .getBandwidthSubscriptions()) {
                subscriptionNames.add(subscription.getName());
            }

            Set<Subscription<T, C>> actualSubscriptions = Sets.newHashSet();
            for (String subName : subscriptionNames) {
                try {
                    Subscription<T, C> actualSubscription = DataDeliveryHandlers
                            .getSubscriptionHandler().getByName(subName);
                    actualSubscriptions.add(actualSubscription);
                } catch (RegistryHandlerException e) {
                    statusHandler
                            .handle(Priority.PROBLEM,
                                    "Unable to lookup the subscription, results may not be accurate for modeling bandwidth changes.",
                                    e);
                }
            }

            // Now for each subscription, attempt to schedule bandwidth
            for (Subscription<T, C> subscription : actualSubscriptions) {
                unscheduled.addAll(this.schedule(subscription));
            }
        } else {
            // Otherwise we can just copy the entire state of the current system
            // and attempt the proposed changes
            final List<SubscriptionRetrieval> subscriptionRetrievals = fromDao
                    .getSubscriptionRetrievals();
            List<BandwidthSubscription> bandwidthSubscriptions = Lists
                    .newArrayListWithCapacity(subscriptionRetrievals.size());
            for (SubscriptionRetrieval retrieval : subscriptionRetrievals) {
                bandwidthSubscriptions
                        .add(retrieval.getBandwidthSubscription());
            }
            bandwidthDao.storeBandwidthSubscriptions(bandwidthSubscriptions);

            bandwidthDao.store(subscriptionRetrievals);

            RetrievalManager fromRetrievalManager = copyFrom.retrievalManager;
            this.retrievalManager.copyState(fromRetrievalManager);
        }

        timer.stop();
        timer.logLaps("copyingState()", performanceHandler);

        return unscheduled;
    }

    /**
     * Performs shutdown necessary for the {@link BandwidthManager} instance.
     */
    @VisibleForTesting
    void shutdown() {
        try {
            retrievalManager.shutdown();
        } catch (Exception e) {
            statusHandler.handle(Priority.WARN,
                    "Unable to shutdown the retrievalManager.", e);
        } finally {
            shutdownInternal();
        }
    }

    /**
     * Get the Spring files used to create a new instance of this
     * {@link BandwidthManager} type.
     * 
     * @return the Spring files
     */
    protected abstract String[] getSpringFilesForNewInstance();

    /**
     * Determine the latency that would be required on the subscription for it
     * to be fully scheduled.
     * 
     * @param subscription
     *            the subscription
     * @return the required latency, in minutes
     */
    @VisibleForTesting
    int determineRequiredLatency(final Subscription<T, C> subscription) {
        final int requiredLatency = determineRequiredValue(subscription,
                new FindSubscriptionRequiredLatency());

        final int bufferRoomInMinutes = retrievalManager.getPlan(
                subscription.getRoute()).getBucketMinutes();

        return requiredLatency + bufferRoomInMinutes;
    }

    /**
     * Determine the dataset size that would be required on the subscription for
     * it to be fully scheduled.
     * 
     * @param subscription
     *            the subscription
     * @return the required dataset size
     */
    private long determineRequiredDataSetSize(
            final Subscription<T, C> subscription) {
        return determineRequiredValue(subscription,
                new FindSubscriptionRequiredDataSetSize());
    }

    /**
     * Determine a value that would be required on the subscription for it to be
     * fully scheduled.
     * 
     * @param subscription
     *            the subscription
     * @param strategy
     *            the required value strategy
     * @return the required value
     */
    private <M extends Comparable<M>> M determineRequiredValue(
            final Subscription<T, C> subscription,
            final IFindSubscriptionRequiredValue<M> strategy) {
        ITimer timer = TimeUtil.getTimer();
        timer.start();

        boolean foundRequiredValue = false;
        M currentValue = strategy.getInitialValue(subscription);

        M previousValue = currentValue;
        do {
            previousValue = currentValue;
            currentValue = strategy.getNextValue(subscription, currentValue);

            Subscription<T, C> clone = strategy.setValue(subscription.copy(),
                    currentValue);
            foundRequiredValue = isSchedulableWithoutConflict(clone);
        } while (!foundRequiredValue);

        SortedSet<M> possibleValues = strategy.getPossibleValues(previousValue,
                currentValue);

        IBinarySearchResponse<M> response = AlgorithmUtil.binarySearch(
                possibleValues, new Comparable<M>() {
                    @Override
                    public int compareTo(M valueToCheck) {
                        Subscription<T, C> clone = strategy.setValue(
                                subscription.copy(), valueToCheck);

                        boolean valueWouldWork = isSchedulableWithoutConflict(clone);

                        // Check if one more restrictive value would not work,
                        // if so then this is the required value, otherwise keep
                        // searching
                        if (valueWouldWork) {
                            clone = strategy.setValue(
                                    subscription.copy(),
                                    strategy.getNextRestrictiveValue(valueToCheck));

                            return (isSchedulableWithoutConflict(clone)) ? 1
                                    : 0;
                        } else {
                            // Stuff would still be unscheduled
                            return -1;
                        }
                    }
                });

        final M binarySearchedValue = response.getItem();
        final String valueDescription = strategy.getValueDescription();
        if (binarySearchedValue != null) {
            currentValue = binarySearchedValue;

            if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                statusHandler.debug(String.format("Found required "
                        + valueDescription + " of [%s] in [%s] iterations",
                        binarySearchedValue, response.getIterations()));
            }
        } else {
            statusHandler.warn(String.format("Unable to find the required "
                    + valueDescription
                    + " with a binary search, using value [%s]", currentValue));
        }

        timer.stop();

        final String logMsg = String.format("Determined required "
                + valueDescription + " of [%s] in [%s] ms.", currentValue,
                timer.getElapsedTime());

        statusHandler.info(logMsg);

        return currentValue;
    }

    /**
     * Checks whether the subscription, as defined, would be schedulable without
     * conflicting with the current bandwidth or any other subscriptions.
     * 
     * @param subscription
     *            the subscription
     * @return true if able to be cleanly scheduled, false otherwise
     */
    private boolean isSchedulableWithoutConflict(
            final Subscription<T, C> subscription) {
        BandwidthMap copyOfCurrentMap = BandwidthMap
                .load(EdexBandwidthContextFactory.getBandwidthMapConfig());

        BandwidthManager<T, C> proposedBandwidthManager = null;
        try {
            proposedBandwidthManager = startProposedBandwidthManager(copyOfCurrentMap);
            Set<String> unscheduled = proposedBandwidthManager
                    .scheduleSubscriptions(Arrays.asList(subscription));
            return unscheduled.isEmpty();
        } catch (SerializationException e) {
            statusHandler
                    .handle(Priority.PROBLEM,
                            "Serialization error while determining required latency.  Returning true in order to be fault tolerant.",
                            e);
            return true;
        } finally {
            nullSafeShutdown(proposedBandwidthManager);
        }
    }

    /**
     * Provide implementation specific shutdown.
     */
    protected abstract void shutdownInternal();

    /**
     * Special handling for Gridded Times with cycles and time indicies
     * 
     * @param subTime
     * @param dataSetMetaDataTime
     * @return
     */
    protected static Time handleCyclesAndSequences(Time subTime,
            Time dataSetMetaDataTime) {

        if (subTime instanceof GriddedTime) {
            GriddedTime time = (GriddedTime) subTime;
            GriddedTime dsmTime = (GriddedTime) dataSetMetaDataTime;
            dsmTime.setSelectedTimeIndices(time.getSelectedTimeIndices());
            dsmTime.setCycleTimes(time.getCycleTimes());
        }

        return dataSetMetaDataTime;
    }
    
    /**
     * Sets a range based on the baseReferenceTime hour.
     * @param baseReferenceTime
     * @return
     */
    public static Map<String, Date> getBaseReferenceTimeDateRange(Calendar baseReferenceTime) {
        
        Map<String, Date> dates = new HashMap<String, Date>(2);
        // Set min range to baseReferenceTime hour "00" minutes, "00" seconds
        // Set max range to baseReferenceTime hour "59" minutes, "59" seconds
        Calendar min = TimeUtil.newGmtCalendar(baseReferenceTime.getTime());
        min.set(Calendar.MINUTE, 0);
        min.set(Calendar.SECOND, 0);
        Calendar max = TimeUtil.newGmtCalendar(baseReferenceTime.getTime());
        max.set(Calendar.MINUTE, 59);
        max.set(Calendar.SECOND, 59);
        
        dates.put(MIN_RANGE_TIME, min.getTime());
        dates.put(MAX_RANGE_TIME, max.getTime());
        
        return dates;
    }
}
