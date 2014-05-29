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
package com.raytheon.uf.viz.datadelivery.notification;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import com.raytheon.uf.common.datadelivery.event.notification.NotificationRecord;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.notification.jobs.NotificationManagerJob;
import com.raytheon.uf.viz.datadelivery.Activator;
import com.raytheon.uf.viz.datadelivery.common.ui.ITableChange;
import com.raytheon.uf.viz.datadelivery.common.ui.LoadSaveConfigDlg;
import com.raytheon.uf.viz.datadelivery.common.ui.LoadSaveConfigDlg.DialogType;
import com.raytheon.uf.viz.datadelivery.common.ui.TableCompConfig;
import com.raytheon.uf.viz.datadelivery.help.HelpManager;
import com.raytheon.uf.viz.datadelivery.notification.PriorityImages.Priority;
import com.raytheon.uf.viz.datadelivery.notification.xml.MessageLoadXML;
import com.raytheon.uf.viz.datadelivery.notification.xml.NotificationConfigXML;
import com.raytheon.uf.viz.datadelivery.notification.xml.NotificationFilterXML;
import com.raytheon.uf.viz.datadelivery.notification.xml.PrioritySettingXML;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils.TABLE_TYPE;
import com.raytheon.uf.viz.datadelivery.utils.NotificationHandler;
import com.raytheon.uf.viz.datadelivery.utils.NotificationHandler.INotificationArrivedListener;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;
import com.raytheon.viz.ui.dialogs.ICloseCallback;

/**
 * Notification Dialog.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 30, 2012            mpduff     Initial creation.
 * Feb 23, 2012            jpiatt     Added load, save & set default xml.
 * Mar 20, 2012   430      jpiatt     Applied filter to table data.
 * May  1, 2012   452      jpiatt     Added pagination.
 * May 22, 2012   645      jpiatt     Added help & tooltips.
 * Jun 07, 2012   687      lvenable   Table data refactor.
 * Aug 09, 2012   430      jpiatt     Modifications for sort Asc & sort Desc.
 * Aug 21, 2012   712      mpduff     Removed unused method.
 * Sep 06, 2012   687      mpduff     Add tableSelection method from the ITableChange interface.
 * Oct 22, 2012  1284      mpduff     Code Cleanup.
 * Dec 03, 2012  1285      bgonzale   Added implementation of the tableLock method.
 *                                    Update title bar text when paused.
 * Jan 22, 2013  1520      mpduff     Change delete menus to hide.
 * Apr 25, 2013  1820      mpduff     Implemente delete config.
 * Jun 06, 2013  2030      mpduff     Refactored help.
 * Aug 30, 2013  2314      mpduff     Change the reading of the xml. Make load config dlg non-blocking.
 * Sep 25, 2013  2408      mpduff     Added a restore hidden notifications menu.
 * Sep 25, 2013  2410      mpduff     Check type of localization file.
 * Feb 07, 2013  2453      mpduff     Support find dialog refactor..
 * Mar 18, 2014  2433      mpduff     Implement view by priority check box menus.
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class NotificationDlg extends CaveSWTDialog implements ITableChange,
        IMessageLoad, INotificationArrivedListener {

    /** Status Handler */
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(NotificationDlg.class);

    private final String TITLE_TEXT = "Notification Center";

    /** Find Dialog */
    private FindDlg fnd = null;

    /** Message load properties */
    private MessageLoadXML messageLoad;

    /** Help text file */
    private final String NOTIFICATION_HELP_FILE = "help/notificationHelp.xml";

    /** Path of the Notification Config xml file */
    private final String CONFIG_PATH = "dataDelivery" + File.separator
            + "notificationManagerConfig";

    /** Path of the Default Notification Config xml file */
    private final String DEFAULT_CONFIG = FileUtil.join("dataDelivery",
            "DefaultNotificationConfig.xml");

    /** Path of the Default Notification Config xml file */
    private final String DEFAULT_CONFIG_XML = "notificationManagerConfig"
            + File.separator + "DefaultNotificationConfig.xml";

    /** The configuration dialog */
    private NotificationConfigDlg configDlg = null;

    /** Data Delivery system tray icon */
    private Image trayImg1;

    /** Data Delivery system tray icon for new notifications present */
    private Image trayImg2;

    /** System tray object */
    private Tray tray;

    /** System tray item */
    private TrayItem trayItem;

    /** Tray menu */
    private Menu trayItemMenu;

    /** Tool tip for the tray item */
    private ToolTip tip;

    /** JAXB context */
    private JAXBContext jax;

    /** Unmarshaller object */
    private Unmarshaller unmarshaller;

    /** Notification handler */
    private final NotificationHandler handler;

    /** Reference to the shell */
    private Shell shellReference;

    /** Tray menu for show/hide the dialog */
    private MenuItem showHideDialogMI;

    /** The last location of the dialog */
    private Point dialogLocation = null;

    /** Table composite */
    private NotificationTableComp tableComp;

    /** Tool tip menu */
    private MenuItem tooltipMI;

    private final Collection<MenuItem> lockableMenuItems = new ArrayList<MenuItem>();

    /** Load config dialog */
    private LoadSaveConfigDlg loadDlg;

    /** Delete config dialog */
    private LoadSaveConfigDlg deleteDlg;

    /** SaveAs config dialog */
    private LoadSaveConfigDlg saveAsDlg;

    /** Priority -> Priority Menu Item map */
    private final Map<Priority, MenuItem> priorityMenuMap = new HashMap<Priority, MenuItem>(
            Priority.values().length);

    /**
     * Constructor.
     * 
     * @param parent
     *            parent shell
     */
    public NotificationDlg(Shell parent) {
        super(parent, SWT.DIALOG_TRIM | SWT.MIN | SWT.RESIZE,
                CAVE.INDEPENDENT_SHELL | CAVE.PERSPECTIVE_INDEPENDENT);
        setText(TITLE_TEXT);
        NotificationHandler.addListener(this);
        handler = new NotificationHandler();
        NotificationManagerJob.addObserver("notify.msg", handler);
        createContext();
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
        shell.setSize(900, 350);
    }

    /**
     * Initialize the composite.
     */
    @Override
    protected void initializeComponents(Shell shell) {

        shellReference = shell;

        NotificationConfigManager configMan = NotificationConfigManager
                .getInstance();
        configMan.rereadxml();

        createMenus();
        createTable();
        createTrayControls();

        tableComp.populateTableDataRows(null);
        tableComp.populateTable();

        shell.addShellListener(new ShellAdapter() {
            @Override
            public void shellDeiconified(ShellEvent e) {
                trayItem.setImage(trayImg1);
            }
        });

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.ui.dialogs.CaveSWTDialogBase#constructShellLayout()
     */
    @Override
    protected Layout constructShellLayout() {
        // Create the main layout for the shell.
        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;

        return mainLayout;
    }

    /**
     * Create the dialog menus.
     */
    private void createMenus() {
        Menu menuBar = new Menu(shell, SWT.BAR);

        // Create the file menu
        MenuItem fileMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        fileMenuItem.setText("&File");

        // Create the File menu item with a File "dropdown" menu
        Menu fileMenu = new Menu(menuBar);
        fileMenuItem.setMenu(fileMenu);

        MenuItem setDefaultMI = new MenuItem(fileMenu, SWT.NONE);
        lockableMenuItems.add(setDefaultMI);
        setDefaultMI.setText("Set as Default");
        setDefaultMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleSetDefault();
            }
        });

        MenuItem loadConfigMI = new MenuItem(fileMenu, SWT.NONE);
        lockableMenuItems.add(loadConfigMI);
        loadConfigMI.setText("Load Configuration...");
        loadConfigMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    handleLoadConfig();
                } catch (JAXBException e) {
                    statusHandler
                            .handle(com.raytheon.uf.common.status.UFStatus.Priority.ERROR,
                                    e.getLocalizedMessage(), e);
                }
            }
        });

        MenuItem saveConfigMI = new MenuItem(fileMenu, SWT.NONE);
        lockableMenuItems.add(saveConfigMI);
        saveConfigMI.setText("Save Configuration");
        saveConfigMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleSaveConfig();
            }
        });

        MenuItem saveConfigAsMI = new MenuItem(fileMenu, SWT.NONE);
        lockableMenuItems.add(saveConfigAsMI);
        saveConfigAsMI.setText("Save Configuration As...");
        saveConfigAsMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleSaveAsConfig();
            }
        });

        MenuItem deleteConfigMI = new MenuItem(fileMenu, SWT.NONE);
        lockableMenuItems.add(deleteConfigMI);
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

        // Create the edit menu
        MenuItem editMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        editMenuItem.setText("&Edit");

        Menu editMenu = new Menu(menuBar);
        editMenuItem.setMenu(editMenu);

        // Find Menu
        MenuItem findMI = new MenuItem(editMenu, SWT.CASCADE);
        lockableMenuItems.add(findMI);
        findMI.setText("Find...");
        findMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleFind();
            }
        });

        // Create view menu
        MenuItem viewMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        viewMenuItem.setText("&View");

        Menu viewMenu = new Menu(menuBar);
        viewMenuItem.setMenu(viewMenu);

        MenuItem priorityMI = new MenuItem(viewMenu, SWT.CASCADE);
        priorityMI.setText("By Priority");

        Menu priorityMenu = new Menu(viewMenu);
        priorityMI.setMenu(priorityMenu);

        createPriorityMenus(priorityMenu);

        new MenuItem(viewMenu, SWT.SEPARATOR);

        MenuItem hideOlderMI = new MenuItem(viewMenu, SWT.NONE);
        lockableMenuItems.add(hideOlderMI);
        hideOlderMI.setText("Hide Older Than Selected");
        hideOlderMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                tableComp.handleHideOlderThan();
            }

        });

        MenuItem hideMI = new MenuItem(viewMenu, SWT.NONE);
        lockableMenuItems.add(hideMI);
        hideMI.setText("Hide Notification(s)");
        hideMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                tableComp.handleHideNotification();
            }
        });

        MenuItem unhideMI = new MenuItem(viewMenu, SWT.NONE);
        lockableMenuItems.add(unhideMI);
        unhideMI.setText("Show Hidden Notifications");
        unhideMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                tableComp.populateTableDataRows(null);
                tableComp.populateTable();
            }

        });

        // Create the settings menu
        MenuItem settingsMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        settingsMenuItem.setText("&Settings");

        Menu settingsMenu = new Menu(menuBar);
        settingsMenuItem.setMenu(settingsMenu);

        MenuItem configureMI = new MenuItem(settingsMenu, SWT.NONE);
        lockableMenuItems.add(configureMI);
        configureMI.setText("Configure Table...");
        configureMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleTableConfiguration();
            }
        });

        MenuItem filterMI = new MenuItem(settingsMenu, SWT.NONE);
        lockableMenuItems.add(filterMI);
        filterMI.setText("Filter Table...");
        filterMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleFilterSelection();
            }
        });

        tooltipMI = new MenuItem(settingsMenu, SWT.CHECK);
        tooltipMI.setText("Tooltips");
        tooltipMI.setSelection(false);
        tooltipMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                tableComp.handleTooltipSelection(tooltipMI.getSelection());
            }

        });

        // Create the help menu
        MenuItem helpMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        helpMenuItem.setText("&Help");

        Menu helpMenu = new Menu(menuBar);
        helpMenuItem.setMenu(helpMenu);

        MenuItem helpNotTableMI = new MenuItem(helpMenu, SWT.NONE);
        helpNotTableMI.setText("About Notification Center...");
        helpNotTableMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleHelp();
            }

        });

        shell.setMenuBar(menuBar);
    }

    /**
     * Create the priority menu.
     * 
     * @param menu
     *            Menu to hold these menu items
     */
    private void createPriorityMenus(Menu menu) {
        NotificationFilterXML xml = NotificationConfigManager.getInstance()
                .getFilterXml();
        for (Priority priority : PriorityImages.Priority.values()) {
            MenuItem mi = new MenuItem(menu, SWT.CHECK);
            mi.setText("Priority " + priority.getPriorityNum());
            mi.setData(priority.getPriorityNum());
            if (priority == Priority.Priority1) {
                mi.setEnabled(false);
                mi.setSelection(true);
                continue;
            }
            for (Priority p : xml.getPriorityList()) {
                if (priority.equals(p)) {
                    mi.setSelection(true);
                    break;
                }
            }

            mi.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updatePriorityView(priorityMenuMap);
                }
            });
            priorityMenuMap.put(priority, mi);
        }
    }

    /**
     * Create the notification table.
     */
    private void createTable() {

        TableCompConfig tableConfig = new TableCompConfig(
                TABLE_TYPE.PENDING_SUBSCRIPTION);
        tableConfig.setTableStyle(SWT.BORDER | SWT.MULTI);
        tableConfig.setTableHeight(0);
        tableComp = new NotificationTableComp(shell, tableConfig, this, this,
                handler);
    }

    /**
     * Handle the help display dialog.
     */
    private void handleHelp() {
        try {
            HelpManager.getInstance().displayHelpDialog(shell,
                    NOTIFICATION_HELP_FILE);
        } catch (Exception e) {
            statusHandler.error("Error loading Help Text file: "
                    + NOTIFICATION_HELP_FILE, e);
        }
    }

    /**
     * Create the tray control items.
     */
    private void createTrayControls() {
        Display display = getDisplay();

        ImageDescriptor id;
        id = Activator.imageDescriptorFromPlugin(
                "com.raytheon.uf.viz.datadelivery", "icons/dd.png");
        trayImg1 = id.createImage();

        id = Activator.imageDescriptorFromPlugin(
                "com.raytheon.uf.viz.datadelivery", "icons/dd_new.png");
        trayImg2 = id.createImage();

        tray = display.getSystemTray();

        createTray();
    }

    /**
     * Create the system tray icon.
     */
    private void createTray() {
        trayItem = new TrayItem(tray, SWT.NONE);
        trayItem.setToolTipText("Data Delivery Notifications");
        tip = new ToolTip(shell, SWT.BALLOON);
        trayItem.setToolTip(tip);

        addTrayMenu();

        trayItem.addMenuDetectListener(new MenuDetectListener() {
            @Override
            public void menuDetected(MenuDetectEvent de) {
                trayItemMenu.setVisible(true);
            }
        });

        trayItem.setImage(this.trayImg1);
    }

    /**
     * Find table text.
     */
    private void handleFind() {
        if (fnd == null || fnd.isDisposed()) {
            fnd = new FindDlg(shell, tableComp.getFilteredTableList(),
                    tableComp.getTable().getSelectionIndex(), tableComp);
            fnd.open();
        } else {
            fnd.bringToTop();
        }

    }

    /**
     * Table Configuration action.
     */
    private void handleTableConfiguration() {
        if ((configDlg == null) || configDlg.isDisposed()) {
            configDlg = new NotificationConfigDlg(shell, this);
            configDlg.open();
        } else {
            configDlg.bringToTop();
        }
    }

    /**
     * Filter table action.
     */
    private void handleFilterSelection() {
        NotificationFilterDlg filter = new NotificationFilterDlg(shell, this);
        boolean change = false;
        Object o = filter.open();

        if (o instanceof Boolean) {
            if (change) {
                tableComp.populateTable();
            }
        }
    }

    /**
     * Set the default configuration file.
     */
    private void handleSetDefault() {

        NotificationConfigManager configMan;
        configMan = NotificationConfigManager.getInstance();

        String fileName = "dataDelivery" + File.separator + DEFAULT_CONFIG_XML;

        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext context = pm.getContext(
                LocalizationType.CAVE_STATIC, LocalizationLevel.USER);

        LocalizationFile locFile = pm.getLocalizationFile(context, fileName);

        try {

            configMan.setConfigFile(locFile);
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
    private void handleLoadConfig() throws JAXBException {
        if (loadDlg == null || loadDlg.isDisposed()) {
            final NotificationConfigManager configMan = NotificationConfigManager
                    .getInstance();
            loadDlg = new LoadSaveConfigDlg(shell, DialogType.OPEN,
                    CONFIG_PATH, DEFAULT_CONFIG, true);
            loadDlg.setCloseCallback(new ICloseCallback() {
                @Override
                public void dialogClosed(Object returnValue) {
                    if (returnValue instanceof LocalizationFile) {
                        try {
                            LocalizationFile fileName = (LocalizationFile) returnValue;
                            // Get the name of the selected file
                            if (fileName != null && fileName.exists()) {
                                File file = fileName.getFile();

                                if (file != null) {
                                    Object obj = unmarshaller.unmarshal(file);

                                    if (obj instanceof NotificationFilterXML) {
                                        configMan
                                                .setFilterXml((NotificationFilterXML) obj);
                                    } else if (obj instanceof NotificationConfigXML) {
                                        configMan
                                                .setConfigXml((NotificationConfigXML) obj);
                                    }

                                    tableComp.tableChangedAfterConfigLoad();
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
     * Create the JAXB context
     */
    @SuppressWarnings("rawtypes")
    private void createContext() {
        Class[] classes = new Class[] { NotificationConfigXML.class,
                MessageLoadXML.class, PrioritySettingXML.class,
                NotificationFilterXML.class };

        try {
            jax = JAXBContext.newInstance(classes);
            this.unmarshaller = jax.createUnmarshaller();
        } catch (JAXBException e) {
            statusHandler.handle(
                    com.raytheon.uf.common.status.UFStatus.Priority.ERROR,
                    e.getLocalizedMessage(), e);
        }
    }

    /**
     * Save configuration action.
     */
    private void handleSaveConfig() {

        NotificationConfigManager configMan;
        configMan = NotificationConfigManager.getInstance();

        if (configMan.currentConfigFile == null) {
            handleSaveAsConfig();
        } else {

            configMan.setConfigFile(configMan.currentConfigFile);
            configMan.saveXml();
        }

    }

    /**
     * Save as configuration action.
     */
    private void handleSaveAsConfig() {
        if (saveAsDlg == null || saveAsDlg.isDisposed()) {
            saveAsDlg = new LoadSaveConfigDlg(shell, DialogType.SAVE_AS,
                    CONFIG_PATH, DEFAULT_CONFIG);
            saveAsDlg.setCloseCallback(new ICloseCallback() {
                @Override
                public void dialogClosed(Object returnValue) {
                    if (returnValue instanceof LocalizationFile) {
                        NotificationConfigManager configMan = NotificationConfigManager
                                .getInstance();

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
                    CONFIG_PATH, true);
            deleteDlg.setCloseCallback(new ICloseCallback() {
                @Override
                public void dialogClosed(Object returnValue) {
                    if (returnValue instanceof LocalizationFile) {
                        LocalizationFile fileName = (LocalizationFile) returnValue;
                        NotificationConfigManager configMan = NotificationConfigManager
                                .getInstance();
                        configMan.deleteXml(fileName);
                        tableComp.tableChangedAfterConfigLoad();
                    }
                }
            });
            deleteDlg.open();
        } else {
            deleteDlg.bringToTop();
        }
    }

    /**
     * Add the system tray menu.
     */
    private void addTrayMenu() {
        trayItemMenu = new Menu(shell, SWT.POP_UP);
        showHideDialogMI = new MenuItem(trayItemMenu, SWT.CHECK);
        showHideDialogMI.setSelection(true);
        showHideDialogMI.setText("Show Notification Dialog");
        showHideDialogMI.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (showHideDialogMI.getSelection() == true) {
                    if (dialogLocation != null) {
                        shell.setLocation(dialogLocation);
                    }

                    shell.setVisible(true);
                    bringToTop();
                    trayItem.setImage(trayImg1);
                } else {
                    dialogLocation = shell.getLocation();
                    shell.setVisible(false);
                }
            }
        });
    }

    /**
     * Retrieves the message load information from the config files.
     * 
     * @return MessgeLoadXML
     */
    @Override
    public MessageLoadXML getMessageLoad() {
        NotificationConfigManager configMan = NotificationConfigManager
                .getInstance();
        NotificationConfigXML xml = configMan.getConfigXml();

        messageLoad = xml.getMessageLoad();

        return messageLoad;
    }

    @Override
    protected void disposed() {
        this.trayImg1.dispose();
        this.trayImg2.dispose();
        this.trayItem.dispose();
        this.showHideDialogMI.dispose();
        NotificationHandler.removeListener(this);
        NotificationManagerJob.removeObserver("notify.msg", handler);
    }

    /**
     * Updates the notification table and displays a balloon tool tip.
     */
    @Override
    public void handleNotification(
            ArrayList<NotificationRecord> notificationRecords) {

        final ArrayList<NotificationRecord> records = notificationRecords;
        VizApp.runAsync(new Runnable() {
            @Override
            public void run() {
                if (isDisposed() == false && tableComp.passesFilter(records)) {
                    tableComp.populateTableDataRows(records);
                    tableComp.populateTable();
                    tableComp.handlePageSelection();
                    // update title display......
                    if (tableComp.isLocked()) {
                        setText(TITLE_TEXT + tableComp.getPauseCountLabel());
                    }

                    if (shellReference.isDisposed() == false
                            && (shellReference.isVisible() == false || shellReference
                                    .getMinimized() == true)) {
                        displayNotificationTip(records);
                    }
                }
            }
        });
    }

    /**
     * Removes data rows from the table.
     */
    @Override
    public void deleteNotification(ArrayList<Integer> ids) {
        final ArrayList<Integer> deleteRecordIds = ids;
        VizApp.runAsync(new Runnable() {
            @Override
            public void run() {
                if (isDisposed() == false) {
                    tableComp.deleteTableDataRows(deleteRecordIds);
                    tableComp.populateTable();
                }
            }
        });

    }

    /**
     * Displays a tool tip with the message contained in the notification record
     */
    private void displayNotificationTip(
            ArrayList<NotificationRecord> notificationRecords) {
        StringBuffer sb = new StringBuffer();
        for (NotificationRecord record : notificationRecords) {
            if (sb.length() != 0) {
                sb.append("\n");
            }
            sb.append(record.getMessage());
        }

        if (sb.toString().trim().length() > 0) {
            tip.setText("Notification");
            tip.setMessage(sb.toString());
            tip.setVisible(true);
            trayItem.setToolTip(tip);
            trayItem.setImage(trayImg2);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.datadelivery.common.ui.ITableChange#tableChanged()
     */
    @Override
    public void tableChanged() {
        updatePriorityViewMenus();
        tableComp.tableChangedAfterConfigLoad();
    }

    @Override
    public void tableSelection() {
        trayItem.setImage(trayImg1);
    }

    @Override
    public void tableLock(boolean isLocked) {
        if (isLocked) {
            setText(TITLE_TEXT + tableComp.getPauseCountLabel());
            shell.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
        } else {
            setText(TITLE_TEXT);
            shell.setBackground(getDisplay().getSystemColor(
                    SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
        }
        for (MenuItem mItem : lockableMenuItems) {
            mItem.setEnabled(!isLocked);
        }
    }

    /**
     * Update the priority menus to match the configuration.
     */
    public void updatePriorityViewMenus() {
        NotificationConfigManager configMan = NotificationConfigManager
                .getInstance();
        configMan.getFilterXml();

        for (Priority priority : priorityMenuMap.keySet()) {
            priorityMenuMap.get(priority).setSelection(false);
            for (Priority p : configMan.getFilterXml().getPriorityList()) {
                if (p.equals(priority)) {
                    priorityMenuMap.get(p).setSelection(true);
                    break;
                }
            }
        }
    }

    /**
     * Update the priority view items
     * 
     * @param priorityMap
     */
    public void updatePriorityView(Map<Priority, MenuItem> priorityMap) {
        NotificationConfigManager configManager = NotificationConfigManager
                .getInstance();
        NotificationFilterXML xml = configManager.getFilterXml();

        xml.clearPriorityList();
        for (Priority p : priorityMap.keySet()) {
            if (priorityMap.get(p).getSelection()) {
                xml.addPriority(p);
            }
        }

        configManager.setFilterXml(xml);
        configManager.saveXml();

        tableComp.tableChangedAfterConfigLoad();
    }
}