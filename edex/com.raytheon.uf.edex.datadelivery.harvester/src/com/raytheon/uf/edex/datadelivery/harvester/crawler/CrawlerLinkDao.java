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
package com.raytheon.uf.edex.datadelivery.harvester.crawler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.Session;

import com.raytheon.uf.common.dataplugin.persist.IPersistableDataObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;
import com.raytheon.uf.edex.database.query.DatabaseQuery;

/**
 *
 * DAO for {@link CrawlerLink} entities.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 7, 2017   6178     tgurney   Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */
public class CrawlerLinkDao extends CoreDao {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(CrawlerLinkDao.class);

    public CrawlerLinkDao() {
        super(DaoConfig.forClass(CrawlerLink.class));
    }

    /** @return List of links for the oldest unprocessed provider/collection */
    public synchronized List<CrawlerLink> getLinks() {
        List<CrawlerLink> rval = Collections.emptyList();
        List<Object[]> nextProviderCollection = null;
        DatabaseQuery q = new DatabaseQuery(CrawlerLink.class);
        q.addReturnedField("providerName");
        q.addReturnedField("collectionName");
        q.addReturnedField("creationTime");
        q.addQueryParam("processed", false);
        q.addOrder("creationTime", true);
        q.setMaxResults(1);
        Session session = null;
        try {
            nextProviderCollection = (List<Object[]>) queryByCriteria(q);
            if (nextProviderCollection != null
                    && !nextProviderCollection.isEmpty()) {
                String providerName = nextProviderCollection.get(0)[0]
                        .toString();
                String collectionName = nextProviderCollection.get(0)[1]
                        .toString();
                session = getSessionFactory().openSession();
                List<CrawlerLink> links = session
                        .createQuery("from CrawlerLink"
                                + " where providerName = :providerName"
                                + " and collectionName = :collectionName"
                                + " and processed = false")
                        .setParameter("providerName", providerName)
                        .setParameter("collectionName", collectionName)
                        .setMaxResults(1000).list();
                if (links != null && !links.isEmpty()) {
                    rval = links;
                }
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Failed to query database for links", e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return rval;
    }

    /**
     * Delete links for the specified provider
     *
     * @param providerName
     *
     * @param daysToKeepLinks
     *            Links must be at least this many days old to delete
     */
    public void removeOldLinks(String providerName, int daysToKeepLinks) {
        DatabaseQuery q;
        long purgeTime = System.currentTimeMillis()
                - daysToKeepLinks * TimeUtil.MILLIS_PER_DAY;
        q = new DatabaseQuery(CrawlerLink.class);
        q.addQueryParam("providerName", providerName);
        q.addQueryParam("creationTime", purgeTime, "<");
        try {
            int deletedCount = deleteByCriteria(q);
            if (deletedCount > 0) {
                statusHandler.info("Deleted " + deletedCount
                        + " old links (provider " + providerName + ")");
            }
        } catch (DataAccessLayerException e) {
            statusHandler.handle(Priority.PROBLEM, "Failed to delete old links",
                    e);
        }
    }

    /**
     * Create each specified link that does not already exist
     *
     * @param links
     */
    public void createLinks(List<CrawlerLink> links) {
        for (CrawlerLink link : links) {
            IPersistableDataObject<?> found = queryById(link.getIdentifier());
            if (found == null) {
                create(link);
            }
        }
    }

    public void setAllProcessed(List<CrawlerLink> links) {
        if (!links.isEmpty()) {
            links.forEach(link -> link.setProcessed(true));
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("urls", links.stream().map(l -> l.getUrl())
                    .collect(Collectors.toList()));
            executeHQLStatement(
                    "update CrawlerLink set processed = true where url in (:urls)",
                    paramMap);
        }
    }
}
