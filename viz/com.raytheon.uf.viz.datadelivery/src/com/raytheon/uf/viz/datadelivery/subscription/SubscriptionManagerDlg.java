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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;

import com.raytheon.uf.common.auth.AuthException;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.datadelivery.registry.SharedSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.handlers.SubscriptionHandler;
import com.raytheon.uf.common.datadelivery.request.DataDeliveryPermission;
import com.raytheon.uf.common.datadelivery.service.SendToServerSubscriptionNotificationService;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.registry.handler.RegistryObjectHandlers;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.Pair;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.auth.UserController;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.datadelivery.actions.DataBrowserAction;
import com.raytheon.uf.viz.datadelivery.common.ui.IGroupAction;
import com.raytheon.uf.viz.datadelivery.common.ui.ITableChange;
import com.raytheon.uf.viz.datadelivery.common.ui.LoadSaveConfigDlg;
import com.raytheon.uf.viz.datadelivery.common.ui.LoadSaveConfigDlg.DialogType;
import com.raytheon.uf.viz.datadelivery.common.ui.TableCompConfig;
import com.raytheon.uf.viz.datadelivery.help.HelpManager;
import com.raytheon.uf.viz.datadelivery.services.DataDeliveryServices;
import com.raytheon.uf.viz.datadelivery.subscription.SubscriptionService.IForceApplyPromptDisplayText;
import com.raytheon.uf.viz.datadelivery.subscription.SubscriptionTableComp.SubscriptionType;
import com.raytheon.uf.viz.datadelivery.subscription.approve.SubscriptionApprovalDlg;
import com.raytheon.uf.viz.datadelivery.subscription.xml.SubscriptionManagerConfigXML;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryGUIUtils;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils.TABLE_TYPE;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;
import com.raytheon.viz.ui.dialogs.ICloseCallback;
import com.raytheon.viz.ui.dialogs.SWTMessageBox;
import com.raytheon.viz.ui.presenter.IDisplay;

/**
 * Subscription Manager Main Dialog.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 9, 2012             mpduff      Initial creation.
 * Mar 9, 2012    418      jpiatt      Added load, save & set default xml.
 * Mar 13, 2012   420      jpiatt      Added retrieval of subscriptions.
 * May 22, 2012   645      jpiatt      Added help dialog.
 * Jun 07, 2012   687      lvenable    Table data refactor.
 * Jul 16, 2012   702      jpiatt      Modified for group name.
 * Aug 21, 2012   712      mpduff      Send a notification for each deleted subscription.
 * Aug 20, 2012  0743      djohnson    Finish making registry type-safe.
 * Aug 29, 2012   223      mpduff      Cleanup.
 * Aug 31, 2012  1128      mpduff      Notification fixes, add wait cursor.
 * Sep 06, 2012  1142      djohnson    Delete pending subscription(s) on subscription deletion.
 * Sep 06, 2012   687      mpduff      Add subscription object into the SubscriptionNotificationRequest object.
 * Sep 14, 2012  1169      djohnson    Use storeOrReplaceRegistryObject.
 * Sep 24, 2012  1157      mpduff      Fixed null pointer problem with auth request checking.
 * Oct 02, 2012  1103      jpiatt      Updated enum naming convention.
 * Oct 03, 2012  1241      djohnson    Use {@link DataDeliveryPermission} and registry handlers.
 * Nov 06, 2012  1306      djohnson    Use authorization/authentication API plugin.
 * Nov 09, 2012  1286      djohnson    Consolidate duplicate subscription handling.
 * Nov 20, 2012  1286      djohnson    Implement IDisplay to display yes/no prompt.
 * Nov 28, 2012  1286      djohnson    Use subscription service.
 * Dec 03, 2012  1285      bgonzale    Added implementation of the tableLock method.
 * Dec 12, 2012  1391      bgonzale    Added job for subscription deletion.
 * Dec 12, 2012  1433      bgonzale    Refresh after subscription copy.
 * Dec 18, 2012  1440      mpduff      Only open edit group dialog if there are group(s) to edit.
 * Jan 02, 2013  1441      djohnson    Add ability to delete groups.
 * Jan 03, 2013  1437      bgonzale    Moved configuration file management code to SubscriptionManagerConfigDlg
 *                                     and SubscriptionConfigurationManager.
 * Jan 21, 2013  1501      djohnson    Only send notification if subscription was actually activated/deactivated,
 *                                     remove race condition of GUI thread updating the table after notification.
 * Jan 22, 2013  1520      mpduff      Removed menu accelerators.
 * Mar 29, 2013  1841      djohnson    Subscription implementations now provide a copy method.
 * May 09, 2013  2000      djohnson    Copy subscription now requires editing first to prevent duplicates, and remove duplicate code.
 * May 17, 2013  1040      mpduff      Change office id to list for shared subscription.
 * May 28, 2013  1650      djohnson    Allow specifying filters for what subscriptions to show.
 * Jun 05, 2013  2064      mpduff      Fix for filtering combo boxes.
 * Jun 06, 2013  2030      mpduff      Refactored help.
 * Jun 14, 2013  2064      mpduff      Check for null/disposed sort column.
 * Jul 26, 2013  2232      mpduff      Refactored Data Delivery permissions.
 * Sep 25. 2013  2409      mpduff      Add check for widget disposed after calling configuration.
 * Oct 25, 2013  2292      mpduff      Move overlap checks to edex.
 * Nov 06, 2013  2358      mpduff      Resurrected file management code.
 * Nov 08, 2013  2506      bgonzale    Removed send notification when a subscription is deleted.
 * Dec 05, 2013  2570      skorolev    Show All subscriptions.
 * Jan 08, 2014  2642      mpduff      Update dialog for permissions, adding site to shared
 * Jan 14, 2014  2459      mpduff      Change Subscription status code
 * Feb 04, 2014  2722      mpduff      Add auto-refresh task.
 * Feb 14, 2014  2806      mpduff      Disable activate/deactivate buttons when viewing other site's subscriptions
 * Feb 11, 2014  2771      bgonzale    Use Data Delivery ID instead of Site.
 * Mar 24, 2014  2951      lvenable    Added dispose checks for SWT widgets.
 * Mar 31, 2014  2889      dhladky     Added username for notification center tracking.
 * Apr 2,  2014  2974      dhladky     DD ID added to list for dropdowns in DD.
 * Apr 18, 2014  3012      dhladky     Null check.
 * Dec 03, 2014  3840      ccody       Correct sorting "contract violation" issue.
 * Jan 26, 2015  2894      dhladky     Default configuration restored for consistency.
 * Jan 30, 2015  2746      dhladky     Special shared sub delete handling.
 * Mar 20, 2015  2894      dhladky     Revisisted consistency in appliying default config.
 * May 17, 2015  4047      dhladky     verified non-blocking.
 * Jun 09, 2015  4047      dhladky     Dialog blocked CAVE at initial startup, fixed.
 * Jul 01, 2015  4047      dhladky     RefreshTask was configured to not run often enough.
 * Jan 29, 2016  5289      tgurney    Add missing maximize button in trim
 * Mar 16, 2016  3919      tjensen    Cleanup unneeded interfaces
 * Mar 28, 2016  5482      randerso    Fixed GUI sizing issues
 * Jan 10, 2017  746       bsteffen    Avoid dialog spam when activating/deactivating many subscriptions
 * Oct 27, 2017  6467      tgurney     Update "not authorized" message text
 * Nov 17, 2017  6343      tgurney     Remove unused groupSelectionUpdate()
 *
 * </pre>
 *
 * @author mpduff
 */

public class SubscriptionManagerDlg extends CaveSWTDialog
        implements ITableChange, ISubscriptionAction, IGroupAction, IDisplay {

    /** Status Handler */
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SubscriptionManagerDlg.class);

    /** Help file */
    private static final String SUBSCRIPTION_MANAGER_HELP_FILE = "help/subscriptionManagerHelp.xml";

    /** Enumeration to use with Data set */
    public static enum FullDataset {
        /** Full data set of type Full */
        FULL("Full"),
        /** Full data set of type Subset */
        SUBSET("Subset");

        private String fullSet;

        private FullDataset(String fullSet) {
            this.fullSet = fullSet;
        }

        @Override
        public String toString() {
            return fullSet;
        }
    }

    /** Current site */
    private final String CURRENT_SITE = DataDeliveryUtils.getDataDeliveryId();

    /** The activate button */
    private Button activateBtn;

    /** The deactivate button */
    private Button deactivateBtn;

    /** Subscription Manager Configuration Dialog */
    private SubscriptionManagerConfigDlg configDlg = null;

    /** Subscription table composite. */
    private SubscriptionTableComp tableComp;

    /** Tool tip menu item */
    private MenuItem tooltipMI;

    /** Office ID combo box */
    private Combo officeCbo;

    /** Group combo box */
    private Combo groupCbo;

    /** Subscription Approval Dialog */
    private SubscriptionApprovalDlg dlg = null;

    /** Create Group Dialog */
    private CreateGroupDefinitionDlg createGroupDlg;

    /** Edit Group Dialog */
    private EditGroupDefinitionDlg editGroupDlg;

    /** Delete Group Dialog */
    private DeleteGroupDlg deleteGroupDlg;

    /** The subscription service */
    private final SubscriptionService subscriptionService = DataDeliveryServices
            .getSubscriptionService();

    /** The subscription notification service */
    private final SendToServerSubscriptionNotificationService subscriptionNotificationService = DataDeliveryServices
            .getSubscriptionNotificationService();

    private ISubscriptionManagerFilter filter;

    /** The selected office */
    private String selectedOffice;

    /** The selected group */
    private String selectedGroup;

    /** Load config dialog */
    private LoadSaveConfigDlg loadDlg;

    /** Delete config dialog */
    private LoadSaveConfigDlg deleteDlg;

    /** SaveAs config dialog */
    private LoadSaveConfigDlg saveAsDlg;

    /** Option to select all subscriptions */
    private static final String ALL = "ALL";

    /** Option to select all groups of subscriptions */
    private static final String ALL_SUBSCRIPTIONS = "All Subscriptions";

    /** Edit menu */
    private MenuItem editMI;

    /** Copy menu */
    private MenuItem copyMI;

    /** Delete menu */
    private MenuItem deleteMI;

    /** Edit group menu */
    private MenuItem editGroupMI;

    /** Delete group menu */
    private MenuItem deleteGroupMI;

    /** Group menu */
    private MenuItem groupMI;

    /** New menu */
    private MenuItem newMI;

    /** scheduled executor */
    private final ScheduledExecutorService scheduler;

    /** instance of configuration manager */
    private final SubscriptionConfigurationManager configMan = SubscriptionConfigurationManager
            .getInstance();

    /**
     * Constructor
     *
     * @param parent
     *            The parent shell
     * @param filter
     */
    public SubscriptionManagerDlg(Shell parent,
            ISubscriptionManagerFilter filter) {
        super(parent, SWT.DIALOG_TRIM | SWT.MIN | SWT.MAX | SWT.RESIZE,
                CAVE.INDEPENDENT_SHELL | CAVE.PERSPECTIVE_INDEPENDENT
                        | CAVE.DO_NOT_BLOCK);

        this.filter = filter;
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new RefreshTask(), 2, 2,
                TimeUnit.MINUTES);
        setText("Data Delivery Subscription Manager");
    }

    @Override
    protected Object constructShellLayoutData() {
        return new GridData(SWT.FILL, SWT.DEFAULT, true, false);
    }

    @Override
    protected void preOpened() {
        /*
         * The reasoning for setting the shell size in the preOpened method is
         * due the way SWT works with tables. When the table has more columns
         * than what can be displayed on screen the table/dialog becomes full
         * screen. The table and composites are set to fill so when the dialog
         * is resized the table will stretch. So to fix this issue the dialog
         * size is set to a predetermined size.
         */
        shell.setSize(1100, 350);
    }

    @Override
    protected void initializeComponents(Shell shell) {

        configMan.loadDefaultFile(true);
        shell.setMinimumSize(750, 320);
        createMenus();
        createTopLayout();
        createTableControl();
        loadGroupNames();
        loadOfficeNames();
        createBottomButtons();
        enableControls(true);
    }

    @Override
    protected Layout constructShellLayout() {
        // Create the main layout for the shell.
        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;

        return mainLayout;
    }

    @Override
    protected void disposed() {
        super.disposed();
        scheduler.shutdownNow();
    }

    /**
     * Create subscription menu.
     */
    private void createMenus() {
        Menu menuBar = new Menu(shell, SWT.BAR);

        // Create the file menu
        MenuItem fileMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        fileMenuItem.setText("&File");

        // Create the File menu item with a File "dropdown" menu
        Menu fileMenu = new Menu(menuBar);
        fileMenuItem.setMenu(fileMenu);

        newMI = new MenuItem(fileMenu, SWT.NONE);
        newMI.setText("New Subscription...");
        newMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                createSubscription();
            }
        });

        groupMI = new MenuItem(fileMenu, SWT.NONE);
        groupMI.setText("New Group...");
        groupMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleGroupCreate(true);
            }
        });

        MenuItem refreshMI = new MenuItem(fileMenu, SWT.NONE);
        refreshMI.setText("Refresh Table");
        refreshMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRefresh();
            }

        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        MenuItem approveMI = new MenuItem(fileMenu, SWT.NONE);
        approveMI.setText("Approve Pending Subscriptions...");
        approveMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                launchApprovalDlg();
            }
        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        MenuItem setDefaultMI = new MenuItem(fileMenu, SWT.NONE);
        setDefaultMI.setText("Set as Default");
        setDefaultMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleSetDefault();
            }
        });

        MenuItem loadConfigMI = new MenuItem(fileMenu, SWT.NONE);
        loadConfigMI.setText("Load Configuration...");
        loadConfigMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleLoadConfig();
            }
        });

        MenuItem saveConfigMI = new MenuItem(fileMenu, SWT.NONE);
        saveConfigMI.setText("Save Configuration");
        saveConfigMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleSaveConfig();
            }
        });

        MenuItem saveConfigAsMI = new MenuItem(fileMenu, SWT.NONE);
        saveConfigAsMI.setText("Save Configuration As...");
        saveConfigAsMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleSaveAsConfig();
            }
        });

        MenuItem deleteConfigMI = new MenuItem(fileMenu, SWT.NONE);
        deleteConfigMI.setText("Delete Configuration...");
        deleteConfigMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDeleteConfig();
            }
        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        MenuItem exitMI = new MenuItem(fileMenu, SWT.NONE);
        exitMI.setText("Exit");
        exitMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                close();
            }
        });

        createEditMenu(menuBar);

        // Create the settings menu
        MenuItem settingsMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        settingsMenuItem.setText("&Settings");

        Menu settingsMenu = new Menu(menuBar);
        settingsMenuItem.setMenu(settingsMenu);

        MenuItem configureMI = new MenuItem(settingsMenu, SWT.NONE);
        configureMI.setText("Configure Table...");
        configureMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleTableConfiguration();
            }
        });

        tooltipMI = new MenuItem(settingsMenu, SWT.CHECK);
        tooltipMI.setText("Tooltips");
        tooltipMI.setSelection(false);
        tooltipMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleTooltipSelection(tooltipMI.getSelection());
            }

        });

        // Create the help menu
        MenuItem helpMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        helpMenuItem.setText("&Help");

        Menu helpMenu = new Menu(menuBar);
        helpMenuItem.setMenu(helpMenu);

        MenuItem helpNotTableMI = new MenuItem(helpMenu, SWT.NONE);
        helpNotTableMI.setText("About Subscription Manager...");
        helpNotTableMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleHelp();
            }

        });

        shell.setMenuBar(menuBar);
    }

    private void createEditMenu(Menu menuBar) {
        MenuItem editMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        editMenuItem.setText("&Edit");

        Menu editMenu = new Menu(menuBar);
        editMenuItem.setMenu(editMenu);

        editMI = new MenuItem(editMenu, SWT.NONE);
        editMI.setText("Edit Subscription...");
        editMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                tableComp.handleEdit();
            }
        });

        copyMI = new MenuItem(editMenu, SWT.NONE);
        copyMI.setText("Copy Subscription...");
        copyMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleCopy();
            }
        });

        deleteMI = new MenuItem(editMenu, SWT.NONE);
        deleteMI.setText("Delete/Remove from Subscription");
        deleteMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleDelete();
            }
        });

        editGroupMI = new MenuItem(editMenu, SWT.NONE);
        editGroupMI.setText("Edit Group...");
        editGroupMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleGroupCreate(false);
            }
        });

        deleteGroupMI = new MenuItem(editMenu, SWT.NONE);
        deleteGroupMI.setText("Delete Group...");
        deleteGroupMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                deleteGroupSelected();
            }
        });
    }

    /**
     * Create the bottom panel with page control and row data information.
     */
    private void createTableControl() {

        TableCompConfig tableConfig = new TableCompConfig(
                TABLE_TYPE.SUBSCRIPTION);
        tableConfig.setTableStyle(SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
                | SWT.MULTI | SWT.FULL_SELECTION);
        tableConfig.setTableHeight(200);
        tableComp = new SubscriptionTableComp(shell, tableConfig, this,
                SubscriptionType.MANAGER, filter);

        tableComp.populateData();
        tableComp.populateTable();

    }

    /**
     * Create portion above table.
     */
    private void createTopLayout() {

        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(2, false);

        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gl = new GridLayout(2, false);
        Composite topComp = new Composite(shell, SWT.NONE);
        topComp.setLayout(gl);
        topComp.setLayoutData(gd);

        Composite officeComp = new Composite(topComp, SWT.NONE);

        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gl = new GridLayout(4, false);
        officeComp.setLayout(gl);
        officeComp.setLayoutData(gd);

        // Office label
        Label officeLbl = new Label(officeComp, SWT.NONE);
        officeLbl.setText("Office: ");

        // Office Selection Combo Box
        officeCbo = new Combo(officeComp, SWT.READ_ONLY | SWT.BORDER);
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT);
        officeCbo.setLayoutData(gd);
        officeCbo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleFilterSelection();
                enableControls(officeCbo.getText().equals(CURRENT_SITE));
            }
        });

        // Group label
        Label groupLbl = new Label(officeComp, SWT.NONE);
        groupLbl.setText("        Group: ");

        // Group Selection Combo Box
        groupCbo = new Combo(officeComp, SWT.READ_ONLY);
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT);
        groupCbo.setLayoutData(gd);
        groupCbo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleFilterSelection();
            }

        });
    }

    /**
     * Refresh the subscription table data.
     */
    @Override
    public void handleRefresh() {

        VizApp.runAsync(new Runnable() {
            @Override
            public void run() {
                if (isDisposed()) {
                    return;
                }
                tableComp.handleRefresh();
            }
        });
    }

    private void createBottomButtons() {
        GridData gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(2, true);

        GridData btnData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);

        Composite actionComp = new Composite(shell, SWT.NONE);
        actionComp.setLayout(gl);
        actionComp.setLayoutData(gd);

        activateBtn = new Button(actionComp, SWT.PUSH);
        activateBtn.setText("Activate");
        activateBtn.setLayoutData(btnData);
        activateBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleActivateDeactivate(true);
            }
        });

        btnData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        deactivateBtn = new Button(actionComp, SWT.PUSH);
        deactivateBtn.setText("Deactivate");
        deactivateBtn.setLayoutData(btnData);
        deactivateBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleActivateDeactivate(false);
            }
        });
    }

    /**
     * Create the subscription.
     */
    private void createSubscription() {
        // check to see if authorized
        final String permission = DataDeliveryPermission.SUBSCRIPTION_CREATE
                .toString();
        IUser user = UserController.getUserObject();
        String msg = user.uniqueId()
                + " is not authorized to create subscriptions.\nPermission: "
                + permission;
        try {
            if (DataDeliveryServices.getPermissionsService()
                    .checkPermission(user, msg, permission).isAuthorized()) {
                DataBrowserAction action = new DataBrowserAction();
                Map<String, String> params = new HashMap<>();
                ExecutionEvent ee = new ExecutionEvent(null, params, null,
                        null);
                action.execute(ee);
            }
        } catch (ExecutionException e) {
            statusHandler.handle(
                    com.raytheon.uf.common.status.UFStatus.Priority.ERROR,
                    e.getLocalizedMessage(), e);
        } catch (AuthException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Open the Create Group dialog.
     *
     * @param create
     *            true for create dialog and false for edit
     */
    private void handleGroupCreate(boolean create) {
        final String permission = DataDeliveryPermission.SUBSCRIPTION_CREATE
                .toString();
        IUser user = UserController.getUserObject();
        String msg = user.uniqueId()
                + " is not authorized to access the Dataset Discovery Browser\nPermission: "
                + permission;

        try {
            if (DataDeliveryServices.getPermissionsService()
                    .checkPermission(user, msg, permission).isAuthorized()) {
                if (create) {
                    if (createGroupDlg == null) {
                        createGroupDlg = new CreateGroupDefinitionDlg(
                                this.shell, this);
                    }
                    createGroupDlg.open();
                } else {
                    if (thereAreGroupsAvailable()) {
                        if (editGroupDlg == null) {
                            editGroupDlg = new EditGroupDefinitionDlg(
                                    this.shell, this);
                        }
                        editGroupDlg.open();
                    } else {
                        DataDeliveryUtils.showMessage(getShell(), SWT.OK,
                                "No Groups Defined",
                                "No groups currently defined.\n\n"
                                        + "Select the File->New Group... menu to create a group");
                    }
                }
            }
        } catch (AuthException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error occurred in authorization request", e);
        }
    }

    /**
     * Deletes the selected group.
     */
    private void deleteGroupSelected() {
        if (thereAreGroupsAvailable()) {
            if (deleteGroupDlg == null) {
                deleteGroupDlg = new DeleteGroupDlg(this.shell, this);
            }
            deleteGroupDlg.open();
        } else {
            DataDeliveryUtils.showMessage(getShell(), SWT.OK,
                    "No Groups Defined", "No groups currently defined.\n\n"
                            + "Select the File->New Group... menu to create a group");
        }
    }

    /**
     * Set the default configuration file.
     */
    private void handleSetDefault() {

        String fileName = configMan.getDefaultXMLConfig();

        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext context = pm.getContext(
                LocalizationType.CAVE_STATIC, LocalizationLevel.USER);

        LocalizationFile locFile = pm.getLocalizationFile(context, fileName);

        try {
            configMan.setCurrentConfigFile(locFile);
            configMan.saveXml();
        } catch (Exception e) {
            statusHandler.handle(
                    com.raytheon.uf.common.status.UFStatus.Priority.ERROR,
                    e.getLocalizedMessage(), e);
        }
    }

    /**
     * Load configuration action.
     */
    private void handleLoadConfig() {
        if (loadDlg == null || loadDlg.isDisposed()) {
            loadDlg = new LoadSaveConfigDlg(shell, DialogType.OPEN,
                    configMan.getLocalizationPath(),
                    configMan.getDefaultXMLConfigFileName(), true);
            loadDlg.addCloseCallback(new ICloseCallback() {
                @Override
                public void dialogClosed(Object returnValue) {
                    if (returnValue instanceof LocalizationFile) {
                        try {
                            LocalizationFile fileName = (LocalizationFile) returnValue;
                            // Get the name of the selected file
                            if (fileName != null && fileName.exists()) {
                                File file = fileName.getFile();

                                if (file != null) {
                                    SubscriptionManagerConfigXML xml = configMan
                                            .unmarshall(file);
                                    configMan.setXml(xml);
                                    configMan.setCurrentConfigFile(fileName);
                                    tableChanged();
                                }
                            }
                        } catch (JAXBException e) {
                            statusHandler.error(e.getLocalizedMessage(), e);
                        }
                    }
                    loadDlg = null;
                }
            });

            loadDlg.open();
        } else {
            loadDlg.bringToTop();
        }
    }

    /**
     * Save configuration action.
     */
    private void handleSaveConfig() {
        if (configMan.getCurrentConfigFile() == null) {
            handleSaveAsConfig();
        } else {
            // configMan.setConfigFile(configMan.getCurrentConfigFile());
            configMan.saveXml();
        }
    }

    /**
     * Save as configuration action.
     */
    private void handleSaveAsConfig() {
        if (saveAsDlg == null || saveAsDlg.isDisposed()) {
            saveAsDlg = new LoadSaveConfigDlg(shell, DialogType.SAVE_AS,
                    configMan.getLocalizationPath(),
                    configMan.getDefaultXMLConfigFileName());
            saveAsDlg.addCloseCallback(new ICloseCallback() {
                @Override
                public void dialogClosed(Object returnValue) {
                    if (returnValue instanceof LocalizationFile) {
                        LocalizationFile fileName = (LocalizationFile) returnValue;
                        configMan.setConfigFile(fileName);
                        configMan.saveXml();
                    }
                }
            });
            saveAsDlg.open();
        } else {
            saveAsDlg.bringToTop();
        }
    }

    /**
     * Delete configuration action.
     */
    private void handleDeleteConfig() {
        if (deleteDlg == null || deleteDlg.isDisposed()) {
            deleteDlg = new LoadSaveConfigDlg(shell, DialogType.DELETE,
                    configMan.getLocalizationPath(), true);
            deleteDlg.addCloseCallback(new ICloseCallback() {
                @Override
                public void dialogClosed(Object returnValue) {
                    if (returnValue instanceof LocalizationFile) {
                        LocalizationFile fileName = (LocalizationFile) returnValue;
                        configMan.deleteXml(fileName);
                        tableChanged();
                    }
                }
            });
            deleteDlg.open();
        } else {
            deleteDlg.bringToTop();
        }
    }

    /**
     * Handle the copy action.
     */
    private void handleCopy() {
        if (!tableComp.verifySingleRowSelected()) {
            return;
        }

        // Get the subscription data
        final Subscription sub = tableComp.getSelectedSubscription();

        ICloseCallback callback = new ICloseCallback() {

            @Override
            public void dialogClosed(Object returnValue) {
                if (returnValue != null) {

                    final String newName = (String) returnValue;

                    if (newName != null && newName.length() > 0
                            && !newName.equals(sub.getName())) {
                        Subscription newSub = sub.copy(newName);

                        // Object is copied, now bring up the edit screen with
                        // the copy
                        tableComp.editSubscription(newSub);
                    }
                }
            }
        };

        FileNameDlg fnd = new FileNameDlg(getShell(), sub.getName());
        fnd.addCloseCallback(callback);

        fnd.open();
    }

    /**
     * Handle the delete action.
     */
    private void handleDelete() {
        int selectionCount = tableComp.getTable().getSelectionCount();
        if (tableComp.getTable().getSelectionCount() == 0) {
            DataDeliveryUtils.showMessage(shell, SWT.ERROR, "No Rows Selected",
                    "Please select a row or rows to Delete");
            return;
        }

        final String permission = DataDeliveryPermission.SUBSCRIPTION_DELETE
                .toString();

        IUser user = UserController.getUserObject();
        String msg = user.uniqueId()
                + " is not authorized to Delete subscriptions.\nPermission: "
                + permission;

        try {
            if (DataDeliveryServices.getPermissionsService()
                    .checkPermission(user, msg, permission).isAuthorized()) {
                ArrayList<SubscriptionManagerRowData> deleteList = new ArrayList<>();
                final List<Subscription> subsToDelete = new ArrayList<>();
                final List<Subscription> subsToUpdate = new ArrayList<>();

                for (int idx : tableComp.getTable().getSelectionIndices()) {
                    SubscriptionManagerRowData removedItem = tableComp
                            .getSubscriptionData().getDataRow(idx);
                    Subscription sub = removedItem.getSubscription();
                    if (sub != null) {
                        if (sub instanceof SharedSubscription) {
                            sub.getOfficeIDs().remove(CURRENT_SITE);
                            if (sub.getOfficeIDs().size() > 0) {
                                subsToUpdate.add(sub);
                            } else {
                                subsToDelete.add(sub);
                            }
                        } else {
                            subsToDelete.add(removedItem.getSubscription());
                        }

                        deleteList.add(removedItem);
                    }
                }

                final ArrayList<SubscriptionManagerRowData> deleted = deleteList;
                String message = getMessage(subsToDelete, subsToUpdate);

                ICloseCallback callback = new ICloseCallback() {

                    @Override
                    public void dialogClosed(Object returnValue) {
                        if (returnValue != null) {

                            int choice = (int) returnValue;

                            if (choice == SWT.YES) {
                                // remove the rows from the table
                                tableComp.getSubscriptionData()
                                        .removeAll(deleted);

                                final String username = LocalizationManager
                                        .getInstance().getCurrentUser();

                                Job job = new Job("Deleting Subscriptions...") {
                                    @Override
                                    protected IStatus run(
                                            IProgressMonitor monitor) {
                                        DataDeliveryGUIUtils
                                                .markBusyInUIThread(shell);
                                        List<RegistryHandlerException> exceptions = new ArrayList<>(
                                                0);
                                        if (!subsToDelete.isEmpty()) {
                                            exceptions = deleteSubscriptions(
                                                    username, subsToDelete);
                                        }
                                        if (!subsToUpdate.isEmpty()) {
                                            exceptions
                                                    .addAll(updateSubscriptions(
                                                            username,
                                                            subsToUpdate));
                                        }
                                        for (RegistryHandlerException t : exceptions) {
                                            statusHandler.handle(Priority.ERROR,
                                                    "Failed to delete some subscriptions: "
                                                            + t.getLocalizedMessage(),
                                                    t);
                                        }

                                        return Status.OK_STATUS;
                                    }
                                };
                                job.addJobChangeListener(
                                        new JobChangeAdapter() {
                                            @Override
                                            public void done(
                                                    IJobChangeEvent event) {
                                                VizApp.runAsync(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (isDisposed()) {
                                                            return;
                                                        }
                                                        handleRefresh();
                                                    }
                                                });

                                                DataDeliveryGUIUtils
                                                        .markNotBusyInUIThread(
                                                                shell);
                                            }
                                        });
                                job.schedule();
                            } else {
                                // Refresh the table to reset any objects edited
                                handleRefresh();
                            }
                        }
                    }
                };

                DataDeliveryUtils.showCallbackMessageBox(shell,
                        SWT.YES | SWT.NO, "Delete Confirmation", message,
                        callback);
            }
        } catch (AuthException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Get the delete confirmation message.
     *
     * @param subsToDelete
     *            subscription list to delete
     * @param subsToUpdate
     *            subscription list to update
     * @return The confirmation message
     */
    private String getMessage(List<Subscription> subsToDelete,
            List<Subscription> subsToUpdate) {
        StringBuilder sb = new StringBuilder();
        if (!subsToDelete.isEmpty()) {
            sb.append("The following subscriptions will be deleted:\n");
            for (Subscription sub : subsToDelete) {
                sb.append(sub.getName()).append("\n");
            }
        }

        if (!subsToUpdate.isEmpty()) {
            sb.append("\nThe following subscriptions will be removed:\n");
            for (Subscription sub : subsToUpdate) {
                sb.append(sub.getName()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Handle filtering the subscription table using combo box selections.
     */
    private void handleFilterSelection() {

        final String group = groupCbo.getText();
        final String office = officeCbo.getText();
        this.selectedOffice = office;
        this.selectedGroup = group;

        tableComp.setSubscriptionFilter(new ISubscriptionManagerFilter() {
            @Override
            public List<Subscription> getSubscriptions(
                    SubscriptionHandler subscriptionHandler)
                    throws RegistryHandlerException {

                if (!office.equals(ALL)) {
                    filter = SubscriptionManagerFilters.getBySiteId(office);
                } else {
                    filter = SubscriptionManagerFilters.getAll();
                }
                final List<Subscription> results = filter
                        .getSubscriptions(subscriptionHandler);

                // Remove any that don't match the configured filters. TODO:
                // This should be cleaned up at some point in the future
                for (Iterator<Subscription> iter = results.iterator(); iter
                        .hasNext();) {
                    Subscription subscription = iter.next();
                    if (group == null || ALL_SUBSCRIPTIONS.equals(group)
                            || group.equals(subscription.getGroupName())) {
                        continue;
                    }
                    iter.remove();
                }
                return results;
            }
        });
        tableComp.populateData();
        tableComp.populateTable();

    }

    /**
     * Handle activating/deactivating the subscription.
     *
     * @param activate
     *            Flag to activate (true) deactivate (false).
     */
    private void handleActivateDeactivate(final boolean activate) {

        int[] selectionIndices = tableComp.getTable().getSelectionIndices();

        if (selectionIndices == null || selectionIndices.length == 0) {
            return;
        }

        // Check for activate permissions
        String permission = DataDeliveryPermission.SUBSCRIPTION_ACTIVATE
                .toString();

        final String actionText = activate ? "Activate" : "Deactivate";

        IUser user = UserController.getUserObject();
        final String username = user.uniqueId().toString();
        String msg = username + " is not authorized to " + actionText
                + " Subscriptions\nPermission: " + permission;

        try {
            if (!DataDeliveryServices.getPermissionsService()
                    .checkPermission(user, msg, permission).isAuthorized()) {
                return;
            }
        } catch (AuthException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
            return;
        }

        final IForceApplyPromptDisplayText forceApplyPromptDisplayText = new CancelForceApplyAndIncreaseLatencyDisplayText(
                actionText, getShell());

        /* Used to reset the cursor only after all jobs are done. */
        final AtomicInteger jobCount = new AtomicInteger();

        /*
         * Queue of subscription name and messages. This is used to aggregate
         * multiple responses and avoid spamming the user with response dialogs.
         */
        final Queue<Pair<String, String>> messages = new ConcurrentLinkedQueue<>();

        /*
         * Responsible for reading the messages queue and displaying all
         * messages available. This is run on the UI thread each time a message
         * is added to the queue. Only one message dialog will be displayed at a
         * time and if multiple messages are already available they will be
         * displayed in a single dialog.
         */
        final Runnable messenger = new Runnable() {

            private SWTMessageBox messageDialog = null;

            @Override
            public void run() {
                if (messageDialog != null) {
                    return;
                }
                Pair<String, String> msgPair = messages.poll();
                if (msgPair == null) {
                    return;
                }
                boolean multiple = false;
                String title = msgPair.getFirst() + " " + actionText + "d";
                StringBuilder message = new StringBuilder(msgPair.getSecond());
                msgPair = messages.poll();
                while (msgPair != null) {
                    multiple = true;
                    message.append("\n").append(msgPair.getSecond());
                    msgPair = messages.poll();
                }
                if (multiple) {
                    title = "Subscriptions " + actionText + "d";
                }
                int remaining = jobCount.get();
                if (remaining == 1) {
                    message.append("\n\n1 subscription is still processing.");
                } else if (remaining > 1) {
                    message.append("\n\n").append(remaining)
                            .append(" subscriptions are still processing.");
                }

                messageDialog = new SWTMessageBox(shell, title,
                        message.toString(), SWT.OK);
                messageDialog.addCloseCallback(new ICloseCallback() {

                    @Override
                    public void dialogClosed(Object returnValue) {
                        messageDialog = null;
                        /*
                         * Must check for more messages since this task ignores
                         * any new calls to run() while the dialog is open.
                         */
                        run();
                    }
                });
                messageDialog.open();
            }
        };

        for (int idx : selectionIndices) {
            SubscriptionManagerRowData rowData = tableComp.getSubscriptionData()
                    .getDataRow(idx);
            if (rowData == null) {
                continue;
            }
            final Subscription<?, ?> sub = rowData.getSubscription();
            if (activate) {
                sub.activate();
            } else {
                sub.deactivate();
            }

            Job job = new Job(actionText + " " + sub.getName() + "...") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        if (jobCount.incrementAndGet() == 1) {
                            DataDeliveryGUIUtils.markBusyInUIThread(shell);
                        }
                        SubscriptionServiceResult response = subscriptionService
                                .update(username, sub,
                                        forceApplyPromptDisplayText);
                        if (!response.isAllowFurtherEditing()) {
                            if (activate) {
                                subscriptionNotificationService
                                        .sendSubscriptionActivatedNotification(
                                                sub, username);
                            } else {
                                subscriptionNotificationService
                                        .sendSubscriptionDeactivatedNotification(
                                                sub, username);
                            }
                        }

                        if (response.hasMessageToDisplay()) {
                            messages.add(new Pair<>(sub.getName(),
                                    response.getMessage()));
                        }
                    } catch (RegistryHandlerException re) {
                        statusHandler
                                .error("Can't activate/deactivate subscription: "
                                        + sub.getName(), re);
                    } finally {
                        /*
                         * Reset the cursor and schedule the messenger. The
                         * reason the messenger is scheduled here is so it can
                         * use the jobCount to decide if it needs to wait for
                         * other jobs to finish.
                         */
                        if (jobCount.decrementAndGet() <= 0) {
                            VizApp.runAsync(messenger);
                            DataDeliveryGUIUtils.markNotBusyInUIThread(shell);
                        } else {
                            /*
                             * Wait a little to give other jobs a chance to
                             * finish so there can be more messages aggregated.
                             */
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                // ignore and go immediately
                            }
                            VizApp.runAsync(messenger);
                        }
                    }

                    return Status.OK_STATUS;
                }
            };
            job.schedule();
        }
    }

    /**
     * Display the table configuration dialog.
     */
    private void handleTableConfiguration() {
        if (configDlg == null || configDlg.isDisposed()) {
            configDlg = new SubscriptionManagerConfigDlg(shell, this);
            configDlg.open();
        } else {
            configDlg.bringToTop();
        }

        if (!this.isDisposed()) {
            handleTooltipSelection(tooltipMI.getSelection());
        }
    }

    /**
     * Handle the help display dialog.
     */
    private void handleHelp() {
        try {
            HelpManager.getInstance().displayHelpDialog(getShell(),
                    SUBSCRIPTION_MANAGER_HELP_FILE);
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Error loading Help Text file: "
                            + SUBSCRIPTION_MANAGER_HELP_FILE,
                    e);
        }
    }

    /**
     * Check whether there are groups available.
     *
     * @return true if there are groups defined
     */
    private boolean thereAreGroupsAvailable() {
        return GroupDefinitionManager.hasGroups();
    }

    /**
     * Turn off and on tool tips.
     */
    private void handleTooltipSelection(boolean toolTipFlag) {

        tableComp.showColumnToolTips(toolTipFlag);

        if (toolTipFlag) {
            activateBtn.setToolTipText("Click to activate subscription(s)");
            deactivateBtn.setToolTipText("Click to deactivate subscription(s)");
        } else {
            activateBtn.setToolTipText(null);
            deactivateBtn.setToolTipText(null);
        }
    }

    /**
     * Load the list of group names available. Default is "All Subscriptions"
     */
    @Override
    public void loadGroupNames() {

        List<String> groupNameList = GroupDefinitionManager.getGroupNames();

        groupNameList.add(0, ALL_SUBSCRIPTIONS);
        String[] groupNames = groupNameList.toArray(new String[0]);
        groupCbo.setItems(groupNames);

        if (this.selectedGroup != null) {
            groupCbo.select(groupNameList.indexOf(selectedGroup));
        } else {
            groupCbo.select(0);
        }
    }

    /**
     * Return the list of office names available. Default is "ALL" office ids
     */
    public void loadOfficeNames() {

        List<String> siteList = DataDeliveryUtils.getDataDeliverySiteList();
        String[] officeNames = siteList.toArray(new String[siteList.size()]);

        officeCbo.setItems(officeNames);

        String site = CURRENT_SITE;
        if (this.selectedOffice != null) {
            for (String item : officeNames) {
                if (item.equals(selectedOffice)) {
                    site = item;
                    break;
                }
            }
        }

        officeCbo.select(officeCbo.indexOf(site));
    }

    @Override
    public void tableChanged() {
        String sortColumnText = null;

        TableColumn sortedTableColumn = tableComp.getSortedTableColumn();

        if (sortedTableColumn != null) {
            sortColumnText = sortedTableColumn.getText();
        }

        TableColumn[] columns = tableComp.getTable().getColumns();
        for (TableColumn column : columns) {
            column.dispose();
        }

        sortedTableColumn = null;

        tableComp.getTable().removeAll();
        tableComp.createColumns();

        if (sortColumnText == null) {
            sortedTableColumn = tableComp.getTable().getColumn(0);
        } else {
            for (TableColumn tc : tableComp.getTable().getColumns()) {
                if (tc.getText().compareTo(sortColumnText) == 0) {
                    sortedTableColumn = tc;
                    break;
                }
            }
        }

        // If null get the first one
        if (sortedTableColumn == null) {
            sortedTableColumn = tableComp.getTable().getColumn(0);
        }
        tableComp.updateSortDirection(sortedTableColumn, false);
        tableComp.populateTable();
    }

    /**
     * Launch the approval dialog.
     */
    private void launchApprovalDlg() {
        // check to see if user if authorized to see pending changes
        if (isApproved()) {
            // Authorized to view
            if (dlg == null || dlg.isDisposed()) {
                dlg = new SubscriptionApprovalDlg(getShell());
                dlg.open();
                dlg = null;
            } else {
                dlg.bringToTop();
            }
        }
    }

    private boolean isApproved() {
        // check to see if user is authorized to see pending changes
        IUser user = UserController.getUserObject();
        try {
            String msg = user.uniqueId()
                    + " is not authorized to access Subscription Approval\nPermission: "
                    + DataDeliveryPermission.SUBSCRIPTION_APPROVE_VIEW
                            .toString();

            return DataDeliveryServices.getPermissionsService()
                    .checkPermissions(user, msg,
                            DataDeliveryPermission.SUBSCRIPTION_APPROVE_SITE
                                    .toString(),
                            DataDeliveryPermission.SUBSCRIPTION_APPROVE_USER
                                    .toString(),
                            DataDeliveryPermission.SUBSCRIPTION_APPROVE_VIEW
                                    .toString())
                    .isAuthorized();
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

        return false;
    }

    /**
     * Deletes a subscription and its associations.
     *
     * @param username
     *
     * @param subscription
     *
     * @return true if successful
     */
    private static List<RegistryHandlerException> deleteSubscriptions(
            String username, List<Subscription> subscriptions) {
        List<RegistryHandlerException> exceptions = new ArrayList<>();

        SubscriptionHandler handler = RegistryObjectHandlers
                .get(SubscriptionHandler.class);
        try {
            handler.delete(username, subscriptions);
        } catch (RegistryHandlerException e) {
            exceptions.add(e);
        }

        return exceptions;
    }

    /**
     * Update subscriptions.
     *
     * @param username
     *            User updating the subscriptions
     * @param subscriptions
     *            Subscriptions to update
     * @return List of errors that occurred
     */
    private static List<RegistryHandlerException> updateSubscriptions(
            String username, List<Subscription> subscriptions) {
        List<RegistryHandlerException> exceptions = new ArrayList<>();

        SubscriptionHandler handler = RegistryObjectHandlers
                .get(SubscriptionHandler.class);
        for (Subscription sub : subscriptions) {
            try {
                handler.update(username, sub);
            } catch (RegistryHandlerException e) {
                exceptions.add(e);
            }
        }

        return exceptions;
    }

    @Override
    public void activateButtonUpdate(String text) {
        activateBtn.setText(text);
    }

    @Override
    public String getGroupNameTxt() {
        return null;
    }

    @Override
    public void tableSelection() {
        // not currently used
    }

    @Override
    public boolean displayYesNoPopup(String title, String message) {
        return DataDeliveryUtils.showYesNoMessage(shell, title,
                message) == SWT.YES;
    }

    @Override
    public void tableLock(boolean isLocked) {
        // no-op
    }

    @Override
    public void updateControls() {
        loadGroupNames();
        loadOfficeNames();
    }

    /**
     * Enable/Disable controls.
     *
     * @param enable
     *            true to enable, false to disable
     */
    private void enableControls(boolean enable) {
        copyMI.setEnabled(enable);
        deleteGroupMI.setEnabled(enable);
        editMI.setEnabled(enable);
        copyMI.setEnabled(enable);
        deleteMI.setEnabled(enable);
        editGroupMI.setEnabled(enable);
        groupMI.setEnabled(enable);
        newMI.setEnabled(enable);
        tableComp.enableMenus(enable);
        activateBtn.setEnabled(enable);
        deactivateBtn.setEnabled(enable);
    }

    /**
     * Private inner work thread used to auto refresh dialog.
     */
    private class RefreshTask implements Runnable {
        @Override
        public void run() {
            if (TimeUtil.currentTimeMillis() - tableComp
                    .getLastUpdateTime() >= TimeUtil.MILLIS_PER_MINUTE * 2) {
                statusHandler
                        .info("Running SubscriptionManager refresh task,lastUpdate: "
                                + new Date(tableComp.getLastUpdateTime()));
                VizApp.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        if (isDisposed()) {
                            return;
                        }
                        handleRefresh();
                    }
                });
            }
        }
    }

}
