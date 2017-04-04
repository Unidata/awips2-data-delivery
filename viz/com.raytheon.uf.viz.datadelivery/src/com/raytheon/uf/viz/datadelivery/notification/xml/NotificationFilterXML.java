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
package com.raytheon.uf.viz.datadelivery.notification.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.viz.datadelivery.notification.PriorityImages.Priority;

/**
 * Notification Filter XML class.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#     Engineer     Description
 * ------------- ----------- ------------ --------------------------
 * Feb 09, 2012              mpduff       Initial creation
 * Apr 19, 2012  452         jpiatt       Added subscription list.
 * Feb 28, 2017  6121        randerso     Cleanup
 *
 * </pre>
 *
 * @author mpduff
 */
@XmlRootElement(name = "NotificationFilter")
@XmlAccessorType(XmlAccessType.NONE)
public class NotificationFilterXML implements ISerializableObject {
    @XmlElement(name = "userFilter", type = UserFilterXML.class)
    private UserFilterXML userFilterXml;

    @XmlElements({ @XmlElement(name = "priority", type = Priority.class) })
    protected ArrayList<Priority> priorityList;

    @XmlElements({
            @XmlElement(name = "subscriptionList", type = String.class) })
    protected List<String> subscriptionList;

    /**
     * Constructor
     */
    public NotificationFilterXML() {
        priorityList = new ArrayList<>();
        for (Priority p : Priority.values()) {
            priorityList.add(p);
        }
        userFilterXml = new UserFilterXML();

        subscriptionList = new ArrayList<>();

    }

    /**
     * Get the User Filter XML file.
     *
     * @return xml file
     */
    public UserFilterXML getUserFilterXml() {
        return userFilterXml;
    }

    /**
     * Set the User Filter XML file
     *
     * @param userFilterXml
     *            the User Filter XML file
     */
    public void setUserFilterXml(UserFilterXML userFilterXml) {
        this.userFilterXml = userFilterXml;
    }

    /**
     * Get the list of Subscription Name.
     *
     * @return the subscription name list
     */
    public List<String> getSubscriptionList() {
        return subscriptionList;
    }

    /**
     * Set the list of Subscription Name.
     *
     * @param subscriptionList
     *            the Subscription Name list.
     */
    public void setsubscriptionList(List<String> subscriptionList) {
        this.subscriptionList = subscriptionList;
    }

    /**
     * Get the priority list array.
     *
     * @return the priority list
     */
    public List<Priority> getPriorityList() {
        return priorityList;
    }

    /**
     * Set the priority list.
     *
     * @param priorityList
     *            the priority list
     */
    public void setPriorityList(ArrayList<Priority> priorityList) {
        this.priorityList = priorityList;
    }

    /**
     * Add the priority.
     *
     * @param priority
     *            the priority object
     */
    public void addPriority(Priority priority) {
        priorityList.add(priority);
    }

    /**
     * Clear the priority list.
     */
    public void clearPriorityList() {
        priorityList.clear();
    }
}
