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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.common.annotations.VisibleForTesting;
import com.raytheon.uf.common.auth.exception.AuthorizationException;
import com.raytheon.uf.common.datadelivery.bandwidth.BandwidthRequest;
import com.raytheon.uf.common.datadelivery.bandwidth.BandwidthRequest.RequestType;
import com.raytheon.uf.common.datadelivery.bandwidth.ProposeScheduleResponse;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthGraphData;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthMap;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthRoute;
import com.raytheon.uf.common.datadelivery.event.retrieval.AdhocSubscriptionRequestEvent;
import com.raytheon.uf.common.datadelivery.event.retrieval.SubscriptionRequestEvent;
import com.raytheon.uf.common.datadelivery.event.retrieval.SubscriptionStatusEvent;
import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.RecurringSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionUtil;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.Utils.SubscriptionStatus;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.time.util.IPerformanceTimer;
import com.raytheon.uf.common.time.util.ITimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.common.util.algorithm.AlgorithmUtil;
import com.raytheon.uf.common.util.algorithm.AlgorithmUtil.IBinarySearchResponse;
import com.raytheon.uf.edex.auth.req.AbstractPrivilegedRequestHandler;
import com.raytheon.uf.edex.auth.resp.AuthorizationResponse;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthBucket;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDbInit;
import com.raytheon.uf.edex.datadelivery.bandwidth.interfaces.IBandwidthInitializer;
import com.raytheon.uf.edex.datadelivery.bandwidth.processing.SimpleSubscriptionAggregator;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalPlan;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.SubscriptionRetrievalAgent;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.UnscheduledAllocationReport;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthDaoUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.handlers.IBandwidthChangedCallback;
import com.raytheon.uf.edex.registry.ebxml.exception.EbxmlRegistryException;
import com.raytheon.uf.edex.registry.ebxml.util.RegistryIdUtil;

/**
 * Abstract implementation which provides core functionality. Intentionally
 * package-private to hide implementation details.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 07, 2012           dhladky   Initial creation
 * Sep 25, 2013  1797     dhladky   separated time from gridded time
 * Oct 23, 2013  2385     bphillip  Change schedule method to scheduleAdhoc
 * Oct 30, 2013  2448     dhladky   Moved methods to TimeUtil.
 * Nov 04, 2013  2506     bgonzale  Added removeBandwidthSubscriptions method.
 * Nov 19, 2013  2545     bgonzale  changed getBandwidthGraphData to protected.
 * Dec 04, 2013  2566     bgonzale  added method to retrieve and parse spring
 *                                  files for a mode.
 * Dec 11, 2013  2566     bgonzale  fix spring resource resolution.
 * Dec 17, 2013  2636     bgonzale  Changed logging to differentiate the output.
 * Jan 08, 2014  2615     bgonzale  getMostRecent checks subscription time
 *                                  constraints before scheduling. handlePoint
 *                                  method now schedules most recent.
 * Jan 14, 2014  2692     dhladky   Bad Point scheduling final Empty list.
 * Jan 14, 2014  2459     mpduff    Change to subscription status.
 * Jan 25, 2014  2636     mpduff    Don't do an initial adhoc query for a new
 *                                  subscription.
 * Jan 24, 2013  2709     bgonzale  Before scheduling adhoc, check if in active
 *                                  period window.
 * Jan 29, 2014  2636     mpduff    Scheduling refactor.
 * Jan 30, 2014  2686     dhladky   refactor of retrieval.
 * Feb 06, 2014  2636     bgonzale  fix overwrite of unscheduled subscription
 *                                  list.  fix scheduling of already scheduled
 *                                  BandwidthAllocations.
 * Feb 11, 2014  2771     bgonzale  Added handler for GET_DATADELIVERY_ID
 *                                  request.
 * Feb 10, 2014  2636     mpduff    Changed how retrieval plan is updated over
 *                                  time.
 * Apr 02, 2014  2810     dhladky   Priority sorting of subscriptions.
 * Apr 09, 2014  3012     dhladky   Range the querries for metadata checks to
 *                                  subscriptions.
 * Apr 22, 2014  2992     dhladky   Ability to get list of all registry nodes
 *                                  containing data.
 * May 22, 2014  2808     dhladky   Schedule unscheduled subs when one is
 *                                  de-activated.
 * May 15, 2014  3113     mpduff    Schedule subscriptions for gridded datasets
 *                                  without cycles.
 * Jul 28, 2014  2752     dhladky   Allow Adhocs for Shared Subscriptions,
 *                                  improved efficency of scheduling.
 * Aug 29, 2014  3446     bphillip  SubscriptionUtil is now a singleton
 * Sep 14, 2014  2131     dhladky   PDA updates
 * Oct 03, 2014  2749     ccody     Changes to startBandwidthManager to refresh
 *                                  only necessary spring components Changes to
 *                                  handleRequest so that after restarting that
 *                                  the EdexBandwidthManager changes and updates
 *                                  itself so that attempts to connect with them
 *                                  do not result in errors and exceptions.
 * Oct 15, 2014  3664     ccody     Add notification event for unscheduled
 *                                  Subscriptions at startup
 * Oct 12, 2014  3707     dhladky   Changed the way gridded subscriptions are
 *                                  triggerd for retrieval.
 * Oct 28, 2014  2748     ccody     Subscription outside of Active period should
 *                                  not throw an exception
 * Nov 03, 2014  2414     dhladky   Refactored bandwidth Manager, better
 *                                  documented methods, fixed race conditions.
 * Nov 19, 2014  3852     dhladky   Fixed un-safe empty allocation state that
 *                                  broke Maintenance Task. More logging.
 * Nov 20, 2014  2749     ccody     Added "propose only" for Set Avail Bandwidth
 * Jan 15, 2014  3884     dhladky   Removed shutdown and shutdown internal
 *                                  methods (un-needed) which undermined #2749.
 * Jan 27, 2014  4041     dhladky   Consolidated time checks for Adhoc
 *                                  creations.
 * Feb 19, 2015  3998     dhladky   Streamlined adhoc subscription processing.
 * May 27, 2015  4531     dhladky   Remove excessive Calendar references.
 * Jun 09, 2015  4047     dhladky   Performance improvement on startup, initial
 *                                  startup scheduling async now.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Sep 12, 2016  5772     tjensen   Allow PDA adhocs for older times
 * Sep 30, 2016  5772     tjensen   Fix Adhocs for older times
 * Apr 05, 2017  1045     tjensen   Update for moving datasets
 * Apr 27, 2017  6186     rjpeter   Removed overloaded scheduleAdhoc, updated
 *                                  queueRetrieval, removed
 *                                  BandwidthDataSetUpdate.
 * Jun 08, 2017  6222     tgurney   Add bandwidthChangedCallback
 * Jun 20, 2017  6299     tgurney   Remove IProposeScheduleResponse
 * Jul 18, 2017  6286     randerso  Changed to use new Roles/Permissions
 *                                  framework
 * Aug 02, 2017  6186     rjpeter   Added retrievalAgent
 * Sep 18, 2017  6415     rjpeter   Updated purgeAllocations to purge
 *                                  SubscriptionRetrieval
 * Oct 25, 2017  6484     tjensen   Merged SubscriptionRetrievals and
 *                                  BandwidthAllocations
 * Feb 02, 2018  6471     tjensen   Improve handling of subscriptions that are
 *                                  too big to schedule
 *
 * </pre>
 *
 * @author dhladky
 */
public abstract class BandwidthManager<T extends Time, C extends Coverage>
        extends AbstractPrivilegedRequestHandler<BandwidthRequest<T, C>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /** Used for min time range (point subs) **/
    public static final String MIN_RANGE_TIME = "min";

    /** Used for max time range (point subs) **/
    public static final String MAX_RANGE_TIME = "max";

    /** used to query for registry subscription object owners **/
    private static final String objectType = "'urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:com.raytheon.uf.common.datadelivery.registry.%Subscription'";

    /** Persistence DAO for BWM **/
    protected final IBandwidthDao<T, C> bandwidthDao;

    /** Aggregates retrievals for subscriptions that have common datasets **/
    private SimpleSubscriptionAggregator aggregator;

    /** Initializes the Bandwidth Manager **/
    private IBandwidthInitializer initializer;

    /** BWM DAO UTIl, has persistance related methods for BWM **/
    protected final BandwidthDaoUtil<T, C> bandwidthDaoUtil;

    /** init for the EDEX bandwidth managers persistance layer **/
    private final IBandwidthDbInit dbInit;

    /**
     * Subscriptions sent from CAVE, internal, etc, requests Will preserve the
     * state of these subscriptions.
     **/
    protected Map<String, Subscription<T, C>> requestSubscriptions;

    /** Contains the registry node identifying info **/
    private final RegistryIdUtil idUtil;

    /**
     * Instance variable and not static, because there are multiple child
     * implementation classes which should each have a unique prefix
     **/
    private final IPerformanceStatusHandler performanceHandler = PerformanceStatus
            .getHandler(this.getClass().getSimpleName());

    /** Manages the Retrieval Process for the Bandwidth Manager **/
    @VisibleForTesting
    protected final RetrievalManager retrievalManager;

    protected final SubscriptionRetrievalAgent retrievalAgent;

    private IBandwidthChangedCallback bandwidthChangedCallback;

    public BandwidthManager(IBandwidthDbInit dbInit,
            IBandwidthDao<T, C> bandwidthDao, RetrievalManager retrievalManager,
            BandwidthDaoUtil<T, C> bandwidthDaoUtil, RegistryIdUtil idUtil,
            SubscriptionRetrievalAgent retrievalAgent) {
        this.dbInit = dbInit;
        this.bandwidthDao = bandwidthDao;
        this.retrievalManager = retrievalManager;
        this.bandwidthDaoUtil = bandwidthDaoUtil;
        this.idUtil = idUtil;
        this.retrievalAgent = retrievalAgent;
    }

    /**
     * Get the Spring files used to create a new instance of this
     * {@link BandwidthManager} type.
     *
     * @return the Spring files
     */
    protected abstract String[] getSpringFilesForNewInstance();

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
                "memory");
    }

    /**
     * Starts the new bandwidth manager.
     *
     * Making changes to preference settings; particularly Bandwidth settings;
     * in CAVE results in a forced refresh of the Spring .xml configuration.
     * (ex. CAVE->Data Delivery->System Management:: Settings->Bandwidth) The
     * call path of the Spring refresh looks like: [ ... -> setBandwidth() ->
     * startNewBandwidthManager() -> startBandwidthManager(...) ] and uses the
     * full list of EDEX Spring .xml files. This operation fails.
     * <p>
     * 1. There are presently a total of 73 .xml files that are included in the
     * EDEX Spring configuration. Using the full list of files results in (non
     * terminal, but repeating) exception errors.
     * <p>
     *
     * 2. Removing the following files from the EDEX file list will allow this
     * method to complete execution without failure, but results in Bandwidth
     * Manager not restarting properly. i.e. Exceptions result elsewhere.
     * /res/spring/harvester-datadelivery-registry.xml
     * /res/spring/harvester-datadelivery.xml
     * /spring/datadelivery-subscription-verification.xml
     * /spring/bandwidth-datadelivery-edex-impl-wfo.xml
     * /spring/bandwidth-datadelivery-edex-impl.xml /res/spring/purge-logs.xml
     * /res/spring/ebxml-garbagecollector-edex-impl.xml
     * /res/spring/ebxml-webserver.xml /spring/datadelivery-cron.xml
     * /spring/datadelivery-wfo-cron.xml /spring/retrieval-datadelivery.xml
     * <p>
     * 3. Using ONLY the Spring files for the "memory" configuration e.g.
     * InMemoryBandwidthManager.IN_MEMORY_BANDWIDTH_MANAGER_FILES (@see
     * BandwidthManager.startProposedBandwidthManager) will cause the Spring
     * component environment to restart properly. This does not appear to
     * negatively impact other parts of the system.
     * <p>
     * always sync up with download time windows
     *
     * @return true if the new bandwidth manager was started
     */
    private boolean startNewBandwidthManager() {

        BandwidthManager<T, C> bandwidthManager = startBandwidthManager(
                InMemoryBandwidthManager.IN_MEMORY_BANDWIDTH_MANAGER_FILES,
                "EDEX");

        final boolean successfullyStarted = bandwidthManager != null;

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
            final String[] springFiles, String type) {
        ITimer timer = TimeUtil.getTimer();
        timer.start();
        try {
            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
                    springFiles, EDEXUtil.getSpringContext());
            BandwidthManager<T, C> bwManager = null;
            bwManager = ctx.getBean("bandwidthManager", BandwidthManager.class);
            try {
                bwManager.initializer.executeAfterRegistryInit();
                return bwManager;
            } catch (EbxmlRegistryException e) {
                logger.error(
                        "Error loading subscriptions after starting the new bandwidth manager! ",
                        e);
                return null;
            }
        } finally {
            timer.stop();
            logger.info("Took [" + timer.getElapsedTime()
                    + "] ms to start a new bandwidth manager of type [" + type
                    + "]");
        }
    }

    /**
     * This is the request portion of the BandwidthManager It receives requests
     * from CAVE clients and proposes them in the InMemory BWM then it persists
     * those changes (if good) through the EDEX (hibernate) BWM.
     *
     * @param request
     */
    @Override
    public Object handleRequest(BandwidthRequest<T, C> request)
            throws Exception {

        ITimer timer = TimeUtil.getTimer();
        timer.start();

        boolean resetManager = false;
        Object response = null;

        final Network requestNetwork = request.getNetwork();
        final int bandwidth = request.getBandwidth();

        /*
         * Set the requestSubscriptions. Used elsewhere for continuity and
         * prevention of race conditions based on non-transactional subscription
         * updates. This way the current (In CAVE's eyes) condition of the
         * subscription is always guaranteed.
         */
        final List<Subscription<T, C>> subscriptions = request
                .getSubscriptions();
        requestSubscriptions = BandwidthUtil
                .getMapFromRequestList(subscriptions);
        final RequestType requestType = request.getRequestType();

        switch (requestType) {
        case GET_ESTIMATED_COMPLETION:
            Subscription<T, C> adhocAsSub = null;
            if (subscriptions.size() != 1 || !((adhocAsSub = subscriptions
                    .get(0)) instanceof AdhocSubscription)) {
                throw new IllegalArgumentException(
                        "Must supply one, and only one, adhoc subscription to get the estimated completion time.");
            }
            response = getEstimatedCompletionTime(
                    (AdhocSubscription<T, C>) adhocAsSub);
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
                final ProposeScheduleResponse proposeResponse = proposeScheduleSbnSubscription(
                        subscriptions);
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
            }
            break;
        case PROPOSE_ONLY_SET_BANDWIDTH:
            Set<String> proposeOnlyUnscheduledSubscriptions = proposeSetBandwidth(
                    requestNetwork, bandwidth);
            response = proposeOnlyUnscheduledSubscriptions;
            if (proposeOnlyUnscheduledSubscriptions.isEmpty()) {
                logger.info(
                        "No subscriptions will be unscheduled by changing the bandwidth for network ["
                                + requestNetwork + "] to [" + bandwidth
                                + "].  This is a Propose-Only call. NO CHANGES WILL BE APPLIED..");
            }
            break;
        case PROPOSE_SET_BANDWIDTH:
            Set<String> unscheduledSubscriptions = proposeSetBandwidth(
                    requestNetwork, bandwidth);
            response = unscheduledSubscriptions;
            if (unscheduledSubscriptions.isEmpty()) {
                logger.info(
                        "No subscriptions will be unscheduled by changing the bandwidth for network ["
                                + requestNetwork + "] to [" + bandwidth
                                + "].  Applying...");
                // This is a safe operation as all subscriptions will remain
                // scheduled, just apply
                setBandwidth(requestNetwork, bandwidth);
                resetManager = true;
            }
            break;
        case FORCE_SET_BANDWIDTH:
            boolean setBandwidthBool = setBandwidth(requestNetwork, bandwidth);
            response = setBandwidthBool;
            resetManager = true;
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
            List<BandwidthAllocation> z = bandwidthDao
                    .getDeferred(requestNetwork, request.getBegin().getTime());
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
            if (subscriptions.size() != 1) {
                throw new IllegalArgumentException(
                        "Must supply one, and only one, subscription to get the status summary.");
            }
            response = bandwidthDao
                    .getSubscriptionStatusSummary(subscriptions.get(0));
            break;
        default:
            throw new IllegalArgumentException(
                    "Unknown request type [" + requestType + "]");
        }

        // Clean up any in-memory bandwidth configuration files
        InMemoryBandwidthContextFactory.deleteInMemoryBandwidthConfigFile();

        if (resetManager) {
            resetBandwidthManager(requestNetwork,
                    "Bandwidth changed to " + bandwidth + " KBps");

        }

        timer.stop();
        logger.info("Processed request of type [" + requestType + "] in ["
                + timer.getElapsedTime() + "] ms");

        return response;
    }

    /**
     ****************************** Scheduling subscriptions related methods **************************
     */

    /**
     * Schedule the list of subscriptions.
     *
     * @param subscriptions
     *            the subscriptions
     * @return the set of unscheduled allocation reports
     */
    protected Set<UnscheduledAllocationReport> scheduleSubscriptions(
            List<Subscription<T, C>> insubscriptions) {
        Set<UnscheduledAllocationReport> unscheduledAllocations = new HashSet<>();
        Map<String, SubscriptionRequestEvent> subscriptionEventsMap = new HashMap<>();

        for (Subscription<T, C> subscription : orderSubscriptionsByPriority(
                insubscriptions)) {

            List<UnscheduledAllocationReport> unscheduled = subscriptionUpdated(
                    subscription);
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
            logger.info(
                    "Subscription scheduling, added to bus as request event: "
                            + event.getId());
            EventBus.publish(event);
        }
        return unscheduledAllocations;
    }

    /**
     * Schedule retrievals for Subscriptions in the list.
     *
     * @param subscription
     * @return A list of unscheduled allocation reports
     */
    public List<UnscheduledAllocationReport> schedule(
            Subscription<T, C> subscription) {
        List<UnscheduledAllocationReport> unscheduled = Collections.emptyList();
        if (subscription instanceof RecurringSubscription) {
            if (!((RecurringSubscription<T, C>) subscription)
                    .shouldSchedule()) {
                return unscheduled;
            }
        }
        RetrievalPlan plan = bandwidthDaoUtil
                .getRetrievalPlan(subscription.getRoute());

        if (plan != null) {

            @SuppressWarnings("rawtypes")
            List<DataSetMetaData> dsmdList = null;

            try {
                dsmdList = DataDeliveryHandlers.getDataSetMetaDataHandler()
                        .getDataSetMetaDataToDate(subscription.getDataSetName(),
                                subscription.getProvider(),
                                plan.getPlanEnd().getTime());
            } catch (RegistryHandlerException e1) {
                dsmdList = Collections.emptyList();
                logger.error(
                        "Unable to look-up list of DataSetMetData during scheduling. ",
                        e1);
            }

            SortedSet<Date> retrievalTimes = subscription.getRetrievalTimes(
                    plan.getPlanStart().getTime(), plan.getPlanEnd().getTime(),
                    dsmdList, SubscriptionUtil.getInstance());

            unscheduled = scheduleSubscriptionForRetrievalTimes(subscription,
                    retrievalTimes);

            if (!unscheduled.isEmpty()) {
                logger.warn(unscheduled.size()
                        + " allocations unscheduled while scheduling for subscription "
                        + subscription.getName());
                if (logger.isDebugEnabled()) {
                    logger.debug(StringUtil.createMessage(
                            "The following allocations were unscheduled:",
                            unscheduled, 3));
                }
            }
        }
        return unscheduled;
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
     * Update the retrieval plan for this subscription.
     *
     * @param subscription
     *            The subscription that needs its scheduling updated
     */
    @SuppressWarnings({ "rawtypes" })
    private void updateSchedule(Subscription<T, C> subscription) {

        RetrievalPlan plan = bandwidthDaoUtil
                .getRetrievalPlan(subscription.getRoute());
        List<DataSetMetaData> dsmdList = null;

        if (plan != null) {

            try {
                dsmdList = DataDeliveryHandlers.getDataSetMetaDataHandler()
                        .getDataSetMetaDataToDate(subscription.getDataSetName(),
                                subscription.getProvider(),
                                plan.getPlanEnd().getTime());
            } catch (RegistryHandlerException e1) {
                logger.error("Unable to look-up list of DataSetMetData for "
                        + subscription.getDataSetName(), e1);
            }
            SortedSet<Date> retrievalTimes = subscription.getRetrievalTimes(
                    plan.getPlanStart().getTime(), plan.getPlanEnd().getTime(),
                    dsmdList, SubscriptionUtil.getInstance());
            List<BandwidthAllocation> currentBandwidthAllocations = bandwidthDao
                    .getBandwidthAllocationsByRegistryId(subscription.getId());

            // Added safety checks
            if (!currentBandwidthAllocations.isEmpty()
                    && !retrievalTimes.isEmpty()) {
                // Get the latest/max scheduled time
                Date max = currentBandwidthAllocations.get(0)
                        .getBaseReferenceTime();
                for (BandwidthAllocation ba : currentBandwidthAllocations) {
                    if (ba.getBaseReferenceTime().after(max)) {
                        max = ba.getBaseReferenceTime();
                    }
                }

                /*
                 * Add 2 minutes to the max to cover averaged values that
                 * increased and remove any retrieval times before the last
                 * scheduled time (max)
                 */
                long extraTime = max.getTime() + TimeUtil.MILLIS_PER_MINUTE * 2;
                max = new Date(extraTime);
                Iterator<Date> iter = retrievalTimes.iterator();
                while (iter.hasNext()) {
                    Date d = iter.next();
                    if (d.before(max)) {
                        iter.remove();
                    }
                }

                scheduleSubscriptionForRetrievalTimes(subscription,
                        retrievalTimes);
            }
        }
    }

    /**
     * Update the retrieval plan scheduling.
     *
     * @param Network
     *            the network to update
     *
     * @return number of subscriptions processed
     */
    public int updateSchedule(Network network) {
        List<Subscription<T, C>> subsToSchedule = getSubscriptionsToSchedule(
                network);
        if (CollectionUtil.isNullOrEmpty(subsToSchedule)) {
            return 0;
        }

        for (Subscription<T, C> subscription : subsToSchedule) {
            updateSchedule(subscription);
        }

        return subsToSchedule.size();
    }

    /**
     * Try to schedule unscheduled subs when another deactivates
     *
     * @param deactivatedSubName
     */
    public abstract void scheduleUnscheduledSubscriptions(
            String deactivatedSubName);

    /**
     * Schedule the given subscription for the specified retrieval times.
     *
     * @param subscription
     *            the subscription
     * @param retrievalTimes
     *            the retrieval times
     * @return the unscheduled allocation reports
     */
    private List<UnscheduledAllocationReport> scheduleSubscriptionForRetrievalTimes(
            Subscription<T, C> subscription, SortedSet<Date> retrievalTimes) {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        if (retrievalTimes.isEmpty()) {
            return Collections.emptyList();
        }

        List<UnscheduledAllocationReport> unscheduled = new ArrayList<>();
        logger.info("Scheduling subscription " + subscription.getName());

        unscheduled.addAll(aggregate(subscription, retrievalTimes));
        timer.lap("aggregate");

        timer.stop();
        timer.logLaps("scheduleSubscriptionForRetrievalTimes() subscription ["
                + subscription.getName() + "] retrievalTimes ["
                + retrievalTimes.size() + "]", performanceHandler);

        return unscheduled;
    }

    /**
     *************************** Propose Scheduling related methods **********************************
     * These will be primarily used in the inMemory Bandwidth Manager
     */

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
        final ProposeScheduleResponse proposeResponse = proposeSchedule(
                subscriptions);
        Set<String> subscriptionsUnscheduled = proposeResponse
                .getUnscheduledSubscriptions();
        if (subscriptionsUnscheduled.isEmpty()) {
            // This is a safe operation as all subscriptions will remain
            // scheduled, just apply
            scheduleSubscriptions(subscriptions);
        } else if (subscriptions.size() == 1) {
            final Subscription<T, C> subscription = subscriptions.iterator()
                    .next();
            int requiredLatency = determineRequiredLatency(subscription);
            proposeResponse.setRequiredLatency(requiredLatency);
        } else {
            Map<String, Subscription<T, C>> allSubscriptionMap = new HashMap<>();
            String name = null;
            for (Subscription<T, C> sub : subscriptions) {
                name = sub.getName();
                allSubscriptionMap.put(name, sub);
            }

            // Publish Notification events
            String msg = " is unscheduled.";
            Subscription<T, C> unSchedSub = null;
            for (String unSchedSubName : subscriptionsUnscheduled) {
                unSchedSub = allSubscriptionMap.get(unSchedSubName);
                EventBus.publish(new SubscriptionStatusEvent(unSchedSub, msg));
            }
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
    protected abstract ProposeScheduleResponse proposeScheduleSbnSubscription(
            List<Subscription<T, C>> subscriptions) throws Exception;

    /**
     * Propose scheduling the subscriptions.
     *
     * @param subscriptions
     *            the subscriptions
     * @return the response
     * @throws SerializationException
     */
    private ProposeScheduleResponse proposeSchedule(
            Collection<Subscription<T, C>> subscriptions)
            throws SerializationException {
        BandwidthMap copyOfCurrentMap = BandwidthMap
                .load(EdexBandwidthContextFactory.getBandwidthMapConfig());

        Set<UnscheduledAllocationReport> unscheduled = Collections.emptySet();
        BandwidthManager<T, C> proposedBwManager = null;
        try {
            proposedBwManager = startProposedBandwidthManager(copyOfCurrentMap);

            unscheduled = proposedBwManager.scheduleSubscriptions(
                    (List<Subscription<T, C>>) subscriptions);
        } catch (Exception e) {
            logger.error("Error proposing scheduling! ", e);
        }

        final ProposeScheduleResponse proposeScheduleResponse = new ProposeScheduleResponse();
        proposeScheduleResponse.setUnscheduledSubscriptions(
                getUnscheduledSubNames(unscheduled));
        /*
         * If allocations were unscheduled for a single subscription, return the
         * maximum available schedule size for the given window.
         */
        if (!unscheduled.isEmpty() && subscriptions.size() == 1) {
            long smallestSizeAvailable = -1;
            for (UnscheduledAllocationReport uas : unscheduled) {
                long scheduleAvailableInKB = uas.getScheduleAvailableInKB();
                if (scheduleAvailableInKB < smallestSizeAvailable
                        || smallestSizeAvailable == -1) {
                    smallestSizeAvailable = scheduleAvailableInKB;
                }
            }
            proposeScheduleResponse.setRequiredDataSetSize(smallestSizeAvailable);
        }

        return proposeScheduleResponse;
    }

    /**
     * Get the names of all subscriptions that had unscheduled allocations
     *
     * @param unscheduled
     *            unscheduled allocation reports
     * @return set of sub names that were unscheduled
     */
    protected Set<String> getUnscheduledSubNames(
            Set<UnscheduledAllocationReport> unscheduled) {
        if (unscheduled == null || unscheduled.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>();
        for (UnscheduledAllocationReport uas : unscheduled) {
            names.add(uas.getUnscheduled().getSubName());
        }
        return names;
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

        Set<String> subscriptions = new HashSet<>();
        BandwidthManager<T, C> proposedBwManager = null;
        try {
            proposedBwManager = startProposedBandwidthManager(copyOfCurrentMap);

            if (logger.isDebugEnabled()) {
                logger.debug("Current retrieval plan:" + FileUtil.EOL
                        + showRetrievalPlan(requestNetwork) + FileUtil.EOL
                        + "Proposed retrieval plan:" + FileUtil.EOL
                        + proposedBwManager.showRetrievalPlan(requestNetwork));
            }

            List<BandwidthAllocation> unscheduledAllocations = proposedBwManager.bandwidthDao
                    .getBandwidthAllocationsInState(
                            RetrievalStatus.UNSCHEDULED);

            for (BandwidthAllocation allocation : unscheduledAllocations) {
                subscriptions.add(allocation.getSubName());
            }
            if (!unscheduledAllocations.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug(StringUtil.createMessage(
                            "The following unscheduled allocations would occur with the proposed bandwidth:",
                            unscheduledAllocations, 3));
                } else if (logger.isInfoEnabled() && !subscriptions.isEmpty()) {
                    logger.info(StringUtil.createMessage(
                            "The following subscriptions would not be scheduled with the proposed bandwidth:",
                            subscriptions, 3));
                }
            }
        } catch (Exception e) {
            logger.error("Error proposing setting of Bandwidth! " + e);
        }

        return subscriptions;
    }

    /**
     ************************ Aggregator related methods ***********************************
     */

    /**
     * Sets the Subscription Retrieval Aggregator
     */
    public void setAggregator(SimpleSubscriptionAggregator aggregator) {
        this.aggregator = aggregator;
    }

    /**
     * Gets the Subscription Retrieval Aggregator
     */
    public SimpleSubscriptionAggregator getAggregator() {
        return aggregator;
    }

    /**
     * Aggregate subscriptions for a given base time and dataset.
     *
     * @param subscription
     * @param baseReferenceTimes
     * @return list of unscheduled allocation reports
     */
    private List<UnscheduledAllocationReport> aggregate(
            Subscription subscription, SortedSet<Date> baseReferenceTimes) {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        List<BandwidthAllocation> retrievals = getAggregator()
                .aggregate(subscription, baseReferenceTimes);
        timer.lap("aggregator");
        if (CollectionUtil.isNullOrEmpty(retrievals)) {
            return Collections.emptyList();
        }

        /*
         * Create a separate list of BandwidthReservations to schedule as the
         * aggregation process may return all subsumedSubscriptionRetrievalsfor
         * the specified Subscription.
         */
        List<BandwidthAllocation> reservations = new ArrayList<>();
        for (BandwidthAllocation retrieval : retrievals) {
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
            if (retrieval.getStatus().equals(RetrievalStatus.RESCHEDULE)
                    || retrieval.getStatus()
                            .equals(RetrievalStatus.PROCESSING)) {

                Date retrievalTime = retrieval.getBaseReferenceTime();
                Calendar startTime = TimeUtil.newGmtCalendar(retrievalTime);

                startTime.add(Calendar.MINUTE,
                        retrieval.getDataSetAvailablityDelay());
                int maxLatency = retrieval.getSubscriptionLatency();
                retrieval.setStartTime(startTime.getTime());

                Calendar endTime = TimeUtil.newGmtCalendar();
                endTime.setTimeInMillis(startTime.getTimeInMillis());

                if (logger.isDebugEnabled()) {
                    logger.debug("Adding latency minutes of [" + maxLatency
                            + "] to start time of " + String.format(
                                    "[%1$tY%1$tm%1$td%1$tH%1$tM]", startTime));
                }

                endTime.add(Calendar.MINUTE, maxLatency);
                retrieval.setEndTime(endTime.getTime());

                // Add SubscriptionRetrieval to the list to schedule..
                reservations.add(retrieval);
            }
        }
        timer.lap("creating retrievals");

        bandwidthDao.store(reservations);
        timer.lap("storing retrievals");

        List<UnscheduledAllocationReport> unscheduled = reservations.isEmpty()
                ? Collections.emptyList()
                : retrievalManager.schedule(reservations);
        timer.lap("scheduling retrievals");

        timer.stop();
        final int numberOfBandwidthSubscriptions = baseReferenceTimes.size();
        timer.logLaps(
                "aggregate() bandwidthSubscriptions ["
                        + numberOfBandwidthSubscriptions + "]",
                performanceHandler);

        return unscheduled;
    }

    /*
     * **************************** Misc Subscription methods
     * ********************************
     */

    /**
     * Get the subscriptions to schedule for the given network.
     *
     * @param network
     *            The network
     * @return List of subscriptions for the network
     */
    protected abstract List<Subscription<T, C>> getSubscriptionsToSchedule(
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
     ************************** ADHOC subscription related methods ************************
     */

    /**
     * Queue the retrieval of the AdhocSubscription for download.
     *
     * @param adhocSub
     * @return list of unscheduled allocation reports
     */
    protected List<UnscheduledAllocationReport> queueRetrieval(
            AdhocSubscription<T, C> adhocSub) {
        logger.info(
                "Scheduling adhoc subscription [" + adhocSub.getName() + "]");
        String url = adhocSub.getUrl();

        if (url == null || "".equals(url)) {
            throw new IllegalArgumentException(
                    "No DataSetMetaData URL specified for AdhocSubscription ["
                            + adhocSub.getName() + "], skipping retrieval");
        }

        Calendar now = BandwidthUtil.now();
        SortedSet<Date> baseRefTimes = new TreeSet<>();
        baseRefTimes.add(now.getTime());

        /*
         * Use SimpleSubscriptionAggregator (i.e. no aggregation) to generate a
         * SubscriptionRetrieval for this AdhocSubscription
         */
        SimpleSubscriptionAggregator a = new SimpleSubscriptionAggregator();
        List<BandwidthAllocation> reservations = new ArrayList<>();
        List<BandwidthAllocation> retrievals = a.aggregate(adhocSub,
                baseRefTimes);

        /*
         * TODO: Should we generate allocations? Generally will be cleared
         * shortly after creation.
         */
        for (BandwidthAllocation retrieval : retrievals) {
            retrieval.setStartTime(now.getTime());
            Calendar endTime = TimeUtil.newCalendar(now);
            endTime.add(Calendar.MINUTE, retrieval.getSubscriptionLatency());
            retrieval.setEndTime(endTime.getTime());

            /*
             * Store the SubscriptionRetrieval - retrievalManager expects the
             * BandwidthAllocations to already be stored.
             */
            bandwidthDao.store(retrieval);
            reservations.add(retrieval);
        }

        List<UnscheduledAllocationReport> unscheduled = retrievalManager
                .schedule(reservations);

        try {
            DataSetMetaData<?, ?> dsmd = DataDeliveryHandlers
                    .getDataSetMetaDataHandler().getById(adhocSub.getUrl());
            retrievalAgent.queueRetrievals(dsmd, Arrays.asList(adhocSub));
        } catch (RegistryHandlerException e) {
            logger.error("Unable to look up DataSetMetaData[" + url
                    + "] for AdhocSubscription [" + adhocSub.getName()
                    + "], skipping retrieval", e);
        }

        return unscheduled;
    }

    /***
     *************************** Subscription Action related methods *********************
     */

    /**
     * Remove BandwidthSubscription's (and dependent Objects) from any
     * RetrievalPlans they are in and adjust the RetrievalPlans accordingly.
     *
     * @param bandwidthSubscriptions
     *            The subscriptionDao's to remove.
     * @return
     */
    protected void remove(List<BandwidthAllocation> bandwidthAllocations) {
        bandwidthDaoUtil.remove(bandwidthAllocations);
    }

    /**
     * When a Subscription is updated in the Registry, update the retrieval plan
     * accordingly to match the updated Subscription.
     *
     * @param subscription
     * @return
     */
    public List<UnscheduledAllocationReport> subscriptionUpdated(
            Subscription<T, C> subscription) {
        /*
         * Since AdhocSubscription extends Subscription it is not possible to
         * separate the processing of those Objects in EventBus. So, handle the
         * case where the updated subscription is actually an AdhocSubscription
         */
        if (subscription instanceof AdhocSubscription) {
            // TODO: Move call once AdhocSubscription is no longer in registry
            List<UnscheduledAllocationReport> unscheduled = Collections
                    .emptyList();
            try {
                unscheduled = queueRetrieval(
                        (AdhocSubscription<T, C>) subscription);
            } catch (Exception e) {
                logger.error("Unable to queue retrieval for adhoc subscription "
                        + subscription.getName(), e);
            }
            return unscheduled;
        }

        // First see if BandwidthManager has seen the subscription before.
        List<BandwidthAllocation> bandwidthAllocations = bandwidthDao
                .getBandwidthAllocationsByRegistryId(subscription.getId());

        // If BandwidthManager does not know about the subscription, and
        // it's active, attempt to add it..
        if (bandwidthAllocations.isEmpty()
                && ((RecurringSubscription<?, ?>) subscription).shouldSchedule()
                && !subscription.isUnscheduled()) {
            return schedule(subscription);
        } else if (subscription.getStatus() == SubscriptionStatus.DEACTIVATED
                || subscription.isUnscheduled()) {
            remove(bandwidthAllocations);

            // Attempt to schedule any subscriptions that are unscheduled
            // More room may be available because of the de-activation
            if (subscription.getStatus() == SubscriptionStatus.DEACTIVATED) {
                scheduleUnscheduledSubscriptions(subscription.getName());
            }

            return Collections.emptyList();

        } else {
            // Normal update, unschedule old allocations and create new ones
            remove(bandwidthAllocations);
            return schedule(subscription);
        }
    }

    /**
     ************************* MISC methods ***********************************************
     */

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
    private Date getEstimatedCompletionTime(
            AdhocSubscription<T, C> subscription) {
        final List<BandwidthAllocation> bandwidthAllocations = bandwidthDao
                .getBandwidthAllocationsByRegistryId(subscription.getId());

        if (bandwidthAllocations.isEmpty()) {
            logger.warn("Unable to find bandwidthAllocations for subscription ["
                    + subscription + "].  Returning current time.");
            return new Date();
        }

        Date latest = null;
        for (BandwidthAllocation allocation : bandwidthAllocations) {
            final Date endTime = allocation.getEndTime();
            if (latest == null || endTime.after(latest)) {
                latest = endTime;
            }
        }

        return latest;
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
            if (bandwidthChangedCallback != null) {
                bandwidthChangedCallback.bandwidthChanged(bandwidth,
                        requestNetwork);
            }
            return true;
        }
        return false;
    }

    /**
     * Checks whether the subscription, as defined, would be schedulable without
     * conflicting with the current bandwidth or any other subscriptions.
     *
     * @param subscription
     *            the subscription
     * @return set of unscheduled allocation reports
     */
    private Set<UnscheduledAllocationReport> findScheduleConflicts(
            final Subscription<T, C> subscription,
            BandwidthMap copyOfCurrentMap) {

        BandwidthManager<T, C> proposedBandwidthManager = null;
        proposedBandwidthManager = startProposedBandwidthManager(
                copyOfCurrentMap);
        Set<UnscheduledAllocationReport> unscheduled = proposedBandwidthManager
                .scheduleSubscriptions(Arrays.asList(subscription));
        return unscheduled;
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
        return a != null ? a.showPlan() : "";
    }

    /**
     * Load the empty bandwidth tables with current active subscription data.
     */
    public void init() {
        initializer.init(this, dbInit, retrievalManager);
    }

    /**
     * @param initializer
     *            the initializer to set
     */
    public void setInitializer(IBandwidthInitializer initializer) {
        this.initializer = initializer;
    }

    /**
     * @return the initializer
     */
    public IBandwidthInitializer getInitializer() {
        return initializer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthorizationResponse authorized(BandwidthRequest<T, C> request)
            throws AuthorizationException {
        return new AuthorizationResponse(true);
    }

    /**
     * Copy state of current BWM, used for transfer to persistance BWM
     *
     * @param copyFrom
     * @return
     */
    public List<UnscheduledAllocationReport> copyState(
            BandwidthManager<T, C> copyFrom) {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();
        List<UnscheduledAllocationReport> unscheduled = Collections.emptyList();
        IBandwidthDao<T, C> fromDao = copyFrom.bandwidthDao;

        final boolean proposingBandwidthChange = retrievalManager
                .isProposingBandwidthChanges(copyFrom.retrievalManager);
        if (proposingBandwidthChange) {

            retrievalManager.initRetrievalPlans();

            // Proposing bandwidth changes requires the old way of bringing up a
            // fresh bandwidth manager and trying the change from scratch
            unscheduled = new ArrayList<>();
            Set<String> subscriptionIds = new HashSet<>();
            for (BandwidthAllocation ba : fromDao.getBandwidthAllocations()) {
                subscriptionIds.add(ba.getSubscriptionId());
            }

            Set<Subscription<T, C>> actualSubscriptions = new HashSet<>();
            for (String subId : subscriptionIds) {
                try {
                    Subscription<T, C> actualSubscription = DataDeliveryHandlers
                            .getSubscriptionHandler().getById(subId);
                    actualSubscriptions.add(actualSubscription);
                } catch (RegistryHandlerException e) {
                    logger.error(
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
            final List<BandwidthAllocation> subscriptionRetrievals = fromDao
                    .getBandwidthAllocations();

            bandwidthDao.store(subscriptionRetrievals);

            RetrievalManager fromRetrievalManager = copyFrom.retrievalManager;
            this.retrievalManager.copyState(fromRetrievalManager);
        }

        timer.stop();
        timer.logLaps("copyingState()", performanceHandler);

        return unscheduled;
    }

    /**
     * Determine the latency that would be required on the subscription for it
     * to be fully scheduled.
     *
     * @param subscription
     *            the subscription
     * @return the required latency, in minutes
     */
    private int determineRequiredLatency(
            final Subscription<T, C> subscription) {

        ITimer timer = TimeUtil.getTimer();
        timer.start();

        int requiredLatency = -1;
        int defaultLatency = subscription.getLatencyInMinutes();

        // Attempt to schedule with latency doubled default
        Subscription<T, C> clone = subscription.copy();
        clone.setLatencyInMinutes(defaultLatency * 2);
        BandwidthMap copyOfCurrentMap = BandwidthMap
                .load(InMemoryBandwidthContextFactory.getBandwidthMapConfig());
        Set<UnscheduledAllocationReport> conflicts = findScheduleConflicts(
                clone, copyOfCurrentMap);

        /*
         * If doubling latency allows subscription to be scheduled, determine
         * the minimum increase needed to allow scheduling. If doubling latency
         * is not enough, do not recommend raising latency beyond double
         * recommended as this could cause bandwidth to be maxed out even as
         * retrievals fall farther behind.
         */
        if (conflicts.isEmpty()) {
            SortedSet<Integer> possibleValues = new TreeSet<>();
            for (int i = defaultLatency; i <= (defaultLatency * 2); i++) {
                possibleValues.add(Integer.valueOf(i));
            }
            IBinarySearchResponse<Integer> response = AlgorithmUtil
                    .binarySearch(possibleValues, new Comparable<Integer>() {
                        @Override
                        public int compareTo(Integer valueToCheck) {
                            clone.setLatencyInMinutes(valueToCheck);

                            boolean valueWouldWork = findScheduleConflicts(
                                    clone, copyOfCurrentMap).isEmpty();

                            /*
                             * Check if one more restrictive value would not
                             * work, if so then this is the required value,
                             * otherwise keep searching
                             */
                            if (valueWouldWork) {
                                clone.setLatencyInMinutes(valueToCheck - 1);

                                return findScheduleConflicts(clone,
                                        copyOfCurrentMap).isEmpty() ? 1 : 0;
                            }
                            // This would still be unscheduled
                            return -1;
                        }
                    });

            final Integer binarySearchedValue = response.getItem();
            if (binarySearchedValue != null) {
                requiredLatency = binarySearchedValue;

                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                            "Found required latency of [%s] in [%s] iterations",
                            binarySearchedValue, response.getIterations()));
                }
            } else {
                logger.warn("Unable to find the required latency "
                        + "with a binary search");
            }

            // Add in buffer for bucket size
            requiredLatency += retrievalManager.getPlan(subscription.getRoute())
                    .getBucketMinutes();
        }
        timer.stop();

        return requiredLatency;
    }

    /**
     * Reset specifically the EdexBandwidthManager.
     *
     * This method must be implemented in EdexBandwidthManager so that it can
     * "reshuffle the deck" of Retrieval Plans, etc. It is empty for this
     * implementation.
     * <p>
     * The EdexBandwidthManageris the only BandwidthManager class that needs to
     * be able to reset itself in response to operations from within the
     * abstract BandwidthManager.handleRequest method processing. Presently,
     * this method must be empty in all other implementations.
     */
    protected abstract void resetBandwidthManager(Network requestNetwork,
            String resetReasonMessage);

    /**
     * Order list by Subscriptions Priority We want highest priority
     * subscriptions scheduled first.
     *
     * @param insubscriptions
     * @return
     */
    protected List<Subscription<T, C>> orderSubscriptionsByPriority(
            List<Subscription<T, C>> insubscriptions) {

        List<Subscription<T, C>> subscriptions = new ArrayList<>(
                insubscriptions.size());
        subscriptions.addAll(insubscriptions);
        Collections.sort(subscriptions);

        return subscriptions;
    }

    /**
     * Called after a BandwidthManager has been created to initialize scheduling
     * with the given subscriptions in preparation for operation.
     *
     * @param subMap
     *            map of subscriptions to initialize scheduling with
     * @throws SerializationException
     *
     * @Returns a list of the names of the subscriptions that were not
     *          scheduled.
     */
    @SuppressWarnings("rawtypes")
    public abstract List<String> initializeScheduling(
            Map<Network, List<Subscription>> subMap)
            throws SerializationException;

    public void setBandwidthChangedCallback(
            IBandwidthChangedCallback bandwidthChangedCallback) {
        this.bandwidthChangedCallback = bandwidthChangedCallback;
    }

    public IBandwidthChangedCallback getBandwidthChangedCallback() {
        return bandwidthChangedCallback;
    }

    /**
     * Purge all bandwidth allocations before current time.
     */
    public void purgeAllocations() {
        Date threshold = TimeUtil.newDate();
        try {
            bandwidthDao.purgeBandwidthAllocationsBeforeDate(threshold);
        } catch (DataAccessLayerException e) {
            logger.error(
                    "Failed to purge allocations before [" + threshold + "]",
                    e);
        }
    }
}
