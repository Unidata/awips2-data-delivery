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

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raytheon.uf.common.datadelivery.bandwidth.data.SubscriptionStatusSummary;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;

/**
 * {@link IBandwidthDao} implementation that interacts with Hibernate.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 23, 2012  1286     djohnson  Extracted from BandwidthContextFactory.
 * Feb 07, 2013  1543     djohnson  Moved session management context to CoreDao.
 * Feb 11, 2013  1543     djohnson  Use Spring transactions.
 * Feb 13, 2013  1543     djohnson  Converted into a service, created new DAOs
 *                                  as required.
 * Jun 03, 2013  2038     djohnson  Add method to get subscription retrievals by
 *                                  provider, dataset, and status.
 * Jun 13, 2013  2095     djohnson  Implement ability to store a collection of
 *                                  subscriptions.
 * Jun 24, 2013  2106     djohnson  Implement new methods.
 * Jul 18, 2013  1653     mpduff    Added getSubscriptionStatusSummary.
 * Aug 28, 2013  2290     mpduff    Check for no subscriptions.
 * Oct 02, 2013  1797     dhladky   Generics
 * Dec 17, 2013  2636     bgonzale  Added method to get a BandwidthAllocation.
 * Dec 09, 2014  3550     ccody     Add method to get BandwidthAllocation list
 *                                  by network and Bandwidth Bucked Id values
 * May 27, 2015  4531     dhladky   Remove excessive Calendar references.
 * Apr 05, 2017  1045     tjensen   Add Coverage generics for DataSetMetaData
 * May 26, 2017  6186     rjpeter   Remove BandwidthDataSetUpdate
 * Aug 02, 2017  6186     rjpeter   Added purgeAllocations.
 * Sep 18, 2017  6415     rjpeter   Purge SubscriptionRetrieval
 * Oct 25, 2017  6484     tjensen   Merged SubscriptionRetrievals and
 *                                  BandwidthAllocations
 *
 * </pre>
 *
 * @author djohnson
 */
@Transactional
@Service
public class HibernateBandwidthDao<T extends Time, C extends Coverage>
        implements IBandwidthDao<T, C> {

    private BandwidthAllocationDao bandwidthAllocationDao;

    /**
     * Constructor.
     */
    public HibernateBandwidthDao() {
    }

    @Override
    public List<BandwidthAllocation> getBandwidthAllocations(Network network) {
        return bandwidthAllocationDao.getByNetwork(network);
    }

    @Override
    public List<BandwidthAllocation> getBandwidthAllocations(Network network,
            List<Long> bandwidthBucketIdList) {
        return bandwidthAllocationDao.getByNetworkAndBandwidthBucketIdList(
                network, bandwidthBucketIdList);
    }

    @Override
    public List<BandwidthAllocation> getBandwidthAllocationsInState(
            RetrievalStatus state) {
        return bandwidthAllocationDao.getByState(state);
    }

    @Override
    public List<BandwidthAllocation> getDeferred(Network network,
            Date endTime) {
        return bandwidthAllocationDao.getDeferred(network, endTime);
    }

    @Override
    public List<BandwidthAllocation> getBandwidthAllocationsByRegistryId(
            String registryId) {
        return bandwidthAllocationDao.getByRegistryId(registryId);
    }

    @Override
    public void store(List<BandwidthAllocation> retrievals) {
        for (BandwidthAllocation retrieval : retrievals) {
            bandwidthAllocationDao.create(retrieval);
        }
    }

    @Override
    public void remove(List<BandwidthAllocation> bandwidthAllocations) {
        bandwidthAllocationDao.deleteAll(bandwidthAllocations);
    }

    @Override
    public void store(BandwidthAllocation bandwidthAllocation) {
        bandwidthAllocationDao.create(bandwidthAllocation);
    }

    @Override
    public void createOrUpdate(BandwidthAllocation allocation) {
        bandwidthAllocationDao.createOrUpdate(allocation);
    }

    @Override
    public void update(BandwidthAllocation allocation) {
        bandwidthAllocationDao.update(allocation);
    }

    @Override
    public List<BandwidthAllocation> getBandwidthAllocations() {
        return bandwidthAllocationDao.getAll();
    }

    @Override
    public SubscriptionStatusSummary getSubscriptionStatusSummary(
            Subscription<T, C> sub) {
        SubscriptionStatusSummary summary = new SubscriptionStatusSummary();

        List<BandwidthAllocation> subRetrievalList = getBandwidthAllocationsByRegistryId(
                sub.getId());
        if (subRetrievalList != null && !subRetrievalList.isEmpty()) {
            Collections.sort(subRetrievalList,
                    new Comparator<BandwidthAllocation>() {
                        @Override
                        public int compare(BandwidthAllocation o1,
                                BandwidthAllocation o2) {
                            Date date1 = o1.getStartTime();
                            Date date2 = o2.getStartTime();
                            if (date1.before(date2)) {
                                return -1;
                            } else if (date1.after(date2)) {
                                return 1;
                            }

                            return 0;
                        }

                    });
            summary.setStartTime(
                    subRetrievalList.get(0).getStartTime().getTime());
            summary.setEndTime(subRetrievalList.get(subRetrievalList.size() - 1)
                    .getEndTime().getTime());
        }
        summary.setDataSize(sub.getDataSetSize());
        summary.setLatency(sub.getLatencyInMinutes());

        return summary;
    }

    @Override
    public BandwidthAllocation getBandwidthAllocation(long id) {
        return bandwidthAllocationDao.getById(id);
    }

    @Override
    public void purgeBandwidthAllocationsBeforeDate(Date threshold)
            throws DataAccessLayerException {
        bandwidthAllocationDao.deleteBeforeDate(threshold);
    }

    /**
     * @return the bandwidthAllocationDao
     */
    public BandwidthAllocationDao getBandwidthAllocationDao() {
        return bandwidthAllocationDao;
    }

    /**
     * @param bandwidthAllocationDao
     *            the bandwidthAllocationDao to set
     */
    public void setBandwidthAllocationDao(
            BandwidthAllocationDao bandwidthAllocationDao) {
        this.bandwidthAllocationDao = bandwidthAllocationDao;
    }

}
