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
package com.raytheon.uf.edex.datadelivery.retrieval.handlers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.raytheon.uf.common.datadelivery.event.status.DataDeliverySystemStatusDefinition;
import com.raytheon.uf.common.datadelivery.event.status.DataDeliverySystemStatusEvent;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.time.domain.api.IDuration;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalDao;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord.State;
import com.raytheon.uf.edex.registry.ebxml.init.RegistryInitializedListener;

/**
 * Provider Retrieval Handler
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 07, 2011           dhladky   Initial creation
 * Aug 09, 2012  1022     djohnson  Use {@link ExecutorService} for retrieval.
 * Mar 04, 2013  1647     djohnson  RetrievalTasks are now scheduled via
 *                                  constructor parameter.
 * Mar 27, 2013  1802     bphillip  Scheduling of retrieval tasks now occurs
 *                                  after camel/spring have been initialized
 * Jan 30, 2014  2686     dhladky   refactor of retrieval.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Jul 27, 2017  6186     rjpeter   Removed Qpid queue to allow for priority
 *                                  filtering.
 * Aug 02, 2017  6186     rjpeter   Added queueRetrievals and notifyRetrieval.
 *
 * </pre>
 *
 * @author dhladky
 */
@Service
public class RetrievalHandler implements RegistryInitializedListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, DataDeliverySystemStatusDefinition> systemNameToStateMap = Collections
            .synchronizedMap(
                    new HashMap<String, DataDeliverySystemStatusDefinition>());

    private final ScheduledExecutorService scheduledExecutorService;

    private final RetrievalDao retrievalDao;

    private final IDuration subnotifyTaskFrequency;

    private final SubscriptionNotifyTask subNotifyTask;

    private final RetrievalTask retrievalTask;

    private final Object notifier = new Object();

    private final RetrievalThread[] retrievalThreads;

    public RetrievalHandler(ScheduledExecutorService scheduledExecutorService,
            RetrievalDao retrievalDao, SubscriptionNotifyTask subNotifyTask,
            IDuration subnotifyTaskFrequency, RetrievalTask retrievalTask,
            int numRetrievalThreads) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.retrievalDao = retrievalDao;
        this.subNotifyTask = subNotifyTask;
        this.subnotifyTaskFrequency = subnotifyTaskFrequency;
        this.retrievalTask = retrievalTask;
        retrievalThreads = new RetrievalThread[numRetrievalThreads];
        for (int i = 0; i < numRetrievalThreads; i++) {
            retrievalThreads[i] = new RetrievalThread();
            retrievalThreads[i].setName("RetrievalThread-" + i);
        }
    }

    private class RetrievalThread extends Thread {
        @Override
        public void run() {
            while (!EDEXUtil.isShuttingDown()) {
                try {
                    if (!scanForRetrievals()) {
                        synchronized (notifier) {
                            try {
                                notifier.wait(30_000);
                            } catch (InterruptedException e) {
                                // ignore
                            }
                        }
                    }
                } catch (Throwable t) {
                    logger.error("Error occurred processing retrievals", t);
                }
            }
        }
    }

    @Override
    public void executeAfterRegistryInit() {
        // set all Running state retrievals to pending
        retrievalDao.resetRunningRetrievalsToPending();
        // run the sub notifier every 30 sec for notifications
        scheduledExecutorService.scheduleWithFixedDelay(subNotifyTask, 30_000,
                subnotifyTaskFrequency.getMillis(), TimeUnit.MILLISECONDS);
        for (RetrievalThread thread : retrievalThreads) {
            thread.start();
        }
    }

    public void queueRetrievals(
            List<RetrievalRequestRecord> retrievalRequests) {
        retrievalDao.persistAll(retrievalRequests);
        notifyRetrieval();
    }

    public void notifyRetrieval() {
        synchronized (notifier) {
            notifier.notifyAll();
        }
    }

    /*
     * TODO: Add task to fail any retrieval that has WAITING_RESPONSE for more
     * than X minutes
     */
    /**
     * Scans database for retrievals waiting to process. Return true if an entry
     * was processed, false if nothing was processed and thread should sleep.
     */
    public boolean scanForRetrievals() {
        RetrievalRequestRecord rec = null;

        try {
            rec = retrievalDao.activateNextRetrievalRequest();
        } catch (Exception e) {
            logger.error("Error occurred looking up next retrieval", e);
            return false;
        }

        if (rec != null) {
            Retrieval retrieval = null;
            try {
                try {
                    retrieval = rec.getRetrievalObj();
                } catch (SerializationException e) {
                    logger.error("Error occurred deserialization retrieval ["
                            + rec + "]", e);
                    rec.setState(State.FAILED);
                    retrievalDao.update(rec);
                    return true;
                }

                // TODO: return state instead of success to allow for multi
                // stage and async retrievals
                boolean success = false;
                try {
                    success = retrievalTask.processRetrieval(retrieval,
                            rec.getPriority());
                } catch (Exception e) {
                    logger.error("Error occurred processing retrieval ["
                            + retrieval + "]", e);
                }

                rec.setState(success ? State.COMPLETED : State.FAILED);
                retrievalDao.completeRetrievalRequest(rec);
            } catch (Exception e) {
                logger.error("Error occurred updating retrieval record [" + rec
                        + "]", e);
            }

            updateSystemStatus(retrieval.getProvider(), rec.getState());
            return true;
        }

        return false;
    }

    protected void updateSystemStatus(String provider, State retrievalState) {
        // Create system status event
        DataDeliverySystemStatusDefinition newSystemStatusState;
        if (retrievalState != null) {
            switch (retrievalState) {
            case COMPLETED:
            case PENDING:
            case RUNNING:
                newSystemStatusState = DataDeliverySystemStatusDefinition.UP;
                break;
            case FAILED:
            default:
                newSystemStatusState = DataDeliverySystemStatusDefinition.UNKNOWN;
                break;
            }
        } else {
            newSystemStatusState = DataDeliverySystemStatusDefinition.UNKNOWN;
        }

        DataDeliverySystemStatusDefinition existingState = systemNameToStateMap
                .get(provider);
        if (existingState == null
                || !existingState.equals(newSystemStatusState)) {
            systemNameToStateMap.put(provider, newSystemStatusState);
            DataDeliverySystemStatusEvent event = new DataDeliverySystemStatusEvent();
            event.setName(provider);
            event.setSystemType("Provider");
            event.setStatus(newSystemStatusState);
            EventBus.publish(event);
        }

    }
}
