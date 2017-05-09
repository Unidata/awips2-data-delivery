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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.IndexColumn;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Class representing a Subscription that may have been aggregated with other
 * subscriptions to maximize bandwidth usage.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 02, 2012  726      jspinks   Initial creation
 * Oct 16, 2012  726      djohnson  Added explicit length to subSubscription,
 *                                  made it nullable for single table strategy.
 * Nov 09, 2012  1286     djohnson  Add reference back to owning
 *                                  BandwidthSubscription.
 * Jun 24, 2013  2106     djohnson  Add copy constructor.
 * Jul 11, 2013  2106     djohnson  Use SubscriptionPriority enum, lazy load the
 *                                  Subscription object.
 * May 09, 2017  6186     rjpeter   Added url column
 *
 * </pre>
 *
 */
@Entity
@DiscriminatorValue("SubscriptionRetrieval")
@DynamicSerialize
public class SubscriptionRetrieval extends BandwidthAllocation {

    private static final long serialVersionUID = 4563049024191145668L;

    @Column
    @DynamicSerializeElement
    private int dataSetAvailablityDelay;

    /**
     * A link to the owning BandwidthSubscription entity.
     */
    @DynamicSerializeElement
    @ManyToOne(fetch = FetchType.EAGER, optional = true, cascade = CascadeType.PERSIST)
    // Must be nullable because we use a single table strategy
    @IndexColumn(name = "subscriptionid_fk", nullable = true)
    private BandwidthSubscription bandwidthSubscription;

    @Column
    @DynamicSerializeElement
    private int subscriptionLatency;

    @Column
    private String url;

    /**
     * Constructor.
     */
    public SubscriptionRetrieval() {
    }

    /**
     * Copy constructor.
     *
     * @param from
     *            the instance to copy from
     */
    public SubscriptionRetrieval(SubscriptionRetrieval from) {
        super(from);
        this.setBandwidthSubscription(from.getBandwidthSubscription().copy());
        this.setDataSetAvailablityDelay(from.dataSetAvailablityDelay);
        this.setSubscriptionLatency(from.getSubscriptionLatency());
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

    /**
     * @return the bandwidthSubscription
     */
    public BandwidthSubscription getBandwidthSubscription() {
        return bandwidthSubscription;
    }

    /**
     * @param bandwidthSubscription
     *            the bandwidthSubscription to set
     */
    public void setBandwidthSubscription(
            BandwidthSubscription bandwidthSubscription) {
        this.bandwidthSubscription = bandwidthSubscription;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionRetrieval copy() {
        return new SubscriptionRetrieval(this);
    }
}
