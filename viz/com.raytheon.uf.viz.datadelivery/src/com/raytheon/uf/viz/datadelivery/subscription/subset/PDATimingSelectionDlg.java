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
package com.raytheon.uf.viz.datadelivery.subscription.subset;

import static com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils.getMaxLatency;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.datadelivery.registry.PDADataSet;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription.SubscriptionPriority;
import com.raytheon.uf.viz.datadelivery.common.ui.PriorityComp;
import com.raytheon.uf.viz.datadelivery.system.SystemRuleManager;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryGUIUtils;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;

/**
 * PDA data selection dialog for adhoc queries (choose time).
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------------------
 * Aug 12, 2014  3121     dhladky   Initial creation.
 * May 17, 2015  4047     dhladky   Verified non-blocking.
 * Jan 19, 2016  5054     randerso  Fixed dialog to display with title bar  
 *                                     and in correct location.
 * Feb 09, 2016  5324     randerso  Remove CAVE.DO_NOT_BLOCK until DR #5327 is worked
 * Mar 28, 2016  5482     randerso  Fixed GUI sizing issues 
 * Jun 16, 2016  5683     tjensen   Change Cancel to return PDATimeSelection
 * Aug 17, 2016  5772     rjpeter   Always return selected time.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDATimingSelectionDlg extends CaveSWTDialog {

    /** The Main Composite */
    private Composite dateComp;

    /** Use latest data check button */
    private Button useLatestChk;

    /** OK button */
    private Button okBtn;

    /** Cancel button */
    private Button cancelBtn;

    /** Priority Composite */
    private PriorityComp priorityComp;

    /** The subscription object */
    @SuppressWarnings("rawtypes")
    private final Subscription subscription;

    private final java.util.List<String> dateList;

    /** List of dates/cycles */
    private List dateCycleList;

    /**
     * Constructor
     * 
     * @param parentShell
     * @param dataset
     * @param subscription
     * @param dateStringToDateMap
     */
    public PDATimingSelectionDlg(Shell parentShell, PDADataSet dataset,
            @SuppressWarnings("rawtypes") Subscription subscription,
            java.util.List<String> dateList) {
        super(parentShell, SWT.DIALOG_TRIM);
        setText("Select Date");
        this.subscription = subscription;
        this.dateList = dateList;

        if (dateList == null || dateList.isEmpty()) {
            throw new IllegalArgumentException(
                    "No data is available for data set.");
        }
    }

    /**
     * Open the dialog.
     * 
     * @return the selection
     */
    public PDATimeSelection openDlg() {
        return (PDATimeSelection) this.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeComponents(Shell shell) {
        GridData gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(1, false);

        dateComp = new Composite(shell, SWT.NONE);
        dateComp.setLayout(gl);
        dateComp.setLayoutData(gd);

        useLatestChk = new Button(dateComp, SWT.CHECK);
        useLatestChk.setLayoutData(gd);
        useLatestChk.setText("Get Latest Data");
        useLatestChk.setSelection(true);
        useLatestChk.setToolTipText("Use the latest time");
        useLatestChk.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dateCycleList.setEnabled(!useLatestChk.getSelection());
            }
        });

        this.dateCycleList = new List(dateComp, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL);
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gd.heightHint = dateCycleList.getItemHeight() * 8;
        dateCycleList.setLayoutData(gd);
        dateCycleList.setEnabled(false);

        // Get latency value
        SystemRuleManager ruleManager = SystemRuleManager.getInstance();

        int latency = ruleManager.getPDADataLatency(this.subscription);
        SubscriptionPriority priority = ruleManager
                .getPDADataPriority(this.subscription);
        priorityComp = new PriorityComp(shell, latency, priority, false);

        gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        gl = new GridLayout(2, false);
        Composite buttonComp = new Composite(shell, SWT.NONE);
        buttonComp.setLayout(gl);
        buttonComp.setLayoutData(gd);

        int btnWidth = buttonComp.getDisplay().getDPI().x;
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, true, false);
        gd.minimumWidth=btnWidth;
        okBtn = new Button(buttonComp, SWT.PUSH);
        okBtn.setLayoutData(gd);
        okBtn.setText("OK");
        okBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleOk();
                close();
            }
        });

        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, true, false);
        gd.minimumWidth=btnWidth;
        cancelBtn = new Button(buttonComp, SWT.PUSH);
        cancelBtn.setLayoutData(gd);
        cancelBtn.setText("Cancel");
        cancelBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PDATimeSelection pts = new PDATimeSelection();
                pts.setCancel(true);
                setReturnValue(pts);
                close();
            }
        });
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    protected void preOpened() {
        populate();
        shell.layout();
        shell.pack();
    }

    /**
     * Check if the latest data checkbox is enabled.
     * 
     * @return true if enabled.
     */
    public boolean isLatestDataEnabled() {
        return useLatestChk.getSelection();
    }

    /**
     * Set the date/cycle list enabled.
     */
    public void setDateCycleListEnabled() {
        this.dateCycleList.setEnabled(!this.useLatestChk.getSelection());
    }

    private void populate() {
        for (String date : this.dateList) {
            dateCycleList.add(date);
        }
    }

    /**
     * OK Button action method.
     */
    @SuppressWarnings("unchecked")
    private void handleOk() {
        PDATimeSelection data = new PDATimeSelection();
        if (!isLatestDataEnabled()) {
            String selection = dateCycleList.getItem(dateCycleList
                    .getSelectionIndex());
            DataDeliveryGUIUtils
                    .latencyValidChk(priorityComp.getLatencyValue(),
                            getMaxLatency(subscription));
            data.setDate(selection);
        } else {
            data.setLatest(true);
            data.setDate(dateCycleList.getItem(0));
        }

        setReturnValue(data);
    }
}
