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

import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.retrieval.db.IRetrievalDao;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecordPK;
import com.raytheon.uf.edex.datadelivery.retrieval.response.AsyncRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.util.RetrievalGeneratorUtilities;

/**
 * AsyncRetrievalProcessor
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 05, 2016  5424     dhladky   Initial creation
 * May 11, 2017  6186     rjpeter   RetrievalRequestRecordPK handling.
 *
 * </pre>
 *
 * @author dhladky
 */

public class AsyncRetrievalProcessor {

    /** Retrieval queue endpoint */
    private final String destinationUri;

    /** retrieval DAO **/
    private final IRetrievalDao retrievalDao;

    /** async retrieval broker **/
    private AsyncRetrievalBroker broker = AsyncRetrievalBroker.getInstance();

    /** status handler **/
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(AsyncRetrievalProcessor.class);

    /**
     * Spring constructor
     *
     * @param destinationUri
     * @param retrievalDao
     */
    public AsyncRetrievalProcessor(String destinationUri,
            IRetrievalDao retrievalDao) {
        this.destinationUri = destinationUri;
        this.retrievalDao = retrievalDao;
    }

    /**
     * Process the retrieval
     *
     * @param retrievalBytes
     */
    public void processRetrieval(byte[] retrievalBytes) {

        AsyncRetrievalResponse ars = null;

        try {
            ars = SerializationUtil.transformFromThrift(
                    AsyncRetrievalResponse.class, retrievalBytes);
        } catch (Exception e) {
            statusHandler
                    .error("Couldn't deserialize RetrievalResponse class! ", e);
        }

        if (ars != null) {
            RetrievalRequestRecordPK pk = ars.getRequestId();

            /**
             * Three scenarios can happen here.
             *
             * 1.) The RetrievalRequestRecord hasn't been persisted yet and the
             * async request returned too early.
             *
             * 2.) The AsyncRetrievalResponse has been re-queued by the
             * SubscriptionRetrievalAgent after persisting the
             * RetrievalRequestRecord, it's ready now.
             *
             * 3.) The SubscriptionRetrievalAgent wins the Race condition and
             * persists the RetrievalRequestRecord before the
             * AsyncRetrievalResponse gets here.
             *
             * broker.addRetrieval MUST occur before retrievalDao to avoid race
             * conditions.
             */
            boolean isRemove = true;

            try {

                broker.addRetrieval(pk.toString(), ars);
                RetrievalRequestRecord rrr = retrievalDao.getById(pk);

                /*
                 * Scenario 2 & 3. Record has been created, process the ARS.
                 */
                if (rrr != null) {

                    Retrieval retrieval = rrr.getRetrievalObj();
                    retrieval.getConnection().setUrl(ars.getFileName());
                    rrr.setRetrievalObj(retrieval);

                    // update for posterity
                    retrievalDao.update(rrr);
                    // send to retrieval queue
                    RetrievalGeneratorUtilities.sendToRetrieval(destinationUri,
                            rrr.getNetwork(), new Object[] { pk });
                    statusHandler
                            .info("Sent async retrieval to Retrieval Queue. "
                                    + rrr.getNetwork().toString());
                }
                /**
                 * Scenario 1 Must wait for the SubscriptionRetrievalAgent to
                 * re-queue the ARS.
                 */
                else {
                    statusHandler.debug(
                            "Async retrieval arrived too soon, waiting. " + pk);
                    isRemove = false;
                }

            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Couldn't process AsyncRetrievalResponse! ID: " + pk,
                        e);
            } finally {
                // Remove the record in the broker.
                if (isRemove) {
                    broker.remove(pk.toString());
                }
            }
        }
    }

}
