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
package com.raytheon.uf.common.datadelivery.service;

import com.raytheon.uf.common.datadelivery.registry.InitialPendingSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.request.DataDeliveryConstants;
import com.raytheon.uf.common.serialization.comm.RequestRouter;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Sends the notification to the server for processing about subscription
 * events.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 04, 2013 1441       djohnson     Initial creation
 * Jan 17, 2013 1501       djohnson     Route to datadelivery.
 * Jan 21, 2013 1501       djohnson     Include subscription on all requests.
 * Oct 15, 2014 3664       ccody        Add Subscription unscheduled notification method
 * Oct 28, 2014 2748       ccody        Remove incorrect update notification for completion of Async operations
 * Nov 03, 2014 2414       dhladky      Brought back update.  turns out we still need it.
 * Mar 16, 2016 3919       tjensen      Cleanup unneeded interfaces
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class SendToServerSubscriptionNotificationService {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SendToServerSubscriptionNotificationService.class);

    /**
     * Send a notification that a subscription was created.
     * 
     * @param subscription
     *            the subscription
     * @param username
     *            the username
     */
    public void sendCreatedSubscriptionNotification(Subscription subscription,
            String username) {
        BaseSubscriptionNotificationRequest<Subscription> req = new SubscriptionNotificationRequest();
        req.setUserId(username);
        req.setCategory("Subscription");
        String msg = "Subscription " + subscription.getName()
                + " has been created.";
        req.setMessage(msg);
        req.setSubscription(subscription);
        req.setPriority(3);
        sendRequest(req);
    }

    /**
     * Send a notification that pending subscription was created.
     * 
     * @param subscription
     *            the subscription
     * @param username
     *            the username
     */
    public void sendCreatedPendingSubscriptionNotification(
            InitialPendingSubscription subscription, String username) {

        BaseSubscriptionNotificationRequest<InitialPendingSubscription> req = new PendingSubscriptionNotificationRequest();
        req.setUserId(username);
        req.setCategory("Subscription");

        req.setSubscription(subscription);
        req.setPriority(2);
        req.setMessage("Pending Subscription " + subscription.getName()
                + " has been created.");

        sendRequest(req);
    }

    /**
     * Send a notification that a pending subscription was updated.
     * 
     * @param subscription
     *            the subscription
     * @param username
     *            the username
     */
    public void sendUpdatedPendingSubscriptionNotification(
            InitialPendingSubscription subscription, String username) {

        BaseSubscriptionNotificationRequest<InitialPendingSubscription> req = new PendingSubscriptionNotificationRequest();
        req.setUserId(username);
        req.setCategory("Subscription");

        req.setMessage("Edited subscription " + subscription.getName());
        req.setSubscription(subscription);
        req.setPriority(2);

        sendRequest(req);
    }

    /**
     * Send a notification that a subscription update is pending.
     * 
     * @param subscription
     *            the subscription
     * @param username
     *            the username
     */
    public void sendCreatedPendingSubscriptionForSubscriptionNotification(
            InitialPendingSubscription subscription, String username) {
        BaseSubscriptionNotificationRequest<InitialPendingSubscription> req = new PendingSubscriptionNotificationRequest();
        req.setUserId(username);
        req.setCategory("Subscription");

        req.setMessage("Edited subscription " + subscription.getName());
        req.setSubscription(subscription);
        req.setPriority(2);

        sendRequest(req);
    }

    /**
     * Send a notification that a pending subscription was approved.
     * 
     * @param subscription
     *            the subscription
     * @param username
     *            the username
     */
    public void sendApprovedPendingSubscriptionNotification(
            InitialPendingSubscription subscription, String username) {
        ApprovedPendingSubscriptionNotificationRequest req = new ApprovedPendingSubscriptionNotificationRequest();
        req.setMessage("Approved subscription " + subscription.getName());
        req.setUserId(username);
        req.setCategory("Subscription Approved");
        req.setPriority(2);
        req.setSubscription(subscription);

        sendRequest(req);
    }

    /**
     * Send a notification that a pending subscription was denied.
     * 
     * @param subscription
     *            the subscription
     * @param username
     *            the username
     * @param denyMessage
     *            the reason for denying
     */
    public void sendDeniedPendingSubscriptionNotification(
            InitialPendingSubscription subscription, String username,
            String denyMessage) {
        DeniedPendingSubscriptionNotificationRequest req = new DeniedPendingSubscriptionNotificationRequest();
        req.setMessage(denyMessage);
        req.setUserId(username);
        req.setCategory("Subscription Approval Denied");
        req.setPriority(2);
        req.setId(subscription.getId());
        req.setSubscription(subscription);

        sendRequest(req);
    }

    /**
     * Send a notification that the subscription was deleted.
     * 
     * @param subscription
     *            the subscription
     * @param username
     *            the username
     */
    public void sendDeletedSubscriptionNotification(Subscription subscription,
            String username) {
        SubscriptionNotificationRequest req = new SubscriptionNotificationRequest();
        req.setUserId(username);
        req.setCategory("Subscription");
        req.setPriority(3);
        req.setMessage(subscription.getName() + " Deleted");
        subscription.setDeleted(true);
        req.setSubscription(subscription);

        sendRequest(req);
    }

    /**
     * Send a notification that the subscription was updated.
     * 
     * @param subscription
     *            the subscription
     * @param username
     *            the username
     */
    public void sendUpdatedSubscriptionNotification(Subscription subscription,
            String username) {
        SubscriptionNotificationRequest req = new SubscriptionNotificationRequest();
        req.setUserId(username);
        req.setCategory("Subscription");
        req.setPriority(3);
        req.setMessage(subscription.getName() + " Updated");
        subscription.setDeleted(true);
        req.setSubscription(subscription);

        sendRequest(req);

    }

    /**
     * Send a notification that the subscription was activated.
     * 
     * @param subscription
     *            the subscription
     * @param username
     *            the username
     */
    public void sendSubscriptionActivatedNotification(
            Subscription subscription, String username) {
        SubscriptionNotificationRequest req = new SubscriptionNotificationRequest();
        req.setUserId(username);
        req.setCategory("Subscription");
        req.setPriority(3);
        req.setMessage(subscription.getName() + " Activated");
        req.setSubscription(subscription);

        sendRequest(req);
    }

    /**
     * Send a notification that the subscription was deactivated.
     * 
     * @param subscription
     *            the subscription
     * @param username
     *            the username
     */
    public void sendSubscriptionDeactivatedNotification(
            Subscription subscription, String username) {
        SubscriptionNotificationRequest req = new SubscriptionNotificationRequest();
        req.setUserId(username);
        req.setCategory("Subscription");
        req.setPriority(3);
        req.setMessage(subscription.getName() + " Deactivated");
        req.setSubscription(subscription);

        sendRequest(req);
    }

    /**
     * Send a notification that the Subscription Bandwidth latency and
     * scheduling have been changed.
     * 
     * @param subscription
     *            the subscription
     * @param username
     *            the username
     */
    public void sendSubscriptionUnscheduledNotification(
            Subscription subscription, String message, String username) {

        SubscriptionNotificationRequest req = new SubscriptionNotificationRequest();
        req.setUserId(username);
        req.setCategory("Subscription");
        req.setPriority(3);
        req.setMessage(subscription.getName() + message);
        req.setSubscription(subscription);

        sendRequest(req);
    }

    /**
     * Send the notification request.
     * 
     * @param req
     */
    private void sendRequest(BaseSubscriptionNotificationRequest<?> req) {
        try {
            RequestRouter
                    .route(req, DataDeliveryConstants.DATA_DELIVERY_SERVER);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
    }

}
