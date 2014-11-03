package com.raytheon.uf.edex.datadelivery.event.handler;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raytheon.uf.common.datadelivery.event.notification.NotificationRecord;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.EdexException;
import com.raytheon.uf.edex.datadelivery.event.notification.NotificationDao;

/**
 * 
 * Abstract class to provide the send and store capabilities to subclasses.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 3/18/2013    1802       bphillip    Implemented transactional boundaries
 * Oct 28, 2014 2748       ccody       Add notification event for Subscription modifications.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
@Service
@Transactional
public abstract class AbstractHandler {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractHandler.class);

    protected NotificationDao notificationDao;

    protected AbstractHandler() {

    }

    /**
     * Sends the object to 'notifyRoute'.
     * 
     * @param obj
     */
    public void send(Object obj, String endpoint) {
        if (obj != null) {
            try {
                byte[] bytes = SerializationUtil.transformToThrift(obj);
                EDEXUtil.getMessageProducer().sendAsyncUri(endpoint, bytes);
            } catch (EdexException e) {
                statusHandler.error("Error sending record to " + endpoint, e);
            } catch (SerializationException e) {
                statusHandler.error("Error serializing record to " + endpoint,
                        e);
            }
        } else {
            statusHandler.error("Unable to serialize null object to "
                    + endpoint);

        }

    }

    /**
     * Stores the record in the notification table.
     * 
     * @param record
     */
    void storeAndSend(NotificationRecord record, String endpoint) {
        if (record != null) {
            store(record);
            send(record, endpoint);
        }
    }

    /**
     * Stores the record in the notification table.
     * 
     * @param record
     */
    void store(NotificationRecord record) {
        if (record != null) {
            try {
                notificationDao.createOrUpdate(record);
            } catch (Exception e) {
                statusHandler.error("Unable to store NotificationRecord ", e);
            }
        }
    }

    public void setNotificationDao(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

}
