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
package com.raytheon.uf.edex.datadelivery.bandwidth;

import java.util.List;
import java.util.Set;

import com.raytheon.uf.common.datadelivery.bandwidth.IBandwidthService;
import com.raytheon.uf.common.datadelivery.bandwidth.IProposeScheduleResponse;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthGraphData;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.IAdhocSubscriptionHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.IDataSetMetaDataHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.ISubscriptionHandler;
import com.raytheon.uf.common.datadelivery.service.ISubscriptionNotificationService;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.edex.datadelivery.bandwidth.EdexBandwidthContextFactory.IEdexBandwidthManagerCreator;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDbInit;
import com.raytheon.uf.edex.datadelivery.bandwidth.hibernate.ISubscriptionFinder;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthDaoUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;
import com.raytheon.uf.edex.registry.ebxml.util.RegistryIdUtil;

/**
 * {@link IEdexBandwidthManagerCreator} for a WFO bandwidth manager.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 20, 2013 1543       djohnson     Initial creation
 * Feb 27, 2013 1644       djohnson     Schedule SBN subscriptions by routing to the NCF bandwidth manager.
 * Mar 11, 2013 1645       djohnson     Add missing Spring file.
 * May 15, 2013 2000       djohnson     Include daos.
 * Jul 10, 2013 2106       djohnson     Dependency inject registry handlers.
 * Oct 2,  2013 1797       dhladky      Generics
 * Oct 28, 2013 2506       bgonzale     SBN (Shared) Scheduled at the central registry.
 *                                      Added subscription notification service to bandwidth manager.
 * Nov 19, 2013 2545       bgonzale     Added registryEventListener method for update events.
 *                                      Added getBandwidthGraphData.
 *                                      Reschedule updated local subscriptions.
 * Nov 27, 2013 2545       mpduff       Get data by network
 * Dec 04, 2013 2566       bgonzale     use bandwidthmanager method to retrieve spring files.
 * Jan 14, 2014 2692       dhladky      AdhocSubscription handler
 * Jan 30, 2014 2636       mpduff       Scheduling refactor.
 * Feb 11, 2014 2771       bgonzale     Use Data Delivery ID instead of Site.
 * Apr 22, 2014 2992       dhladky      Added IdUtil for siteList
 * Oct 08, 2014 2746       ccody        Relocated registryEventListener to EdexBandwidthManager super class
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
public class WfoBandwidthManagerCreator<T extends Time, C extends Coverage>
        implements IEdexBandwidthManagerCreator {

    /**
     * WFO {@link BandwidthManager} implementation.
     */
    static class WfoBandwidthManager<T extends Time, C extends Coverage>
            extends EdexBandwidthManager<T, C> {

        private static final String MODE_NAME = "registry";

        private static final String[] WFO_BANDWIDTH_MANAGER_FILES = BandwidthUtil.getSpringFileNamesForMode(MODE_NAME);

        // TODO: Change to be DIed in Spring
        private final IBandwidthService<T, C> ncfBandwidthService = new NcfBandwidthService();

        /**
         * Constructor.
         * 
         * @param dbInit
         * @param bandwidthDao
         * @param retrievalManager
         * @param bandwidthDaoUtil
         * @param idUtil
         * @param dataSetMetaDataHandler
         * @param subscriptionHandler
         * @param adhocSubscriptionHandler
         * @param subscriptionNotificationService
         * @param findSubscriptionsStrategy
         */
        public WfoBandwidthManager(
                IBandwidthDbInit dbInit,
                IBandwidthDao bandwidthDao,
                RetrievalManager retrievalManager,
                BandwidthDaoUtil bandwidthDaoUtil,
                RegistryIdUtil idUtil,
                IDataSetMetaDataHandler dataSetMetaDataHandler,
                ISubscriptionHandler subscriptionHandler,
                IAdhocSubscriptionHandler adhocSubscriptionHandler,
                ISubscriptionNotificationService subscriptionNotificationService,
                ISubscriptionFinder findSubscriptionsStrategy) {
            super(dbInit, bandwidthDao, retrievalManager, bandwidthDaoUtil, idUtil,
                    dataSetMetaDataHandler, subscriptionHandler,
                    adhocSubscriptionHandler, subscriptionNotificationService,
                    findSubscriptionsStrategy);
        }
        @Override
        protected String[] getSpringFilesForNewInstance() {
            return WFO_BANDWIDTH_MANAGER_FILES;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected IProposeScheduleResponse proposeScheduleSbnSubscription(
                List<Subscription<T, C>> subscriptions) throws Exception {

            final IProposeScheduleResponse proposeResponse = ncfBandwidthService
                    .proposeSchedule(subscriptions);

            // If the NCF bandwidth manager says they fit without
            // unscheduling anything, then schedule them at the NCF level
            if (proposeResponse.getUnscheduledSubscriptions().isEmpty()) {
                ncfBandwidthService.schedule(subscriptions);
            }

            return proposeResponse;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Set<String> scheduleSbnSubscriptions(
                List<Subscription<T, C>> subscriptions)
                throws SerializationException {

            return ncfBandwidthService.schedule(subscriptions);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected BandwidthGraphData getBandwidthGraphData() {
            BandwidthGraphData data = super.getBandwidthGraphData();
            BandwidthGraphData data2 = ncfBandwidthService
                    .getBandwidthGraphData();
            if (data2 != null) {
                data.merge(data2);
            }

            return data;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IBandwidthManager<T, C> getBandwidthManager(IBandwidthDbInit dbInit,
            IBandwidthDao bandwidthDao, RetrievalManager retrievalManager,
            BandwidthDaoUtil bandwidthDaoUtil,
            RegistryIdUtil idUtil,
            IDataSetMetaDataHandler dataSetMetaDataHandler,
            ISubscriptionHandler subscriptionHandler,
            IAdhocSubscriptionHandler adhocSubscriptionHandler,
            ISubscriptionNotificationService subscriptionNotificationService,
            ISubscriptionFinder findSubscriptionsStrategy) {
        return new WfoBandwidthManager<T, C>(dbInit, bandwidthDao,
                retrievalManager, bandwidthDaoUtil, idUtil, dataSetMetaDataHandler,
                subscriptionHandler, adhocSubscriptionHandler,
                subscriptionNotificationService, findSubscriptionsStrategy);
    }
}
