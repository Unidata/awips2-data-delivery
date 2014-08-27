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

import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDbInit;
import com.raytheon.uf.edex.datadelivery.bandwidth.interfaces.IBandwidthInitializer;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;

/**
 * Integration test {@link BandwidthInitializer}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 12, 2012 0726       djohnson     Initial creation
 * Apr 16, 2013 1906       djohnson     Implements RegistryInitializedListener.
 * Aug 26, 2014 3365       ccody        Separate Data Delivery tests out of AWIPS 2 baseline.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class IntegrationTestBandwidthInitializer implements
        IBandwidthInitializer {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean init(IBandwidthManager instance, IBandwidthDbInit dbInit,
            RetrievalManager retrievalManager) {

        retrievalManager.initRetrievalPlans();

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeAfterRegistryInit() {
        // Nothing to do
    }

    @Override
    public Map<Network, List<Subscription>> getSubMapByRoute() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
