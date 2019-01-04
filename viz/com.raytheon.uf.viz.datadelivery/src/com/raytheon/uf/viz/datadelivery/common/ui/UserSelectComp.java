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
package com.raytheon.uf.viz.datadelivery.common.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.google.common.collect.Sets;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.registry.handlers.SubscriptionHandler;
import com.raytheon.uf.common.datadelivery.request.DataDeliveryPermission;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.datadelivery.subscription.SubscriptionService.ForceApplyPromptResponse;
import com.raytheon.uf.viz.datadelivery.subscription.SubscriptionService.IForceApplyPromptDisplayText;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;
import com.raytheon.viz.ui.presenter.IDisplay;
import com.raytheon.viz.ui.widgets.duallist.DualList;
import com.raytheon.viz.ui.widgets.duallist.DualListConfig;
import com.raytheon.viz.ui.widgets.duallist.IUpdate;

/**
 * This is the user select composite. This class is intended to be extended so
 * common classes can be created and shared.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 27, 2012  702      jpiatt    Initial creation.
 * Aug 08, 2012  863      jpiatt    Added new interface method.
 * Aug 22, 2012  712      mpduff    Fix notifications.
 * Aug 20, 2012  743      djohnson  Finish making registry type-safe,
 *                                  AssociationQuery for pending subscriptions.
 * Aug 30, 2012  702      jpiatt    Populate selected subscriptions according to
 *                                  group.
 * Aug 31, 2012  1128     mpduff    Additional notification fixes, only set
 *                                  group related fields in subscription.
 * Sep 06, 2012  687      mpduff    Add the Subscription object back into the
 *                                  SubscriptionNotificationRequest object.
 * Sep 14, 2012  1169     djohnson  Use storeOrReplaceRegistryObject.
 * Sep 24, 2012  1157     mpduff    Use InitialPendingSubscription as needed.
 * Oct 03, 2012  1241     djohnson  Use {@link DataDeliveryPermission} and
 *                                  handlers for registry interaction.
 * Oct 24, 2012  1290     mpduff    Added check for group definition areal data
 *                                  being set.
 * Nov 09, 2012  1286     djohnson  Consolidate duplicate subscription handling.
 * Nov 20, 2012  1286     djohnson  Fix formatting, implement IDisplay to
 *                                  display yes/no prompt.
 * Dec 10, 2012  1259     bsteffen  Switch Data Delivery from LatLon to
 *                                  referenced envelopes.
 * Jan 02, 2013  1441     djohnson  Access GroupDefinitionManager in a static
 *                                  fashion.
 * Apr 08, 2013  1826     djohnson  Remove unused code, delivery options.
 * May 15, 2013  1040     mpduff    OfficeID is now a list so need to add it
 *                                  rather than set it.
 * May 23, 2013  1650     djohnson  Fix creation of new GroupDefinitions.
 * May 28, 2013  1650     djohnson  More information when failing to schedule
 *                                  subscriptions.
 * Jun 13, 2013  2108     mpduff    Refactored DataSizeUtils.
 * Oct 28, 2013  2292     mpduff    Change overlap services.
 * Feb 11, 2014  2771     bgonzale  Use Data Delivery ID instead of Site.
 * Mar 31, 2014  2889     dhladky   Added username for notification center
 *                                  tracking.
 * Aug 18, 2014  2746     ccody     Non-local Subscription changes not updating
 *                                  dialogs
 * Oct 28, 2014  2748     ccody     Remove Live update. Updates are event
 *                                  driven.
 * Nov 19, 2014  3850     dhladky   Bad cast from Subscription to
 *                                  InitialPendingSubscription.
 * Nov 19, 2014  3851     dhladky   Fixed userName subscription selection bounce
 *                                  back on change of user.
 * Nov 19, 2014  3852     dhladky   Resurrected the unscheduled state.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Mar 28, 2016  5482     randerso  Fixed GUI sizing issues
 * Jan 09, 2017  746      bsteffen  Unique wording for FORCE_APPLY_DEACTIVATED option
 * Feb 21, 2017  746      bsteffen  Set request envelope when area changes.
 * Feb 28, 2017  6121     randerso  Update DualListConfig settings
 * Jun 27, 2017  746      bsteffen  Ensure subscription messages make it to the user.
 * Nov 16, 2017  6343     tgurney   Add get subscription methods
 * Jan 03, 2019  7503     troberts  Remove subscription grouping capabilities.
 * </pre>
 *
 * @author jpiatt
 */
public class UserSelectComp<T extends Time, C extends Coverage> extends
        Composite implements IUpdate, IDisplay, IForceApplyPromptDisplayText {

    /** Group Name combo box. */
    private Combo userNameCombo;

    /** Dual List Object */
    private DualList dualList;

    /** Currently logged in user */
    private final String currentUser = LocalizationManager.getInstance()
            .getCurrentUser();

    /** User Name array list */
    private final List<String> nameArr = new ArrayList<>();

    /** DualListConfig object */
    private DualListConfig dualConfig;

    /** map to hold user subscriptions */
    private final Map<String, Map<String, Subscription<T, C>>> userMap = new HashMap<>();

    private final Set<String> initiallySelectedSubscriptions = new HashSet<>();

    /** Keeps track of UserName in selection combo **/
    private String previousUserNameComboSelection = "";

    /**
     * Registry handler for subscriptions.
     */
    private final SubscriptionHandler subHandler = DataDeliveryHandlers
            .getSubscriptionHandler();

    /**
     * Constructor.
     *
     * @param parent
     *            Parent composite.
     */
    public UserSelectComp(Composite parent) {
        super(parent, SWT.NONE);

        init();
    }

    /**
     * Initialize method.
     */
    private void init() {
        /*
         * Setup the layout for the composite
         */
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        GridLayout gl = new GridLayout(1, true);
        gl.verticalSpacing = 2;
        gl.marginHeight = 2;
        gl.marginWidth = 2;
        this.setLayout(gl);
        this.setLayoutData(gd);

        createUserInfo();
        loadUsers();

    }

    /**
     * Create the user information.
     */
    private void createUserInfo() {
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(1, false);

        Group userNameInfo = new Group(this, SWT.NONE);
        userNameInfo.setLayout(gl);
        userNameInfo.setLayoutData(gd);
        userNameInfo.setText("  User Information  ");

        Composite userComp = new Composite(userNameInfo, SWT.NONE);
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gl = new GridLayout(2, false);
        userComp.setLayoutData(gd);
        userComp.setLayout(gl);

        // User Combo box
        Label userName = new Label(userComp, SWT.NONE);
        userName.setText("User: ");

        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT);
        userNameCombo = new Combo(userComp, SWT.READ_ONLY);
        userNameCombo.setLayoutData(gd);
        userNameCombo.setToolTipText("Select a user name");
        userNameCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleUserSelect();
            }
        });

        // Available & Selected Subscriptions
        dualConfig = new DualListConfig();
        dualConfig.setVisibleItems(10);
        dualConfig.setListWidthInChars(20);
        dualConfig.setShowUpDownBtns(false);
        dualConfig.setAvailableListLabel("Available Subscriptions:");
        dualConfig.setSelectedListLabel("Selected Subscriptions:");

        dualList = new DualList(userNameInfo, SWT.NONE, dualConfig, this);

    }

    /**
     * Handle a different user selected from the combo box.
     */
    private void handleUserSelect() {

        // Must handle "First Time" selected
        if ("".equals(previousUserNameComboSelection)) {
            previousUserNameComboSelection = userNameCombo.getText();
        }

        if (!userNameCombo.getText().equals(previousUserNameComboSelection)) {
            populateUserSubscriptions(userNameCombo.getText());
            previousUserNameComboSelection = userNameCombo.getText();
        }
    }

    /**
     * Populate the Available subscriptions for a selected user.
     */
    private void populateUserSubscriptions(String owner) {
        /** Status Handler */
        final IUFStatusHandler statusHandler = UFStatus
                .getHandler(UserSelectComp.class);

        final String ownerToUse = owner == null ? currentUser : owner;

        @SuppressWarnings("rawtypes")
        List<Subscription> results = Collections.emptyList();
        try {
            results = subHandler.getByOwner(ownerToUse);
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to retrieve subscriptions for user " + ownerToUse,
                    e);
        }

        ArrayList<String> fullList = new ArrayList<>();
        Map<String, Subscription<T, C>> hMap = new HashMap<>();

        for (Subscription<T, C> subscription : results) {

            String subName = subscription.getName();
            String user = subscription.getOwner();

            if (!nameArr.contains(user)) {
                nameArr.add(user);
            }

            fullList.add(subName);
            hMap.put(subName, subscription);
        }

        dualConfig = new DualListConfig();
        dualList.setFullList(fullList);
        userMap.put(owner, hMap);
    }

    public Set<Subscription> getInitiallySelectedSubscriptions() {
        String owner = userNameCombo.getText();
        Map<String, Subscription<T, C>> ownerSubs = userMap.get(owner);
        return initiallySelectedSubscriptions.stream()
                .map(str -> ownerSubs.get(str)).collect(Collectors.toSet());
    }

    public Set<Subscription> getSelectedSubscriptions() {
        String owner = userNameCombo.getText();
        Map<String, Subscription<T, C>> ownerSubs = userMap.get(owner);
        Set<String> selectedSubNames = Sets
                .newHashSet(dualList.getSelectedListItems());
        return selectedSubNames.stream().map(str -> ownerSubs.get(str))
                .collect(Collectors.toSet());
    }

    /**
     * Load the User list.
     */
    private void loadUsers() {
        if (!nameArr.contains(currentUser)) {
            nameArr.add(currentUser);
        }

        populateUserSubscriptions(currentUser);

        String[] userArr = nameArr.toArray(new String[nameArr.size()]);
        userNameCombo.setItems(userArr);
        userNameCombo.select(0);
    }

    /**
     * Check the selected list when it has changed.
     */
    @Override
    public void hasEntries(boolean entries) {
        // unused
    }

    @Override
    public void selectionChanged() {
        // unused
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean displayYesNoPopup(String title, String message) {
        return DataDeliveryUtils.showYesNoMessage(getShell(), title,
                message) == SWT.YES;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public String getOptionDisplayText(ForceApplyPromptResponse option,
            int requiredLatency, Subscription subscription,
            Set<String> wouldBeUnscheduledSubscriptions) {
        final boolean singleSubscription = wouldBeUnscheduledSubscriptions
                .size() == 1;
        switch (option) {
        case CANCEL:
            return "Do not update the group definition.";
        case FORCE_APPLY_DEACTIVATED:
            if (singleSubscription) {
                return "Update the group definition and deactivate "
                        + wouldBeUnscheduledSubscriptions.iterator().next();
            }
            return "Update the group definition and leave in a Deactivated status";
        case FORCE_APPLY_UNSCHEDULED:
            if (singleSubscription) {
                return "Update the group definition and unschedule "
                        + wouldBeUnscheduledSubscriptions.iterator().next();
            }
            return "Update the group definition and unschedule the subscriptions";
        case EDIT_SUBSCRIPTIONS:
            return "Edit the "
                    + (singleSubscription ? "subscription" : "subscriptions");
        case INCREASE_LATENCY:
            // Signifies it should not be an option
            return null;
        default:
            throw new IllegalArgumentException(
                    "Don't know how to handle option [" + option + "]");
        }
    }
}
