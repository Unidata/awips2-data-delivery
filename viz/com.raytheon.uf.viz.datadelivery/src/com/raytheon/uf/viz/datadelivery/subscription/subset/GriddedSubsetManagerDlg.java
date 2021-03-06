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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.geotools.geometry.jts.ReferencedEnvelope;

import com.google.common.collect.Ordering;
import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.Ensemble;
import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedDataSet;
import com.raytheon.uf.common.datadelivery.registry.GriddedDataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterUtils;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.retrieval.util.GriddedDataSizeUtils;
import com.raytheon.uf.common.gridcoverage.GridCoverage;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.ImmutableDate;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.common.util.SizeUtil;
import com.raytheon.uf.viz.datadelivery.common.xml.AreaXML;
import com.raytheon.uf.viz.datadelivery.filter.MetaDataManager;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.GriddedTimeXML;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.SpecificDateTimeXML;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.SubsetXML;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.TimeXML;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.VerticalXML;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;

/**
 * {@link SubsetManagerDlg} for gridded data sets.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 22, 2012  743      djohnson  Initial creation
 * Aug 29, 2012  223      mpduff    Removed call to add cycle times to
 *                                  subscription.
 * Sep 27, 2012  1202     bgonzale  Fixed dateStringtoDateMap key creation.
 * Oct 05, 2012  1241     djohnson  Replace RegistryManager calls with registry
 *                                  handler calls.
 * Oct 11, 2012  1263     jpiatt    Modified for cancel flag.
 * Dec 10, 2012  1259     bsteffen  Switch Data Delivery from LatLon to
 *                                  referenced envelopes.
 * Jan 04, 2013  1299     djohnson  Add logging of invalid forecast hour
 *                                  information if it occurs again.
 * Jan 04, 2013  1420     mpduff    Pass cycles in for rules.
 * Jan 18, 2013  1414     bsteffen  Add ensemble tab.
 * Jan 28, 2013  1533     djohnson  Update the calculated dataset size after
 *                                  loading subset xml.
 * Mar 21, 2013  1794     djohnson  Add option to create a shared subscription,
 *                                  if phase3 code is available.
 * Mar 29, 2013  1841     djohnson  Subscription is now UserSubscription.
 * May 21, 2013  2020     mpduff    Rename UserSubscription to SiteSubscription.
 * Jun 04, 2013  223      mpduff    Added grid specific items to this class.
 * Jun 11, 2013  2064     mpduff    Fix editing of subscriptions.
 * Jun 14, 2013  2108     mpduff    Refactored DataSizeUtils.
 * Jul 18, 2013  2205     djohnson  If null time is selected from the dialog,
 *                                  return null for the adhoc.
 * Sep 25, 2013  1797     dhladky   Separated Time from GriddedTime
 * Oct 11, 2013  2386     mpduff    Refactor DD Front end.
 * Sep 04, 2014  2131     dhladky   Changes to allow for PDA data type
 * Jul 08, 2015  4566     dhladky   Use AWIPS naming rather than provider
 *                                  naming.
 * Apr 25, 2017  1045     tjensen   Update for moving datasets
 * Aug 02, 2017  6186     rjpeter   Fix adhoc processing.
 * Aug 29, 2017  6186     rjpeter   Add url for adhoc.
 * Sep 12, 2017  6413     tjensen   Updated to support ParameterGroups
 * Sep 26, 2017  6438     tgurney   Fix handling of Sea Ice selected levels
 * Sep 27, 2017  5948     tjensen   Update populateSubsetXML to get vertList
 * Dec 19, 2017  6523     tjensen   Changes for VerticalXML updates
 * Mar 01, 2018  7204     nabowle   Add subEnvelope to constructor. Give to SpatialTab.
 *
 * </pre>
 *
 * @author djohnson
 */

public class GriddedSubsetManagerDlg extends SubsetManagerDlg {
    private static final String TIMING_TAB_GRID = "Forecast Hours";

    private static final String NO_DATA_FOR_DATE_AND_CYCLE = "No data is available for the specified date and cycle combination.";

    /** Status Handler */
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GriddedSubsetManagerDlg.class);

    private final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy - H 'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            return sdf;
        }
    };

    private final List<String> asString = new ArrayList<>();

    private final Map<String, ImmutableDate> dateStringToDateMap = new HashMap<>();

    private GriddedEnsembleSubsetTab ensembleTab;

    private TabItem timingTab;

    /** Gridded data size utility */
    private GriddedDataSizeUtils dataSize;

    private GriddedTimingSubsetTab timingTabControls;

    /**
     * Constructor.
     *
     * @param shell
     *            parent shell
     * @param loadDataSet
     *            true if loading data set
     * @param subscription
     *            the subscription object
     */
    public GriddedSubsetManagerDlg(Shell shell, boolean loadDataSet,
            Subscription subscription) {
        super(shell, loadDataSet, subscription);
        this.dataSet = MetaDataManager.getInstance().getDataSet(
                subscription.getDataSetName(), subscription.getProvider());
        setTitle();
    }

    /**
     * Constructor.
     *
     * @param shell
     *            parent shell
     * @param dataSet
     *            The dataset object
     * @param loadDataSet
     *            true if loading data set
     * @param subsetXml
     *            The subset object
     */
    public GriddedSubsetManagerDlg(Shell shell, GriddedDataSet dataSet,
            boolean loadDataSet, SubsetXML subsetXml) {
        super(shell, loadDataSet, dataSet, null);
        this.dataSet = dataSet;
        this.subsetXml = subsetXml;
        setTitle();
    }

    /**
     * Constructor.
     *
     * @param shell
     *            parent shell
     * @param dataSet
     *            The dataset object
     */
    public GriddedSubsetManagerDlg(Shell shell, GriddedDataSet dataSet,
            ReferencedEnvelope subEnvelope) {
        super(shell, dataSet, subEnvelope);
        this.dataSet = dataSet;
        setTitle();
    }

    @Override
    protected void createTabs(TabFolder tabFolder) {
        GriddedDataSet griddedDataSet = (GriddedDataSet) dataSet;
        GridData gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(1, false);

        TabItem verticalTab = new TabItem(tabFolder, SWT.NONE);
        verticalTab.setText(VERTICAL_TAB);
        verticalTab.setData("valid", false);
        Composite vertComp = new Composite(tabFolder, SWT.NONE);
        vertComp.setLayout(gl);
        vertComp.setLayoutData(gd);
        verticalTab.setControl(vertComp);
        vTab = new VerticalSubsetTab(vertComp, dataSet, this);

        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gl = new GridLayout(1, false);

        timingTab = new TabItem(tabFolder, SWT.NONE);
        timingTab.setText(TIMING_TAB_GRID);
        timingTab.setData("valid", false);
        Composite timingComp = new Composite(tabFolder, SWT.NONE);
        timingComp.setLayout(gl);
        timingComp.setLayoutData(gd);
        timingTab.setControl(timingComp);
        timingTabControls = new GriddedTimingSubsetTab(timingComp, this, shell);

        Ensemble e = griddedDataSet.getEnsemble();
        if (e != null && e.getMembers() != null) {
            TabItem ensembleTabItem = new TabItem(tabFolder, SWT.NONE, 2);
            Composite ensembleComp = new Composite(tabFolder, SWT.NONE);
            ensembleComp.setLayout(new GridLayout(1, false));
            ensembleComp.setLayoutData(
                    new GridData(SWT.CENTER, SWT.DEFAULT, true, false));
            ensembleTabItem.setControl(ensembleComp);
            ensembleTab = new GriddedEnsembleSubsetTab(ensembleComp,
                    griddedDataSet.getEnsemble());
            ensembleTab.addListener(this);
            ensembleTabItem.setText(ensembleTab.getName());
        }

        TabItem spatialTab = new TabItem(tabFolder, SWT.NONE);
        spatialTab.setText(SPATIAL_TAB);
        Composite spatialComp = new Composite(tabFolder, SWT.NONE);
        spatialComp.setLayout(gl);
        spatialComp.setLayoutData(gd);
        spatialTab.setControl(spatialComp);
        spatialTabControls = new SpatialSubsetTab(spatialComp, dataSet, this,
                subEnvelope);

        SortedSet<Integer> forecastHours = new TreeSet<>(
                griddedDataSet.getForecastHours());

        List<String> forecastHoursAsString = new ArrayList<>();
        for (Integer integer : forecastHours) {
            forecastHoursAsString.add(Integer.toString(integer));
        }

        timingTabControls.setAvailableForecastHours(forecastHoursAsString);
    }

    @Override
    protected Collection<String> getInvalidTabs() {
        Collection<String> invalidTabs = super.getInvalidTabs();
        if (ensembleTab != null && !ensembleTab.isValid()) {
            invalidTabs.add(ensembleTab.getName());
        }

        if (!vTab.isValid()) {
            invalidTabs.add(VERTICAL_TAB);
        }

        if (!timingTabControls.isValid()) {
            invalidTabs.add(timingTab.getText());
        }

        return invalidTabs;
    }

    @Override
    protected void populateSubsetXML(SubsetXML subset) {
        super.populateSubsetXML(subset);
        if (ensembleTab != null) {
            ensembleTab.populateSubsetXML(subset);
        }
        if (vTab != null) {
            // next save vertical layer/parameter info
            List<VerticalXML> vertList = vTab.getSaveInfo();
            subset.setVerticalList(vertList);
        }
    }

    @Override
    protected void loadFromSubsetXML(SubsetXML subsetXml) {
        super.loadFromSubsetXML(subsetXml);
        if (ensembleTab != null) {
            ensembleTab.loadFromSubsetXML(subsetXml);
        }

        List<VerticalXML> vertList = subsetXml.getVerticalList();
        vTab.populate(vertList, dataSet);

        TimeXML time = subsetXml.getTime();
        this.timingTabControls.setSelectedForecastHours(
                ((GriddedTimeXML) time).getFcstHours());

        updateDataSize();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void loadFromSubscription(Subscription subscription) {
        super.loadFromSubscription(subscription);
        if (ensembleTab != null) {
            ensembleTab.loadFromSubscription(subscription);
        }

        // Cycle time
        SpecificDateTimeXML timeXml = getTimeXmlFromSubscription();

        timeXml.setLatestData(true);

        this.timingTabControls.setSelectedForecastHours(timeXml.getFcstHours());

        // Vertical/Parameters
        Map<String, VerticalXML> levelMap = new HashMap<>();
        Map<String, Map<String, ParameterGroup>> paramsByNameByLevel = ParameterUtils
                .getParameterLevelMap(this.subscription.getParameterGroups());

        for (Entry<String, Map<String, ParameterGroup>> levelEntry : paramsByNameByLevel
                .entrySet()) {
            String levelLabel = levelEntry.getKey();
            Map<String, ParameterGroup> params = levelEntry.getValue();
            VerticalXML v = new VerticalXML();
            v.setLayerType(levelLabel);
            v.setLevels(
                    ParameterUtils.getLevelNamesForLevel(levelLabel, params));
            v.setParameterList(new ArrayList<>(params.keySet()));
            v.setSelectedList(ParameterUtils
                    .getDescriptionsForParameters(levelLabel, params));
            levelMap.put(levelLabel, v);
        }
        ArrayList<VerticalXML> vertList = new ArrayList<>(levelMap.values());
        vTab.populate(vertList, dataSet);

        // Area
        AreaXML area = new AreaXML();

        ReferencedEnvelope envelope = this.subscription.getCoverage()
                .getEnvelope();
        ReferencedEnvelope requestEnvelope = this.subscription.getCoverage()
                .getRequestEnvelope();

        if (requestEnvelope != null && !requestEnvelope.isEmpty()) {
            area.setEnvelope(requestEnvelope);
        } else {
            area.setEnvelope(envelope);
        }

        spatialTabControls.setDataSet(this.dataSet);
        spatialTabControls.populate(area);
    }

    @Override
    protected boolean isDirty() {
        boolean modified = super.isDirty();
        if (!modified && ensembleTab != null) {
            modified = ensembleTab.isModified();
        }

        if (!modified && timingTabControls.isDirty()) {
            return true;
        }

        return modified;
    }

    @Override
    protected void setClean() {
        super.setClean();
        if (ensembleTab != null) {
            ensembleTab.setModified(false);
        }
        timingTabControls.setDirty(false);
    }

    @Override
    protected SpecificDateTimeXML getTimeXmlFromSubscription() {
        SpecificDateTimeXML timeXml = new SpecificDateTimeXML();
        GriddedTime time = (GriddedTime) this.subscription.getTime();
        List<Integer> cycleTimes = time.getCycleTimes();
        if (!CollectionUtil.isNullOrEmpty(cycleTimes)) {
            for (int cycle : cycleTimes) {
                timeXml.addCycle(cycle);
            }
        }

        // All Forecast hours
        List<String> fcstHours = time.getFcstHours();
        final int numberOfFcstHours = fcstHours.size();

        // Selected Forecast hour indices
        List<Integer> selectedTimeIndices = time.getSelectedTimeIndices();
        if (!CollectionUtil.isNullOrEmpty(selectedTimeIndices)) {
            for (int idx : selectedTimeIndices) {
                if (idx < 0 || idx >= numberOfFcstHours) {
                    //warnOfInvalidForecastHourIndex(this.subscription,
                    //        numberOfFcstHours, idx);
                } else {
                    timeXml.addHour(fcstHours.get(idx));
                }
            }
        }
        return timeXml;
    }

    /**
     * Warns of an invalid forecast hour index, with debugging information.
     *
     * @param subscription
     *            the time object
     * @param numberOfFcstHours
     *            the number of forecast hours in the time object
     * @param idx
     *            the requested index, which was invalid
     */
    private void warnOfInvalidForecastHourIndex(Subscription subscription,
            final int numberOfFcstHours, int idx) {
        String subscriptionAsXml;
        try {
            subscriptionAsXml = new JAXBManager(Subscription.class)
                    .marshalToXml(subscription);
        } catch (JAXBException e) {
            StringWriter writer = new StringWriter();
            writer.append("Unable to convert the subscription object to xml:");
            e.printStackTrace(new PrintWriter(writer));
            subscriptionAsXml = writer.toString();
            statusHandler.error(
                    "Unable to convert the subscription object to xml", e);
        }

        statusHandler.handle(Priority.WARN,
                String.format(
                        "Invalid value for selected forecast hour.  Expected less than [%s] but was [%s].\nSubscription represented as XML:\n%s",
                        numberOfFcstHours, idx, subscriptionAsXml),
                new IllegalStateException("Debugging stacktrace"));
    }

    @Override
    public void updateDataSize() {
        if (!initialized) {
            return;
        }

        if (dataSize == null) {
            this.dataSize = new GriddedDataSizeUtils((GriddedDataSet) dataSet);
        }

        // Update the data set size label text.
        Map<String, ParameterGroup> params = vTab.getParameters();
        int numFcstHrs = this.timingTabControls.getSelectedFcstHours().length;
        ReferencedEnvelope envelope = this.spatialTabControls.getEnvelope();
        int ensembleCount = 1;
        if (ensembleTab != null) {
            ensembleCount = ensembleTab.getEnsembleWithSelection()
                    .getMemberCount();
        }

        long numBytes = dataSize.getDataSetSizeInBytes(params, numFcstHrs,
                ensembleCount, envelope);

        this.sizeLbl.setText(SizeUtil.prettyByteSize(numBytes) + " of "
                + SizeUtil.prettyByteSize(dataSize.getFullSizeInBytes()));
    }

    private GriddedTime setupDataSpecificTime(Time subTime, Subscription sub) {
        GriddedTime newTime = (GriddedTime) subTime;
        GriddedDataSet griddedDataSet = (GriddedDataSet) dataSet;

        if (asString.isEmpty()) {
            SortedSet<ImmutableDate> newestToOldest = new TreeSet<>(
                    Ordering.natural().reverse());
            try {
                if (dataSet.isMoving()) {
                    newestToOldest.addAll(DataDeliveryHandlers
                            .getDataSetMetaDataHandler()
                            .getDatesForDataSetByIntersection(
                                    dataSet.getDataSetName(),
                                    dataSet.getProviderName(),
                                    sub.getCoverage().getRequestEnvelope()));
                } else {
                    newestToOldest.addAll(DataDeliveryHandlers
                            .getDataSetMetaDataHandler()
                            .getDatesForDataSet(dataSet.getDataSetName(),
                                    dataSet.getProviderName()));
                }
            } catch (RegistryHandlerException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to retrieve dates for the dataset!", e);
            }

            if (newestToOldest.isEmpty()) {
                asString.add("No Data Available");
            } else {
                for (ImmutableDate date : newestToOldest) {
                    // this.dataSet.getTime().getCycleTimes();
                    String displayString = dateFormat.get().format(date);

                    if (!asString.contains(displayString)) {
                        asString.add(displayString);
                        dateStringToDateMap.put(displayString, date);
                    }
                }
            }
        }

        GriddedTimingSelectionDlg dlg = new GriddedTimingSelectionDlg(
                getShell(), griddedDataSet, sub, asString);

        GriddedTimeSelection selection = dlg.openDlg();

        if (selection.isCancel()) {
            return null;
        }

        int cycle = selection.getCycle();
        DataSetMetaData metaData = null;

        if (!selection.isLatest()) {
            String selectedDate = selection.getDate();
            metaData = retrieveFilteredDataSetMetaData(selectedDate, cycle);
        } else {
            // If useLatest, grab the most recent matching metadata
            try {
                if (dataSet.isMoving()) {
                    metaData = DataDeliveryHandlers.getDataSetMetaDataHandler()
                            .getMostRecentDataSetMetaDataByIntersection(
                                    dataSet.getDataSetName(),
                                    dataSet.getProviderName(),
                                    sub.getCoverage().getRequestEnvelope());
                } else {
                    metaData = DataDeliveryHandlers.getDataSetMetaDataHandler()
                            .getMostRecentDataSetMetaData(
                                    dataSet.getDataSetName(),
                                    dataSet.getProviderName());
                }
            } catch (RegistryHandlerException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to retrieve dates for the dataset!", e);
            }
        }
        if (metaData == null) {
            return null;
        }

        if (sub instanceof AdhocSubscription) {
            ((AdhocSubscription) sub).setUrl(metaData.getUrl());
        }

        GriddedDataSetMetaData gdsmd = (GriddedDataSetMetaData) metaData;
        GriddedTime time = gdsmd.getTime();

        // Remove once central on 18.1.1
        if (gdsmd.getCycle() != GriddedDataSetMetaData.NO_CYCLE) {
            time.addCycleTime(cycle);
        }

        return time;
    }

    /**
     * Retrieve the filtered {@link DATASETMETADATA}.
     *
     * @return the DataSetMetaData that applies, or null if none
     */
    protected DataSetMetaData retrieveFilteredDataSetMetaData(
            String selectedDate, int cycle) {
        try {
            GriddedDataSetMetaData dsmd = DataDeliveryHandlers
                    .getGriddedDataSetMetaDataHandler()
                    .getByDataSetDateAndCycle(dataSet.getDataSetName(),
                            dataSet.getProviderName(), cycle,
                            dateStringToDateMap.get(selectedDate));
            if (dsmd == null) {
                DataDeliveryUtils.showMessage(getShell(), SWT.OK, POPUP_TITLE,
                        NO_DATA_FOR_DATE_AND_CYCLE);
            }
            return dsmd;
        } catch (RegistryHandlerException e) {
            statusHandler.error("Error retrieving applicable DataSetMetaData.",
                    e);
            return null;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected <T extends Subscription> T populateSubscription(T sub,
            boolean create) {
        GriddedDataSet griddedDataSet = (GriddedDataSet) dataSet;

        Map<String, ParameterGroup> selectedParameterObjs = vTab
                .getParameters();
        sub.setParameterGroups(selectedParameterObjs);

        // TODO: OBE after all sites are 18.1.1 or beyond
        try {
            DataSetMetaData dsmd = DataDeliveryHandlers
                    .getDataSetMetaDataHandler()
                    .getMostRecentDataSetMetaData(dataSet.getDataSetName(),
                            dataSet.getProviderName());
            List<Parameter> paramList = new ArrayList<>(ParameterUtils
                    .generateParametersFromGroups(selectedParameterObjs,
                            DataType.GRID, dsmd)
                    .values());
            sub.setParameter(paramList);
        } catch (RegistryHandlerException e) {
            statusHandler
                    .error("Error generating Parameters from ParameterGroup. "
                            + "Parameters not set for subscription", e);
        }

        sub.setProvider(dataSet.getProviderName());
        sub.setDataSetName(dataSet.getDataSetName());
        sub.setDataSetType(dataSet.getDataSetType());

        GriddedCoverage cov = griddedDataSet.getCoverage();
        cov.setModelName(dataSet.getDataSetName());
        cov.setGridName(getNameText());
        GridCoverage coverage = cov.getGridCoverage();
        coverage.setName(getNameText());
        setCoverage(sub, cov);

        GriddedTime dataSetTime = griddedDataSet.getTime();
        GriddedTime newTime = new GriddedTime();

        if (sub instanceof AdhocSubscription) {
            newTime = setupDataSpecificTime(newTime, sub);

            if (newTime == null) {
                // user clicked cancel
                return null;
            }

            sub.setTime(newTime);
        } else if (!create) {
            GriddedTime time = (GriddedTime) sub.getTime();
            List<String> fcstHours = time.getFcstHours();
            String[] selectedItems = this.timingTabControls
                    .getSelectedFcstHours();
            List<Integer> fcstIndices = new ArrayList<>();
            for (String hr : selectedItems) {
                fcstIndices.add(fcstHours.indexOf(hr));
            }

            time.setSelectedTimeIndices(fcstIndices);
            subscription.setTime(time);
        } else {
            newTime.setEnd(dataSetTime.getEnd());
            newTime.setFormat(dataSetTime.getFormat());
            newTime.setNumTimes(dataSetTime.getNumTimes());
            newTime.setRequestEnd(dataSetTime.getRequestEnd());
            newTime.setRequestStart(dataSetTime.getRequestStart());
            newTime.setStart(dataSetTime.getStart());
            newTime.setStep(dataSetTime.getStep());
            newTime.setStepUnit(dataSetTime.getStepUnit());
            sub.setTime(newTime);
        }

        List<String> fcstHours = newTime.getFcstHours();

        // Set the gridded specific data on the time object
        String[] selectedItems = this.timingTabControls.getSelectedFcstHours();
        List<Integer> fcstIndices = new ArrayList<>();
        for (String hr : selectedItems) {
            fcstIndices.add(fcstHours.indexOf(hr));
        }

        newTime.setSelectedTimeIndices(fcstIndices);

        if (ensembleTab != null) {
            ensembleTab.populateSubscription(sub);
        }

        // Pass a fully populated subscription in to get the size
        if (dataSize != null) {
            sub.setDataSetSize(dataSize.getDataSetSizeInKb(sub));
        }

        return sub;
    }

    @Override
    protected TimeXML getDataTimeInfo() {
        return timingTabControls.getSaveInfo();
    }
}
