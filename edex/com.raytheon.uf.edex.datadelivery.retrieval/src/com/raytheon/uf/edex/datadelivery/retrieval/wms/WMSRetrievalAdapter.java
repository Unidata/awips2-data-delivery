package com.raytheon.uf.edex.datadelivery.retrieval.wms;

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
 * WMS Provider Retrieval Adapter
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 07, 2011           dhladky   Initial creation
 * Jul 25, 2012  955      djohnson  Moved to wms specific package.
 * May 22, 2017  6130     tjensen   Add RetrievalRequestRecord to
 *                                  processResponse
 *
 * </pre>
 *
 * @author dhladky
 * @version 1.0
 */

class WMSRetrievalAdapter<T extends Time, C extends Coverage>
        extends RetrievalAdapter<T, C> {

    WMSRetrievalAdapter() {

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
