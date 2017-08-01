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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.ProviderHandler;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.time.util.ITimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator.RETRIEVAL_MODE;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;
import com.raytheon.uf.edex.datadelivery.retrieval.handlers.RetrievalHandler;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.ServiceTypeFactory;

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
 * Jul 27, 2017  6186     rjpeter   Remove asyncBroker.
 * Aug 02, 2017  6186     rjpeter   Refactored to queueRetrievals directly.
 *
 * </pre>
 *
 */
public class SubscriptionRetrievalAgent {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final int defaultPriority;

    private final RetrievalHandler retrievalHandler;

    private final ProviderHandler providerHandler;

    public SubscriptionRetrievalAgent(int defaultPriority,
            RetrievalHandler retrievalHandler,
            ProviderHandler providerHandler) {
        this.defaultPriority = defaultPriority;
        this.retrievalHandler = retrievalHandler;
        this.providerHandler = providerHandler;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void queueRetrievals(
            DataSetMetaData<? extends Time, ? extends Coverage> dsmd,
            List<? extends Subscription> subscriptions) {
        Provider provider = null;

        try {
            provider = providerHandler.getByName(dsmd.getProviderName());

            if (provider == null) {
                logger.error("No provider for name [" + dsmd.getProviderName()
                        + "] found. All subscriptions for DataSet ["
                        + dsmd.getDataSetName() + "] will be skipped");
                return;
            }
        } catch (RegistryHandlerException e) {
            logger.error("Error looking up provider [" + dsmd.getProviderName()
                    + "]. All subscriptions for DataSet ["
                    + dsmd.getDataSetName() + "] will be skipped", e);
            return;
        }

        // process the bundle into a retrieval
        RetrievalGenerator rg = ServiceTypeFactory
                .retrieveServiceFactory(provider).getRetrievalGenerator();

        for (Subscription<?, ?> sub : subscriptions) {
            final String subscriptionName = sub.getName();
            logger.info("Subscription: " + subscriptionName
                    + " Being Processed for Retrieval...");

            List<Retrieval> retrievals = rg.buildRetrieval(dsmd, sub, provider);
            boolean retrievalsGenerated = !CollectionUtil
                    .isNullOrEmpty(retrievals);

            if (retrievalsGenerated) {
                queueSubscriptionRetrievals(rg, dsmd, sub, retrievals);
            } else {
                logger.error("Subscription [" + subscriptionName
                        + "] did not generate any retrieval messages for dataSet ["
                        + dsmd.getDataSetName() + "], url [" + dsmd.getUrl()
                        + "]");
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean queueSubscriptionRetrievals(RetrievalGenerator rg,
            DataSetMetaData<?, ?> dsmd, Subscription<?, ?> subscription,
            List<Retrieval> retrievals) {
        List<RetrievalRequestRecord> requestRecords = null;

        // Default to "now"
        Long requestRetrievalTimeLong = Long
                .valueOf(System.currentTimeMillis());
        Time requestRetrievalTimeT = subscription.getTime();
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

        int priority = (subscription.getPriority() != null)
                ? subscription.getPriority().getPriorityValue()
                : defaultPriority;
        requestRecords = new ArrayList<>(retrievals.size());

        ITimer timer = TimeUtil.getTimer();
        timer.start();
        RetrievalRequestRecord.State retrievalState = (RETRIEVAL_MODE.SYNC
                .equals(rg.getRetrievalMode())
                        ? RetrievalRequestRecord.State.PENDING
                        : RetrievalRequestRecord.State.WAITING_RESPONSE);

        for (Retrieval retrieval : retrievals) {
            retrieval.setRequestRetrievalTime(requestRetrievalTimeLong);
            RetrievalRequestRecord rec = new RetrievalRequestRecord(retrieval,
                    dsmd.getUrl(), retrievalState, priority);

            try {
                rec.setRetrievalObj(retrieval);
                requestRecords.add(rec);
            } catch (Exception e) {
                logger.error(
                        "Failed to serialize retrieval [" + retrieval + "]", e);
                rec.setRetrieval(new byte[0]);
                rec.setState(RetrievalRequestRecord.State.FAILED);
            }
        }

        timer.stop();
        logger.info("Cumulative time to create [" + retrievals.size()
                + "] request records [" + timer.getElapsedTime() + "] ms");

        try {
            timer.reset();
            timer.start();

            retrievalHandler.queueRetrievals(requestRecords);

            timer.stop();
            logger.info("Time to persist requests to db ["
                    + timer.getElapsedTime() + "] ms");

        } catch (Exception e) {
            logger.error("Subscription: " + subscription.getName()
                    + " Failed to store to retrievals.", e);
            return false;
        }

        timer.reset();
        timer.start();
        List<RetrievalRequestRecord> updates = rg.postSaveActions(dsmd,
                subscription, requestRecords);
        if (!CollectionUtil.isNullOrEmpty(updates)) {
            retrievalHandler.queueRetrievals(updates);
        }

        timer.stop();
        if (timer.getElapsedTime() > 0) {
            logger.info("Retrieval Post Save actions took "
                    + timer.getElapsedTime() + "ms");
        }

        return true;
    }
}
