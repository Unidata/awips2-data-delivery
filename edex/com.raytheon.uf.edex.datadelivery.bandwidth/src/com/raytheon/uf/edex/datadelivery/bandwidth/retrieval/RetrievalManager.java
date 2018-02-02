package com.raytheon.uf.edex.datadelivery.bandwidth.retrieval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;

/**
 *
 * Retrieval manager.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 11, 2012  726      djohnson  Add SW history, check for bandwidth enabled,
 *                                  change the event listener type.
 * Oct 26, 2012  1286     djohnson  Return list of unscheduled allocations.
 * Feb 05, 2013  1580     mpduff    EventBus refactor.
 * Feb 14, 2013  1596     djohnson  Warn log when unable to find a
 *                                  SubscriptionRetrieval.
 * Mar 18, 2013  1802     bphillip  Event bus registration is now a
 *                                  post-construct operation to ensure proxy is
 *                                  registered with bus
 * Mar 13, 2013  1802     bphillip  Moved event bus registration from
 *                                  post-construct to spring static method call
 * Jun 13, 2013  2095     djohnson  Can schedule any subclass of
 *                                  BandwidthAllocation.
 * Jun 25, 2013  2106     djohnson  Copy state from another instance, add
 *                                  ability to check for proposed bandwidth
 *                                  throughput changes.
 * Jul 09, 2013  2106     djohnson  Only needs to unregister from the EventBus
 *                                  when used in an EDEX instance, so handled in
 *                                  EdexBandwidthManager.
 * Oct 03, 2013  2267     bgonzale  Added check for no retrieval plan matching
 *                                  in the proposed retrieval plans.
 * Jan 30, 2014  2686     dhladky   refactor of retrieval.
 * Feb 10, 2014  2678     dhladky   Prevent duplicate allocations.
 * Apr 02, 2014  2810     dhladky   Priority sorting of allocations.
 * Sep 14, 0201  2131     dhladky   PDA additions
 * Jan 15, 2014  3884     dhladky   Removed shutdown, replaced with restart(),
 *                                  shutdown undermined #2749 BWM ticket;
 * Mar 08, 2015  3950     dhladky   Better logging of foreign retrieval ID's.
 * May 27, 2015  4531     dhladky   Remove excessive Calendar references.
 * Aug 09, 2016  5771     rjpeter   Allow concurrent event processing
 * Aug 02, 2017  6186     rjpeter   Removed RetrievalManagerNotifyEvent.
 * Nov 15, 2017  6498     tjensen   Improved logging on deferred allocations
 * Feb 02, 2018  6471     tjensen   Added UnscheduledAllocationReports
 *
 * </pre>
 *
 * @author djohnson
 */
public class RetrievalManager {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(RetrievalManager.class);

    private final IBandwidthDao bandwidthDao;

    // A Map of the Paths to retrievalPlans
    private Map<Network, RetrievalPlan> retrievalPlans = Collections
            .synchronizedSortedMap(new TreeMap<Network, RetrievalPlan>());

    public RetrievalManager(IBandwidthDao bandwidthDao) {
        this.bandwidthDao = bandwidthDao;
    }

    public Map<Network, RetrievalPlan> getRetrievalPlans() {
        return retrievalPlans;
    }

    public void setRetrievalPlans(Map<Network, RetrievalPlan> retrievalPlans) {
        this.retrievalPlans = retrievalPlans;
    }

    /**
     * Schedule the allocations.
     *
     * @param bandwidthAllocations
     *            The BandwidthAllocations to schedule.
     * @return the list of unscheduled allocation reports
     */
    public List<UnscheduledAllocationReport> schedule(
            List<BandwidthAllocation> inallocations) {
        List<UnscheduledAllocationReport> unscheduled = new ArrayList<>();
        // Arrange allocations in priority order
        List<BandwidthAllocation> bandwidthAllocations = new ArrayList<>(
                inallocations.size());
        bandwidthAllocations.addAll(inallocations);
        Collections.sort(bandwidthAllocations);

        for (BandwidthAllocation bandwidthAllocation : bandwidthAllocations) {
            Network network = bandwidthAllocation.getNetwork();
            RetrievalPlan plan = getRetrievalPlans().get(network);

            if (plan != null) {
                if (bandwidthAllocation.getStartTime()
                        .before(plan.getPlanStart().getTime())
                        || bandwidthAllocation.getEndTime()
                                .after(plan.getPlanEnd().getTime())) {

                    statusHandler
                            .warn("Attempt to schedule bandwidth outside current window. "
                                    + "Allocation Time: ("
                                    + bandwidthAllocation.getStartTime()
                                    + " to " + bandwidthAllocation.getEndTime()
                                    + ") Plan Window: ("
                                    + plan.getPlanStart().getTime() + " to "
                                    + plan.getPlanEnd().getTime()
                                    + "). BandwidthAllocation ["
                                    + bandwidthAllocation.getIdentifier()
                                    + "] will be deferred.");
                    bandwidthAllocation.setStatus(RetrievalStatus.DEFERRED);
                    bandwidthDao.createOrUpdate(bandwidthAllocation);
                } else {
                    synchronized (plan) {
                        unscheduled.addAll(plan.schedule(bandwidthAllocation));
                        bandwidthDao.createOrUpdate(bandwidthAllocation);
                    }
                }
            } else {
                throw new IllegalArgumentException(String.format(
                        "There is no configuration for network [%s]", network));
            }
        }

        // Update any unscheduled allocations
        for (UnscheduledAllocationReport uas : unscheduled) {
            BandwidthAllocation allocation = uas.getUnscheduled();
            allocation.setStatus(RetrievalStatus.UNSCHEDULED);
            bandwidthDao.createOrUpdate(allocation);
        }

        return unscheduled;
    }

    public BandwidthAllocation nextAllocation(Network network,
            String agentType) {

        RetrievalPlan plan = getRetrievalPlans().get(network);
        if (plan != null) {
            synchronized (plan) {
                return plan.nextAllocation(agentType);
            }
        }
        return null;
    }

    /***
     * Method used in practice because we need to search for expired
     * allocations.
     *
     * @param network
     * @return
     */
    public List<BandwidthAllocation> getRecentAllocations(Network network) {

        List<BandwidthAllocation> allocations = null;

        RetrievalPlan plan = getRetrievalPlans().get(network);
        if (plan != null) {
            synchronized (plan) {
                return plan.getRecentAllocations();
            }
        }

        return allocations;
    }

    public final RetrievalPlan getPlan(Network network) {
        return getRetrievalPlans().get(network);
    }

    public void remove(BandwidthAllocation allocation) {
        RetrievalPlan plan = getRetrievalPlans().get(allocation.getNetwork());
        if (plan != null) {
            synchronized (plan) {
                plan.remove(allocation);
            }
        }
    }

    public void updateBandwidthAllocation(BandwidthAllocation allocation) {
        RetrievalPlan plan = getRetrievalPlans().get(allocation.getNetwork());
        if (plan != null) {
            synchronized (plan) {
                plan.updateBandwidthReservation(allocation);
            }
        }
    }

    /**
     * Restart the Retrieval
     */
    public void restart() {
        initRetrievalPlans();
    }

    /**
     * @param fromRetrievalManager
     */
    public void copyState(RetrievalManager fromRetrievalManager) {
        for (Entry<Network, RetrievalPlan> entry : fromRetrievalManager.retrievalPlans
                .entrySet()) {
            final Network network = entry.getKey();
            final RetrievalPlan fromPlan = entry.getValue();
            final RetrievalPlan toPlan = this.retrievalPlans.get(network);

            toPlan.copyState(fromPlan);
        }

    }

    /**
     * Check whether a change in the bandwidth throughput is being proposed.
     *
     * @param proposedRetrievalManager
     *            the other retrieval manager with any proposed changes
     * @return true if a bandwidth throughput change is being proposed
     */
    public boolean isProposingBandwidthChanges(
            RetrievalManager proposedRetrievalManager) {
        boolean proposingBandwidthChanges = false;

        // If any retrieval plans have a different value for bandwidth, then
        // return true
        for (Entry<Network, RetrievalPlan> entry : this.retrievalPlans
                .entrySet()) {
            final RetrievalPlan proposedRetrievalPlan = proposedRetrievalManager.retrievalPlans
                    .get(entry.getKey());
            if ((proposedRetrievalPlan != null) && (entry.getValue() != null)) {
                if (proposedRetrievalPlan.getDefaultBandwidth() != entry
                        .getValue().getDefaultBandwidth()) {
                    proposingBandwidthChanges = true;
                    break;
                }
            } else {
                StringBuilder sb = new StringBuilder(
                        "The ProposedRetrievalPlan, ");
                sb.append(proposedRetrievalPlan);
                sb.append(", or the Existing RetrievalPlan, ");
                sb.append(entry.getKey());
                sb.append(" : ");
                sb.append(entry.getValue());
                sb.append(", is null.  Skipping this check.");
                statusHandler.info(sb.toString());
            }
        }

        return proposingBandwidthChanges;
    }

    /**
     * Initializes the retrieval plans.
     */
    public void initRetrievalPlans() {
        for (RetrievalPlan retrievalPlan : this.getRetrievalPlans().values()) {
            synchronized (retrievalPlan) {
                retrievalPlan.init();
            }
        }
        statusHandler.info("Initialized Retrieval Manager...");
    }
}