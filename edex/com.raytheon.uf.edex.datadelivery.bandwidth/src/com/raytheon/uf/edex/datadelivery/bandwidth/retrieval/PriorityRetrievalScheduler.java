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
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.SizeUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthBucket;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;

/**
 * Retrieval Scheduler that evaluates Subscription priority values and fills the
 * RetrievalPlan accordingly.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 27, 2012  726      jspinks   Initial release.
 * Oct 17, 2012  726      djohnson  If unable to find a bucket with floorKey,
 *                                  use ceilingKey.
 * Oct 26, 2012  1286     djohnson  Return list of unscheduled allocations.
 * Jan 25, 2013  1528     djohnson  Lower priority requests should not be able
 *                                  to unschedule higher priority requests.
 * Jun 25, 2013  2106     djohnson  Access bandwidth bucket contents through
 *                                  RetrievalPlan.
 * Dec 17, 2013  2636     bgonzale  When adding to buckets, call the constrained
 *                                  method.
 * Feb 14, 2014  2636     mpduff    Clean up logging.
 * Apr 02, 2014  2810     dhladky   Priority sorting of allocations.
 * May 27, 2015  4531     dhladky   Remove excessive Calendar references.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * May 23, 2016  5639     tjensen   Fix reprioritization
 * Feb 02, 2018  6471     tjensen   Added UnscheduledAllocationReports. Made static
 *
 * </pre>
 *
 */
public class PriorityRetrievalScheduler {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PriorityRetrievalScheduler.class);

    /**
     * Attempt to schedule a BandwidthAllocation in the specified RetrievalPlan.
     *
     * @param plan
     *            The RetrievalPlan to attempt to schedule the
     *            BandwidthAllocation in.
     *
     * @param allocation
     *            The BandwidthAllocation to schedule.
     * @return the {@link BandwidthAllocation}s that are unable to be scheduled,
     *         this can be formerly scheduled allocations that were booted to
     *         make room for an allocation deemed more important
     */
    public static List<UnscheduledAllocationReport> schedule(RetrievalPlan plan,
            BandwidthAllocation allocation) {

        List<UnscheduledAllocationReport> unscheduled = new ArrayList<>();

        /*
         * First get the retrieval start time. Compare the buckets in order, to
         * see if there is room for the BandwidthAllocation If there is room,
         * simply add the allocation to the bucket and return. If there is not
         * room, then we need to compare the allocations that are not active to
         * the proposed allocation and see if we need to move already scheduled
         * allocations.
         */
        Date startTime = allocation.getStartTime();
        Date endTime = allocation.getEndTime();

        long startTimeMillis = startTime.getTime();
        long endTimeMillis = endTime.getTime();

        if (startTimeMillis > endTimeMillis) {
            throw new IllegalArgumentException(String.format(
                    "Invalid start and end times passed for allocation [%s]: "
                            + "start time [%s] is greater than end time [%s].",
                    allocation.getId(), BandwidthUtil.format(startTime),
                    BandwidthUtil.format(endTime)));
        }

        boolean notScheduled = true;

        // Get the buckets that are in the 'window' for the BandwidthAllocation.
        SortedSet<BandwidthBucket> window = plan
                .getBucketsInWindow(startTimeMillis, endTimeMillis);

        // Look through the buckets in the window for bandwidth.
        Iterator<BandwidthBucket> itr = window.iterator();

        long bandwidthRequired = allocation.getEstimatedSizeInBytes();
        boolean split = false;

        SortedMap<BandwidthBucket, Object> reservations = new TreeMap<>();

        while (notScheduled && itr.hasNext()) {

            BandwidthBucket bucket = itr.next();
            // How much is available?
            long available = bucket.getAvailableBytes();

            // The whole allocation can fit..
            if (available >= bandwidthRequired) {

                if (split) {
                    BandwidthReservation reserve = new BandwidthReservation(
                            allocation, bandwidthRequired);
                    /*
                     * Since we have had to split the allocation between
                     * buckets, assign the reservation to the current bucket.
                     */
                    reserve.setBandwidthBucket(bucket.getBucketStartTime());
                    reservations.put(bucket, reserve);
                } else {
                    /*
                     * If we haven't split the allocation over buckets, assign
                     * the bucket number to the allocation.
                     */
                    allocation.setBandwidthBucket(bucket.getBucketStartTime());
                    reservations.put(bucket, allocation);
                }
                /*
                 * All of the required bandwidth was found. Consider the
                 * BandwidthAllocation scheduled.
                 */
                notScheduled = false;

            } else if (available > 0) {
                /*
                 * There is some bandwidth to be used, so add the request and
                 * let the rest overflow into the next bucket...
                 */
                if (split) {
                    BandwidthReservation reserve = new BandwidthReservation(
                            allocation, available);
                    /*
                     * Since we have had to split the allocation between
                     * buckets, assign the reservation to the current bucket.
                     */
                    reserve.setBandwidthBucket(bucket.getBucketStartTime());
                    reservations.put(bucket, reserve);
                } else {
                    allocation.setBandwidthBucket(bucket.getBucketStartTime());
                    reservations.put(bucket, allocation);
                }
                split = true;

                /*
                 * Reduced the required amount by what is available in this
                 * bucket.
                 */
                bandwidthRequired -= available;
            }
        }

        // If still not scheduled,
        if (notScheduled) {
            // Time to look at re-prioritizing some retrievals...
            unscheduled.addAll(reprioritize(plan, allocation, startTimeMillis,
                    endTimeMillis));
        } else {
            // Commit the reservations
            for (BandwidthBucket key : reservations.keySet()) {
                Object o = reservations.get(key);

                if (o instanceof BandwidthAllocation) {
                    BandwidthAllocation obj = (BandwidthAllocation) o;
                    obj.setStatus(RetrievalStatus.SCHEDULED);
                    plan.addToBucketWithSizeConstraint(key, obj);
                } else {
                    plan.addToBucket(key, (BandwidthReservation) o);
                }
            }

            /*
             * Update the requestMap in the RetrievalPlan with all the bucket(s)
             * that the BandwidthAllocation and/or BandwidthAllocation has been
             * allocated to.
             */
            plan.updateRequestMapping(allocation.getId(),
                    reservations.keySet());
        }

        return unscheduled;
    }

    private static List<UnscheduledAllocationReport> reprioritize(
            RetrievalPlan plan, BandwidthAllocation request, Long startKey,
            Long endKey) {

        statusHandler
                .debug("Re-prioritizing necessary for BandwidthAllocation: "
                        + request);

        /*
         * Look in the window between start and end times to see if there are
         * lower priority retrievals that can be moved.
         */
        SortedSet<BandwidthBucket> window = plan.getBucketsInWindow(startKey,
                endKey);

        boolean enoughBandwidth = false;
        long total = 0;
        long requestSize = request.getEstimatedSizeInBytes();
        List<BandwidthAllocation> lowerPriorityRequests = new ArrayList<>();

        /*
         * Calculate how much bandwidth is already available before trying to
         * make room.
         */
        for (BandwidthBucket bucket : window) {
            total += bucket.getAvailableBytes();
        }
        if (total < requestSize) {
            for (BandwidthBucket bucket : window) {
                for (BandwidthAllocation o : plan
                        .getBandwidthAllocationsForBucket(bucket)) {
                    long estimatedSizeInBytes = o.getEstimatedSizeInBytes();
                    bucket.getAvailableBytes();

                    // Priority Enum has Highest Priority = lowest value
                    if (request.compareTo(o) == -1) {
                        total += estimatedSizeInBytes;
                        lowerPriorityRequests.add(o);
                    }
                    // See if we have found enough room
                    if (total >= requestSize) {
                        enoughBandwidth = true;
                        break;
                    }
                }
            }
        } else {
            /*
             * We have enough bandwidth available without having to reprioritize
             * anything. We shouldn't get here since reprioritize should only
             * get called if there isn't enough space available.
             */
            statusHandler.warn(
                    "Attempted reprioritize when enough bandwidth is already available");
            enoughBandwidth = true;
        }

        if (enoughBandwidth) {
            /*
             * Since we have found enough bandwidth, go back and remove the
             * identified BandwidthAllocations from the plan. Then attempt to
             * reinsert them at a later point...
             */
            for (BandwidthAllocation reservation : lowerPriorityRequests) {
                statusHandler.info("Removing request " + reservation
                        + " to make room for request " + request);
                plan.remove(reservation);
            }

            // This should insert the request in the window we just created...
            List<UnscheduledAllocationReport> unscheduled = schedule(plan,
                    request);

            /*
             * Now attempt to reschedule the removed requests (but not the
             * reservations), which may result in further rescheduling...
             */
            for (BandwidthAllocation reservation : lowerPriorityRequests) {
                unscheduled.addAll(schedule(plan, reservation));
            }
            return unscheduled;
        }
        // Not enough bandwidth available to schedule
        List<UnscheduledAllocationReport> unscheduled = new ArrayList<>(1);
        UnscheduledAllocationReport status = new UnscheduledAllocationReport(
                request, (total / SizeUtil.BYTES_PER_KB), startKey, endKey);
        unscheduled.add(status);

        return unscheduled;
    }
}
