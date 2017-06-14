package com.raytheon.uf.edex.datadelivery.retrieval.interfaces;

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
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.util.rate.TokenBucket;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;

/**
 * Interface for Provider Retrieval Adapter
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 07, 2011            dhladky     Initial creation
 * May 22, 2017  6130      tjensen     Add RetrievalRequestRecord to processResponse
 * Jun 05, 2017  6222      tgurney     Add token bucket and priority
 *
 * </pre>
 *
 * @author dhladky
 */

public interface IRetrievalAdapter<T extends Time, C extends Coverage> {

    public IRetrievalRequestBuilder<T, C> createRequestMessage(
            RetrievalAttribute<T, C> prxml);

    public com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalResponse<T, C> performRequest(
            IRetrievalRequestBuilder<T, C> requestBuilder);

    public Map<String, PluginDataObject[]> processResponse(
            IRetrievalResponse<T, C> response,
            RetrievalRequestRecord requestRecord) throws Exception;

    public void setProviderRetrievalXML(Retrieval prxml);

    public Retrieval getProviderRetrievalXMl();

    public void setTokenBucket(TokenBucket tokenBucket);

    public TokenBucket getTokenBucket();

    public void setPriority(int priority);

    public int getPriority();

}
