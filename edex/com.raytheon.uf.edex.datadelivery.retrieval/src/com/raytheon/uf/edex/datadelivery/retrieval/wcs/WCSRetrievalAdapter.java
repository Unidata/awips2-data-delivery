package com.raytheon.uf.edex.datadelivery.retrieval.wcs;

import java.util.Map;

import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalRequestBuilder;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalResponse;

/**
 * WCS OGC Provider Retrieval Adapter
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 07, 2011           dhladky   Initial creation
 * Jul 25, 2012  955      djohnson  Make package-private.
 * May 22, 2017  6130     tjensen   Add RetrievalRequestRecord to
 *                                  processResponse
 *
 * </pre>
 *
 * @author dhladky
 * @version 1.0
 */
class WCSRetrievalAdapter
        extends RetrievalAdapter<GriddedTime, GriddedCoverage> {

    @Override
    public IRetrievalRequestBuilder<GriddedTime, GriddedCoverage> createRequestMessage(
            RetrievalAttribute<GriddedTime, GriddedCoverage> prxml) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, PluginDataObject[]> processResponse(
            IRetrievalResponse<GriddedTime, GriddedCoverage> response,
            RetrievalRequestRecord requestRecord) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RetrievalResponse<GriddedTime, GriddedCoverage> performRequest(
            IRetrievalRequestBuilder<GriddedTime, GriddedCoverage> request) {
        // TODO Auto-generated method stub
        return null;
    }

}
