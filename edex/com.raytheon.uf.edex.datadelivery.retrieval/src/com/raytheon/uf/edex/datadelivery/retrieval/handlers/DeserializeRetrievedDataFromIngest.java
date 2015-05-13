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
package com.raytheon.uf.edex.datadelivery.retrieval.handlers;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.handlers.ISubscriptionHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.SubscriptionHandler;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.wmo.WMOMessage;
import com.raytheon.uf.common.wmo.XmlWMOMessage;
import com.raytheon.uf.edex.datadelivery.retrieval.opendap.OpenDapRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.PDARetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.wfs.WFSRetrievalResponse;
import com.raytheon.uf.edex.registry.ebxml.util.RegistryIdUtil;

/**
 * Deserializes the retrieved data in a retrievalQueue.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 01, 2013 1543       djohnson     Initial creation
 * Mar 05, 2013 1647       djohnson     Remove WMO header.
 * Mar 19, 2013 1794       djohnson     Read from a queue rather than the file system.
 * Oct 04, 2013 2267       bgonzale     Added WfsRetrieval to unmarshal classes.
 * Nov 04, 2013 2506       bgonzale     Added SbnRetrievalResponseXml to unmarshal classes.
 *                                      Trim content after last xml tag during 
 *                                      unmarshalling from xml.
 * Jan 30, 2014 2686       dhladky      refactor of retrieval.
 * May 14, 2014 2536       bclement     moved WMO Header to common
 * Feb 02, 2014 4064       dhladky      Filter SBN deliveries for Subscriptions local registry is subscribed too.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
public class DeserializeRetrievedDataFromIngest implements IRetrievalsFinder {

    private final JAXBManager jaxbManager;
    
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DeserializeRetrievedDataFromIngest.class);

    private final ISubscriptionHandler subscriptionHandler;
    
    public static final String UNKNOWN_SUBSCRIPTION = "UNKNOWN SUBSCRIPTION";
        
    /**
     * @param subscriptionHandler
     */
    public DeserializeRetrievedDataFromIngest(
            SubscriptionHandler subscriptionHandler) {

        this.subscriptionHandler = subscriptionHandler;

        try {
            this.jaxbManager = new JAXBManager(RetrievalResponseXml.class,
                    SbnRetrievalResponseXml.class,
                    OpenDapRetrievalResponse.class, WFSRetrievalResponse.class,
                    Coverage.class, PDARetrievalResponse.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public RetrievalResponseXml processRequest(RetrievalRequestWrapper rrw)
            throws Exception {

        String xml = (String) rrw.getPayload();

        if (xml == null) {
            return null;
        } else {
         
            String subName = UNKNOWN_SUBSCRIPTION;
            
            try {

                WMOMessage message = new XmlWMOMessage(xml.getBytes());
                RetrievalResponseXml retrievalResponse = (RetrievalResponseXml) jaxbManager
                        .unmarshalFromXml(message.getBodyText());
                subName = retrievalResponse.getRequestRecord()
                        .getSubscriptionName();
                Subscription sub = subscriptionHandler.getByName(subName);

                // Do we want this data?
                if (sub != null
                        && sub.getOfficeIDs().contains(RegistryIdUtil.getId())) {
                    
                    statusHandler.info("Delivering Subscription:  " + subName
                            + " WMO Header: "
                            + message.getWmoHeader().getWmoHeader());

                    return retrievalResponse;
                }

            } catch (Exception e) {
                statusHandler.error(
                        "Unable to deliver subscription data! "
                                + subName, e);
            }

            return null;
        }
    }

}
