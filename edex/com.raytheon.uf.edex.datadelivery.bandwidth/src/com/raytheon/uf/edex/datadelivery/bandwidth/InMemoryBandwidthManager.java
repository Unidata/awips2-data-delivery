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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.datadelivery.bandwidth.ProposeScheduleResponse;
import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDbInit;
import com.raytheon.uf.edex.datadelivery.bandwidth.interfaces.IBandwidthInitializer;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.UnscheduledAllocationReport;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthDaoUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;
import com.raytheon.uf.edex.registry.ebxml.util.RegistryIdUtil;

/**
 * An in-memory {@link BandwidthManager} that does not communicate with an
 * actual database. Intentionally package-private.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 30, 2012  1286     djohnson  Initial creation
 * Feb 20, 2013  1543     djohnson  For now assume all in-memory bandwidth
 *                                  managers are WFOs.
 * Feb 27, 2013  1644     djohnson  Schedule SBN subscriptions.
 * Apr 16, 2013  1906     djohnson  Implements RegistryInitializedListener.
 * Jun 25, 2013  2106     djohnson  init() now takes a {@link RetrievalManager}
 *                                  as well.
 * Jul 09, 2013  2106     djohnson  Add shutdownInternal().
 * Oct 02, 2013  1797     dhladky   Generics
 * Dec 04, 2013  2566     bgonzale  use bandwidthmanager method to retrieve
 *                                  spring files.
 * Feb 06, 2014  2636     bgonzale  added initializeScheduling method.
 * Feb 12, 2014  2636     mpduff    Override getSubscriptionsToSchedule
 * Apr 22, 2014  2992     dhladky   Added IdUtil for siteList
 * May 22, 2014  2808     dhladky   Scheduling unscheduled
 * Nov 03, 2014  2414     dhladky   refactoring some methods in BWM.
 * Jan 15, 2014  3884     dhladky   Removed useless shutdown and shutdown
 *                                  internal methods.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Aug 02, 2017  6186     rjpeter   Updated super call to null for retrievalAgent
 * Aug 29, 2017  6186     rjpeter   Override queueRetrieval to do nothing
 * Nov 22, 2017  6484     tjensen   Improve logging
 * Feb 02, 2018  6471     tjensen   Added UnscheduledAllocationReports
 *
 * </pre>
 *
 * @author djohnson
 */
class InMemoryBandwidthManager<T extends Time, C extends Coverage>
        extends BandwidthManager<T, C> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(InMemoryBandwidthManager.class);

    private static final String MODE_NAME = "inMemoryBandwidthManager";

    // NOTE: NEVER add the bandwidth-datadelivery-eventbus.xml file to this
    // array, in-memory versions should not coordinate with the event bus in any
    // fashion
    public static final String[] IN_MEMORY_BANDWIDTH_MANAGER_FILES = BandwidthUtil
            .getSpringFileNamesForMode(MODE_NAME);

    /**
     * {@link IBandwidthInitializer} which will make a copy of the current
     * running EDEX {@link BandwidthManager} data.
     */
    public static class InMemoryBandwidthInitializer
            implements IBandwidthInitializer {

        /**
         * {@inheritDoc}
         */
        @Override
        public void init(BandwidthManager instance, IBandwidthDbInit dbInit,
                RetrievalManager retrievalManager) {
            BandwidthManager edexBandwidthManager = EdexBandwidthContextFactory
                    .getInstance();
            if (instance instanceof InMemoryBandwidthManager) {
                ((InMemoryBandwidthManager) instance)
                        .copyState(edexBandwidthManager);
            } else {
                statusHandler
                        .error("Cannot initialize InMemoryBandwidthManager from non-InMemory instance ("
                                + instance.getClass() + ")."
                                + "  This is likely a configuration error. Skipping");
            }
        }

        @Override
        public void executeAfterRegistryInit() {
            // Nothing to do
        }

        @Override
        public Map<Network, List<Subscription>> getSubMapByRoute()
                throws Exception {
            // This method is not implemented by the in-memory bandwidth manager
            return null;
        }
    }

    /**
     * Constructor.
     *
     * @param dbInit
     * @param bandwidthDao
     * @param retrievalManager
     * @param bandwidthDaoUtil
     * @param IdUtil
     */
    public InMemoryBandwidthManager(IBandwidthDbInit dbInit,
            IBandwidthDao<T, C> bandwidthDao, RetrievalManager retrievalManager,
            BandwidthDaoUtil bandwidthDaoUtil, RegistryIdUtil idUtil) {
        super(dbInit, bandwidthDao, retrievalManager, bandwidthDaoUtil, idUtil,
                null);
    }

    @Override
    protected String[] getSpringFilesForNewInstance() {
        return IN_MEMORY_BANDWIDTH_MANAGER_FILES;
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

    @Override
    protected void unscheduleSubscriptionsForAllocations(
            List<BandwidthAllocation> unscheduled) {
        // Nothing to do for in-memory version
    }

    @Override
    public List<String> initializeScheduling(
            Map<Network, List<Subscription>> subMap) {
        // Nothing to do for in-memory version
        return new ArrayList<>(0);
    }

    @Override
    protected List<Subscription<T, C>> getSubscriptionsToSchedule(
            Network network) {
        // Nothing to do for in-memory version
        return new ArrayList<>(0);
    }

    @Override
    public void scheduleUnscheduledSubscriptions(String subUnscheduledName) {
        // The in-memory bandwidth manager will never schedule unscheduled
        // subscriptions
    }

    @Override
    protected void resetBandwidthManager(Network requestNetwork,
            String resetReasonMessage) {
        // No in memory implementation.
    }

    @Override
    protected List<UnscheduledAllocationReport> queueRetrieval(
            AdhocSubscription<T, C> adhocSub) {
        // No queuing for in memory impl
        return Collections.emptyList();
    }

}
