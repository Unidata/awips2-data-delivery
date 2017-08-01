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
import com.raytheon.uf.common.datadelivery.registry.SubscriptionUtil;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrieval;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;

/**
 * This SubscriptionAggregator does no aggregation of subscriptions. It simply
 * creates one SubscriptionRetrieval per subscription, populated with the
 * necessary RetrievalRequests to fulfill the subscription using the existing
 * retrieval engine.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 18, 2012  726      jspinks   Initial creation
 * Nov 09, 2012  1286     djohnson  Rename interface to comply with standards.
 * Nov 20, 2012  1286     djohnson  Change some logging to debug.
 * Jun 13, 2013  2095     djohnson  No need to query the database, we are only
 *                                  receiving new bandwidth subscriptions.
 * Jul 11, 2013  2106     djohnson  aggregate() signature changed.
 * Jan 06, 2014  2636     mpduff    Changed how data set offset is set.
 * Jan 30, 2014  2686     dhladky   refactor of retrieval.
 * Apr 15, 2014  3012     dhladky   help with confusing nomenclature.
 * Jun 09, 2014  3113     mpduff    Use SubscriptionUtil rather than
 *                                  BandwidthUtil.
 * Aug 29, 2014  3446     bphillip  SubscriptionUtil is now a singleton
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Aug 09, 2016  5771     rjpeter   Get latency for subscription once
 * Aug 02, 2017  6186     rjpeter   Removed RetrievalAgent.
 *
 * </pre>
 *
 * @author jspinks
 */
public class SimpleSubscriptionAggregator {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SimpleSubscriptionAggregator.class);

    private final IBandwidthDao bandwidthDao;

    public SimpleSubscriptionAggregator(IBandwidthDao bandwidthDao) {
        this.bandwidthDao = bandwidthDao;
    }

    /**
     * Generate a List of SubscriptionRetrieval Object for the provided
     * BandwidthSubscription Objects.
     *
     * @param container
     *            A container with a List of BandwidthSubscription Objects which
     *            were just added, and their subscription
     *
     * @return The SubscriptionRetrieval Objects used to fulfill the
     *         BandwidthSubscription Objects provided.
     */
    public List<SubscriptionRetrieval> aggregate(
            BandwidthSubscriptionContainer container) {

        List<SubscriptionRetrieval> subscriptionRetrievals = new ArrayList<>();

        /*
         * No aggregation or decomposition of subscriptions, simply create the
         * necessary retrievals without regards to 'sharing' retrievals across
         * subscriptions.
         */
        Subscription sub = container.subscription;
        int latency = BandwidthUtil.getSubscriptionLatency(sub);

        for (BandwidthSubscription subDao : container.newSubscriptions) {

            /*
             * First check to see if the Object already was scheduled (i.e. has
             * SubscriptionRetrievals associated with it) if not, create a
             * SubscriptionRetrieval for the subscription
             */

            /*
             * Do NOT confuse this with an actual SubscriptionRetrieval. This
             * SubscriptionRetrieval object is a BandwidthAllocation object
             */
            SubscriptionRetrieval subscriptionRetrieval = new SubscriptionRetrieval();
            // Link this SubscriptionRetrieval with the subscription.
            subscriptionRetrieval.setBandwidthSubscription(subDao);
            subscriptionRetrieval.setNetwork(subDao.getRoute());
            subscriptionRetrieval.setStatus(RetrievalStatus.PROCESSING);
            subscriptionRetrieval.setPriority(subDao.getPriority());
            subscriptionRetrieval.setEstimatedSize(subDao.getEstimatedSize());

            // Create a Retrieval Object for the Subscription
            subscriptionRetrieval.setSubscriptionLatency(latency);

            int offset = 0;
            try {
                offset = SubscriptionUtil.getInstance()
                        .getDataSetAvailablityOffset(sub,
                                subDao.getBaseReferenceTime());
            } catch (RegistryHandlerException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to retrieve data availability offset, using 0 for the offset.",
                        e);
            }

            subscriptionRetrieval.setDataSetAvailablityDelay(offset);
            subscriptionRetrievals.add(subscriptionRetrieval);

            if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                statusHandler.debug("Created [" + subscriptionRetrievals.size()
                        + "] SubscriptionRetrieval Objects for BandwidthSubscription ["
                        + subDao.getIdentifier() + "]");
            }
        }

        return subscriptionRetrievals;
    }

    /**
     * This method is called once all the SubscriptionRetrievals for a
     * subscription are fulfilled and allows the ISubscriptionAggregator to
     * "assemble" the finished subscription(s). This method will be called once
     * for each Subscription who's SubscriptionRetrieval Objects are all in the
     * "FULFILLED" state.
     *
     * @param retrievals
     *            A List of SubscriptionRetrieval(s) that make were produced
     *            from calling aggregate for a particular subscription.
     *
     * @return A List of completed subscriptions, ready for notification to the
     *         user.
     */
    public List<BandwidthSubscription> completeRetrieval(
            List<SubscriptionRetrieval> retrievals) {

        List<BandwidthSubscription> daos = new ArrayList<>();
        /*
         * We know that only one SubscriptionRetrieval was created for each
         * Subscription so there will not be any duplication of subscription
         * ids.
         */
        for (SubscriptionRetrieval retrieval : retrievals) {
            daos.add(bandwidthDao.getBandwidthSubscription(
                    retrieval.getBandwidthSubscription().getId()));
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