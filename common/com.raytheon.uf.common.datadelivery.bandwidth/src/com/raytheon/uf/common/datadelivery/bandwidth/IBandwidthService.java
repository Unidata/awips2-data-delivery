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

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthGraphData;
import com.raytheon.uf.common.datadelivery.bandwidth.data.SubscriptionStatusSummary;
import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;

/**
 * Service interface for interacting with the bandwidth manager.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 22, 2012 1286       djohnson     Initial creation
 * Nov 20, 2012 1286       djohnson     Add proposeSchedule methods.
 * Dec 06, 2012 1397       djohnson     Add ability to get bandwidth graph data.
 * Jul 11, 2013 2106       djohnson     Bandwidth service now returns names of subscriptions for proposing bandwidth availability.
 * Jul 18, 2013 1653       mpduff       Added getSubscriptionStatusSummary.
 * Oct 2, 2013 1797       dhladky      Generics
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public interface IBandwidthService<T extends Time, C extends Coverage> {

    /**
     * Retrieve the available bandwidth for a {@link Network}.
     * 
     * @param network
     *            the network
     * @return the bandwidth, in kilobytes (KB)
     */
    int getBandwidthForNetworkInKilobytes(Network network);

    /**
     * Set the available bandwidth for a {@link Network}.
     * 
     * @param network
     *            the network
     * @param bandwidth
     *            the bandwidth
     * @return true if successfully changed
     */
    boolean setBandwidthForNetworkInKilobytes(Network network, int bandwidth);

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
    Set<String> proposeBandwidthForNetworkInKilobytes(Network network,
            int bandwidth);

    /**
     * Schedules a list of subscriptions for bandwidth management.
     * 
     * @param subscriptions
     *            the subscription
     * @return the set of subscription names that have had some cycles
     *         unscheduled
     */
    Set<String> schedule(List<Subscription<T, C>> subscriptions);
    
    /**
     * Schedules a subscription for bandwidth management.
     * 
     * @param subscriptions
     *            the subscription
     * @return the set of subscription names that have had some cycles
     *         unscheduled
     */
    Set<String> schedule(Subscription<T, C> subscription);
    
    /**
     * Proposes scheduling a subscription for bandwidth management
     * 
     * @param subscription
     *            the subscription
     * @return the response object
     */
    IProposeScheduleResponse proposeSchedule(Subscription<T, C> subscription);

    /**
     * Proposes scheduling the subscriptions with bandwidth management.
     * 
     * @param subscriptions
     *            the subscriptions
     * @return the response object
     */
    IProposeScheduleResponse proposeSchedule(List<Subscription<T, C>> subscriptions);

    /**
     * Reinitializes the state of bandwidth management using the persistent
     * store. Should only be called when the in-memory objects may be corrupted,
     * e.g. a change was scheduled however the store of the actual object fails.
     */
    void reinitialize();

    /**
     * Retrieve the estimated completion time for an adhoc subscription.
     * 
     * @param sub
     *            the subscription
     * @return the estimated completion time as a date
     */
    Date getEstimatedCompletionTime(AdhocSubscription<T, C> sub);

    /**
     * Retrieve bandwidth graph data.
     * 
     * @return bandwidth graph data
     */
    BandwidthGraphData getBandwidthGraphData();

    /**
     * Get the Subscription status summary.
     * 
     * @param subscription
     *            The subscription
     * 
     * @return The summary
     */
    SubscriptionStatusSummary getSubscriptionStatusSummary(
            Subscription<T, C> subscription);

}
