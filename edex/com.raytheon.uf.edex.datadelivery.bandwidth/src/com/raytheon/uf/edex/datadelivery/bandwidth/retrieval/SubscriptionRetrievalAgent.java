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
package com.raytheon.uf.edex.datadelivery.bandwidth.retrieval;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.ProviderType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionBundle;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.registry.handlers.ProviderHandler;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig.RETRIEVAL_MODE;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.time.util.ITimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.edex.core.EdexException;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription;
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
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 27, 2012  726      jspinks   Initial release.
 * Oct 10, 2012  726      djohnson  Add generics, constants, defaultPriority.
 * Nov 26, 2012           dhladky   Override default ingest routes based on
 *                                  plugin
 * Jan 30, 2013  1543     djohnson  Should not implement IRetrievalHandler.
 * Feb 05, 2013  1580     mpduff    EventBus refactor.
 * Jun 24, 2013  2106     djohnson  Set actual start time when sending to
 *                                  retrieval rather than overwrite scheduled
 *                                  start.
 * Jul 09, 2013  2106     djohnson  Dependency inject registry handlers.
 * Jul 11, 2013  2106     djohnson  Use SubscriptionPriority enum.
 * Jan 15, 2014  2678     bgonzale  Use Queue for passing
 *                                  RetrievalRequestRecords to the
 *                                  RetrievalTasks
 *                                  (PerformRetrievalsThenReturnFinder). Added
 *                                  constructor that sets the retrievalQueue to
 *                                  null.
 * Jan 30, 2014  2686     dhladky   refactor of retrieval.
 * Feb 10, 2014  2678     dhladky   Prevent duplicate allocations.
 * Jul 22, 2014  2732     ccody     Add Date Time to SubscriptionRetrievalEvent
 *                                  message
 * Feb 19, 2015  3998     dhladky   Fixed wrong date on notification center
 *                                  retrieval message.
 * May 27, 2015  4531     dhladky   Remove excessive Calendar references.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Apr 06, 2016  5424     dhladky   Allow for ASYNC processing of retrievals.
 * Apr 20, 2017  6186     rjpeter   Allow multiple subscriptions to the same
 *                                  dataSet per allocation.
 * May 22, 2017  6130     tjensen   Add DataSetName to RetrievalRequestRecord
 *
 * </pre>
 *
 */
public class SubscriptionRetrievalAgent
        extends RetrievalAgent<SubscriptionRetrieval> {
    private final int defaultPriority;

    private final IBandwidthDao<?, ?> bandwidthDao;

    private final IRetrievalDao retrievalDao;

    private final ProviderHandler providerHandler;

    private final AsyncRetrievalBroker broker = AsyncRetrievalBroker
            .getInstance();

    public SubscriptionRetrievalAgent(Network network, String retrievalRoute,
            String asyncRetrievalUri, final Object notifier,
            int defaultPriority, RetrievalManager retrievalManager,
            IBandwidthDao<?, ?> bandwidthDao, IRetrievalDao retrievalDao,
            ProviderHandler providerHandler) {
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
        Map<String, List<SubscriptionRetrieval>> retrievalUrlMap = new HashMap<>();

        for (SubscriptionRetrieval subRetrieval : subRetrievals) {
            String url = subRetrieval.getUrl();
            List<SubscriptionRetrieval> retrievals = retrievalUrlMap.get(url);

            if (retrievals == null) {
                retrievals = new LinkedList<>();
                retrievalUrlMap.put(url, retrievals);
            }

            /*
             * Could have multiple subscriptions to the same dataset for
             * different parameters
             */
            retrievals.add(subRetrieval);
        }

        for (Entry<String, List<SubscriptionRetrieval>> entry : retrievalUrlMap
                .entrySet()) {
            String url = entry.getKey();
            DataSetMetaData<?, ?> dsmd = null;
            try {
                dsmd = DataDeliveryHandlers.getDataSetMetaDataHandler()
                        .getById(url);
                if (dsmd == null) {
                    logger.error("No DataSetMetaData found for url [" + url
                            + "]. Skipping retrieval");
                    continue;
                }
            } catch (RegistryHandlerException e) {
                logger.error("Unable to look up DataSetMetaData[" + url
                        + "], skipping associated retrievals", e);
                continue;
            }

            List<SubscriptionRetrieval> subRetrievalsForUrl = entry.getValue();
            for (SubscriptionRetrieval retrieval : subRetrievalsForUrl) {
                /*
                 * TODO: Should we check/remove the same sub in list twice? Is
                 * there a bug in generation of SubscriptionRetrieval objects?
                 */
                BandwidthSubscription bwSub = retrieval
                        .getBandwidthSubscription();
                final String subName = bwSub.getName();
                Provider provider = null;

                try {
                    provider = providerHandler.getByName(bwSub.getProvider());
                    if (provider == null) {
                        logger.error(
                                "No provider for name [" + bwSub.getProvider()
                                        + "]. Skipping retrieval for subscription ["
                                        + subName + "]");
                    }
                } catch (RegistryHandlerException e) {
                    logger.error("Error looking up provider ["
                            + bwSub.getProvider() + "] for subscription ["
                            + subName + "], skipping retrieval", e);
                    continue;
                }

                Subscription<?, ?> sub = null;

                try {
                    // TODO: Could be adhoc or recurring
                    sub = bandwidthDao
                            .getSubscriptionRetrievalAttributes(retrieval)
                            .getSubscription();
                    if (sub == null) {
                        logger.error("Unable to find subscription [" + subName
                                + "] based on retrieval attributes. Skipping retrieval");
                        continue;
                    }
                } catch (SerializationException e) {
                    logger.error(
                            "Unable to deserialize subscription, skipping retrieval for subscription ["
                                    + subName + "]",
                            e);
                    continue;
                }

                SubscriptionBundle bundle = new SubscriptionBundle(sub,
                        provider.getConnection(), provider);
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
                        dsmd, bundle, retrieval.getIdentifier());

                if (!CollectionUtil.isNullOrEmpty(retrievals)) {

                    if (getRetrievalMode(type) == RETRIEVAL_MODE.SYNC) {
                        try {
                            Object[] payload = retrievals.toArray();
                            RetrievalGeneratorUtilities.sendToRetrieval(
                                    retrievalRoute, network, payload);
                        } catch (Exception e) {
                            logger.error(
                                    "Couldn't send RetrievalRecords to Queue!",
                                    e);
                        }
                        logger.info("Sent " + retrievals.size()
                                + " retrieval(s) to queue. "
                                + network.toString());
                    } else {

                        for (RetrievalRequestRecordPK pk : retrievals) {

                            AsyncRetrievalResponse ars = broker
                                    .getRetrieval(pk.toString());

                            if (ars != null) {
                                try {
                                    RetrievalGeneratorUtilities
                                            .sendToAsyncRetrieval(
                                                    asyncRetrievalRoute, ars);
                                } catch (Exception e) {
                                    logger.error(
                                            "Couldn't send RetrievalRecords to Async Queue!",
                                            e);
                                }
                            } else {
                                logger.info("Processed " + retrievals.size()
                                        + " retrieval(s), awaiting provider trigger. "
                                        + network.toString());
                            }
                        }
                    }

                } else {
                    /**
                     * Normally this is the job of the SubscriptionNotifyTask,
                     * but if no retrievals were generated we have to send it
                     * manually
                     */
                    RetrievalManagerNotifyEvent retrievalManagerNotifyEvent = new RetrievalManagerNotifyEvent();
                    retrievalManagerNotifyEvent
                            .setId(Long.toString(retrieval.getId()));
                    EventBus.publish(retrievalManagerNotifyEvent);
                }
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
     * @param dsmd
     *            the data set meta data
     * @param bundle
     *            the bundle
     * @return true if retrievals were generated (and waiting to be processed)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<RetrievalRequestRecordPK> generateRetrieval(
            DataSetMetaData<?, ?> dsmd, SubscriptionBundle bundle,
            Long subRetrievalKey) {

        // process the bundle into a retrieval
        RetrievalGenerator rg = ServiceTypeFactory
                .retrieveServiceFactory(bundle.getProvider())
                .getRetrievalGenerator();

        final String subscriptionName = bundle.getSubscription().getName();
        logger.info("Subscription: " + subscriptionName
                + " Being Processed for Retrieval...");

        List<Retrieval> retrievals = rg.buildRetrieval(dsmd, bundle);
        List<RetrievalRequestRecord> requestRecords = null;
        List<RetrievalRequestRecordPK> requestRecordPKs = null;
        boolean retrievalsGenerated = !CollectionUtil.isNullOrEmpty(retrievals);

        // Default to "now"
        Long requestRetrievalTimeLong = Long
                .valueOf(System.currentTimeMillis());
        Subscription<?, ?> bundleSub = bundle.getSubscription();
        Time requestRetrievalTimeT = bundleSub.getTime();
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
                requestRetrievalTimeLong = Long
                        .valueOf(requestRetrievalDate.getTime());
            }
        }

        if (retrievalsGenerated) {
            String owner = bundle.getSubscription().getOwner();
            String provider = bundle.getSubscription().getProvider();

            int priority = (bundle.getPriority() != null)
                    ? bundle.getPriority().getPriorityValue() : defaultPriority;
            Date insertTime = TimeUtil.newDate();
            requestRecords = new ArrayList<>(retrievals.size());
            requestRecordPKs = new ArrayList<>(retrievals.size());

            ITimer timer = TimeUtil.getTimer();
            timer.start();

            final int numberOfRetrievals = retrievals.size();
            final ProviderType providerType = bundle.getProvider()
                    .getProviderType(bundle.getDataType());
            final String plugin = providerType.getPlugin();
            for (int i = 0; i < numberOfRetrievals; i++) {
                Retrieval retrieval = retrievals.get(i);
                RetrievalRequestRecord rec = new RetrievalRequestRecord(
                        dsmd.getUrl(), subscriptionName, i, subRetrievalKey);

                /*
                 * Stored ONLY as Retrieval (Thrift) data; not part of the
                 * record proper
                 */
                retrieval.setRequestRetrievalTime(requestRetrievalTimeLong);

                rec.setOwner(owner);
                rec.setPriority(priority);
                rec.setInsertTime(insertTime);
                rec.setNetwork(retrieval.getNetwork());
                rec.setProvider(provider);
                rec.setDataSetName(dsmd.getDataSetName());
                rec.setPlugin(plugin);
                rec.setSubscriptionType(retrieval.getSubscriptionType());

                try {
                    rec.setRetrieval(
                            SerializationUtil.transformToThrift(retrieval));
                    rec.setState(RetrievalRequestRecord.State.PENDING);
                    requestRecords.add(rec);
                    requestRecordPKs.add(rec.getId());
                } catch (Exception e) {
                    logger.error("Subscription: " + subscriptionName
                            + " Failed to serialize request [" + retrieval
                            + "]", e);
                    rec.setRetrieval(new byte[0]);
                    rec.setState(RetrievalRequestRecord.State.FAILED);
                }
            }

            timer.stop();
            logger.info("Cumulative time to create [" + numberOfRetrievals
                    + "] request records [" + timer.getElapsedTime() + "] ms");

            try {
                timer.reset();
                timer.start();

                retrievalDao.persistAll(requestRecords);

                timer.stop();
                logger.info("Time to persist requests to db ["
                        + timer.getElapsedTime() + "] ms");
            } catch (Exception e) {
                logger.error("Subscription: " + subscriptionName
                        + " Failed to store to retrievals.", e);
                requestRecordPKs.clear();
            }
        } else {
            logger.warn("Subscription: " + subscriptionName
                    + " Did not generate any retrieval messages");
        }

        return requestRecordPKs;
    }
}
