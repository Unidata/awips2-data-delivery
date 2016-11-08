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
package com.raytheon.uf.viz.datadelivery.comm;

import com.raytheon.uf.common.jms.notification.NotificationMessage;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.IConditionMatcher;

/**
 * Checks for a notification message to contain one of the specified types of
 * payload.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 02, 2013 1841      djohnson     Initial creation
 * Oct 28, 2014 3769      ccody        Evaluate non-deprecated com.raytheon.uf.common.jms.notification.NotificationMessage
 * Nov 08, 2016 5976       bsteffen    Move to data delivery plugin
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
public class NotificationMessageContainsType
        implements IConditionMatcher<NotificationMessage[]> {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(NotificationMessageContainsType.class);

    private final Class<?>[] classTypes;

    /**
     * Constructor.
     * 
     * @param classTypes
     *            the class types to check for, if the notification message
     *            contains any of the types then true will be returned
     */
    public NotificationMessageContainsType(Class<?>... classTypes) {
        this.classTypes = classTypes;
    }

    @Override
    public boolean matchesCondition(NotificationMessage[] item) {
        try {
            for (com.raytheon.uf.common.jms.notification.NotificationMessage msg : item) {
                Object obj = msg.getMessagePayload();
                for (Class<?> classType : classTypes) {
                    if (classType.isAssignableFrom(obj.getClass())) {
                        return (true);
                    }
                }
            }
        } catch (com.raytheon.uf.common.jms.notification.NotificationException e) {
            statusHandler.error("Error when checking notification", e);
        }
        return (false);
    }
}
