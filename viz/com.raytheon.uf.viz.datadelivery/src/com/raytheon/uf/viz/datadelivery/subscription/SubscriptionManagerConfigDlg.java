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

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.viz.datadelivery.common.ui.ITableChange;
import com.raytheon.uf.viz.datadelivery.common.xml.ColumnXML;
import com.raytheon.uf.viz.datadelivery.subscription.xml.SubscriptionManagerConfigXML;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;
import com.raytheon.viz.ui.widgets.duallist.DualList;
import com.raytheon.viz.ui.widgets.duallist.DualListConfig;

/**
 * Subscription Manager Configuration Dialog
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 11, 2012            mpduff     Initial creation
 * Jun 07, 2012   687      lvenable   Table data refactor.
 * Jan 03, 2013  1437      bgonzale   Removed xml attribute, use SubscriptionConfigurationManager
 *                                    instead.  Added configuration file management controls to
 *                                    this dialog.
 * Sep 04, 2013  2314      mpduff     Load/save config dialog now non-blocking.
 * Nov 06, 2013  2358      mpduff     Removed configuration file management from this dialog.
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class SubscriptionManagerConfigDlg extends CaveSWTDialog {
    /** Callback to SubscriptionManagerDialog */
    private final ITableChange callback;

    /** Dual List Config */
    private DualListConfig dualConfig;

    /** Dual list widget */
    private DualList dualList;

    /**
     * Configuration manager.
     */
    private final SubscriptionConfigurationManager configManager;

    /**
     * Initialization Constructor.
     * 
     * @param parentShell
     * @param callback
     */
    public SubscriptionManagerConfigDlg(Shell parentShell, ITableChange callback) {
        super(parentShell, SWT.DIALOG_TRIM, CAVE.INDEPENDENT_SHELL);
        setText("Subscription Manager Configuration");
        this.callback = callback;
        configManager = SubscriptionConfigurationManager.getInstance();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.ui.dialogs.CaveSWTDialogBase#initializeComponents(org
     * .eclipse.swt.widgets.Shell)
     */
    @Override
    protected void initializeComponents(Shell shell) {
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, true);
        GridLayout gl = new GridLayout(1, false);

        shell.setLayout(gl);
        shell.setLayoutData(gd);

        createMain();
        createBottomButtons();
    }

    private void createMain() {
        GridData groupData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
        GridLayout groupLayout = new GridLayout(1, false);

        Group configGroup = new Group(shell, SWT.NONE);
        configGroup.setLayout(groupLayout);
        configGroup.setLayoutData(groupData);
        configGroup.setText(" Table Column Configuration Settings ");

        dualConfig = new DualListConfig();
        dualConfig.setAvailableListLabel("Hidden Columns:");
        dualConfig.setSelectedListLabel("Visible Columns:");
        dualConfig.setListHeight(250);
        dualConfig.setListWidth(170);
        dualConfig.setShowUpDownBtns(true);
        dualList = new DualList(configGroup, SWT.NONE, dualConfig);
        updateDualListSelections();
    }

    /**
     * Update the DualList selection-available based on the current
     * configuration manager xml.
     */
    private void updateDualListSelections() {
        SubscriptionManagerConfigXML xml = configManager.getXml();

        ArrayList<ColumnXML> columns = xml.getColumnList();
        ArrayList<String> fullList = new ArrayList<String>();
        ArrayList<String> selectedList = new ArrayList<String>();

        for (ColumnXML column : columns) {
            if (column.isVisible()) {
                selectedList.add(column.getName());
            }
            fullList.add(column.getName());
        }
        dualList.clearAvailableList(true);
        dualList.setFullList(fullList);
        dualList.setSelectedList(selectedList);
    }

    /**
     * Create the bottom control buttons
     */
    private void createBottomButtons() {
        GridData gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(6, false);

        Composite bottomComp = new Composite(shell, SWT.NONE);
        bottomComp.setLayout(gl);
        bottomComp.setLayoutData(gd);

        int buttonWidth = 75;
        GridData btnData = new GridData(buttonWidth, SWT.DEFAULT);

        Button applyBtn = new Button(bottomComp, SWT.PUSH);
        applyBtn.setText("Apply");
        applyBtn.setLayoutData(btnData);
        applyBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleApply();
            }
        });

        Button okBtn = new Button(bottomComp, SWT.PUSH);
        okBtn.setText("OK");
        okBtn.setLayoutData(btnData);
        okBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleOK();
            }
        });

        Button closeBtn = new Button(bottomComp, SWT.PUSH);
        closeBtn.setText("Close");
        closeBtn.setLayoutData(btnData);
        closeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                close();
            }
        });
    }

    /**
     * Apply changes with dialog still open.
     */
    protected void handleApply() {
        if (dualList.getSelectedListItems().length == 0) {
            DataDeliveryUtils
                    .showMessage(shell, SWT.ERROR, "No Columns Visible",
                            "No columns are visible.  At least one column must be visible.");
            return;
        }

        String[] visibleColumns = dualList.getSelectedListItems();
        String[] hiddenColumns = dualList.getAvailableListItems();

        configManager.setVisibleAndHidden(visibleColumns, hiddenColumns);
        callback.tableChanged();
    }

    private void handleOK() {
        handleApply();
        close();
    }
}
