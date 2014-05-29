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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.raytheon.uf.common.datadelivery.harvester.CrawlAgent;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.harvester.ProtoCollection;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.retrieval.ProviderCollectionLinkStore;

/**
 * Decorates a {@link CommunicationStrategy} to use an {@link ExecutorService}
 * while sending data to the harvester.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 30, 2012 1022       djohnson     Initial creation
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
public class MainSequenceCommunicationStrategyDecorator implements
        CommunicationStrategy {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(MainSequenceCommunicationStrategyDecorator.class);

    private static final long SHUTDOWN_TIMEOUT_IN_MINUTES = 1;

    private final ExecutorService taskExecutor;

    private final CommunicationStrategy decorated;

    public MainSequenceCommunicationStrategyDecorator(
            CommunicationStrategy decorated) {
        this(decorated, Executors.newSingleThreadExecutor());
    }

    public MainSequenceCommunicationStrategyDecorator(
            CommunicationStrategy decorated, ExecutorService taskExecutor) {
        this.decorated = decorated;
        this.taskExecutor = taskExecutor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Throwable> getErrors() {
        return decorated.getErrors();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderCollectionLinkStore getNextLinkStore() {
        return decorated.getNextLinkStore();
    }

    @Override
    public void processCollections(HarvesterConfig hconfig,
            Map<String, ProtoCollection> collections, Provider provider,
            CrawlAgent agent) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendException(final Exception e) {
        taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                decorated.sendException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendLinkStore(
            final ProviderCollectionLinkStore providerCollectionLinkStore) {
        taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                decorated.sendLinkStore(providerCollectionLinkStore);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        try {
            decorated.shutdown();
        } finally {
            taskExecutor.shutdown();
            try {
                taskExecutor.awaitTermination(SHUTDOWN_TIMEOUT_IN_MINUTES,
                        TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                statusHandler
                        .handle(Priority.WARN,
                                "Timeout occurred waiting for tasks to finish processing!",
                                e);
            }
        }
    }
}
