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
package com.raytheon.uf.common.datadelivery.registry.handlers;

import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.PendingSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.registry.handler.IRegistryObjectHandler;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;

/**
 * The {@link IRegistryObjectHandler} interface for {@link Subscription}s.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 05, 2013 1841       djohnson     Initial creation
 * May 21, 2013 2020       mpduff       Rename UserSubscription to SiteSubscription.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
public interface ISubscriptionTypeHandler<T extends Subscription> extends
        IBaseSubscriptionHandler<T> {

    /**
     * Retrieve a subscription that a {@link PendingSubscription} is associated
     * to.
     * 
     * @param pending
     *            the pending subscription
     * @return the subscription, or null if not found
     * @throws RegistryHandlerException
     */
    T getByPendingSubscription(
            PendingSubscription pending)
            throws RegistryHandlerException;

    /**
     * Retrieve a subscription that a {@link PendingSubscription} is associated
     * to by the pending subscription's id.
     * 
     * @param id
     *            the pending subscription id
     * @return the subscription, or null if not found
     * @throws RegistryHandlerException
     */
    T getByPendingSubscriptionId(String id)
            throws RegistryHandlerException;

    /**
     * Retrieve active subscriptions for the dataset name and provider.
     * 
     * @param dataSetName
     *            the dataset name
     * @param providerName
     *            the provider name
     */
    List<T> getActiveByDataSetAndProvider(String dataSetName,
            String providerName) throws RegistryHandlerException;
}
