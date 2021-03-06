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
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.DataSetLatency;
import com.raytheon.uf.edex.datadelivery.bandwidth.hibernate.DataSetLatencyDao;

/**
 * Computes current latency for the subscription.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 31, 2012 1286       djohnson    Initial creation
 * Dec 01, 2014 3550       ccody       Added extended Latency Processing
 * Mar 16, 2016 3919       tjensen     Cleanup unneeded interfaces
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class SubscriptionValueLatencyCalculator implements
        ISubscriptionLatencyCalculator {

    protected DataSetLatencyDao dataSetLatencyDao = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDataSetLatencyDao(DataSetLatencyDao dataSetLatencyDao) {
        this.dataSetLatencyDao = dataSetLatencyDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLatency(Subscription subscription) {

        if (dataSetLatencyDao != null) {
            String dataSetName = subscription.getDataSetName();
            String providerName = subscription.getProvider();
            DataSetLatency dataSetLatency = dataSetLatencyDao
                    .getByDataSetNameAndProvider(dataSetName, providerName);
            if (dataSetLatency != null) {
                long now = TimeUtil.currentTimeMillis();
                boolean isValid = dataSetLatency.isDataSetLatencyValid(now);
                if (isValid == true) {
                    return (dataSetLatency.getExtendedLatency());
                } else {
                    // The lingering (old) record must be deleted.
                    dataSetLatencyDao.delete(dataSetLatency);
                }
            }
        }
        return subscription.getLatencyInMinutes();
    }

}
