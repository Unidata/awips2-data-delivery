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
package com.raytheon.uf.common.datadelivery.event.retrieval;

import com.raytheon.uf.common.datadelivery.event.INotifiableEvent;
import com.raytheon.uf.common.datadelivery.event.notification.NotificationRecord;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.event.Event;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

/**
 * Subscription Status Event
 * 
 * Needs
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY 
 * 
 * 
 * Date         Ticket# Engineer      Description 
 * ------------ -----   ------------------------------------------------ 
 * Oct 15, 2014 3664    ccody         Initial version
 * 
 * </pre>
 * 
 * @author ccody
 * @version 1.0
 */
@DynamicSerialize
public class SubscriptionStatusEvent extends Event implements INotifiableEvent {

    private static final long serialVersionUID = -325623082102350914L;

    private final Subscription subscription;

    private final String message;

    /**
     * Constructor.
     * 
     * @param subscription
     * @param message
     */
    public SubscriptionStatusEvent(Subscription subscription, String message) {
        this.subscription = subscription;
        this.message = message;
    }

    public Subscription getSubscription() {
        return (this.subscription);
    }

    public String getMessage() {
        return (this.message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationRecord generateNotification() {
        NotificationRecord record = new NotificationRecord();
        record.setCategory("Subscription");
        record.setUsername(subscription.getOwner());
        record.setPriority(3);
        record.setMessage(message);
        record.setDate(getDate());
        return record;
    }

}
