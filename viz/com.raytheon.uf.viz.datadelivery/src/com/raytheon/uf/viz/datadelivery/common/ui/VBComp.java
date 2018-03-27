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
package com.raytheon.uf.viz.datadelivery.common.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 *
 * Composition for Volume Browser related fields.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 08, 2017 6355       nabowle     Initial creation
 *
 * </pre>
 *
 * @author nabowle
 */
public class VBComp extends Composite {

    private final Composite parentComp;

    private Button verticalCheck;

    public VBComp(Composite parent) {
        super(parent, SWT.NONE);
        this.parentComp = parent;
        init();
    }

    private void init() {
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        GridLayout gl = new GridLayout(1, true);
        gl.verticalSpacing = 2;
        gl.marginHeight = 2;
        gl.marginWidth = 2;
        this.setLayout(gl);
        this.setLayoutData(gd);

        verticalCheck = new Button(parentComp, SWT.CHECK);
        verticalCheck.setText("Subscription contains vertical data.");
        verticalCheck.setToolTipText(
                "Determines whether the subscription will be available for Cross Section, Time Height, Var Height, and Sounding menus.");
        GridData gridData = new GridData();
        verticalCheck.setLayoutData(gridData);
    }

    public boolean isVertical() {
        return this.verticalCheck.getSelection();
    }

    public void setVertical(boolean vertical) {
        this.verticalCheck.setSelection(vertical);
    }

}
