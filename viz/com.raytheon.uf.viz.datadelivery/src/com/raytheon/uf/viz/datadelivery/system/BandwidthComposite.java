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
package com.raytheon.uf.viz.datadelivery.system;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryGUIUtils;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;
import com.raytheon.viz.ui.widgets.ApplyCancelComposite;
import com.raytheon.viz.ui.widgets.IApplyCancelAction;

/**
 * Bandwidth settings composite
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 06, 2013  2180     mpduff    Initial creation
 * Oct 17, 2013  2455     skorolev  Fixed a problem with Changes Applied window.
 * Nov 19, 2014  2749     ccody     Put Set Avail Bandwidth Save into async,
 *                                  non-UI thread
 * Mar 28, 2016  5482     randerso  Fixed GUI sizing issues
 * Jun 20, 2016  5676     tjensen   Use showYesNoMessage for prompts that need
 *                                  to block
 * Jun 12, 2017  6222     tgurney   Set minimum bandwidth to 1 kB/s
 * 
 * </pre>
 * 
 * @author mpduff
 */

public class BandwidthComposite extends Composite implements IApplyCancelAction {
    /** Status Handler */
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(BandwidthComposite.class);

    /** Available bandwidth spinner widget */
    private Spinner availBandwidthSpinner;

    /**
     * Save success boolean for org.eclipse.core.runtime.jobs.Job scheduled in:
     * performAsyncAvailableBandwidthChanges method.
     */
    private boolean saveSuccessful = false;

    /** The listener that should be used to signify changes were made **/
    private final Runnable changesWereMade = new Runnable() {
        @Override
        public void run() {
            buttonComp.enableButtons(true);
        }
    };

    /** Button composite */
    private ApplyCancelComposite buttonComp;

    /**
     * Constructor.
     * 
     * @param parent
     *            Parent Composite
     * @param style
     *            Style bits
     */
    public BandwidthComposite(Composite parent, int style) {
        super(parent, style);
        init();
    }

    /**
     * Initialize the class
     */
    private void init() {
        GridLayout gl = new GridLayout(1, true);
        this.setLayout(gl);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        this.setLayoutData(gd);

        gl = new GridLayout(1, false);
        gd = new GridData(SWT.VERTICAL, SWT.DEFAULT, true, false);
        Composite configurationComposite = new Composite(this, SWT.NONE);
        configurationComposite.setLayout(gl);
        configurationComposite.setLayoutData(gd);

        // Label for directions
        gd = new GridData(SWT.FILL, SWT.DEFAULT, false, false);
        Label directionsLabel = new Label(configurationComposite, SWT.NONE);
        directionsLabel.setLayoutData(gd);
        directionsLabel
                .setText("Please enter the available bandwidth for the OPSNET network.");

        Composite outerComp = new Composite(configurationComposite, SWT.NONE);
        gl = new GridLayout(2, false);
        gl.marginWidth = 0;
        outerComp.setLayout(gl);

        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        outerComp.setLayoutData(gd);

        // Bandwidth spinner
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT);
        Label availBandwith = new Label(outerComp, SWT.NONE);
        availBandwith.setLayoutData(gd);
        availBandwith.setText("OPSNET Bandwidth (kB/s):");

        final Spinner availBandwidthSpinner = new Spinner(outerComp, SWT.BORDER);
        availBandwidthSpinner.setMinimum(1);
        availBandwidthSpinner.setMaximum(300_000);
        availBandwidthSpinner.setToolTipText("Select bandwidth in Kilobytes");
        this.availBandwidthSpinner = availBandwidthSpinner;

        // Buttons
        buttonComp = new ApplyCancelComposite(this, SWT.NONE, this);

        loadConfiguration();
    }

    /**
     * Load the configuration
     */
    private void loadConfiguration() {
        DataDeliveryGUIUtils.removeListeners(availBandwidthSpinner,
                SWT.Selection, SWT.DefaultSelection);

        final int availableBandwidth = SystemRuleManager
                .getAvailableBandwidth(Network.OPSNET);

        availBandwidthSpinner.setSelection(availableBandwidth);
        availBandwidthSpinner.addSelectionListener(DataDeliveryGUIUtils
                .addValueChangedSelectionListener(availableBandwidth,
                        availBandwidthSpinner, changesWereMade));
    }

    /**
     * Save the configuration.
     * 
     * @return true if saved
     */
    private boolean saveConfiguration() {

        boolean applyChanges = false;

        final int bandwidth = availBandwidthSpinner.getSelection();

        Set<String> unscheduledSubscriptions = SystemRuleManager
                .proposeToSetAvailableBandwidth(Network.OPSNET, bandwidth);
        if (unscheduledSubscriptions == null
                || unscheduledSubscriptions.isEmpty()) {
            applyChanges = true;
        } else {
            Set<String> subscriptionNames = new TreeSet<>(
                    unscheduledSubscriptions);

            StringBuilder sb = new StringBuilder(StringUtil.createMessage(
                    "Changing the bandwidth for " + Network.OPSNET
                            + " will unschedule the following subscriptions:",
                    subscriptionNames));
            sb.append(StringUtil.NEWLINE).append(StringUtil.NEWLINE);
            sb.append("Would you like to change the bandwidth anyway?");

            int response = DataDeliveryUtils.showYesNoMessage(getParent()
                    .getShell(), "Bandwidth Amount", sb.toString());
            if (response == SWT.YES) {
                applyChanges = true;
            }
        }

        if (applyChanges) {
            performAsyncAvailableBandwidthChanges(bandwidth);
        }

        return applyChanges;
    }

    /**
     * Asynchronous save action method.
     * 
     * This method creates an org.eclipse.core.runtime.jobs.Job to save
     * Bandwidth value changes of the the configuration. The request-response
     * loop for this call is synchronous and takes several seconds. The Job
     * allows the processing and reporting to be performed asynchronously, in a
     * separate thread. The JobChangeAdapter (fired from within the non-UI Job
     * thread re-attaches" to the UI thread and displays the popup notification
     * Dialogs.
     * 
     * The Bandwidth Composite save (Change OPSNET Bandwidth save) requires a
     * complete refresh of the BandwidthManager. This operation takes time. This
     * implementation allows the CAVE application to remain responsive during
     * the BandwidthManager refresh and reshuffle, and allows the server
     * response (success or failure) to be presented to the user.
     * 
     * @param bandwidth
     *            New bandwidth availability parameter. Must be 'final'. Cannot
     *            refer to a non-final variable bandwidth inside an inner class
     *            defined in a different method.
     */
    protected void performAsyncAvailableBandwidthChanges(final int bandwidth) {

        // Must be final. Cannot refer to
        final Shell parentShell = this.getShell();

        Job job = new Job("Updating Bandwidth for Network: " + Network.OPSNET
                + "...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                DataDeliveryGUIUtils.markBusyInUIThread(parentShell);

                saveSuccessful = SystemRuleManager.forceSetAvailableBandwidth(
                        Network.OPSNET, bandwidth);

                return Status.OK_STATUS;
            }
        };
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {

                if (!event.getResult().isOK()) {
                    saveSuccessful = false;
                }

                VizApp.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (saveSuccessful) {
                                Shell currentShell = null;
                                if (parentShell != null
                                        && !parentShell.isDisposed()) {

                                    currentShell = parentShell;
                                    currentShell.forceFocus();
                                    currentShell.forceActive();
                                    DataDeliveryUtils
                                            .showChangesWereAppliedMessage(currentShell);
                                } else {
                                    statusHandler
                                            .handle(Priority.INFO,
                                                    "Bandwidth Change",
                                                    "The changes were successfully applied.");
                                }
                            } else {
                                statusHandler
                                        .handle(Priority.ERROR,
                                                "Bandwidth Change",
                                                "Unable to change the bandwidth for network "
                                                        + Network.OPSNET
                                                        + ".  Please check the server for details.");
                                Shell currentShell = null;
                                if (parentShell != null
                                        && !parentShell.isDisposed()) {
                                    currentShell = parentShell;
                                    currentShell.forceFocus();
                                    currentShell.forceActive();
                                    MessageBox messageDialog = new MessageBox(
                                            currentShell, SWT.ERROR);
                                    messageDialog.setText("Apply Failed");
                                    messageDialog
                                            .setMessage("The apply action (Change Available OPSNET Bandwidth) failed.\nSee server logs for details.");
                                    messageDialog.open();
                                }
                            }
                        } finally {
                            if (parentShell != null
                                    && !parentShell.isDisposed()) {
                                DataDeliveryGUIUtils
                                        .markNotBusyInUIThread(parentShell);
                            }
                        }
                    }
                });
            }
        });

        job.schedule();

        return;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean apply() {
        return saveConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() {
        loadConfiguration();
    }
}
