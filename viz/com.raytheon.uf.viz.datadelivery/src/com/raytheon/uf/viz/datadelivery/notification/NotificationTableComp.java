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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.raytheon.uf.common.datadelivery.event.notification.NotificationRecord;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.notification.NotificationMessage;
import com.raytheon.uf.viz.datadelivery.common.ui.ITableChange;
import com.raytheon.uf.viz.datadelivery.common.ui.ITableFind;
import com.raytheon.uf.viz.datadelivery.common.ui.SortDirection;
import com.raytheon.uf.viz.datadelivery.common.ui.TableComp;
import com.raytheon.uf.viz.datadelivery.common.ui.TableCompConfig;
import com.raytheon.uf.viz.datadelivery.common.ui.TableDataManager;
import com.raytheon.uf.viz.datadelivery.common.xml.ColumnXML;
import com.raytheon.uf.viz.datadelivery.notification.PriorityImages.Priority;
import com.raytheon.uf.viz.datadelivery.notification.PriorityImages.PriorityDisplay;
import com.raytheon.uf.viz.datadelivery.notification.xml.MessageLoadXML;
import com.raytheon.uf.viz.datadelivery.notification.xml.NotificationConfigXML;
import com.raytheon.uf.viz.datadelivery.notification.xml.PrioritySettingXML;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils.NotifColumnNames;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils.TABLE_TYPE;
import com.raytheon.uf.viz.datadelivery.utils.NotificationHandler;

/**
 * This class contains the notification table and the controls to change the
 * page that will be displayed in the table.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#     Engineer     Description
 * ------------- ----------- ------------ --------------------------
 * Jun 18, 2012  687         lvenable     Initial creation.
 * Aug 09, 2012  430         jpiatt       Modifications for sort asc & sort
 *                                        desc.
 * Aug 30, 2012  1120        jpiatt       Added clickSort flag.
 * Sep 06, 2012  687         mpduff       Call the table selection method of the
 *                                        ITableChanged interface.
 * Oct 22, 2012  1284        mpduff       Fix the start/end index for pagination
 *                                        of new records, code cleanup.
 * Nov 29, 2012  1285        bgonzale     Added a refresh pause button to the
 *                                        Notification Center Dialog.
 * Jan 22, 2013  1520        mpduff       Update javadoc.
 * Apr 25, 2013  1820        mpduff       Get the column list every time.
 * Aug 30, 2013  2314        mpduff       Sort the table data on load.
 * Sep 16, 2013  2375        mpduff       Removed initial sorting.
 * Sep 26, 2013  2417        mpduff       Fix the find all row selection.
 * Oct 15, 2013  2451        skorolev     Get highlighted rows after message
 *                                        update.
 * Nov 01, 2013  2431        skorolev     Changed labels on the table.
 * Feb 07, 2014  2453        mpduff       Refactored.
 * Apr 18, 2014  3012        dhladky      Null check.
 * Aug 18, 2014  2746        ccody        Non-local Subscription changes not
 *                                        updating dialogs
 * Oct 29, 2014  2749        ccody        Unable to change the OPSNET Bandwidth
 *                                        value for Data Delivery
 * Dec 03, 2014  3840        ccody        Implement Comparator based sorting
 * Jun 09, 2015  4047        dhladky      Dialog blocked CAVE at initial
 *                                        startup, fixed.
 * Jun 10, 2015  4059        dhladky      Fixed manual selections being blown
 *                                        away by updates. (under #4047 check
 *                                        in)
 * Jul 01, 2015  4047        dhladky      Selected indexes never took paging
 *                                        into account.
 * Jul 08, 2015  2805        dhladky      Removed re-evaluation of find dialog
 *                                        buttons on update.
 * Mar 28, 2016  5482        randerso     Fixed GUI sizing issues, changed page
 *                                        number selection to spinner instead of
 *                                        combo box that gets huge if there are
 *                                        lots of pages
 * Apr 26, 2016  5528        dhladky      Prevent null pointer on initial use of
 *                                        find tool.
 * Feb 28, 2017  6121        randerso     Cleanup
 *
 * </pre>
 *
 * @author lvenable
 */

public class NotificationTableComp extends TableComp implements ITableFind {

    /** UFStatus handler. */
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(NotificationTableComp.class);

    /** Priority image creation class */
    private PriorityImages pImage = null;

    /** Master Table list object with users filtered only */
    private final TableDataManager<NotificationRowData> masterTableList = new TableDataManager<>(
            TABLE_TYPE.NOTIFICATION);

    /** Filtered Table list object */
    private final TableDataManager<NotificationRowData> filteredTableList = new TableDataManager<>(
            TABLE_TYPE.NOTIFICATION);

    /** Filtered Table list object */
    private final List<NotificationRowData> visibleTableList = new ArrayList<>();

    /** Concurrent List applicable here **/
    private List<NotificationRecord> notificationList = Collections
            .synchronizedList(new ArrayList<NotificationRecord>());

    /** Notification rows */
    private final String ROWS = "Rows ";

    private final String PAUSE_BUTTON_TEXT = "Pause";

    private final String PAUSE_MSG_TEXT = " <Paused>";

    private final String PAGE_AMT_FORMAT = "of %d";

    private final int MAX_PAGES = 999;

    /** Number row label */
    private Label numRowsLbl;

    /** Page label */
    private Label pageLbl;

    /** Number row label */
    private Label pageAmtLbl;

    /** Page Spinner */
    private Spinner pageSpin;

    /** Configured value per page */
    private int rowsPerPage = 100;

    /** Highlight indices */
    private List<NotificationRowData> highlightRows = null;

    /** Callback for the message loader */
    private final IMessageLoad msgLoadCallback;

    /** Callback for the table change */
    private final ITableChange tableChangeCallback;

    /** Notification handler */
    private final NotificationHandler handler;

    /** Column List */
    private ArrayList<ColumnXML> columnList = null;

    /** Scroll/Refresh pause button */
    private Button pauseButton;

    /** Count of messages receieved when the dialog is paused. */
    private int messageReceivedWhilePausedCount = 0;

    /** Highlighted row ids */
    private final Set<Integer> selectedRowIds = new HashSet<>();

    /** Index of the first visible data row */
    private int tableDataStartIndex = 0;

    /** Index of the last visible data row */
    private int tableDataEndIndex = 20;

    /** Enables and disables find Dialog in favor of manually selected rows */
    private boolean isFindable = true;

    /**
     * Constructor.
     *
     * Note: For the super class we are passing in a false for the notification
     * flag. This is turned off because the notification dialog is using the
     * NotificationHandler and it contains the necessary code that needs to be
     * executed.
     *
     * @param parent
     * @param tableConfig
     * @param callback
     * @param msgLoadCallback
     * @param handler
     */
    public NotificationTableComp(Composite parent, TableCompConfig tableConfig,
            ITableChange callback, IMessageLoad msgLoadCallback,
            NotificationHandler handler) {
        super(parent, tableConfig, false);

        this.tableChangeCallback = callback;
        this.msgLoadCallback = msgLoadCallback;
        this.handler = handler;

        init();
    }

    /**
     * Initialize the composite.
     */
    private void init() {
        pImage = new PriorityImages(this.getShell());
        pImage.setPriorityDisplay(PriorityDisplay.ColorNumName);

        createColumns();
        createBottomPageControls();
    }

    /**
     * Create the paging and row functions.
     */
    private void createBottomPageControls() {

        // Bottom Composite
        Composite bottomComp = new Composite(this, SWT.NONE);
        GridLayout gl = new GridLayout(3, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        bottomComp.setLayout(gl);

        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        bottomComp.setLayoutData(gd);

        Composite pageComp = new Composite(bottomComp, SWT.NONE);

        gd = new GridData(SWT.DEFAULT, SWT.CENTER, false, false);
        gl = new GridLayout(3, false);
        pageComp.setLayout(gl);
        pageComp.setLayoutData(gd);

        // Page label
        pageLbl = new Label(pageComp, SWT.NONE);
        pageLbl.setText("Page: ");

        // Page Selection Spinner
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT);
        pageSpin = new Spinner(pageComp, SWT.BORDER);
        pageSpin.setLayoutData(gd);
        pageSpin.setValues(1, 1, MAX_PAGES, 0, 1, 10);
        pageSpin.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handlePageSelection();
            }
        });

        pageAmtLbl = new Label(pageComp, SWT.NONE);
        GC gc = new GC(pageAmtLbl);
        int textWidth = gc
                .textExtent(String.format(PAGE_AMT_FORMAT, MAX_PAGES)).x;
        gc.dispose();

        gd = new GridData(textWidth, SWT.DEFAULT);
        pageAmtLbl.setLayoutData(gd);

        pauseButton = new Button(bottomComp, SWT.CHECK);
        pauseButton.setText(PAUSE_BUTTON_TEXT);
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT);
        pauseButton.setLayoutData(gd);
        pauseButton
                .setToolTipText("When checked, the Notification Table UI will"
                        + " not refresh, re-sort, or allow user modification"
                        + " until unchecked.");
        pauseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isLocked = pauseButton.getSelection();

                messageReceivedWhilePausedCount = 0;
                pageSpin.setEnabled(!isLocked);
                tableChangeCallback.tableLock(isLocked);
                if (isLocked) {
                    pauseButton.setBackground(
                            getDisplay().getSystemColor(SWT.COLOR_RED));
                } else {
                    populateTable();
                    messageReceivedWhilePausedCount = 0;
                    pauseButton.setBackground(getDisplay()
                            .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
                }

            }
        });

        // Row Label
        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        numRowsLbl = new Label(bottomComp, SWT.RIGHT);
        numRowsLbl.setLayoutData(gd);
    }

    /**
     * Get the table list with filters applied.
     *
     * @return TableDataManager obj
     */
    public TableDataManager<NotificationRowData> getFilteredTableList() {
        return filteredTableList;
    }

    /**
     * Get the rows of data to display.
     *
     * @return list of Notification Row Data objects
     */
    private List<NotificationRowData> gatherVisibleTableRows() {
        visibleTableList.clear();
        int numFilteredRows = filteredTableList.getSize();

        calculateNumberOfPages();

        int pageNumber = pageSpin.getSelection() - 1;
        tableDataStartIndex = pageNumber * rowsPerPage;
        tableDataEndIndex = tableDataStartIndex + rowsPerPage - 1;

        // Add rows to the visible table list
        for (int i = tableDataStartIndex; i <= tableDataEndIndex; i++) {
            if (i < numFilteredRows) {
                NotificationRowData data = filteredTableList.getDataRow(i);
                if (data != null) {
                    visibleTableList.add(data);
                }
            } else {
                break;
            }
        }

        return visibleTableList;
    }

    /**
     * Delete table rows.
     *
     * @param deleteRecordIds
     */
    public void deleteTableDataRows(List<NotificationRowData> deleteRecordIds) {
        filteredTableList.removeAll(deleteRecordIds);
    }

    /**
     * Get the table obj.
     *
     * @return the table obj.
     */
    public Table getTable() {
        return table;
    }

    private void handleColumnSelection(TableColumn tc) {
        if (pauseButton.getSelection()) {
            // no table sort if paused.
            return;
        }

        SortDirection sortDirection = updateSortDirection(tc, true);

        String sortedColumnText = this.sortedColumn.getText();
        Comparator<NotificationRowData> sortComparator = NotificationRowDataComparators
                .getComparator(sortedColumnText, sortDirection);

        filteredTableList.sortData(sortComparator);

        populateTable();
        updateColumnSortImage();
    }

    /**
     * Action taken when hiding a notification from view.
     */
    public void handleHideNotification() {
        // Verify that at least one notification was selected.
        int[] indices = table.getSelectionIndices();
        if (indices.length == 0) {
            return;
        }

        List<NotificationRowData> ids = new ArrayList<>();
        for (int index : indices) {
            NotificationRowData rowData = visibleTableList.get(index);
            ids.add(rowData);
        }

        deleteTableDataRows(ids);
        populateTable();
    }

    /**
     * Action taken when tool tip is selected.
     *
     * @param showToolTips
     *            true when tooltips are on
     */
    public void handleTooltipSelection(boolean showToolTips) {

        showColumnToolTips(showToolTips);

        if (showToolTips) {
            numRowsLbl.setToolTipText("Rows per page");
            pageLbl.setToolTipText("Page Selection");
            pageSpin.setToolTipText("Select a Page");
        } else {
            numRowsLbl.setToolTipText("");
            pageLbl.setToolTipText("");
            pageSpin.setToolTipText("");
        }
    }

    /**
     * Update labels on the table.
     */
    private void updateLabels() {
        int startRow = this.tableDataStartIndex + 1;
        int endRow = this.tableDataEndIndex + 1;

        int numFilteredRows = filteredTableList.getSize();

        int numPages = calculateNumberOfPages();

        pageAmtLbl.setText(String.format(PAGE_AMT_FORMAT, numPages));

        if (numFilteredRows == 0) {
            // No rows visible possibly due to filtering
            numRowsLbl.setText(
                    "No rows to display. Please check the configuration and "
                            + "filtering options.");
        } else if (numFilteredRows < endRow) {
            numRowsLbl.setText(ROWS + startRow + " - " + numFilteredRows
                    + " from " + numFilteredRows + " of "
                    + this.masterTableList.getSize());
        } else {
            numRowsLbl.setText(ROWS + startRow + " - " + endRow + " from "
                    + numFilteredRows + " of "
                    + this.masterTableList.getSize());
        }
    }

    /**
     * Refresh table after configurations have changed.
     */
    public void tableChangedAfterConfigLoad() {
        rowsPerPage = NotificationConfigManager.getInstance().getConfigXml()
                .getPaginationSetting();
        populateTableDataRows(null);
        gatherVisibleTableRows();

        table.setRedraw(false);

        TableColumn[] columns = table.getColumns();
        for (TableColumn column : columns) {
            column.dispose();
        }

        table.removeAll();
        createColumns();
        table.setRedraw(true);

        populateTable();
        handlePageSelection();
    }

    /**
     * Action taken when deleted notifications from view by time.
     */
    public void handleHideOlderThan() {
        // Verify that at least one notification was selected.
        int[] indices = table.getSelectionIndices();

        if (indices == null || indices.length == 0) {
            return;
        }

        if (indices.length > 1) {
            DataDeliveryUtils.showMessage(getShell(), SWT.OK, "Selection Error",
                    "Please select only one row.");
            return;
        }

        TableItem ti = table.getItem(indices[0]);
        NotificationRowData rd = (NotificationRowData) ti.getData();

        List<NotificationRowData> deleteList = new ArrayList<>();

        // Loop over rows and delete the matching rows
        for (NotificationRowData data : filteredTableList.getDataArray()) {
            if (data.getDate().before(rd.getDate())) {
                deleteList.add(data);
            }
        }

        if (!deleteList.isEmpty()) {
            filteredTableList.removeAll(deleteList);
            populateTable();
        }
    }

    /**
     * Calculate number of pages needed
     */
    private int calculateNumberOfPages() {
        // Calculate number of pages needed
        int numFilteredRows = filteredTableList.getSize();
        int numPages = numFilteredRows / rowsPerPage;

        // Add an extra page if excess rows
        if (numFilteredRows > numPages * rowsPerPage) {
            numPages = numPages + 1;
        }

        return numPages;
    }

    /**
     * Action taken to delete notifications from view by priority.
     *
     * @param priority
     *            priority indicator
     */
    public void handleHideByPriority(int priority) {
        List<NotificationRowData> deleteList = new ArrayList<>();

        // Loop over rows and delete the matching rows
        for (NotificationRowData data : filteredTableList.getDataArray()) {
            // Priority is 0 based, so must subtract 1
            if (data.getPriority() == priority - 1) {
                deleteList.add(data);
            }
        }

        if (deleteList.size() > 0) {
            deleteTableDataRows(deleteList);
            populateTable();
        }
    }

    /**
     * Get the table cell text.
     *
     * @param name
     *            The column name
     * @param rd
     *            The data object
     * @return The text for the table cell
     */
    private String getCellText(String name, NotificationRowData rd) {
        String returnValue = null;

        if (name.equals(NotifColumnNames.TIME.getColumnName())) {
            SimpleDateFormat format = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            returnValue = format.format(rd.getDate());
        } else if (name.equals(NotifColumnNames.PRIORITY.getColumnName())) {
            returnValue = String.valueOf(rd.getPriority());
        } else if (name.equals(NotifColumnNames.CATEGORY.getColumnName())) {
            returnValue = rd.getCategory();
        } else if (name.equals(NotifColumnNames.USER.getColumnName())) {
            returnValue = rd.getUser();
        } else if (name.equals(NotifColumnNames.MESSAGE.getColumnName())) {
            returnValue = rd.getMessage();
        }
        return returnValue;
    }

    /**
     * Populate the NotificationRowData objects
     *
     * @param notificationRecords
     *            list of notification records
     */
    public void populateTableDataRows(
            List<NotificationRecord> notificationRecords) {

        final List<NotificationRecord> fnotificationRecords = notificationRecords;

        Job job = new Job("Requesting Notification Records...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {

                NotificationConfigManager configMan = NotificationConfigManager
                        .getInstance();
                List<String> users = configMan.getFilterXml().getUserFilterXml()
                        .getUserList();

                MessageLoadXML messageLoad = msgLoadCallback.getMessageLoad();

                if (CollectionUtil.isNullOrEmpty(fnotificationRecords)) {

                    notificationList = handler.intialLoad(messageLoad, users);
                    masterTableList.clearAll();
                    filteredTableList.clearAll();
                } else {
                    for (NotificationRecord rec : fnotificationRecords) {
                        // prevents duplicates
                        synchronized (notificationList) {
                            if (!notificationList.contains(rec)) {
                                notificationList.add(rec);
                            }
                        }
                    }
                }

                synchronized (notificationList) {

                    if (CollectionUtil.isNullOrEmpty(notificationList)) {
                        return Status.OK_STATUS;
                    }

                    Iterator<NotificationRecord> i = notificationList
                            .iterator();
                    while (i.hasNext()) {

                        NotificationRecord record = i.next();
                        NotificationRowData rd = new NotificationRowData();
                        Integer recordId = record.getId();
                        Calendar recordCalendarDate = record.getDate();
                        if (recordId == null || recordCalendarDate == null) {
                            statusHandler
                                    .error("Error Extracting data from Notification Message: One or more mandatory values are null.\n"
                                            + "ID: " + record.getId()
                                            + "  Date: " + record.getDate()
                                            + "  Category: "
                                            + record.getCategory()
                                            + "\nPriority: "
                                            + record.getPriority() + "  User: "
                                            + record.getUsername()
                                            + "\nMessage: "
                                            + record.getMessage());
                            continue;
                        }
                        rd.setId(recordId.intValue());
                        rd.setDate(recordCalendarDate.getTime());
                        rd.setCategory(record.getCategory());
                        rd.setMessage(record.getMessage());
                        rd.setPriority(record.getPriority());
                        rd.setUser(record.getUsername());
                        // Master table list is filtered for user only
                        masterTableList.addDataRow(rd);
                        ++messageReceivedWhilePausedCount;

                        // Apply filter
                        if (passesFilter(record)) {
                            filteredTableList.addDataRow(rd);
                        }
                    }
                }

                VizApp.runSync(new Runnable() {
                    @Override
                    public void run() {
                        if (isDisposed()) {
                            return;
                        }

                        resetTable();
                        MessageLoadXML messageLoad = msgLoadCallback
                                .getMessageLoad();

                        // ensure default values
                        if (messageLoad == null) {
                            messageLoad = new MessageLoadXML();
                        }

                        if (!messageLoad.isLoadAllMessages()) {
                            // Sort data by time
                            sortByTime(masterTableList);

                            int loadLast = messageLoad.getLoadLast();
                            List<NotificationRowData> removeList = new ArrayList<>();

                            // Keep only the specified number of rows
                            if (messageLoad.isNumMessages()) {
                                // int numRecs = filteredTableList.getSize();
                                int numRecs = masterTableList.getSize();
                                if (numRecs > loadLast) {
                                    removeList = masterTableList.getDataArray()
                                            .subList(loadLast,
                                                    masterTableList.getSize());
                                }
                            } else {
                                long backTime = loadLast
                                        * TimeUtil.MILLIS_PER_HOUR;
                                long currentTime = TimeUtil.currentTimeMillis();

                                List<NotificationRowData> dataList = masterTableList
                                        .getDataArray();
                                for (int i = 0; i < dataList.size(); i++) {
                                    if (currentTime - dataList.get(i).getDate()
                                            .getTime() > backTime) {
                                        removeList.add(dataList.get(i));
                                    }
                                }
                            }

                            if (!removeList.isEmpty()) {
                                filteredTableList.removeAll(removeList);
                                masterTableList.removeAll(removeList);
                            }
                        }

                        String sortedColumnText = sortedColumn.getText();
                        SortDirection sortDirection = getCurrentSortDirection();
                        Comparator<NotificationRowData> sortComparator = NotificationRowDataComparators
                                .getComparator(sortedColumnText, sortDirection);

                        filteredTableList.sortData(sortComparator);

                        int numPages = calculateNumberOfPages();
                        pageSpin.setMaximum(numPages);

                        populateTable();
                    }
                });

                return Status.OK_STATUS;
            }
        };

        job.schedule();
    }

    private void sortByTime(TableDataManager<NotificationRowData> data) {

        Comparator<NotificationRowData> sortComparator = NotificationRowDataComparators
                .getComparator("Time", SortDirection.DESCENDING);
        data.sortData(sortComparator);
    }

    /**
     * Clear the table display.
     */
    private void resetTable() {
        if (!pauseButton.getSelection()) {
            // only refresh table if not paused.
            table.clearAll();
            table.removeAll();
        }
    }

    /**
     * Pass filter value information.
     *
     * @param username
     *            user name table data
     * @param priority
     *            data priority table data
     * @param category
     *            subscription category table data
     * @return boolean true if passes value filter
     */
    public boolean passesValueFilter(String username, Integer priority,
            String category) {
        boolean filterFlag = false;
        boolean userFlag = false;
        boolean subscriptionFlag = false;
        boolean priorityFlag = false;

        NotificationConfigManager configMan = NotificationConfigManager
                .getInstance();
        List<String> users = configMan.getFilterXml().getUserFilterXml()
                .getUserList();
        List<Priority> priorityList = configMan.getFilterXml()
                .getPriorityList();
        List<String> subscriptionCategoryList = configMan.getFilterXml()
                .getSubscriptionList();
        List<Integer> num = new ArrayList<>();

        for (Priority p : priorityList) {
            num.add(p.getPriorityNum());
        }

        if (username != null) {
            if (users == null || users.isEmpty() || username.equals("")
                    || users.contains(username)) {
                userFlag = true;
            }
        }

        if (subscriptionCategoryList == null
                || subscriptionCategoryList.isEmpty()
                || subscriptionCategoryList.equals("")
                || subscriptionCategoryList.contains(category)) {
            subscriptionFlag = true;
        }

        if (num.contains(priority + 1)) {
            priorityFlag = true;
        }

        if (userFlag && subscriptionFlag && priorityFlag) {
            filterFlag = true;
        }

        return filterFlag;
    }

    private boolean passesFilter(NotificationRecord notificationRecord) {

        boolean filterFlag = false;

        if (notificationRecord == null) {
            return false;
        }

        String nrUsername = notificationRecord.getUsername();
        Integer nrPriority = notificationRecord.getPriority();
        String nrCategory = notificationRecord.getCategory();

        filterFlag = passesValueFilter(nrUsername, nrPriority, nrCategory);

        if (filterFlag == true) {
            // Check to see if we already HAVE this message
            NotificationRowData rd = null;
            int filteredTableListSize = this.filteredTableList.getSize();
            for (int i = 0; i < filteredTableListSize
                    && filterFlag == true; i++) {
                rd = this.filteredTableList.getDataRow(i);
                String rdCategory = rd.getCategory();
                Integer rdPriority = rd.getPriority();
                String rdMessage = rd.getMessage();
                String rdUsername = rd.getUser();
                Date rdDate = rd.getDate();
                long rdTimeInMils = 0;
                if (rdDate != null) {
                    rdTimeInMils = rdDate.getTime();
                }
                boolean areEquivalent = notificationRecord
                        .areFunctionallyEquivalent(rdCategory, rdPriority,
                                rdMessage, rdUsername, rdTimeInMils);
                if (areEquivalent == true) {
                    filterFlag = false;
                    break;
                }
            }
        }

        return filterFlag;
    }

    /**
     * Pass filter information.
     *
     * @param records
     *            Notification record
     * @return boolean true if passes filter
     *
     */
    public boolean passesValueFilter(List<NotificationRecord> records) {
        for (NotificationRecord record : records) {
            if (passesFilter(record) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the column data.
     *
     * @param colName
     *            The column name of interest
     *
     * @return The populated ColumnXML object
     */
    private ColumnXML getColumnData(String colName) {

        if (columnList == null) {
            NotificationConfigManager configMan = NotificationConfigManager
                    .getInstance();

            columnList = configMan.getConfigXml().getColumnList();
        }

        for (ColumnXML column : columnList) {
            if (column.getName().equals(colName)) {
                return column;
            }
        }

        return null;
    }

    /**
     * Create table columns.
     */
    @Override
    protected void createColumns() {
        NotificationConfigManager configMan;
        configMan = NotificationConfigManager.getInstance();

        // Get list of columns from config
        NotificationConfigXML xml = configMan.getConfigXml();

        columnList = xml.getColumnList();

        rowsPerPage = xml.getPaginationSetting();
        if (rowsPerPage < 1) {
            // default to 20 rows
            rowsPerPage = 20;
        }

        PrioritySettingXML pri = xml.getPrioritySetting();
        PriorityDisplay pd = PriorityDisplay.ColorNumName;
        if (pri != null) {
            if (pri.isColorNum()) {
                pd = PriorityDisplay.ColorNum;
            } else if (pri.isColor()) {
                pd = PriorityDisplay.Color;
            } else if (pri.isNum()) {
                pd = PriorityDisplay.Num;
            } else if (pri.isNumName()) {
                pd = PriorityDisplay.NumName;
            }
        }
        pImage.setPriorityDisplay(pd);

        // Get the ones that are visible
        for (ColumnXML column : columnList) {
            if (column.isVisible()) {
                TableColumn tc = new TableColumn(table, SWT.NONE);
                String colName = column.getName();

                tc.setText(colName);
                if (colName.equals(NotifColumnNames.PRIORITY.toString())) {
                    tc.setAlignment(SWT.CENTER);
                } else {
                    tc.setAlignment(SWT.LEFT);
                }
                tc.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        handleColumnSelection((TableColumn) event.getSource());
                    }
                });

                if (column.getName()
                        .equals(NotifColumnNames.MESSAGE.toString())) {
                    tc.setResizable(true);
                } else {
                    tc.setResizable(false);
                }

                // Find which column is configured to be the sort column
                if (column.isSortColumn()) {
                    this.sortedColumn = tc;
                }

                SortDirection sortDirection = SortDirection.ASCENDING;
                // Check if any columns set to descending
                if (column.isSortAsc() == false) {
                    sortDirection = SortDirection.DESCENDING;
                }

                sortDirectionMap.put(colName, sortDirection);
            }
        }
    }

    /**
     * Populate the table with data.
     */
    @Override
    public void populateTable() {
        if (pauseButton.getSelection()) {
            // no table repopulate if paused.
            return;
        }

        String sortedColumnText = this.sortedColumn.getText();
        SortDirection sortDirection = getCurrentSortDirection();
        Comparator<NotificationRowData> sortComparator = NotificationRowDataComparators
                .getComparator(sortedColumnText, sortDirection);

        filteredTableList.sortData(sortComparator);

        TableColumn[] columns = table.getColumns();

        resetTable();

        gatherVisibleTableRows();

        for (NotificationRowData rd : this.visibleTableList) {
            int idx = 0;
            TableItem item = new TableItem(table, SWT.NONE);
            item.setData(rd);

            for (TableColumn column : columns) {
                ColumnXML columnXml = getColumnData(column.getText());
                if (columnXml != null) {
                    if (columnXml.isVisible()) {
                        if (column.getText()
                                .equals(NotifColumnNames.PRIORITY.toString())) {
                            item.setImage(idx++, pImage.getImage(
                                    Priority.values()[rd.getPriority()]));
                        } else {
                            String text = getCellText(columnXml.getName(), rd);
                            if (text == null) {
                                item.setText(idx++, "");
                            } else {
                                item.setText(idx++, text);
                            }
                        }
                    }
                }
            }
        }

        // Update the bottom label values
        updateLabels();
        updateColumnSortImage();
        highlightRows();
    }

    @Override
    protected void handleTableMouseClick(MouseEvent event) {
        // no op
    }

    @Override
    protected void handleTableSelection(SelectionEvent e) {
        if (tableChangeCallback != null) {
            // disable find dialog for this update
            setFindable(false);
            tableChangeCallback.tableSelection();
            int[] indices = table.getSelectionIndices();
            selectedRowIds.clear();
            // Extract selected notification ids from the table page
            List<NotificationRowData> highlights = new ArrayList<>(
                    indices.length);
            for (int index : indices) {
                // have to account for paging
                index = rowsPerPage * (pageSpin.getSelection() - 1) + index;
                NotificationRowData rowData = filteredTableList
                        .getDataRow(index);
                if (rowData == null) {
                    continue;
                }
                highlights.add(rowData);
                selectedRowIds.add(rowData.getId());
            }

            if (!highlights.isEmpty()) {
                selectRows(highlights);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * This method is not used. The Notification dialog is using the
     * NotificationHandler so this override method is not used.
     */
    @Override
    public void notificationArrived(NotificationMessage[] messages) {
        // No op
    }

    /**
     * Handle the page selection.
     */
    @Override
    public void handlePageSelection() {

        // Clean highlighted selections on the page
        selectedRowIds.clear();

        // Calculate indices
        this.tableDataStartIndex = rowsPerPage * (pageSpin.getSelection() - 1);
        this.tableDataEndIndex = tableDataStartIndex + rowsPerPage;
        populateTable();
    }

    /**
     * Find the selected table row index.
     */
    @Override
    public void selectRow(NotificationRowData row) {

        int dataIndex = filteredTableList.getDataArray().indexOf(row);

        int selectedPage = dataIndex / rowsPerPage + 1;
        int pageIndex = dataIndex % rowsPerPage;

        pageSpin.setSelection(selectedPage);
        handlePageSelection();
        TableItem item = table.getItem(pageIndex);
        table.setSelection(item);
        if (highlightRows != null) {
            highlightRows.clear();
            highlightRows.add(row);
        }
    }

    /**
     * Find the selected table row indices.
     */
    @Override
    public void selectRows(List<NotificationRowData> rows) {
        this.highlightRows = rows;
        highlightRows();
    }

    /**
     * Highlight the rows
     */
    private void highlightRows() {
        table.deselectAll();
        if (!CollectionUtil.isNullOrEmpty(highlightRows)) {
            for (NotificationRowData row : highlightRows) {
                if (visibleTableList.contains(row)) {
                    table.select(visibleTableList.indexOf(row));
                }
            }
        }
    }

    /**
     * Returns a string with a count of the messages received while paused.
     *
     * @return formatted string.
     */
    public String getPauseCountLabel() {
        StringBuilder pauseString = new StringBuilder();

        if (messageReceivedWhilePausedCount > 0) {
            pauseString.append(" <Received ");
            pauseString.append(messageReceivedWhilePausedCount);
            pauseString.append(" message");
            if (messageReceivedWhilePausedCount > 1) {
                pauseString.append("s");
            }
            pauseString.append(" while Paused>");
        } else {
            pauseString.append(PAUSE_MSG_TEXT);
        }
        return pauseString.toString();
    }

    /**
     * @return true if pause is selected by the user; false otherwise.
     */
    public boolean isLocked() {
        return pauseButton.getSelection();
    }

    @Override
    public void clearSelections() {
        table.deselectAll();
    }

    @Override
    public int getCurrentSelectionIndex() {
        return rowsPerPage * pageSpin.getSelection()
                + table.getSelectionIndex();
    }

    /**
     * Delete rows based on id.
     *
     * @param deleteRecordIds
     *            List of ids to delete.
     */
    public void deleteTableDataRows(ArrayList<Integer> deleteRecordIds) {
        List<NotificationRowData> deleteList = new ArrayList<>(
                deleteRecordIds.size());

        for (NotificationRowData rd : filteredTableList.getDataArray()) {
            if (deleteRecordIds.contains(rd.getId())) {
                deleteList.add(rd);
            }
        }

        filteredTableList.removeAll(deleteList);
        populateTable();
    }

    public boolean isFindable() {
        return isFindable;
    }

    public void setFindable(boolean isFindable) {
        this.isFindable = isFindable;
    }
}