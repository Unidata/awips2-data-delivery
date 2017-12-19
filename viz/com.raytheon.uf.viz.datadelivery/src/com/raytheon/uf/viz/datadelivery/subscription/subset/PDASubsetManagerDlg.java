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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.PDADataSet;
import com.raytheon.uf.common.datadelivery.registry.PDADataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterUtils;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.retrieval.util.PDADataSizeUtils;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.ImmutableDate;
import com.raytheon.uf.common.util.SizeUtil;
import com.raytheon.uf.viz.datadelivery.common.xml.AreaXML;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.PointTimeXML;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.SubsetXML;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.TimeXML;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.VerticalXML;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;

/**
 * {@link SubsetManagerDlg} for point data sets.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -------------------------------------------
 * Aug 14, 2014  3121     dhladky   Initial creation.
 * Apr 25, 2016  5424     dhladky   Updated datasize calculation.
 * Apr 27, 2016  5366     tjensen   Updates for time selection changes
 * Jul 05, 2016  5683     tjensen   Added handling for null returns
 * Aug 17, 2016  5772     rjpeter   Fix time handling.
 * Oct 03, 2016  5772     tjensen   Set URL for PDA adhoc queries
 * Apr 25, 2017  1045     tjensen   Update for moving datasets
 * Jun 29, 2017  6130     tjensen   Set coverage before getting specific time.
 * Aug 02, 2017  6186     rjpeter   Removed setting of url.
 * Sep 12, 2017  6413     tjensen   Updated to support ParameterGroups
 * Sep 27, 2017  5948     tjensen   Added saving to and loading from subset xml
 * Oct 13, 2017  6461     tgurney   Allow creating queries with a time range
 * Nov 02, 2017  6461     tgurney   Use a single message box for multiple adhoc
 *                                  subs created
 * Dec 19, 2017  6523     tjensen   Changes for VerticalXML updates
 *
 * </pre>
 *
 * @author dhladky
 */

public class PDASubsetManagerDlg extends SubsetManagerDlg {
    /** Status Handler */
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDASubsetManagerDlg.class);

    private static final String TIMING_TAB_TEXT = "Retrieval Times";

    /** Point data size utility */
    private PDADataSizeUtils dataSize;

    /** The point subset tab */
    private PDATimingSubsetTab timingTabControls;

    /**
     * Constructor.
     *
     * @param shell
     *            parent shell
     * @param loadDataSet
     *            load data set flag
     * @param subscription
     *            Subscription object
     */
    @SuppressWarnings("rawtypes")
    public PDASubsetManagerDlg(Shell shell, boolean loadDataSet,
            Subscription subscription) {
        super(shell, loadDataSet, subscription);
        this.adhocCallback = new PDACreateAdhocCallback();
        setTitle();
    }

    /**
     * Constructor.
     *
     * @param shell
     *            parent shell
     * @param dataSet
     *            the data set
     * @param loadDataSet
     *            load data set flag
     * @param subsetXml
     *            the subset xml object
     */
    public PDASubsetManagerDlg(Shell shell, PDADataSet dataSet,
            boolean loadDataSet, SubsetXML subsetXml) {
        super(shell, loadDataSet, dataSet);
        this.dataSet = dataSet;
        this.subsetXml = subsetXml;
        this.adhocCallback = new PDACreateAdhocCallback();
        setTitle();
    }

    /**
     * Constructor.
     *
     * @param shell
     *            the parent shell
     * @param dataSet
     *            the data set
     */
    public PDASubsetManagerDlg(Shell shell, PDADataSet dataSet) {
        super(shell, dataSet);
        this.dataSet = dataSet;
        this.adhocCallback = new PDACreateAdhocCallback();
        setTitle();
    }

    /**
     * Creates the date formatter
     */
    private static SimpleDateFormat createDateFormatter() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy - HH:mm'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf;
    }

    @Override
    void createTabs(TabFolder tabFolder) {
        GridData tgd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        GridLayout tgl = new GridLayout(1, false);
        GridData sgd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        GridLayout sgl = new GridLayout(1, false);

        TabItem timingTab = new TabItem(tabFolder, SWT.NONE);
        timingTab.setText(TIMING_TAB_TEXT);
        timingTab.setData("valid", false);
        Composite timingComp = new Composite(tabFolder, SWT.NONE);
        timingComp.setLayout(tgl);
        timingComp.setLayoutData(tgd);
        timingTab.setControl(timingComp);
        SortedSet<ImmutableDate> dates = retrieveDatesForDataSet(null);
        timingTabControls = new PDATimingSubsetTab(timingComp, this, shell,
                dates);

        TabItem verticalTab = new TabItem(tabFolder, SWT.NONE);
        verticalTab.setText(VERTICAL_TAB);
        verticalTab.setData("valid", false);
        Composite vertComp = new Composite(tabFolder, SWT.NONE);
        vertComp.setLayout(sgl);
        vertComp.setLayoutData(sgd);
        verticalTab.setControl(vertComp);
        vTab = new VerticalSubsetTab(vertComp, dataSet, this);

        TabItem spatialTab = new TabItem(tabFolder, SWT.NONE);
        spatialTab.setText(SPATIAL_TAB);
        Composite spatialComp = new Composite(tabFolder, SWT.NONE);
        spatialComp.setLayout(sgl);
        spatialComp.setLayoutData(sgd);
        spatialTab.setControl(spatialComp);
        spatialTabControls = new SpatialSubsetTab(spatialComp, dataSet, this);
    }

    @Override
    public void updateDataSize() {
        if (!initialized) {
            return;
        }

        if (dataSize == null) {
            this.dataSize = new PDADataSizeUtils((PDADataSet) dataSet);
        }

        ReferencedEnvelope env = spatialTabControls.getEnvelope();
        Map<String, ParameterGroup> params = vTab.getParameters();

        // Update the data set size label text.
        this.sizeLbl.setText(SizeUtil.prettyByteSize(
                dataSize.getDataSetSizeInBytes(params, env)) + " of "
                + SizeUtil.prettyByteSize(dataSize.getFullSizeInBytes()));
    }

    /**
     * @return Only the metadatas that satisfy the subscription. If the
     *         subscription is null then get all metadatas in the subset
     */
    private List<PDADataSetMetaData> getFilteredMetaDatas(Subscription sub) {
        List<DataSetMetaData> metaDatas = null;
        try {
            metaDatas = DataDeliveryHandlers.getDataSetMetaDataHandler()
                    .getDataSetMetaDataByIntersection(dataSet.getDataSetName(),
                            dataSet.getProviderName(),
                            dataSet.getCoverage().getEnvelope(), 0);
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error when retrieving metadata for dataset "
                            + dataSet.getDataSetName(),
                    e);
            return Collections.emptyList();
        }

        List<PDADataSetMetaData> filteredMetaDatas;
        if (sub != null) {
            filteredMetaDatas = metaDatas.stream()
                    .map(m -> (PDADataSetMetaData) m).filter(m -> {
                        try {
                            return m.satisfiesSubscription(sub) == null;
                        } catch (Exception e) {
                            statusHandler.handle(Priority.PROBLEM,
                                    "Failed to check metadata "
                                            + m.getMetaDataID()
                                            + " against subscription "
                                            + sub.getName(),
                                    e);
                            return false;
                        }
                    }).collect(Collectors.toList());
        } else {
            filteredMetaDatas = metaDatas.stream()
                    .map(m -> (PDADataSetMetaData) m)
                    .collect(Collectors.toList());
        }
        return filteredMetaDatas;
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    protected void handleQuery() {
        if (!validated() || querySubExists()) {
            return;
        }

        AdhocSubscription tmpSub = createSubscription(new AdhocSubscription(),
                Network.OPSNET);

        List<PDADataSetMetaData> pdaMetaDatas = getFilteredMetaDatas(tmpSub);

        if (pdaMetaDatas.isEmpty()) {
            DataDeliveryUtils.showMessage(getShell(), SWT.OK, POPUP_TITLE,
                    "No data available for this subset.");
            return;
        }

        SortedSet<Date> availableDates = pdaMetaDatas.stream()
                .map(m -> m.getDate())
                .collect(Collectors.toCollection(TreeSet::new));

        PDATimingSelectionDlg dlg = new PDATimingSelectionDlg(getShell(),
                availableDates, tmpSub);

        PDATimeSelection selection = dlg.openDlg();

        if (selection.isCancel()) {
            return;
        }

        List<Time> times = pdaMetaDatas.stream()
                .filter(m -> selection.getTimeRange().contains(m.getDate()))
                .map(m -> m.getTime()).collect(Collectors.toList());

        if (times == null || times.isEmpty()) {
            DataDeliveryUtils.showMessage(getShell(), SWT.OK, POPUP_TITLE,
                    "No data were found in the selected time range.");
            return;
        }

        int result = DataDeliveryUtils.showYesNoMessage(getShell(),
                "Confirm Query Creation",
                times.size() + " product(s) found for dataset "
                        + dataSet.getDataSetName()
                        + " in specified time range. Continue?");

        if (result != SWT.YES) {
            return;
        }

        /*
         * Subs will be named name-1, name-2, ... Make sure these names are not
         * already being used
         */
        for (int i = 0; i < times.size(); i++) {
            if (querySubExists(getNameText() + "-" + i)) {
                return;
            }
        }
        StringBuilder subListMessage = new StringBuilder();
        int subsCreated = 0;
        for (int i = 0; i < times.size(); i++) {
            AdhocSubscription as = createSubscription(new AdhocSubscription(),
                    Network.OPSNET);
            as.setName(getNameText() + "-" + (i + 1));
            as.setTime(times.get(i));
            String thisMessage = storeQuerySub(as, false);
            subListMessage.append("\n" + as.getName() + ": ");
            if (thisMessage != null) {
                subsCreated += 1;
                subListMessage.append(thisMessage);
            } else {
                subListMessage.append("Failed to create");
            }
        }
        DataDeliveryUtils.showMessage(getShell(), SWT.OK, "Query Scheduled",
                subsCreated + "/" + times.size()
                        + " queries successfully created." + subListMessage);
    }

    @SuppressWarnings({ "rawtypes" })
    protected Time handleRecurringDataSpecificTime(Subscription sub) {
        Time time = null;
        SortedSet<ImmutableDate> newestToOldest = retrieveDatesForDataSet(sub);

        if ((newestToOldest != null) && newestToOldest.isEmpty()) {
            DataSetMetaData metaData = retrieveFilteredDataSetMetaData(
                    newestToOldest.first());
            if (metaData != null) {
                time = metaData.getTime();
            }
        }

        if (time == null) {
            time = new Time();
            time.setStart(new Date());
            time.setEnd(new Date());
        }

        return time;

    }

    @Override
    protected PointTimeXML getTimeXmlFromSubscription() {
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Subscription populateSubscription(Subscription sub,
            boolean create) {

        sub.setProvider(dataSet.getProviderName());
        sub.setDataSetName(dataSet.getDataSetName());
        sub.setDataSetType(dataSet.getDataSetType());
        sub.setDataSetName(dataSet.getDataSetName());

        // Coverage must be set before getting specific time
        Coverage cov = new Coverage();
        cov.setEnvelope(dataSet.getCoverage().getEnvelope());
        setCoverage(sub, cov);

        Map<String, ParameterGroup> selectedParameterObjs = vTab
                .getParameters();
        sub.setParameterGroups(selectedParameterObjs);

        if (!(sub instanceof AdhocSubscription)) {
            Time newTime = handleRecurringDataSpecificTime(sub);
            if (newTime == null) {
                return null;
            }
            sub.setTime(newTime);
        }

        // TODO: OBE after all sites are 18.1.1 or beyond
        List<Parameter> paramList = new ArrayList<>(
                ParameterUtils.generateParametersFromGroups(
                        selectedParameterObjs, DataType.PDA, null).values());
        sub.setParameter(paramList);

        if (dataSize == null) {
            this.dataSize = new PDADataSizeUtils((PDADataSet) dataSet);
        }

        sub.setDataSetSize(dataSize.getDataSetSizeInKb(sub));

        return sub;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void loadFromSubscription(Subscription mySubscription) {
        super.loadFromSubscription(mySubscription);
        AreaXML area = new AreaXML();
        ReferencedEnvelope envelope = this.subscription.getCoverage()
                .getEnvelope();
        ReferencedEnvelope requestEnvelope = this.subscription.getCoverage()
                .getRequestEnvelope();

        if ((requestEnvelope != null) && !requestEnvelope.isEmpty()) {
            area.setEnvelope(requestEnvelope);
        } else {
            area.setEnvelope(envelope);
        }
        spatialTabControls.setDataSet(this.dataSet);
        spatialTabControls.populate(area);

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
    }

    @Override
    protected void populateSubsetXML(SubsetXML subset) {
        super.populateSubsetXML(subset);
        if (vTab != null) {
            // next save vertical layer/parameter info
            List<VerticalXML> vertList = vTab.getSaveInfo();
            subset.setVerticalList(vertList);
        }
    }

    @Override
    protected void loadFromSubsetXML(SubsetXML subsetXml) {
        super.loadFromSubsetXML(subsetXml);

        List<VerticalXML> vertList = subsetXml.getVerticalList();
        vTab.populate(vertList, dataSet);

        updateDataSize();
    }

    @Override
    protected TimeXML getDataTimeInfo() {
        return timingTabControls.getSaveInfo();
    }

    /**
     * Retrieve the filtered {@link DATASETMETADATA}.
     *
     * @return the DataSetMetaData that applies, or null if none
     */
    @SuppressWarnings("rawtypes")
    protected DataSetMetaData retrieveFilteredDataSetMetaData(
            Date selectedDate) {
        try {
            PDADataSetMetaData dsmd = (PDADataSetMetaData) DataDeliveryHandlers
                    .getDataSetMetaDataHandler()
                    .getByDataSetDate(dataSet.getDataSetName(),
                            dataSet.getProviderName(), selectedDate);
            return dsmd;
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error retrieving applicable DataSetMetaData.", e);
            return null;
        }
    }

    /**
     * Retrieve the filtered {@link DATES}.
     *
     * @return the Set<ImmutableDate> that apply, or null if none
     */
    private SortedSet<ImmutableDate> retrieveDatesForDataSet(Subscription sub) {

        SortedSet<ImmutableDate> rval = null;

        List<PDADataSetMetaData> pdaMetaDatas = getFilteredMetaDatas(sub);

        if (pdaMetaDatas.isEmpty()) {
            return null;
        }

        rval = new TreeSet<>(Ordering.natural().reverse());
        for (PDADataSetMetaData m : pdaMetaDatas) {
            rval.add(m.getDate());
        }

        if (rval.isEmpty()) {
            return null;
        }
        return rval;

    }

}
