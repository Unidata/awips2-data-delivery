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
package com.raytheon.uf.edex.datadelivery.bandwidth.util;

import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.edex.datadelivery.bandwidth.hibernate.DataSetLatencyDao;

/**
 * Calculate the latency that should be added to the retrievals for a
 * subscription.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 05, 2012 0726       djohnson     Initial creation
 * Dec 01, 2014 3550       ccody        Added extended Latency Processing
 * Mar 16, 2016 3919       tjensen      Cleanup unneeded interfaces
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public interface ISubscriptionLatencyCalculator {

    /**
     * Set DataSetLatencyDao.
     * 
     * Set DataSetLatencyDao to calculate Subscriptions with extended Latency
     * 
     * @param dataSetLatencyDao
     *            DataSetLatencyDao
     */
    public void setDataSetLatencyDao(DataSetLatencyDao dataSetLatencyDao);

    /**
     * Calculate the latency.
     * 
     * @param subscription
     *            the subscription
     * @return the latency
     */
    int getLatency(Subscription subscription);
}
