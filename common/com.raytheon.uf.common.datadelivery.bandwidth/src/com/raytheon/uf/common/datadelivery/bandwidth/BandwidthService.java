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
package com.raytheon.uf.common.datadelivery.bandwidth;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.raytheon.uf.common.auth.req.BasePrivilegedServerService;
import com.raytheon.uf.common.datadelivery.bandwidth.BandwidthRequest.RequestType;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthGraphData;
import com.raytheon.uf.common.datadelivery.bandwidth.data.SubscriptionStatusSummary;
import com.raytheon.uf.common.datadelivery.bandwidth.util.LogUtil;
import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Service for interacting with the bandwidth manager.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 22, 2012 1286       djohnson     Initial creation
 * Nov 15, 2012 1286       djohnson     No longer abstract.
 * Nov 20, 2012 1286       djohnson     Add proposeSchedule methods.
 * Dec 06, 2012 1397       djohnson     Add ability to get bandwidth graph data.
 * Feb 27, 2013 1644       djohnson     Now abstract, sub-classes provide specific service lookup keys.
 * Jul 18, 2013 1653       mpduff       Add getSubscriptionStatusSummary method.
 * Oct 2,  2013 1797       dhladky      Generics
 * Oct 01, 2013 2267       bgonzale     Log error response from proposed scheduling.
 * Dec 11, 2013 2625       mpduff       Fix error handling to not return null.
 * Apr 22, 2014 2992       dhladky      renamed BandwidthRequest
 * Nov 20, 2014 2749       ccody        Added "propose only" for  Set Avail Bandwidth
 * Jun 09, 2015 4047       dhladky      cleanup.
 * Mar 16, 2016 3919       tjensen      Cleanup unneeded interfaces
 * Jun 20, 2017 6299       tgurney      Remove IProposeScheduleResponse
 *
 * </pre>
 *
 * @author djohnson
 * @version 1.0
 */
public abstract class BandwidthService<T extends Time, C extends Coverage>
        extends BasePrivilegedServerService<BandwidthRequest<T, C>> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(BandwidthService.class);

    /**
     * Constructor.
     *
     * @param serviceKey
     */
    protected BandwidthService(String serviceKey) {
        super(serviceKey);
    }

    /**
     * Retrieve the available bandwidth for a {@link Network}.
     *
     * @param network
     *            the network
     * @return the bandwidth, in kilobytes (KB)
     */
    public final int getBandwidthForNetworkInKilobytes(Network network) {
        BandwidthRequest<T, C> request = new BandwidthRequest<>();
        request.setRequestType(RequestType.GET_BANDWIDTH);
        request.setNetwork(network);

        try {
            return sendRequest(request, Integer.class).intValue();
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to set available bandwidth for network [" + network
                            + "]",
                    e);
            return 0;
        }
    }

    /**
     * Proposes changing the available bandwidth for a {@link Network}.
     *
     * @param network
     *            the network
     * @param bandwidth
     *            the bandwidth
     * @return the set of current subscription names which would be unable to
     *         fit into the retrieval plan with the new bandwidth amount
     */
    @SuppressWarnings("unchecked")
    public Set<String> proposeBandwidthForNetworkInKilobytes(Network network,
            int bandwidth) {
        BandwidthRequest<T, C> request = new BandwidthRequest<>();
        request.setRequestType(RequestType.PROPOSE_SET_BANDWIDTH);
        request.setNetwork(network);
        request.setBandwidth(bandwidth);

        try {
            return sendRequest(request, Set.class);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to set available bandwidth for network [" + network
                            + "]",
                    e);
            return null;
        }
    }

    /**
     * Propose ONLY making changes. Do NOT make any scheduling changes for the
     * available bandwidth for a {@link Network}.
     *
     * @param network
     *            the network
     * @param bandwidth
     *            the bandwidth
     * @return the set of current subscription names which would be unable to
     *         fit into the retrieval plan with the new bandwidth amount
     */
    @SuppressWarnings("unchecked")
    public Set<String> proposeOnlyBandwidthForNetworkInKilobytes(
            Network network, int bandwidth) {
        BandwidthRequest<T, C> request = new BandwidthRequest<>();
        request.setRequestType(RequestType.PROPOSE_ONLY_SET_BANDWIDTH);
        request.setNetwork(network);
        request.setBandwidth(bandwidth);

        try {
            return sendRequest(request, Set.class);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to Propose to set available bandwidth for network ["
                            + network + "]",
                    e);
            return null;
        }
    }

    /**
     * Set the available bandwidth for a {@link Network}.
     *
     * @param network
     *            the network
     * @param bandwidth
     *            the bandwidth
     * @return true if successfully changed
     */
    public final boolean setBandwidthForNetworkInKilobytes(Network network,
            int bandwidth) {
        BandwidthRequest<T, C> request = new BandwidthRequest<>();
        request.setRequestType(RequestType.FORCE_SET_BANDWIDTH);
        request.setNetwork(network);
        request.setBandwidth(bandwidth);

        try {
            return sendRequest(request, Boolean.class).booleanValue();
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to set available bandwidth for network [" + network
                            + "]",
                    e);
            return false;
        }
    }

    /**
     * Schedules a subscription for bandwidth management.
     *
     * @param subscriptions
     *            the subscription
     * @return the set of subscription names that have had some cycles
     *         unscheduled
     */
    public Set<String> schedule(Subscription<T, C> subscription) {
        return schedule(Arrays.asList(subscription));
    }

    /**
     * Schedules a list of subscriptions for bandwidth management.
     *
     * @param subscriptions
     *            the subscription
     * @return the set of subscription names that have had some cycles
     *         unscheduled
     */
    public Set<String> schedule(List<Subscription<T, C>> subscriptions) {
        BandwidthRequest<T, C> request = new BandwidthRequest<>();
        request.setRequestType(RequestType.SCHEDULE_SUBSCRIPTION);
        request.setSubscriptions(subscriptions);

        try {
            @SuppressWarnings("unchecked")
            Set<String> retVal = sendRequest(request, Set.class);
            return retVal;
        } catch (Exception e) {
            LogUtil.logIterable(statusHandler, Priority.PROBLEM,
                    "Unable to schedule the following subscriptions for bandwidth management:",
                    subscriptions, e);
            return Collections.emptySet();
        }
    }

    /**
     * Proposes scheduling a subscription for bandwidth management
     *
     * @param subscription
     *            the subscription
     * @return the response object
     */
    public ProposeScheduleResponse proposeSchedule(
            Subscription<T, C> subscription) {
        return proposeSchedule(Arrays.asList(subscription));
    }

    /**
     * Proposes scheduling the subscriptions with bandwidth management.
     *
     * @param subscriptions
     *            the subscriptions
     * @return the response object
     */
    public ProposeScheduleResponse proposeSchedule(
            List<Subscription<T, C>> subscriptions) {
        BandwidthRequest<T, C> request = new BandwidthRequest<>();
        request.setRequestType(RequestType.PROPOSE_SCHEDULE_SUBSCRIPTION);
        request.setSubscriptions(subscriptions);

        try {
            return sendRequest(request, ProposeScheduleResponse.class);
        } catch (Exception e) {
            LogUtil.logIterable(statusHandler, Priority.PROBLEM,
                    "Returning null response object, unable to propose scheduling"
                            + "the following subscriptions for bandwidth management:",
                    subscriptions, e);
            return new ProposeScheduleResponse();
        }
    }

    /**
     * Reinitializes the state of bandwidth management using the persistent
     * store. Should only be called when the in-memory objects may be corrupted,
     * e.g. a change was scheduled however the store of the actual object fails.
     */
    public void reinitialize() {
        BandwidthRequest<T, C> request = new BandwidthRequest<>();
        request.setRequestType(RequestType.REINITIALIZE);

        try {
            sendRequest(request);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to reinitialize the bandwidth manager.", e);
        }
    }

    /**
     * Retrieve the estimated completion time for an adhoc subscription.
     *
     * @param sub
     *            the subscription
     * @return the estimated completion time as a date
     */
    public Date getEstimatedCompletionTime(AdhocSubscription<T, C> sub) {
        BandwidthRequest<T, C> request = new BandwidthRequest<>();
        request.setSubscriptions(Arrays.<Subscription<T, C>>asList(sub));
        request.setRequestType(RequestType.GET_ESTIMATED_COMPLETION);
        try {
            return sendRequest(request, Date.class);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to retrieve the estimated completion time, returning the current time.",
                    e);
            return new Date();
        }
    }

    /**
     * Retrieve bandwidth graph data.
     *
     * @return bandwidth graph data
     */
    public BandwidthGraphData getBandwidthGraphData() {
        BandwidthRequest<T, C> request = new BandwidthRequest<>();
        request.setRequestType(RequestType.GET_BANDWIDTH_GRAPH_DATA);
        try {
            return sendRequest(request, BandwidthGraphData.class);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to retrieve bandwidth graph data.", e);
            return new BandwidthGraphData();
        }
    }

    /**
     * Get the Subscription status summary.
     *
     * @param subscription
     *            The subscription
     *
     * @return The summary
     */
    public SubscriptionStatusSummary getSubscriptionStatusSummary(
            Subscription<T, C> subscription) {
        BandwidthRequest<T, C> request = new BandwidthRequest<>();
        request.setSubscriptions(
                Arrays.<Subscription<T, C>>asList(subscription));
        request.setRequestType(RequestType.GET_SUBSCRIPTION_STATUS);
        try {
            return sendRequest(request, SubscriptionStatusSummary.class);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to retrieve the estimated completion time, returning the current time.",
                    e);
            return new SubscriptionStatusSummary();
        }
    }
}
