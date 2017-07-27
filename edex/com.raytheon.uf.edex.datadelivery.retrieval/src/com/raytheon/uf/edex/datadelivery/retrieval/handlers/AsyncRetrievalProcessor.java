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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.db.IRetrievalDao;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord.State;
import com.raytheon.uf.edex.datadelivery.retrieval.response.AsyncRetrievalResponse;

/**
 * AsyncRetrievalProcessor
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * May 05, 2016  5424     dhladky   Initial creation
 * May 11, 2017  6186     rjpeter   RetrievalRequestRecordPK handling.
 * Jul 27, 2017  6186     rjpeter   Removed AsyncBroker, record always stored
 *                                  before request sent to provider.
 *
 * </pre>
 *
 * @author dhladky
 */

public class AsyncRetrievalProcessor {
    /** retrieval DAO **/
    private final IRetrievalDao retrievalDao;

    /** status handler **/
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Spring constructor
     *
     * @param destinationUri
     * @param retrievalDao
     */
    public AsyncRetrievalProcessor(IRetrievalDao retrievalDao) {
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
            logger.error("Couldn't deserialize AsyncRetrievalResponse class! ",
                    e);
            return;
        }

        String pk = ars.getRequestId();

        try {
            RetrievalRequestRecord rrr = retrievalDao
                    .getById(Integer.parseInt(pk));

            if (rrr != null) {
                Retrieval retrieval = rrr.getRetrievalObj();
                String file = ars.getFileName();

                if (!State.WAITING_RESPONSE.equals(rrr.getState())) {
                    logger.warn("Ignoring ASYNC response for retrieval id ["
                            + retrieval
                            + "] retrieval not in WAITING_RESPONSE state ["
                            + rrr.getState() + "] response ["
                            + ars.getFileName() + "]");
                } else if (file != null && !file.isEmpty()) {
                    logger.info("Received async response for retrieval ["
                            + retrieval + "] response [" + file + "]");
                    retrieval.setUrl(ars.getFileName());
                    rrr.setState(State.PENDING);
                    rrr.setRetrievalObj(retrieval);
                } else {
                    logger.error(
                            "Received emptry async response for retrieval ["
                                    + retrieval + "] response [" + file + "]");
                    // TODO: Also needs to fire a SubscriptionNotifyTask
                    rrr.setState(State.FAILED);
                }

                // update for posterity
                retrievalDao.update(rrr);
            } else {
                logger.error(
                        "No RetrievalRequestRecord found for async response id: "
                                + pk);
            }

        } catch (Exception e) {
            logger.error("Couldn't process AsyncRetrievalResponse! ID: " + pk,
                    e);
        }
    }

}
