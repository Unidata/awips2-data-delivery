package com.raytheon.uf.edex.datadelivery.retrieval.metadata;

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

import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.DataSetName;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.ebxml.DataSetQuery;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataSetHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataSetMetaDataHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.ParameterHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.ProviderHandler;
import com.raytheon.uf.common.datadelivery.retrieval.util.LookupManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetInformation;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.registry.ebxml.RegistryUtil;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IParseMetaData;

/**
 * Parse MetaData.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 20, 2011 218        dhladky      Initial creation
 * May 15, 2012 455        jspinks      Modified for storing object associations in registry.
 * Jun 21, 2012 736        djohnson     Change OPERATION_STATUS to OperationStatus.
 * Aug 02, 2012 955        djohnson     Type-safe registry query/responses.
 * Aug 10, 2012 1022       djohnson     {@link DataSetQuery} requires provider name.
 * Aug 20, 2012 0743       djohnson     Finish making registry type-safe.
 * Sep 14, 2012 1169       djohnson     Use storeOrReplaceRegistryObject.
 * Oct 03, 2012 1241       djohnson     Use registry handler, move unresolved reference handling into handlers themselves.
 * Nov 19, 2012 1166       djohnson     Clean up JAXB representation of registry objects.
 * Sept 30, 2013 1797      dhladky      Generics
 * Mar 31, 2014 2889       dhladky      Added username for notification center tracking.
 * Jun 21, 2014 3120       dhladky      Added more helper methods
 * Jan 20, 2016 5280       dhladky      Increase efficiency of object replication.
 * Feb 16, 2016 5365       dhladky      Refined interface for transaction, getRecords() exclusions.
 * Apr 05, 2016 5424       dhladky      Fixed initial condition for dataset which prevented storage.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public abstract class MetaDataParser<O extends Object> implements
        IParseMetaData<O> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(MetaDataParser.class);

    protected ServiceConfig serviceConfig;

    public MetaDataParser() {

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
            DataSet currentDataSet = DataDeliveryHandlers.getDataSetHandler()
                    .getByNameAndProvider(dataSet.getDataSetName(),
                            dataSet.getProviderName());
            if (currentDataSet != null) {
                if (!currentDataSet.equals(dataSet)) {
                    dataSet.combine(currentDataSet);
                }
            } else {
                // If no current exists, return null and store the original.
                return null;
            }
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to retrieve dataset.", e);
        }
        return dataSet;
    }

    /**
     * @param dataSet
     */
    @SuppressWarnings("rawtypes")
    protected void storeDataSet(final DataSet dataSet) {

        String dataSetName = null;

        try {

            boolean store = false;
            // This returns null if no previous dataSet exists.
            DataSet dataSetToStore = getDataSetToStore(dataSet);

            if (dataSetToStore == null) {
                store = true;
                dataSetToStore = dataSet;
            } else {
                if (!dataSetToStore.equals(dataSet)) {
                    store = true;
                }
            }

            if (store) {
                dataSetName = dataSetToStore.getDataSetName();
                DataSetHandler handler = DataDeliveryHandlers.getDataSetHandler();
                handler.update(RegistryUtil.registryUser, dataSetToStore);
                statusHandler.info("Dataset [" + dataSetName
                        + "] successfully stored in Registry");
                storeDataSetName(dataSetToStore);
            }

        } catch (RegistryHandlerException e) {
            statusHandler.info("Dataset [" + dataSetName
                    + "] failed to store in Registry");
        }
    }

    /**
     * Store Data objects
     * 
     * @param DataSetMetaData
     *            <?>
     */
    public void storeMetaData(final DataSetMetaData<?> metaData) {

        DataSetMetaDataHandler handler = DataDeliveryHandlers
                .getDataSetMetaDataHandler();
        final String description = metaData.getDataSetDescription();
        DataSetMetaData<?> currentMetaData = null;
        boolean store = false;

        try {
            // "url" is ID for metaData
            currentMetaData = handler.getById(metaData.getUrl());
        } catch (RegistryHandlerException e1) {
            statusHandler
                    .handle(Priority.PROBLEM, "Unable to lookup metadata ID: "+metaData.getUrl(), e1);
        }

        if (currentMetaData != null) {
            if (!currentMetaData.equals(metaData)) {
                store = true;
            }
        } else {
            store = true;
        }

        if (store) {
            try {
                handler.update(RegistryUtil.registryUser, metaData);
                statusHandler.info("DataSetMetaData [" + description
                        + "] successfully stored in Registry");
            } catch (RegistryHandlerException e) {
                statusHandler.handle(Priority.PROBLEM, "DataSetMetaData ["
                        + description + "] failed to store in Registry");

            }
        }
    }
    /**
     * Stores the name of the dataset, used in lookups.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void storeDataSetName(DataSet dataSetToStore) {

        DataSetName dsn = new DataSetName();
        // Set the RegistryObject Id keys for this Object
        // using the values from the DataSetMetaData Object.
        dsn.setProviderName(dataSetToStore.getProviderName());
        dsn.setDataSetType(dataSetToStore.getDataSetType());
        dsn.setDataSetName(dataSetToStore.getDataSetName());

        // Now add the parameter Objects so we can associate
        // the DataSetName with parameters..
        dsn.setParameters(dataSetToStore.getParameters());
 
        try {
            DataDeliveryHandlers.getDataSetNameHandler().update(
                    RegistryUtil.registryUser, dsn);
            statusHandler.info("DataSetName object store complete, dataset ["
                    + dsn.getDataSetName() + "]");
        } catch (RegistryHandlerException e) {
            statusHandler.error("DataSetName object store failed:", e);
        }
    }

    /**
     * Store the DataSetMetaData Object to the registry.
     * 
     * @param metaDatas
     *            The DataSetMetaData Object to store.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void storeMetaData(final List<DataSetMetaData<?>> metaDatas,
            final DataSet dataSet) {

        DataSetMetaDataHandler handler = DataDeliveryHandlers
                .getDataSetMetaDataHandler();
        Iterator<DataSetMetaData<?>> iter = metaDatas.iterator();
        
        while (iter.hasNext()) {

            final DataSetMetaData<?> dsmd = iter.next();
            DataSetMetaData<?> currentMetaData = null;
            final String url = dsmd.getUrl();
            boolean store = false;

            try {
                // "url" is ID for metaData
                currentMetaData = handler.getById(dsmd.getUrl());
            } catch (RegistryHandlerException e1) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to lookup metadata ID: " + dsmd.getUrl(), e1);
            }

            if (currentMetaData != null) {
                if (!currentMetaData.equals(dsmd)) {
                    store = true;
                }
            } else {
                store = true;
            }

            if (store) {
                try {
                    handler.update(RegistryUtil.registryUser, dsmd);
                    statusHandler.info("DataSetMetaData [" + url
                            + "] successfully stored in Registry");
                } catch (RegistryHandlerException e) {
                    statusHandler.handle(Priority.PROBLEM, "DataSetMetaData ["
                            + url + "] failed to store in Registry");

                }
            }
        }
    }

    /**
     * Make sure our provider is contained in the Registry
     * 
     * @param provider
     */
    public void storeProvider(final Provider provider) {

        try {

            ProviderHandler handler = DataDeliveryHandlers.getProviderHandler();

            Provider currentProvider = handler.getByName(provider.getName());
            boolean store = false;

            if (currentProvider != null) {
                if (!currentProvider.equals(provider)) {
                    store = true;
                }
            } else {
                store = true;
            }

            if (store) {
                handler.update(RegistryUtil.registryUser, provider);
                statusHandler.info("Provider [" + provider.getName()
                        + "] successfully stored in Registry");
            }
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Provider [" + provider.getName()
                            + "] failed to store in Registry");
        }
    }

    /**
     * Store a parameter object to the registry. If necessary, also store the
     * ParameterLevel Objects needed to successfully store the Parameter Object.
     * 
     * @param parameter
     *            The Parameter Object to store.
     */
    public void storeParameter(Parameter parameter) {

        try {

            ParameterHandler handler = DataDeliveryHandlers
                    .getParameterHandler();
            Parameter currentParameter = handler.getByName(parameter.getName());
            boolean store = false;

            if (currentParameter != null) {
                if (!currentParameter.equals(parameter)) {
                    store = true;
                }
            } else {
                store = true;
            }

            if (store) {
                handler.update(RegistryUtil.registryUser, parameter);
                statusHandler.info("Parameter [" + parameter.getName()
                        + "] successfully stored in Registry");
            }

        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Failed to store parameter [" + parameter.getName() + "]");
        }
    }

    /**
     * Gets the availability offset for this data set
     * 
     * @param dataSetName
     * @param startMillis
     * @return
     */
    public int getDataSetAvailabilityTime(String dataSetName, long startMillis) {

        // Calculate dataset availability delay
        DataSetInformation dsi = LookupManager.getInstance()
                .getDataSetInformation(dataSetName);
        long offset = 0l;
        long endMillis = TimeUtil.newGmtCalendar().getTimeInMillis();

        /**
         * This is here for if no one has configured this particular model They
         * are gross defaults and will not guarantee this model working
         */
        if (dsi == null) {
            Double multi = Double.parseDouble(serviceConfig
                    .getConstantValue("DEFAULT_MULTIPLIER"));
            Integer runIncrement = Integer.parseInt(serviceConfig
                    .getConstantValue("DEFAULT_RUN_INCREMENT"));
            Integer defaultOffest = Integer.parseInt(serviceConfig
                    .getConstantValue("DEFAULT_OFFSET"));
            dsi = new DataSetInformation(dataSetName, multi, runIncrement,
                    defaultOffest);
            // writes out a place holder DataSetInformation object in the
            // file
            LookupManager.getInstance().modifyDataSetInformationLookup(dsi);
        }

        offset = (endMillis - startMillis) / TimeUtil.MILLIS_PER_MINUTE;
        // this is the actually ranging check
        if (dsi.getRange() < offset) {
            offset = dsi.getDefaultOffset();
        }

        return (int) offset;
    }

}
