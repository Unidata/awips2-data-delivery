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
package com.raytheon.uf.edex.datadelivery.bandwidth.ncf;

import java.util.List;
import java.util.Set;

import com.raytheon.uf.common.datadelivery.bandwidth.ProposeScheduleResponse;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataSetMetaDataHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.SubscriptionHandler;
import com.raytheon.uf.common.datadelivery.service.SendToServerSubscriptionNotificationService;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.edex.datadelivery.bandwidth.BandwidthManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.EdexBandwidthContextFactory.IEdexBandwidthManagerCreator;
import com.raytheon.uf.edex.datadelivery.bandwidth.EdexBandwidthManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDbInit;
import com.raytheon.uf.edex.datadelivery.bandwidth.hibernate.ISubscriptionFinder;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.SubscriptionRetrievalAgent;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthDaoUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;
import com.raytheon.uf.edex.registry.ebxml.util.RegistryIdUtil;

/**
 * {@link IEdexBandwidthManagerCreator} for an NCF bandwidth manager.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 20, 2013  1543     djohnson  Initial creation
 * Feb 27, 2013  1644     djohnson  Schedule SBN subscriptions.
 * Mar 11, 2013  1645     djohnson  Add missing Spring file.
 * May 15, 2013  2000     djohnson  Include daos.
 * Jul 10, 2013  2106     djohnson  Dependency inject registry handlers.
 * Oct 03, 2013  1797     dhladky   Generics added
 * Nov 08, 2013  2506     bgonzale  Added subscription notification service to
 *                                  bandwidth manager.
 * Nov 19, 2013  2545     bgonzale  Added registryEventListener method for
 *                                  update events. Reschedule updated shared
 *                                  subscriptions.
 * Dec 04, 2013  2566     bgonzale  use bandwidthmanager method to retrieve
 *                                  spring files.
 * Jan 14, 2014  2692     dhladky   AdhocSubscription handler
 * Apr 22, 2014  2992     dhladky   Added IdUtil for siteList
 * Oct 08, 2014  2746     ccody     Relocated registryEventListener to
 *                                  EdexBandwidthManager super class
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Aug 09, 2016  5771     rjpeter   Update constructor
 * Aug 02, 2017  6186     rjpeter   Added SubscriptionRetrievalAgent
 * Feb 02, 2018  6471     tjensen   Added UnscheduledAllocationReports
 *
 * </pre>
 *
 * @author djohnson
 */
public class NcfBandwidthManagerCreator<T extends Time, C extends Coverage>
        implements IEdexBandwidthManagerCreator<T, C> {

    /**
     * NCF {@link BandwidthManager} implementation.
     */
    static class NcfBandwidthManager<T extends Time, C extends Coverage>
            extends EdexBandwidthManager<T, C> {

        private static final String MODE_NAME = "centralRegistry";

        private static final String[] NCF_BANDWIDTH_MANAGER_FILES = BandwidthUtil
                .getSpringFileNamesForMode(MODE_NAME);

        /**
         * Constructor.
         *
         * @param dbInit
         * @param bandwidthDao
         * @param retrievalManager
         * @param bandwidthDaoUtil
         */
        public NcfBandwidthManager(IBandwidthDbInit dbInit,
                IBandwidthDao<T, C> bandwidthDao,
                RetrievalManager retrievalManager,
                BandwidthDaoUtil<T, C> bandwidthDaoUtil, RegistryIdUtil idUtil,
                SubscriptionRetrievalAgent retrievalAgent,
                DataSetMetaDataHandler dataSetMetaDataHandler,
                SubscriptionHandler subscriptionHandler,
                SendToServerSubscriptionNotificationService subscriptionNotificationService,
                ISubscriptionFinder findSubscriptionsStrategy) {
            super(dbInit, bandwidthDao, retrievalManager, bandwidthDaoUtil,
                    idUtil, retrievalAgent, dataSetMetaDataHandler,
                    subscriptionHandler, subscriptionNotificationService,
                    findSubscriptionsStrategy);
        }

        @Override
        protected String[] getSpringFilesForNewInstance() {
            return NCF_BANDWIDTH_MANAGER_FILES;
        }

        @Override
        protected ProposeScheduleResponse proposeScheduleSbnSubscription(
                List<Subscription<T, C>> subscriptions) throws Exception {
            return proposeScheduleSubscriptions(subscriptions);
        }

        @Override
        protected Set<String> scheduleSbnSubscriptions(
                List<Subscription<T, C>> subscriptions)
                throws SerializationException {
            return getUnscheduledSubNames(scheduleSubscriptions(subscriptions));
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public BandwidthManager<T, C> getBandwidthManager(IBandwidthDbInit dbInit,
            IBandwidthDao bandwidthDao, RetrievalManager retrievalManager,
            BandwidthDaoUtil bandwidthDaoUtil, RegistryIdUtil idUtil,
            SubscriptionRetrievalAgent retrievalAgent,
            DataSetMetaDataHandler dataSetMetaDataHandler,
            SubscriptionHandler subscriptionHandler,
            SendToServerSubscriptionNotificationService subscriptionNotificationService,
            ISubscriptionFinder findSubscriptionsStrategy) {
        return new NcfBandwidthManager(dbInit, bandwidthDao, retrievalManager,
                bandwidthDaoUtil, idUtil, retrievalAgent,
                dataSetMetaDataHandler, subscriptionHandler,
                subscriptionNotificationService, findSubscriptionsStrategy);
    }

}
