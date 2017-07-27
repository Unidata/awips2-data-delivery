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

import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.util.rate.TokenBucket;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;

/**
 * Retrieves and decodes data locally
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 14, 2017 6186       rjpeter     Initial creation
 *
 * </pre>
 *
 * @author rjpeter
 */
public class SiteRetrievalTask extends RetrievalTask {

    private final StoreRetrievedData storeData;

    public SiteRetrievalTask(TokenBucket tokenBucket,
            StoreRetrievedData storeData) {
        super(tokenBucket);
        this.storeData = storeData;
    }

    @Override
    protected boolean processRetrievedData(Retrieval retrieval,
            IRetrievalResponse retrievalResponse) throws Exception {
        return storeData.processRetrievedData(retrieval, retrievalResponse);
    }

}
