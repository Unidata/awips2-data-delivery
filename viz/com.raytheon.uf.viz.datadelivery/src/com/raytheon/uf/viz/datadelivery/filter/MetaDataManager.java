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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.GriddedDataSet;
import com.raytheon.uf.common.datadelivery.registry.LevelGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.ProviderType;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.ITimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.viz.datadelivery.filter.config.xml.FilterSettingsXML;
import com.raytheon.uf.viz.datadelivery.filter.config.xml.FilterTypeXML;

/**
 * Metadata manager.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Mar 01, 2012           mpduff    Initial creation
 * Jun 21, 2012  736      djohnson  Use queries to perform DataSet filtering.
 * Jul 24, 2012  955      djohnson  Matching datasets are returned in a {@link
 *                                  Set}.
 * Aug 02, 2012  955      djohnson  Type-safe registry query/responses.
 * Aug 10, 2012  1022     djohnson  Store provider name in {@link SubsetXml},
 *                                  use {@link GriddedDataSet}.
 * Aug 20, 2012  743      djohnson  Use {@link ITimer} to time operations, use
 *                                  DataSet.
 * Aug 28, 2012  1022     djohnson  Speed up filter retrieval by using worker
 *                                  threads.
 * Oct 05, 2012  1241     djohnson  Replace RegistryManager calls with registry
 *                                  handler calls.
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Dec 10, 2012  1259     bsteffen  Switch Data Delivery from LatLon to
 *                                  referenced envelopes.
 * Jun 04, 2013  223      mpduff    Add data set type to filter.
 * Jul 05, 2013  2137     mpduff    Single data type.
 * Jul 29, 2013  2196     bgonzale  Added levels isEmpty check.
 * Sep 14, 2017  6413     tjensen   Update for ParameterGroups
 * Mar 01, 2018  7204     nabowle   Add clearAvailableDataSets()
 *
 * </pre>
 *
 * @author mpduff
 */

public class MetaDataManager {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(MetaDataManager.class);

    /** The single instance */
    private static MetaDataManager instance = new MetaDataManager();

    /**
     * Get an instance of this class.
     *
     * @return an instance
     */
    public static MetaDataManager getInstance() {
        return instance;
    }

    // These collection variables must be volatile so we can safely publish new
    // references from a multi-threaded call
    private volatile SortedSet<String> allAvailableDataSetNames;

    private volatile Set<Provider> allAvailableProviders;

    private volatile SortedSet<String> allAvailableLevels;

    private volatile SortedSet<String> allAvailableParameters;

    private volatile Set<DataSet> allAvailableDataSets;

    /**
     * envelope of selected area.
     */
    private ReferencedEnvelope envelope;

    /** reread flag */
    private boolean reread = true;

    /** The data type */
    private String dataType;

    /**
     * Private constructor.
     */
    private MetaDataManager() {
    }

    /**
     * Get the available data providers for the provided data type.
     *
     * @param dataType
     *            The data type
     *
     * @return SortedSet of available providers
     */
    public SortedSet<String> getAvailableDataProvidersByType(String dataType) {
        SortedSet<String> providers = new TreeSet<>();

        if (allAvailableProviders == null) {
            populateAvailableProviders();
        }
        for (Provider p : allAvailableProviders) {
            for (ProviderType pt : p.getProviderType()) {
                if (pt.getDataType().toString().equalsIgnoreCase(dataType)) {
                    providers.add(p.getName());
                    break;
                }
            }
        }

        return providers;
    }

    /**
     * Get all available Data Providers
     *
     * @return all data providers
     */
    public SortedSet<String> getAvailableDataProviders() {
        if (allAvailableProviders == null) {
            populateAvailableProviders();
        }

        SortedSet<String> providers = new TreeSet<>();
        for (Provider p : allAvailableProviders) {
            providers.add(p.getName());
        }

        return providers;
    }

    /**
     * Populate the available providers.
     */
    private void populateAvailableProviders() {
        allAvailableProviders = new HashSet<>();

        DataType dataType = DataType.valueOfIgnoreCase(this.dataType);
        try {
            for (Provider provider : DataDeliveryHandlers.getProviderHandler()
                    .getAll()) {

                ProviderType providerType = provider.getProviderType(dataType);
                if (providerType != null) {
                    allAvailableProviders.add(provider);
                }
            }
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to retrieve the list of providers.", e);
        }
    }

    /**
     * Get all available data sets
     *
     * @return all data sets
     */
    @SuppressWarnings("rawtypes")
    public SortedSet<String> getAvailableDataSetNames() {
        if (allAvailableDataSetNames == null) {
            SortedSet<String> newAllAvailableDataSetNames = new TreeSet<>();

            Set<DataSet> dataSets = getAvailableDataSets();
            for (DataSet ds : dataSets) {
                newAllAvailableDataSetNames.add(ds.getDataSetName());
            }
            allAvailableDataSetNames = newAllAvailableDataSetNames;
        }

        return allAvailableDataSetNames;
    }

    /**
     * Get the available data types
     *
     * @return List of data types
     */
    public List<String> getAvailableDataTypes() {
        Set<String> typeSet = new TreeSet<>();

        ITimer timer = TimeUtil.getTimer();
        timer.start();

        try {
            for (Provider provider : DataDeliveryHandlers.getProviderHandler()
                    .getAll()) {

                for (ProviderType type : provider.getProviderType()) {
                    typeSet.add(type.getDataType().toString());
                }
            }
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to retrieve the provider list.", e);
        }

        List<String> typeList = new ArrayList<>(typeSet);
        timer.stop();

        return typeList;
    }

    /**
     * Get all available levels
     *
     * @return available levels
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public SortedSet<String> getAvailableLevels() {
        if (allAvailableLevels == null) {
            SortedSet<String> newAllAvailableLevels = new TreeSet<>();

            Set<DataSet> dataSets = getAvailableDataSets();
            for (DataSet ds : dataSets) {
                Map<String, ParameterGroup> parameterGroups = ds
                        .getParameterGroups();
                for (ParameterGroup pg : parameterGroups.values()) {
                    Set<String> dsLevels = pg.getGroupedLevels().keySet();
                    newAllAvailableLevels.addAll(dsLevels);
                }
            }
            allAvailableLevels = newAllAvailableLevels;
        }

        return allAvailableLevels;
    }

    /**
     * Get all available parameters
     *
     * @return all available parameters
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public SortedSet<String> getAvailableParameters() {
        if (allAvailableParameters == null) {
            SortedSet<String> newAllAvailableParameters = new TreeSet<>();

            Set<DataSet> dataSets = getAvailableDataSets();
            for (DataSet ds : dataSets) {
                Set<String> dsParams = ds.getParameterGroups().keySet();
                newAllAvailableParameters.addAll(dsParams);
            }
            allAvailableParameters = newAllAvailableParameters;
        }
        return allAvailableParameters;
    }

    @SuppressWarnings("rawtypes")
    public Set<DataSet> getAvailableDataSets() {
        if (allAvailableDataSets == null) {
            Set<DataSet> newAllAvailableDataSets = new HashSet<>();

            List<String> dataTypeList = new ArrayList<>();
            dataTypeList.add(dataType);
            try {
                List<DataSet> dataSets = DataDeliveryHandlers
                        .getDataSetHandler().getByFilters(null, null, null,
                                null, dataTypeList, envelope);
                if (dataSets != null) {
                    newAllAvailableDataSets.addAll(dataSets);
                }
                allAvailableDataSets = newAllAvailableDataSets;
            } catch (RegistryHandlerException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to retrieve the dataset list.", e);
            }
        }
        return allAvailableDataSets;
    }

    /**
     * Get the data set.
     *
     * @param dataSetName
     *            The data set name
     * @param providerName
     *            The provider name
     * @return The DataSet
     */
    public DataSet getDataSet(String dataSetName, String providerName) {
        try {
            return DataDeliveryHandlers.getDataSetHandler()
                    .getByNameAndProvider(dataSetName, providerName);
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to retrieve dataset.", e);
            return null;
        }
    }

    /**
     * Get the data sets that match the filter criteria.
     *
     * @param xml
     *            The FilterSettingsXML filter settings
     *
     * @return DataSet objects that match the filter criteria
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Set<DataSet> getMatchingDataSets(FilterSettingsXML xml) {
        Set<DataSet> filteredDataSets = new HashSet<>();

        // Get the filter types
        ArrayList<FilterTypeXML> filterTypeList = xml.getFilterTypeList();

        HashMap<String, ArrayList<String>> filterMap = new HashMap<>();

        // Now filter the available data values
        for (FilterTypeXML filterType : filterTypeList) {
            if (filterType.getValues().size() > 0) {
                filterMap.put(filterType.getFilterType(),
                        filterType.getValues());
            }
        }

        List<String> providers = null;
        List<String> dataSetNames = null;
        List<String> levels = null;
        List<String> parameterNames = null;

        // iterate over the filters
        for (Entry<String, ArrayList<String>> filterEntry : filterMap
                .entrySet()) {
            String filterType = filterEntry.getKey();
            List<String> values = filterEntry.getValue();

            // If there are no values to filter on, just continue to the next
            // filter
            if (values == null) {
                continue;
            }

            if ("Data Provider".equalsIgnoreCase(filterType)) {
                providers = values;
            } else if ("Data Set".equalsIgnoreCase(filterType)) {
                dataSetNames = values;
            } else if ("Level".equalsIgnoreCase(filterType)) {
                levels = values;
            } else if ("Parameter".equalsIgnoreCase(filterType)) {
                parameterNames = values;
            }
        }

        Set<DataSet> dataSets = getAvailableDataSets();
        for (DataSet ds : dataSets) {
            boolean dsMatch = false;
            if (dataSetNames == null
                    || dataSetNames.contains(ds.getDataSetName())) {
                if (providers == null
                        || providers.contains(ds.getProviderName())) {
                    if (parameterNames == null && levels == null) {
                        /*
                         * If we aren't filtering on params or levels, we
                         * already match
                         */
                        dsMatch = true;
                    } else {
                        Map<String, ParameterGroup> dsParams = ds
                                .getParameterGroups();
                        for (ParameterGroup dsParam : dsParams.values()) {
                            if (parameterNames == null || parameterNames
                                    .contains(dsParam.getKey())) {
                                if (levels == null) {
                                    dsMatch = true;
                                } else {
                                    for (LevelGroup dsLevel : dsParam
                                            .getGroupedLevels().values()) {
                                        if (levels.contains(dsLevel.getKey())) {
                                            dsMatch = true;
                                            break;
                                        }
                                    }
                                }

                                // if we found a match, we can stop looking
                                if (dsMatch) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (dsMatch) {
                filteredDataSets.add(ds);
            }
        }

        return filteredDataSets;
    }

    /**
     * Get the metadata
     *
     * @param dataType
     *            the data type
     */
    public void readMetaData(String dataType) {
        if (reread) {
            this.dataType = dataType;

            reread = false;
            allAvailableDataSetNames = null;
            allAvailableLevels = null;
            allAvailableParameters = null;
            allAvailableProviders = null;
            allAvailableDataSets = null;

            retrieveNewMetadata();
        }
    }

    /**
     * Sets flag to reread the metadata
     */
    public void rereadMetaData() {
        reread = true;
    }

    public void clearAvailableDataSets() {
        allAvailableDataSets = null;
    }

    /**
     * Uses multiple threads to speed up the retrieval of new metadata
     * information. This reduces the loading of new metadata from the cumulative
     * time of all queries to the max time of the queries.
     */
    private void retrieveNewMetadata() {
        getAvailableDataProviders();
        getAvailableDataSetNames();
        getAvailableParameters();
        getAvailableLevels();
    }

    public void setArea(ReferencedEnvelope envelope) {
        this.envelope = envelope;
    }
}
