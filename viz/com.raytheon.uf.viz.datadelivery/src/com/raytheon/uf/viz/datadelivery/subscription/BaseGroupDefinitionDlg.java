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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.datadelivery.registry.GroupDefinition;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.ebxml.DataSetQuery;
import com.raytheon.uf.common.datadelivery.request.DataDeliveryPermission;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.datadelivery.common.ui.ActivePeriodComp;
import com.raytheon.uf.viz.datadelivery.common.ui.AreaControlComp;
import com.raytheon.uf.viz.datadelivery.common.ui.DurationComp;
import com.raytheon.uf.viz.datadelivery.common.ui.IGroupAction;
import com.raytheon.uf.viz.datadelivery.common.ui.UserSelectComp;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryGUIUtils;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;
import com.raytheon.viz.ui.presenter.components.ComboBoxConf;
import com.raytheon.viz.ui.presenter.components.WidgetConf;

/**
 * The Data Delivery Create and Edit Subscription Group Dialog.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- -------------------------
 * Jul 2, 2012    702      jpiatt       Initial creation.
 * Aug 02, 2012 955        djohnson     Type-safe registry query/responses.
 * Aug 10, 2012 1022       djohnson     {@link DataSetQuery} requires provider name.
 * Aug 20, 2012 0743       djohnson     Finish making registry type-safe.
 * Aug 29, 2012   223      mpduff       Renamed some methods.
 * Aug 31, 2012   702      jpiatt       Correct group data population.
 * Oct 03, 2012 1241       djohnson     Use {@link DataDeliveryPermission} and registry handlers.
 * Dec 18, 2012 1440       mpduff       Made non-blocking
 * Dec 10, 2012 1259       bsteffen     Switch Data Delivery from LatLon to referenced envelopes.
 * Jan 02, 2013 1441       djohnson     Access GroupDefinitionManager in a static fashion.
 * Jan 08, 2013 1453       djohnson     Split creation and edit dialogs.
 * Apr 08, 2013 1826       djohnson     Remove delivery options.
 * Mar 31, 2014 2889       dhladky      Added username for notification center tracking.
 * Feb 01, 2016 5289       tgurney      Add missing minimize button in trim
 * Nov 16, 2017 6343       tgurney      Validate group before saving
 *
 * </pre>
 *
 * @author jpiatt
 */
public abstract class BaseGroupDefinitionDlg extends CaveSWTDialog
        implements IGroupValidationData {

    /** Status Handler */
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(BaseGroupDefinitionDlg.class);

    /** Delivery options strings */
    protected static final String[] DELIVERY_OPTIONS = new String[] {
            "Deliver data when available", "Notify when data are available" };

    /** Delivery combo config object */
    protected final ComboBoxConf DELIVERY_COMBO_CONF = new ComboBoxConf(true,
            "Select delivery method", WidgetConf.DO_NOTHING);

    /** The Main Composite */
    protected Composite mainComp;

    /** The Subscription Duration Composite */
    private DurationComp durComp;

    /** The Subscription Duration Composite */
    private ActivePeriodComp activePeriodComp;

    /** The Area Control Composite */
    private AreaControlComp areaControlComp;

    /** The User Selection Composite */
    private UserSelectComp userSelectComp;

    /** IGroupAction callback */
    private final IGroupAction callback;

    /** The new group definition. Not populated until OK button is pressed */
    private GroupDefinition newGroupDefinition;

    /**
     * Validators that will run on any new or updated group definition before it
     * is saved
     */
    private static final List<IGroupValidator> validators = new ArrayList<>();

    static {
        validators.add(new MadisGroupValidator());
    }

    /**
     * Constructor.
     *
     * @param parent
     *            The parent shell
     * @param create
     * @param callback
     *            callback to subscription manager
     */
    public BaseGroupDefinitionDlg(Shell parent, IGroupAction callback) {
        super(parent, SWT.DIALOG_TRIM | SWT.MIN,
                CAVE.INDEPENDENT_SHELL | CAVE.DO_NOT_BLOCK);
        setText(getDialogTitle());
        this.callback = callback;
    }

    @Override
    protected void initializeComponents(Shell shell) {
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(1, false);
        mainComp = new Composite(shell, SWT.NONE);
        mainComp.setLayout(gl);
        mainComp.setLayoutData(gd);

        createGroupInfo();

        durComp = new DurationComp(mainComp);
        activePeriodComp = new ActivePeriodComp(mainComp);
        areaControlComp = new AreaControlComp(mainComp);
        userSelectComp = new UserSelectComp(mainComp);

        createButtons();
    }

    @Override
    protected Layout constructShellLayout() {
        // Create the main layout for the shell.
        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;

        return mainLayout;
    }

    /**
     * Create buttons at the bottom of the dialog.
     */
    private void createButtons() {
        int buttonWidth = 75;
        GridData btnData = new GridData(buttonWidth, SWT.DEFAULT);

        GridData gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(2, false);
        Composite btnComp = new Composite(mainComp, SWT.NONE);
        btnComp.setLayout(gl);
        btnComp.setLayoutData(gd);

        // OK Button
        Button okBtn = new Button(btnComp, SWT.PUSH);
        okBtn.setText("OK");
        okBtn.setLayoutData(btnData);
        okBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (handleOK()) {
                    close();
                }
            }
        });

        // Cancel button
        btnData = new GridData(buttonWidth, SWT.DEFAULT);
        Button cancelBtn = new Button(btnComp, SWT.PUSH);
        cancelBtn.setText("Cancel");
        cancelBtn.setLayoutData(btnData);
        cancelBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                close();
            }
        });
    }

    /**
     * Event handler for the OK button.
     *
     * @return true if data are valid
     */
    private boolean handleOK() {

        boolean datesValid = false;
        boolean activeDatesValid = false;

        String groupName = getGroupName();

        if (!validateGroupName(groupName)) {
            return false;
        }

        // Validate the date entries
        datesValid = durComp.isValidChk();
        activeDatesValid = activePeriodComp.isValidChk();

        if (!datesValid || !activeDatesValid) {
            return false;
        }

        try {

            GroupDefinition groupDefinition = new GroupDefinition();

            groupDefinition.setOwner(
                    LocalizationManager.getInstance().getCurrentUser());
            groupDefinition.setGroupName(groupName);

            // Set Duration
            if (!durComp.isIndefiniteChk()) {

                String startText = durComp.getStartText();
                String endText = durComp.getEndText();

                if (startText.length() > 0) {
                    Date startDate = DataDeliveryGUIUtils
                            .getSubscriptionFormat().parse(startText);
                    groupDefinition.setSubscriptionStart(startDate);
                }
                if (endText.length() > 0) {
                    Date endDate = DataDeliveryGUIUtils.getSubscriptionFormat()
                            .parse(endText);
                    groupDefinition.setSubscriptionEnd(endDate);
                }

            }

            // Set active period
            if (!activePeriodComp.isAlwaysChk()) {

                String startText = activePeriodComp.getActiveStartText();
                String endText = activePeriodComp.getActiveEndText();

                if (startText.length() > 0) {
                    Date startPeriodDate = DataDeliveryGUIUtils
                            .getActiveFormat().parse(startText);
                    groupDefinition.setActivePeriodStart(startPeriodDate);
                }
                if (endText.length() > 0) {
                    Date endPeriodDate = DataDeliveryGUIUtils.getActiveFormat()
                            .parse(endText);
                    groupDefinition.setActivePeriodEnd(endPeriodDate);
                }

            }

            if (!areaControlComp.isAreaChk()
                    && areaControlComp.getEnvelope() != null) {
                // Set Area
                groupDefinition.setEnvelope(areaControlComp.getEnvelope());
            }

            newGroupDefinition = groupDefinition;

            // Validate the new group definition
            for (IGroupValidator validator : validators) {
                String errorMessage = validator.validate(this);
                if (errorMessage != null) {
                    DataDeliveryUtils.showMessage(shell, SWT.ERROR | SWT.OK,
                            "Invalid Group Definition", errorMessage);
                    return false;
                }
            }
            try {
                saveGroupDefinition(
                        LocalizationManager.getInstance().getCurrentUser(),
                        groupDefinition);
            } catch (RegistryHandlerException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to save Group object", e);
            }

        } catch (ParseException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to save Group object", e);
        }

        // Save group properties to selected subscriptions
        userSelectComp.getSubscriptionNames(groupName);

        if (callback != null) {
            // Re-load the group combo box
            callback.loadGroupNames();

            // refresh table
            callback.handleRefresh();
        }

        close();
        return true;

    }

    /** Populate the dlg with the group properties. */
    public void populate(GroupDefinition groupDefinition) {

        // Set duration info
        Date sDate = groupDefinition.getSubscriptionStart();
        Date eDate = groupDefinition.getSubscriptionEnd();

        if (sDate != null || eDate != null) {
            durComp.setStartDate(sDate);
            durComp.setEndDate(eDate);
            durComp.setNoExpiration(false);
        } else {
            durComp.setNoExpiration(true);
            durComp.resetTextBoxes(false);
            durComp.setStartBtnEnabled(false);
            durComp.setEndBtnEnabled(false);

        }

        // Set the Active Period info
        Date saDate = groupDefinition.getActivePeriodStart();
        Date eaDate = groupDefinition.getActivePeriodEnd();

        if (saDate != null || eaDate != null) {
            activePeriodComp.setStartDate(saDate);
            activePeriodComp.setEndDate(eaDate);
            activePeriodComp.setAlwaysActive(false);
        } else {

            activePeriodComp.setAlwaysActive(true);
            activePeriodComp.resetTextBoxes(false);
            activePeriodComp.setStartBtnEnabled(false);
            activePeriodComp.setEndBtnEnabled(false);

        }

        // Select the Area info
        if (groupDefinition.isArealDataSet()) {

            areaControlComp.updateAreaLabel(groupDefinition.getEnvelope());

            areaControlComp.setNoArealCoverage(false);
            areaControlComp.setClearButton(true);
            areaControlComp.setAreaButton(true);

        } else {
            areaControlComp.setNoArealCoverage(true);
            areaControlComp.setClearButton(false);
            areaControlComp.setAreaButton(false);
            areaControlComp.resetTextBoxes(false);
        }

        // populate user info properties
        userSelectComp
                .selectSubscriptionsInGroup(groupDefinition.getGroupName());

    }

    @Override
    public Set<Subscription> getOldSubscriptions() {
        return userSelectComp.getInitiallySelectedSubscriptions();
    }

    @Override
    public Set<Subscription> getNewSubscriptions() {
        return userSelectComp.getSelectedSubscriptions();
    }

    @Override
    public GroupDefinition getOldGroupDefinition() {
        /* No old group definition unless editing an existing group */
        return null;
    }

    @Override
    public GroupDefinition getNewGroupDefinition() {
        return newGroupDefinition;
    }

    /**
     * @return the dialog title
     */
    protected abstract String getDialogTitle();

    /**
     * Create the group name section.
     */
    protected abstract void createGroupInfo();

    /**
     * @return the group name
     */
    protected abstract String getGroupName();

    /**
     * Save the group definition.
     *
     * @param groupDefinition
     *            the group definition
     * @throws RegistryHandlerException
     */
    protected abstract void saveGroupDefinition(String username,
            GroupDefinition groupDefinition) throws RegistryHandlerException;

    /**
     * Validate the group name.
     *
     * @param groupName
     *            the group name
     * @return true if validated
     */
    protected abstract boolean validateGroupName(String groupName);
}
