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
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.raytheon.uf.common.datadelivery.bandwidth.data.SubscriptionStatusSummary;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.dataplugin.persist.IPersistableDataObject;
import com.raytheon.uf.common.util.ReflectionUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrieval;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrievalAttributes;
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
 * May 26, 2017  6186     rjpeter   Remove BandwidthDataSetUpdate
 *
 * </pre>
 *
 * @author djohnson
 */
class InMemoryBandwidthDao<T extends Time, C extends Coverage>
        implements IBandwidthDao<T, C> {

    private static final AtomicLong idSequence = new AtomicLong(1);

    // Explicitly ConcurrentLinkedQueue so we can use methods that require that
    // type to be concurrently safe
    private final ConcurrentLinkedQueue<BandwidthAllocation> bandwidthAllocations = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<BandwidthSubscription> bandwidthSubscriptions = new ConcurrentLinkedQueue<>();

    @Override
    public List<BandwidthAllocation> getBandwidthAllocations(
            Long subscriptionId) {
        List<BandwidthAllocation> allocations = new ArrayList<>();

        for (BandwidthAllocation current : bandwidthAllocations) {
            if ((current instanceof SubscriptionRetrieval)
                    && subscriptionId.equals(((SubscriptionRetrieval) current)
                            .getBandwidthSubscription().getId())) {
                allocations.add(current.copy());
            }
        }

        return allocations;
    }

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
    public BandwidthSubscription getBandwidthSubscription(long identifier) {
        for (BandwidthSubscription dao : bandwidthSubscriptions) {
            if (dao.getIdentifier() == identifier) {
                return dao.copy();
            }
        }
        return null;
    }

    @Override
    public BandwidthSubscription getBandwidthSubscription(String registryId,
            Date baseReferenceTime) {
        List<BandwidthSubscription> bandwidthSubscriptions = getBandwidthSubscriptionByRegistryId(
                registryId);
        for (BandwidthSubscription bandwidthSubscription : bandwidthSubscriptions) {
            if (bandwidthSubscription.getBaseReferenceTime()
                    .equals(baseReferenceTime)) {
                return bandwidthSubscription;
            }
        }
        return null;
    }

    @Override
    public List<BandwidthSubscription> getBandwidthSubscription(
            Subscription<T, C> subscription) {
        return getBandwidthSubscriptionByRegistryId(subscription.getId());
    }

    @Override
    public List<BandwidthSubscription> getBandwidthSubscriptionByRegistryId(
            String registryId) {
        final List<BandwidthSubscription> results = Lists.newArrayList();

        for (BandwidthSubscription current : bandwidthSubscriptions) {
            if (registryId.equals(current.getRegistryId())) {
                results.add(current.copy());
            }
        }
        return results;
    }

    @Override
    public SubscriptionRetrieval getSubscriptionRetrieval(long identifier) {
        for (BandwidthAllocation current : bandwidthAllocations) {
            if (current.getId() == identifier
                    && current instanceof SubscriptionRetrieval) {
                return ((SubscriptionRetrieval) current).copy();
            }
        }
        return null;
    }

    @Override
    public List<SubscriptionRetrieval> getSubscriptionRetrievals(
            String provider, String dataSetName, Date baseReferenceTime) {
        List<SubscriptionRetrieval> results = Lists.newArrayList();

        for (BandwidthAllocation current : bandwidthAllocations) {
            if (current instanceof SubscriptionRetrieval) {
                BandwidthSubscription subscription;
                final SubscriptionRetrieval subscriptionRetrieval = (SubscriptionRetrieval) current;
                subscription = subscriptionRetrieval.getBandwidthSubscription();
                if (provider.equals(subscription.getProvider())
                        && dataSetName.equals(subscription.getDataSetName())
                        && baseReferenceTime.getTime() == subscriptionRetrieval
                                .getBandwidthSubscription()
                                .getBaseReferenceTime().getTime()) {
                    results.add(subscriptionRetrieval.copy());
                }
            }
        }

        return results;
    }

    @Override
    public List<SubscriptionRetrieval> getSubscriptionRetrievals(
            String provider, String dataSetName) {
        List<SubscriptionRetrieval> results = Lists.newArrayList();

        for (BandwidthAllocation current : bandwidthAllocations) {
            if (current instanceof SubscriptionRetrieval) {
                BandwidthSubscription subscription;
                final SubscriptionRetrieval subscriptionRetrieval = (SubscriptionRetrieval) current;
                subscription = subscriptionRetrieval.getBandwidthSubscription();
                if (provider.equals(subscription.getProvider())
                        && dataSetName.equals(subscription.getDataSetName())) {
                    results.add(subscriptionRetrieval.copy());
                }
            }
        }

        return results;
    }

    @Override
    public List<BandwidthSubscription> getBandwidthSubscriptions() {
        List<BandwidthSubscription> results = Lists.newArrayList();
        for (BandwidthSubscription subscription : bandwidthSubscriptions) {
            results.add(subscription.copy());
        }
        return results;
    }

    @Override
    public List<BandwidthSubscription> getBandwidthSubscriptions(
            String provider, String dataSetName, Date baseReferenceTime) {
        List<BandwidthSubscription> bandwidthSubscriptions = Lists
                .newArrayList();

        for (BandwidthSubscription current : this.bandwidthSubscriptions) {
            if (provider.equals(current.getProvider())
                    && dataSetName.equals(current.getDataSetName())
                    && baseReferenceTime.getTime() == current
                            .getBaseReferenceTime().getTime()) {
                bandwidthSubscriptions.add(current.copy());
            }
        }

        return bandwidthSubscriptions;
    }

    @Override
    public BandwidthSubscription newBandwidthSubscription(
            Subscription<T, C> subscription, Date baseReferenceTime) {
        BandwidthSubscription entity = BandwidthUtil
                .getSubscriptionDaoForSubscription(subscription,
                        baseReferenceTime);

        update(entity);

        return entity;
    }

    @Override
    public List<SubscriptionRetrieval> querySubscriptionRetrievals(
            long subscriptionId) {
        List<SubscriptionRetrieval> results = new ArrayList<>();

        for (BandwidthAllocation current : bandwidthAllocations) {
            if (current instanceof SubscriptionRetrieval) {
                final SubscriptionRetrieval subscriptionRetrieval = (SubscriptionRetrieval) current;
                if (subscriptionRetrieval.getBandwidthSubscription()
                        .getId() == subscriptionId) {
                    results.add(subscriptionRetrieval.copy());
                }
            }
        }

        return results;
    }

    @Override
    public List<SubscriptionRetrieval> querySubscriptionRetrievals(
            BandwidthSubscription dao) {
        return querySubscriptionRetrievals(dao.getId());
    }

    @Override
    public void remove(BandwidthSubscription subscriptionDao) {
        removeFromCollection(bandwidthSubscriptions, subscriptionDao);
    }

    @Override
    public void store(BandwidthAllocation bandwidthAllocation) {
        replaceOldOrAddToCollection(bandwidthAllocations, bandwidthAllocation);
    }

    @Override
    public void store(BandwidthSubscription subscriptionDao) {
        replaceOldOrAddToCollection(bandwidthSubscriptions, subscriptionDao);
    }

    @Override
    public void storeBandwidthSubscriptions(
            Collection<BandwidthSubscription> newSubscriptions) {
        for (BandwidthSubscription newSubscription : newSubscriptions) {
            store(newSubscription);
        }
    }

    /**
     * @return
     */
    private static long getNextId() {
        return idSequence.getAndIncrement();
    }

    @Override
    public void store(List<SubscriptionRetrieval> retrievals) {
        for (SubscriptionRetrieval retrieval : retrievals) {
            update(retrieval);
        }
    }

    @Override
    public void createOrUpdate(BandwidthAllocation allocation) {
        replaceOldOrAddToCollection(bandwidthAllocations, allocation);
    }

    @Override
    public void update(BandwidthSubscription dao) {
        replaceOldOrAddToCollection(bandwidthSubscriptions, dao);
    }

    @Override
    public void update(BandwidthAllocation allocation) {
        replaceOldOrAddToCollection(bandwidthAllocations, allocation);
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
    public SortedSet<SubscriptionRetrieval> getSubscriptionRetrievals(
            String provider, String dataSetName, RetrievalStatus status) {

        List<SubscriptionRetrieval> results = Lists.newArrayList();

        for (BandwidthAllocation current : bandwidthAllocations) {
            if (current instanceof SubscriptionRetrieval) {
                BandwidthSubscription subscription;
                final SubscriptionRetrieval subscriptionRetrieval = (SubscriptionRetrieval) current;
                subscription = subscriptionRetrieval.getBandwidthSubscription();
                if (provider.equals(subscription.getProvider())
                        && dataSetName.equals(subscription.getDataSetName())
                        && status.equals(subscriptionRetrieval.getStatus())) {
                    results.add(subscriptionRetrieval.copy());
                }
            }
        }

        final TreeSet<SubscriptionRetrieval> treeSet = Sets
                .newTreeSet(new Comparator<SubscriptionRetrieval>() {
                    @Override
                    public int compare(SubscriptionRetrieval o1,
                            SubscriptionRetrieval o2) {
                        return o1.getStartTime().compareTo(o2.getStartTime());
                    }
                });

        treeSet.addAll(results);

        return treeSet;
    }

    @Override
    public SortedSet<SubscriptionRetrieval> getSubscriptionRetrievals(
            String provider, String dataSetName, RetrievalStatus status,
            Date earliestDate, Date latestDate) {

        List<SubscriptionRetrieval> results = Lists.newArrayList();

        for (BandwidthAllocation current : bandwidthAllocations) {
            if (current instanceof SubscriptionRetrieval) {
                BandwidthSubscription subscription;
                final SubscriptionRetrieval subscriptionRetrieval = (SubscriptionRetrieval) current;
                subscription = subscriptionRetrieval.getBandwidthSubscription();

                final Date subRetrievalStartTime = subscriptionRetrieval
                        .getStartTime();
                final boolean withinTimeLimits = !(earliestDate
                        .after(subRetrievalStartTime)
                        || latestDate.before(subRetrievalStartTime));

                if (provider.equals(subscription.getProvider())
                        && dataSetName.equals(subscription.getDataSetName())
                        && status.equals(subscriptionRetrieval.getStatus())
                        && withinTimeLimits) {
                    results.add(subscriptionRetrieval.copy());
                }
            }
        }

        final TreeSet<SubscriptionRetrieval> treeSet = Sets
                .newTreeSet(new Comparator<SubscriptionRetrieval>() {
                    @Override
                    public int compare(SubscriptionRetrieval o1,
                            SubscriptionRetrieval o2) {
                        return o1.getStartTime().compareTo(o2.getStartTime());
                    }
                });

        treeSet.addAll(results);

        return treeSet;
    }

    @Override
    public List<SubscriptionRetrieval> getSubscriptionRetrievals() {
        List<SubscriptionRetrieval> results = new ArrayList<>();

        for (BandwidthAllocation current : bandwidthAllocations) {
            if (current instanceof SubscriptionRetrieval) {
                results.add(((SubscriptionRetrieval) current).copy());
            }
        }
        return results;
    }

    @Override
    public List<BandwidthAllocation> getBandwidthAllocationsForNetworkAndBucketStartTime(
            Network network, long bucketStartTime) {

        List<BandwidthAllocation> allocations = new ArrayList<>();

        for (BandwidthAllocation current : bandwidthAllocations) {
            if (current.getNetwork() == network
                    && current.getBandwidthBucket() == bucketStartTime) {
                allocations.add(current.copy());
            }
        }

        return allocations;
    }

    @Override
    public void store(SubscriptionRetrievalAttributes<T, C> attributes) {
        // Does nothing
    }

    @Override
    public void storeSubscriptionRetrievalAttributes(
            List<SubscriptionRetrievalAttributes<T, C>> retrievalAttributes) {
        // Does nothing
    }

    @Override
    public void update(SubscriptionRetrievalAttributes<T, C> attributes) {
        // Does nothing
    }

    @Override
    public SubscriptionRetrievalAttributes<T, C> getSubscriptionRetrievalAttributes(
            SubscriptionRetrieval retrieval) {
        return null;
    }

    @Override
    public SubscriptionStatusSummary getSubscriptionStatusSummary(
            Subscription<T, C> sub) {
        // Does nothing
        return null;
    }

    @Override
    public BandwidthAllocation getBandwidthAllocation(long id) {
        for (BandwidthAllocation current : bandwidthAllocations) {
            if (current.getId() == id) {
                return current;
            }
        }
        return null;
    }

}
