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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;

/**
 * Create a copy of a subscription dialog.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 16, 2012            mpduff      Initial creation.
 * Dec 17, 2012   1434     mpduff      Don't allow underscores in name.
 * Nov 14, 2013   2538     mpduff      Check for same name entered.
 * May 17, 2015   4047     dhladky     verified non-blocking, restored functionality (copy was broken)
 * Mar 28, 2016   5482     randerso    Fixed GUI sizing issues
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class FileNameDlg extends CaveSWTDialog {
    /** Filename text field */
    private Text nameTxt;

    /** The original subscription name */
    private final String origName;

    /**
     * Constructor.
     * 
     * @param parent
     *            The parent shell
     * @param origName
     *            The original subscription name
     */
    public FileNameDlg(Shell parent, String origName) {
        super(parent, SWT.DIALOG_TRIM, CAVE.DO_NOT_BLOCK
                | CAVE.INDEPENDENT_SHELL);
        this.setText("Copy Subscription");
        this.origName = origName;
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.ui.dialogs.CaveSWTDialogBase#initializeComponents(org
     * .eclipse.swt.widgets.Shell)
     */
    @Override
    protected void initializeComponents(Shell shell) {
        Composite mainComp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        mainComp.setLayout(gl);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        mainComp.setLayoutData(gd);

        // New Subscription Name text box
        gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        Label lbl = new Label(mainComp, SWT.NONE);
        lbl.setText("New Subscription Name:");
        lbl.setLayoutData(gd);

        nameTxt = new Text(mainComp, SWT.BORDER);
        GC gc = new GC(nameTxt);
        int textWidth = gc.getFontMetrics().getAverageCharWidth() * 45;
        gc.dispose();

        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gd.widthHint = textWidth;
        nameTxt.setLayoutData(gd);

        nameTxt.setText(origName);
        nameTxt.selectAll();

        Composite btnComp = new Composite(shell, SWT.NONE);
        gl = new GridLayout(2, true);
        btnComp.setLayout(gl);
        gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        btnComp.setLayoutData(gd);

        // OK button
        int btnWidth = btnComp.getDisplay().getDPI().x;
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gd.minimumWidth = btnWidth;
        Button okBtn = new Button(btnComp, SWT.PUSH);
        okBtn.setText("OK");
        okBtn.setLayoutData(gd);
        okBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (handleOk()) {
                    close();
                }
            }
        });

        // Cancel button
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gd.minimumWidth = btnWidth;
        Button cancelBtn = new Button(btnComp, SWT.PUSH);
        cancelBtn.setText("Cancel");
        cancelBtn.setLayoutData(gd);
        cancelBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                close();
            }
        });
    }

    /**
     * OK Button action handler.
     */
    private boolean handleOk() {
        String name = nameTxt.getText().trim();

        String title;
        String message;
        if (name == null || name.isEmpty()) {
            title = "Name Required";
            message = "Name required. A Subscription Name must be entered.";
        } else if (name.equals(origName)) {
            title = "Different Name Required";
            message = "A different name must be used.";
        } else if (name.contains("_") || name.contains(" ")) {
            title = "Name Invalid";
            message = "Underscore/space is not a valid character for a subscription name.";
        } else {
            setReturnValue(name);
            return true;
        }

        DataDeliveryUtils.showMessage(getShell(), SWT.OK, title, message);
        return false;
    }

}