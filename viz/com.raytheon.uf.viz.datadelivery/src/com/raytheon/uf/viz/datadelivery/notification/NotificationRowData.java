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
package com.raytheon.uf.viz.datadelivery.notification;

import java.util.Date;

import com.raytheon.uf.viz.datadelivery.common.ui.ITableData;

/**
 * Data object for a row in the Notification Dialog.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 3, 2012            mpduff     Initial creation
 * Jun 07, 2012   687     lvenable   Table data refactor.
 * Aug 30, 2013  2314     mpduff     Fix formatting.
 * Sep 16, 2013  2375     mpduff     Change date sort order.
 * Feb 07, 2014  2453     mpduff     Added toString()
 * Dec 03, 2014  3840     ccody      Correct sorting "contract violation" issue
 * Oct 04, 2017  6470     mapeters   Remove unused columns field
 *
 * </pre>
 *
 * @author mpduff
 */

public class NotificationRowData implements ITableData {

    /** Notification Identification number */
    private int id;

    /** Date */
    private Date date;

    /** Priority of notification */
    private int priority;

    /** Notification Category */
    private String category;

    /** User tied to the Notification */
    private String user;

    /** Notification Message */
    private String message;

    /**
     * Constructor.
     *
     */
    public NotificationRowData() {
    }

    /**
     * Get the notification identity.
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Set the notification identity.
     *
     * @param id
     *            the notification identity
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Get the notification date.
     *
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Set the notification date.
     *
     * @param date
     *            the notification date
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * Get the notification priority.
     *
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Set the notification priority.
     *
     * @param priority
     *            the notification priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Get the notification category.
     *
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Set the notification category.
     *
     * @param category
     *            the notification category
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Get the notification user name.
     *
     * @return the user name
     */
    public String getUser() {
        return user;
    }

    /**
     * Set the notification user.
     *
     * @param user
     *            the notification user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Get the notification message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the notification message.
     *
     * @param message
     *            the notification message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return this.date.toString() + " - " + this.message;
    }
}
