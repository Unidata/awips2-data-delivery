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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.PDATimeXML;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.TimeXML;
import com.raytheon.viz.ui.widgets.duallist.DualList;
import com.raytheon.viz.ui.widgets.duallist.DualListConfig;

/**
 * PDA Time Subset Tab. Sets the data retrieval dates.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 29, 2014   3121     dhladky      Initial creation.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDATimingSubsetTab extends DataTimingSubsetTab {
    
    /** Time list */
    private DualList timeDualList;

    /** Time configuration */
    private final DualListConfig timeHoursDualConfig = new DualListConfig();

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
            Shell shell, List<String> times) {
        super(parentComp, callback, shell);

        init(times);
    }

    private void init(List<String> times) {

        GridLayout gl = new GridLayout(1, false);
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);

        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginWidth = 0;
        gl.marginHeight = 0;

        Group timeGrp = new Group(parentComp, SWT.NONE);
        timeGrp.setText(" Dataset Times ");
        timeGrp.setLayout(gl);
        timeGrp.setLayoutData(gd);

        timeHoursDualConfig.setAvailableListLabel("Available Times:");
        timeHoursDualConfig.setSelectedListLabel("Selected Times:");
        timeHoursDualConfig.setListHeight(125);
        timeHoursDualConfig.setListWidth(175);
        timeHoursDualConfig.setShowUpDownBtns(false);
        timeHoursDualConfig.setNumericData(true);
        timeHoursDualConfig.setFullList(times);
   
        timeDualList = new DualList(timeGrp, SWT.NONE,
                timeHoursDualConfig, this);
    }

    /**
     * Get the selected times
     * 
     * @return the selected times
     */
    public String[] getSelectedTimes() {
        return this.timeDualList.getSelectedListItems();
    }

    /**
     * List of times to set
     * 
     * @param fcstHours
     *            list of times
     */
    public void setSelectedTimes(List<String> times) {
        timeDualList.clearSelection();

        if (!CollectionUtil.isNullOrEmpty(times)) {
            String[] selTimes = times.toArray(new String[times.size()]);
            timeDualList.selectItems(selTimes);
        }
    }

    /**
     * Set the available times
     * 
     * @param times
     *            list of times
     */
    public void setAvailableTimes(List<String> times) {
        timeDualList.setFullList(new ArrayList<String>(times));
    }

    /**
     * Update the selected times.
     * 
     * @param fcstHours
     *            list of times
     */
    public void updateSelectedTimes(List<String> times) {
        List<String> selectedTimes = new ArrayList<String>();
        String[] selectedItems = timeDualList.getSelectedSelection();

        // Add the saved hours
        for (String time : timeDualList.getAvailableListItems()) {
            if (times.contains(time)) {
                selectedTimes.add(time);
            }
        }

        // Add in the previously selected times
        selectedTimes.addAll(Arrays.asList(selectedItems));

        // Sort the times
        List<Integer> intList = new ArrayList<Integer>();
        for (String hr : selectedTimes) {
            intList.add(Integer.parseInt(hr));
        }

        Collections.sort(intList);

        selectedTimes.clear();
        for (int i : intList) {
            selectedTimes.add(String.valueOf(i));
        }
        timeDualList.selectItems(selectedTimes.toArray(new String[selectedTimes
                .size()]));
    }

    /**
     * Are the data valid?
     * 
     * @return true if valid
     */
    public boolean isValid() {
        if (timeDualList.getSelectedListItems().length > 0) {
            return true;
        }
        return false;
    }

    /**
     * Get the save info for the tab.
     * 
     * @return TimeXML object
     */
    public TimeXML getSaveInfo() {
        PDATimeXML time = new PDATimeXML();
        time.setTimes(new ArrayList<String>(Arrays.asList(timeDualList
                .getSelectedListItems())));
        return time;
    }
}
