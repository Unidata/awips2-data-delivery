package com.raytheon.uf.viz.datadelivery.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.raytheon.uf.common.datadelivery.event.notification.DeleteNotificationRequest;
import com.raytheon.uf.common.datadelivery.event.notification.DeleteNotificationResponse;
import com.raytheon.uf.common.datadelivery.event.notification.GetNotificationRequest;
import com.raytheon.uf.common.datadelivery.event.notification.NotificationRecord;
import com.raytheon.uf.common.datadelivery.request.DataDeliveryConstants;
import com.raytheon.uf.common.serialization.comm.RequestRouter;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.notification.INotificationObserver;
import com.raytheon.uf.viz.core.notification.NotificationException;
import com.raytheon.uf.viz.core.notification.NotificationMessage;
import com.raytheon.uf.viz.datadelivery.notification.xml.MessageLoadXML;

/**
 * 
 * Manages the retrieval of current and arriving notification records
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 12, 2012            jsanchez     Initial creation
 * Jan 22, 2013 1501       djohnson     Route requests to datadelivery.
 * Sep 05, 2013 2314       mpduff       support the load all messages option.
 * Feb 07, 2014 2453       mpduff       Remove username query param.
 * 
 * </pre>
 * 
 * @author jsanchez
 * @version 1.0
 */
public class NotificationHandler implements INotificationObserver {

    public static interface INotificationArrivedListener {
        public void handleNotification(
                ArrayList<NotificationRecord> notificationRecords);

        public void deleteNotification(ArrayList<Integer> ids);
    }

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(NotificationHandler.class);

    private static Set<INotificationArrivedListener> listeners = new CopyOnWriteArraySet<INotificationArrivedListener>();

    /**
     * Add a notifications arrived listener, listeners will get notified when
     * new notifications have been ingested in edex
     * 
     * @param listener
     */
    public static void addListener(INotificationArrivedListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove the listener
     * 
     * @param listener
     */
    public static void removeListener(INotificationArrivedListener listener) {
        listeners.remove(listener);
    }

    /**
     * Request the notifications from the notification database.Reads in the
     * default configuration.
     * 
     * @param messageLoad
     *            messages to load
     * @param users
     *            user list
     * @return List of notification records
     */
    @SuppressWarnings("unchecked")
    public List<NotificationRecord> intialLoad(MessageLoadXML messageLoad,
            ArrayList<String> users) {
        int loadAmount;
        Integer hours = null;
        Integer maxResults = null;
        boolean loadAll = false;
        // Retrieve the message load configuration
        if (messageLoad != null) {
            loadAll = messageLoad.isLoadAllMessages();
            loadAmount = messageLoad.getLoadLast();

            if (messageLoad.isNumHours()) {
                hours = loadAmount;
            } else if (messageLoad.isNumMessages()) {
                maxResults = loadAmount;
            }
        }

        // Request data from the notification table.
        // Note: Removing username query parameter for front end label
        // consistency
        try {
            GetNotificationRequest request = new GetNotificationRequest();
            request.setHours(hours);
            request.setMaxResults(maxResults);
            request.setLoadAll(loadAll);
            ArrayList<NotificationRecord> response = (ArrayList<NotificationRecord>) RequestRouter
                    .route(request, DataDeliveryConstants.DATA_DELIVERY_SERVER);
            return response;
        } catch (Exception e) {
            statusHandler.error(
                    "Error trying to retrieve notifications from database", e);
        }

        return new ArrayList<NotificationRecord>();
    }

    /**
     * Deletes records from the notification table
     * 
     * @param ids
     *            The record ids to be deleted
     * @return the number of rows deleted from the table
     */
    public int delete(ArrayList<Integer> ids) {
        int rowsDeleted = 0;

        try {
            DeleteNotificationRequest request = new DeleteNotificationRequest();
            request.setIds(ids);
            DeleteNotificationResponse response = (DeleteNotificationResponse) RequestRouter
                    .route(request, DataDeliveryConstants.DATA_DELIVERY_SERVER);
            rowsDeleted = response.getRowsDeleted();
        } catch (Exception e) {
            statusHandler.error(
                    "Error trying to delete notification(s) from database", e);
        }

        return rowsDeleted;
    }

    /**
     * Processes an arriving NotificationRecord and notifies all listeners (i.e.
     * dialog)
     * 
     * @param messages
     *            The array of messages being sent from 'notify.msg'
     */
    @Override
    @SuppressWarnings("unchecked")
    public void notificationArrived(NotificationMessage[] messages) {
        ArrayList<Integer> deleteRecordIds = new ArrayList<Integer>();
        ArrayList<NotificationRecord> notificationRecords = new ArrayList<NotificationRecord>();

        try {
            for (NotificationMessage msg : messages) {
                Object obj = msg.getMessagePayload();
                if (obj instanceof NotificationRecord) {
                    notificationRecords.add((NotificationRecord) obj);
                } else if (obj instanceof DeleteNotificationResponse) {
                    DeleteNotificationResponse response = (DeleteNotificationResponse) obj;
                    deleteRecordIds.addAll(response.getIds());
                }
            }
        } catch (NotificationException e) {
            statusHandler.error("Error when receiving notification", e);
        }

        if (notificationRecords.isEmpty() == false) {
            for (INotificationArrivedListener listener : listeners) {
                listener.handleNotification(notificationRecords);
            }
        }

        if (deleteRecordIds.isEmpty() == false) {
            for (INotificationArrivedListener listener : listeners) {
                listener.deleteNotification(deleteRecordIds);
            }
        }
    }
}
