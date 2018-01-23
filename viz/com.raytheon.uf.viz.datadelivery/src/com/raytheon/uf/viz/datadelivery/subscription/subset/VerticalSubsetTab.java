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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.geotools.geometry.jts.ReferencedEnvelope;

import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterUtils;
import com.raytheon.uf.viz.datadelivery.common.ui.ExpandBarControls;
import com.raytheon.uf.viz.datadelivery.common.ui.ExpandBarControlsConfig;
import com.raytheon.uf.viz.datadelivery.common.ui.IExpandControlAction;
import com.raytheon.uf.viz.datadelivery.common.ui.ViewDetailsDlg;
import com.raytheon.uf.viz.datadelivery.filter.FilterImages;
import com.raytheon.uf.viz.datadelivery.filter.FilterImages.ExpandItemState;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.VerticalXML;

/**
 * Controls for the Vertical Tab of the Subset Dialog
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#    Engineer  Description
 * ------------- ---------- --------- ------------------------------------------
 * Mar 30, 2012             mpduff    Initial creation.
 * Jul 30, 2012  702        jpiatt    Code clean up.
 * Jul 25, 2012  955        djohnson  Use List instead of ArrayList.
 * Aug 08, 2012  863        jpiatt    Added clean & dirty checks.
 * Aug 10, 2012  1002       mpduff    Implementing dataset size estimation.
 * Aug 30, 2012  1121/1122  mpduff    Fix level type heading to match starts
 *                                    with  rather than equals
 * Sep 24, 2012  1209       djohnson  isValid() no longer needs @Override.
 * Oct 04, 2012  1245       jpiatt    Correct isValid method & code clean up.
 * Nov 19, 2012  1166       djohnson  Clean up JAXB representation of registry
 *                                    objects.
 * Dec 10, 2012  1259       bsteffen  Switch Data Delivery from LatLon to
 *                                    referenced envelopes.
 * Jan 10, 2013  1444       mpduff    Add updateSettings method.
 * Sep 30, 1797  1797       dhladky   separated time from gridded time
 * Oct 09, 2013  2267       bgonzale  Fix Collection cast to List error.
 * Jul 08, 2015  4566       dhladky   Use AWIPS naming rather than provider
 *                                    naming.
 * Sep 12, 2017  6413       tjensen   Updated to support ParameterGroups
 * Dec 19, 2017  6523       tjensen   Updates for LevelParameterSelection's
 *                                    ToggleSelectList
 *
 * </pre>
 *
 * @author mpduff
 */

public class VerticalSubsetTab extends SubsetTab
        implements IExpandControlAction, ISubset {

    /** Parent Composite */
    private final Composite parentComp;

    /** Expand Bar */
    private ExpandBar expandBar;

    /** FilterImages object */
    private FilterImages filterImgs;

    /** Callback */
    private final IDataSize callback;

    private Map<String, Map<String, ParameterGroup>> paramsByNameByLevel;

    private Map<String, ParameterGroup> parameterGroups;

    /**
     * Constructor.
     *
     * @param parentComp
     *            The Composite holding these controls
     *
     * @param dataSet
     *            The DataSet object
     * @param callback
     *            IDataSize callback
     */
    public VerticalSubsetTab(Composite parentComp, DataSet<?, ?> dataSet,
            IDataSize callback) {
        this.parentComp = parentComp;
        this.callback = callback;
        this.parameterGroups = dataSet.getParameterGroups();

        populateLevelMap();

        init();
    }

    /**
     * Initialize the tab.
     */
    private void init() {
        filterImgs = new FilterImages(parentComp.getShell());

        ExpandBarControlsConfig expBarConfig = new ExpandBarControlsConfig();
        expBarConfig.setCollapseAll(true);
        expBarConfig.setExpandAll(true);
        expBarConfig.setExpandSelected(true);
        expBarConfig.setPreviewSelected(true);

        new ExpandBarControls(parentComp, expBarConfig, this, filterImgs);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);

        expandBar = new ExpandBar(parentComp, SWT.V_SCROLL);
        expandBar.setLayoutData(gd);

        createExpandBarItems();

    }

    /**
     * Create the bar that may be expanded depending on item count.
     */
    private void createExpandBarItems() {
        disposeExpandItemsAndControls();

        for (Entry<String, Map<String, ParameterGroup>> levelEntry : paramsByNameByLevel
                .entrySet()) {

            String levelLabel = levelEntry.getKey();
            Map<String, ParameterGroup> params = levelEntry.getValue();

            ExpandItem item = new ExpandItem(expandBar, SWT.NONE);
            item.setText(levelLabel);
            item.setImage(
                    filterImgs.getExpandItemImage(ExpandItemState.NoEntries));
            LevelParameterSelection lps = new LevelParameterSelection(expandBar,
                    SWT.BORDER, params, this, levelLabel);
            item.setHeight(lps.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
            item.setControl(lps);
        }
    }

    private void populateLevelMap() {
        paramsByNameByLevel = ParameterUtils
                .getParameterLevelMap(parameterGroups);
    }

    /**
     * Dispose items & controls.
     */
    private void disposeExpandItemsAndControls() {
        for (ExpandItem ei : expandBar.getItems()) {
            if (ei != null && ei.getControl() != null) {
                ei.getControl().dispose();
                ei.dispose();
            }
        }
    }

    @Override
    public void collapseAction() {
        for (ExpandItem item : expandBar.getItems()) {
            item.setExpanded(false);
        }
    }

    @Override
    public void expandAction() {
        for (ExpandItem item : expandBar.getItems()) {
            item.setExpanded(true);
        }
    }

    @Override
    public void expandSelectedAction() {
        for (ExpandItem item : expandBar.getItems()) {
            item.setExpanded(false);
            Control control = item.getControl();
            if (control instanceof LevelParameterSelection) {
                LevelParameterSelection lps = (LevelParameterSelection) control;
                if (lps.isSelection()) {
                    item.setExpanded(true);
                }
            }
        }
    }

    @Override
    public void disableAction() {
        // Nothing here
    }

    @Override
    public void clearAllAction() {
        // Nothing here
    }

    /**
     * Get the selected parameters
     *
     * @return List of Parameter objects
     */
    public Map<String, ParameterGroup> getParameters() {
        // Get the parameters first
        Map<String, ParameterGroup> selectedParameterObjs = new HashMap<>();

        for (ExpandItem item : expandBar.getItems()) {
            LevelParameterSelection lps = (LevelParameterSelection) item
                    .getControl();
            if (lps == null) {
                continue;
            }

            Map<String, List<String>> pgKeys = lps.getParameterKeys();
            String levelGroup = lps.getId();
            for (Entry<String, List<String>> keyEntry : pgKeys.entrySet()) {
                String parameter = keyEntry.getKey();
                List<String> levelList = keyEntry.getValue();
                ParameterGroup param = paramsByNameByLevel.get(levelGroup)
                        .get(parameter);
                if (param != null) {
                    ParameterGroup p = ParameterUtils.copyParameter(param,
                            levelList.toArray(new String[levelList.size()]),
                            levelGroup);
                    selectedParameterObjs.put(p.getKey(), p);
                }
            }
        }

        return selectedParameterObjs;
    }

    @Override
    public void previewAction() {
        final String nl = "\n";
        StringBuilder sb = new StringBuilder();

        boolean showBreak = false;
        for (ExpandItem item : expandBar.getItems()) {
            LevelParameterSelection selection = (LevelParameterSelection) item
                    .getControl();
            String[] parameters = selection.getSelectedParameters();
            String[] levels = selection.getSelectedLevels();
            String[] selected = selection.getSelectedParameterLevels();
            if (levels.length > 0 || parameters.length > 0) {
                if (showBreak) {
                    sb.append(
                            "--------------------------------------------------------"
                                    + nl);
                } else {
                    showBreak = true;
                }
                sb.append(item.getText() + nl);
                sb.append("  Selected Levels/Parameters:" + nl);
                for (String s : selected) {
                    sb.append("      " + s + nl);
                }
            }
        }

        if (sb.toString().length() == 0) {
            sb.append("No Selections");
        }
        ViewDetailsDlg details = new ViewDetailsDlg(parentComp.getShell(),
                sb.toString(), "Level/Parameter Details", null, null);
        details.open();
    }

    @Override
    public void updateBounds(ReferencedEnvelope envelope) {
        // Not used
    }

    @Override
    public void updateSelectionState(boolean selected, String id) {
        for (ExpandItem item : expandBar.getItems()) {
            if (item.getText().equals(id)) {
                if (selected) {
                    item.setImage(filterImgs
                            .getExpandItemImage(ExpandItemState.Entries));
                } else {
                    item.setImage(filterImgs
                            .getExpandItemImage(ExpandItemState.NoEntries));
                }
            }
        }

        callback.updateDataSize();
    }

    /**
     * Populate the tab.
     *
     * @param vertList
     *            List of VerticalXML objects
     * @param dataSet
     *            The DataSetMetaData object
     */
    public void populate(List<VerticalXML> vertList, DataSet<?, ?> dataSet) {
        this.parameterGroups = dataSet.getParameterGroups();
        populateLevelMap();
        createExpandBarItems();

        for (VerticalXML vert : vertList) {
            for (ExpandItem item : expandBar.getItems()) {
                LevelParameterSelection lps = (LevelParameterSelection) item
                        .getControl();
                if (vert.getLayerType().equalsIgnoreCase(item.getText())) {
                    List<String> levels = vert.getLevels();
                    if (levels != null && !levels.isEmpty()) {
                        lps.selectLevels(levels);
                    }
                    List<String> parameterList = vert.getParameterList();
                    if (parameterList != null && !parameterList.isEmpty()) {
                        lps.selectParameters(parameterList);
                    }
                    lps.selectionChanged();
                    List<String> selectedList = vert.getSelectedList();
                    if (selectedList != null && !selectedList.isEmpty()) {
                        lps.selectParameterLevels(selectedList);
                    }
                }
            }
        }

        parentComp.layout();
    }

    /**
     * Get the save details from this tab
     *
     * @return List of VertcialXML objects
     */
    public List<VerticalXML> getSaveInfo() {
        ArrayList<VerticalXML> vertList = new ArrayList<>();
        ExpandItem[] items = this.expandBar.getItems();
        for (ExpandItem item : items) {
            LevelParameterSelection lps = (LevelParameterSelection) item
                    .getControl();
            if (lps.isSelection()) {
                VerticalXML vertical = new VerticalXML();
                vertical.setLayerType(item.getText());

                for (String level : lps.getSelectedLevels()) {
                    vertical.addLevel(level);
                }

                for (String parameter : lps.getSelectedParameters()) {
                    vertical.addParameter(parameter);
                }

                for (String select : lps.getSelectedParameterLevels()) {
                    vertical.addSelected(select);
                }
                vertList.add(vertical);
            }
        }

        return vertList;
    }

    /**
     * Determine if the tab selections are valid.
     *
     * @return true if tab is valid
     */
    public boolean isValid() {

        for (ExpandItem eItem : expandBar.getItems()) {
            Control control = eItem.getControl();
            if (control instanceof LevelParameterSelection) {
                LevelParameterSelection lps = (LevelParameterSelection) control;
                if (lps.isSelection()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check for changes in selection to the tab.
     *
     * @return true if changes in selection occurred.
     */
    public boolean isDirty() {
        for (ExpandItem eItem : expandBar.getItems()) {
            Control control = eItem.getControl();
            if (control instanceof LevelParameterSelection) {
                LevelParameterSelection lps = (LevelParameterSelection) control;
                if (lps.isDirty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Set the selection changes to clean.
     */
    public void setClean() {
        for (ExpandItem eItem : expandBar.getItems()) {
            Control control = eItem.getControl();
            if (control instanceof LevelParameterSelection) {
                LevelParameterSelection lps = (LevelParameterSelection) control;
                lps.setClean();
            }
        }
    }
}
