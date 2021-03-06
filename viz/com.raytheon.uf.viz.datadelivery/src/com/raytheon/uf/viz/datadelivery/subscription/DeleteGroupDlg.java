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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.service.GroupDefinitionService;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.datadelivery.common.ui.GroupSelectComp;
import com.raytheon.uf.viz.datadelivery.common.ui.IGroupAction;
import com.raytheon.uf.viz.datadelivery.services.DataDeliveryServices;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;

/**
 * GUI to delete a group.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 2, 2013  1441       djohnson     Initial creation
 * Jan 18, 2013 1441       djohnson     Use group definition service.
 * Feb 26, 2013 1643       djohnson     Catch any exception.
 * Oct 19, 2015 4996       dhladky      Removed "are you sure" un-needed double auth message.
 * Feb 01, 2016 5289       tgurney      Add missing minimize button in trim
 * Mar 16, 2016 3919       tjensen      Cleanup unneeded interfaces
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class DeleteGroupDlg extends CaveSWTDialog {

    private final IGroupAction groupAction;

    /** The Main Composite */
    private Composite mainComp;

    /** The Subscription Group Information Composite */
    private GroupSelectComp groupSelectComp;

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DeleteGroupDlg.class);

    private final GroupDefinitionService groupService = DataDeliveryServices
            .getGroupDefinitionService();

    /**
     * @param shell
     * @param groupAction
     */
    public DeleteGroupDlg(Shell shell, IGroupAction groupAction) {
        super(shell, SWT.DIALOG_TRIM | SWT.MIN, CAVE.INDEPENDENT_SHELL
                | CAVE.DO_NOT_BLOCK);

        setText("Delete Group");

        this.groupAction = groupAction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeComponents(Shell shell) {
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(1, false);
        mainComp = new Composite(shell, SWT.NONE);
        mainComp.setLayout(gl);
        mainComp.setLayoutData(gd);

        groupSelectComp = new GroupSelectComp(mainComp, true);

        createButtons();
    }

    /**
     * Create buttons at the bottom of the dialog.
     */
    private void createButtons() {
        int buttonWidth = 75;
        GridData btnData = new GridData(buttonWidth, SWT.DEFAULT);

        GridData gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(2, false);
        Composite btnComp = new Composite(mainComp, SWT.NONE);
        btnComp.setLayout(gl);
        btnComp.setLayoutData(gd);

        Button deleteBtn = new Button(btnComp, SWT.PUSH);
        deleteBtn.setText("Delete");
        deleteBtn.setLayoutData(btnData);
        deleteBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (handleDelete()) {
                    close();
                }
            }
        });

        btnData = new GridData(buttonWidth, SWT.DEFAULT);
        Button cancelBtn = new Button(btnComp, SWT.PUSH);
        cancelBtn.setText("Cancel");
        cancelBtn.setLayoutData(btnData);
        cancelBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                close();
            }
        });
    }

    private boolean handleDelete() {
        if (groupSelectComp.isGroupSelected()) {

            final String groupName = groupSelectComp.getGroupName();

            try {
                groupService.deleteGroupDefinition(DataDeliveryHandlers
                        .getGroupDefinitionHandler().getByName(groupName));
                groupAction.loadGroupNames();
                return true;
            } catch (Exception e) {
                statusHandler.handle(Priority.ERROR,
                        "Unable to delete a group.", e);
            }
        } else {
            DataDeliveryUtils.showMessage(getShell(), SWT.OK, "Delete Group",
                    "Please select a valid group.");
        }

        return false;
    }
}
