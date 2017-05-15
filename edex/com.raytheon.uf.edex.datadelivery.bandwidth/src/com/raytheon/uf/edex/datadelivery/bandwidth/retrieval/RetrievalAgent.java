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
package com.raytheon.uf.edex.datadelivery.bandwidth.retrieval;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig.RETRIEVAL_MODE;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.core.EdexException;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;

/**
 *
 * A retrieval agent.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 11, 2012  726      djohnson  Add SW history, use generics, separate work
 *                                  method from loop control.
 * Nov 09, 2012  1286     djohnson  Add ability to kill the threads when
 *                                  BandwidthManager instance is replaced.
 * Mar 05, 2013  1647     djohnson  Sleep one minute between checks.
 * Jan 30, 2014  2686     dhladky   refactor of retrieval.
 * Feb 10, 2014  2678     dhladky   Prevent duplicate allocations.
 * Apr 06, 2016  5424     dhladky   Allow for ASYNC processing of retrievals.
 * May 11, 2017  6186     rjpeter   Added TODO and logger.
 *
 * </pre>
 *
 * @author djohnson
 * @param <ALLOCATION_TYPE>
 */
public abstract class RetrievalAgent<ALLOCATION_TYPE extends BandwidthAllocation>
        extends Thread {

    private static final long SLEEP_TIME = TimeUtil.MILLIS_PER_MINUTE;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String SUBSCRIPTION_AGENT = "SubscriptionAgent";

    private final Object notifier;

    protected final Network network;

    protected final String retrievalRoute;

    protected final String asyncRetrievalRoute;

    protected final RetrievalManager retrievalManager;

    private boolean dead;

    /**
     * Constructor.
     *
     * @param network
     *            the network this retrieval agent utilizes
     * @param retrievalRoute
     *            the destination uri to send objects
     * @param asyncRetrievalUri
     *            the destination uri to send objects
     * @param notifier
     *            the object used to signal the agent that data is available
     * @param retrievalManager
     *            the retrieval manager
     */
    public RetrievalAgent(Network network, String retrievalRoute,
            String asyncRetrievalUri, final Object notifier,
            RetrievalManager retrievalManager) {
        this.network = network;
        this.retrievalRoute = retrievalRoute;
        this.asyncRetrievalRoute = asyncRetrievalUri;
        this.notifier = notifier;
        this.retrievalManager = retrievalManager;
    }

    @Override
    public void run() {
        try {
            // don't start immediately
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            // ignore
        }

        while (!dead) {
            try {
                doRun();
            } catch (Throwable e) {
                // so thread can't die
                logger.error(
                        "Unable to look up next retrieval request.  Sleeping for 1 minute before trying again.",
                        e);
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e1) {
                    // ignore
                }
            }
        }
    }

    /**
     * Do the actual work.
     *
     * @throws EdexException
     */
    public void doRun() throws EdexException {
        /*
         * TODO: Remove launching subs per minute, base on data arrival, and
         * should not interact with BandwidthAllocation
         */
        logger.info(
                network + ": Checking for bandwidth allocations to process...");
        List<BandwidthAllocation> allocationReservations = retrievalManager
                .getRecentAllocations(network, getAgentType());

        if (allocationReservations != null) {

            List<ALLOCATION_TYPE> allocations = new ArrayList<>(
                    allocationReservations.size());

            for (BandwidthAllocation bandwidthAlloc : allocationReservations) {
                if (bandwidthAlloc == RetrievalManager.POISON_PILL) {
                    logger.info(
                            "Received kill request, this thread is shutting down...");
                    dead = true;
                    return;
                }
                // cast to type class
                ALLOCATION_TYPE allocation = getAllocationTypeClass()
                        .cast(bandwidthAlloc);
                allocations.add(allocation);
                logger.info(network + ": Processing allocation["
                        + allocation.getId() + "]");
            }

            processAllocations(allocations);

        } else {
            synchronized (notifier) {
                try {
                    logger.info(network + ": None found, sleeping for ["
                            + SLEEP_TIME + "]");

                    notifier.wait(SLEEP_TIME);
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while waiting for notification.",
                            e);
                }
            }
        }
    }

    /**
     * Return the concrete class type that the agent handles.
     *
     * @return the class reference
     */
    abstract Class<ALLOCATION_TYPE> getAllocationTypeClass();

    /**
     * Return the agent type this retrieval agent processes.
     *
     * @return the agent type
     */
    abstract String getAgentType();

    /**
     * Process the {@link BandwidthAllocation} retrieved.
     *
     * @param allocations
     *            the allocations
     * @throws EdexException
     *             on error processing the allocation
     */
    abstract void processAllocations(List<ALLOCATION_TYPE> allocations)
            throws EdexException;

    /**
     * Get the network
     *
     * @return
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * Returns the Retrieval Mode for the retrievals generated here. This can be
     * configured specific to each provider. Defaults to SYNC
     *
     * @param SeriviceType
     * @return RETRIEVAL_MODE
     */
    protected RETRIEVAL_MODE getRetrievalMode(ServiceType type) {

        // default to synchronous processing
        RETRIEVAL_MODE mode = RETRIEVAL_MODE.SYNC;

        ServiceConfig sc = getServiceConfig(type);

        if (sc != null) {
            if (sc.getConstantValue(
                    RetrievalGenerator.RETRIEVAL_MODE_CONSTANT) != null) {
                String mode_constant = sc.getConstantValue(
                        RetrievalGenerator.RETRIEVAL_MODE_CONSTANT);
                mode = RETRIEVAL_MODE.valueOf(mode_constant);
            }
        }

        return mode;
    }

    /**
     * Get the service configuration
     *
     * @param ServiceType
     *
     * @return ServiceConfig
     */
    protected ServiceConfig getServiceConfig(ServiceType type) {

        ServiceConfig serviceConfig = null;

        try {
            serviceConfig = HarvesterServiceManager.getInstance()
                    .getServiceConfig(type);
        } catch (Exception e) {
            logger.error("Unable to load ServiceConfig! " + type.name(), e);
        }

        return serviceConfig;
    }
}