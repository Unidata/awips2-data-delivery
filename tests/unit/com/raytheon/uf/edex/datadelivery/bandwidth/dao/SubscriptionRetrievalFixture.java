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

import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.SubscriptionRetrievalAgent;

/**
 * TODO Add Description
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 13, 2012            djohnson     Initial creation
 * Aug 26, 2014 3365       ccody        Separate Data Delivery tests out of AWIPS 2 baseline.
 *
 * </pre>
 *
 * @author djohnson
 * @version 1.0	
 */

public class SubscriptionRetrievalFixture extends
        BaseBandwidthAllocationFixture<SubscriptionRetrieval> {

    public static final SubscriptionRetrievalFixture INSTANCE = new SubscriptionRetrievalFixture();

    /**
     * Private.
     */
    private SubscriptionRetrievalFixture() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionRetrieval getInstance(long seedValue, Random random) {
        SubscriptionRetrieval entity = super.getInstance(seedValue, random);
        entity.setDataSetAvailablityDelay(0);
        entity.setAgentType(SubscriptionRetrievalAgent.SUBSCRIPTION_AGENT);
        entity.setEstimatedSize(seedValue);
        entity.setBandwidthSubscription(SubscriptionDaoFixture.INSTANCE
                .get(seedValue));
        entity.setSubscriptionLatency(0);

        return entity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SubscriptionRetrieval getBandwidthAllocation() {
        return new SubscriptionRetrieval();
    }

}
