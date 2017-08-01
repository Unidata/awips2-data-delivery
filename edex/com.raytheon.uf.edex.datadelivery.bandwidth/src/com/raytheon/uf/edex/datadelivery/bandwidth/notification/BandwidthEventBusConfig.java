package com.raytheon.uf.edex.datadelivery.bandwidth.notification;

/**
 * Configuration bean for the BandwidthEventBus.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 03, 2012  726      jspinks   Initial creation
 * Jul 09, 2013  2106     djohnson  No Spring required to get thread pool sizes,
 *                                  remove subscriptionBus.
 * Jun 09, 2015  4047     dhladky   Performance improvement on startup, brought
 *                                  back subscription bus.
 * Aug 02, 2017  6186     rjpeter   Removed retrieval pool size.
 *
 * </pre>
 *
 * @author jspinks
 */
public class BandwidthEventBusConfig {

    // Set reasonable default values
    private static final int dataSetMetaDataPoolSize = Integer
            .getInteger("bandwidth.dataSetMetaDataPoolSize", 2);

    // Set reasonable default values
    private static final int subscriptionPoolSize = Integer
            .getInteger("bandwidth.subscriptiontionPoolSize", 2);

    /**
     * Get attribute dataSetMetaDataPoolSize.
     *
     * @return The value of attribute dataSetMetaDataPoolSize.
     */
    public int getDataSetMetaDataPoolSize() {
        return dataSetMetaDataPoolSize;
    }

    public int getSubscriptionPoolSize() {
        return subscriptionPoolSize;
    }
}
