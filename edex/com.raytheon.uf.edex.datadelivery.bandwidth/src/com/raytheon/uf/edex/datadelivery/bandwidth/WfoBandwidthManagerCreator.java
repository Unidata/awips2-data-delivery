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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.raytheon.uf.common.datadelivery.bandwidth.BandwidthService;
import com.raytheon.uf.common.datadelivery.bandwidth.IProposeScheduleResponse;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthGraphData;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataSetMetaDataHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.SubscriptionHandler;
import com.raytheon.uf.common.datadelivery.service.SendToServerSubscriptionNotificationService;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
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
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 20, 2013  1543     djohnson  Initial creation
 * Feb 27, 2013  1644     djohnson  Schedule SBN subscriptions by routing to the
 *                                  NCF bandwidth manager.
 * Mar 11, 2013  1645     djohnson  Add missing Spring file.
 * May 15, 2013  2000     djohnson  Include daos.
 * Jul 10, 2013  2106     djohnson  Dependency inject registry handlers.
 * Oct 02, 2013  1797     dhladky   Generics
 * Oct 28, 2013  2506     bgonzale  SBN (Shared) Scheduled at the central
 *                                  registry. Added subscription notification
 *                                  service to bandwidth manager.
 * Nov 19, 2013  2545     bgonzale  Added registryEventListener method for
 *                                  update events. Added getBandwidthGraphData.
 *                                  Reschedule updated local subscriptions.
 * Nov 27, 2013  2545     mpduff    Get data by network
 * Dec 04, 2013  2566     bgonzale  use bandwidthmanager method to retrieve
 *                                  spring files.
 * Jan 14, 2014  2692     dhladky   AdhocSubscription handler
 * Jan 30, 2014  2636     mpduff    Scheduling refactor.
 * Feb 11, 2014  2771     bgonzale  Use Data Delivery ID instead of Site.
 * Apr 22, 2014  2992     dhladky   Added IdUtil for siteList
 * Oct 08, 2014  2746     ccody     Relocated registryEventListener to
 *                                  EdexBandwidthManager super class
 * Mar 25, 2015  4329     dhladky   Threaded the graph data requests.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Aug 09, 2016  5771     rjpeter   Update constructor
 * Jan 05, 2017  746      bsteffen  Handle multiple concurrent threads in
 *                                  getBandwidthGraphData()
 * 
 * </pre>
 * 
 * @author djohnson
 */
public class WfoBandwidthManagerCreator<T extends Time, C extends Coverage>
        implements IEdexBandwidthManagerCreator {

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(WfoBandwidthManagerCreator.class);

    /** NCF **/
    protected static final String NCF = "NCF";

    /** LOCAL registry **/
    protected static final String LOCAL_REGISTRY = "LOCAL_REGISTRY";

    /** The threaded requester for graph data **/
    protected static ExecutorService graphDataExecutor;

    /**
     * WFO {@link BandwidthManager} implementation.
     */
    static class WfoBandwidthManager<T extends Time, C extends Coverage>
            extends EdexBandwidthManager<T, C> {

        private static final String MODE_NAME = "registry";

        private static final String[] WFO_BANDWIDTH_MANAGER_FILES = BandwidthUtil
                .getSpringFileNamesForMode(MODE_NAME);

        // TODO: Change to be tied in Spring
        @SuppressWarnings({ "unchecked", "rawtypes" })
        private final BandwidthService<T, C> ncfBandwidthService = new NcfBandwidthService();

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
                DataSetMetaDataHandler dataSetMetaDataHandler,
                SubscriptionHandler subscriptionHandler,
                SendToServerSubscriptionNotificationService subscriptionNotificationService,
                ISubscriptionFinder findSubscriptionsStrategy) {
            super(dbInit, bandwidthDao, retrievalManager, bandwidthDaoUtil,
                    idUtil, dataSetMetaDataHandler, subscriptionHandler,
                    subscriptionNotificationService, findSubscriptionsStrategy);

            graphDataExecutor = Executors.newFixedThreadPool(2);
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
            return requestGraphData();
        }

        /**
         * Requests the local registry's bandwidth graph data.
         * 
         * @return
         */
        protected BandwidthGraphData getLocalBandwidthGraphData() {
            return super.getBandwidthGraphData();
        }

        /**
         * Thread out the requests for graph data
         * 
         * @return
         */
        protected BandwidthGraphData requestGraphData() {
            BandwidthGraphData data = null;

            List<GraphDataRequestor> requestors = new ArrayList<>(2);

            // Add the registries needed
            requestors.add(new GraphDataRequestor(LOCAL_REGISTRY, this));
            requestors.add(new GraphDataRequestor(NCF, this));

            try {
                // start threads
                List<Future<BandwidthGraphData>> results = graphDataExecutor
                        .invokeAll(requestors);

                for (Future<BandwidthGraphData> result : results) {
                    try {
                        if (data == null) {
                            data = result.get();
                        } else {
                            data.merge(result.get());
                        }
                    } catch (ExecutionException e) {
                        statusHandler.error(
                                "Failed to request bandwidth graph data.", e);
                    }
                }
            } catch (InterruptedException e) {
                statusHandler.error(
                        "Process thread had been interupted!", e);
            }
            return data;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BandwidthManager<T, C> getBandwidthManager(
            IBandwidthDbInit dbInit,
            IBandwidthDao bandwidthDao,
            RetrievalManager retrievalManager,
            BandwidthDaoUtil bandwidthDaoUtil,
            RegistryIdUtil idUtil,
            DataSetMetaDataHandler dataSetMetaDataHandler,
            SubscriptionHandler subscriptionHandler,
            SendToServerSubscriptionNotificationService subscriptionNotificationService,
            ISubscriptionFinder findSubscriptionsStrategy) {
        return new WfoBandwidthManager<T, C>(dbInit, bandwidthDao,
                retrievalManager, bandwidthDaoUtil, idUtil,
                dataSetMetaDataHandler, subscriptionHandler,
                subscriptionNotificationService, findSubscriptionsStrategy);
    }

    /**
     * Class to thread the retrieval of GraphData
     * 
     * @author dhladky
     * 
     */
    static class GraphDataRequestor implements Callable<BandwidthGraphData> {

        private final String registryName;

        @SuppressWarnings("rawtypes")
        private WfoBandwidthManager wfoBandwidthManager;

        @Override
        public BandwidthGraphData call() {
            try {
                return process();
            } catch (Exception e) {
                statusHandler
                        .error("Unable to request graph data from this registry! "
                                + registryName, e);
                return null;
            }
        }

        public GraphDataRequestor(String registryName) {
            this.registryName = registryName;
        }

        @SuppressWarnings("rawtypes")
        public GraphDataRequestor(String registryName,
                WfoBandwidthManager wfoBandwidthManager) {
            this.registryName = registryName;
            this.wfoBandwidthManager = wfoBandwidthManager;
        }

        /**
         * Request to the correct registry
         */
        public BandwidthGraphData process() throws Exception {

            /* WFO's hits two servers currently, itself and NCF */
            if (registryName
                    .equals(WfoBandwidthManagerCreator.LOCAL_REGISTRY)) {
                return wfoBandwidthManager.getLocalBandwidthGraphData();
            } else if (registryName.equals(WfoBandwidthManagerCreator.NCF)) {
                return wfoBandwidthManager.ncfBandwidthService
                        .getBandwidthGraphData();
            } else {
                return null;
            }
        }
    }
}
