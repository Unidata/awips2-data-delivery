
/**
 * 
 */
package com.raytheon.uf.edex.datadelivery.bandwidth.retrieval;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.ProviderType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionBundle;
import com.raytheon.uf.common.datadelivery.registry.handlers.IProviderHandler;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig.RETRIEVAL_MODE;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.ITimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.edex.core.EdexException;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrieval;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalManagerNotifyEvent;
import com.raytheon.uf.edex.datadelivery.retrieval.db.IRetrievalDao;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecordPK;
import com.raytheon.uf.edex.datadelivery.retrieval.handlers.AsyncRetrievalBroker;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.ServiceTypeFactory;
import com.raytheon.uf.edex.datadelivery.retrieval.response.AsyncRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.util.RetrievalGeneratorUtilities;

/**
 * Class used to process SubscriptionRetrieval BandwidthAllocations.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 27, 2012 726        jspinks      Initial release.
 * Oct 10, 2012 0726       djohnson     Add generics, constants, defaultPriority.
 * Nov 26, 2012            dhladky      Override default ingest routes based on plugin
 * Jan 30, 2013 1543       djohnson     Should not implement IRetrievalHandler.
 * Feb 05, 2013 1580       mpduff       EventBus refactor.
 * Jun 24, 2013 2106       djohnson     Set actual start time when sending to retrieval rather than overwrite scheduled start.
 * Jul 09, 2013 2106       djohnson     Dependency inject registry handlers.
 * Jul 11, 2013 2106       djohnson     Use SubscriptionPriority enum.
 * Jan 15, 2014 2678       bgonzale     Use Queue for passing RetrievalRequestRecords to the 
 *                                      RetrievalTasks (PerformRetrievalsThenReturnFinder).
 *                                      Added constructor that sets the retrievalQueue to null.
 * Jan 30, 2014 2686       dhladky      refactor of retrieval.
 * Feb 10, 2014 2678       dhladky      Prevent duplicate allocations.
 * Jul 22, 2014 2732       ccody        Add Date Time to SubscriptionRetrievalEvent message
 * Feb 19, 2015 3998       dhladky      Fixed wrong date on notification center retrieval message.
 * May 27, 2015  4531      dhladky      Remove excessive Calendar references.
 * Apr 06, 2016 5424       dhladky      Allow for ASYNC processing of retrievals.
 * 
 * </pre>
 * 
 * @version 1.0
 */
public class SubscriptionRetrievalAgent extends
        RetrievalAgent<SubscriptionRetrieval> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SubscriptionRetrievalAgent.class);

    private final int defaultPriority;

    private final IBandwidthDao<?, ?> bandwidthDao;

    private final IRetrievalDao retrievalDao;

    private final IProviderHandler providerHandler;
    
    private final AsyncRetrievalBroker broker = AsyncRetrievalBroker.getInstance();

    public SubscriptionRetrievalAgent(Network network, String retrievalRoute,
            String asyncRetrievalUri, final Object notifier,
            int defaultPriority, RetrievalManager retrievalManager,
            IBandwidthDao<?, ?> bandwidthDao, IRetrievalDao retrievalDao,
            IProviderHandler providerHandler) {
        super(network, retrievalRoute, asyncRetrievalUri, notifier,
                retrievalManager);
        this.defaultPriority = defaultPriority;
        this.bandwidthDao = bandwidthDao;
        this.retrievalDao = retrievalDao;
        this.providerHandler = providerHandler;
    }

    @Override
    void processAllocations(List<SubscriptionRetrieval> subRetrievals)
            throws EdexException {

        SubscriptionBundle bundle = new SubscriptionBundle();
        ConcurrentHashMap<Subscription<?, ?>, SubscriptionRetrieval> retrievalsMap = new ConcurrentHashMap<Subscription<?, ?>, SubscriptionRetrieval>();

        // Get subs from allocations and search for duplicates
        for (SubscriptionRetrieval subRetrieval : subRetrievals) {

            Subscription<?, ?> sub = null;

            try {
                sub = bandwidthDao.getSubscriptionRetrievalAttributes(
                        subRetrieval).getSubscription();
            } catch (SerializationException e) {
                throw new EdexException(
                        "Unable to deserialize the subscription.", e);
            }

            /**
             * We only allow one subscription retrieval per DSM update. Remove
             * any duplicate subscription allocations/retrievals, cancel them.
             */
            if (!retrievalsMap.containsKey(sub)) {
                retrievalsMap.put(sub, subRetrieval);
            } else {
                // Check for most recent startTime, that's the one we want for
                // retrieval.
                SubscriptionRetrieval currentRetrieval = retrievalsMap.get(sub);
                if (subRetrieval.getStartTime().after(
                        currentRetrieval.getStartTime())) {
                    // Replace it in the map, set previous to canceled.
                    currentRetrieval.setStatus(RetrievalStatus.CANCELLED);
                    bandwidthDao.update(currentRetrieval);
                    retrievalsMap.replace(sub, subRetrieval);
                    statusHandler
                            .info("More recent, setting previous allocation to Cancelled ["
                                    + currentRetrieval.getIdentifier()
                                    + "] "
                                    + sub.getName());
                } else {
                    // Not more recent, cancel
                    subRetrieval.setStatus(RetrievalStatus.CANCELLED);
                    bandwidthDao.update(subRetrieval);
                    statusHandler.info("Older, setting to Cancelled ["
                            + currentRetrieval.getIdentifier() + "] "
                            + sub.getName());
                }
            }
        }

        for (Entry<Subscription<?, ?>, SubscriptionRetrieval> entry : retrievalsMap
                .entrySet()) {

            SubscriptionRetrieval retrieval = entry.getValue();
            Subscription<?, ?> sub = entry.getKey();
            final String originalSubName = sub.getName();

            Provider provider = getProvider(sub.getProvider());
            if (provider == null) {
                statusHandler
                        .error("provider was null, skipping subscription ["
                                + originalSubName + "]");
                return;
            }
            bundle.setBundleId(sub.getSubscriptionId());
            bundle.setPriority(retrieval.getPriority());
            bundle.setProvider(provider);
            bundle.setConnection(provider.getConnection());
            bundle.setSubscription(sub);
            ServiceType type = provider.getServiceType();

            retrieval.setActualStart(TimeUtil.newDate());
            retrieval.setStatus(RetrievalStatus.RETRIEVAL);

            // update database
            bandwidthDao.update(retrieval);

            /*
             * generateRetrieval will pipeline the RetrievalRecord Objects
             * created to the DB. The PK objects returned are sent to the
             * RetrievalQueue for processing.
             */
            List<RetrievalRequestRecordPK> retrievals = generateRetrieval(
                    bundle, retrieval.getIdentifier());

            if (!CollectionUtil.isNullOrEmpty(retrievals)) {

                if (getRetrievalMode(type) == RETRIEVAL_MODE.SYNC) {
                    try {
                        Object[] payload = retrievals.toArray();
                        RetrievalGeneratorUtilities.sendToRetrieval(
                                retrievalRoute, network, payload);
                    } catch (Exception e) {
                        statusHandler.handle(Priority.PROBLEM,
                                "Couldn't send RetrievalRecords to Queue!", e);
                    }
                    statusHandler.info("Sent " + retrievals.size()
                            + " retrieval(s) to queue. " + network.toString());
                } else {

                    for (RetrievalRequestRecordPK pk : retrievals) {

                        AsyncRetrievalResponse ars = broker.getRetrieval(pk.toString());
                        
                        if (ars != null) {
                            try {
                                RetrievalGeneratorUtilities
                                        .sendToAsyncRetrieval(
                                                asyncRetrievalRoute, ars);
                            } catch (Exception e) {
                                statusHandler
                                        .handle(Priority.PROBLEM,
                                                "Couldn't send RetrievalRecords to Async Queue!",
                                                e);
                            }
                        } else {
                            statusHandler
                                    .info("Processed "
                                            + retrievals.size()
                                            + " retrieval(s), awaiting provider trigger. "
                                            + network.toString());
                        }
                    }
                }

            } else {
                /**
                 * Normally this is the job of the SubscriptionNotifyTask, but
                 * if no retrievals were generated we have to send it manually
                 */
                RetrievalManagerNotifyEvent retrievalManagerNotifyEvent = new RetrievalManagerNotifyEvent();
                retrievalManagerNotifyEvent.setId(Long.toString(retrieval
                        .getId()));
                EventBus.publish(retrievalManagerNotifyEvent);
            }
        }
    }

    @Override
    protected String getAgentType() {
        return SUBSCRIPTION_AGENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Class<SubscriptionRetrieval> getAllocationTypeClass() {
        return SubscriptionRetrieval.class;
    }

    /**
     * Generate the retrievals for a subscription bundle.
     * 
     * @param bundle
     *            the bundle
     * @param subRetrievalKey
     *            the subscription retrieval key
     * @return true if retrievals were generated (and waiting to be processed)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<RetrievalRequestRecordPK> generateRetrieval(
            SubscriptionBundle bundle, Long subRetrievalKey) {

        // process the bundle into a retrieval
        RetrievalGenerator rg = ServiceTypeFactory.retrieveServiceFactory(
                bundle.getProvider()).getRetrievalGenerator();

        final String subscriptionName = bundle.getSubscription().getName();
        statusHandler.info("Subscription: " + subscriptionName
                + " Being Processed for Retrieval...");

        List<Retrieval> retrievals = rg.buildRetrieval(bundle);
        List<RetrievalRequestRecord> requestRecords = null;
        List<RetrievalRequestRecordPK> requestRecordPKs = null;
        boolean retrievalsGenerated = !CollectionUtil.isNullOrEmpty(retrievals);

        // Default to "now"
        Long requestRetrievalTimeLong = Long
                .valueOf(System.currentTimeMillis());
        Subscription<?, ?> bundleSub = bundle.getSubscription();
        com.raytheon.uf.common.datadelivery.registry.Time requestRetrievalTimeT = bundleSub
                .getTime();
        Date requestRetrievalDate = null;

        if (requestRetrievalTimeT != null) {
            // cases of starting date set
            if (requestRetrievalTimeT.getRequestStart() != null) {
                requestRetrievalDate = requestRetrievalTimeT.getRequestStart();
            } else {
                requestRetrievalDate = requestRetrievalTimeT.getStart();
            }
            // cases of ending date set
            if (requestRetrievalDate == null) {
                if (requestRetrievalTimeT.getRequestEnd() != null) {
                    requestRetrievalDate = requestRetrievalTimeT
                            .getRequestEnd();
                } else {
                    requestRetrievalDate = requestRetrievalTimeT.getEnd();
                }
            }
            // set the date of retrieved data
            if (requestRetrievalDate != null) {
                requestRetrievalTimeLong = Long.valueOf(requestRetrievalDate
                        .getTime());
            }
        }

        if (retrievalsGenerated) {
            String owner = bundle.getSubscription().getOwner();
            String provider = bundle.getSubscription().getProvider();

            int priority = (bundle.getPriority() != null) ? bundle
                    .getPriority().getPriorityValue() : defaultPriority;
            Date insertTime = TimeUtil.newDate();
            requestRecords = new ArrayList<RetrievalRequestRecord>(
                    retrievals.size());
            requestRecordPKs = new ArrayList<RetrievalRequestRecordPK>(
                    retrievals.size());

            ITimer timer = TimeUtil.getTimer();
            timer.start();

            final int numberOfRetrievals = retrievals.size();
            final ProviderType providerType = bundle.getProvider()
                    .getProviderType(bundle.getDataType());
            final String plugin = providerType.getPlugin();
            for (int i = 0; i < numberOfRetrievals; i++) {
                Retrieval retrieval = retrievals.get(i);
                RetrievalRequestRecord rec = new RetrievalRequestRecord(
                        subscriptionName, i, subRetrievalKey);
                /**
                 * Stored ONLY as Retrieval (Thrift) data; not part of the
                 * record proper
                 */
                retrieval.setRequestRetrievalTime(requestRetrievalTimeLong);

                rec.setOwner(owner);
                rec.setPriority(priority);
                rec.setInsertTime(insertTime);
                rec.setNetwork(retrieval.getNetwork());
                rec.setProvider(provider);
                rec.setPlugin(plugin);
                rec.setSubscriptionType(retrieval.getSubscriptionType());

                try {
                    rec.setRetrieval(SerializationUtil
                            .transformToThrift(retrieval));
                    rec.setState(RetrievalRequestRecord.State.PENDING);
                    requestRecords.add(rec);
                    requestRecordPKs.add(rec.getId());
                } catch (Exception e) {
                    statusHandler.error("Subscription: " + subscriptionName
                            + " Failed to serialize request [" + retrieval
                            + "]", e);
                    rec.setRetrieval(new byte[0]);
                    rec.setState(RetrievalRequestRecord.State.FAILED);
                }
            }

            timer.stop();
            statusHandler.info("Cumulative time to create ["
                    + numberOfRetrievals + "] request records ["
                    + timer.getElapsedTime() + "] ms");

            try {
                timer.reset();
                timer.start();

                retrievalDao.persistAll(requestRecords);

                timer.stop();
                statusHandler.info("Time to persist requests to db ["
                        + timer.getElapsedTime() + "] ms");
            } catch (Exception e) {
                statusHandler.handle(Priority.WARN, "Subscription: "
                        + subscriptionName + " Failed to store to retrievals.",
                        e);
                requestRecordPKs.clear();
            }
        } else {
            statusHandler.warn("Subscription: " + subscriptionName
                    + " Did not generate any retrieval messages");
        }

        return requestRecordPKs;
    }

    /**
     * Get the provider by name
     * @param providerName
     * @return
     */
    private Provider getProvider(String providerName) {
        try {
            return providerHandler.getByName(providerName);
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to retrieve provider by name.", e);
            return null;
        }
    }

}