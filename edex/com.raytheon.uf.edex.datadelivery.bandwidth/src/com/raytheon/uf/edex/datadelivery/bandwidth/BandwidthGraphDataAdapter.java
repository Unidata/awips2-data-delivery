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
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.annotations.VisibleForTesting;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthBucketDescription;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthGraphData;
import com.raytheon.uf.common.datadelivery.bandwidth.data.SubscriptionWindowData;
import com.raytheon.uf.common.datadelivery.bandwidth.data.TimeWindowData;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription.SubscriptionPriority;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthBucket;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrieval;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalPlan;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;

/**
 * Adapts the {@link BandwidthManager} formatted data into a GUI usable graphing
 * object format.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 06, 2012 1397       djohnson     Initial creation
 * Jan 25, 2013 1528       djohnson     Subscription priority is now an enum.
 * Jun 24, 2013 2106       djohnson     Access bucket allocations through RetrievalPlan.
 * Jul 11, 2013 2106       djohnson     Use priority straight from the BandwidthSubscription.
 * Sep 20, 2013 2397       bgonzale     Add Map of Bucket Descriptions to BandwidthGraphData.
 * Nov 27, 2013 2545       mpduff       Get data by network
 * Dec 11, 2013 2566       bgonzale     handle case when there are no reservations.
 * Dec 17, 2013 2636       bgonzale     Refactored bucket fill in edex.
 * Jan 23, 2014 2636       mpduff       Changed download window generation.
 * Feb 03, 2014 2745       mpduff       Don't display fulfilled or cancelled allocations.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

class BandwidthGraphDataAdapter {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(BandwidthGraphDataAdapter.class);

    private final RetrievalManager retrievalManager;

    /**
     * Constructor.
     * 
     * @param retrievalManager
     *            the bucket time in minutes
     */
    public BandwidthGraphDataAdapter(RetrievalManager retrievalManager) {
        this.retrievalManager = retrievalManager;
    }

    /**
     * Return the adapted graph data.
     * 
     * @return the data
     */
    public BandwidthGraphData get() {
        final BandwidthGraphData bandwidthGraphData = new BandwidthGraphData();

        Collection<RetrievalPlan> retrievalPlans = retrievalManager
                .getRetrievalPlans().values();
        Map<Network, List<SubscriptionWindowData>> networkMap = new HashMap<Network, List<SubscriptionWindowData>>();
        SubscriptionAllocationMapping subAllocationMapping = new SubscriptionAllocationMapping();

        // One retrieval plan per network
        for (RetrievalPlan retrievalPlan : retrievalPlans) {
            Network network = retrievalPlan.getNetwork();
            if (!networkMap.containsKey(network)) {
                networkMap
                        .put(network, new ArrayList<SubscriptionWindowData>());
            }

            // Get all buckets that are in the retrieval plan from the current
            // time forward
            final SortedSet<BandwidthBucket> bandwidthBuckets = retrievalPlan
                    .getBucketsInWindow(TimeUtil.currentTimeMillis(),
                            Long.MAX_VALUE);

            // % utilized graph data
            SortedSet<BandwidthBucketDescription> buckets = toDescriptions(bandwidthBuckets);
            bandwidthGraphData.addBucketDescriptions(network, buckets);

            // Latency window data
            List<BandwidthAllocation> allocationList = EdexBandwidthContextFactory
                    .getInstance().bandwidthDao
                    .getBandwidthAllocations(network);

            for (BandwidthAllocation allocation : allocationList) {
                if (allocation instanceof SubscriptionRetrieval) {
                    // Don't display fulfilled or cancelled allocations
                    if (allocation.getStatus() != RetrievalStatus.FULFILLED
                            && allocation.getStatus() != RetrievalStatus.CANCELLED) {
                        final SubscriptionRetrieval subRetrieval = (SubscriptionRetrieval) allocation;
                        String subName = subRetrieval
                                .getBandwidthSubscription().getName();
                        subAllocationMapping.addAllocationForSubscription(
                                subName, allocation);
                    }
                }
            }
        }

        Map<String, List<BandwidthAllocation>> subAllocationMap = subAllocationMapping
                .getSubAllocationMap();
        for (Map.Entry<String, List<BandwidthAllocation>> entry : subAllocationMap
                .entrySet()) {
            String sub = entry.getKey();
            for (BandwidthAllocation ba : entry.getValue()) {
                if (ba instanceof SubscriptionRetrieval) {
                    ((SubscriptionRetrieval) ba).getBandwidthSubscription()
                            .getBaseReferenceTime();
                    SubscriptionRetrieval sr = (SubscriptionRetrieval) ba;
                    BandwidthSubscription dao = sr.getBandwidthSubscription();
                    SubscriptionPriority priority = dao.getPriority();
                    Calendar baseRefTime = ((SubscriptionRetrieval) ba)
                            .getBandwidthSubscription().getBaseReferenceTime();
                    int offset = ((SubscriptionRetrieval) ba)
                            .getDataSetAvailablityDelay();
                    String registryId = sr.getBandwidthSubscription()
                            .getRegistryId();
                    Network network = sr.getNetwork();

                    SubscriptionWindowData windowData = null;
                    List<SubscriptionWindowData> subList = networkMap
                            .get(network);
                    for (SubscriptionWindowData subData : subList) {
                        if (subData.getRegistryId().equals(registryId)) {
                            windowData = subData;
                            break;
                        }
                    }

                    if (windowData == null) {
                        windowData = new SubscriptionWindowData();
                        windowData.setNetwork(network);
                        windowData.setPriority(priority);
                        windowData.setRegistryId(registryId);
                        windowData.setSubscriptionName(sub);
                        networkMap.get(network).add(windowData);
                    }

                    final long startMillis = sr.getStartTime()
                            .getTimeInMillis();
                    final long endMillis = sr.getEndTime().getTimeInMillis();
                    TimeWindowData window = new TimeWindowData(startMillis,
                            endMillis);
                    window.setBaseTime(baseRefTime.getTimeInMillis());
                    window.setOffset(offset);
                    windowData.addTimeWindow(window);
                }
            }
        }

        bandwidthGraphData.setNetworkDataMap(networkMap);

        return bandwidthGraphData;
    }

    /**
     * Return BandwithBucketDescription objects for the given BandwidthBuckets.
     */
    @VisibleForTesting
    SortedSet<BandwidthBucketDescription> toDescriptions(
            SortedSet<BandwidthBucket> bandwidthBuckets) {
        SortedSet<BandwidthBucketDescription> descriptions = new TreeSet<BandwidthBucketDescription>();
        long leftovers = 0;
        for (BandwidthBucket bucket : bandwidthBuckets) {
            BandwidthBucketDescription desc = new BandwidthBucketDescription(
                    bucket.getNetwork(), bucket.getBucketSize(),
                    bucket.getCurrentSize(), bucket.getBucketStartTime());
            descriptions.add(desc);
        }

        return descriptions;
    }
}
