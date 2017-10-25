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
import java.util.Date;
import java.util.List;
import java.util.SortedSet;

import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionUtil;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
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
 * Oct 25, 2017  6484     tjensen   Merged SubscriptionRetrievals and
 *                                  BandwidthAllocations. Remove
 *                                  BandwidthSubscriptionContainer.
 *
 * </pre>
 *
 * @author jspinks
 */
public class SimpleSubscriptionAggregator {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SimpleSubscriptionAggregator.class);

    public SimpleSubscriptionAggregator() {
    }

    /**
     * Generate a List of Bandwidth Allocation Object for the provided
     * subscription and times.
     * 
     * @param sub
     *            The subscription to get allocations for
     * @param baseReferenceTimes
     *            The times of the allocations
     * @return A list of all bandwidthAllocations for the subscription at the
     *         provided times
     */
    public List<BandwidthAllocation> aggregate(Subscription sub,
            SortedSet<Date> baseReferenceTimes) {

        List<BandwidthAllocation> subscriptionRetrievals = new ArrayList<>();

        /*
         * No aggregation or decomposition of subscriptions, simply create the
         * necessary retrievals without regards to 'sharing' retrievals across
         * subscriptions.
         */
        int latency = BandwidthUtil.getSubscriptionLatency(sub);

        for (Date baseRefTime : baseReferenceTimes) {

            /*
             * Create a BandwidthAllocation for the subscription each reference
             * time.
             */
            BandwidthAllocation bandwidthAllocation = new BandwidthAllocation();
            bandwidthAllocation.setNetwork(sub.getRoute());
            bandwidthAllocation.setStatus(RetrievalStatus.PROCESSING);
            bandwidthAllocation.setPriority(sub.getPriority());
            bandwidthAllocation.setEstimatedSize(sub.getDataSetSize());
            bandwidthAllocation.setSubName(sub.getName());
            bandwidthAllocation.setSubscriptionId(sub.getId());
            bandwidthAllocation.setBaseReferenceTime(baseRefTime);
            bandwidthAllocation.setSubscriptionLatency(latency);

            int offset = 0;
            try {
                offset = SubscriptionUtil.getInstance()
                        .getDataSetAvailablityOffset(sub, baseRefTime);
            } catch (RegistryHandlerException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to retrieve data availability offset, using 0 for the offset.",
                        e);
            }

            bandwidthAllocation.setDataSetAvailablityDelay(offset);
            subscriptionRetrievals.add(bandwidthAllocation);
        }
        if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
            statusHandler.debug("Created [" + subscriptionRetrievals.size()
                    + "] BandwidthAllocations Objects for Subscription ["
                    + sub.getName() + "]");
        }

        return subscriptionRetrievals;
    }
}