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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.InitialPendingSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.ebxml.SubscriptionFilterableQuery;
import com.raytheon.uf.common.registry.RegistryQueryResponse;
import com.raytheon.uf.common.registry.RegistryResponse;
import com.raytheon.uf.common.registry.constants.AssociationTypes;
import com.raytheon.uf.common.registry.ebxml.AssociationQuery;
import com.raytheon.uf.common.registry.ebxml.RegistryUtil;
import com.raytheon.uf.common.registry.handler.IRegistryObjectHandler;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;

/**
 * {@link IRegistryObjectHandler} implementation for
 * {@link InitialPendingSubscription}s.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 04, 2013 1841       djohnson     Extracted from PendingSubscriptionHandler and genericized.
 * Jun 24, 2013 2106       djohnson     Now composes a registryHandler.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
public abstract class BasePendingSubscriptionHandler<T extends InitialPendingSubscription, QUERY extends SubscriptionFilterableQuery<T>>
        extends BaseSubscriptionHandler<T, QUERY> implements
        IBasePendingSubscriptionHandler<T> {

    /**
     * Overridden because pending subscriptions must also have their
     * associations to their {@link Subscription} objects deleted.
     * 
     * @param username
     *            the username
     * @param objects
     *            the objects to delete
     * 
     */
    @Override
    public void deleteByIds(String username, List<String> ids)
            throws RegistryHandlerException {

        for (String id : ids) {
            deleteAssociationToSubscription(id);
        }

        super.deleteByIds(username, ids);
    }

    /**
     * Deletes the association from a {@link InitialPendingSubscription} to a
     * {@link Subscription}.
     * 
     * @param id
     *            the pending subscription id
     * @throws RegistryHandlerException
     *             on unsuccessful response from the registry
     */
    private void deleteAssociationToSubscription(String id)
            throws RegistryHandlerException {
        AssociationQuery query = new AssociationQuery();
        query.setAssociationType(AssociationTypes.RELATED_TO_PATH);
        query.setSourceObjectId(id);
        query.setReturnObjects(false);

        // Delete associations
        RegistryResponse<Object> removalResponse = registryHandler
                .removeObjects(query);

        checkResponse(removalResponse, "deleteAssociationToSubscription");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getBySubscription(Subscription subscription)
            throws RegistryHandlerException {
        return getBySubscriptionId(RegistryUtil
                .getRegistryObjectKey(subscription));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getBySubscriptionId(final String id)
            throws RegistryHandlerException {
        // Checks for the existence of a pending subscription
        AssociationQuery query = new AssociationQuery();
        query.setAssociationType(AssociationTypes.RELATED_TO_PATH);
        query.setTargetObjectId(id);
        query.setReturnObjects(true);

        RegistryQueryResponse<Object> response = registryHandler
                .getObjects(query);

        checkResponse(response, "getBySubscriptionId");

        List<Object> results = response.getResults();
        // Currently only InitialPendingSubscriptions are associated to
        // Subscriptions,
        // but there could be other types of objects in the future
        Class<T> registryObjectClass = getRegistryObjectClass();
        for (Object obj : results) {
            if (registryObjectClass.isAssignableFrom(obj.getClass())) {
                return registryObjectClass.cast(obj);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     */
    @Override
    public List<T> getBySubscriptions(Collection<Subscription> subscriptions)
            throws RegistryHandlerException {
        List<String> ids = new ArrayList<String>(subscriptions.size());
        for (Subscription subscription : subscriptions) {
            ids.add(RegistryUtil.getRegistryObjectKey(subscription));
        }

        return getBySubscriptionIds(ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> getBySubscriptionIds(final List<String> ids)
            throws RegistryHandlerException {
        List<T> pending = new ArrayList<T>();

        for (String id : ids) {
            T possiblePending = getBySubscriptionId(id);
            if (possiblePending != null) {
                pending.add(possiblePending);
            }
        }

        return pending;
    }
}
