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
package com.raytheon.uf.edex.datadelivery.retrieval.handlers;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter.TranslationException;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.ServiceTypeFactory;
import com.raytheon.uf.edex.datadelivery.retrieval.util.RetrievalPersistUtil;

/**
 * Implementation of {@link IRetrievedDataProcessor} that stores the plugin data
 * objects to the database.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 31, 2013  1543     djohnson  Initial creation
 * Feb 12, 2013  1543     djohnson  Now handles the retrieval responses
 *                                  directly.
 * Feb 15, 2013  1543     djohnson  Retrieve the retrieval attributes from the
 *                                  database.
 * Aug 09, 2013  1822     bgonzale  Added parameters to
 *                                  processRetrievedPluginDataObjects.
 * Aug 06, 2013  1654     bgonzale  Added AdhocDataRetrievalEvent.
 * Oct 01, 2013  2267     bgonzale  Removed request parameter.  Return
 *                                  RetrievalRequestRecord.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * May 22, 2017  6130     tjensen   Add RetrievalRequestRecord to
 *                                  processResponse call.
 * Jul 27, 2017  6186     rjpeter   Utilize Retrieval.
 *
 * </pre>
 *
 * @author djohnson
 */

public class StoreRetrievedData {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private final String generalDestinationUri;

    /**
     * Constructor.
     *
     * @param generalDestinationUri
     *            the destination uri most plugin data will travel through
     */
    public StoreRetrievedData(String generalDestinationUri) {
        this.generalDestinationUri = generalDestinationUri;
    }

    /**
     * Processes retrieved data into plugin data objects and stores them.
     *
     * @param retrieval
     * @param retrievalResponse
     * @throws SerializationException
     *             on error with serialization
     * @throws TranslationException
     */
    @SuppressWarnings("rawtypes")
    public boolean processRetrievedData(Retrieval retrieval,
            IRetrievalResponse retrievalResponse)
            throws SerializationException, TranslationException {
        Map<String, PluginDataObject[]> pluginDataObjects = Maps.newHashMap();
        final ServiceType serviceType = retrieval.getServiceType();
        final RetrievalAdapter serviceRetrievalAdapter = ServiceTypeFactory
                .retrieveServiceRetrievalAdapter(serviceType);

        // currently map will only have one key, the plugin
        Map<String, PluginDataObject[]> value = serviceRetrievalAdapter
                .processResponse(retrieval, retrievalResponse);

        if (value == null || value.isEmpty()) {
            return value != null;
        }

        for (Entry<String, PluginDataObject[]> entry : value.entrySet()) {
            final String key = entry.getKey();
            final PluginDataObject[] objectsForEntry = entry.getValue();

            PluginDataObject[] objectsForPlugin = pluginDataObjects.get(key);
            objectsForPlugin = CollectionUtil.combine(PluginDataObject.class,
                    objectsForPlugin, objectsForEntry);

            pluginDataObjects.put(key, objectsForPlugin);
        }

        boolean rval = true;
        for (Entry<String, PluginDataObject[]> entry : pluginDataObjects
                .entrySet()) {
            final String pluginName = entry.getKey();
            final PluginDataObject[] records = entry.getValue();

            if (records == null) {
                statusHandler
                        .warn("The plugin data objects was a null array, the service retrieval adapter "
                                + "should not return a null map of plugin data objects!");
                rval = false;
                continue;
            }

            statusHandler.info("Successfully processed: " + records.length
                    + " : " + serviceType + " Plugin : " + pluginName);

            sendToDestinationForStorage(records);
        }

        return rval;
    }

    /**
     * Sends the plugin data objects to their configured destination for storage
     * to the database.
     */
    private void sendToDestinationForStorage(PluginDataObject[] pdos) {
        String pluginName = pdos[0].getPluginName();

        if (pluginName != null) {
            RetrievalPersistUtil.routePlugin(generalDestinationUri, pluginName,
                    pdos);
        }

    }
}
