package com.raytheon.uf.edex.datadelivery.retrieval.handlers;

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

import com.raytheon.uf.edex.datadelivery.retrieval.response.AsyncRetrievalResponse;

/**
 * AsyncRetrievalBroker Takes care of situation where PDA is to quick with
 * response.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 05, 2016 5424       dhladky     Initial creation
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class AsyncRetrievalBroker {

    /** Singleton instance of this class */
    private static AsyncRetrievalBroker instance = new AsyncRetrievalBroker();

    /** map to hold async retrievals waiting to be delivered **/
    private Map<String, AsyncRetrievalResponse> retrievals = new HashMap<String, AsyncRetrievalResponse>();

    /* Private Constructor */
    private AsyncRetrievalBroker() {

    }

    /**
     * Get an instance of this singleton.
     * 
     * @return Instance of this class
     */
    public static AsyncRetrievalBroker getInstance() {
        return instance;
    }

    /**
     * Add an async retrieval to the broker.
     * 
     * @param retrievalId
     * @param ars
     * 
     */
    public void addRetrieval(String retrievalId, AsyncRetrievalResponse ars) {

        synchronized (retrievals) {
            retrievals.put(retrievalId, ars);
        }
    }

    /**
     * Retrieve an async retrieval from the broker.
     * 
     * @param retrievalId
     * @return
     */
    public AsyncRetrievalResponse getRetrieval(String retrievalId) {
        synchronized (retrievals) {
            return retrievals.remove(retrievalId);
        }
    }
    
    /**
     * remove unneeded entries.
     * @param retrievalId
     */
    public void remove(String retrievalId) {
        synchronized (retrievals) {
            retrievals.remove(retrievalId);
        }
    }

}
