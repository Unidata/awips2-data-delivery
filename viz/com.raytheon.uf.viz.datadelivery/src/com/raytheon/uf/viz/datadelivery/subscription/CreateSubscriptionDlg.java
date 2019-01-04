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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.raytheon.uf.common.auth.AuthException;
import com.raytheon.uf.common.auth.req.IPermissionsService;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.datadelivery.bandwidth.data.SubscriptionStatusSummary;
import com.raytheon.uf.common.datadelivery.bandwidth.datasetlatency.DataSetLatencyService;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.InitialPendingSubscription;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.OpenDapGriddedDataSet;
import com.raytheon.uf.common.datadelivery.registry.PointTime;
import com.raytheon.uf.common.datadelivery.registry.SharedSubscription;
import com.raytheon.uf.common.datadelivery.registry.SiteSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription.SubscriptionPriority;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.ebxml.DataSetQuery;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.registry.handlers.PendingSubscriptionHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.SubscriptionHandler;
import com.raytheon.uf.common.datadelivery.request.DataDeliveryConstants;
import com.raytheon.uf.common.datadelivery.request.DataDeliveryPermission;
import com.raytheon.uf.common.datadelivery.service.SendToServerSubscriptionNotificationService;
import com.raytheon.uf.common.registry.ebxml.RegistryUtil;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.registry.handler.RegistryObjectHandlers;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.auth.UserController;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.datadelivery.common.ui.ActivePeriodComp;
import com.raytheon.uf.viz.datadelivery.common.ui.DurationComp;
import com.raytheon.uf.viz.datadelivery.common.ui.PriorityComp;
import com.raytheon.uf.viz.datadelivery.common.ui.VBComp;
import com.raytheon.uf.viz.datadelivery.services.DataDeliveryServices;
import com.raytheon.uf.viz.datadelivery.system.SystemRuleManager;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryGUIUtils;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;
import com.raytheon.viz.ui.dialogs.ICloseCallback;

/**
 * The Data Delivery Create Subscription Dialog.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 09, 2012           mpduff    Initial creation.
 * Mar 29, 2012  431      jpiatt    Edit name update.
 * Jun 21, 2012  736      djohnson  Change OPERATION_STATUS to OperationStatus.
 * Jul 10, 2012  455      djohnson  Disallow creating subscriptions with empty
 *                                  names, don't pull all subscriptions to check
 *                                  for an existing one by name.
 * Jul 16, 2012  702      jpiatt    Modifications for group name.
 * Aug 02, 2012  955      djohnson  Type-safe registry query/responses.
 * Aug 10, 2012  1002     mpduff    Implementing dataset size estimation.
 * Aug 10, 2012  1022     djohnson  {@link DataSetQuery} requires provider name,
 *                                  set NO_GROUP if user doesn't specify a
 *                                  group.
 * Aug 21, 2012  712      mpduff    Add registry ID to subscription objects at
 *                                  creation time
 * Aug 29, 2012  223      mpduff    Add Cycle times for gridded data.  Made to
 *                                  follow MVP pattern.
 * Sep 17, 2012  223      mpduff    Add wait cursor to ok button click.
 * Oct 03, 2012  1103     jpiatt    Changed label.
 * Nov 20, 2012  1286     djohnson  Implement IDisplay to display yes/no prompt.
 * Dec 13, 2012  1391     bgonzale  Added cancel/ok selection status.
 * Jan 02, 2013  1441     djohnson  Add isGroupSelected().
 * Jan 04, 2013  1420     mpduff    Add latency.
 * Jan 25, 2013  1528     djohnson  Use priority enum instead of raw integers.
 * Apr 08, 2013  1826     djohnson  Remove delivery options.
 * May 15, 2013  1040     mpduff    Add Shared sites.
 * Jun 04, 2013  223      mpduff    Modify for point data.
 * Jun 12, 2013  2038     djohnson  No longer modal.
 * Jul 26, 2013  2232     mpduff    Refactored Data Delivery permissions.
 * Aug 21, 2013  1848     mpduff    Check subscription.create and
 *                                  shared.subscription.create.
 * Aug 30, 2013  2288     bgonzale  Added display of priority and latency rules.
 * Sep 04, 2013  2314     mpduff    Pass in the office to Shared Subscription
 *                                  Dialog.
 * Sep 30, 2013  1797     dhladky   separated Time from GriddedTime
 * Oct 11, 2013  2386     mpduff    Refactor DD Front end.
 * Oct 15, 2013  2477     mpduff    Fix bug in group settings.
 * Oct 23, 2013  2484     dhladky   Unique ID for subscriptions updated.
 * Oct 21, 2013  2292     mpduff    Close dialog on OK.
 * Nov 07, 2013  2291     skorolev  Used showText() method for "Unable to Create
 *                                  Subscription" message.
 * Nov 08, 2013  2506     bgonzale  Removed send notification when a
 *                                  subscription is updated and created.
 * Jan 14, 2014  2459     mpduff    Change Subscription status code
 * Feb 11, 2014  2771     bgonzale  Use Data Delivery ID instead of Site.
 * Mar 31, 2014  2889     dhladky   Added username for notification center
 *                                  tracking.
 * May 15, 2014  3113     mpduff    Don't display the gridded cycle composite if
 *                                  no cycles.
 * Aug 18, 2014  2746     ccody     Non-local Subscription changes not updating
 *                                  dialogs
 * Sep 05, 2014  2131     dhladky   Added PDA data type subscriptions
 * Oct 28, 2014  2748     ccody     Remove Live update. Updates are event
 *                                  driven.
 * Dec 01, 2014  3550     ccody     Added extended Latency Processing
 * Jan 05, 2015  3898     ccody     Delete existing Site subscription if it is
 *                                  updated to a Shared Subscription
 * Feb 13, 2015  3852     dhladky   All messaging is done from the BWM and
 *                                  Registry regarding subscriptions.
 * May 17, 2015  4047     dhladky   Verified non-blocking.
 * Oct 15, 2015  4657     rferrel   Make blocking so parent dialog stays busy.
 * Feb 01, 2016  5289     tgurney   Add missing minimize button in trim
 * Mar 15, 2016  5482     randerso  Fix GUI sizing issues
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces.
 * Nov 08, 2016  5976     bsteffen  Use VizApp for GUI execution.
 * Apr 25, 2017  1045     tjensen   Create Adhoc subscriptions for latest when
 *                                  creating a recurring subscription
 * Jun 09, 2017  746      bsteffen  Handle group conflicts with duration and period.
 * Aug 31, 2017  6396     nabowle   Fix SharedSubscription creation.
 * Oct 26, 2017  6461     tgurney   Add ICreateAdhocCallback
 * Dec 08, 2017  6355     nabowle   Add VBComp.
 * Feb 08, 2018  6451     nabowle   Require selected cycle times if available.
 *                                  Select All The Cycles by default.
 * Jan 03, 2019  7503     troberts  Remove subscription grouping capabilities.
 * Jan 08, 2019  7330     troberts  Made latency for PDA subscriptions editable.
 *
 * </pre>
 *
 * @author mpduff
 */
public class CreateSubscriptionDlg extends CaveSWTDialog {
    /** Status Handler */
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(CreateSubscriptionDlg.class);

    /** Pending approval message */
    private static final String PENDING_APPROVAL_MESSAGE = "The subscription is awaiting approval.\n\n"
            + "A notification message will be generated upon approval.";

    /** Subscription retrieval service */
    private static SubscriptionService subscriptionService = DataDeliveryServices
            .getSubscriptionService();

    /** Subscription notification service */
    private final SendToServerSubscriptionNotificationService subscriptionNotificationService = DataDeliveryServices
            .getSubscriptionNotificationService();

    /** Constant */
    private static final String UPDATED_TITLE = "Subscription Updated";

    /** The Main Composite */
    private Composite mainComp;

    /** The Subscription Duration Composite */
    private DurationComp durComp;

    /** The Subscription Duration Composite */
    private ActivePeriodComp activePeriodComp;

    /** The Subscription Duration Composite */
    private PriorityComp priorityComp;

    /** Description text field */
    private Text descNameTxt;

    /** Change reason text field */
    private Text changeReasonTxt;

    /** Subscription Name text field */
    private Text subNameTxt;

    private VBComp vbcomp;

    /** Create/edit flag */
    private final boolean create;

    /** Hour button */
    private Button[] hourBtnArr;

    /** Did the user Status.OK or SWT.CANCEL subscription creation */
    private int status = SWT.NONE;

    /** The subscription object */
    private Subscription<Time, Coverage> subscription;

    /** Available cycle times */
    private Set<Integer> cycleTimes;

    private String[] sharedSites;

    private Label selectedSiteLbl;

    private final Font font;

    /** The dataset object */
    private final DataSet dataSet;

    private ICreateAdhocCallback adhocCallback;

    public static interface ICreateAdhocCallback {
        public String storeAdhocFromRecurring(
                CreateSubscriptionDlg subscriptionDlg);
    }

    /**
     * Constructor.
     *
     * @param parent
     *            The parent shell
     * @param create
     *            true for new subscription, false for edit
     * @param dataSet
     *            The data set object
     * @param guiThreadTaskExecutor
     *            The task executor thread
     */
    public CreateSubscriptionDlg(Shell parent, boolean create, DataSet dataSet,
            ICreateAdhocCallback adhocCallback) {
        // Make blocking so parent shell stays busy until the dialog closes.
        super(parent, SWT.DIALOG_TRIM | SWT.MIN,
                CAVE.INDEPENDENT_SHELL | CAVE.PERSPECTIVE_INDEPENDENT);
        this.create = create;
        this.dataSet = dataSet;
        this.adhocCallback = adhocCallback;

        if (create) {
            setText("Create Subscription");
        } else {
            setText("Edit Subscription");
        }

        font = new Font(this.getDisplay(), "Monospace", 9, SWT.NORMAL);
    }

    @Override
    protected void initializeComponents(Shell shell) {
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(1, false);

        mainComp = new Composite(shell, SWT.NONE);
        mainComp.setLayout(gl);
        mainComp.setLayoutData(gd);

        createSubscriptionInfoGroup();

        durComp = new DurationComp(mainComp);
        activePeriodComp = new ActivePeriodComp(mainComp);

        // Get latency value
        int latency = 15;
        SubscriptionPriority priority = SubscriptionPriority.NORMAL;
        SystemRuleManager ruleManager = SystemRuleManager.getInstance();
        // rule values
        SubscriptionPriority priorityRule = null;
        int latencyRule = 0;

        if (this.subscription.getDataSetType() == DataType.GRID) {
            // cycle times
            cycleTimes = Sets
                    .newTreeSet(((OpenDapGriddedDataSet) dataSet).getCycles());
            latencyRule = ruleManager.getLatency(subscription, cycleTimes);
            priorityRule = ruleManager.getPriority(subscription, cycleTimes);
        } else if (this.subscription.getDataSetType() == DataType.POINT) {
            // For point the latency is the retrieval interval
            latencyRule = ((PointTime) subscription.getTime()).getInterval();
            priorityRule = ruleManager.getPointDataPriority(subscription);
        } else if (this.subscription.getDataSetType() == DataType.PDA) {
            // For PDA the latency is static
            latencyRule = ruleManager.getPDADataLatency(subscription);
            priorityRule = ruleManager.getPDADataPriority(subscription);
        }

        if (this.create) {
            latency = latencyRule;
            priority = priorityRule;
        } else {
            latency = subscription.getLatencyInMinutes();
            priority = subscription.getPriority();
        }

        priorityComp = new PriorityComp(mainComp, latencyRule, latency,
                priorityRule, priority);

        if (this.subscription.getDataSetType() == DataType.GRID) {
            if (!CollectionUtil.isNullOrEmpty(cycleTimes)) {
                this.createCycleGroup();
            }
        }

        createSiteSelection();

        if (!create) {
            createChangeText();
        }

        createVbComp();

        createButtons();

        populate();
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
     * Create the Subscription Information Group
     */
    private void createSubscriptionInfoGroup() {
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(2, false);

        Group subInfoGroup = new Group(mainComp, SWT.NONE);
        subInfoGroup.setLayout(gl);
        subInfoGroup.setLayoutData(gd);
        subInfoGroup.setText("Subscription Information");

        Label subName = new Label(subInfoGroup, SWT.NONE);
        subName.setText("Name: ");

        // If in Edit mode do not allow Subscription Name to be changed
        subNameTxt = new Text(subInfoGroup,
                SWT.BORDER | (create ? SWT.None : SWT.READ_ONLY));
        if (!create) {
            subNameTxt.setBackground(subInfoGroup.getBackground());
        }

        GC gc = new GC(subNameTxt);
        int textWidth = gc.getFontMetrics().getAverageCharWidth() * 40;
        gc.dispose();
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gd.widthHint = textWidth;
        subNameTxt.setLayoutData(gd);

        Label descName = new Label(subInfoGroup, SWT.NONE);
        descName.setText("Description: ");

        descNameTxt = new Text(subInfoGroup, SWT.BORDER);
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gd.widthHint = textWidth;
        descNameTxt.setLayoutData(gd);
    }

    private void createChangeText() {
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(1, false);

        Group reasonGroup = new Group(mainComp, SWT.NONE);
        reasonGroup.setLayout(gl);
        reasonGroup.setLayoutData(gd);
        reasonGroup.setText("Reason for Change");

        Label descName = new Label(reasonGroup, SWT.NONE);
        descName.setText("Reason for Requesting Change: ");

        changeReasonTxt = new Text(reasonGroup, SWT.BORDER);
        GC gc = new GC(changeReasonTxt);
        int textWidth = gc.getFontMetrics().getAverageCharWidth() * 60;
        gc.dispose();
        changeReasonTxt.setLayoutData(new GridData(textWidth, SWT.DEFAULT));
    }

    private void createVbComp() {
        if (this.subscription.getDataSetType() == DataType.GRID) {
            GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
            GridLayout gl = new GridLayout(1, false);

            Group vbGroup = new Group(mainComp, SWT.NONE);
            vbGroup.setLayout(gl);
            vbGroup.setLayoutData(gd);
            vbGroup.setText("Volume Browser");

            vbcomp = new VBComp(vbGroup);
        }
    }

    /**
     * Create the site selection widgets.
     */
    private void createSiteSelection() {
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(2, false);

        final Group group = new Group(mainComp, SWT.NONE);
        group.setLayout(gl);
        group.setLayoutData(gd);
        group.setText("Shared Sites");

        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false);
        gl = new GridLayout(2, false);
        final Composite c = new Composite(group, SWT.NONE);
        c.setLayout(gl);
        c.setLayoutData(gd);

        final Button btn = new Button(c, SWT.NONE);
        btn.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT));
        btn.setText("Select Sites...");
        btn.setToolTipText("Select sites for sharing");
        btn.setEnabled(false);

        final String createSharedSubPermission = DataDeliveryPermission.SHARED_SUBSCRIPTION_CREATE
                .toString();
        final String createSubPermission = DataDeliveryPermission.SUBSCRIPTION_CREATE
                .toString();

        final IUser user = UserController.getUserObject();
        final String msg = user.uniqueId()
                + " is not authorized to create shared subscriptions. "
                + StringUtil.NEWLINE + "Permission: "
                + createSharedSubPermission;
        try {
            if (DataDeliveryServices.getPermissionsService()
                    .checkPermissions(user, msg, createSharedSubPermission,
                            createSubPermission)
                    .hasPermission(createSharedSubPermission)) {
                btn.setEnabled(true);
            } else {
                c.addMouseTrackListener(new MouseTrackAdapter() {
                    @Override
                    public void mouseExit(MouseEvent e) {
                        DataDeliveryGUIUtils.hideToolTip();
                    }

                    @Override
                    public void mouseHover(MouseEvent e) {
                        handleMouseEvent(e, msg, group.getBounds());
                    }

                    @Override
                    public void mouseEnter(MouseEvent e) {
                        handleMouseEvent(e, msg, group.getBounds());
                    }
                });
            }
        } catch (AuthException e1) {
            statusHandler.handle(Priority.PROBLEM, e1.getLocalizedMessage(),
                    e1);
        }
        btn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String currentSite = DataDeliveryUtils.getDataDeliveryId();
                /*
                 * If there are no shared sites, default selection to the
                 * current site.
                 */
                String[] ssites = sharedSites;
                if (ssites == null || ssites.length == 0) {
                    ssites = new String[] { currentSite };
                }
                SiteSelectionDlg dlg = new SiteSelectionDlg(shell, currentSite,
                        ssites);
                dlg.addCloseCallback(new ICloseCallback() {
                    @Override
                    public void dialogClosed(Object returnValue) {
                        if (returnValue instanceof String[]) {
                            String[] sites = (String[]) returnValue;
                            processSites(sites);
                        }
                    }
                });
                dlg.open();
            }
        });

        selectedSiteLbl = new Label(group, SWT.BORDER);
        selectedSiteLbl.setFont(font);
        selectedSiteLbl.setText("");
        selectedSiteLbl
                .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        if (!create) {
            if (subscription != null
                    && subscription.getOfficeIDs().size() > 0) {
                String[] siteArr = subscription.getOfficeIDs().toArray(
                        new String[subscription.getOfficeIDs().size()]);
                processSites(siteArr);
            }
        }

    }

    /**
     * Create the bottom buttons
     */
    private void createButtons() {
        GridData gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(2, true);
        Composite btnComp = new Composite(mainComp, SWT.NONE);
        btnComp.setLayout(gl);
        btnComp.setLayoutData(gd);

        int buttonWidth = btnComp.getDisplay().getDPI().x;
        Button okBtn = new Button(btnComp, SWT.PUSH);
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gd.minimumWidth = buttonWidth;
        okBtn.setLayoutData(gd);
        okBtn.setText("OK");
        okBtn.setEnabled(true);
        okBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    status = Status.OK;
                    getShell().setCursor(
                            getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
                    if (handleOkAction()) {
                        close();
                    }
                } finally {
                    if (!getShell().isDisposed()) {
                        getShell().setCursor(null);
                    }
                }
            }
        });

        Button cancelBtn = new Button(btnComp, SWT.PUSH);
        cancelBtn.setText("Cancel");
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gd.minimumWidth = buttonWidth;
        cancelBtn.setLayoutData(gd);
        cancelBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                status = SWT.CANCEL;
                close();
            }
        });
    }

    /**
     * Create the cycle group portion of the subscription view.
     */
    public void createCycleGroup() {
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(1, false);

        Group cycleGroup = new Group(mainComp, SWT.NONE);
        cycleGroup.setLayout(gl);
        cycleGroup.setLayoutData(gd);
        cycleGroup.setText("Model Cycle Times");

        gl = new GridLayout(8, false);
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        Composite cycleComp = new Composite(cycleGroup, SWT.NONE);
        cycleComp.setLayout(gl);
        cycleComp.setLayoutData(gd);

        hourBtnArr = new Button[cycleTimes.size()];
        int i = 0;
        for (int cycle : cycleTimes) {
            String hour = Strings.padStart(String.valueOf(cycle), 2, '0');
            Button btn = new Button(cycleComp, SWT.CHECK);
            btn.setText(hour);
            btn.setSelection(this.create);
            hourBtnArr[i] = btn;
            i++;
        }

        gl = new GridLayout(2, true);
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false);
        Composite selectAllComp = new Composite(cycleGroup, SWT.NONE);
        selectAllComp.setLayout(gl);
        selectAllComp.setLayoutData(gd);

        GridData btnData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        Button selectAllBtn = new Button(selectAllComp, SWT.PUSH);
        selectAllBtn.setLayoutData(btnData);
        selectAllBtn.setText("Select All");
        selectAllBtn.setToolTipText("Select all cycle times");
        selectAllBtn.setEnabled(true);
        selectAllBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (Button btn : hourBtnArr) {
                    btn.setSelection(true);
                }
            }
        });

        btnData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        Button deselectAllBtn = new Button(selectAllComp, SWT.PUSH);
        deselectAllBtn.setLayoutData(btnData);
        deselectAllBtn.setText("Deselect All");
        deselectAllBtn.setToolTipText("Deselect all cycle times");
        deselectAllBtn.setEnabled(true);
        deselectAllBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (Button btn : hourBtnArr) {
                    btn.setSelection(false);
                }
            }
        });
    }

    /**
     * Handle the mouse event and display the tooltip.
     *
     * @param e
     *            MouseEvent
     * @param msg
     *            Message to display
     * @param bounds
     *            Bounds
     */
    private void handleMouseEvent(MouseEvent e, String msg, Rectangle bounds) {
        Point pos = shell.toDisplay(bounds.x + e.x + 15, bounds.y + e.y + 15);
        DataDeliveryGUIUtils.showTooltip(this.shell, pos.x, pos.y, msg);
    }

    @Override
    protected void disposed() {
        super.disposed();
        if (font != null && !font.isDisposed()) {
            font.dispose();
        }
    }

    /**
     * Get the subscription Name.
     *
     * @return
     */
    private String getSubscriptionName() {
        return this.subNameTxt.getText().trim();
    }

    /**
     * Set the subscription name.
     *
     * @param subscriptionName
     *            The subscription name
     */
    public void setSubscriptionName(String subscriptionName) {
        if (subscriptionName != null) {
            this.subNameTxt.setText(subscriptionName);
        }
    }

    /**
     * Get the subscription description
     *
     * @return
     */
    private String getSubscriptionDescription() {
        return this.descNameTxt.getText().trim();
    }

    /**
     * Set the subscription description.
     *
     * @param subscriptionDescription
     *            description of the subscription
     */
    public void setSubscriptionDescription(String subscriptionDescription) {
        descNameTxt.setText(subscriptionDescription);
    }

    /**
     * Set the no expiration date checkbox
     *
     * @param noExpiration
     *            true of the subscription does not expire
     */
    public void setNoExpiration(boolean noExpiration) {
        durComp.setNoExpiration(noExpiration);
    }

    /**
     * Set the start date
     *
     * @param startDate
     *            Start date of the subscription
     */
    public void setStartDate(Date startDate) {
        durComp.setStartDate(startDate);
    }

    /**
     * Set the expiration date
     *
     * @param expDate
     *            expiration date of the subscription
     */
    public void setExpirationDate(Date expDate) {
        durComp.setEndDate(expDate);
    }

    /**
     * Set the always active selection.
     *
     * @param active
     *            true if the subscription is always active
     */
    public void setAlwaysActive(boolean active) {
        activePeriodComp.setAlwaysActive(active);
    }

    /**
     * Set the active start date
     *
     * @param activeStartDate
     *            Date the subscription becomes active
     */
    public void setActiveStartDate(Date activeStartDate) {
        activePeriodComp.setStartDate(activeStartDate);
    }

    /**
     * Set the active end date
     *
     * @param activeEndDate
     *            Date the subscription stops being active
     */
    public void setActiveEndDate(Date activeEndDate) {
        activePeriodComp.setEndDate(activeEndDate);
    }

    /**
     * Set the priority selection
     *
     * @param priority
     *            Priority of the subscription
     */
    public void setPriority(SubscriptionPriority priority) {
        priorityComp.setPriority(priority);
    }

    /**
     * Set the subscription date fields enabled/disabled
     *
     * @param enabled
     *            true if subscription dates enabled
     */
    public void setSubscriptionDatesEnabled(boolean enabled) {
        this.durComp.resetTextBoxes(enabled);
    }

    /**
     * Set the start date button enabled/disabled
     *
     * @param enabled
     *            true if start date button is enabled
     */
    public void setStartDateBtnEnabled(boolean enabled) {
        durComp.setStartBtnEnabled(enabled);
    }

    /**
     * Set the end date button enabled/disabled
     *
     * @param enabled
     *            true if end date button is enabled
     */
    public void setEndDateBtnEnabled(boolean enabled) {
        durComp.setEndBtnEnabled(enabled);
    }

    /**
     * Set the active date fields enabled/disabled
     *
     * @param enabled
     *            true if active dates buttons are enabled
     */
    public void setActiveDatesEnabled(boolean enabled) {
        this.activePeriodComp.resetTextBoxes(enabled);
    }

    /**
     * Set the active end date button enabled/disabled
     *
     * @param enabled
     *            true if active end date button is enabled
     */
    public void setActiveEndDateBtnEnabled(boolean enabled) {
        this.activePeriodComp.setEndBtnEnabled(enabled);
    }

    /**
     * Set the active start date button enabled/disabled
     *
     * @param enabled
     *            true if active start date button is enabled
     */
    public void setActiveStartDateBtnEnabled(boolean enabled) {
        this.activePeriodComp.setStartBtnEnabled(enabled);
    }

    /**
     * Close the view dialog
     */
    public void closeDlg() {
        this.close();
    }

    /**
     * Display a popup message
     *
     * @param title
     *            The title
     * @param message
     *            The message
     */
    public void displayPopup(String title, String message) {
        DataDeliveryUtils.showMessage(getShell(), SWT.OK, title, message);
    }

    /**
     * Display an error popup.
     *
     * @param title
     *            The title
     * @param message
     *            The message
     */
    private void displayErrorPopup(String title, String message) {
        DataDeliveryUtils.showMessage(shell, SWT.ERROR, title, message);
    }

    /**
     * Get list of selected cycle times
     *
     * @return Cycle times
     */
    private List<Integer> getCycleTimes() {
        ArrayList<Integer> cycleList = new ArrayList<>();
        if (hourBtnArr != null) {
            for (Button b : hourBtnArr) {
                if (b.getSelection()) {
                    cycleList.add(Integer.parseInt(b.getText()));
                }
            }
        }

        return cycleList;
    }

    /**
     * Set the date text field enabled.
     *
     * @param flag
     *            true for enabled
     */
    public void setDateTxtFieldsEnabled(boolean flag) {
        this.durComp.resetTextBoxes(flag);
    }

    /**
     * Set the active text field enabled.
     *
     * @param flag
     *            true for enabled
     */
    public void setActiveTextFieldsEnabled(boolean flag) {
        this.activePeriodComp.resetTextBoxes(flag);
    }

    /**
     * Select the provided cycles.
     *
     * @param cycleStrings
     *            List of cycles to select
     */
    public void selectCycles(List<String> cycleStrings) {
        for (Button b : this.hourBtnArr) {
            if (cycleStrings.contains(b.getText())) {
                b.setSelection(true);
            }
        }
    }

    /**
     * The status of user selection.
     *
     * @return The user's selected status
     */
    public int getStatus() {
        return status;
    }

    /**
     * Set the user status.
     *
     * @param status
     *            The status
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Set the subscription.
     *
     * @param subscription
     *            The subscription to use
     */
    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    /**
     * Process the site list
     *
     * @param sites
     *            list of sites
     */
    private void processSites(String[] sites) {
        this.sharedSites = sites;
        StringBuilder toolTipText = new StringBuilder();
        StringBuilder labelText = new StringBuilder();
        boolean overflow = false;
        for (int i = 0; i < sites.length; i++) {
            toolTipText.append(sites[i]).append(" ");
            if (i < 8) {
                labelText.append(sites[i]).append(" ");
            } else {
                overflow = true;
            }
        }
        String lt = labelText.toString().trim();
        if (!lt.isEmpty() && overflow) {
            lt = lt.concat("...");
        }

        selectedSiteLbl.setText(lt);
        selectedSiteLbl.setToolTipText(toolTipText.toString());
    }

    /**
     * Get the shared sites.
     *
     * @return Array of shared site ids
     */
    public String[] getSharedSites() {
        return this.sharedSites;
    }

    /**
     * Set the office Ids.
     *
     * @param officeIDs
     *            The office ids to set
     */
    public void setOfficeIds(Set<String> officeIDs) {
        List<String> list = new ArrayList<>(officeIDs);
        this.sharedSites = list.toArray(new String[officeIDs.size()]);
    }

    /**
     * The OK Button action.
     *
     * @return
     */
    private boolean handleOkAction() {
        if (!validate()) {
            return false;
        }
        String currentSite = DataDeliveryUtils.getDataDeliveryId();

        Subscription cachedSiteSubscription = null;
        Set<String> sitesSet = sharedSites == null ? Collections.emptySet()
                : Sets.newHashSet(sharedSites);
        /*
         * Create a shared subscription when multiple sites are selected, or a
         * single remote site is chosen.
         */
        if (sitesSet.size() > 1
                || (sitesSet.size() == 1 && !sitesSet.contains(currentSite))) {
            SharedSubscription sharedSub = new SharedSubscription(subscription);
            sharedSub.setRoute(Network.SBN);
            Set<String> officeList = Sets.newHashSet(sharedSites);
            sharedSub.setOfficeIDs(officeList);
            // Cache Existing Site Subscription for deletion.
            // This is only for updates from Site Subscription TO Shared
            // Subscription
            if (subscription instanceof SiteSubscription) {
                cachedSiteSubscription = subscription;
            }
            subscription = sharedSub;
        } else {
            Set<String> officeList = Sets.newHashSet();
            officeList.add(currentSite);
            subscription.setOfficeIDs(officeList);
        }

        // Populate the subscription with the dialog inputs
        populateSubscriptionFromInputs();

        IUser user = UserController.getUserObject();

        PendingSubscriptionHandler handler = DataDeliveryHandlers
                .getPendingSubscriptionHandler();

        String currentUser = LocalizationManager.getInstance().getCurrentUser();
        final String username = user.uniqueId().toString();

        // Check for permission
        boolean autoApprove = checkApprovalPermisssions(user);

        if (this.create) {
            cachedSiteSubscription = null;

            setSubscriptionId(subscription);
            if (autoApprove) {
                final BlockingQueue<SubscriptionStatusSummary> exchanger = new ArrayBlockingQueue<>(
                        1);

                final Shell jobShell = getShell();
                Job job = new Job("Creating Subscription...") {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        DataDeliveryGUIUtils.markBusyInUIThread(jobShell);
                        SubscriptionServiceResult result = storeSubscription(
                                subscription, username);
                        if (result != null) {
                            if (result.isAllowFurtherEditing()) {
                                return new Status(Status.CANCEL,
                                        CreateSubscriptionDlg.class.getName(),
                                        result.getMessage());
                            }
                            SubscriptionStatusSummary sum = result
                                    .getSubscriptionStatusSummary();

                            exchanger.add(sum);

                            // Also schedule an immediate adhoc for the latest
                            String queryStatus = adhocCallback
                                    .storeAdhocFromRecurring(
                                            CreateSubscriptionDlg.this);

                            return new Status(Status.OK,
                                    CreateSubscriptionDlg.class.getName(),
                                    result.getMessage() + " " + queryStatus);
                        }
                        return new Status(Status.ERROR,
                                CreateSubscriptionDlg.class.getName(),
                                "Error Storing Subscription");
                    }
                };
                job.addJobChangeListener(new JobChangeAdapter() {
                    @Override
                    public void done(final IJobChangeEvent event) {
                        try {
                            final IStatus status = event.getResult();
                            final boolean subscriptionCreated = status.isOK();

                            if (!Strings.isNullOrEmpty(status.getMessage())) {
                                VizApp.runAsync(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!isDisposed()) {
                                            if (subscriptionCreated) {
                                                try {
                                                    displaySummary(
                                                            exchanger.take(),
                                                            status.getMessage());

                                                } catch (InterruptedException e) {
                                                    statusHandler.handle(
                                                            Priority.PROBLEM,
                                                            e.getLocalizedMessage(),
                                                            e);
                                                }
                                                setStatus(Status.OK);
                                                close();
                                            } else {
                                                setStatus(Status.CANCEL);
                                                DataDeliveryUtils.showText(
                                                        getShell(),
                                                        "Unable to Create Subscription",
                                                        status.getMessage());
                                            }
                                        }
                                    }
                                });
                            }
                        } finally {
                            DataDeliveryGUIUtils
                                    .markNotBusyInUIThread(jobShell);
                        }
                    }
                });
                job.schedule();

                return false;
            }
            // Create: Auto Approve == false
            InitialPendingSubscription pendingSub = subscription
                    .initialPending(currentUser);
            try {
                handler.store(username, pendingSub);

                this.subscription = pendingSub;

                subscriptionNotificationService
                        .sendCreatedPendingSubscriptionNotification(pendingSub,
                                username);
            } catch (RegistryHandlerException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to create pending subscription.", e);
            }
        } else {
            /*
             * Update Existing Subscription. Check for pending subscription, can
             * only have one pending change
             */

            // Create the registry ids
            setSubscriptionId(subscription);

            PendingSubscriptionHandler pendingSubHandler = RegistryObjectHandlers
                    .get(PendingSubscriptionHandler.class);
            try {
                InitialPendingSubscription result = pendingSubHandler
                        .getBySubscription(subscription);
                if (result != null) {
                    String msg = "There is already an edited version of this subscription.\n\nPlease "
                            + "reconcile the pending subscription before making further edits.";
                    displayPopup("Pending", msg);
                    return false;
                }
            } catch (RegistryHandlerException e1) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to retrieve pending subscriptions.", e1);
                return false;
            }

            if (autoApprove) {
                try {
                    final SubscriptionServiceResult response = subscriptionService
                            .update(username, subscription,
                                    new CancelForceApplyAndIncreaseLatencyDisplayText(
                                            "update", getShell()));

                    /*
                     * If this save promotes a Site to a Shared; then Delete
                     * Existing SiteSubscription
                     */
                    if (cachedSiteSubscription != null) {
                        deleteSubscription(username, cachedSiteSubscription);
                    }

                    /*
                     * Subscription Data Latency operates off of Provider and
                     * Data Set Name; it is not directly tied to the
                     * Subscription.
                     */
                    resetSubscriptionDataSetLatency(subscription);
                    if (response.hasMessageToDisplay()) {
                        displayPopup(UPDATED_TITLE, response.getMessage());
                    }

                    /*
                     * If there was a force apply prompt, and the user selects
                     * no, then we want to allow them to continue editing the
                     * subscription
                     */
                    if (response.isAllowFurtherEditing()) {
                        return false;
                    }

                    if (cachedSiteSubscription != null) {
                        String responseMessage = response.getMessage();
                        if (responseMessage != null && responseMessage
                                .indexOf(" has been updated") > -1) {
                        }
                    }
                } catch (RegistryHandlerException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Unable to update subscription.", e);
                }
            } else {
                // Update Pending Subscription
                InitialPendingSubscription pendingSub = subscription
                        .initialPending(currentUser);
                pendingSub
                        .setChangeReason(this.changeReasonTxt.getText().trim());

                // Create the registry ids
                setSubscriptionId(pendingSub);
                setSubscriptionId(subscription);
                try {
                    pendingSubHandler.update(username, pendingSub);

                    subscriptionNotificationService
                            .sendCreatedPendingSubscriptionForSubscriptionNotification(
                                    pendingSub, username);

                    final String msg = PENDING_APPROVAL_MESSAGE;
                    displayPopup("Subscription Pending", msg);
                } catch (RegistryHandlerException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Unable to create pending subscription.", e);
                }
            }
        }

        return true;
    }

    private boolean checkApprovalPermisssions(IUser user) {
        IPermissionsService permissionsService = DataDeliveryServices
                .getPermissionsService();
        boolean autoApprove = false;
        if (permissionsService instanceof RequestFromServerPermissionsService) {
            try {
                /*
                 * check to see if user is authorized to approve. If so then
                 * auto-approve
                 */
                autoApprove = ((RequestFromServerPermissionsService) permissionsService)
                        .checkPermissionToChangeSubscription(user,
                                PENDING_APPROVAL_MESSAGE, subscription)
                        .isAuthorized();
            } catch (AuthException e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        }
        return autoApprove;
    }

    private void populateSubscriptionFromInputs() {
        // Data are valid, now add info to the subscription object and store
        subscription.setProvider(dataSet.getProviderName());

        if (this.durComp.isIndefiniteChk()) {
            Calendar cal = TimeUtil.newGmtCalendar();
            subscription.setSubscriptionStart(cal.getTime());
            subscription.setSubscriptionEnd(null);
        } else {
            try {

                String startText = this.durComp.getStartText();
                String endText = this.durComp.getEndText();

                if (!startText.isEmpty()) {
                    Date startDate = DataDeliveryGUIUtils
                            .getSubscriptionFormat().parse(startText);
                    subscription.setSubscriptionStart(startDate);
                }
                if (!endText.isEmpty()) {
                    Date endDate = DataDeliveryGUIUtils.getSubscriptionFormat()
                            .parse(endText);
                    subscription.setSubscriptionEnd(endDate);
                }
            } catch (ParseException e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        }

        // active period
        if (!this.activePeriodComp.isAlwaysChk()) {
            try {
                String startText = this.activePeriodComp.getActiveStartText();
                String endText = this.activePeriodComp.getActiveEndText();

                if (!startText.isEmpty()) {
                    Date startPeriodDate = DataDeliveryGUIUtils
                            .getActiveFormat().parse(startText);
                    subscription.setActivePeriodStart(startPeriodDate);
                }
                if (!endText.isEmpty()) {
                    Date endPeriodDate = DataDeliveryGUIUtils.getActiveFormat()
                            .parse(endText);
                    subscription.setActivePeriodEnd(endPeriodDate);
                }
            } catch (ParseException e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        } else {
            subscription.setActivePeriodStart(null);
            subscription.setActivePeriodEnd(null);
        }

        // priority
        SubscriptionPriority priority = priorityComp.getPriority();
        subscription.setPriority(priority);

        subscription.setName(getSubscriptionName());

        subscription.setDescription(getSubscriptionDescription());

        if (this.dataSet.getDataSetType() == DataType.GRID) {
            ((GriddedTime) subscription.getTime())
                    .setCycleTimes(getCycleTimes());
        }

        subscription.setLatencyInMinutes(priorityComp.getLatencyValue());

        if (vbcomp != null) {
            subscription.setVertical(vbcomp.isVertical());
        }
    }

    /**
     * Validate the dialog's data
     *
     * @return true if valid
     */
    private boolean validate() {
        boolean valid = false;
        boolean datesValid = false;
        boolean activeDatesValid = false;
        boolean groupDurValid = false;
        boolean groupActiveValid = false;
        boolean latencyValid = false;
        boolean cyclesValid = true;

        // Validate the date entries
        datesValid = this.durationValidChk();
        activeDatesValid = this.activePeriodValidChk();
        int maxLatency = 0;
        if (subscription.getDataSetType() == DataType.POINT
                || subscription.getDataSetType() == DataType.PDA) {
            maxLatency = DataDeliveryUtils.getMaxLatency(subscription);
        } else if (subscription.getDataSetType() == DataType.GRID) {
            maxLatency = DataDeliveryUtils.getMaxLatency(getCycleTimes());
        }
        latencyValid = DataDeliveryGUIUtils
                .latencyValidChk(priorityComp.getLatencyValue(), maxLatency);
        if (!latencyValid) {
            displayErrorPopup("Invalid Latency",
                    "Invalid latency value entered.\n\n"
                            + "Please enter a value in minutes between 0 and "
                            + maxLatency);
        }

        if (subscription.getDataSetType() == DataType.GRID) {
            if (this.cycleTimes != null && !this.cycleTimes.isEmpty()) {
                List<Integer> selectedCycles = getCycleTimes();
                if (selectedCycles.isEmpty()) {
                    displayErrorPopup("Cycle Required",
                            "No cycle times were selected.\n\n"
                                    + "Please select at least one of the available cycles.");
                    cyclesValid = false;
                }
            }
        }

        // Validate the subscription name if entered into text box
        String subscriptionName = getSubscriptionName();
        if (this.create) {

            // Is Subset Name entered
            if (subscriptionName == null || subscriptionName.isEmpty()) {
                displayErrorPopup(DataDeliveryGUIUtils.NAME_REQUIRED_TITLE,
                        DataDeliveryGUIUtils.NAME_REQUIRED_MESSAGE);
                return false;
            }

            if (!DataDeliveryGUIUtils.VALID_CHAR_PATTERN
                    .matcher(subscriptionName.trim()).find()
                    || subscriptionName.trim().contains("  ")) {
                displayErrorPopup(DataDeliveryGUIUtils.INVALID_CHARS_TITLE,
                        DataDeliveryGUIUtils.INVALID_CHARS_MESSAGE);

                return false;
            }

            // Check for existing subscription
            SubscriptionHandler handler = RegistryObjectHandlers
                    .get(SubscriptionHandler.class);
            try {
                if (handler.getByName(subscriptionName) != null) {
                    String message = "A subscription with this name already exists.\n\nPlease enter a different subscription name.";
                    displayPopup("Duplicate Subscription", message);
                    this.subNameTxt.selectAll();
                    return false;
                }
            } catch (RegistryHandlerException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to check for an existing subscription by name.",
                        e);
            }
        }

        if (activeDatesValid && datesValid && latencyValid && cyclesValid) {
            valid = true;
        }

        // If valid is not set to true for any of the composites return
        if (!valid) {
            return false;
        }

        return true;
    }

    /**
     * Check if duration dates are valid
     *
     * @return true if valid
     */
    private boolean durationValidChk() {
        boolean datesValid = false;
        boolean dateOrderValid = false;

        if (this.durComp.isIndefiniteChk()) {
            datesValid = true;
            dateOrderValid = true;
        } else {
            boolean validateDur = DataDeliveryGUIUtils.validateDate(false,
                    this.durComp.getStartText());
            if (validateDur) {

                validateDur = DataDeliveryGUIUtils.validateDate(false,
                        this.durComp.getEndText());
                if (validateDur) {
                    datesValid = true;
                    dateOrderValid = DataDeliveryGUIUtils.checkDateOrder(
                            this.durComp.getStartText(),
                            this.durComp.getEndText(), true);
                }
            }
        }

        // Display error message
        if (!datesValid) {
            displayErrorPopup("Invalid Date/Time",
                    "Invalid Subscription Duration values entered.\n\n"
                            + "Please use the Select Date button\n"
                            + "to select the date/time.");
        } else if (!dateOrderValid) {
            displayErrorPopup("Invalid Start/End Dates",
                    "Invalid Start or Expiration Duration Date entered.\n\n"
                            + "The expiration date is before the start date.");
        }

        return datesValid && dateOrderValid;
    }

    /**
     * Check if dates are valid.
     *
     * @return true if dates are valid
     */
    private boolean activePeriodValidChk() {
        boolean activeDatesValid = false;

        if (!this.activePeriodComp.isAlwaysChk()) {
            boolean validateAct = DataDeliveryGUIUtils.validateDate(false,
                    this.activePeriodComp.getActiveStartText());
            if (validateAct) {
                validateAct = DataDeliveryGUIUtils.validateDate(false,
                        this.activePeriodComp.getActiveEndText());
                if (validateAct) {
                    activeDatesValid = true;
                }
            }
        } else {
            activeDatesValid = true;
        }

        // Display error message
        if (!activeDatesValid) {
            displayErrorPopup("Invalid Date",
                    "Invalid Subscription Active Period values entered.\n\n"
                            + "Please use the Select Date button\n"
                            + "to select the date.");
        }

        return activeDatesValid;
    }

    /**
     * Add the registry id to the subscription object.
     *
     * @param sub
     *            The subscription to get the registry id
     */
    private void setSubscriptionId(Subscription sub) {
        if (sub.getOriginatingSite() == null) {
            sub.setOriginatingSite(DataDeliveryUtils.getDataDeliveryId());
        }
        String id = RegistryUtil.getRegistryObjectKey(sub);
        sub.setId(id);
    }

    /**
     * Store the subscription for the user.
     *
     * @param subscription
     *            the subscription
     * @param username
     *            the username
     * @return true if the dialog can be closed, false otherwise
     */
    private SubscriptionServiceResult storeSubscription(
            Subscription subscription, String username) {
        SubscriptionServiceResult result = null;
        try {
            result = subscriptionService.store(username, subscription,
                    new CancelForceApplyAndIncreaseLatencyDisplayText("create",
                            getShell()));

        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to create subscription.", e);
        }
        return result;
    }

    /**
     * Display the summary dialog
     */
    private void displaySummary(SubscriptionStatusSummary summary, String msg) {
        SubscriptionStatusDlg dlg = new SubscriptionStatusDlg(getShell(),
                summary, msg);
        dlg.open();
    }

    /**
     * Populate the dialog.
     */
    private void populate() {
        if (this.subscription == null) {
            return;
        }

        setSubscriptionName(subscription.getName());

        if (subscription.getDescription() != null) {
            setSubscriptionDescription(subscription.getDescription());
        }

        if (subscription.getSubscriptionEnd() != null) {
            setStartDate(subscription.getSubscriptionStart());
            setExpirationDate(subscription.getSubscriptionEnd());
            setNoExpiration(false);
        } else {
            setNoExpiration(true);
            setDateTxtFieldsEnabled(false);
            setStartDateBtnEnabled(false);
            setEndDateBtnEnabled(false);
        }

        Date activePeriodStartDate = subscription.getActivePeriodStart();
        Date activePeriodEndDate = subscription.getActivePeriodEnd();

        if (activePeriodStartDate != null && activePeriodEndDate != null) {
            final Calendar now = TimeUtil.newGmtCalendar();
            int calendarYearToUse = now.get(Calendar.YEAR);

            // If currently in the window, assume starting from last year for
            // the start date
            if (subscription.isActive()) {
                calendarYearToUse--;
            }

            activePeriodStartDate = calculateNextOccurenceOfMonthAndDay(
                    activePeriodStartDate, calendarYearToUse, now);

            Calendar activePeriodStartCal = TimeUtil.newGmtCalendar();
            activePeriodStartCal.setTime(activePeriodStartDate);

            activePeriodEndDate = calculateNextOccurenceOfMonthAndDay(
                    activePeriodEndDate,
                    activePeriodStartCal.get(Calendar.YEAR), now);

            setActiveStartDate(activePeriodStartDate);
            setActiveEndDate(activePeriodEndDate);
            setAlwaysActive(false);
        } else {
            setAlwaysActive(true);
            setActiveTextFieldsEnabled(false);
            setActiveEndDateBtnEnabled(false);
            setActiveEndDateBtnEnabled(false);
        }

        if (this.dataSet.getDataSetType() == DataType.GRID) {
            List<Integer> cycleTimes = ((GriddedTime) subscription.getTime())
                    .getCycleTimes();
            if (!CollectionUtil.isNullOrEmpty(cycleTimes)) {
                List<String> cycleStrings = new ArrayList<>();

                for (int cycle : cycleTimes) {
                    if (cycle < 10) {
                        cycleStrings.add("0" + String.valueOf(cycle));
                    } else {
                        cycleStrings.add(String.valueOf(cycle));
                    }
                }

                selectCycles(cycleStrings);
            }
        }

        setOfficeIds(subscription.getOfficeIDs());

        if (vbcomp != null) {
            vbcomp.setVertical(subscription.isVertical());
        }
    }

    /**
     * Calculate the next occurrence of the month and day on the specified date
     * object.
     *
     * @param dateWithMonthAndDay
     *            the date to retrieve the month and day from
     * @param yearToStartAt
     *            the year to start moving forward from, checking for the date
     *            to not before the current time
     * @param now
     *            the current calendar
     *
     * @return the date object of the next occurrence
     */
    private static Date calculateNextOccurenceOfMonthAndDay(
            Date dateWithMonthAndDay, int yearToStartAt, Calendar now) {
        final Calendar cal = TimeUtil.newCalendar();
        cal.setTime(dateWithMonthAndDay);
        cal.set(Calendar.YEAR, yearToStartAt);
        if (cal.before(now)) {
            cal.add(Calendar.YEAR, 1);
        }
        return cal.getTime();
    }

    /**
     * Delete DataSetLatency entities for a subscription (if one exists).
     *
     * DataSetLatency objects are not registry entities. Changes to a
     * Subscription will not automatically trigger their deletion.
     *
     * @param subscription
     *            Subscription to delete any existing DataSetLatency entities
     *            for
     */
    private static void resetSubscriptionDataSetLatency(
            Subscription subscription) {

        if (subscription != null) {
            String dataSetName = subscription.getDataSetName();
            String providerName = subscription.getProvider();
            DataSetLatencyService dataSetLatencyService = new DataSetLatencyService(
                    DataDeliveryConstants.DATA_DELIVERY_SERVER);

            dataSetLatencyService.deleteByDataSetNameAndProvider(dataSetName,
                    providerName);
        }
    }

    /**
     * Deletes a subscription and its associations.
     *
     * @param username
     *
     * @param subscription
     */
    private void deleteSubscription(String username,
            Subscription subscription) {

        try {
            resetSubscriptionDataSetLatency(subscription);
            SubscriptionHandler handler = RegistryObjectHandlers
                    .get(SubscriptionHandler.class);
            handler.delete(username, subscription);
        } catch (RegistryHandlerException e) {
            statusHandler
                    .error("Unable to delete duplicate (Site) Subscription "
                            + subscription.getName(), e);
        }

        return;
    }

    public Subscription<Time, Coverage> getSubscription() {
        return subscription;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

}
