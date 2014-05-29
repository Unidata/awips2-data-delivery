package com.raytheon.uf.edex.datadelivery.bandwidth.interfaces;

import java.util.List;

import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.SubscriptionRetrieval;
import com.raytheon.uf.edex.datadelivery.bandwidth.processing.BandwidthSubscriptionContainer;

/**
 * 
 * Interface for Subscription aggregation and RetrievalRequest generation. Each
 * implementation of this interface will examine the List of
 * BandwidthSubscription Objects provided and evaluate how to combine and/or
 * subset those Subscriptions into SubscriptionRetrieval Objects.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 18, 2012 726        jspinks     Initial creation
 * Nov 09, 2012 1286       djohnson    Renamed to comply with AWIPS standards.
 * Jun 13, 2013 2095       djohnson    Aggregator only receives the newly created bandwidth subscriptions now.
 * 
 * </pre>
 * 
 * @author jspinks
 * @version 1.0
 */
public interface ISubscriptionAggregator {

    /**
     * Generate a List of SubscriptionRetrieval Object for the provided
     * BandwidthSubscription Objects.
     * 
     * @param container
     *            A container with a List of BandwidthSubscription Objects which
     *            were just added, and their subscription
     * 
     * @return The SubscriptionRetrieval Objects used to fulfill the
     *         BandwidthSubscription Objects provided.
     */
    List<SubscriptionRetrieval> aggregate(
            BandwidthSubscriptionContainer container);

    /**
     * This method is called once all the SubscriptionRetrievals for a
     * subscription are fulfilled and allows the ISubscriptionAggregator to
     * "assemble" the finished subscription(s). This method will be called once
     * for each Subscription who's SubscriptionRetrieval Objects are all in the
     * "FULFILLED" state.
     * 
     * @param retrievals
     *            A List of SubscriptionRetrieval(s) that make were produced
     *            from calling aggregate for a particular subscription.
     * 
     * @return A List of completed subscriptions, ready for notification to the
     *         user.
     */
    List<BandwidthSubscription> completeRetrieval(
            List<SubscriptionRetrieval> retrievals);
}
