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

import java.util.Random;

import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription.SubscriptionPriority;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.AbstractFixture;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;

/**
 * Creates {@link BandwidthAllocation} instances.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 13, 2012            djohnson     Initial creation
 * Jul 11, 2013 2106       djohnson     Use SubscriptionPriority enum.
 * Aug 26, 2014 3365       ccody        Separate Data Delivery tests out of AWIPS 2 baseline.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public abstract class BaseBandwidthAllocationFixture<T extends BandwidthAllocation>
        extends AbstractFixture<T> {

    /**
     * {@inheritDoc}
     */
    @Override
    public T getInstance(long seedValue, Random random) {
        T entity = getBandwidthAllocation();
        entity.setActualStart(BandwidthUtil.now());
        entity.setActualEnd(BandwidthUtil.now());
        entity.setBandwidthBucket(TimeUtil.currentTimeMillis());
        entity.setNetwork(Network.OPSNET);
        entity.setPriority(SubscriptionPriority.HIGH);
        entity.setStatus(RetrievalStatus.DEFERRED);
        entity.setStartTime(BandwidthUtil.now());
        entity.setEndTime(BandwidthUtil.now());

        return entity;
    }

    /**
     * Get the implementation of {@link BandwidthAllocation}.
     * 
     * @return the object
     */
    protected abstract T getBandwidthAllocation();
}
