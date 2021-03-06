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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.PendingSiteSubscription;
import com.raytheon.uf.common.datadelivery.registry.PendingSubscription;
import com.raytheon.uf.common.datadelivery.registry.SharedSubscription;
import com.raytheon.uf.common.datadelivery.registry.SiteSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.registry.handler.IRegistryObjectHandler;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.CollectionUtil;

/**
 * {@link IRegistryObjectHandler} implementation for {@link Subscription}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 17, 2012  1169     djohnson  Initial creation.
 * Sep 24, 2012  1157     mpduff    Change to use InitialPendingSubscription.
 * Oct 17, 2012  726      djohnson  Add {@link #getActiveByDataSetAndProvider}.
 * Apr 05, 2013  1841     djohnson  Add support for shared subscriptions.
 * Apr 09, 2013  1802     bphillip  Using constant values from constants package
 *                                  instead of RegistryUtil
 * May 21, 2013  2020     mpduff    Rename UserSubscription to SiteSubscription.
 * May 28, 2013  1650     djohnson  Add getByNames.
 * May 29, 2013  1650     djohnson  Fix ability to delete multiple types of
 *                                  subscriptions at once.
 * May 31, 2013  1650     djohnson  Fix ability to get shared subscriptions by
 *                                  id.
 * Sep 11, 2013  2352     mpduff    Add siteId to getSubscribedToDataSetNames
 *                                  method.
 * Jan 20, 2014  2538     mpduff    Added AdhocSubscriptionHandler.
 * Jan 29, 2014  2636     mpduff    Scheduling refactor.
 * Mar 31, 2014  2889     dhladky   Added username for notification center
 *                                  tracking.
 * May 22, 2014  2808     dhladky   Added sorting by priority as default.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Apr 03, 2018  7240     tjensen   Added getByDataSetAndProviderForSite
 *
 * </pre>
 *
 * @author djohnson
 */
@SuppressWarnings("rawtypes")
public class SubscriptionHandler
        implements IBaseSubscriptionHandler<Subscription> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SubscriptionHandler.class);

    private final ISiteSubscriptionHandler siteSubscriptionHandler;

    private final ISharedSubscriptionHandler sharedSubscriptionHandler;

    private final IAdhocSubscriptionHandler adhocSubscriptionHandler;

    /**
     * Constructor.
     *
     * @param siteSubscriptionHandler
     *            the user subscription handler
     * @param sharedSubscriptionHandler
     *            the shared subscription handler
     */
    public SubscriptionHandler(ISiteSubscriptionHandler siteSubscriptionHandler,
            ISharedSubscriptionHandler sharedSubscriptionHandler,
            IAdhocSubscriptionHandler adhocSubscriptionHandler) {
        this.siteSubscriptionHandler = siteSubscriptionHandler;
        this.sharedSubscriptionHandler = sharedSubscriptionHandler;
        this.adhocSubscriptionHandler = adhocSubscriptionHandler;
    }

    /**
     * Retrieve a subscription that a {@link PendingSubscription} is associated
     * to.
     *
     * @param pending
     *            the pending subscription
     * @return the subscription, or null if not found
     * @throws RegistryHandlerException
     */
    public Subscription getByPendingSubscription(PendingSubscription pending)
            throws RegistryHandlerException {
        if (pending instanceof PendingSiteSubscription) {
            return siteSubscriptionHandler.getByPendingSubscription(pending);
        }
        return sharedSubscriptionHandler.getByPendingSubscription(pending);
    }

    /**
     * Retrieve a subscription that a {@link PendingSubscription} is associated
     * to by the pending subscription's id.
     *
     * @param id
     *            the pending subscription id
     * @return the subscription, or null if not found
     * @throws RegistryHandlerException
     */
    public Subscription getByPendingSubscriptionId(final String id)
            throws RegistryHandlerException {
        Subscription value = siteSubscriptionHandler.getById(id);
        if (value == null) {
            value = sharedSubscriptionHandler.getById(id);
        }
        return value;
    }

    @Override
    public void deleteByIds(String username, List<String> ids)
            throws RegistryHandlerException {
        siteSubscriptionHandler.deleteByIds(username, ids);
        sharedSubscriptionHandler.deleteByIds(username, ids);
    }

    /**
     * Retrieve active subscriptions for the dataset name and provider.
     *
     * @param dataSetName
     *            the dataset name
     * @param providerName
     *            the provider name
     */
    public List<Subscription> getActiveByDataSetAndProvider(String dataSetName,
            String providerName) throws RegistryHandlerException {
        List<Subscription> subs = new ArrayList<>();
        subs.addAll(siteSubscriptionHandler
                .getActiveByDataSetAndProvider(dataSetName, providerName));
        subs.addAll(sharedSubscriptionHandler
                .getActiveByDataSetAndProvider(dataSetName, providerName));
        Collections.sort(subs);
        return subs;
    }

    /**
     * Retrieve active subscriptions for the dataset name and provider.
     *
     * @param dataSetName
     *            the dataset name
     * @param providerName
     *            the provider name
     */
    public List<Subscription> getByDataSetAndProviderForSite(String dataSetName,
            String providerName, String officeID)
            throws RegistryHandlerException {
        List<Subscription> subs = new ArrayList<>();
        subs.addAll(siteSubscriptionHandler.getByDataSetAndProviderForSite(
                dataSetName, providerName, officeID));
        subs.addAll(sharedSubscriptionHandler
                .getByDataSetAndProvider(dataSetName, providerName));
        Collections.sort(subs);
        return subs;
    }

    @Override
    public Subscription getByName(String name) throws RegistryHandlerException {
        Subscription value = siteSubscriptionHandler.getByName(name);
        if (value == null) {
            value = sharedSubscriptionHandler.getByName(name);
        }
        return value;
    }

    @Override
    public List<Subscription> getByNames(Collection<String> names)
            throws RegistryHandlerException {
        List<Subscription> subs = new ArrayList<>();
        subs.addAll(siteSubscriptionHandler.getByNames(names));
        subs.addAll(sharedSubscriptionHandler.getByNames(names));
        Collections.sort(subs);
        return subs;
    }

    @Override
    public List<Subscription> getByOwner(String owner)
            throws RegistryHandlerException {
        List<Subscription> subs = new ArrayList<>();
        subs.addAll(siteSubscriptionHandler.getByOwner(owner));
        subs.addAll(sharedSubscriptionHandler.getByOwner(owner));
        Collections.sort(subs);
        return subs;
    }

    @Override
    public List<Subscription> getByGroupName(String group)
            throws RegistryHandlerException {
        List<Subscription> subs = new ArrayList<>();
        subs.addAll(siteSubscriptionHandler.getByGroupName(group));
        subs.addAll(sharedSubscriptionHandler.getByGroupName(group));
        Collections.sort(subs);
        return subs;
    }

    @Override
    public List<Subscription> getByFilters(String group, String officeId)
            throws RegistryHandlerException {
        List<Subscription> subs = new ArrayList<>();
        subs.addAll(siteSubscriptionHandler.getByFilters(group, officeId));
        subs.addAll(sharedSubscriptionHandler.getByFilters(group, officeId));
        Collections.sort(subs);
        return subs;
    }

    @Override
    public Set<String> getSubscribedToDataSetNames(String siteId)
            throws RegistryHandlerException {
        Set<String> set = new HashSet<>();
        set.addAll(siteSubscriptionHandler.getSubscribedToDataSetNames(siteId));
        set.addAll(
                sharedSubscriptionHandler.getSubscribedToDataSetNames(siteId));

        return set;
    }

    @Override
    public List<Subscription> getActive() throws RegistryHandlerException {
        List<Subscription> subs = new ArrayList<>();
        subs.addAll(siteSubscriptionHandler.getActive());
        subs.addAll(sharedSubscriptionHandler.getActive());
        Collections.sort(subs);

        return subs;
    }

    @Override
    public List<Subscription> getActiveForRoute(Network route)
            throws RegistryHandlerException {
        List<Subscription> subs = new ArrayList<>();
        subs.addAll(siteSubscriptionHandler.getActiveForRoute(route));
        subs.addAll(sharedSubscriptionHandler.getActiveForRoute(route));
        Collections.sort(subs);

        return subs;
    }

    @Override
    public Map<Network, List<Subscription>> getActiveForRoutes(
            Network... routes) throws RegistryHandlerException {
        Map<Network, List<Subscription>> returnMap = new HashMap<>();
        Map<Network, List<SiteSubscription>> subMap = siteSubscriptionHandler
                .getActiveForRoutes(routes);
        returnMap.putAll(
                (Map<? extends Network, ? extends List<Subscription>>) subMap);

        Map<Network, List<SharedSubscription>> sharedSubMap = sharedSubscriptionHandler
                .getActiveForRoutes(routes);
        // Check for existing networks and add to them if they exist
        for (Map.Entry<Network, List<SharedSubscription>> entry : sharedSubMap
                .entrySet()) {
            Network key = entry.getKey();
            if (returnMap.containsKey(key)) {
                returnMap.get(key).addAll(entry.getValue());
            } else {
                List<SharedSubscription> sharedList = entry.getValue();

                returnMap.put(key,
                        new ArrayList<Subscription>(sharedList.size()));
                returnMap.get(key).addAll(sharedList);
            }
        }

        return returnMap;
    }

    @Override
    public Subscription getById(String id) throws RegistryHandlerException {
        Subscription value = null;
        try {
            value = siteSubscriptionHandler.getById(id);
        } catch (RegistryHandlerException e) {
            if (e.getCause() instanceof ClassCastException) {
                // This will happen for shared subscriptions
                if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                    statusHandler
                            .handle(Priority.DEBUG,
                                    "Registry object with id [" + id
                                            + "] is not a site subscription.",
                                    e);
                }
            }
        }
        if (value == null) {
            value = sharedSubscriptionHandler.getById(id);
        }
        return value;
    }

    @Override
    public List<Subscription> getAll() throws RegistryHandlerException {
        List<Subscription> subs = new ArrayList<>();
        subs.addAll(siteSubscriptionHandler.getAll());
        subs.addAll(sharedSubscriptionHandler.getAll());
        Collections.sort(subs);
        return subs;
    }

    @Override
    public void store(String username, Subscription obj)
            throws RegistryHandlerException {
        if (obj instanceof SiteSubscription) {
            siteSubscriptionHandler.store(username, (SiteSubscription) obj);
        } else {
            sharedSubscriptionHandler.store(username, (SharedSubscription) obj);
        }
    }

    @Override
    public void update(String username, Subscription obj)
            throws RegistryHandlerException {
        if (obj instanceof SiteSubscription) {
            siteSubscriptionHandler.update(username, (SiteSubscription) obj);
        } else {
            sharedSubscriptionHandler.update(username,
                    (SharedSubscription) obj);
        }
    }

    @Override
    public void delete(Subscription obj) throws RegistryHandlerException {
        if (obj instanceof SiteSubscription) {
            siteSubscriptionHandler.delete((SiteSubscription) obj);
        } else {
            sharedSubscriptionHandler.delete((SharedSubscription) obj);
        }
    }

    @Override
    public void deleteById(String username, String registryId)
            throws RegistryHandlerException {
        siteSubscriptionHandler.deleteById(username, registryId);
        sharedSubscriptionHandler.deleteById(username, registryId);
    }

    @Override
    public void delete(String username, Subscription obj)
            throws RegistryHandlerException {
        if (obj instanceof SiteSubscription) {
            siteSubscriptionHandler.delete(username, (SiteSubscription) obj);
        } else {
            sharedSubscriptionHandler.delete(username,
                    (SharedSubscription) obj);
        }
    }

    @Override
    public void delete(Collection<Subscription> objects)
            throws RegistryHandlerException {
        if (!CollectionUtil.isNullOrEmpty(objects)) {
            final Collection<SiteSubscription> siteSubscriptions = new ArrayList<>();
            final Collection<SharedSubscription> sharedSubscriptions = new ArrayList<>();
            for (Subscription sub : objects) {
                if (sub instanceof SiteSubscription) {
                    siteSubscriptions.add((SiteSubscription) sub);
                } else if (sub instanceof SharedSubscription) {
                    sharedSubscriptions.add((SharedSubscription) sub);
                } else {
                    throw new RegistryHandlerException(
                            new IllegalArgumentException(
                                    "Unable to delete subscription of type ["
                                            + sub.getClass().getName()
                                            + "].  Did you add a new subscription type?"));
                }
            }

            if (!siteSubscriptions.isEmpty()) {
                siteSubscriptionHandler.delete(siteSubscriptions);
            }

            if (!sharedSubscriptions.isEmpty()) {
                sharedSubscriptionHandler.delete(sharedSubscriptions);
            }
        }
    }

    @Override
    public void delete(String username, Collection<Subscription> objects)
            throws RegistryHandlerException {
        if (!CollectionUtil.isNullOrEmpty(objects)) {
            final Collection<SiteSubscription> siteSubscriptions = new ArrayList<>();
            final Collection<SharedSubscription> sharedSubscriptions = new ArrayList<>();
            for (Subscription sub : objects) {
                if (sub instanceof SiteSubscription) {
                    siteSubscriptions.add((SiteSubscription) sub);
                } else if (sub instanceof SharedSubscription) {
                    sharedSubscriptions.add((SharedSubscription) sub);
                } else {
                    throw new RegistryHandlerException(
                            new IllegalArgumentException(
                                    "Unable to delete subscription of type ["
                                            + sub.getClass().getName()
                                            + "].  Did you add a new subscription type?"));
                }
            }

            if (!siteSubscriptions.isEmpty()) {
                siteSubscriptionHandler.delete(username, siteSubscriptions);
            }

            if (!sharedSubscriptions.isEmpty()) {
                sharedSubscriptionHandler.delete(username, sharedSubscriptions);
            }
        }
    }

    /**
     * @return the siteSubscriptionHandler
     */
    public ISiteSubscriptionHandler getSiteSubscriptionHandler() {
        return siteSubscriptionHandler;
    }

    /**
     * @return the sharedSubscriptionHandler
     */
    public ISharedSubscriptionHandler getSharedSubscriptionHandler() {
        return sharedSubscriptionHandler;
    }

    /**
     * @return the adhocSubscriptionHandler
     */
    public IAdhocSubscriptionHandler getAdhocSubscriptionHandler() {
        return adhocSubscriptionHandler;
    }

}
