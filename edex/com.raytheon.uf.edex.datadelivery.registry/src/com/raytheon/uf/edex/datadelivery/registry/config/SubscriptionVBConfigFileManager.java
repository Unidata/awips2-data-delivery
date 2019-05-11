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
package com.raytheon.uf.edex.datadelivery.registry.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.raytheon.uf.common.datadelivery.registry.DataDeliveryRegistryObjectTypes;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.SiteSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.handlers.SubscriptionHandler;
import com.raytheon.uf.common.registry.event.RegistryEvent;
import com.raytheon.uf.common.registry.event.RemoveRegistryEvent;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.edex.registry.ebxml.util.RegistryIdUtil;

import oasis.names.tc.ebxml.regrep.xsd.rim.v4.RegistryObjectType;

/**
 * Manages localized VolumeBrowser config files based on Subscription registry
 * events.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 06, 2017 6355       nabowle     Initial creation
 * Apr 03, 2018 7240       tjensen     Change to support files by dataset
 *
 * </pre>
 *
 * @author nabowle
 */
public class SubscriptionVBConfigFileManager {

    private static final Logger statusHandler = LoggerFactory
            .getLogger(SubscriptionVBConfigFileManager.class);

    /** Stand-in for a removed sub that would otherwise be null. */
    private static final Subscription<?, ?> REMOVED_SUB = new SiteSubscription<>();

    /** handler for Subscription objects from the registry **/
    private final SubscriptionHandler subscriptionHandler;

    /**
     * Constructor.
     */
    public SubscriptionVBConfigFileManager(
            SubscriptionHandler subscriptionHandler) {
        super();
        this.subscriptionHandler = subscriptionHandler;
    }

    /**
     * Handles registry events. This single, non-concurrent handling point
     * prevents conflicting attempts to manage the files.
     *
     * @param event
     */
    @Subscribe
    public void handleRegistryEvents(RegistryEvent event) {
        Subscription<?, ?> affectedSub;
        if ((affectedSub = getAffectedSub(event)) != null
                && event.getAction() != null) {
            switch (event.getAction()) {
            case DELETE:
                subscriptionRemoved((RemoveRegistryEvent) event, affectedSub);
                break;
            case INSERT:
                subscriptionInserted(event, affectedSub);
                break;
            case UPDATE:
                subscriptionUpdated(event, affectedSub);
                break;
            default:
                statusHandler.warn(
                        "Unable to handle action of type " + event.getAction());
                break;
            }
        }
    }

    private void subscriptionInserted(RegistryEvent event,
            Subscription<?, ?> sub) {
        if (!sub.getOfficeIDs().contains(RegistryIdUtil.getId())) {
            return;
        }

        SubscriptionVBConfigFileUtil.updateDataSetFiles(sub.getDataSetName(),
                sub.getProvider());
    }

    private void subscriptionUpdated(RegistryEvent event,
            Subscription<?, ?> sub) {
        if (DataDeliveryRegistryObjectTypes.SHARED_SUBSCRIPTION
                .equals(event.getObjectType())
                && !sub.getOfficeIDs().contains(RegistryIdUtil.getId())) {
            /*
             * We may have been part of the Shared Subscription previously, so
             * remove the configs if they exist.
             */
            siteRemoved(event, sub);
        } else {
            subscriptionInserted(event, sub);
        }
    }

    private Subscription<?, ?> getAffectedSub(RegistryEvent event) {
        Subscription<?, ?> subscription = null;
        String objectType = event.getObjectType();
        if ((DataDeliveryRegistryObjectTypes.SHARED_SUBSCRIPTION
                .equals(objectType))
                || (DataDeliveryRegistryObjectTypes.SITE_SUBSCRIPTION
                        .equals(objectType))
                || (DataDeliveryRegistryObjectTypes.ADHOC_SUBSCRIPTION)
                        .equals(objectType)) {
            try {
                subscription = subscriptionHandler.getById(event.getId());
            } catch (RegistryHandlerException e) {
                statusHandler.error("Error attempting to retrieve Subscription["
                        + event.getId() + "] from Registry.", e);
            }

            if (subscription == null) {
                // Ignore null subscriptions, unless it's a DELETE action.
                if (RegistryEvent.Action.DELETE.equals(event.getAction())) {
                    /*
                     * fake finding a relevant sub in the case of a removal,
                     * mostly to simplify future logic.
                     */
                    subscription = REMOVED_SUB;
                }
            } else if (!DataType.GRID.equals(subscription.getDataSetType())) {
                // Ignore Non-GRID subscriptions.
                subscription = null;
            } else if (!subscription.getOfficeIDs()
                    .contains(RegistryIdUtil.getId())
                    && (RegistryEvent.Action.INSERT.equals(event.getAction())
                            || DataDeliveryRegistryObjectTypes.SITE_SUBSCRIPTION
                                    .equals(objectType))) {
                /*
                 * Ignore INSERT events for any subscriptions that do not affect
                 * us, or UPDATES to Site Subscriptions that do not affect us.
                 * UPDATES to Shared Subscriptions may have removed us from the
                 * subscription.
                 */
                subscription = null;
            }
        }
        return subscription;
    }

    private void subscriptionRemoved(RemoveRegistryEvent event,
            Subscription<?, ?> sub) {
        /*
         * Changing a SiteSubscription to a SharedSubscription will cause an
         * insert of the SharedSub before the removal of the SiteSub. In cases
         * like this, we want to preserve the files. Likewise, Adhocs with the
         * same name as a recurring sub will be deleted while the recurring sub
         * may remain.
         */
        Subscription<?, ?> existingSub;
        String subName = getSubscriptionName(event);
        try {
            existingSub = this.subscriptionHandler.getByName(subName);
        } catch (RegistryHandlerException e) {
            /*
             * It's easier to force the re-creation of Volume Browser config
             * files and get back to a happy state, than to leave a series of
             * dead-configs that will need manually removed.
             */
            statusHandler.warn(
                    "Unable to determine if a Subscription with the name "
                            + subName
                            + " still exists. The relevant Volume Browser config files will be deleted.",
                    e);
            existingSub = null;
        }

        if (existingSub != null && existingSub.getOfficeIDs()
                .contains(RegistryIdUtil.getId())) {
            return;
        }

        updateConfigFiles(event);

    }

    private void updateConfigFiles(RemoveRegistryEvent event) {
        RegistryObjectType rot = event.getRemovedObject();
        String dsName = rot.getSlotValue("dataSetName");
        String provider = rot.getSlotValue("provider");
        SubscriptionVBConfigFileUtil.updateDataSetFiles(dsName, provider);
    }

    private void siteRemoved(RegistryEvent event, Subscription<?, ?> sub) {
        SubscriptionVBConfigFileUtil.updateDataSetFiles(sub.getDataSetName(),
                sub.getProvider());
    }

    /**
     * Get the subscription name from a RemoveRegistryEvent.
     *
     * @param event
     * @return
     */
    private String getSubscriptionName(RemoveRegistryEvent event) {
        RegistryObjectType rot = event.getRemovedObject();
        String name = rot.getSlotValue("name");
        return name;
    }

    public SubscriptionHandler getSubscriptionHandler() {
        return subscriptionHandler;
    }

}
