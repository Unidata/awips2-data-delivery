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

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.BandwidthManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDbInit;
import com.raytheon.uf.edex.datadelivery.bandwidth.interfaces.IBandwidthInitializer;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;

/**
 *
 * {@link IBandwidthInitializer} that uses Hibernate.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 20, 2013  1543     djohnson  Add SW history, separate how to find
 *                                  subscriptions.
 * Apr 16, 2013  1906     djohnson  Implements RegistryInitializedListener.
 * Apr 30, 2013  1960     djohnson  just call init rather than drop/create
 *                                  tables explicitly.
 * Jun 25, 2013  2106     djohnson  init() now takes a {@link RetrievalManager}
 *                                  as well.
 * Sep 05, 2013  2330     bgonzale  On WFO registry init, only subscribe to
 *                                  local site subscriptions.
 * Sep 06, 2013  2344     bgonzale  Removed attempt to add to immutable empty
 *                                  set.
 * Oct 16, 2013  2267     bgonzale  executeAfterRegistryInit subscribes to all
 *                                  local.  Removed is shared checks.
 * Nov 04, 2013  2506     bgonzale  added site field.  facilitates testing.
 * Nov 19, 2013  2545     bgonzale  Removed programmatic customization for
 *                                  central, client, and dev(monolithic)
 *                                  registries since the injected
 *                                  FindSubscription handler will be configured
 *                                  now.
 * Jan 29, 2014  2636     mpduff    Scheduling refactor.
 * Feb 06, 2014  2636     bgonzale  Use scheduling initialization method after
 *                                  registry init.
 * Feb 11, 2014  2771     bgonzale  Use Data Delivery ID instead of Site.
 * Feb 14, 2014  2636     mpduff    Clean up logging
 * Apr 09, 2014  3012     dhladky   Adhoc firing prevention.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Nov 22, 2017  6484     tjensen   Remove unused init return value and site
 *                                  field
 *
 * </pre>
 *
 * @author djohnson
 */
public class HibernateBandwidthInitializer implements IBandwidthInitializer {

    private static final Logger logger = LoggerFactory
            .getLogger(HibernateBandwidthInitializer.class);

    private final ISubscriptionFinder findSubscriptionsStrategy;

    private BandwidthManager instance;

    /**
     * @param strategy
     */
    public HibernateBandwidthInitializer(
            ISubscriptionFinder findSubscriptionsStrategy) {
        this.findSubscriptionsStrategy = findSubscriptionsStrategy;
    }

    @Override
    public void init(BandwidthManager instance, IBandwidthDbInit dbInit,
            RetrievalManager retrievalManager) {

        this.instance = instance;

        // TODO: Need to resolve how to load Subscriptions that SHOULD have been
        // fulfilled. In the case were DD has been down for a while
        // BEFORE removing the tables...

        try {
            dbInit.init();
        } catch (Exception e1) {
            throw new RuntimeException(
                    "Error generating bandwidth manager tables", e1);
        }

        retrievalManager.initRetrievalPlans();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeAfterRegistryInit() {
        try {

            Map<Network, List<Subscription>> subMap = findSubscriptionsStrategy
                    .findSubscriptionsToSchedule();

            List<String> unscheduled = instance.initializeScheduling(subMap);

            if (!unscheduled.isEmpty()) {
                StringBuilder sb = new StringBuilder(
                        "The following subscriptions could not be scheduled at startup: ");
                sb.append(StringUtil.NEWLINE);
                for (String subscription : unscheduled) {
                    sb.append(subscription).append(" ");
                }
                logger.info(sb.toString());
            }

        } catch (Exception e) {
            logger.error("Failed to query for subscriptions to schedule", e);
        }
    }

    /**
     * Get a map of the active subs by route.
     *
     * @return Map<Network, List<Subscription>>
     * @throws Exception
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map<Network, List<Subscription>> getSubMapByRoute()
            throws Exception {

        return findSubscriptionsStrategy.findSubscriptionsToSchedule();
    }
}
