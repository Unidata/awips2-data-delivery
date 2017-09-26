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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.raytheon.uf.common.datadelivery.event.status.DataDeliverySystemStatusDefinition;
import com.raytheon.uf.common.datadelivery.event.status.DataDeliverySystemStatusEvent;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.time.domain.api.IDuration;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalThreadsConfig;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalThreadsProvider;
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
 * Sep 21, 2017  6433     tgurney   Use per-provider retrieval threads
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

    private final List<RetrievalThread> retrievalThreads = new ArrayList<>();

    private final Map<String, Object> providerSyncObjects = new HashMap<>();

    private final Map<String, Object> providerNotifiers = new HashMap<>();

    public RetrievalHandler(ScheduledExecutorService scheduledExecutorService,
            RetrievalDao retrievalDao, SubscriptionNotifyTask subNotifyTask,
            IDuration subnotifyTaskFrequency, RetrievalTask retrievalTask)
            throws Exception {
        this.scheduledExecutorService = scheduledExecutorService;
        this.retrievalDao = retrievalDao;
        this.subNotifyTask = subNotifyTask;
        this.subnotifyTaskFrequency = subnotifyTaskFrequency;
        this.retrievalTask = retrievalTask;
        createRetrievalThreads();
    }

    /** Creates all retrieval threads listed in XML config. Call only once! */
    private void createRetrievalThreads() throws Exception {
        // Read config files into map with incremental override
        LocalizationLevel[] levels = new LocalizationLevel[] {
                LocalizationLevel.BASE, LocalizationLevel.SITE };
        Map<String, Integer> providerThreadCountMap = new HashMap<>();
        for (LocalizationLevel level : levels) {
            RetrievalThreadsConfig config = getRetrievalThreadsConfig(level);
            if (config != null) {
                for (RetrievalThreadsProvider provider : config
                        .getProviders()) {
                    providerThreadCountMap.put(provider.getName(),
                            provider.getThreads());
                }
            }
        }
        if (providerThreadCountMap.isEmpty()) {
            throw new IllegalArgumentException(
                    "No retrieval threads config was found");
        }
        // Create objects for per-provider synchronization and notify
        for (String provider : providerThreadCountMap.keySet()) {
            providerSyncObjects.put(provider, new Object());
            providerNotifiers.put(provider, new Object());
        }
        // Create the threads
        for (Entry<String, Integer> e : providerThreadCountMap.entrySet()) {
            for (int i = 1; i <= e.getValue(); i++) {
                String provider = e.getKey();
                Object notifier = providerNotifiers.get(provider);
                RetrievalThread thread = new RetrievalThread(provider,
                        notifier);
                thread.setName("Retrieval-" + provider + "-" + i);
                retrievalThreads.add(thread);
            }
        }
    }

    private RetrievalThreadsConfig getRetrievalThreadsConfig(
            LocalizationLevel level) throws JAXBException,
            SerializationException, LocalizationException {
        IPathManager pathManager = PathManagerFactory.getPathManager();
        LocalizationContext ctx = pathManager
                .getContext(LocalizationType.COMMON_STATIC, level);
        ILocalizationFile file = PathManagerFactory.getPathManager()
                .getLocalizationFile(ctx, "datadelivery/retrieval-threads.xml");
        RetrievalThreadsConfig config = null;
        if (file.exists()) {
            try (InputStream is = file.openInputStream()) {
                config = new JAXBManager(RetrievalThreadsConfig.class)
                        .unmarshalFromInputStream(RetrievalThreadsConfig.class,
                                is);
            } catch (IOException e) {
                logger.debug("Error on stream close", e);
            }
        }
        return config;
    }

    private class RetrievalThread extends Thread {

        private final String provider;

        private final Object notifier;

        public RetrievalThread(String provider, Object notifier) {
            this.provider = provider;
            this.notifier = notifier;
        }

        @Override
        public void run() {
            while (!EDEXUtil.isShuttingDown()) {
                try {
                    if (!scanForRetrievals(provider)) {
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
        Set<String> providersToNotify = new HashSet<>();
        for (RetrievalRequestRecord r : retrievalRequests) {
            providersToNotify.add(r.getProvider());
        }
        for (String provider : providersToNotify) {
            notifyRetrieval(provider);
        }
    }

    public void notifyRetrieval(String provider) {
        Object notifier = providerNotifiers.get(provider);
        boolean noProviderThreads = true;
        for (RetrievalThread r : retrievalThreads) {
            if (r.provider.equals(provider)) {
                noProviderThreads = false;
            }
        }
        if (noProviderThreads) {
            logger.error("Got a retrieval request for " + provider
                    + ", but no retrieval threads for " + provider + " exist!");
        } else {
            synchronized (notifier) {
                notifier.notifyAll();
            }
        }
    }

    /*
     * TODO: Add task to fail any retrieval that has WAITING_RESPONSE for more
     * than X minutes
     */
    /**
     * Scans database for retrievals waiting to process. Return true if an entry
     * was processed, false if nothing was processed and thread should sleep.
     *
     * @param provider
     */
    public boolean scanForRetrievals(String provider) {
        RetrievalRequestRecord rec = null;

        try {
            rec = retrievalDao.activateNextRetrievalRequest(provider,
                    providerSyncObjects.get(provider));
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
