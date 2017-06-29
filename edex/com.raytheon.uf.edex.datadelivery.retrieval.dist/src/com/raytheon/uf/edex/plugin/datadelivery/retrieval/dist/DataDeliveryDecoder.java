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
import java.util.Map;

import org.apache.camel.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.event.notification.NotificationRecord;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.EdexException;

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

    private String notificationRoute;

    public DataDeliveryDecoder() {

    }

    public DataDeliveryDecoder(String route) {
        notificationRoute = route;
    }

    public void process(byte[] infoBytes, @Headers Map<String, Object> header)
            throws SerializationException {
        DecodeInfo info = SerializationUtil
                .transformFromThrift(DecodeInfo.class, infoBytes);
        String pathToFile = info.getPathToFile();
        logger.info("Processing " + pathToFile + " for subscription "
                + info.getSubscriptionName() + "...");
        String routeId = info.getRouteId();
        try {
            // populate header info for logging purposes
            header.put("dataType", info.getDataType());
            header.put("ingestFileName", pathToFile);
            header.put("enqueueTime", info.getEnqueTime());

            // Send data to correct route to be decoded
            int numPdos = (int) EDEXUtil.getMessageProducer().sendSync(routeId,
                    pathToFile);

            // If no pdos returned, decoding failed. Else success.
            if (numPdos <= 0) {
                StringBuilder sb = new StringBuilder();

                sb.append("Failed data retrieval for ");
                int priority = 1;
                sb.append(info.getSubscriptionName());

                // Send results to notifications
                NotificationRecord record = new NotificationRecord();
                record.setDate(Calendar.getInstance());
                record.setPriority(priority);
                record.setCategory(info.getSubscriptionName());
                record.setUsername(info.getSubscriptionOwner());
                record.setMessage(sb.toString());

                EDEXUtil.getMessageProducer().sendSync(notificationRoute,
                        record);
            } else {
                logger.info("Successfully retrieved and stored data for "
                        + info.getSubscriptionName());
            }

        } catch (EdexException e) {
            logger.error("Failed to route file [" + pathToFile
                    + "] into to decoder '" + routeId + "'", e);
        }

    }

    public String getNotificationRoute() {
        return notificationRoute;
    }

    public void setNotificationRoute(String notificationRoute) {
        this.notificationRoute = notificationRoute;
    }
}
