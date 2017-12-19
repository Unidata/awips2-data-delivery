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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.raytheon.uf.common.datadelivery.registry.LevelGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.registry.ParameterUtils;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.Pair;
import com.raytheon.viz.ui.widgets.ToggleSelectList;
import com.raytheon.viz.ui.widgets.duallist.DualList;
import com.raytheon.viz.ui.widgets.duallist.DualListConfig;
import com.raytheon.viz.ui.widgets.duallist.IUpdate;

/**
 * Level/Parameter selection widgets.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 16, 2012           mpduff    Initial creation.
 * Aug 08, 2012  863      jpiatt    Added clean & dirty checks.
 * May 05, 2016  5487     tjensen   Added special case for Pressure Level
 *                                  sorting
 * Feb 28, 2017  6121     randerso  Update DualListConfig settings
 * Sep 12, 2017  6413     tjensen   Remove levelType
 * Dec 19, 2017  6523     tjensen   Added ToggleSelectList to show valid
 *                                  Parameter/Level combinations
 *
 * </pre>
 *
 * @author mpduff
 */

public class LevelParameterSelection extends Composite implements IUpdate {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(LevelParameterSelection.class);

    private final List<String> levelList;

    private final List<String> paramList;

    /** Dual list for levels */
    private DualList dualLevelList;

    /** Dual list for parameters */
    private DualList dualParamList;

    private final ISubset callback;

    private final String id;

    /** Flag to determine if tab has changed */
    private boolean isDirty = false;

    private ToggleSelectList toggleList;

    private final Map<String, ParameterGroup> params;

    /**
     * Maps a friendly named description of a parameter at a given level. Pair
     * order is <parameter, level display string>
     */
    private Map<String, Pair<String, String>> toggleMap;

    /**
     * Constructor
     *
     * @param parent
     * @param style
     * @param params
     * @param callback
     * @param id
     */
    public LevelParameterSelection(Composite parent, int style,
            Map<String, ParameterGroup> params, ISubset callback, String id) {
        super(parent, style);
        this.params = params;

        this.levelList = ParameterUtils.getLevelNamesForLevel(id, params);
        this.paramList = new ArrayList<>(params.keySet());
        this.callback = callback;
        this.id = id;
        init();
    }

    private void init() {

        GridData gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false);
        GridLayout gl = new GridLayout(1, false);
        gl.verticalSpacing = 2;
        gl.marginHeight = 2;
        gl.marginWidth = 2;
        this.setLayout(gl);
        this.setLayoutData(gd);

        if (levelList != null && !levelList.isEmpty()) {
            DualListConfig levelConfig = new DualListConfig();
            levelConfig.setAvailableListLabel("Available Levels:");
            levelConfig.setSelectedListLabel("Selected Levels:");
            levelConfig.setVisibleItems(6);
            levelConfig.setListWidthInChars(15);
            levelConfig.setShowUpDownBtns(false);
            levelConfig.setFullList(levelList);
            levelConfig.setPreSorted(true);
            levelConfig.setSortList(true);

            dualLevelList = new DualList(this, SWT.NONE, levelConfig, this);
        }

        DualListConfig paramConfig = new DualListConfig();
        paramConfig.setAvailableListLabel("Available Parameters:");
        paramConfig.setSelectedListLabel("Selected Parameters:");
        paramConfig.setVisibleItems(6);
        paramConfig.setListWidthInChars(15);
        paramConfig.setShowUpDownBtns(false);
        paramConfig.setFullList(paramList);
        dualParamList = new DualList(this, SWT.NONE, paramConfig, this);

        if (levelList != null && !levelList.isEmpty()) {
            Label topLabel = new Label(this, SWT.NONE);
            topLabel.setText("Valid Parameters/Levels:");
            toggleList = new ToggleSelectList(this, SWT.V_SCROLL);
            toggleList.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    hasEntries(false);
                }
            });

            gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
            gd.heightHint = toggleList.getItemHeight() * 5;
            toggleList.setLayoutData(gd);
        }
        initToggleMap();

    }

    private void initToggleMap() {
        toggleMap = new HashMap<>(2);
        for (Entry<String, ParameterGroup> pge : params.entrySet()) {
            ParameterGroup pg = pge.getValue();
            String paramName = pge.getKey();
            LevelGroup lg = pg.getLevelGroup(id);
            for (ParameterLevelEntry level : lg.getLevels()) {
                String displayString = level.getDisplayString();
                String mapKey = ParameterUtils
                        .generateParameterLevelDescription(paramName,
                                lg.getKey(), displayString);
                toggleMap.put(mapKey, new Pair<>(paramName, displayString));
            }
        }
    }

    @Override
    public void hasEntries(boolean entries) {
        /*
         * passed in value not used. Calculate if we have entries based on the
         * selections in our lists.
         */
        callback.updateSelectionState(isSelection(), id);
    }

    /**
     * Get the number of items in the level selected list.
     *
     * @return The number of items in the selected list
     */
    public int getLevelSelectListCount() {
        return dualLevelList.getItemCount();
    }

    /**
     * Get the number of items in the parameter selected list.
     *
     * @return The number of items in the selected list
     */
    public int getParamSelectListCount() {
        return dualParamList.getItemCount();
    }

    /**
     * Get the items in the selected parameter list.
     *
     * @return String[] of items
     */
    public String[] getSelectedParameters() {
        return dualParamList.getSelectedListItems();
    }

    /**
     * Get the items in the selected level list.
     *
     * @return String[] of items
     */
    public String[] getSelectedLevels() {
        if (dualLevelList != null) {
            return dualLevelList.getSelectedListItems();
        }

        return new String[0];
    }

    public String[] getSelectedParameterLevels() {
        if (toggleList != null) {
            return toggleList.getSelection();
        }
        return new String[0];
    }

    /**
     * If items are selected in the list
     *
     * @return true if item are selected
     */
    public boolean isSelection() {
        boolean isSelection = false;
        if (toggleList != null) {
            if (toggleList.getSelectionCount() > 0) {
                isSelection = true;
            }
        } else if (dualParamList != null && dualParamList.getItemCount() > 0) {
            isSelection = true;
        }

        return isSelection;
    }

    /**
     * Action when selecting Parameters on the vertical tab.
     *
     * @param levels
     */
    public void selectLevels(List<String> levels) {
        if (dualLevelList != null) {
            dualLevelList
                    .selectItems(levels.toArray(new String[levels.size()]));
        } else {
            statusHandler.warn("Attempted to select levels for group '" + id
                    + "' which has no valid levels. Skipping selection.");
        }
    }

    /**
     * Action when selecting Parameters on the vertical tab.
     *
     * @param parameters
     */
    public void selectParameters(List<String> parameters) {
        dualParamList
                .selectItems(parameters.toArray(new String[parameters.size()]));
    }

    public void selectParameterLevels(List<String> selectedStrings) {
        if (toggleList != null) {
            List<Integer> selectionList = new ArrayList<>(2);
            for (int i = 0; i < toggleList.getItemCount(); i++) {
                String item = toggleList.getItem(i);
                if (selectedStrings.contains(item)) {
                    selectionList.add(i);
                }
            }
            int[] selection = new int[selectionList.size()];
            for (int i = 0; i < selection.length; i++) {
                selection[i] = selectionList.get(i);
            }
            toggleList.setSelection(selection);
        } else {
            statusHandler
                    .warn("Attempted to select valid parameter/levels for group '"
                            + id
                            + "' which has no valid levels. Skipping selection.");
        }
    }

    /**
     * Set isDirty flag.
     *
     * @return isDirty
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Set parameters to clean.
     */
    public void setClean() {
        isDirty = false;
    }

    @Override
    public void selectionChanged() {
        if (levelList != null && !levelList.isEmpty()) {
            updateToggleList();
        }
        isDirty = true;
    }

    private void updateToggleList() {
        List<String> levels = Arrays.asList(getSelectedLevels());
        String[] parameters = getSelectedParameters();
        // Get current list items and selection
        List<String> currentList = Arrays.asList(toggleList.getItems());
        List<String> selectedStrings = Arrays.asList(toggleList.getSelection());

        // Determine new list items
        Set<String> newOptionList = new TreeSet<>();
        for (String paramName : parameters) {
            ParameterGroup myParam = params.get(paramName);
            LevelGroup lg = myParam.getLevelGroup(id);
            if (lg != null && lg.getLevels() != null) {
                for (ParameterLevelEntry level : lg.getLevels()) {
                    if (levels.contains(level.getDisplayString())) {
                        newOptionList.add(ParameterUtils
                                .generateParameterLevelDescription(paramName,
                                        lg.getKey(), level.getDisplayString()));
                    }
                }
            }
        }
        toggleList.setItems(
                newOptionList.toArray(new String[newOptionList.size()]));

        // Determine which items are new, so they can be selected by default.
        List<String> addedList = new ArrayList<>(newOptionList);
        addedList.removeAll(currentList);
        addedList.addAll(selectedStrings);

        // Select all items that were previously selected or are new.
        selectParameterLevels(addedList);
        // Update if we have any entries
        hasEntries(false);
    }

    public String getId() {
        return id;
    }

    public Map<String, Pair<String, String>> getToggleMap() {
        return toggleMap;
    }

    public Map<String, List<String>> getParameterKeys() {
        Map<String, List<String>> pgKeys = new HashMap<>(2);

        if (toggleList != null) {
            for (String selectedName : getSelectedParameterLevels()) {
                Pair<String, String> keyPair = toggleMap.get(selectedName);
                if (keyPair != null) {
                    List<String> levels = pgKeys.get(keyPair.getFirst());
                    if (levels == null) {
                        levels = new ArrayList<>(2);
                        pgKeys.put(keyPair.getFirst(), levels);
                    }
                    levels.add(keyPair.getSecond());
                }
            }
        } else {
            // This is case where we only have parameters, with no levels.
            String[] selectedParameters = getSelectedParameters();

            for (String parameter : selectedParameters) {
                List<String> levels = pgKeys.get(parameter);
                if (levels == null) {
                    levels = Collections.emptyList();
                    pgKeys.put(parameter, levels);
                }
            }
        }
        return pgKeys;
    }
}
