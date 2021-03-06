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
package com.raytheon.uf.viz.datadelivery.subscription;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.eclipse.swt.widgets.Shell;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.raytheon.uf.common.auth.AuthException;
import com.raytheon.uf.common.auth.req.IPermissionsService;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.datadelivery.bandwidth.BandwidthService;
import com.raytheon.uf.common.datadelivery.bandwidth.ProposeScheduleResponse;
import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.InitialPendingSubscription;
import com.raytheon.uf.common.datadelivery.registry.PointTime;
import com.raytheon.uf.common.datadelivery.registry.RecurringSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription.SubscriptionState;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.registry.handlers.PendingSubscriptionHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.SubscriptionHandler;
import com.raytheon.uf.common.datadelivery.request.DataDeliveryConstants;
import com.raytheon.uf.common.datadelivery.service.SendToServerSubscriptionNotificationService;
import com.raytheon.uf.common.datadelivery.service.subscription.SubscriptionOverlapRequest;
import com.raytheon.uf.common.datadelivery.service.subscription.SubscriptionOverlapResponse;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.serialization.comm.RequestRouter;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.auth.UserController;
import com.raytheon.uf.viz.datadelivery.actions.SubscriptionManagerAction;
import com.raytheon.uf.viz.datadelivery.system.SystemRuleManager;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;

/**
 * Services for working with subscriptions.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Nov 07, 2012  1286     djohnson  Initial creation
 * Nov 20, 2012  1286     djohnson  Use propose schedule methods to see effects
 *                                  of subscription scheduling.
 * Nov 28, 2012  1286     djohnson  Add more notification methods.
 * Dec 11, 2012  1404     mpduff    Add message to
 *                                  sendDeletedSubscriptionNotification.
 * Dec 11, 2012  1403     djohnson  Adhoc subscriptions no longer go to the
 *                                  registry.
 * Dec 18, 2012  1443     bgonzale  Open force apply prompt pop-up on the UI
 *                                  thread.
 * Dec 20, 2012  1413     bgonzale  Added new pending approve and denied request
 *                                  and responses.
 * Jan 04, 2013  1441     djohnson  Separated out notification methods into
 *                                  their own service.
 * Jan 28, 2013  1530     djohnson  Reset unscheduled flag with each update.
 * Mar 29, 2013  1841     djohnson  Subscription is now UserSubscription.
 * May 14, 2013  2000     djohnson  Check for subscription overlap/duplication.
 * May 23, 2013  1650     djohnson  Move out some presentation logic to
 *                                  DisplayForceApplyPromptDialog.
 * Jun 12, 2013  2038     djohnson  Launch subscription manager on the UI
 *                                  thread.
 * Jul 18, 2013  1653     mpduff    Add SubscriptionStatusSummary.
 * Jul 26, 2013  2232     mpduff    Refactored Data Delivery permissions.
 * Sep 25, 2013  1797     dhladky   separated time from gridded time
 * Oct 12, 2013  2460     dhladky   restored adhoc subscriptions to registry
 *                                  storage.
 * Oct 22, 2013  2292     mpduff    Removed subscriptionOverlapService.
 * Nov 07, 2013  2291     skorolev  Used showText() method for "Shared
 *                                  Subscription" message.
 * Jan 26, 2014  2259     mpduff    Turn off subs to be deactivated.
 * Feb 04, 2014  2677     mpduff    Don't do overlap checks when deactivating
 *                                  subs.
 * Mar 31, 2014  2889     dhladky   Added username for notification center
 *                                  tracking.
 * Oct 15, 2014  3664     ccody     Added notification for scheduling status of
 *                                  subscriptions changes
 * Nov 19, 2014  3852     dhladky   Resurrected the Unscheduled state.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Feb 21, 2017  746      bsteffen  Do not deactivate after increasing latency.
 * Apr 10, 2017  6074     mapeters  Activate after increasing latency.
 * Apr 25, 2017  1045     tjensen   Update for moving datasets
 * Jun 20, 2017  6299     tgurney   Remove IProposeScheduleResponse
 * Sep 13, 2017  6431     tgurney   Fix save of pending subscription
 *
 * </pre>
 *
 * @author djohnson
 */

public class SubscriptionService {
    private static final String PENDING_SUBSCRIPTION_AWAITING_APPROVAL = "The subscription is awaiting approval.\n\n"
            + "A notification message will be generated upon approval.";

    private static final String OVERLAPPING_SUBSCRIPTIONS = "The following subscriptions overlap with this one "
            + "and are candidates for a shared subscription: ";

    private static final String DUPLICATE_SUBSCRIPTIONS = "This subscription is completely fulfilled by ";

    /**
     * Uses an SWT dialog to shell a force apply prompt.
     */
    private static class DisplayForceApplyPrompt {

        private ForceApplyPromptResponse forceApplyPromptResponse = ForceApplyPromptResponse.CANCEL;

        /**
         * Display the force apply prompt.
         *
         * @param configuration
         *            the configuration
         *
         * @return the response
         */
        public ForceApplyPromptResponse displayForceApplyPrompt(
                ForceApplyPromptConfiguration configuration) {
            DisplayForceApplyPromptDialog dlg = new DisplayForceApplyPromptDialog(
                    configuration);
            forceApplyPromptResponse = (ForceApplyPromptResponse) dlg.open();
            return forceApplyPromptResponse;
        }

        /**
         * get the response from the last call to the displayForceApplyPrompt
         * method.
         */
        public ForceApplyPromptResponse getForceApplyPromptResponse() {
            return forceApplyPromptResponse;
        }

        /**
         * Display a popup message to the user.
         *
         * @param displayTextStrategy
         * @param message
         */
        public static void displayMessage(
                IForceApplyPromptDisplayText displayTextStrategy,
                final String message) {
            final Shell shell = displayTextStrategy.getShell();
            shell.getDisplay().syncExec(new Runnable() {
                @Override
                public void run() {
                    DataDeliveryUtils.showText(shell, "Shared Subscription",
                            message);
                }
            });
        }
    }

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SubscriptionService.class);

    @VisibleForTesting
    public static final String TITLE = "Subscription";

    private final SendToServerSubscriptionNotificationService notificationService;

    private final BandwidthService bandwidthService;

    private final IPermissionsService permissionsService;

    private final DisplayForceApplyPrompt forceApplyPrompt;

    /**
     * Result class used internally to denote whether the user should be
     * prompted, and any result messages.
     */
    private final class ProposeResult {
        private final boolean promptUser;

        private final ForceApplyPromptConfiguration config;

        private ProposeResult(boolean promptUser,
                ForceApplyPromptConfiguration config) {
            this.promptUser = promptUser;
            this.config = config;
        }
    }

    /**
     * A service interaction.
     */
    private interface ServiceInteraction extends Callable<String> {
        // Throws only one exception
        @Override
        String call() throws RegistryHandlerException;
    }

    /**
     * Enumeration of force apply responses.
     */
    public static enum ForceApplyPromptResponse {
        CANCEL, INCREASE_LATENCY, EDIT_SUBSCRIPTIONS, FORCE_APPLY_DEACTIVATED, FORCE_APPLY_UNSCHEDULED;
    }

    /**
     * Interface that must be implemented by classes that will be showing a
     * force apply prompt message.
     */
    public static interface IForceApplyPromptDisplayText {
        /**
         * Retrieve the display text that will be displayed for each option.
         *
         * @param option
         *            the option
         * @param requiredLatency
         *            the required latency that would be required to schedule
         *            the item(s)
         * @param subscription
         *            the subscription that would require the increased latency,
         *            or null if this is a multi-subscription operation
         * @param wouldBeUnscheduledSubscriptions
         *            the subscription names that would be unscheduled
         * @return the display text, or null if the option should not be
         *         displayed
         */
        String getOptionDisplayText(ForceApplyPromptResponse option,
                int requiredLatency, Subscription subscription,
                Set<String> wouldBeUnscheduledSubscriptions);

        /**
         * Get the shell to use.
         *
         * @return the shell
         */
        Shell getShell();
    }

    /**
     * Private constructor. Use
     * {@link #newInstance(SendToServerSubscriptionNotificationService)}
     * instead.
     *
     * @param notificationService
     *            the subscription notification service
     * @param bandwidthService
     *            the bandwidth service
     */
    @SuppressWarnings("rawtypes")
    @VisibleForTesting
    SubscriptionService(
            SendToServerSubscriptionNotificationService notificationService,
            BandwidthService bandwidthService,
            IPermissionsService permissionsService,
            DisplayForceApplyPrompt displayForceApplyPrompt) {
        this.notificationService = notificationService;
        this.bandwidthService = bandwidthService;
        this.permissionsService = permissionsService;
        this.forceApplyPrompt = displayForceApplyPrompt;
    }

    /**
     * Factory method to create an {@link ISubscriptionService}. Allows for
     * changing to use sub-classes or different implementations later, without
     * tying specifically to the implementation class.
     *
     * @param notificationService
     * @param bandwidthService
     * @param permissionsService
     * @param
     * @return the subscription service
     */
    public static SubscriptionService newInstance(
            SendToServerSubscriptionNotificationService notificationService,
            BandwidthService bandwidthService,
            IPermissionsService permissionsService) {
        return new SubscriptionService(notificationService, bandwidthService,
                permissionsService, new DisplayForceApplyPrompt());
    }

    /**
     * Store the subscription.
     *
     * @param subscription
     *            the subscription to store
     * @param dataSet
     * @param displayTextStrategy
     * @return the result object
     * @throws RegistryHandlerException
     */
    public SubscriptionServiceResult store(final String username,
            final Subscription subscription,
            IForceApplyPromptDisplayText displayTextStrategy)
            throws RegistryHandlerException {

        final List<Subscription> subscriptions = Arrays.asList(subscription);
        final String successMessage = "Subscription " + subscription.getName()
                + " has been created.";
        final ServiceInteraction action = new ServiceInteraction() {
            @Override
            public String call() throws RegistryHandlerException {
                DataDeliveryHandlers.getSubscriptionHandler().store(username,
                        subscription);
                return successMessage;
            }
        };

        SubscriptionServiceResult result = performAction(username,
                subscriptions, action, displayTextStrategy);

        if (!result.isAllowFurtherEditing()) {
            result.setSubscriptionStatusSummary(bandwidthService
                    .getSubscriptionStatusSummary(subscription));
        }

        return result;
    }

    /**
     * Update the subscription.
     *
     * @param subscription
     *            the subscription to update
     * @param displayTextStrategy
     * @return the result object
     */
    public SubscriptionServiceResult update(final String username,
            final Subscription subscription,
            IForceApplyPromptDisplayText displayTextStrategy)
            throws RegistryHandlerException {

        final List<Subscription> subscriptions = Arrays.asList(subscription);
        final String successMessage = "Subscription " + subscription.getName()
                + " has been updated.";
        final ServiceInteraction action = new ServiceInteraction() {
            @Override
            public String call() throws RegistryHandlerException {
                subscription.setUnscheduled(false);
                DataDeliveryHandlers.getSubscriptionHandler().update(username,
                        subscription);
                return successMessage;
            }
        };

        return performAction(username, subscriptions, action,
                displayTextStrategy);
    }

    /**
     * Update the subscriptions.
     *
     * @param subscriptions
     *            the subscriptions to update
     * @param displayTextStrategy
     * @return the result object
     * @throws RegistryHandlerException
     */
    public SubscriptionServiceResult update(final String username,
            final List<Subscription> subs,
            IForceApplyPromptDisplayText displayTextStrategy)
            throws RegistryHandlerException {

        final String successMessage = "The subscriptions have been updated.";
        final ServiceInteraction action = new ServiceInteraction() {
            @Override
            public String call() throws RegistryHandlerException {
                for (Subscription sub : subs) {
                    sub.setUnscheduled(false);
                    DataDeliveryHandlers.getSubscriptionHandler()
                            .update(username, sub);
                }
                return successMessage;
            }
        };

        return performAction(username, subs, action, displayTextStrategy);
    }

    /**
     * Update the subscriptions, checking for an existing pending change
     * already.
     *
     * @param subscriptions
     * @param displayTextStrategy
     * @return the result
     * @throws RegistryHandlerException
     */
    public SubscriptionServiceResult updateWithPendingCheck(String username,
            final List<Subscription> subscriptions,
            IForceApplyPromptDisplayText displayTextStrategy)
            throws RegistryHandlerException {
        final ServiceInteraction action = new ServiceInteraction() {

            @Override
            public String call() throws RegistryHandlerException {
                final SortedSet<String> alreadyPending = new TreeSet<>();
                final SortedSet<String> pendingCreated = new TreeSet<>();
                final SortedSet<String> unableToUpdate = new TreeSet<>();
                final StringBuilder successMessage = new StringBuilder(
                        "The subscriptions have been updated.");

                final PendingSubscriptionHandler pendingSubscriptionHandler = DataDeliveryHandlers
                        .getPendingSubscriptionHandler();

                for (Subscription subscription : subscriptions) {

                    try {
                        InitialPendingSubscription pending = pendingSubscriptionHandler
                                .getBySubscription(subscription);

                        if (pending != null) {
                            alreadyPending.add(subscription.getName());
                            continue;
                        }
                    } catch (RegistryHandlerException e1) {
                        statusHandler.handle(Priority.INFO,
                                DataDeliveryUtils.UNABLE_TO_RETRIEVE_PENDING_SUBSCRIPTIONS,
                                e1);
                        unableToUpdate.add(subscription.getName());
                        continue;
                    }

                    IUser user = UserController.getUserObject();
                    final String username = user.uniqueId().toString();

                    try {
                        if (!(permissionsService instanceof RequestFromServerPermissionsService)) {
                            throw new RegistryHandlerException(
                                    "Invalid Handler " + permissionsService
                                            .getClass().toString());
                        }
                        boolean authorized = ((RequestFromServerPermissionsService) permissionsService)
                                .checkPermissionToChangeSubscription(user,
                                        PENDING_SUBSCRIPTION_AWAITING_APPROVAL,
                                        subscription)
                                .isAuthorized();
                        try {
                            if (authorized) {
                                subscription.setUnscheduled(false);
                                DataDeliveryHandlers.getSubscriptionHandler()
                                        .update(username, subscription);
                            } else {
                                InitialPendingSubscription pendingSub = subscription
                                        .initialPending(username);
                                pendingSub.setChangeReason(
                                        "Group Definition Changed");

                                savePendingSub(pendingSub, username);
                                pendingCreated.add(subscription.getName());
                            }
                        } catch (RegistryHandlerException e1) {
                            statusHandler.handle(Priority.INFO,
                                    DataDeliveryUtils.UNABLE_TO_RETRIEVE_PENDING_SUBSCRIPTIONS,
                                    e1);
                            unableToUpdate.add(subscription.getName());
                            continue;
                        }

                    } catch (AuthException e) {
                        statusHandler.handle(Priority.INFO,
                                e.getLocalizedMessage(), e);
                    }
                }
                appendCollectionPortion(successMessage,
                        "\n\nThe following subscriptions have pending changes awaiting approval:",
                        pendingCreated);

                appendCollectionPortion(successMessage,
                        "\n\nThe following subscriptions already had pending changes and were not modified:",
                        alreadyPending);

                appendCollectionPortion(successMessage,
                        "\n\nThe following subscriptions were unable to be modified:",
                        unableToUpdate);

                return successMessage.toString();
            }
        };

        return performAction(username, subscriptions, action,
                displayTextStrategy);
    }

    /**
     * Store the adhoc subscription.
     *
     * @param subscription
     *            the subscription to store
     * @param display
     *            the display to use to prompt the user
     * @param displayTextStrategy
     * @return the result object
     * @throws RegistryHandlerException
     */
    public SubscriptionServiceResult store(final String username,
            final AdhocSubscription sub,
            IForceApplyPromptDisplayText displayTextStrategy)
            throws RegistryHandlerException {

        final List<Subscription> subscriptions = Arrays
                .<Subscription>asList(sub);
        final String successMessage = "The query was successfully stored.";
        final ServiceInteraction action = new ServiceInteraction() {
            @Override
            public String call() throws RegistryHandlerException {
                DataDeliveryHandlers.getSubscriptionHandler().store(username,
                        sub);
                return successMessage;
            }
        };

        SubscriptionServiceResult result = performAction(username,
                subscriptions, action, displayTextStrategy);
        if (!result.isAllowFurtherEditing()) {
            Date date = bandwidthService.getEstimatedCompletionTime(sub);
            if (date != null) {
                SimpleDateFormat sdf = new SimpleDateFormat(
                        "MM/dd/yyyy HH:mm zzz");
                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                result = new SubscriptionServiceResult(result.getMessage()
                        + "\n\nEstimated completion time:" + sdf.format(date));
            }
        }

        return result;
    }

    /**
     * Performs the action on the subscriptions. If the action would cause
     * subscriptions to be unscheduled, the user is prompted whether or not they
     * would like to continue with the action forcibly. If so, the action is
     * performed and the affected subscriptions are updated to be in the
     * unscheduled state.
     *
     * @param subscriptions
     * @param action
     * @param displayTextStrategy
     * @return the result object
     * @throws RegistryHandlerException
     */
    private SubscriptionServiceResult performAction(String username,
            List<Subscription> subscriptions, ServiceInteraction action,
            final IForceApplyPromptDisplayText displayTextStrategy)
            throws RegistryHandlerException {

        if (subscriptions == null || subscriptions.isEmpty()) {
            return new SubscriptionServiceResult(false,
                    "No subscriptions submitted.");
        }

        /*
         * If activating the subscriptions check for overlaps.
         *
         * Only need to check one because all are being updated the same way.
         */
        if (subscriptions.get(0)
                .getSubscriptionState() == SubscriptionState.ON) {
            SubscriptionOverlapRequest request = new SubscriptionOverlapRequest(
                    subscriptions);

            SubscriptionOverlapResponse response = null;
            try {
                response = (SubscriptionOverlapResponse) RequestRouter.route(
                        request, DataDeliveryConstants.DATA_DELIVERY_SERVER);
                if (response.isDuplicate()) {
                    return new SubscriptionServiceResult(true,
                            StringUtil.createMessage(DUPLICATE_SUBSCRIPTIONS,
                                    response.getSubscriptionNameList()));
                }

                if (response.isOverlap()) {
                    List<String> subNames = response.getSubscriptionNameList();
                    Collections.sort(subNames);
                    forceApplyPrompt.displayMessage(displayTextStrategy,
                            StringUtil.createMessage(OVERLAPPING_SUBSCRIPTIONS,
                                    subNames));
                }
            } catch (Exception e) {
                statusHandler.error("Error checking subscription overlapping",
                        e);
                return new SubscriptionServiceResult(false);
            }
        }

        try {
            final ProposeResult result = proposeScheduleAndAction(subscriptions,
                    action, displayTextStrategy);

            if (result.promptUser) {
                VizApp.runSync(new Runnable() {
                    @Override
                    public void run() {
                        forceApplyPrompt.displayForceApplyPrompt(result.config);
                    }
                });

                StringBuilder notificationSB = new StringBuilder();
                switch (forceApplyPrompt.getForceApplyPromptResponse()) {
                case INCREASE_LATENCY:
                    Subscription sub = subscriptions.get(0);
                    int numSubs = subscriptions.size();
                    if (numSubs > 1) {
                        /*
                         * This shouldn't be able to happen, as the
                         * INCREASE_LATENCY option is currently only displayed
                         * to the user when working with a single subscription.
                         */
                        String msg = "Increase Latency option selected for "
                                + numSubs + " subscriptions. Only subscription "
                                + sub.getName() + " will be modified.";
                        statusHandler.warn(msg);
                    }
                    int oldLatency = sub.getLatencyInMinutes();
                    sub.setLatencyInMinutes(result.config.requiredLatency);
                    notificationSB.append(
                            " Bandwidth Latency has been modified from ");
                    notificationSB.append(oldLatency);
                    notificationSB.append(" to ");
                    notificationSB.append(sub.getLatencyInMinutes());
                    notificationSB.append(
                            " (minutes) to fit in available bandwidth.");
                    notificationService.sendSubscriptionUnscheduledNotification(
                            sub, notificationSB.toString(), username);

                    sub.activate();
                    String successMessageIncreasedLatency = action.call();

                    return getForceApplyMessage(subscriptions,
                            successMessageIncreasedLatency, username);

                case FORCE_APPLY_UNSCHEDULED:
                    // Have to make sure we set them to BE UNSCHEDULED. We don't
                    // want the bandwidth manager scheduling it.... YET.
                    for (Subscription temp : subscriptions) {
                        temp.setUnscheduled(true);
                    }
                    String successMessageUnscheduled = action.call();

                    return getForceApplyMessage(subscriptions,
                            successMessageUnscheduled, username);

                case FORCE_APPLY_DEACTIVATED:
                    // Have to make sure we set them to NOT BE UNSCHEDULED, let
                    // the bandwidth manager decide they can't be scheduled
                    for (Subscription temp : subscriptions) {
                        temp.setUnscheduled(true);
                        temp.deactivate();
                    }
                    String successMessageDeactivate = action.call();

                    return getForceApplyMessage(subscriptions,
                            successMessageDeactivate, username);
                case CANCEL:
                    return new SubscriptionServiceResult(true);
                case EDIT_SUBSCRIPTIONS:
                    if (!result.config
                            .isNotAbleToScheduleOnlyTheSubscription()) {
                        VizApp.runSync(new Runnable() {
                            @Override
                            public void run() {
                                new SubscriptionManagerAction()
                                        .loadSubscriptionManager(
                                                SubscriptionManagerFilters
                                                        .getByNames(
                                                                result.config.wouldBeUnscheduledSubscriptions));
                            }
                        });
                    }

                    return new SubscriptionServiceResult(true);
                default:
                    throw new IllegalArgumentException(
                            "Unknown force apply prompt response!  Did you add a new type that must be handled?");
                }
            }

            return new SubscriptionServiceResult(result.config.message);
        } catch (RegistryHandlerException e) {
            // The in-memory objects must be corrupted since we schedule first,
            // then store to the registry, so a reinitialize is called for
            bandwidthService.reinitialize();

            throw e;
        }
    }

    /**
     * Proposes scheduling the subscriptions (with any modifications that have
     * been made) in the bandwidth manager. If subscriptions would be
     * unscheduled as a result, then a message is returned designating such.
     *
     * @param subscriptions
     * @param serviceInteraction
     * @return the result
     * @throws RegistryHandlerException
     */
    private ProposeResult proposeScheduleAndAction(
            List<Subscription> subscriptions,
            ServiceInteraction serviceInteraction,
            IForceApplyPromptDisplayText displayTextStrategy)
            throws RegistryHandlerException {

        ProposeScheduleResponse proposeScheduleresponse = bandwidthService
                .proposeSchedule(subscriptions);
        Set<String> unscheduledSubscriptions = proposeScheduleresponse
                .getUnscheduledSubscriptions();
        boolean wouldUnscheduleSubs = !unscheduledSubscriptions.isEmpty();

        ForceApplyPromptConfiguration response = null;
        if (wouldUnscheduleSubs) {
            response = getWouldCauseUnscheduledSubscriptionsPortion(
                    unscheduledSubscriptions, subscriptions,
                    proposeScheduleresponse, displayTextStrategy);
        } else {
            response = new ForceApplyPromptConfiguration(TITLE,
                    serviceInteraction.call(), displayTextStrategy,
                    unscheduledSubscriptions);
        }

        return new ProposeResult(wouldUnscheduleSubs, response);
    }

    /**
     * Appends the unscheduled subscriptions portion to the StringBuilder.
     *
     * @param unscheduledSubscriptions
     *            the unscheduled subscriptions
     * @param subscriptions
     *            the subscriptions which were attempting to schedule
     * @param dataSize
     */
    private static ForceApplyPromptConfiguration getWouldCauseUnscheduledSubscriptionsPortion(
            Set<String> unscheduledSubscriptions,
            List<Subscription> subscriptions,
            ProposeScheduleResponse proposeScheduleResponse,
            IForceApplyPromptDisplayText displayTextStrategy) {
        StringBuilder msg = new StringBuilder();

        // Handle the case where it's just the subscription we're changing
        // itself that would not schedule
        final boolean singleSubscription = subscriptions.size() == 1;
        if (singleSubscription && unscheduledSubscriptions.size() == 1
                && subscriptions.get(0).getName()
                        .equals(unscheduledSubscriptions.iterator().next())) {
            final Subscription subscription = subscriptions.get(0);
            msg.append(subscription instanceof AdhocSubscription ? "The query"
                    : "Subscription " + subscription.getName())
                    .append(" would not fully schedule with the bandwidth management system if this action were performed.");
        } else {
            msg.append(
                    "The following subscriptions would not fully schedule with the bandwidth management system if this action were performed:");
        }

        if (singleSubscription) {
            Subscription subscription = subscriptions.get(0);
            final int maximumLatencyFromRules = getMaximumAllowableLatency(
                    subscription);

            return new ForceApplyPromptConfiguration(TITLE, msg.toString(),
                    proposeScheduleResponse.getRequiredLatency(),
                    maximumLatencyFromRules,
                    proposeScheduleResponse.getRequiredDataSetSize(),
                    displayTextStrategy, subscription,
                    unscheduledSubscriptions);
        }
        return new ForceApplyPromptConfiguration(TITLE, msg.toString(),
                displayTextStrategy, unscheduledSubscriptions);
    }

    /**
     * Appends the unscheduled subscriptions portion to the StringBuilder.
     *
     * @param unscheduledSubscriptions
     *            the unscheduled subscriptions
     */
    private static void getUnscheduledSubscriptionsPortion(StringBuilder msg,
            Set<String> unscheduledSubscriptions) {
        appendCollectionPortion(msg,
                "\n\nThe following subscriptions did not fully schedule with the bandwidth management system:",
                unscheduledSubscriptions);
    }

    /**
     * Append a collection of items underneath a preamble text.
     *
     * @param msg
     *            the current text
     * @param preamble
     *            the preamble
     * @param collection
     *            the collection of items
     */
    private static void appendCollectionPortion(StringBuilder msg,
            String preamble, Collection<String> collection) {
        if (collection.isEmpty()) {
            return;
        }
        msg.append(StringUtil.createMessage(preamble, collection));
    }

    /**
     * Save a pending subscription.
     *
     * @throws RegistryHandlerException
     */
    private void savePendingSub(InitialPendingSubscription pendingSub,
            String username) throws RegistryHandlerException {
        DataDeliveryHandlers.getPendingSubscriptionHandler().update(username,
                pendingSub);

        notificationService
                .sendCreatedPendingSubscriptionForSubscriptionNotification(
                        pendingSub, username);
    }

    private static void updateSubscriptionsByNameToUnscheduled(String username,
            java.util.Collection<String> subscriptionNames)
            throws RegistryHandlerException {
        SubscriptionHandler subscriptionHandler = DataDeliveryHandlers
                .getSubscriptionHandler();
        for (String subName : subscriptionNames) {
            Subscription unscheduledSub = subscriptionHandler
                    .getByName(subName);
            if (unscheduledSub == null) {
                continue;
            }
            unscheduledSub.setUnscheduled(true);
            if (unscheduledSub instanceof RecurringSubscription) {
                ((RecurringSubscription) unscheduledSub)
                        .setSubscriptionState(SubscriptionState.OFF);
            }
            subscriptionHandler.update(username, unscheduledSub);
        }
    }

    /**
     * Gets the max allowed latency for this subscription from rules for it's
     * type
     *
     * @param subscription
     * @return
     */
    private static int getMaximumAllowableLatency(Subscription subscription) {

        Time subTime = subscription.getTime();

        // gridded subs
        if (subTime instanceof GriddedTime) {
            return SystemRuleManager.getInstance().getLatency(subscription,
                    Sets.newTreeSet(((GriddedTime) subTime).getCycleTimes()));
            // point subs
        } else if (subTime instanceof PointTime) {
            return ((PointTime) subTime).getInterval();
            // PDA, general data type subscriptions
        } else {
            return subscription.getLatencyInMinutes();
        }
    }

    /**
     * Handle the FORCE case for subscription proposal. We create the part of
     * the Result message for the Proposal of the forcing. In particular the
     * part describing whether everything can or can't be scheduled. This is
     * handed back to the dialog and displayed to the user.
     *
     * @param subscriptions
     * @param successMessage
     * @param username
     * @return
     */
    private SubscriptionServiceResult getForceApplyMessage(
            List<Subscription> subscriptions, String successMessage,
            String username) {

        final Set<String> unscheduled = bandwidthService
                .schedule(subscriptions);

        try {
            updateSubscriptionsByNameToUnscheduled(username, unscheduled);
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.ERROR,
                    "Can't update Subscription To set UNSCHEDULED! "
                            + unscheduled,
                    e);
        }

        StringBuilder sb = new StringBuilder(successMessage);
        getUnscheduledSubscriptionsPortion(sb, unscheduled);

        if (unscheduled != null && !unscheduled.isEmpty()) {

            Map<String, Subscription> allSubscriptionMap = new HashMap<>();
            String name = null;
            for (Subscription sub : subscriptions) {
                name = sub.getName();
                allSubscriptionMap.put(name, sub);
            }

            // Publish Notification events
            String msg = " is unscheduled.";
            for (String unSchedSubName : unscheduled) {
                Subscription unSchedSub = allSubscriptionMap
                        .get(unSchedSubName);
                notificationService.sendSubscriptionUnscheduledNotification(
                        unSchedSub, msg, username);
            }
        }

        return new SubscriptionServiceResult(sb.toString());
    }

}
