package com.raytheon.uf.edex.datadelivery.retrieval.wxxm;

import java.util.Map;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalRequestBuilder;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalResponse;

/**
 * WXXM Provider Retrieval Adapter
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 07, 2011           dhladky   Initial creation
 * Jul 25, 2012  955      djohnson  Moved to wxxm specific package.
 * May 22, 2017  6130     tjensen   Add RetrievalRequestRecord to
 *                                  processResponse
 *
 * </pre>
 *
 * @author dhladky
 * @version 1.0
 */

class WXXMRetrievalAdapter<T extends Time, C extends Coverage>
        extends RetrievalAdapter<T, C> {

    WXXMRetrievalAdapter() {

    }

    @Override
    public IRetrievalRequestBuilder<T, C> createRequestMessage(
            RetrievalAttribute<T, C> prxml) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, PluginDataObject[]> processResponse(
            IRetrievalResponse<T, C> response,
            RetrievalRequestRecord requestRecord) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RetrievalResponse<T, C> performRequest(
            IRetrievalRequestBuilder<T, C> request) {
        // TODO Auto-generated method stub
        return null;
    }

}
