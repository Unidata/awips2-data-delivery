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
package com.raytheon.uf.viz.datadelivery.browser;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.viz.datadelivery.filter.AbstractFilterComp;
import com.raytheon.uf.viz.datadelivery.filter.FilterImages.ExpandItemState;
import com.raytheon.uf.viz.datadelivery.filter.IFilterUpdate;
import com.raytheon.viz.ui.widgets.duallist.DualList;
import com.raytheon.viz.ui.widgets.duallist.DualListConfig;
import com.raytheon.viz.ui.widgets.duallist.IUpdate;
import com.raytheon.viz.ui.widgets.duallist.SearchUtils;

/**
 * Standard Filter Composite
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 21, 2012            mpduff      Initial creation
 * Aug 08, 2012    863     jpiatt      Added new interface method.
 * Jan 07, 2013   1432     mpduff      Fix case sensitive and exclude checkboxes.
 * Feb 25, 2013   1588     mpduff      Fix match any/all.
 * Aug 20, 2013   1733     mpduff      Match any/all now executes the search on selection.
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class FilterComp extends AbstractFilterComp implements IUpdate {

    /** Search text field */
    private Text regExTxt;

    /** Match any button */
    private Button matchAnyBtn;

    /** Match all button */
    private Button matchAllBtn;

    /** Case sensitive check */
    private Button caseBtn;

    /** Exclusion check */
    private Button exclusionBtn;

    /** Match any flag */
    private boolean matchAnyFlag = true;

    /** Dual list widget */
    private DualList dualList;

    /** Dual list configuration */
    private final DualListConfig dualConfig;

    /** Filter Configuration */
    private final FilterConfig config;

    /** Flag */
    private boolean dirty = false;

    /**
     * Constructor.
     * 
     * @param parent
     *            Parent Control
     * @param style
     *            Control style
     * @param callback
     *            Callback object
     * @param config
     *            Control config object
     * @param idx
     *            Control index
     */
    public FilterComp(Composite parent, int style, IFilterUpdate callback,
            FilterConfig config, int idx) {
        super(parent, callback, idx);
        this.config = config;
        this.dualConfig = config.getDualConfig();

        init();
    }

    /**
     * Initialize the control
     */
    private void init() {
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        GridLayout gl = new GridLayout(1, true);
        gl.verticalSpacing = 2;
        gl.marginHeight = 2;
        gl.marginWidth = 2;
        this.setLayout(gl);
        this.setLayoutData(gd);

        if (config.isRegExVisible()) {
            createRegEx();
            if (config.isDualListVisible()) {
                addSeparator(this);
            }
        }

        if (config.isMatchControlVisible()) {
            createMatchControls();
        }

        if (config.isDualListVisible()) {
            createDualList();
        }
    }

    /**
     * Create the search widget
     */
    private void createRegEx() {
        Composite controlComp = new Composite(this, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        controlComp.setLayout(gl);
        controlComp.setLayoutData(gd);

        Label regExLbl = new Label(controlComp, SWT.NONE);
        regExLbl.setText("Search: ");

        regExTxt = new Text(controlComp, SWT.BORDER);
        gd = new GridData(225, SWT.DEFAULT);
        regExTxt.setLayoutData(gd);
        regExTxt.setToolTipText("Enter text for search");
        regExTxt.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // handleFocusLost(e);
            }
        });
        regExTxt.addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent e) {
                handleSearch();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                // Not Called

            }
        });

        caseBtn = new Button(controlComp, SWT.CHECK);
        caseBtn.setText("Case Sensitive");
        caseBtn.setToolTipText("Match upper and lower case");
        caseBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dualConfig.setCaseFlag(caseBtn.getSelection());
                handleSearch();
            }
        });

        exclusionBtn = new Button(controlComp, SWT.CHECK);
        exclusionBtn.setText("Exclude");
        exclusionBtn.setToolTipText("Does not contain search text");
        exclusionBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dualConfig.setExcludeFlag(exclusionBtn.getSelection());
                handleSearch();
            }
        });

    }

    /**
     * Create the match radio buttons
     */
    private void createMatchControls() {
        Composite matchComp = new Composite(this, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        gl.horizontalSpacing = 10;
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        matchComp.setLayout(gl);
        matchComp.setLayoutData(gd);

        matchAnyBtn = new Button(matchComp, SWT.RADIO);
        matchAnyBtn.setText("Match Any");
        matchAnyBtn.setSelection(true);
        matchAnyBtn.setToolTipText("Results match any in selected list");
        matchAnyBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (matchAnyBtn.getSelection()) {
                    matchAnyFlag = true;
                }
                handleSearch();
            }
        });

        matchAllBtn = new Button(matchComp, SWT.RADIO);
        matchAllBtn.setText("Match All");
        matchAllBtn.setToolTipText("Results match all selected list");
        matchAllBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (matchAllBtn.getSelection()) {
                    matchAnyFlag = false;
                }
                handleSearch();
            }
        });
    }

    /**
     * Create the Dual List widget
     */
    private void createDualList() {
        dualList = new DualList(this, SWT.NONE, dualConfig, this);
    }

    /**
     * Add a separator
     * 
     * @param parentComp
     *            Composite that gets the separator
     */
    private void addSeparator(Composite parentComp) {
        GridLayout gl = (GridLayout) parentComp.getLayout();

        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gd.horizontalSpan = gl.numColumns;
        Label sepLbl = new Label(parentComp, SWT.SEPARATOR | SWT.HORIZONTAL);
        sepLbl.setLayoutData(gd);
    }

    /**
     * Reset the filter controls.
     */
    @Override
    public void resetControls() {
        if (regExTxt != null) {
            regExTxt.setText("");
        }

        dualList.clearSelection();
        setCurrentState(ExpandItemState.NoEntries);
    }

    /**
     * Handle the search action.
     */
    private void handleSearch() {
        boolean excludeSearch = exclusionBtn.getSelection();

        String search = regExTxt.getText();
        dualConfig.setSearchField(search);
        dualConfig.setMatchAny(matchAnyFlag);
        List<String> fullList = dualConfig.getFullList();

        if (search != null && search.length() > 0) {
            String[] filteredList = dualConfig.getFullList().toArray(
                    new String[dualConfig.getFullList().size()]);

            List<String> tmpFilterList = SearchUtils.search(search,
                    filteredList, matchAnyFlag, caseBtn.getSelection(),
                    excludeSearch);

            // Clear the list and add the newly filtered items
            dualList.clearAvailableList(false);
            for (String s : dualList.getSelectedListItems()) {
                tmpFilterList.remove(s);
            }
            dualList.setAvailableItems(tmpFilterList);
            return;
        } else {

            // Clear the list and repopulate with the full list
            dualList.clearAvailableList(false);
            for (String s : dualList.getSelectedListItems()) {
                fullList.remove(s);
            }
            dualList.setAvailableItems(fullList);
        }

        dualList.enableDisableLeftRightButtons();
    }

    /**
     * Set the selectedItems into the selected list.
     * 
     * @param selectedItems
     */
    public void setSelectedItems(String[] selectedItems) {
        dualList.setSelectedItems(selectedItems);
    }

    /**
     * Select the items passed in. This moves the items from the available list
     * into the selected list.
     * 
     * @param items
     */
    public void selectItems(String[] items) {
        dualList.selectItems(items);
    }

    /**
     * Get the rows in the Selected List.
     * 
     * @return array list of selected items
     */
    public String[] getSelectedListItems() {
        return dualList.getSelectedListItems();
    }

    /**
     * Get the Filter Configuration.
     * 
     * @return FilterConfig object
     */
    public FilterConfig getConfig() {
        return config;
    }

    /**
     * Get flag for matching.
     * 
     * @return boolean true if match any entry
     */
    public boolean matchAnyEntry() {
        return matchAnyFlag;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.datadelivery.common.ui.IUpdate#hasEntries(boolean)
     */
    @Override
    public void hasEntries(boolean entries) {
        if (entries) {
            setCurrentState(ExpandItemState.Entries);
        } else {
            setCurrentState(ExpandItemState.NoEntries);
        }
        this.dirty = true;
    }

    /**
     * Flag for dirty.
     * 
     * @return the dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * If any mods have been made to the composite selections, set dirty true.
     * 
     * @param dirty
     *            the dirty to set
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public void selectionChanged() {
        // unused

    }
}
