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

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;
import com.raytheon.viz.ui.widgets.DualListComposite;
import com.raytheon.viz.ui.widgets.duallist.DualListConfig;

/**
 * Shared site selection dialog.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------------------
 * Apr 29, 2013  1040     mpduff    Initial creation
 * Feb 11, 2014  2771     bgonzale  Show all SiteDataTypes in site list.
 * Apr 02, 2014  2974     dhladky   DD ID added to list for dropdowns in DD.
 * May 17, 2015  4047     dhladky   verified non-blocking.
 * Mar 28, 2016  5482     randerso  Fixed GUI sizing issues
 * Feb 28, 2017  6121     randerso  Update DualListConfig settings
 * Aug 31, 2017  6396     nabowle   Include the site in site list.
 *
 * </pre>
 *
 * @author mpduff
 */

public class SiteSelectionDlg extends CaveSWTDialog {
    private DualListComposite comp;

    private final String[] sharedSites;

    /**
     * Constructor.
     *
     * @param parent
     *            Parent Shell
     * @param site
     *            Home Site
     * @param sharedSites
     *            list of shared sites
     */
    public SiteSelectionDlg(Shell parent, String site, String[] sharedSites) {
        super(parent, SWT.APPLICATION_MODAL | SWT.RESIZE, CAVE.DO_NOT_BLOCK);
        this.setText(site + " Site Selection");
        this.sharedSites = sharedSites;
    }

    @Override
    protected void initializeComponents(Shell shell) {
        GridLayout gl = new GridLayout(1, false);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        Composite mainComp = new Composite(shell, SWT.NONE);
        mainComp.setLayout(gl);
        mainComp.setLayoutData(gd);

        gl = new GridLayout(1, false);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        Label label = new Label(mainComp, SWT.NONE);
        label.setText("Select sites to share with:");
        gl = new GridLayout(1, false);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);

        DualListConfig config = new DualListConfig();
        config.setAvailableListLabel("Available Sites:");
        config.setSelectedListLabel("Selected Sites:");
        config.setShowUpDownBtns(false);
        config.setVisibleItems(12);
        config.setListWidthInChars(12);
        config.setFullList(getSiteList());
        config.setSelectedList(Arrays.asList(sharedSites));

        comp = new DualListComposite(mainComp, config);
        comp.setLayout(gl);
        comp.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        Composite buttonComp = new Composite(this.getShell(), SWT.NONE);
        buttonComp.setLayout(gl);
        buttonComp.setLayoutData(gd);

        int buttonWidth = buttonComp.getDisplay().getDPI().x;

        Button okBtn = new Button(buttonComp, SWT.NONE);
        okBtn.setText("OK");
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gd.minimumWidth = buttonWidth;
        okBtn.setLayoutData(gd);
        okBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleOk();
            }
        });

        Button cancelBtn = new Button(buttonComp, SWT.NONE);
        cancelBtn.setText("Cancel");
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gd.minimumWidth = buttonWidth;
        cancelBtn.setLayoutData(gd);
        cancelBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                close();
            }
        });
    }

    /**
     * Get a list of WFO/RFC ids
     *
     * @return list of site ids
     */
    private List<String> getSiteList() {
        List<String> siteList = DataDeliveryUtils.getDataDeliverySiteList();

        return siteList;
    }

    /**
     * OK event handler.
     */
    private void handleOk() {
        String[] selectedItems = comp.getSelectedItems();
        setReturnValue(selectedItems);
        close();
    }
}