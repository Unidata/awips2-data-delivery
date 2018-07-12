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

import java.util.Calendar;
import java.util.Date;
import java.util.SortedSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription.SubscriptionPriority;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.viz.datadelivery.common.ui.PriorityComp;
import com.raytheon.uf.viz.datadelivery.system.SystemRuleManager;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryGUIUtils;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;
import com.raytheon.viz.ui.widgets.DateTimeEntry;

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
 * Oct 13, 2017  6461     tgurney   Replace single time with time range
 * Jul 12, 2018  7358     tjensen   Fixed time range selection
 *
 * </pre>
 *
 * @author dhladky
 */

public class PDATimingSelectionDlg extends CaveSWTDialog {

    private DateTimeEntry startTimeEntry;

    private DateTimeEntry endTimeEntry;

    /** Use latest data check button */
    private Button useLatestChk;

    /** Priority Composite */
    private PriorityComp priorityComp;

    /** The subscription object */
    @SuppressWarnings("rawtypes")
    private final Subscription subscription;

    /** List of dates/cycles sorted oldest to newest */
    private final SortedSet<Date> dateCycleList;

    /**
     * Constructor
     * 
     * @param parentShell
     * @param oldestToNewest
     * @param subscription
     */
    public PDATimingSelectionDlg(Shell parentShell,
            SortedSet<Date> oldestToNewest,
            @SuppressWarnings("rawtypes") Subscription subscription) {
        super(parentShell, SWT.DIALOG_TRIM);
        setText("Select Time Range");
        this.subscription = subscription;

        if (oldestToNewest == null || oldestToNewest.isEmpty()) {
            throw new IllegalArgumentException(
                    "No data is available for data set.");
        }
        this.dateCycleList = oldestToNewest;
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
        GridLayout gl = new GridLayout(2, false);
        GridData gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        Composite dateComp = new Composite(shell, SWT.NONE);

        dateComp.setLayout(gl);
        dateComp.setLayoutData(gd);

        useLatestChk = new Button(dateComp, SWT.CHECK);
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, true);
        gd.horizontalSpan = 2;
        useLatestChk.setLayoutData(gd);
        useLatestChk.setText("Get Latest Data");
        useLatestChk.setSelection(true);
        useLatestChk.setToolTipText("Use the latest time");
        useLatestChk.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                startTimeEntry.setEnabled(!useLatestChk.getSelection());
                endTimeEntry.setEnabled(!useLatestChk.getSelection());
            }
        });
        Label startTimeLabel = new Label(dateComp, SWT.CENTER);
        startTimeLabel.setText("Start Time: ");
        this.startTimeEntry = new DateTimeEntry(dateComp, "yyyy-MM-dd HH:mm");
        startTimeEntry.setEnabled(false);

        Label endTimeLabel = new Label(dateComp, SWT.CENTER);
        endTimeLabel.setText("End Time: ");
        this.endTimeEntry = new DateTimeEntry(dateComp, "yyyy-MM-dd HH:mm");
        endTimeEntry.setEnabled(false);

        Calendar startDate = Calendar.getInstance();
        startDate.setTime(dateCycleList.first());
        startDate.add(Calendar.HOUR_OF_DAY, -1);
        startTimeEntry.setDate(startDate.getTime());

        Calendar endDate = Calendar.getInstance();
        endDate.setTime(dateCycleList.last());
        // Round up to the next minute
        endDate.add(Calendar.SECOND, 60 - endDate.get(Calendar.SECOND));
        endTimeEntry.setDate(endDate.getTime());

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
        gd.minimumWidth = btnWidth;
        Button okBtn = new Button(buttonComp, SWT.PUSH);
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
        gd.minimumWidth = btnWidth;
        Button cancelBtn = new Button(buttonComp, SWT.PUSH);
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

    @Override
    protected Layout constructShellLayout() {
        // Create the main layout for the shell.
        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;

        return mainLayout;
    }

    @Override
    protected void preOpened() {
        shell.layout();
        shell.pack();
    }

    /**
     * OK Button action method.
     */
    @SuppressWarnings("unchecked")
    private void handleOk() {
        PDATimeSelection data = new PDATimeSelection();
        if (useLatestChk.getSelection()) {
            data.setLatest(true);
            startTimeEntry.setDate(dateCycleList.last());
            Calendar endDateCal = Calendar.getInstance();
            endDateCal.setTime(dateCycleList.last());
            // Round up to the next minute
            endDateCal.add(Calendar.SECOND,
                    60 - endDateCal.get(Calendar.SECOND));
            endTimeEntry.setDate(endDateCal.getTime());
        } else {
            DataDeliveryGUIUtils.latencyValidChk(priorityComp.getLatencyValue(),
                    DataDeliveryUtils.getMaxLatency(subscription));
        }
        data.setTimeRange(new TimeRange(startTimeEntry.getDate(),
                endTimeEntry.getDate()));
        setReturnValue(data);
    }
}
