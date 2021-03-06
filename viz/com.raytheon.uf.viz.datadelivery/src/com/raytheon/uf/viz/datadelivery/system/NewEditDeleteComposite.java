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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.raytheon.viz.ui.widgets.IEnableAction;

/**
 * New/Edit/Delete Button composite.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 07, 2013   2180     mpduff      Initial creation
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class NewEditDeleteComposite extends Composite implements IEnableAction {
    /** Button Width. */
    private final int buttonWidth = 70;

    /** Edit button. */
    private Button editBtn;

    /** New button. */
    private Button newBtn;

    /** Delete button. */
    private Button deleteBtn;

    /** Flag for create and edit. */
    private boolean create;

    /** Button callback action */
    private final INewEditDeleteAction callback;

    /**
     * Constructor.
     * 
     * @param parent
     *            Parent composite
     * @param style
     *            style bits
     * @param callback
     *            button action callback
     */
    public NewEditDeleteComposite(Composite parent, int style,
            INewEditDeleteAction callback) {
        super(parent, style);
        this.callback = callback;
        init();
    }

    /**
     * Initialize class
     */
    private void init() {
        GridLayout gl = new GridLayout(1, false);
        GridData gd = new GridData(SWT.RIGHT, SWT.DEFAULT, true, false);
        this.setLayout(gl);
        this.setLayoutData(gd);

        GridData actionData = new GridData(SWT.DEFAULT, SWT.BOTTOM, false, true);
        GridLayout actionLayout = new GridLayout(3, false);
        Composite actionComp = new Composite(this, SWT.NONE);
        actionComp.setLayout(actionLayout);
        actionComp.setLayoutData(actionData);

        GridData btnData = new GridData(buttonWidth, SWT.DEFAULT);
        btnData.horizontalAlignment = SWT.RIGHT;

        newBtn = new Button(actionComp, SWT.PUSH);
        newBtn.setText("New...");
        newBtn.setLayoutData(btnData);
        newBtn.setEnabled(true);
        newBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                callback.newAction();
            }
        });

        btnData = new GridData(buttonWidth, SWT.DEFAULT);
        btnData.widthHint = buttonWidth;
        btnData.heightHint = SWT.DEFAULT;
        editBtn = new Button(actionComp, SWT.PUSH);
        editBtn.setText("Edit...");
        editBtn.setLayoutData(btnData);
        editBtn.setEnabled(false);
        editBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                callback.editAction();
            }
        });

        btnData = new GridData(buttonWidth, SWT.DEFAULT);
        btnData.widthHint = buttonWidth;
        btnData.heightHint = SWT.DEFAULT;
        deleteBtn = new Button(actionComp, SWT.PUSH);
        deleteBtn.setText("Delete");
        deleteBtn.setLayoutData(btnData);
        deleteBtn.setEnabled(false);
        deleteBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                callback.deleteAction();
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableButtons(boolean enabled) {
        editBtn.setEnabled(enabled);
        deleteBtn.setEnabled(enabled);
    }
}
