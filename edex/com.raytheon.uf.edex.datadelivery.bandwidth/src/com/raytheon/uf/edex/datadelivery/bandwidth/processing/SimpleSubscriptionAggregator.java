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
package com.raytheon.uf.edex.datadelivery.bandwidth.processing;

import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrieval;
import com.raytheon.uf.edex.datadelivery.bandwidth.interfaces.ISubscriptionAggregator;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalAgent;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;

/**
 * This implementation of ISubscriptionAggregator does no aggregation of
 * subscriptions. It simply creates one SubscriptionRetrieval per subscription,
 * populated with the necessary RetrievalRequests to fulfill the subscription
 * using the existing retrieval engine.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 18, 2012 726        jspinks     Initial creation
 * Nov 09, 2012 1286       djohnson    Rename interface to comply with standards.
 * Nov 20, 2012 1286       djohnson    Change some logging to debug.
 * Jun 13, 2013 2095       djohnson    No need to query the database, we are only receiving new bandwidth subscriptions.
 * Jul 11, 2013 2106       djohnson    aggregate() signature changed.
 * Jan 06, 2014 2636       mpduff      Changed how data set offset is set.
 * Jan 30, 2014   2686     dhladky      refactor of retrieval.
 * Apr 15, 2014 3012       dhladky     help with confusing nomenclature.
 * </pre>
 * 
 * @author jspinks
 * @version 1.0
 */
public class SimpleSubscriptionAggregator implements ISubscriptionAggregator {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SimpleSubscriptionAggregator.class);

    private final IBandwidthDao bandwidthDao;

    public SimpleSubscriptionAggregator(IBandwidthDao bandwidthDao) {
        this.bandwidthDao = bandwidthDao;
    }

    @Override
    public List<SubscriptionRetrieval> aggregate(
            BandwidthSubscriptionContainer container) {

        List<SubscriptionRetrieval> subscriptionRetrievals = new ArrayList<SubscriptionRetrieval>();

        // No aggregation or decomposition of subscriptions, simply create the
        // necessary retrievals without regards to 'sharing' retrievals across
        // subscriptions.

        for (BandwidthSubscription subDao : container.newSubscriptions) {

            // First check to see if the Object already was scheduled
            // (i.e. has SubscriptionRetrievals associated with it) if
            // not, create a SubscriptionRetrieval for the subscription

            // Do NOT confuse this with an actual SubscriptionRetrieval.
            // This SubscriptionRetrieval object is a BandwidthAllocation object
            SubscriptionRetrieval subscriptionRetrieval = new SubscriptionRetrieval();
            // Link this SubscriptionRetrieval with the subscription.
            subscriptionRetrieval.setBandwidthSubscription(subDao);
            subscriptionRetrieval.setNetwork(subDao.getRoute());
            subscriptionRetrieval
                    .setAgentType(RetrievalAgent.SUBSCRIPTION_AGENT);
            subscriptionRetrieval.setStatus(RetrievalStatus.PROCESSING);
            subscriptionRetrieval.setPriority(subDao.getPriority());
            subscriptionRetrieval.setEstimatedSize(subDao.getEstimatedSize());

            // Create a Retrieval Object for the Subscription
            Subscription sub = container.subscription;

            subscriptionRetrieval.setSubscriptionLatency(BandwidthUtil
                    .getSubscriptionLatency(sub));

            int offset = 0;
            try {
                offset = BandwidthUtil.getDataSetAvailablityOffset(sub,
                        subDao.getBaseReferenceTime());
            } catch (RegistryHandlerException e) {
                statusHandler
                        .handle(Priority.PROBLEM,
                                "Unable to retrieve data availability offset, using 0 for the offset.",
                                e);
            }
            subscriptionRetrieval.setDataSetAvailablityDelay(offset);

            subscriptionRetrievals.add(subscriptionRetrieval);

            if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                statusHandler
                        .debug("Created ["
                                + subscriptionRetrievals.size()
                                + "] SubscriptionRetrieval Objects for BandwidthSubscription ["
                                + subDao.getIdentifier() + "]");
            }
        }

        return subscriptionRetrievals;
    }

    @Override
    public List<BandwidthSubscription> completeRetrieval(
            List<SubscriptionRetrieval> retrievals) {

        List<BandwidthSubscription> daos = new ArrayList<BandwidthSubscription>();
        // We know that only one SubscriptionRetrieval was created for each
        // Subscription so there will not be any duplication of subscription
        // ids.
        for (SubscriptionRetrieval retrieval : retrievals) {
            daos.add(bandwidthDao.getBandwidthSubscription(retrieval
                    .getBandwidthSubscription().getId()));
        }

        StringBuilder sb = new StringBuilder();
        for (BandwidthSubscription dao : daos) {
            sb.append("Fulfilled subscription [").append(dao.getIdentifier());
            sb.append("][").append(dao.getRegistryId()).append("]\n");
        }
        if (sb.length() > 0) {
            statusHandler.info(sb.toString());
        }

        return daos;
    }
}
