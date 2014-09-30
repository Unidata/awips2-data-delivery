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
package com.raytheon.uf.edex.datadelivery.event.handler;

import java.util.Calendar;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.raytheon.uf.common.datadelivery.event.notification.NotificationRecord;
import com.raytheon.uf.common.datadelivery.event.status.DataDeliverySystemStatus;
import com.raytheon.uf.common.datadelivery.event.status.DataDeliverySystemStatusDefinition;
import com.raytheon.uf.common.datadelivery.event.status.DataDeliverySystemStatusId;
import com.raytheon.uf.common.datadelivery.event.status.DataDeliverySystemStatusEvent;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.EdexException;
import com.raytheon.uf.edex.datadelivery.event.dao.DataDeliverySystemStatusDao;

/**
 * Event bus message handler class.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 20, 2013   1655     mpduff      Initial creation.
 * Oct 03, 2014   2749     ccody       Add code to send event in addition to storing it
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */
@Service
@Transactional
public class DataDeliverySystemStatusEventHandler {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DataDeliverySystemStatusEventHandler.class);

    private String uri;

    /** The Data Access Object */
    private DataDeliverySystemStatusDao statusDao;

    /**
     * Gets called when Event Bus publishes a DataDeliverySystemStatus event
     * 
     * @param event
     *            EventBus's
     */
    @Subscribe
    @AllowConcurrentEvents
    public void eventListener(DataDeliverySystemStatusEvent event) {
        DataDeliverySystemStatus ddssRecord = new DataDeliverySystemStatus();
        DataDeliverySystemStatusId id = new DataDeliverySystemStatusId();
        id.setName(event.getName());
        id.setSystemType(event.getSystemType());
        DataDeliverySystemStatusDefinition ddsdStatus = event.getStatus();
        String status = ddsdStatus.getStatus();
        ddssRecord.setStatus(status);
        ddssRecord.setKey(id);
        storeAndSend(ddssRecord, uri);
    }

    /**
     * Stores the record in the notification table.
     * 
     * @param record
     */
    protected void storeAndSend(DataDeliverySystemStatus ddssRecord,
            String endpoint) {
        statusDao.createOrUpdate(ddssRecord);

        DataDeliverySystemStatusId id = ddssRecord.getKey();
        String systemType = id.getSystemType();
        NotificationRecord notificationRecord = new NotificationRecord();
        notificationRecord.setDate(Calendar.getInstance());
        notificationRecord.setCategory(systemType);
        notificationRecord.setUsername("");
        notificationRecord.setPriority(1);

        String ddssName = id.getName();
        String ddssRecordStatus = ddssRecord.getStatus();
        notificationRecord.setMessage("Bandwidth Manager "+ ddssName + " System Event " + ddssRecordStatus);

        send(notificationRecord, endpoint);
    }

    /**
     * Sends the object to 'notify.msg' JMS endpoint.
     * 
     * Serialize given object (NotificationRecord) and send through JMS to given
     * endpoint (notify.msg).
     * 
     * @param obj
     */
    public void send(Object obj, String endpoint) {
        try {
            byte[] bytes = SerializationUtil.transformToThrift(obj);
            EDEXUtil.getMessageProducer().sendAsyncUri(endpoint, bytes);
        } catch (EdexException e) {
            statusHandler.error("Error sending record to " + endpoint, e);
        } catch (SerializationException e) {
            statusHandler.error("Error serializing record to " + endpoint, e);
        }
    }

    /**
     * @return the statusDao
     */
    public DataDeliverySystemStatusDao getStatusDao() {
        return statusDao;
    }

    /**
     * @param statusDao
     *            the statusDao to set
     */
    public void setStatusDao(DataDeliverySystemStatusDao statusDao) {
        this.statusDao = statusDao;
    }
    
    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri
     *            the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }
    
}
