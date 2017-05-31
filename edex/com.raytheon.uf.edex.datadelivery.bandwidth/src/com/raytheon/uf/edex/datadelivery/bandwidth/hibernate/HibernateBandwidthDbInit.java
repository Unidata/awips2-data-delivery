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

package com.raytheon.uf.edex.datadelivery.bandwidth.hibernate;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.cfg.Configuration;
import org.hibernate.jdbc.Work;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raytheon.uf.edex.database.init.DbInit;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthBucket;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDbInit;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrievalAttributes;

/**
 * The DbInit class is responsible for ensuring that the appropriate tables are
 * present in the bandwidth manager database implementation.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 31, 2012  726      jspinks   Copied and refactored from ebxml registry
 *                                  DbInit
 * Oct 26, 2012  1286     djohnson  Renamed to Hibernate specific.
 * Apr 30, 2013  1960     djohnson  Extend the generalized DbInit.
 * Jun 24, 2013  2106     djohnson  Add {@link BandwidthBucket} to annotated
 *                                  classes.
 * Jul 11, 2013  2106     djohnson  Add {@link SubscriptionRetrievalAttributes}.
 * Jan 08, 2013  2645     bgonzale  Added RegistryBandwidthRecord to
 *                                  configuration annotated class list.
 * Oct 16, 2014  3454     bphillip  Upgrading to Hibernate 4.
 * Aug 17, 2016  5771     rjpeter   Auto create DataSetLatency table.
 * Feb 16, 2017  5899     rjpeter   Updated getTableCheckQuery to not always
 *                                  recreate tables. Updated init to truncate
 *                                  tables on start.
 * May 26, 2017  6186     rjpeter   Remove BandwidthDataSetUpdate
 *
 * </pre>
 *
 * @author jspinks
 */
@Transactional
@Service
public class HibernateBandwidthDbInit extends DbInit
        implements IBandwidthDbInit {

    /**
     * Creates a new instance of DbInit. This constructor should only be called
     * once when loaded by the Spring container.
     *
     * @param bandwidthDao
     *            the dao to use
     *
     */
    public HibernateBandwidthDbInit() {
        super("bandwidth manager");
    }

    @Override
    protected Configuration getConfiguration() {
        /*
         * Create a new configuration object which holds all the classes that
         * this Hibernate SessionFactory is aware of
         */
        Configuration aConfig = new Configuration();
        aConfig.addAnnotatedClass(
                com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthBucket.class);
        aConfig.addAnnotatedClass(
                com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription.class);
        aConfig.addAnnotatedClass(
                com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrieval.class);
        aConfig.addAnnotatedClass(
                com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation.class);
        aConfig.addAnnotatedClass(
                com.raytheon.uf.edex.datadelivery.bandwidth.dao.DataSetLatency.class);
        aConfig.addAnnotatedClass(
                com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrievalAttributes.class);
        aConfig.addAnnotatedClass(
                com.raytheon.uf.edex.datadelivery.bandwidth.registry.RegistryBandwidthRecord.class);
        return aConfig;
    }

    @Override
    public void init() throws Exception {
        initDb();
        logger.info("Clearing previous bandwidth data...");
        final Work work = new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(
                            "truncate bandwidth_bucket, bandwidth_subscription_retrieval_attributes, bandwidth_allocation, bandwidth_subscription, datadeliveryregistrybandwidth");
                    connection.commit();
                }
            }
        };

        dao.executeWork(work);
        logger.info("Previous bandwidth data cleared");
    }

    @Override
    protected String getTableCheckQuery() {
        return "SELECT schemaname || '.' || tablename from pg_tables where schemaname = 'awips' and "
                + "(tablename like 'bandwidth_%' or tablename in ('datadeliveryregistrybandwidth', 'dd_data_set_latency'));";
    }
}
