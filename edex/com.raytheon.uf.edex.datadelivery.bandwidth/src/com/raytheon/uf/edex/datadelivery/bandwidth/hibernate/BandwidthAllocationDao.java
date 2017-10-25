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
package com.raytheon.uf.edex.datadelivery.bandwidth.hibernate;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;

import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.SessionManagedDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;

/**
 * DAO that handles {@link BandwidthAllocation} instances.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 13, 2013  1543     djohnson  Initial creation
 * Feb 22, 2013  1543     djohnson  Made public as YAJSW doesn't like Spring
 *                                  exceptions.
 * Aug 02, 2017  6186     rjpeter   Added purgeAllocations
 * Oct 25, 2017  6484     tjensen   Merged with SubscriptionRetrievalDao
 *
 * </pre>
 *
 * @author djohnson
 */
public class BandwidthAllocationDao
        extends SessionManagedDao<Long, BandwidthAllocation> {

    private static final String DELETE_SUBSCRIPTIONRETRIEVAL_BEFORE_DATE = "delete from BandwidthAllocation sr where sr.endTime <= :endDate";

    private static final String GET_BANDWIDTH_ALLOCATIONS_BY_SUBSCRIPTION_ID = "from BandwidthAllocation res where res.registryId = :subscriptionId";

    private static final String GET_BANDWIDTH_ALLOCATIONS_BY_NETWORK = "from BandwidthAllocation res where res.network = :network";

    private static final String GET_BANDWIDTH_ALLOCATIONS_BY_NETWORK_AND_BUCKET_START_TIME = GET_BANDWIDTH_ALLOCATIONS_BY_NETWORK
            + " and res.bandwidthBucket = :bandwidthBucket";

    private static final String GET_BANDWIDTH_ALLOCATIONS_BY_STATE = "from BandwidthAllocation res where res.status = :state";

    private static final String GET_DEFERRED = "from BandwidthAllocation alloc where "
            + "alloc.status = :status and " + "alloc.network = :network and "
            + "alloc.endTime <= :endTime";

    private static final String GET_BANDWIDTH_ALLOCATIONS_BY_NETWORK_AND_BUCKET_ID_LIST = GET_BANDWIDTH_ALLOCATIONS_BY_NETWORK
            + " and res.bandwidthBucket in ( :bandwidthBucketIdList )";

    /**
     * Get by the network.
     *
     * @param network
     * @return
     */
    public List<BandwidthAllocation> getByNetwork(Network network) {
        return query(GET_BANDWIDTH_ALLOCATIONS_BY_NETWORK, "network", network);
    }

    /**
     * Get by the network and the bucket id list.
     *
     * @param network
     * @param bucketIdList
     *
     * @return
     */
    public List<BandwidthAllocation> getByNetworkAndBandwidthBucketIdList(
            Network network, List<Long> bandwidthBucketIdList) {

        String queryString = GET_BANDWIDTH_ALLOCATIONS_BY_NETWORK_AND_BUCKET_ID_LIST;
        Query query = getCurrentSession().createQuery(queryString);
        query.setMaxResults(0);
        query.setParameter("network", network);
        query.setParameterList("bandwidthBucketIdList", bandwidthBucketIdList);

        return (query.list());
    }

    /**
     * Get by the network and the bucket start time.
     *
     * @param network
     * @param bucketStartTime
     *
     * @return
     */
    public List<BandwidthAllocation> getByNetworkAndBucketStartTime(
            Network network, long bucketStartTime) {
        return query(

                GET_BANDWIDTH_ALLOCATIONS_BY_NETWORK_AND_BUCKET_START_TIME,
                "network", network, "bandwidthBucket", bucketStartTime);
    }

    /**
     * Get by retrieval status.
     *
     * @param state
     * @return
     */
    public List<BandwidthAllocation> getByState(RetrievalStatus state) {
        return query(GET_BANDWIDTH_ALLOCATIONS_BY_STATE, "state", state);
    }

    /**
     * Get deferred bandwidth allocations for the network and end time.
     *
     * @param network
     * @param endTime
     * @return
     */
    public List<BandwidthAllocation> getDeferred(Network network,
            Date endTime) {
        return query(GET_DEFERRED, "status", RetrievalStatus.DEFERRED,
                "network", network, "endTime", endTime);
    }

    /**
     * Get all Bandwidth Allocations for a subscription with the given registry
     * ID.
     *
     * @param registryId
     * @return
     */
    public List<BandwidthAllocation> getByRegistryId(String registryId) {
        return query(GET_BANDWIDTH_ALLOCATIONS_BY_SUBSCRIPTION_ID,
                "subscriptionId", registryId);
    }

    /**
     * Delete BandwidthAllocation entries before the specified date.
     *
     * @param thresholdDate
     * @return
     */
    public void deleteBeforeDate(Date thresholdDate)
            throws DataAccessLayerException {
        executeHQLStatement(DELETE_SUBSCRIPTIONRETRIEVAL_BEFORE_DATE, "endDate",
                thresholdDate);
    }

    @Override
    protected Class<BandwidthAllocation> getEntityClass() {
        return BandwidthAllocation.class;
    }
}
