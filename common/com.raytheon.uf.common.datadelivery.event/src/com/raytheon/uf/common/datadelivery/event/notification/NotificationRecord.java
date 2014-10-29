package com.raytheon.uf.common.datadelivery.event.notification;

import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.dataplugin.persist.IPersistableDataObject;
import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * 
 * Record class for the notification table in the ebxml database
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 1, 2012             jsanchez     Initial creation
 * 3/18/2012    1802       bphillip    Modified to implement IPersistableDataObject
 * Aug 18, 2014 2746       ccody       Non-local Subscription changes not updating dialogs
 * 10/28/2014   3454       bphillip    Added sequence for id generation
 * 
 * </pre>
 * 
 * @author jsanchez
 * @version 1.0
 */
@Entity
@Table(name = "notification", schema = "events")
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class NotificationRecord implements ISerializableObject,
        IPersistableDataObject<Integer> {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO, generator="notification_seq")
    @SequenceGenerator(name="notification_seq", sequenceName="notification_seq")
    @DynamicSerializeElement
    private Integer id;

    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private Calendar date;

    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private Integer priority;

    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private String category;

    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private String username;

    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private String message;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Calendar getDate() {
        return date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Integer getIdentifier() {
        return this.id;
    }
    
    /**
     * Is this NotificationRecord a Functional Match to the other NotificationRecord.
     * 
     * Checks to see if this NotificationRecord is equivalent the other NotificationRecord.<p>
     * Does the new NotificationRecord match the last  NotificationRecord<p>
     * Note that two NotificationRecord instances CAN have 
     * different Id values but the SAME message pay-load and very close creation dates.
     * This makes them functionally equivalent; and refer to the same event.
     * It is not uncommon for the generated Subscription Notification Records to refer to the same event,
     * but have different Ids and slightly different time stamps. 
     * This method will filter out these unnecessary duplicates.
     * 
     * @param otherNotificationRecord A NotificationRecord to compare against
     * 
     * @return boolean true this is a functionally equivalent NotificationRecord
     *                  When areEquivalent is FALSE; the two objects are NOT considered to reference the same Notification event.
     */
    public Boolean areFunctionallyEquivalent(NotificationRecord otherNotificationRecord) {
        
        boolean areEquivalent = true;
        if (otherNotificationRecord != null) {
            String otherCategory = otherNotificationRecord.getCategory();
            Integer otherPriority = otherNotificationRecord.getPriority();
            String otherMessage = otherNotificationRecord.getMessage();
            String otherUsername = otherNotificationRecord.getUsername();
            Calendar otherDate = otherNotificationRecord.getDate();
            long otherTimeInMils = otherDate.getTimeInMillis();
            if (otherDate != null) {
                otherTimeInMils = otherDate.getTimeInMillis();
            }
    
            areEquivalent = areFunctionallyEquivalent(otherCategory,  otherPriority, otherMessage, otherUsername, otherTimeInMils);
        } else {
            areEquivalent = false;
        }
        
        return(areEquivalent);
    }
        
    /**
     * Is this NotificationRecord a Functional Match to the other NotificationRecord properties.
     * 
     * Checks to see if this NotificationRecord is equivalent the other NotificationRecord properties<p>
     * Note that two NotificationRecord instances CAN have 
     * different Id values (hence an Id parameter is omitted here) but the SAME message 
     * pay-load and very close creation dates.
     * This makes them functionally equivalent; and refer to the same event.
     * It is not uncommon for the generated Subscription Notification Records to refer to the same event, 
     * but have different Id values and/or slightly different time stamps.
     * This method will filter out these unnecessary duplicates.
     * 
     * @param otherCategory  Other Notification Category String value
     * @param otherPriority  Other Notification Priority Integer value
     * @param otherMessage   Other Notification Message String value
     * @param otherUsername  Other Notification User Name String value
     * @param otherTimeInMils  Other Notification creation time in Mils long value
     * 
     * @return boolean true this is a matching NotificationRecord
     *                  When FALSE; the two objects are NOT considered to reference the same Notification event.
     */
    public Boolean areFunctionallyEquivalent(String otherCategory,  
                            Integer otherPriority, String otherMessage, String otherUsername, 
                            long otherTimeInMils) {
        if (this.message == null) {
            if (otherMessage != null) {
                return(false);
            }
        } else if (this.message.equals(otherMessage) == false) {
            return(false);
        }
        if (this.category == null) {
            if (otherCategory != null) {
                return(false);
            }
        } else if (this.category.equals(otherCategory) == false) {
            return(false);
        }
        if (this.username == null) {
            if (otherUsername != null) {
                return(false);
            }
        } else if (this.username.equals(otherUsername) == false) {
            return(false);
        }
        if (this.priority == null) {
            if (otherPriority != null) {
                return(false);
            }
        } else if (this.priority.equals(otherPriority) == false) {
            return(false);
        }

        /* The two Notification events can have "slightly" different time stamps and still refer to the same event. */
        long timeInMils = 0;
        if (this.date != null) {
            timeInMils = this.date.getTimeInMillis();
        }
        
        long threshhold = 200L; // 200 milliseconds
        long difference = Math.abs(timeInMils - otherTimeInMils);
        if (difference > threshhold) {
            return(false);
        }
        
        return(true);
    }

}
