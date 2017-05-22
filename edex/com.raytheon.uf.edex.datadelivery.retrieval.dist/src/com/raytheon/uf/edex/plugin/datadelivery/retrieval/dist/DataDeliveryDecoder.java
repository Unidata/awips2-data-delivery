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
package com.raytheon.uf.edex.plugin.datadelivery.retrieval.dist;

import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.event.notification.NotificationRecord;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.EdexException;
import com.raytheon.uf.edex.datadelivery.event.handler.NotificationHandler;
import com.raytheon.uf.edex.datadelivery.retrieval.DecodeInfo;

/**
 *
 * Receives files from Data Delivery and routes them to the proper decoders
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 17, 2017  6130     tjensen   Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
public class DataDeliveryDecoder {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private String notificationEndpoint;

    public DataDeliveryDecoder() {

    }

    public DataDeliveryDecoder(String endpoint) {
        notificationEndpoint = endpoint;
    }

    public void process(DecodeInfo info) {
        String pathToFile = info.getPathToFile();
        logger.info("Processing " + pathToFile + " for subscription "
                + info.getSubscriptionName() + "...");
        String routeId = info.getRouteId();
        try {
            // Send data to correct route to be decoded
            PluginDataObject[] pdos = (PluginDataObject[]) EDEXUtil
                    .getMessageProducer().sendSync(routeId, pathToFile);

            int priority = 3;
            StringBuilder sb = new StringBuilder();

            // If no pdos returned, decoding failed. Else success.
            if (pdos != null && pdos.length > 0) {
                sb.append("Successfully retrieved and stored data for ");
            } else {
                sb.append("Failed data retrieval for ");
                priority = 1;
            }
            sb.append(info.getSubscriptionName());

            // Send results to notifications
            NotificationRecord record = new NotificationRecord();
            record.setDate(Calendar.getInstance());
            record.setPriority(priority);
            record.setCategory(info.getSubscriptionName());
            record.setUsername(info.getSubscriptionOwner());
            record.setMessage(sb.toString());

            NotificationHandler noteHandler = new NotificationHandler();
            noteHandler.storeAndSend(record, notificationEndpoint);
        } catch (EdexException e) {
            logger.error("Failed to route file [" + pathToFile
                    + "] into to decoder '" + routeId + "'", e);
        }

    }

    public String getNotificationEndpoint() {
        return notificationEndpoint;
    }

    public void setNotificationEndpoint(String notificationEndpoint) {
        this.notificationEndpoint = notificationEndpoint;
    }
}
