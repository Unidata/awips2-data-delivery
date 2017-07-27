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
package com.raytheon.uf.edex.datadelivery.retrieval.db;

import java.util.List;
import java.util.Map;

import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.ISessionManagedDao;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord.State;
import com.raytheon.uf.edex.datadelivery.retrieval.handlers.SubscriptionNotifyTask;

/**
 * DAO interface for retrievals.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------
 * Feb 13, 2013  1543     djohnson  Generated from RetrievalDao.
 * May 09, 2017  6186     rjpeter   Added url
 *
 * </pre>
 *
 * @author djohnson
 */
public interface IRetrievalDao
        extends ISessionManagedDao<Integer, RetrievalRequestRecord> {

    /**
     * Returns the next PENDING retrieval request, puts it into a RUNNING state,
     * based on current time.
     *
     * @param network
     *            the network to constrain requests to
     *
     * @return
     */
    RetrievalRequestRecord activateNextRetrievalRequest()
            throws DataAccessLayerException;

    void completeRetrievalRequest(RetrievalRequestRecord rec)
            throws DataAccessLayerException;

    /**
     * TODO: This will fail in a cluster, need to limit by machine in a cluster
     *
     * @return
     */
    boolean resetRunningRetrievalsToPending();

    /**
     * Returns the state counts for the passed subscription and url.
     *
     * @param subName
     * @param url
     * @return
     */
    Map<State, Integer> getSubscriptionStateCounts(String subName, String url)
            throws DataAccessLayerException;

    /**
     * Returns any failed request for the given subscription and url.
     *
     * @param subName
     * @param url
     * @return
     * @throws DataAccessLayerException
     */
    List<RetrievalRequestRecord> getFailedRequests(String subName, String url)
            throws DataAccessLayerException;

    /**
     * Removes all RetrievalRequestRecord entries for the given subscription and
     * url.
     *
     * @param subName
     * @param url
     * @return
     * @throws DataAccessLayerException
     */
    boolean removeSubscription(String subName, String url)
            throws DataAccessLayerException;

    /**
     * Get all requests for the subscription name and url.
     *
     * @param subName
     * @param url
     * @return
     */
    List<RetrievalRequestRecord> getRequests(String subName, String url)
            throws DataAccessLayerException;

    SubscriptionNotifyTask getNotifyTask();

    void setNotifyTask(SubscriptionNotifyTask notifyTask);
}