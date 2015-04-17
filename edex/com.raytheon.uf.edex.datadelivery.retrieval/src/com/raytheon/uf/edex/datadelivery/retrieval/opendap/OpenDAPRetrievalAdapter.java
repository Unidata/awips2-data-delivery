package com.raytheon.uf.edex.datadelivery.retrieval.opendap;

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

import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalEvent;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalRequestBuilder;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.response.OpenDAPTranslator;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.util.OpenDAPConnectionUtil;



/**
 * OpenDAP Provider Retrieval Adapter
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 07, 2011            dhladky     Initial creation
 * Jun 28, 2012 819        djohnson    Use utility class for DConnect.
 * Jul 25, 2012 955        djohnson    Make package-private.
 * Feb 05, 2013 1580       mpduff      EventBus refactor.
 * Feb 12, 2013 1543       djohnson    The payload can just be an arbitrary object, implementations can define an array if required.
 * Sept 19, 2013 2388      dhladky     Logging for failed requests.
 * Apr 12, 2015 4400       dhladky     Upgraded to DAP2 and preserved backward compatibility.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

class OpenDAPRetrievalAdapter extends RetrievalAdapter<GriddedTime, GriddedCoverage> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(OpenDAPRetrievalAdapter.class);
    
    /** convert if older XDODS version **/
    boolean isDods = DodsUtils.isOlderXDODSVersion();

    @Override
    public OpenDAPRequestBuilder createRequestMessage(RetrievalAttribute<GriddedTime, GriddedCoverage> attXML) {

        OpenDAPRequestBuilder reqBuilder = new OpenDAPRequestBuilder(this,
                attXML);

        return reqBuilder;
    }

    @Override
    public RetrievalResponse<GriddedTime, GriddedCoverage> performRequest(
            IRetrievalRequestBuilder<GriddedTime, GriddedCoverage> request) {

        Object data = null;

        try {
            if (isDods) {
                dods.dap.DConnect connect = OpenDAPConnectionUtil
                        .getDConnectDODS(request.getRequest());
                data = connect.getData(null);
            } else {
                opendap.dap.DConnect connect = OpenDAPConnectionUtil
                        .getDConnectDAP2(request.getRequest());
                data = connect.getData(null);
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Request: " + request.getRequest()
                            + " could not be fullfilled!", e.getMessage());
            EventBus.publish(new RetrievalEvent(e.getMessage()));
        }

        RetrievalResponse<GriddedTime, GriddedCoverage> pr = new OpenDapRetrievalResponse(
                request.getAttribute());
        pr.setPayLoad(data);

        return pr;
    }

    @Override
    public Map<String, PluginDataObject[]> processResponse(
            IRetrievalResponse<GriddedTime, GriddedCoverage> response) throws TranslationException {
        Map<String, PluginDataObject[]> map = new HashMap<String, PluginDataObject[]>();

        OpenDAPTranslator translator;
        try {
            translator = getOpenDapTranslator(response.getAttribute());
        } catch (InstantiationException e) {
            throw new TranslationException(
                    "Unable to instantiate a required class!", e);
        }

        Object payload = null;
        try {
            if (response.getPayLoad() != null) {
                
                payload = response.getPayLoad();
                
                if (payload instanceof dods.dap.DataDDS) {
                    payload = dods.dap.DataDDS.class.cast(response.getPayLoad());
                } else if (payload instanceof opendap.dap.DataDDS) {
                    payload = opendap.dap.DataDDS.class.cast(response.getPayLoad());
                }
            }
        } catch (ClassCastException e) {
            throw new TranslationException(e);
        }

        if (payload != null) {
            PluginDataObject[] pdos = translator.asPluginDataObjects(payload);

            if (!CollectionUtil.isNullOrEmpty(pdos)) {
                String pluginName = pdos[0].getPluginName();
                map.put(pluginName,
                        CollectionUtil.combine(PluginDataObject.class,
                                map.get(pluginName), pdos));
            }
        }

        return map;
    }

    /**
     * @param attribute
     * @return
     */
    OpenDAPTranslator getOpenDapTranslator(RetrievalAttribute<GriddedTime, GriddedCoverage> attribute)
            throws InstantiationException {
        return new OpenDAPTranslator(attribute);
    }
}
