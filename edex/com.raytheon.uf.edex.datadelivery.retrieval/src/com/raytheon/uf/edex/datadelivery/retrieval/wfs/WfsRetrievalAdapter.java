package com.raytheon.uf.edex.datadelivery.retrieval.wfs;

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

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.PointTime;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalRequestBuilder;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;

/**
 * WFS OGC Provider Retrieval Adapter
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 07, 2011           dhladky   Initial creation
 * Jul 25, 2012  955      djohnson  Moved to wfs specific package.
 * May 12, 2013  753      dhladky   implemented.
 * May 31, 2013  2038     djohnson  Move to correct repo.
 * Jun 03, 2013  1763     dhladky   Readied for retrievals.
 * Jun 18, 2013  2120     dhladky   Removed provider and fixed time processing.
 * May 22, 2017  6130     tjensen   Add RetrievalRequestRecord to
 *                                  processResponse
 * Jun 06, 2017  6222     tgurney   Use token bucket to rate-limit requests
 * Jun 23, 2017  6322     tgurney   performRequest() throws Exception
 * Jul 25, 2017  6186     rjpeter   Use Retrieval
 *
 * </pre>
 *
 * @author dhladky
 */

public class WfsRetrievalAdapter extends RetrievalAdapter<PointTime, Coverage> {

    WfsRetrievalAdapter() {
    }

    @Override
    public IRetrievalRequestBuilder<PointTime, Coverage> createRequestMessage(
            Retrieval<PointTime, Coverage> retrieval) {

        WfsRequestBuilder<PointTime, Coverage> reqBuilder = new WfsRequestBuilder<>(
                this, retrieval);

        return reqBuilder;
    }

    @Override
    public Map<String, PluginDataObject[]> processResponse(
            Retrieval<PointTime, Coverage> retrieval,
            IRetrievalResponse response) throws TranslationException {

        Map<String, PluginDataObject[]> map = new HashMap<>();
        WfsTranslator translator;
        try {
            translator = getWfsTranslator(retrieval);
        } catch (InstantiationException e) {
            throw new TranslationException(
                    "Unable to instantiate a required class!", e);
        }

        String payload = ((WFSRetrievalResponse) response).getPayLoad();

        try {
            if (payload != null) {
                PluginDataObject[] pdos = translator
                        .asPluginDataObjects(payload);

                if (!CollectionUtil.isNullOrEmpty(pdos)) {
                    String pluginName = pdos[0].getPluginName();
                    map.put(pluginName, pdos);
                }
            }
        } catch (ClassCastException e) {
            throw new TranslationException(e);
        }

        return map;

    }

    @Override
    public WFSRetrievalResponse performRequest(
            Retrieval<PointTime, Coverage> retrieval,
            IRetrievalRequestBuilder<PointTime, Coverage> request)
            throws Exception {
        // This is used as the "Realm" in HTTPS connections
        String providerName = retrieval.getProvider();
        String xmlMessage = WFSConnectionUtil.wfsConnect(request.getRequest(),
                retrieval.getUrl(), providerName, getTokenBucket(),
                getPriority());
        WFSRetrievalResponse rval = null;

        if (xmlMessage != null && !xmlMessage.isEmpty()) {
            rval = new WFSRetrievalResponse();
            rval.setPayLoad(xmlMessage);
            generateRetrievalEvent(retrieval, xmlMessage.length());
        }

        return rval;
    }

    /**
     * @param retrieval
     * @return
     */
    WfsTranslator getWfsTranslator(Retrieval<PointTime, Coverage> retrieval)
            throws InstantiationException {
        return new WfsTranslator(retrieval);
    }

}
