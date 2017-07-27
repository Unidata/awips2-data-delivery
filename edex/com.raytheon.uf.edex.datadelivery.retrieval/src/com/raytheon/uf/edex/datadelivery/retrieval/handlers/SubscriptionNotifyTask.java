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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.google.common.annotations.VisibleForTesting;
import com.raytheon.uf.common.datadelivery.event.retrieval.SubscriptionRetrievalEvent;
import com.raytheon.uf.common.datadelivery.event.retrieval.SubscriptionRetrievalEvent.Status;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval.SubscriptionType;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalManagerNotifyEvent;
import com.raytheon.uf.edex.datadelivery.retrieval.db.IRetrievalDao;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;

/**
 *
 * Handles subscription notification.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 09, 2012  1022     djohnson  No longer extends Thread, simplify {@link
 *                                  SubscriptionDelay}.
 * Oct 10, 2012  726      djohnson  Use the subRetrievalKey for notifying the
 *                                  retrieval manager.
 * Nov 25, 2012  1268     dhladky   Added additional fields to process
 *                                  subscription tracking
 * Feb 05, 2013  1580     mpduff    EventBus refactor.
 * Mar 05, 2013  1647     djohnson  Debug log running message.
 * Jan 08, 2013  2645     bgonzale  Catch all exceptions in run to prevent the
 *                                  recurring timer from failing.
 * Jul 22, 2014  2732     ccody     Add Date Time to SubscriptionRetrievalEvent
 *                                  message
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * May 17, 2016  5662     tjensen   Cleanup duplicate parameters in failed
 *                                  message
 * May 09, 2017  6186     rjpeter   Added url
 * May 22, 2017  6130     tjensen   Fix error handling
 * Jul 27, 2017  6186     rjpeter   Removed unused fields.
 *
 * </pre>
 *
 * @author djohnson
 */
public class SubscriptionNotifyTask implements Runnable {
    static class SubscriptionDelay implements Delayed {
        private final String subName;

        private final String dsmdUrl;

        private final String owner;

        private final String provider;

        private final SubscriptionType subscriptionType;

        private final Network network;

        private final Long bandwidthAllocationId;

        private final long delayedUntilMillis;

        private final String key;

        private final Long retrievalRequestTime;

        SubscriptionDelay(String subName, String url, String owner,
                SubscriptionType subscriptionType, Network network,
                String provider, Long bandwidthAllocationId,
                long delayedUntilMillis, Long retrievalRequestTime) {
            this.subName = subName;
            this.dsmdUrl = url;
            this.owner = owner;
            this.subscriptionType = subscriptionType;
            this.network = network;
            this.provider = provider;
            this.bandwidthAllocationId = bandwidthAllocationId;
            this.delayedUntilMillis = delayedUntilMillis;
            this.retrievalRequestTime = retrievalRequestTime;
            key = subName + "_" + dsmdUrl;
        }

        @Override
        public int compareTo(Delayed o) {
            int rval = 0;
            if (o instanceof SubscriptionDelay) {
                SubscriptionDelay oSub = (SubscriptionDelay) o;

                rval = Long.valueOf(delayedUntilMillis)
                        .compareTo(Long.valueOf(oSub.delayedUntilMillis));

                if (rval == 0) {
                    rval = subName.compareTo(oSub.subName);
                }

                if (rval == 0) {
                    rval = owner.compareTo(oSub.owner);
                }
            }

            return rval;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long currentTime = System.currentTimeMillis();
            long remainingDelayMillis = getRemainingDelay(currentTime);

            return unit.convert(remainingDelayMillis, TimeUnit.MILLISECONDS);
        }

        public String getKey() {
            return key;
        }

        /**
         * Returns how many milliseconds are remaining until the delayed time
         * has passed.
         *
         * @param currentTimeMillis
         *            the millis to see how much delay is remaining
         *
         * @return how many more milliseconds of delay remaining, or 0 if the
         *         delay has expired
         */
        @VisibleForTesting
        long getRemainingDelay(long currentTimeMillis) {
            return Math.max(delayedUntilMillis - currentTimeMillis, 0);
        }

        /**
         * Returns The date time (long) of the Data Retrieval Request. has
         * passed.
         *
         * @return retrievalRequestTime Time that Retrieval Request was
         *         generated
         */
        @VisibleForTesting
        Long getRetrievalRequestTime() {
            return (retrievalRequestTime);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SubscriptionDelay other = (SubscriptionDelay) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return getKey();
        }
    }

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SubscriptionNotifyTask.class);

    /**
     * Creates a SubscriptionDelay delayed for 11 seconds.
     *
     * @param record
     *            the record to base from
     * @param startTime
     *            the start time to which 11 seconds should be added
     * @return
     */
    @VisibleForTesting
    static SubscriptionDelay createSubscriptionDelay(
            RetrievalRequestRecord record, long startTime) {

        Long retrievalRequestTimeLong = null;
        Network network = null;
        SubscriptionType subType = null;

        try {
            Retrieval retrievalObject = record.getRetrievalObj();
            if (retrievalObject != null) {
                retrievalRequestTimeLong = retrievalObject
                        .getRequestRetrievalTime();
                network = retrievalObject.getNetwork();
                subType = retrievalObject.getSubscriptionType();
            }
        } catch (SerializationException se) {
            statusHandler.error(
                    "Error occurred unmarshalling retrieval object for determining Subscriptiong Request time.",
                    se);
        }

        if (retrievalRequestTimeLong == null) {
            retrievalRequestTimeLong = Long.valueOf(0L);
        }
        // 11 seconds from start time
        return new SubscriptionDelay(record.getSubscriptionName(),
                record.getDsmdUrl(), record.getOwner(), subType, network,
                record.getProvider(), record.getBandwidthAllocationId(),
                startTime + 11_000, retrievalRequestTimeLong);
    }

    // set written to by other threads
    private ConcurrentMap<String, SubscriptionDelay> waitingSubscriptions = new ConcurrentHashMap<>();

    // set used for draining all entries, while other queue being written to
    private ConcurrentMap<String, SubscriptionDelay> subscriptionsInProcess = new ConcurrentHashMap<>(
            64);

    private final DelayQueue<SubscriptionDelay> subscriptionQueue = new DelayQueue<>();

    private IRetrievalDao dao;

    public SubscriptionNotifyTask(IRetrievalDao dao) {
        this.dao = dao;
        this.dao.setNotifyTask(this);
    }

    public void checkNotify(RetrievalRequestRecord record) {
        SubscriptionDelay subDelay = createSubscriptionDelay(record,
                System.currentTimeMillis());
        waitingSubscriptions.put(subDelay.getKey(), subDelay);
    }

    @Override
    public void run() {
        statusHandler.debug("SubscriptionNotifyTask() - Running...");
        try {
            SubscriptionDelay nextSub = subscriptionQueue.peek();

            if (nextSub != null) {
                try {
                    // wait an extra second for a few more to accumulate
                    long timeToWait = nextSub.getDelay(TimeUnit.MILLISECONDS)
                            + 1000;
                    Thread.sleep(timeToWait);
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            // switch set references
            if (!waitingSubscriptions.isEmpty()) {
                ConcurrentMap<String, SubscriptionDelay> tmp = subscriptionsInProcess;
                subscriptionsInProcess = waitingSubscriptions;
                waitingSubscriptions = tmp;
            }

            // don't check if complete until haven't received a new
            // retrieval for 10 seconds
            if (!subscriptionsInProcess.isEmpty()) {
                subscriptionQueue.addAll(subscriptionsInProcess.values());
                subscriptionsInProcess.clear();
            }

            SubscriptionDelay subToCheck = subscriptionQueue.poll();
            while (subToCheck != null) {
                Map<RetrievalRequestRecord.State, Integer> stateCounts = dao
                        .getSubscriptionStateCounts(subToCheck.subName,
                                subToCheck.dsmdUrl);
                Integer numPending = stateCounts
                        .get(RetrievalRequestRecord.State.PENDING);
                Integer numRunning = stateCounts
                        .get(RetrievalRequestRecord.State.RUNNING);
                if ((numPending == null || numPending.intValue() == 0)
                        && (numRunning == null || numRunning.intValue() == 0)) {
                    SubscriptionRetrievalEvent event = new SubscriptionRetrievalEvent();
                    event.setId(subToCheck.subName);
                    event.setOwner(subToCheck.owner);
                    event.setProvider(subToCheck.provider);
                    event.setSubscriptionType(
                            subToCheck.subscriptionType.name());
                    event.setNetwork(subToCheck.network.name());
                    event.setRetrievalRequestTime(
                            subToCheck.retrievalRequestTime);

                    // event needs new key
                    RetrievalManagerNotifyEvent retrievalManagerNotifyEvent = new RetrievalManagerNotifyEvent();
                    retrievalManagerNotifyEvent.setId(
                            Long.toString(subToCheck.bandwidthAllocationId));

                    // check if any of the retrievals failed
                    Integer numFailed = stateCounts
                            .get(RetrievalRequestRecord.State.FAILED);
                    Integer numComplete = stateCounts
                            .get(RetrievalRequestRecord.State.COMPLETED);
                    if (numFailed == null || numFailed.intValue() == 0) {
                        event.setStatus(Status.SUCCESS);
                    } else {
                        if (numComplete != null && numComplete.intValue() > 0) {
                            event.setStatus(Status.PARTIAL_SUCCESS);
                        } else {
                            event.setStatus(Status.FAILED);
                        }

                        // generate message
                        List<RetrievalRequestRecord> failedRecs = dao
                                .getFailedRequests(subToCheck.subName,
                                        subToCheck.dsmdUrl);
                        StringBuilder sb = new StringBuilder(300);
                        try {
                            sb.append("Failed parameters: ");
                            List<String> parameters = new ArrayList<>();
                            for (RetrievalRequestRecord failedRec : failedRecs) {
                                Retrieval retrieval = failedRec
                                        .getRetrievalObj();
                                RetrievalAttribute<?, ?> att = retrieval
                                        .getAttribute();
                                if (!parameters.contains(
                                        att.getParameter().getName())) {
                                    parameters
                                            .add(att.getParameter().getName());
                                }
                            }
                            for (String param : parameters) {
                                sb.append(param + ", ");
                            }
                            sb.delete(sb.length() - 2, sb.length());
                        } catch (SerializationException e) {
                            sb.append(
                                    "Failed parameters: Unable to determine the parameters that failed due to serialization errors.");
                            statusHandler.error(
                                    "Error occurred unmarshalling retrieval object for determining failed parameters.",
                                    e);
                        }
                        event.setFailureMessage(sb.toString());

                    }
                    if (numComplete == null) {
                        event.setNumComplete(0);
                    } else {
                        event.setNumComplete(numComplete);
                    }
                    if (numFailed == null) {
                        event.setNumFailed(0);
                    } else {
                        event.setNumFailed(numFailed);
                    }

                    EventBus.publish(event);
                    EventBus.publish(retrievalManagerNotifyEvent);
                    dao.removeSubscription(subToCheck.subName,
                            subToCheck.dsmdUrl);
                }

                subToCheck = subscriptionQueue.poll();
            }
        } catch (DataAccessLayerException e) {
            statusHandler.handle(Priority.ERROR,
                    "Unable to contact the database", e);
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Unexpected error during Subscription Notify Task processing...",
                    e);
        }
    }
}
