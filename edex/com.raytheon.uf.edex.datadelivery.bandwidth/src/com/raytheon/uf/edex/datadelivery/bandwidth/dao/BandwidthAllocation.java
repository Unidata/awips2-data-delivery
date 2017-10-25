package com.raytheon.uf.edex.datadelivery.bandwidth.dao;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription.SubscriptionPriority;
import com.raytheon.uf.common.dataplugin.persist.IPersistableDataObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.util.IDeepCopyable;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;

/**
 *
 * A bandwidth allocation.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 12, 2012  726      djohnson  Add SW history, use string version of enum.
 * Jun 24, 2013  2106     djohnson  Add copy constructor.
 * Jul 11, 2013  2106     djohnson  Use SubscriptionPriority enum.
 * Oct 30, 2013  2448     dhladky   Moved methods to TimeUtil.
 * Apr 02, 2014  2810     dhladky   Priority sorting of allocations.
 * May 27, 2015  4531     dhladky   Remove excessive Calendar references.
 * Aug 02, 2017  6186     rjpeter   Removed agentType.
 * Oct 25, 2017  6484     tjensen   Merged SubscriptionRetrievals and
 *                                  BandwidthAllocations
 *
 * </pre>
 *
 * @author djohnson
 */
@Entity
@Table(name = "bandwidth_allocation")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("BandwidthAllocation")
@DynamicSerialize
@SequenceGenerator(name = "BANDWIDTH_SEQ", sequenceName = "bandwidth_seq", allocationSize = 1, initialValue = 1)
public class BandwidthAllocation
        implements IPersistableDataObject<Long>, Serializable,
        IDeepCopyable<BandwidthAllocation>, Comparable<BandwidthAllocation> {

    private static final long serialVersionUID = 743702044231376839L;

    @Column(nullable = true)
    @DynamicSerializeElement
    private long bandwidthBucket;

    @Column(nullable = false)
    @DynamicSerializeElement
    private Date endTime;

    @Column(nullable = false)
    @DynamicSerializeElement
    private long estimatedSize;

    @Id
    @Column(name = "identifier")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BANDWIDTH_SEQ")
    @DynamicSerializeElement
    private long id = BandwidthUtil.DEFAULT_IDENTIFIER;

    @Column(nullable = false)
    @DynamicSerializeElement
    @Enumerated(EnumType.STRING)
    private SubscriptionPriority priority;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @DynamicSerializeElement
    private Network network;

    @Column(nullable = false)
    @DynamicSerializeElement
    private Date startTime;

    @Column(nullable = true)
    @DynamicSerializeElement
    @Enumerated(EnumType.STRING)
    private RetrievalStatus status;

    @Column
    @DynamicSerializeElement
    private int dataSetAvailablityDelay;

    @Column
    @DynamicSerializeElement
    private int subscriptionLatency;

    @Column
    @DynamicSerializeElement
    private String subName;

    @DynamicSerializeElement
    @Column(nullable = false)
    private Date baseReferenceTime;

    @DynamicSerializeElement
    @Column(nullable = false)
    private String subscriptionId;

    public BandwidthAllocation() {
        // Constructor for bean interface.
    }

    /**
     * Copy constructor.
     *
     * @param from
     */
    public BandwidthAllocation(BandwidthAllocation from) {
        final Date fromStartTime = from.getStartTime();
        if (fromStartTime != null) {
            this.setStartTime(fromStartTime);
        }
        final Date fromEndTime = from.getEndTime();
        if (fromEndTime != null) {
            this.setEndTime(fromEndTime);
        }

        this.setBandwidthBucket(from.getBandwidthBucket());
        this.setEstimatedSize(from.getEstimatedSize());
        this.setId(from.getId());
        this.setNetwork(from.getNetwork());
        this.setPriority(from.getPriority());
        this.setStatus(from.getStatus());
        this.setDataSetAvailablityDelay(from.getDataSetAvailablityDelay());
        this.setSubscriptionLatency(from.getSubscriptionLatency());
        this.setSubName(from.getSubName());
        this.setBaseReferenceTime(from.getBaseReferenceTime());
        this.setSubscriptionId(from.getSubscriptionId());
    }

    public long getBandwidthBucket() {
        return bandwidthBucket;
    }

    public Date getEndTime() {
        return this.endTime;
    }

    /**
     * Get the estimated size in kilobytes (kB).
     *
     * @return
     */
    public long getEstimatedSize() {
        return this.estimatedSize;
    }

    public long getEstimatedSizeInBytes() {
        return getEstimatedSize() * BandwidthUtil.BYTES_PER_KILOBYTE;
    }

    public long getId() {
        return id;
    }

    @Override
    public Long getIdentifier() {
        return Long.valueOf(id);
    }

    public SubscriptionPriority getPriority() {
        return priority;
    }

    public Network getNetwork() {
        return network;
    }

    public Date getStartTime() {
        return startTime;
    }

    public RetrievalStatus getStatus() {
        return status;
    }

    public void setBandwidthBucket(long bandwidthBucket) {
        this.bandwidthBucket = bandwidthBucket;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    /**
     * Set the estimated size in kilobytes (kB).
     *
     * @param estimatedSize
     *            the estimated size
     */
    public void setEstimatedSize(long estimatedSize) {
        this.estimatedSize = estimatedSize;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setIdentifier(Long identifier) {
        setId(identifier.longValue());
    }

    public void setPriority(SubscriptionPriority priority) {
        this.priority = priority;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setStatus(RetrievalStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("allocation id: [").append(getId()).append("] ");
        sb.append("path [").append(getNetwork()).append("] ");
        sb.append("priority [").append(getPriority()).append("] ");
        sb.append("size (bytes) [").append(getEstimatedSizeInBytes())
                .append("] ");
        sb.append("status [").append(getStatus()).append("] ");
        sb.append("startTime [").append(BandwidthUtil.format(getStartTime()))
                .append("] ");
        sb.append("endTime [").append(BandwidthUtil.format(getEndTime()))
                .append("]");
        return sb.toString();
    }

    /**
     * @return
     */
    @Override
    public BandwidthAllocation copy() {
        return new BandwidthAllocation(this);
    }

    @Override
    public int compareTo(BandwidthAllocation o) {

        SubscriptionPriority oPriority = o.priority;
        SubscriptionPriority myPriority = this.priority;

        return myPriority.compareTo(oPriority);
    }

    /**
     * @return the dataSetAvailablityDelay
     */
    public int getDataSetAvailablityDelay() {
        return dataSetAvailablityDelay;
    }

    /**
     * @return the subscriptionLatency
     */
    public int getSubscriptionLatency() {
        return subscriptionLatency;
    }

    public void setDataSetAvailablityDelay(int dataSetAvailablityDelay) {
        this.dataSetAvailablityDelay = dataSetAvailablityDelay;

    }

    /**
     * @param subscriptionLatency
     *            the subscriptionLatency to set
     */
    public void setSubscriptionLatency(int subscriptionLatency) {
        this.subscriptionLatency = subscriptionLatency;
    }

    public String getSubName() {
        return subName;
    }

    public void setSubName(String subName) {
        this.subName = subName;
    }

    public Date getBaseReferenceTime() {
        return baseReferenceTime;
    }

    public void setBaseReferenceTime(Date baseReferenceTime) {
        this.baseReferenceTime = baseReferenceTime;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String registryId) {
        this.subscriptionId = registryId;
    }
}
