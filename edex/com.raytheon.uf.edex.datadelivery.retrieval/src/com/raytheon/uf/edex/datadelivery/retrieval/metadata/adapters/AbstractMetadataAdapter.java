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
package com.raytheon.uf.edex.datadelivery.retrieval.metadata.adapters;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.registry.GenericRegistry;
import com.raytheon.uf.common.util.registry.RegistryException;

/**
 *
 * Abstract class for converting RetrievalAttribute to PluginDataObjects
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Nov 19, 2012           bsteffen  Initial javadoc
 * May 12, 2013  753      dhladky   Added support for Madis
 * May 31, 2013  2038     djohnson  Plugin contributable registry.
 * Jun 11, 2013  2101     dhladky   Updated for Madis
 * Sep 20, 2014  2131     dhladky   Make service type generics better.  Need to
 *                                  work on raw types more.
 * Jan 28, 2016  5299     dhladky   Added service config retrieval method.
 * Jul 27, 2017  6186     rjpeter   Use Retrieval
 *
 * </pre>
 *
 * @author dhladky
 */
public abstract class AbstractMetadataAdapter<RecordKey, T extends Time, C extends Coverage> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractMetadataAdapter.class);

    protected boolean isPointData = false;

    protected PluginDataObject[] pdos;

    protected static Map<String, String[]> parameterMap = new HashMap<>();

    protected static ServiceConfig serviceConfig;

    @SuppressWarnings("rawtypes")
    // TODO Figure a way to not have to do raw types with
    // AbstractMetadataAdapter
    protected static GenericRegistry<String, Class<AbstractMetadataAdapter>> metadataAdapterRegistry = new GenericRegistry<String, Class<AbstractMetadataAdapter>>() {

        @Override
        public Object register(String t, Class<AbstractMetadataAdapter> s)
                throws RegistryException {

            Validate.notNull(t);
            Validate.notNull(s);

            statusHandler.info("Registered metadata adapter [" + s.getName()
                    + "] for class [" + t + "]");

            return super.register(t, s);
        }

    };

    @SuppressWarnings({ "unchecked", "rawtypes" })
    // TODO Figure a way to not have to do raw types with RecordKey, T, C
    public static <RecordKey, T, C> AbstractMetadataAdapter<RecordKey, ?, ?> getMetadataAdapter(
            Class<?> clazz, Retrieval retrieval) throws InstantiationException {

        final String className = clazz.getName();
        Class<AbstractMetadataAdapter> adapterClass = metadataAdapterRegistry
                .getRegisteredObject(className);

        if (adapterClass == null) {
            throw new IllegalArgumentException(
                    "There is no metadata adapter registered for class ["
                            + className + "]");
        }

        try {
            // Must return a new instance every time, because none of the
            // metadata adapters are thread safe
            AbstractMetadataAdapter<RecordKey, ?, ?> adapter = adapterClass
                    .newInstance();
            adapter.processRetrieval(retrieval);
            return adapter;
        } catch (IllegalAccessException e) {
            final InstantiationException instantiationException = new InstantiationException();
            instantiationException.initCause(e);
            throw instantiationException;
        }
    }

    /**
     * @param attXML
     *
     * @throws InstantiationException
     */
    public abstract void processRetrieval(Retrieval<T, C> retrieval)
            throws InstantiationException;

    // setup an individual record from the direct plugin translation
    public abstract PluginDataObject getRecord(RecordKey o);

    // set the size of the PDO array to return
    public abstract void allocatePdoArray(int size);

    /**
     * get the PDO list
     *
     * @return
     */
    public PluginDataObject[] getPdos() {
        return pdos;
    }

    /**
     * @return the metadataAdapterRegistry
     */
    @SuppressWarnings("rawtypes")
    // TODO Figure a way to not have to do raw types with
    // AbstractMetadataAdapter
    public static GenericRegistry<String, Class<AbstractMetadataAdapter>> getMetadataAdapterRegistry() {
        return metadataAdapterRegistry;
    }

    /**
     * Sets whether this adapter is pointData or not
     *
     * @param isPointData
     */
    public void setIsPointData(boolean isPointData) {
        this.isPointData = isPointData;
    }

    public boolean isPointData() {
        return isPointData;
    }

    /**
     * Get the instance of the service config
     *
     * @return
     */
    protected synchronized ServiceConfig getServiceConfig(
            ServiceType serviceType) {

        if (serviceConfig == null) {
            serviceConfig = HarvesterServiceManager.getInstance()
                    .getServiceConfig(serviceType);
        }

        return serviceConfig;
    }

}
