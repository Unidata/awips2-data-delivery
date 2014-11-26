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
import com.raytheon.uf.common.event.Event;
import com.raytheon.uf.common.registry.ebxml.RegistryUtil;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.time.util.TimeUtil;

/**
 * Generic Notify Event.
 * 
 * Generic Notification Message for Subscription events (containing multiple
 * Subscriptions).
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY 
 * 
 * 
 * Date         Ticket# Engineer      Description 
 * ------------ -----   ------------  ---------------------------------- 
 * Dec 01, 2014 3550    ccody         Initial version
 * 
 * </pre>
 * 
 * @author ccody
 * @version 1.0
 */
@DynamicSerialize
public class GenericNotifyEvent extends Event implements INotifiableEvent {

    private static final long serialVersionUID = -4448137076356795889L;

    private String category;

    private final String message;

    private String owner;

    private int priority;

    /**
     * Constructor.
     * 
     * @param message
     */
    public GenericNotifyEvent(String message) {
        this.message = message;
    }

    public String getCategory() {
        return (this.category);
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOwner() {
        return (this.owner);
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getPriority() {
        return (this.priority);
    }

    public void setPriority(int priority) {
        this.priority = priority;
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
        if (category == null) {
            record.setCategory("Subscription"); // Default value
        } else {
            record.setCategory(this.category);
        }
        if (owner == null) {
            record.setUsername(RegistryUtil.defaultUser); // Default value
        } else {
            record.setUsername(this.owner);
        }
        record.setPriority(this.priority);
        record.setMessage(message);

        if (date == null) {
            date = TimeUtil.newGmtCalendar();
        }
        record.setDate(date);

        return record;
    }

}
