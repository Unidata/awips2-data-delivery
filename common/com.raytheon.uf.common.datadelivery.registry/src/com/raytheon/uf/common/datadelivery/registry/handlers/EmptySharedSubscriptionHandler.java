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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.PendingSubscription;
import com.raytheon.uf.common.datadelivery.registry.SharedSubscription;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;

/**
 * SharedSubscriptionHandler that performs no operations. Injected when shared
 * subscriptions should be ignored. Used by the wfo registry since wfos do not
 * process shared subscriptions.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 04, 2013  2545      bgonzale    Initial creation
 * Jan 29, 2014  2636      mpduff      Scheduling refactor.
 * Mar 31, 2014 2889      dhladky      Added username for notification center tracking.
 * Apr 03, 2018 7240       tjensen     Added getByDataSetAndProvider
 *
 * </pre>
 *
 * @author bgonzale
 */

public class EmptySharedSubscriptionHandler
        implements ISharedSubscriptionHandler {

    @Override
    public SharedSubscription getByPendingSubscription(
            PendingSubscription pending) throws RegistryHandlerException {
        return null;
    }

    @Override
    public SharedSubscription getByPendingSubscriptionId(String id)
            throws RegistryHandlerException {
        return null;
    }

    @Override
    public List<SharedSubscription> getActiveByDataSetAndProvider(
            String dataSetName, String providerName)
            throws RegistryHandlerException {
        return Collections.emptyList();
    }

    @Override
    public SharedSubscription getByName(String name)
            throws RegistryHandlerException {
        return null;
    }

    @Override
    public List<SharedSubscription> getByNames(Collection<String> names)
            throws RegistryHandlerException {
        return Collections.emptyList();
    }

    @Override
    public List<SharedSubscription> getByOwner(String owner)
            throws RegistryHandlerException {
        return Collections.emptyList();
    }

    @Override
    public List<SharedSubscription> getByGroupName(String group)
            throws RegistryHandlerException {
        return Collections.emptyList();
    }

    @Override
    public List<SharedSubscription> getByFilters(String group, String officeId)
            throws RegistryHandlerException {
        return Collections.emptyList();
    }

    @Override
    public Set<String> getSubscribedToDataSetNames(String siteId)
            throws RegistryHandlerException {
        return Collections.emptySet();
    }

    @Override
    public List<SharedSubscription> getActive()
            throws RegistryHandlerException {
        return Collections.emptyList();
    }

    @Override
    public List<SharedSubscription> getActiveForRoute(Network route)
            throws RegistryHandlerException {
        return Collections.emptyList();
    }

    @Override
    public Map<Network, List<SharedSubscription>> getActiveForRoutes(
            Network... routes) throws RegistryHandlerException {
        return new HashMap<>(0);
    }

    @Override
    public SharedSubscription getById(String id)
            throws RegistryHandlerException {
        return null;
    }

    @Override
    public List<SharedSubscription> getAll() throws RegistryHandlerException {
        return Collections.emptyList();
    }

    @Override
    public void store(String username, SharedSubscription obj)
            throws RegistryHandlerException {
    }

    @Override
    public void update(String username, SharedSubscription obj)
            throws RegistryHandlerException {
    }

    @Override
    public void delete(SharedSubscription obj) throws RegistryHandlerException {
    }

    @Override
    public void deleteById(String username, String registryId)
            throws RegistryHandlerException {
    }

    @Override
    public void deleteByIds(String username, List<String> registryIds)
            throws RegistryHandlerException {
    }

    @Override
    public void delete(String username, SharedSubscription obj)
            throws RegistryHandlerException {
    }

    @Override
    public void delete(Collection<SharedSubscription> objects)
            throws RegistryHandlerException {
    }

    @Override
    public void delete(String username, Collection<SharedSubscription> objects)
            throws RegistryHandlerException {
    }

    @Override
    public List<SharedSubscription> getByDataSetAndProvider(String dataSetName,
            String providerName) throws RegistryHandlerException {
        return Collections.emptyList();
    }

}
