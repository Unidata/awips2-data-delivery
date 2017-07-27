package com.raytheon.uf.edex.datadelivery.retrieval.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.SessionManagedDao;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord.State;
import com.raytheon.uf.edex.datadelivery.retrieval.handlers.SubscriptionNotifyTask;

/**
 *
 * DAO for {@link RetrievalRequestRecord} entities.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 30, 2013  1543     djohnson  Add SW history.
 * Feb 07, 2013  1543     djohnson  Use session management code.
 * Feb 13, 2013  1543     djohnson  Exported interface which is now implemented.
 * Feb 22, 2013  1543     djohnson  Made public as YAJSW doesn't like Spring
 *                                  exceptions.
 * May 22, 2014  2808     dhladky   Fixed notification upon SBN delivery
 * Oct 13, 2014  3707     dhladky   Shared subscription delivery requires you to
 *                                  create a new record.
 * Oct 16, 2014  3454     bphillip  Upgrading to Hibernate 4
 * May 09, 2017  6186     rjpeter   Added url
 * Jul 25, 2017  6186     rjpeter   Removed network
 *
 * </pre>
 *
 * @author djohnson
 */
@Repository
@Transactional
// TODO: Split service functionality from DAO functionality
public class RetrievalDao
        extends SessionManagedDao<Integer, RetrievalRequestRecord>
        implements IRetrievalDao {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(RetrievalDao.class);

    private SubscriptionNotifyTask notifyTask;

    /**
     * Constructor.
     */
    public RetrievalDao() {
    }

    public SubscriptionNotifyTask getNotifyTask() {
        return notifyTask;
    }

    public void setNotifyTask(SubscriptionNotifyTask notifyTask) {
        this.notifyTask = notifyTask;
    }

    /*
     * TODO: Take into account latency so retrievals that happen often like
     * MADIS and PDA don't get stuck behind a grid task
     */
    @Override
    public synchronized RetrievalRequestRecord activateNextRetrievalRequest()
            throws DataAccessLayerException {
        Session sess = null;
        RetrievalRequestRecord rval = null;

        try {
            sess = getCurrentSession();

            // Find lowest priority item
            final String minPriHql = "select min(rec.priority) from RetrievalRequestRecord rec "
                    + "where rec.state = :statePending";

            /*
             * user lowest id, which is generally first inserted without having
             * to worry about duplicate insert time, thus preserving ordering
             */
            final String pkHql = "select min(rec.id) from RetrievalRequestRecord rec "
                    + "where rec.state = :statePending and rec.priority = :minPri";

            Query minPriQuery = sess.createQuery(minPriHql);
            setQueryState(minPriQuery, State.PENDING);

            Query pkQuery = sess.createQuery(pkHql);
            setQueryState(pkQuery, State.PENDING);

            boolean done = false;

            while (!done) {
                Object result = minPriQuery.uniqueResult();
                if (result != null) {
                    int minPri = ((Number) result).intValue();
                    pkQuery.setInteger("minPri", minPri);
                    result = pkQuery.uniqueResult();
                    if (result != null) {
                        rval = (RetrievalRequestRecord) sess.get(
                                RetrievalRequestRecord.class,
                                ((Number) result).intValue(),
                                LockOptions.UPGRADE);

                        if (rval == null
                                || !State.PENDING.equals(rval.getState())) {
                            /*
                             * another thread grabbed request while waiting for
                             * upgrade lock, redo sub query, null out rval in
                             * case it was due to state change
                             */
                            rval = null;
                            continue;
                        }
                    }
                    /*
                     * else another thread grabbed last entry for this priority,
                     * repeat loop
                     */
                } else {
                    // no Pending entries
                    done = true;
                }
            }

            if (rval != null) {
                rval.setState(State.RUNNING);
                sess.update(rval);
            }
        } catch (Exception e) {
            throw new DataAccessLayerException(
                    "Failed looking up next retrieval", e);
        }

        return rval;
    }

    @Override
    public void completeRetrievalRequest(RetrievalRequestRecord rec)
            throws DataAccessLayerException {
        try {
            // Made create or Update because SBN deliveries will not exist
            // locally
            createOrUpdate(rec);
        } catch (HibernateException e) {
            throw new DataAccessLayerException(
                    "Failed to update the database while changing the status on ["
                            + rec.getId() + "]" + " to [" + rec.getState()
                            + "]",
                    e);
        }
    }

    @Override
    public boolean resetRunningRetrievalsToPending() {
        boolean rval = false;

        try {
            String hql = "update RetrievalRequestRecord rec set rec.state = :pendState where rec.state = :runState";

            Query query = getCurrentSession().createQuery(hql);
            query.setParameter("pendState", State.PENDING);
            query.setParameter("runState", State.RUNNING);
            query.executeUpdate();
            rval = true;
        } catch (Exception e) {
            statusHandler.error(
                    "Unable to reset old RUNNING retrievals to PENDING", e);
        }

        return rval;
    }

    @Override
    public Map<State, Integer> getSubscriptionStateCounts(String subName,
            String url) throws DataAccessLayerException {
        Map<State, Integer> rval = new HashMap<>(8);

        try {
            String hql = "select rec.state, count(rec.subscriptionName) from RetrievalRequestRecord rec "
                    + "where rec.subscriptionName = :subName and rec.dsmdUrl = :url group by rec.state";
            Query query = getCurrentSession().createQuery(hql);
            query.setString("subName", subName);
            query.setString("url", url);
            List<Object> result = query.list();

            if (result != null && !result.isEmpty()) {
                for (Object row : result) {
                    if (row instanceof Object[]) {
                        Object[] cols = (Object[]) row;
                        rval.put((State) cols[0],
                                ((Number) cols[1]).intValue());
                    } else {
                        throw new DataAccessLayerException(
                                "Unhandled result from database.  Expected ["
                                        + Object[].class + "], received ["
                                        + row.getClass() + "]");
                    }
                }
            }
        } catch (Exception e) {
            throw new DataAccessLayerException(
                    "Failed check pending/running retrieval count for subscription ["
                            + subName + "]",
                    e);
        }

        return rval;
    }

    @Override
    public List<RetrievalRequestRecord> getFailedRequests(String subName,
            String url) throws DataAccessLayerException {
        try {
            Criteria query = getCurrentSession()
                    .createCriteria(RetrievalRequestRecord.class);
            query.add(Restrictions.eq("state", State.FAILED));
            query.add(Restrictions.eq("subscriptionName", subName));
            query.add(Restrictions.eq("dsmdUrl", url));
            List<RetrievalRequestRecord> rval = query.list();
            return rval;
        } catch (Exception e) {
            throw new DataAccessLayerException(
                    "Failed check pending/running retrieval count for subscription ["
                            + subName + "]",
                    e);
        }
    }

    @Override
    public boolean removeSubscription(String subName, String url)
            throws DataAccessLayerException {
        boolean rval = false;

        try {
            String hql = "delete from RetrievalRequestRecord rec "
                    + "where rec.subscriptionName = :subName and rec.dsmdUrl = :url";
            Query query = getCurrentSession().createQuery(hql);
            query.setString("subName", subName);
            query.setString("url", url);
            query.executeUpdate();
            rval = true;
        } catch (Exception e) {
            throw new DataAccessLayerException(
                    "Failed removing retrievals for subscription [" + subName
                            + "]",
                    e);
        }
        return rval;
    }

    @Override
    public List<RetrievalRequestRecord> getRequests(String subName, String url)
            throws DataAccessLayerException {
        try {
            Criteria query = getCurrentSession()
                    .createCriteria(RetrievalRequestRecord.class);
            query.add(Restrictions.eq("subscriptionName", subName));
            query.add(Restrictions.eq("dsmdUrl", url));
            List<RetrievalRequestRecord> rval = query.list();
            return rval;
        } catch (Exception e) {
            throw new DataAccessLayerException(
                    "Failed to return retrieval records for subscription ["
                            + subName + "]",
                    e);
        }
    }

    @Override
    public void create(RetrievalRequestRecord obj) {
        super.create(obj);
        notifyTask.checkNotify(obj);
    }

    @Override
    public void update(RetrievalRequestRecord obj) {
        super.update(obj);
        notifyTask.checkNotify(obj);
    }

    @Override
    public void createOrUpdate(RetrievalRequestRecord obj) {
        super.createOrUpdate(obj);
        notifyTask.checkNotify(obj);
    }

    @Override
    public void persistAll(Collection<RetrievalRequestRecord> objs) {
        super.persistAll(objs);
    }

    /**
     * @param query
     * @param state
     */
    private void setQueryState(Query query, State state) {
        query.setParameter("statePending", state);
    }

    @Override
    protected Class<RetrievalRequestRecord> getEntityClass() {
        return RetrievalRequestRecord.class;
    }

}
