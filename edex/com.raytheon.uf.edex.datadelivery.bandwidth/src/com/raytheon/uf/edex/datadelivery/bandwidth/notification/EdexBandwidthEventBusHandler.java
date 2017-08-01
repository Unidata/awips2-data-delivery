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
package com.raytheon.uf.edex.datadelivery.bandwidth.notification;

import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.edex.event.BaseEdexEventBusHandler;

/**
 * Bandwidth Event Bus handler used on EDEX.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * May 28, 2013  1650     djohnson  Extracted from {@link BandwidthEventBus}.
 * Jul 09, 2013  2106     djohnson  Remove subscriptionBus.
 * Jun 09, 2015  4047     dhladky   Performance improvement on startup, brought
 *                                  back subscription bus.
 * Aug 02, 2017  6186     rjpeter   Removed retrieval bus.
 *
 * </pre>
 *
 * @author djohnson
 */

public class EdexBandwidthEventBusHandler extends
        BaseEdexEventBusHandler<Object> implements IBandwidthEventBusHandler {

    private final com.google.common.eventbus.EventBus dataSetBus;

    private final com.google.common.eventbus.EventBus subscriptionBus;

    /**
     * Constructor.
     */
    public EdexBandwidthEventBusHandler() {
        this(new BandwidthAsyncEventBusFactory());
    }

    /**
     * Constructor.
     *
     * @param eventBusFactory
     *            the factory to retrieve google event buses
     */
    EdexBandwidthEventBusHandler(BandwidthEventBusFactory eventBusFactory) {
        super(eventBusFactory);
        this.dataSetBus = eventBusFactory.getDataSetBus();
        this.subscriptionBus = eventBusFactory.getSubscriptionBus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void publishInternal(Object object) {

        if (object instanceof DataSetMetaData) {
            dataSetBus.post(object);
        } else if (object instanceof Subscription) {
            subscriptionBus.post(object);
        } else {
            throw new IllegalArgumentException(
                    "Object type [" + object.getClass().getName()
                            + "] not supported in BandwidthEventBus");
        }
    }
}
