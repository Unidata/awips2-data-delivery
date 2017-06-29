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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

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
import com.raytheon.uf.common.datadelivery.registry.DataLevelType;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.PDADataSet;
import com.raytheon.uf.common.datadelivery.registry.PDADataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
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

    private static final String NO_DATA_FOR_DATE = "No data is available for the specified time.";

    /** Point data size utility */
    private PDADataSizeUtils dataSize;

    /** The point subset tab */
    private PDATimingSubsetTab timingTabControls;

    private final DateFormat dateFormat;

    private DataSetMetaData metaData;

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
        dateFormat = createDateFormatter();
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
        dateFormat = createDateFormatter();
        this.dataSet = dataSet;
        this.subsetXml = subsetXml;
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
        dateFormat = createDateFormatter();
        this.dataSet = dataSet;
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

        // Update the data set size label text.
        this.sizeLbl.setText(
                SizeUtil.prettyByteSize(dataSize.getDataSetSizeInBytes(env)));
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    protected Time setupDataSpecificTime(Time newTime, Subscription sub) {
        if (sub instanceof AdhocSubscription) {
            return handleAdhocDataSpecificTime(sub);
        }

        return handleRecurringDataSpecificTime(sub);
    }

    protected Time handleAdhocDataSpecificTime(Subscription sub) {

        SortedSet<ImmutableDate> newestToOldest = retrieveDatesForDataSet(sub);

        if ((newestToOldest == null) || newestToOldest.isEmpty()) {
            DataDeliveryUtils.showMessage(getShell(), SWT.OK, POPUP_TITLE,
                    "No data is available for this Data Set");
            return null;
        }

        List<String> asString = new ArrayList<>(newestToOldest.size());
        Map<String, ImmutableDate> dateStringToDateMap = new HashMap<>(
                newestToOldest.size(), 1);

        for (ImmutableDate date : newestToOldest) {
            String displayString = dateFormat.format(date);

            if (!asString.contains(displayString)) {
                asString.add(displayString);
                dateStringToDateMap.put(displayString, date);
            }
        }

        PDATimingSelectionDlg dlg = new PDATimingSelectionDlg(getShell(),
                (PDADataSet) dataSet, sub, asString);

        PDATimeSelection selection = dlg.openDlg();

        if (selection.isCancel()) {
            return null;
        }

        Date selectedDate = dateStringToDateMap.get(selection.getDate());
        metaData = retrieveFilteredDataSetMetaData(selectedDate);
        if (metaData == null) {
            DataDeliveryUtils.showMessage(getShell(), SWT.OK, POPUP_TITLE,
                    NO_DATA_FOR_DATE);
            return null;
        }

        Time time = metaData.getTime();

        if (time == null) {
            DataDeliveryUtils.showMessage(getShell(), SWT.OK, POPUP_TITLE,
                    NO_DATA_FOR_DATE);
        }

        return time;

    }

    /**
     * Return the {@link Time} object that should be associated with this
     * subscription. It will either be null for a reoccurring subscription, or
     * the {@link DataSetMetaData} url if an adhoc query is being performed for
     * a non-latest date.
     *
     * @return the url to use
     */
    public String getSubscriptionUrl() {
        return (this.metaData == null) ? null : this.metaData.getUrl();
    }

    protected Time handleRecurringDataSpecificTime(Subscription sub) {
        Time time = null;
        SortedSet<ImmutableDate> newestToOldest = retrieveDatesForDataSet(sub);

        if ((newestToOldest != null) && newestToOldest.isEmpty()) {
            metaData = retrieveFilteredDataSetMetaData(newestToOldest.first());
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

        Time newTime = new Time();
        newTime = setupDataSpecificTime(newTime, sub);
        if (newTime == null) {
            return null;
        }
        sub.setTime(newTime);

        sub.setUrl(getSubscriptionUrl());

        List<Parameter> paramList = new ArrayList<>();
        Map<String, Parameter> paramMap = dataSet.getParameters();
        List<DataLevelType> levelTypeList = new ArrayList<>();
        levelTypeList.add(new DataLevelType(DataLevelType.LevelType.SFC));
        for (Parameter p : paramMap.values()) {
            p.setDataType(DataType.PDA);
            p.setLevelType(levelTypeList);
            paramList.add(p);
        }

        sub.setParameter(paramList);

        if (dataSize == null) {
            this.dataSize = new PDADataSizeUtils((PDADataSet) dataSet);
        }

        sub.setDataSetSize(dataSize.getDataSetSizeInKb(sub));

        return sub;
    }

    @SuppressWarnings("rawtypes")
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
    }

    /**
     * {@inheritDoc}
     */
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
    protected SortedSet<ImmutableDate> retrieveDatesForDataSet(
            Subscription sub) {
        SortedSet<ImmutableDate> newestToOldest = new TreeSet<>(
                Ordering.natural().reverse());
        try {
            if (dataSet.isMoving() && sub != null) {
                newestToOldest.addAll(DataDeliveryHandlers
                        .getDataSetMetaDataHandler()
                        .getDatesForDataSetByIntersection(
                                dataSet.getDataSetName(),
                                dataSet.getProviderName(),
                                sub.getCoverage().getRequestEnvelope()));
            } else {
                newestToOldest
                        .addAll(DataDeliveryHandlers.getDataSetMetaDataHandler()
                                .getDatesForDataSet(dataSet.getDataSetName(),
                                        dataSet.getProviderName()));
            }
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error retrieving applicable Dates.", e);
            return null;
        }
        return newestToOldest;

    }
}
