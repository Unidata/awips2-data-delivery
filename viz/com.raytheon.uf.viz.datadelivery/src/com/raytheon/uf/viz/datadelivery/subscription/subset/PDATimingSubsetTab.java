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

import java.util.SortedSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.time.util.ImmutableDate;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.PDATimeXML;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.TimeXML;

/**
 * PDA Time Subset Tab. Sets the data retrieval dates.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 29, 2014  3121     dhladky   Initial creation.
 * Apr 27, 2016  5366     tjensen   Only allow the most recent available time to
 *                                  be selected.
 * Aug 17, 2016  5772     rjpeter   Fix time handling.
 * 
 * </pre>
 * 
 * @author dhladky
 */
public class PDATimingSubsetTab extends DataTimingSubsetTab {

    private Text ltValueTxt;

    /**
     * Constructor.
     * 
     * @param parentComp
     *            Parent composite
     * @param callback
     *            The callback class
     * @param shell
     *            the shell
     */
    public PDATimingSubsetTab(Composite parentComp, IDataSize callback,
            Shell shell, SortedSet<ImmutableDate> times) {
        super(parentComp, callback, shell);

        init(times);
    }

    private void init(SortedSet<ImmutableDate> times) {

        GridLayout gl = new GridLayout(1, false);
        GridData gd = new GridData(SWT.LEFT, SWT.DEFAULT, true, false);

        Group timeGrp = new Group(parentComp, SWT.NONE);
        timeGrp.setText(" Dataset Time ");
        timeGrp.setLayout(gl);
        timeGrp.setLayoutData(gd);

        gd = new GridData();
        gd.horizontalSpan = 1;
        Label l = new Label(timeGrp, SWT.LEFT);
        l.setText("PDA retrieves the latest metadata time available.");
        l.setLayoutData(gd);

        gl = new GridLayout(4, false);
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);

        Composite timeComp = new Composite(timeGrp, SWT.NONE);
        timeComp.setLayout(gl);
        timeComp.setLayoutData(gd);

        Label ltLabel = new Label(timeComp, SWT.NONE);
        ltLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        ltLabel.setText("Latest Time:");

        ltValueTxt = new Text(timeComp, SWT.BORDER);
        ltValueTxt.setLayoutData(new GridData(250, SWT.DEFAULT));

        if ((times != null) && !times.isEmpty()) {
            ltValueTxt.setText(times.first().toString());
        } else {
            ltValueTxt.setText("No Data Available");
        }

        ltValueTxt.setEditable(false);
    }

    /**
     * Get the save info for the tab.
     * 
     * @return TimeXML object
     */
    public TimeXML getSaveInfo() {
        return new PDATimeXML();
    }
}
