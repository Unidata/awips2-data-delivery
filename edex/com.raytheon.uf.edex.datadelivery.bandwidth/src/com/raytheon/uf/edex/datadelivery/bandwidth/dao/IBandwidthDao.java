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
package com.raytheon.uf.edex.datadelivery.bandwidth.dao;

import java.util.Date;
import java.util.List;

import com.raytheon.uf.common.datadelivery.bandwidth.data.SubscriptionStatusSummary;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.datadelivery.bandwidth.BandwidthManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;

/**
 * Extracted from {@link BandwidthContextFactory} so that
 * {@link BandwidthManager} can be run in memory (e.g. for testing proposed
 * bandwidth size limitations and informing the user which subscriptions would
 * be unable to be scheduled).
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 23, 2012  1286     djohnson  Initial creation
 * Jun 03, 2013  2038     djohnson  Add method to get subscription retrievals by
 *                                  provider, dataset, and status.
 * Jun 13, 2013  2095     djohnson  Implement ability to store a collection of
 *                                  subscriptions.
 * Jun 24, 2013  2106     djohnson  Add more methods.
 * Jul 18, 2013  1653     mpduff    Added getSubscriptionStatusSummary.
 * Dec 17, 2013  2636     bgonzale  Added method to get a BandwidthAllocation.
 * May 27, 2015  4531     dhladky   Remove excessive Calendar references.
 * Apr 05, 2017  1045     tjensen   Add Coverage generics for DataSetMetaData
 * May 26, 2017  6186     rjpeter   Remove BandwidthDataSetUpdate and added
 *                                  purgeAllocations
 * Sep 18, 2017  6415     rjpeter   Purge SubscriptionRetrieval
 * Oct 25, 2017  6484     tjensen   Merged SubscriptionRetrievals and
 *                                  BandwidthAllocations
 * </pre>
 *
 * @author djohnson
 */

public interface IBandwidthDao<T extends Time, C extends Coverage> {

    /**
     * Get BandwidthAllocations.
     *
     * @param network
     *            Retrieve BandwidthAllocations with the specified network.
     *
     * @return A List of BandwidthAllocations that have the specified network.
     */
    List<BandwidthAllocation> getBandwidthAllocations(Network network);

    /**
     * Get BandwidthAllocations by Network and Bandwidth Bucket Id.
     *
     * @param network
     *            Retrieve BandwidthAllocations with the specified network.
     * @param bandwidthBucketIdList
     *            Retrieve BandwidthAllocations with the specified set of
     *            Bandwidth Bucket Id values.
     *
     * @return A List of BandwidthAllocations that have the specified network
     *         and Bandwidth Bucket Id values.
     */
    List<BandwidthAllocation> getBandwidthAllocations(Network network,
            List<Long> bandwidthBucketIdList);

    /**
     * Get BandwidthAllocations with a status of
     * {@link RetrievalStatus.DEFERRED}.
     *
     * @return A List of BandwidthAllocations that have a status of
     *         {@link RetrievalStatus.DEFERRED}.
     */
    List<BandwidthAllocation> getDeferred(Network network, Date endTime);

    /**
     * Get a BandwidthAllocations.
     *
     * @param registryId
     *            Retrieve the BandwidthAllocations with the specified
     *            registryId.
     *
     * @return A List of BandwidthAllocations that has the specified registryId
     *         or null if no such BandwidthAllocation exists.
     */
    List<BandwidthAllocation> getBandwidthAllocationsByRegistryId(
            String registryId);

    /**
     * Remove a BandwidthAllocation from the database.
     *
     * @param bandwidthAllocation
     *            The bandwidthAllocation to remove.
     */
    void remove(List<BandwidthAllocation> bandwidthAllocation);

    /**
     * Persist a BandwidthAllocation to the database.
     *
     * @param bandwidthAllocation
     *            The BandwidthAllocation to store.
     */
    void store(BandwidthAllocation bandwidthAllocation);

    /**
     * Persist a list of BandwidthAllocations to the database.
     *
     * @param bandwidthAllocations
     *            The BandwidthAllocations to store.
     */
    void store(List<BandwidthAllocation> bandwidthAllocations);

    /**
     * Update a BandwidthAllocation in the database.
     *
     * @param allocation
     *            The BandwidthAllocation to store.
     */
    void createOrUpdate(BandwidthAllocation allocation);

    /**
     * Update a BandwidthAllocation in the database.
     *
     * @param bandwidthAllocation
     *            The bandwidthAllocation to update.
     */
    void update(BandwidthAllocation allocation);

    /**
     * Find all bandwidth allocations in the specified state.
     *
     * @param state
     * @return the allocations in that state
     */
    List<BandwidthAllocation> getBandwidthAllocationsInState(
            RetrievalStatus state);

    /**
     * Get all {@link BandwidthAllocation} instances.
     *
     * @return the retrievals
     */
    List<BandwidthAllocation> getBandwidthAllocations();

    /**
     * Get the subscription status summary.
     *
     * @param sub
     *            The subscription
     *
     * @return the SubscriptionStatusSummary
     */
    SubscriptionStatusSummary getSubscriptionStatusSummary(
            Subscription<T, C> sub);

    /**
     * Get the BandwidthAllocation identified by the given id.
     *
     * @param id
     */
    BandwidthAllocation getBandwidthAllocation(long id);

    /**
     * Purge all bandwidth allocation prior to the threshold.
     *
     * @param purgeThreshold
     */
    void purgeBandwidthAllocationsBeforeDate(Date purgeThreshold)
            throws DataAccessLayerException;
}
