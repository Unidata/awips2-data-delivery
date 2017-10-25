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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import com.raytheon.uf.common.datadelivery.bandwidth.data.SubscriptionStatusSummary;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.dataplugin.persist.IPersistableDataObject;
import com.raytheon.uf.common.util.ReflectionUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;

/**
 * Provides a {@link IBandwidthDao} implementation in memory. Intentionally
 * package-private.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 24, 2012  1286     djohnson  Initial creation
 * Dec 12, 2012  1286     djohnson  Use concurrent lists to avoid concurrent
 *                                  modification exceptions.
 * Jun 03, 2013  2038     djohnson  Add method to get subscription retrievals by
 *                                  provider, dataset, and status.
 * Jun 13, 2013  2095     djohnson  Implement ability to store a collection of
 *                                  subscriptions.
 * Jul 09, 2013  2106     djohnson  Rather than copy all elements and remove
 *                                  unnecessary, just copy the ones that apply.
 * Jul 11, 2013  2106     djohnson  Use BandwidthSubscription instead of
 *                                  Subscription.
 * Jul 18, 2013  1653     mpduff    Implemented method.
 * Oct 02, 2013  1797     dhladky   generics
 * Dec 17, 2013  2636     bgonzale  Added method to get a BandwidthAllocation.
 * Dec 09, 2014  3550     ccody     Add method to get BandwidthAllocation list
 *                                  by network and Bandwidth Bucked Id values
 * May 27, 2015  4531     dhladky   Remove excessive Calendar references.
 * Apr 05, 2017  1045     tjensen   Add Coverage generics for DataSetMetaData
 * Aug 02, 2017  6186     rjpeter   Remove BandwidthDataSetUpdate and added
 *                                  purgeAllocations.
 * Sep 18, 2017  6415     rjpeter   Purge SubscriptionRetrieval
 * Oct 25, 2017  6484     tjensen   Merged SubscriptionRetrievals and
 *                                  BandwidthAllocations
 *
 * </pre>
 *
 * @author djohnson
 */
class InMemoryBandwidthDao<T extends Time, C extends Coverage>
        implements IBandwidthDao<T, C> {

    private static final AtomicLong idSequence = new AtomicLong(1);

    /*
     * Explicitly ConcurrentLinkedQueue so we can use methods that require that
     * type to be concurrently safe
     */
    private final ConcurrentLinkedQueue<BandwidthAllocation> bandwidthAllocations = new ConcurrentLinkedQueue<>();

    @Override
    public List<BandwidthAllocation> getBandwidthAllocations(Network network) {
        List<BandwidthAllocation> allocations = new ArrayList<>();

        for (BandwidthAllocation current : bandwidthAllocations) {
            if (current.getNetwork() == network) {
                allocations.add(current.copy());
            }
        }

        return allocations;
    }

    @Override
    public List<BandwidthAllocation> getBandwidthAllocations(Network network,
            List<Long> bandwidthBucketIdList) {
        List<BandwidthAllocation> allocations = new ArrayList<>();

        if (bandwidthBucketIdList == null) {
            return (allocations);
        }

        for (BandwidthAllocation current : bandwidthAllocations) {
            long bandwidthBucketId = current.getBandwidthBucket();
            Long bandwidthBucketIdLong = Long.valueOf(bandwidthBucketId);
            if ((current.getNetwork() == network) && (bandwidthBucketIdList
                    .contains(bandwidthBucketIdLong))) {
                allocations.add(current.copy());
            }
        }

        return allocations;
    }

    @Override
    public List<BandwidthAllocation> getBandwidthAllocationsInState(
            RetrievalStatus state) {
        List<BandwidthAllocation> allocations = new ArrayList<>();

        for (BandwidthAllocation current : bandwidthAllocations) {
            if (state.equals(current.getStatus())) {
                allocations.add(current.copy());
            }
        }

        return allocations;
    }

    @Override
    public List<BandwidthAllocation> getDeferred(Network network,
            Date endTime) {

        List<BandwidthAllocation> allocations = new ArrayList<>();

        for (BandwidthAllocation current : bandwidthAllocations) {
            if (network == current.getNetwork()
                    && RetrievalStatus.DEFERRED.equals(current.getStatus())
                    && !current.getEndTime().after(endTime)) {
                allocations.add(current.copy());
            }
        }

        return allocations;
    }

    @Override
    public List<BandwidthAllocation> getBandwidthAllocationsByRegistryId(
            String registryId) {

        final List<BandwidthAllocation> results = new ArrayList<>(2);

        for (BandwidthAllocation current : bandwidthAllocations) {
            if (registryId.equals(current.getSubscriptionId())) {
                results.add(current.copy());
            }
        }
        return results;
    }

    @Override
    public void store(List<BandwidthAllocation> retrievals) {
        for (BandwidthAllocation retrieval : retrievals) {
            update(retrieval);
        }
    }

    @Override
    public void store(BandwidthAllocation bandwidthAllocation) {
        replaceOldOrAddToCollection(bandwidthAllocations, bandwidthAllocation);
    }

    @Override
    public void createOrUpdate(BandwidthAllocation allocation) {
        replaceOldOrAddToCollection(bandwidthAllocations, allocation);
    }

    @Override
    public void update(BandwidthAllocation allocation) {
        replaceOldOrAddToCollection(bandwidthAllocations, allocation);
    }

    @Override
    public List<BandwidthAllocation> getBandwidthAllocations() {
        return new ArrayList<>(bandwidthAllocations);
    }

    @Override
    public SubscriptionStatusSummary getSubscriptionStatusSummary(
            Subscription<T, C> sub) {
        // Does nothing
        return null;
    }

    @Override
    public BandwidthAllocation getBandwidthAllocation(long id) {
        BandwidthAllocation ba = null;
        for (BandwidthAllocation current : bandwidthAllocations) {
            if (current.getId() == id) {
                ba = current;
                break;
            }
        }
        return ba;
    }

    @Override
    public void purgeBandwidthAllocationsBeforeDate(Date threshold) {
        Iterator<BandwidthAllocation> iter = bandwidthAllocations.iterator();
        while (iter.hasNext()) {
            BandwidthAllocation alloc = iter.next();
            if (threshold.after(alloc.getEndTime())) {
                iter.remove();
            }
        }
    }

    /**
     * @return
     */
    private static long getNextId() {
        return idSequence.getAndIncrement();
    }

    private <M extends IPersistableDataObject<Long>> void replaceOldOrAddToCollection(
            ConcurrentLinkedQueue<M> collection, M obj) {
        if (obj.getIdentifier() == BandwidthUtil.DEFAULT_IDENTIFIER) {
            // Have to reflectively set the identifier since it's not part of
            // the interface
            ReflectionUtil.setter(obj, "identifier", getNextId());
        } else {
            // Always use a greater id than any of the objects in the collection
            long idValue = idSequence.get();
            while (obj.getIdentifier() + 1 > idValue) {
                idValue = idSequence.incrementAndGet();
            }
            removeFromCollection(collection, obj);
        }

        collection.add(obj);
    }

    private static <M extends IPersistableDataObject<Long>> void removeFromCollection(
            ConcurrentLinkedQueue<M> collection, M obj) {
        for (Iterator<M> iter = collection.iterator(); iter.hasNext();) {
            M old = iter.next();
            if (old.getIdentifier().equals(obj.getIdentifier())) {
                iter.remove();
                break;
            }
        }
    }

    @Override
    public void remove(List<BandwidthAllocation> bas) {
        this.bandwidthAllocations.removeAll(bas);
    }
}
