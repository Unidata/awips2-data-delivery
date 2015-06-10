package com.raytheon.uf.edex.datadelivery.bandwidth.notification;

/**
 * Configuration bean for the BandwidthEventBus.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 3, 2012  0726      jspinks     Initial creation
 * Jul 09, 2013 2106      djohnson    No Spring required to get thread pool sizes, remove subscriptionBus.
 * Jun 09, 2015 4047       dhladky    Performance improvement on startup, brought back subscription bus.
 * 
 * </pre>
 * 
 * @author jspinks
 * @version 1.0
 */
public class BandwidthEventBusConfig {

    // Set reasonable default values
    private static final int dataSetMetaDataPoolSize = Integer.getInteger(
            "bandwidth.dataSetMetaDataPoolSize", 2);

    private static final int retrievalPoolSize = Integer.getInteger(
            "bandwidth.retrievalPoolSize", 4);
    
    // Set reasonable default values
    private static final int subscriptionPoolSize = Integer.getInteger(
            "bandwidth.subscriptiontionPoolSize", 2);

    /**
     * Get attribute dataSetMetaDataPoolSize.
     * 
     * @return The value of attribute dataSetMetaDataPoolSize.
     */
    public int getDataSetMetaDataPoolSize() {
        return dataSetMetaDataPoolSize;
    }

    /**
     * Get attribute retrievalPoolSize.
     * 
     * @return The value of attribute retrievalPoolSize.
     */
    public int getRetrievalPoolSize() {
        return retrievalPoolSize;
    }
    
    public int getSubscriptionPoolSize() {
        return subscriptionPoolSize;
    }
}
