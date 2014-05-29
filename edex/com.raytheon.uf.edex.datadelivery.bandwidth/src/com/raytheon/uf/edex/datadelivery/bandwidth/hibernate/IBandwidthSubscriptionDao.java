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
package com.raytheon.uf.edex.datadelivery.bandwidth.hibernate;

import java.util.Calendar;
import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.edex.database.dao.ISessionManagedDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription;

/**
 * DAO interface for {@link BandwidthSubscription}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 13, 2013 1543       djohnson     Initial creation
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
interface IBandwidthSubscriptionDao extends
        ISessionManagedDao<Long, BandwidthSubscription> {

    /**
     * Get {@link BandwidthSubscription} instances by the subscription object's
     * registry id and a base reference time.
     * 
     * @param registryId
     * @param baseReferenceTime
     * @return
     */
    BandwidthSubscription getByRegistryIdReferenceTime(String registryId,
            Calendar baseReferenceTime);

    /**
     * Get {@link BandwidthSubscription} instances by the subscription object's
     * registry id.
     * 
     * @param subscription
     * @return
     */
    List<BandwidthSubscription> getBySubscription(Subscription subscription);

    /**
     * Get {@link BandwidthSubscription} instances by the subscription object's
     * registry id.
     * 
     * @param registryId
     * @return
     */
    List<BandwidthSubscription> getByRegistryId(String registryId);

    /**
     * Get {@link BandwidthSubscription} instances by the provider name, dataset
     * name, and base reference time.
     * 
     * @param provider
     * @param dataSetName
     * @param baseReferenceTime
     * @return
     */
    List<BandwidthSubscription> getByProviderDataSetReferenceTime(
            String provider, String dataSetName, Calendar baseReferenceTime);

}