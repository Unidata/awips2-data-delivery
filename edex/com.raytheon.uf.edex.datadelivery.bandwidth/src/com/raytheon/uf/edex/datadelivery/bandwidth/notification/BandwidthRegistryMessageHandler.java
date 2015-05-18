package com.raytheon.uf.edex.datadelivery.bandwidth.notification;

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

import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.registry.event.RegistryEvent;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * 
 * Bandwidth Registry Event Message Handler
 * 
 * This class listens to the JMS topic/queue that 
 * Registry Events are dropped to for the BWM (Bandwidth Manager).
 * It places these events on the internal Guava Event Bus.
 * The Event Bus handles delivery of the associated events.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 14, 2015  4493      dhladky     Initial creation
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class BandwidthRegistryMessageHandler {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(BandwidthRegistryMessageHandler.class);

    // default pub constuctor
    public BandwidthRegistryMessageHandler() {
       
    }

    /**
     * Notify of arrival on the JMS route specified in spring
     * Drop to the Guava EventBus for local processing.
     * 
     * @param RegistryEvent
     */
    public void notify(RegistryEvent event) {

        if (event != null) {
            if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                statusHandler.debug("Received Registry event externally: "
                        + event.toString());
            }
            try {
                EventBus.publish(event);
            } catch (Exception e) {
                statusHandler
                        .error("Couldn't publish event to Bus: "
                                + event.toString(), e);
            }
        }
    }

}
