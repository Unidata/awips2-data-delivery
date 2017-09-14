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
package com.raytheon.uf.edex.ogc.registry;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfigurationManager;
import com.raytheon.uf.common.datadelivery.harvester.OGCAgent;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataLevelType;
import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.DataSetName;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.Levels;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataSetHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataSetMetaDataHandler;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.ILocalizationPathObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.registry.ebxml.RegistryUtil;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.registry.schemas.ebxml.util.EbxmlJaxbManager;
import com.raytheon.uf.common.util.PropertiesUtil;
import com.raytheon.uf.edex.ogc.common.db.ICollectorAddon;
import com.raytheon.uf.edex.ogc.common.db.SimpleDimension;
import com.raytheon.uf.edex.ogc.common.db.SimpleLayer;

/**
 * Collector Used to gather data with DPA, used for AWIPS registry data feeds
 * from providers.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 23, 2013           bclement  Initial creation
 * Aug 08, 2013           dhladky   Made operational
 * Jan 13, 2014  2679     dhladky   multiple layers
 * Mar 31, 2014  2889     dhladky   Added username for notification center
 *                                  tracking.
 * Apr 14, 2014  3012     dhladky   Cleaned up.
 * Jun 08, 2014  3141     dhladky   DPA SOAP to central registry comm
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Apr 05, 2017  1045     tjensen   Add Coverage generics for DataSetMetaData
 * May 25, 2017  6186     rjpeter   Updated abstract method naming.
 * Aug 04, 2017  6356     tjensen   Add support for DPA failover
 * Sep 12, 2017  6413     tjensen   Removed parameters from DataSetName
 *
 * </pre>
 *
 * @author bclement
 */
public abstract class RegistryCollectorAddon<D extends SimpleDimension, L extends SimpleLayer<D>, R extends PluginDataObject, DS extends DataSet<?, ?>, DSMD extends DataSetMetaData<?, ?>>
        implements ICollectorAddon<D, L, R> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected HarvesterConfig config = null;

    protected OGCAgent agent = null;

    protected Map<String, Map<String, Parameter>> parametersByLayer = new HashMap<>();

    protected String[] packageNames = null;

    protected static final String pathSeparator = ":";

    private static final String centralUpdatesProperty = "dpa.enable.central.updates";

    private volatile boolean centralUpdatesEnabled;

    private static final String DPA_PROP_FILE_PATH = "datadelivery"
            + IPathManager.SEPARATOR + "dpa" + IPathManager.SEPARATOR
            + "dpaConfig.properties";

    private final ILocalizationPathObserver observer = new ILocalizationPathObserver() {

        @Override
        public void fileChanged(ILocalizationFile file) {
            if (file.getPath().contains(DPA_PROP_FILE_PATH)) {
                reloadDpaProperties();
            }
        }
    };

    /**
     * public (Spring loaded) constructor
     */
    public RegistryCollectorAddon() {
        /*
         * Need these to be setup before anything happens. This is the default
         * JAXB path for registry objects, collector won't work without it.
         */
        String paths = System.getProperty("registry.ogc.jaxb.path");

        if (paths != null) {
            try {
                this.packageNames = paths.split(pathSeparator);
            } catch (Exception e) {
                logger.error(
                        "Couldn't parse Registry Collector JAXB path list: "
                                + paths,
                        e);
            }

            if (packageNames != null) {
                for (String path : packageNames) {
                    if (!path.isEmpty()) {
                        EbxmlJaxbManager.getInstance().findJaxables(path);
                    }
                }
                this.config = HarvesterConfigurationManager
                        .getOGCConfiguration();
                setAgent((OGCAgent) config.getAgent());
                storeProvider(config.getProvider());
            }
            IPathManager pm = PathManagerFactory.getPathManager();
            pm.addLocalizationPathObserver(DPA_PROP_FILE_PATH, observer);
            reloadDpaProperties();
        } else {
            throw new IllegalArgumentException(
                    "The registry OGC JAXB path property, {registry.ogc.jaxb.path} is not viewable! \n"
                            + "Registry Collector will not operate correctly in this state!");
        }

    }

    public HarvesterConfig getConfig() {
        return config;
    }

    public void setConfig(HarvesterConfig config) {
        this.config = config;
    }

    /**
     * Find the DPA config
     *
     * @return
     */
    public HarvesterConfig getConfiguration() {
        return config;
    }

    /**
     * Store DataSetMetaData objects
     *
     * @param metaDatas
     * @param dataSet
     */
    protected void storeMetaData(DSMD metaData) {

        DataSetMetaDataHandler handler = DataDeliveryHandlers
                .getDataSetMetaDataHandler();

        final String description = metaData.getDataSetDescription();

        if (centralUpdatesEnabled) {
            logger.info("Attempting store of DataSetMetaData[" + description
                    + "] " + "Date: " + metaData.getDate());

            try {
                handler.update(RegistryUtil.registryUser, metaData);
                logger.info("DataSetMetaData [" + description
                        + "] successfully stored in Registry");
            } catch (RegistryHandlerException e) {
                logger.error("DataSetMetaData [" + description
                        + "] failed to store in Registry", e);
            }
        } else {
            logger.info("DataSetMetaData [" + description
                    + "] not stored in Registry. Registry updates currently disabled.");
        }
    }

    /**
     *
     * @param dataSetToStore
     */
    protected void storeDataSetName(DS dataSetToStore) {

        DataSetName dsn = new DataSetName();
        // Set the RegistryObject Id keys for this Object
        // using the values from the DataSetMetaData Object.
        dsn.setProviderName(dataSetToStore.getProviderName());
        dsn.setDataSetType(dataSetToStore.getDataSetType());
        dsn.setDataSetName(dataSetToStore.getDataSetName());

        if (centralUpdatesEnabled) {
            try {
                DataDeliveryHandlers.getDataSetNameHandler()
                        .update(RegistryUtil.registryUser, dsn);
                logger.info("DataSetName object store complete, dataset ["
                        + dsn.getDataSetName() + "]");
            } catch (RegistryHandlerException e) {
                logger.error("DataSetName object store failed:", e);
            }
        } else {
            logger.info("DatasetName object for dataset ["
                    + dsn.getDataSetName()
                    + "] not stored in Registry. Registry updates currently disabled.");
        }

    }

    /**
     * @param dataSet
     */
    protected void storeDataSet(final DS dataSet) {

        DataSet<?, ?> dataSetToStore = getDataSetToStore(dataSet);
        final String dataSetName = dataSetToStore.getDataSetName();
        DataSetHandler handler = DataDeliveryHandlers.getDataSetHandler();

        if (centralUpdatesEnabled) {
            try {
                handler.update(RegistryUtil.registryUser, dataSetToStore);
                logger.info("Dataset [" + dataSetName
                        + "] successfully stored in Registry");
                storeDataSetName(dataSet);

            } catch (RegistryHandlerException e) {
                logger.error("Dataset [" + dataSetName
                        + "] failed to store in Registry", e);
            }
        } else {
            logger.info("Dataset [" + dataSetName
                    + "] not stored in Registry. Registry updates currently disabled.");
        }
    }

    /**
     * Make sure our provider is contained in the Registry
     *
     * @param provider
     */
    protected void storeProvider(final Provider provider) {

        try {
            DataDeliveryHandlers.getProviderHandler()
                    .update(RegistryUtil.registryUser, provider);
        } catch (RegistryHandlerException e) {
            logger.error("Provider [" + provider.getName()
                    + "] failed to store in Registry", e);
        }
    }

    /**
     * Checks for a {@link DataSet} already existing with the same name in the
     * Registry. If so, then combine the objects.
     *
     * @param dataSet
     *            the dataSet
     * @return the dataSet instance that should be stored to the registry
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected DataSet getDataSetToStore(DataSet dataSet) {
        try {
            DataSet<Time, Coverage> result = DataDeliveryHandlers
                    .getDataSetHandler()
                    .getByNameAndProvider(dataSet.getDataSetName(),
                            dataSet.getProviderName());
            if (result != null) {
                dataSet.combine(result);
            }
        } catch (RegistryHandlerException e) {
            logger.error("Unable to retrieve dataset.", e);
        }
        return dataSet;
    }

    /**
     * Store a parameter object to the registry. If necessary, also store the
     * ParameterLevel Objects needed to successfully store the Parameter Object.
     *
     * @param parameter
     *            The Parameter Object to store.
     */
    protected void storeParameter(Parameter parameter) {
        if (centralUpdatesEnabled) {
            try {
                DataDeliveryHandlers.getParameterHandler()
                        .update(RegistryUtil.registryUser, parameter);
            } catch (RegistryHandlerException e) {
                logger.error("Failed to store parameter [" + parameter.getName()
                        + "]", e);
            }
        } else {
            logger.debug("Parameter [" + parameter.getName()
                    + "] not stored in Registry. Registry updates currently disabled.");
        }
    }

    /**
     * Get me my level types for this param
     *
     * @param cp
     * @return
     */
    public List<DataLevelType> getDataLevelTypes(Parameter cp) {
        return cp.getLevelType();
    }

    protected abstract Coverage getCoverage(String layerName);

    public Map<String, Parameter> getParameters(L layer) {
        Map<String, Parameter> rval = null;

        synchronized (parametersByLayer) {
            rval = parametersByLayer.get(layer.getName());

            if (rval == null) {
                rval = new HashMap<>();
                for (Parameter parm : agent.getLayer(layer.getName())
                        .getParameters()) {
                    // place in map
                    rval.put(parm.getName(), parm);
                }

                parametersByLayer.put(layer.getName(), rval);
            }
        }

        return rval;
    }

    protected abstract DataSet<?, ?> createDataSet(String layerName);

    protected abstract void populateDataSet(DS dataSet, L layer);

    protected abstract DSMD createDataSetMetaData(String layerName);

    protected abstract void populateDataSetMetaData(DSMD dataSetMetaData,
            DS dataSet, L layer);

    protected abstract DataType getDataType();

    public abstract Levels getLevels(DataLevelType type, String collectionName);

    public OGCAgent getAgent() {
        return agent;
    }

    public void setAgent(OGCAgent agent) {
        this.agent = agent;
    }

    public abstract Set<String> getValidLayers(R record);

    public abstract ISpatialObject getSpatial(R record);

    private void reloadDpaProperties() {
        IPathManager pm = PathManagerFactory.getPathManager();
        try {
            Properties newProps = null;
            LocalizationContext[] localSearchHierarchy = pm
                    .getLocalSearchHierarchy(LocalizationType.COMMON_STATIC);
            for (int ind = localSearchHierarchy.length - 1; ind >= 0; ind--) {
                LocalizationFile file = pm.getLocalizationFile(
                        localSearchHierarchy[ind], DPA_PROP_FILE_PATH);
                if (file.exists()) {
                    Properties properties = PropertiesUtil
                            .read(file.openInputStream());
                    if (newProps == null) {
                        newProps = properties;
                    } else {
                        newProps.putAll(properties);
                    }

                }
            }
            /*
             * Store to system properties to allow flexibility for future
             * property additions.
             */
            if (newProps != null && !newProps.isEmpty()) {
                System.getProperties().putAll(newProps);
            }
        } catch (IOException | LocalizationException e) {
            logger.error("Failed reading property file '" + DPA_PROP_FILE_PATH
                    + "' from localization", e);
        }

        centralUpdatesEnabled = Boolean.getBoolean(centralUpdatesProperty);
    }
}
