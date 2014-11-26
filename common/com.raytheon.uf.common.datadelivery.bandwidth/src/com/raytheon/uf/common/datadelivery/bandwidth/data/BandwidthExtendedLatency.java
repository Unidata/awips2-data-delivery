package com.raytheon.uf.common.datadelivery.bandwidth.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class to describe Extended Latency Properties for the BandwidthAllocations of
 * scheduled Subscriptions which have Data Set Meta Data, have not been
 * retrieved, and are about to expire. using {@link IBandwidthManager}.
 * BandwidthExtendedLatency contains a single attribut of "factor" which is a
 * whole integer of the ADDITIONAL percentage that should be given to an
 * expiring and incomplete Subscription.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 01, 2014 3550       ccody       Initial release.
 * 
 * </pre>
 * 
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
public class BandwidthExtendedLatency {

    @XmlAttribute(name = "factor", required = true)
    private int factor;

    /**
     * @return the factor for Latent Subscription Extension
     */
    public int getFactor() {
        return factor;
    }

    /**
     * @param newFactor
     *            the factor for Latent Subscription Extension
     */
    public void setFactor(int newFactor) {
        this.factor = newFactor;
    }

}
