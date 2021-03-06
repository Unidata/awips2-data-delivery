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
package com.raytheon.uf.viz.datadelivery.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.geotools.geometry.jts.ReferencedEnvelope;

import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.datadelivery.browser.FilterComp;
import com.raytheon.uf.viz.datadelivery.browser.FilterConfig;
import com.raytheon.uf.viz.datadelivery.common.ui.ExpandBarControls;
import com.raytheon.uf.viz.datadelivery.common.ui.ExpandBarControlsConfig;
import com.raytheon.uf.viz.datadelivery.common.ui.IExpandControlAction;
import com.raytheon.uf.viz.datadelivery.filter.FilterImages.ExpandItemState;
import com.raytheon.uf.viz.datadelivery.filter.config.xml.FilterSettingsXML;
import com.raytheon.uf.viz.datadelivery.filter.config.xml.FilterTypeXML;
import com.raytheon.uf.viz.datadelivery.filter.definition.FilterDefinitionManager;
import com.raytheon.uf.viz.datadelivery.filter.definition.xml.DataTypeFilterElementXML;
import com.raytheon.uf.viz.datadelivery.filter.definition.xml.DataTypeFilterXML;
import com.raytheon.uf.viz.datadelivery.filter.definition.xml.FilterElementsXML;
import com.raytheon.uf.viz.datadelivery.filter.definition.xml.FilterXML;
import com.raytheon.uf.viz.datadelivery.filter.definition.xml.SettingsXML;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryGUIUtils;
import com.raytheon.viz.ui.widgets.duallist.DualListConfig;

/**
 *
 * The main class that contains the filter expand bar and the associated
 * controls.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 20, 2012           lvenable  Initial creation
 * Jun 21, 2012  736      djohnson  Add setter for coordinates.
 * Dec 12, 2012  1391     bgonzale  Added a job for the dataset query.
 * Dec 10, 2012  1259     bsteffen  Switch Data Delivery from LatLon to
 *                                  referenced envelopes.
 * Dec 18, 2012  1436     bgonzale  When creating the filter dialogs, use the
 *                                  loaded configuration when populating the
 *                                  filters. Fixed selection icon update when
 *                                  loading from a file.
 * Feb 24, 2013  1620     mpduff    Fixed set clean issue when loading
 *                                  configurations.  Set clean needs to be
 *                                  called after the data load job is complete.
 * May 15, 2013  1040     mpduff    Called markNotBusyInUIThread.
 * Jul 05, 2013  2137     mpduff    Only a single data type can be selected.
 * Jul 05, 2013  2138     mpduff    Fixed to not use filter if filter is
 *                                  disabled.
 * Sep 26, 2013  2412     mpduff    Don't create expand items if no data type is
 *                                  selected.
 * Apr 10, 2014  2892     mpduff    Clear selected items when changing data
 *                                  types.
 * Mar 28, 2016  5482     randerso  Fixed GUI sizing issues
 * Feb 28, 2017  6121     randerso  Update DualListConfig settings
 *
 * </pre>
 *
 * @author lvenable
 */
public class FilterExpandBar extends Composite
        implements IFilterUpdate, IExpandControlAction {
    private static final String DATA_PROVIDER = "Data Provider";

    private static final String DATA_SET = "Data Set";

    private static final String PARAMETER = "Parameter";

    private static final String LEVEL = "Level";

    /**
     * Filter expand bar.
     */
    private ExpandBar expandBar;

    /**
     * Filter Images for the filter items and the expand bar controls.
     */
    private FilterImages filterImgs;

    /**
     * Dialog that will enable or disable a filter.
     */
    private EnableFilterDlg enableFilterDlg = null;

    private DataTypeFilterXML dataTypeFilterXml;

    private FilterXML filterXml;

    private FilterSettingsXML filterSettingsXml;

    /** The selected data type */
    private String dataType;

    /**
     * envelope for filtering.
     */
    private ReferencedEnvelope envelope;

    /**
     * Constructor.
     *
     * @param parent
     *            The parent composite
     */
    public FilterExpandBar(Composite parent) {
        super(parent, SWT.NONE);

        init();
    }

    /**
     * Initialize the filter group.
     */
    private void init() {
        GridLayout gl = new GridLayout(1, false);
        gl.marginWidth = 2;
        gl.marginHeight = 0;
        gl.verticalSpacing = 2;
        this.setLayout(gl);
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        filterImgs = new FilterImages(this);

        createFilterControls();

        GC gc = new GC(this);
        int charWidth = gc.getFontMetrics().getAverageCharWidth();
        int charHeight = gc.getFontMetrics().getHeight();
        gc.dispose();

        Rectangle monitorBounds = this.getMonitor().getBounds();

        expandBar = new ExpandBar(this, SWT.V_SCROLL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = charHeight * 18;
        // limit width to half of the width of an equivalent 5:4 aspect ratio
        // monitor
        gd.widthHint = Math.min(charWidth * 100,
                monitorBounds.height * 5 / 4 / 2);
        expandBar.setLayoutData(gd);

        FilterDefinitionManager filterMan = FilterDefinitionManager
                .getInstance();
        dataTypeFilterXml = filterMan.getDataTypeXml();
        filterXml = filterMan.getFilterXml();

        createExpandItems();
    }

    /**
     * Create the filter controls.
     */
    private void createFilterControls() {
        Composite composite = new Composite(this, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);
        composite.setLayoutData(
                new GridData(SWT.FILL, SWT.DEFAULT, true, false));

        ExpandBarControlsConfig expBarConfig = new ExpandBarControlsConfig();
        expBarConfig.setCollapseAll(true);
        expBarConfig.setExpandAll(true);
        expBarConfig.setDisable(true);
        expBarConfig.setClearAll(true);

        new ExpandBarControls(composite, expBarConfig, this, filterImgs);
        addSeparator(composite);
    }

    /**
     * Create the filter expand items.
     */
    private void createExpandItems() {
        if (dataType != null) {
            final Shell parentShell = this.getShell();
            final Job job = new Job("Dataset Discovery...") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    MetaDataManager dataManager = MetaDataManager.getInstance();

                    DataDeliveryGUIUtils.markBusyInUIThread(parentShell);
                    dataManager.rereadMetaData();
                    dataManager.readMetaData(dataType);
                    return Status.OK_STATUS;
                }
            };

            job.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    try {
                        DataTypeFilterElementXML dtfe;

                        // Get filters for the data type
                        dtfe = dataTypeFilterXml.getFilterData(dataType);
                        final List<String> filterIDList = dtfe
                                .getFilterIDList();

                        VizApp.runAsync(new Runnable() {
                            @Override
                            public void run() {
                                // Now we have a list of common filters, lets
                                // build them
                                for (String filter : filterIDList) {
                                    final FilterElementsXML fex = filterXml
                                            .getFilter(filter);
                                    String clazz = fex.getClazz();

                                    // TODO use reflection here to instantiate
                                    // the class
                                    if ("FilterComp".equals(clazz)) {
                                        createFilter(fex);
                                    }
                                }
                                notifyListeners(SWT.SetData, new Event());
                                setClean();
                            }
                        });
                    } finally {
                        DataDeliveryGUIUtils.markNotBusyInUIThread(parentShell);
                    }
                }
            });

            job.schedule();
        }
    }

    private void createFilter(FilterElementsXML data) {
        // Create a provider filter
        ExpandItem expItem = new ExpandItem(expandBar, SWT.NONE);
        int idx = expandBar.indexOf(expItem);

        String displayName = data.getDisplayName();
        String filterID = data.getId();
        // String clazz = data.getClazz();
        ArrayList<SettingsXML> settingsList = data.getSettingsList();

        DualListConfig dualConfig = new DualListConfig();
        FilterConfig filterConfig = new FilterConfig();

        // Process the settings
        for (SettingsXML setting : settingsList) {
            String settingName = setting.getName();
            String settingValue = setting.getValue();
            if ("availableText".equalsIgnoreCase(settingName)) {
                dualConfig.setAvailableListLabel(settingValue);
            } else if ("selectedText".equalsIgnoreCase(settingName)) {
                dualConfig.setSelectedListLabel(settingValue);
            } else if ("showUpDownBtns".equalsIgnoreCase(settingName)) {
                dualConfig.setShowUpDownBtns(getBoolean(settingValue));
            } else if ("showRegex".equalsIgnoreCase(settingName)) {
                filterConfig.setRegExVisible(getBoolean(settingValue));
            } else if ("showMatch".equalsIgnoreCase(settingName)) {
                filterConfig.setMatchControlVisible(getBoolean(settingValue));
            } else if ("showDualList".equalsIgnoreCase(settingName)) {
                filterConfig.setDualListVisible(getBoolean(settingValue));
            }
        }

        MetaDataManager dataManager = MetaDataManager.getInstance();
        dataManager.setArea(envelope);

        /*
         * TODO : this needs to be reworked as this only has 4 display
         * (filters). This should be configurable.
         */
        if (displayName.equals(DATA_PROVIDER)) {
            Set<String> providerSet = dataManager
                    .getAvailableDataProvidersByType(dataType);
            dualConfig.setFullList(new ArrayList<>(providerSet));
            dualConfig.setSelectedList(getFilterSettingsValues(DATA_PROVIDER));
        } else if (displayName.equals(DATA_SET)) {
            dualConfig.setFullList(
                    new ArrayList<>(dataManager.getAvailableDataSetNames()));
            dualConfig.setSelectedList(getFilterSettingsValues(DATA_SET));
        } else if (displayName.equals(PARAMETER)) {
            dualConfig.setFullList(
                    new ArrayList<>(dataManager.getAvailableParameters()));
            dualConfig.setSelectedList(getFilterSettingsValues(PARAMETER));
        } else if (displayName.equals(LEVEL)) {
            dualConfig.setFullList(
                    new ArrayList<>(dataManager.getAvailableLevels()));
            dualConfig.setSelectedList(getFilterSettingsValues(LEVEL));
        }

        filterConfig.setDualConfig(dualConfig);
        filterConfig.setDualListVisible(true);
        filterConfig.setFilterID(filterID);

        expItem.setText(displayName);
        FilterComp filterComp = new FilterComp(expandBar, SWT.NONE, this,
                filterConfig, idx);

        expItem.setHeight(filterComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        expItem.setControl(filterComp);
    }

    /**
     * Get the list of defined filterSettings for a filter type defined in
     * filterSettingsXml.
     *
     * @param filterType
     * @return List of filter values.
     */
    private List<String> getFilterSettingsValues(String filterType) {
        if (filterSettingsXml != null) {
            for (FilterTypeXML filter : filterSettingsXml.getFilterTypeList()) {
                if (filter.getFilterType().equals(filterType)) {
                    return filter.getValues();
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Add a separator line to the display.
     *
     * @param parentComp
     *            Parent component.
     */
    private void addSeparator(Composite parentComp) {
        GridLayout gl = (GridLayout) parentComp.getLayout();

        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gd.horizontalSpan = gl.numColumns;
        Label sepLbl = new Label(parentComp, SWT.SEPARATOR | SWT.HORIZONTAL);
        sepLbl.setLayoutData(gd);
    }

    /**
     * Dispose the expand items and the controls in the expand items.
     */
    private void disposeExpandItemsAndControls() {
        for (ExpandItem ei : expandBar.getItems()) {
            if (ei != null) {
                ei.getControl().dispose();
                ei.dispose();
            }
        }
    }

    /**
     * Display the enable filter dialog.
     */
    private void displayEnableFilterDialog() {

        if (expandBar.getItemCount() == 0) {
            MessageBox mb = new MessageBox(this.getShell(),
                    SWT.ICON_ERROR | SWT.OK);
            mb.setText("Warning");
            mb.setMessage("No filters are available to enable/disable.");
            mb.open();
            return;
        }

        if (enableFilterDlg == null || enableFilterDlg.isDisposed()) {
            enableFilterDlg = new EnableFilterDlg(this.getShell(),
                    getFilterNames(), getEnabledFilters());
            enableFilterDlg.open();

            if (enableFilterDlg.getReturnValue() != null
                    && (Boolean) enableFilterDlg.getReturnValue()) {
                this.enableFilters(enableFilterDlg.getSelectedIndexes());
            }
        } else {
            enableFilterDlg.bringToTop();
        }
    }

    /**
     * Called when a filter has been updated.
     */
    @Override
    public void filterUpdate(int index, ExpandItemState state) {
        // Get the correct item
        ExpandItem expItem = expandBar.getItem(index);

        expItem.setImage(filterImgs.getExpandItemImage(state));
    }

    /**
     * Get a list of filter names.
     *
     * @return A list of filter names.
     */
    public List<String> getFilterNames() {
        List<String> names = new ArrayList<>();

        for (ExpandItem ei : expandBar.getItems()) {
            names.add(ei.getText());
        }

        return names;
    }

    /**
     * Any filters in the expandBar?
     *
     * @return true if one or more filters exist in the expand bar
     */
    public boolean hasFilters() {
        if (expandBar.getItemCount() > 0) {
            return true;
        }

        return false;
    }

    /**
     * Get a list of enabled filters.
     *
     * @return A list of enabled filters.
     */
    public List<Integer> getEnabledFilters() {
        List<Integer> enabledIndexes = new ArrayList<>();

        for (int i = 0; i < expandBar.getItems().length; i++) {
            AbstractFilterComp afc = (AbstractFilterComp) expandBar.getItem(i)
                    .getControl();
            if (afc.isEnabled()) {
                enabledIndexes.add(i);
            }
        }

        return enabledIndexes;
    }

    /**
     * Enable the list of specified filters.
     *
     * @param indexes
     *            Array of indexes specifying the filters to be enabled.
     */
    public void enableFilters(List<Integer> indexes) {
        for (ExpandItem ei : expandBar.getItems()) {
            ((AbstractFilterComp) ei.getControl()).setEnabled(false);
        }

        for (int idx : indexes) {
            ((AbstractFilterComp) expandBar.getItem(idx).getControl())
                    .setEnabled(true);
        }
    }

    @Override
    public void collapseAction() {
        ExpandItem[] items = expandBar.getItems();

        for (ExpandItem ei : items) {
            ei.setExpanded(false);
        }
    }

    @Override
    public void expandAction() {
        ExpandItem[] items = expandBar.getItems();

        for (ExpandItem ei : items) {
            ei.setExpanded(true);
        }
    }

    @Override
    public void expandSelectedAction() {
        // NOT USED...
    }

    @Override
    public void disableAction() {
        displayEnableFilterDialog();
    }

    @Override
    public void clearAllAction() {

        MessageBox mb = new MessageBox(this.getShell(),
                SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        mb.setText("Clear All Filters");
        mb.setMessage(
                "You are about to clear all of your filter settings.  This\n"
                        + "cannot be undone.\n\nDo you wish to continue?");
        int result = mb.open();

        if (result == SWT.NO) {
            return;
        }

        for (ExpandItem ei : expandBar.getItems()) {
            if (ei.getControl() instanceof FilterComp) {
                ((FilterComp) ei.getControl()).resetControls();
            }
        }
    }

    @Override
    public void previewAction() {
        // NOT USED...
    }

    private boolean getBoolean(String bool) {
        return Boolean.parseBoolean(bool);
    }

    /**
     * Update the filters.
     *
     * @param dataType
     *            the data type
     * @param envelope
     *            envelope
     */
    public void updateFilters(String dataType, ReferencedEnvelope envelope) {
        this.dataType = dataType;
        setEnvelope(envelope);
        disposeExpandItemsAndControls();

        // Clear previously selected items
        if (filterSettingsXml != null) {
            for (FilterTypeXML xml : this.filterSettingsXml
                    .getFilterTypeList()) {
                xml.clearValues();
            }
        }
        if (!dataType.isEmpty()) {
            createExpandItems();
        }
    }

    /**
     * Update the filter settings.
     */
    private void updateFilterSettings() {
        ArrayList<FilterTypeXML> filterTypeList = filterSettingsXml
                .getFilterTypeList();
        if (filterTypeList != null && !filterTypeList.isEmpty()) {
            for (FilterTypeXML ftx : filterTypeList) {
                if ("Data Type".equals(ftx.getFilterType())) {
                    // only one data type
                    List<String> values = ftx.getValues();
                    if (values != null && !values.isEmpty()) {
                        dataType = values.get(0);
                    }
                }
            }
        }
    }

    /**
     * Populate the filters.
     *
     * @param filterSettingsXml
     *            Settings to populate
     */
    public void populateFilterSettingsXml(FilterSettingsXML filterSettingsXml) {
        ExpandItem[] items = expandBar.getItems();
        for (ExpandItem item : items) {
            Control control = item.getControl();
            if (control instanceof FilterComp) {
                FilterComp fc = (FilterComp) control;
                if (fc.isEnabled()) {
                    String[] selectedItems = fc.getSelectedListItems();
                    ArrayList<String> values = new ArrayList<>();
                    for (String selectedItem : selectedItems) {
                        values.add(selectedItem);
                    }
                    String type = item.getText();
                    FilterTypeXML ftx = new FilterTypeXML();
                    ftx.setFilterType(type);
                    ftx.setValues(values);

                    filterSettingsXml.addFilterType(ftx);
                }
            }
        }
    }

    /**
     * @param filterSettingsXml
     *            the filterSettingsXml to set
     */
    public void setFilterSettingsXml(FilterSettingsXML filterSettingsXml) {
        this.filterSettingsXml = filterSettingsXml;
        updateFilterSettings();
    }

    /**
     * Check for changes in the filter.
     *
     * @return true if changes have been made;
     */
    public boolean isDirty() {
        ExpandItem[] items = expandBar.getItems();
        for (ExpandItem item : items) {
            FilterComp comp = (FilterComp) item.getControl();
            if (comp != null) {
                if (comp.isDirty()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Set the filters clean.
     */
    public void setClean() {
        ExpandItem[] items = expandBar.getItems();
        for (ExpandItem item : items) {
            FilterComp comp = (FilterComp) item.getControl();
            if (comp != null) {
                comp.setDirty(false);
            }
        }
    }

    /**
     * Set the referenced envelope.
     *
     * @param envelope
     *            The ReferencedEnvelope
     */
    public void setEnvelope(ReferencedEnvelope envelope) {
        this.envelope = envelope;
    }
}
