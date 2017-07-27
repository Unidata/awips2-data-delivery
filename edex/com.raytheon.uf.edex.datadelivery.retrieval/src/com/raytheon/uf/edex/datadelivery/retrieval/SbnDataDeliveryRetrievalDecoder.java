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
package com.raytheon.uf.edex.datadelivery.retrieval;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.handlers.SubscriptionHandler;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.wmo.WMOMessage;
import com.raytheon.uf.common.wmo.XmlWMOMessage;
import com.raytheon.uf.edex.datadelivery.retrieval.handlers.StoreRetrievedData;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.opendap.OpenDapRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.PDARetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.wfs.WFSRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.xml.legacy.SbnRetrievalResponseXml;
import com.raytheon.uf.edex.registry.ebxml.util.RegistryIdUtil;

/**
 * Decodes data delivery retrievals from the SBN feed.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 19, 2013 1648       djohnson    Initial creation
 * Jan 30, 2014 2686       dhladky     Refactor of retrieval.
 * Jul 14, 2017 6186       rjpeter     Refactored to do processing inline
 *
 * </pre>
 *
 * @author djohnson
 */
public class SbnDataDeliveryRetrievalDecoder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final JAXBManager jaxbManager;

    private final SubscriptionHandler subscriptionHandler;

    private final StoreRetrievedData processor;

    public SbnDataDeliveryRetrievalDecoder(
            SubscriptionHandler subscriptionHandler,
            StoreRetrievedData processor) {
        this.subscriptionHandler = subscriptionHandler;
        this.processor = processor;

        try {
            this.jaxbManager = new JAXBManager(SbnRetrievalResponseXml.class,
                    OpenDapRetrievalResponse.class, WFSRetrievalResponse.class,
                    Coverage.class, PDARetrievalResponse.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Process the data.
     *
     * @param data
     *            the data
     * @param headers
     *            the headers
     */
    @SuppressWarnings("rawtypes")
    public void process(File file) {
        String subName = "UNKNOWN";

        try {
            WMOMessage message = new XmlWMOMessage(
                    Files.readAllBytes(Paths.get(file.getAbsolutePath())));

            // TODO: Handle condensed version
            SbnRetrievalResponseXml sbnRetrievalResponse = jaxbManager
                    .unmarshalFromXml(SbnRetrievalResponseXml.class,
                            message.getBodyText());
            subName = sbnRetrievalResponse.getRequestRecord()
                    .getSubscriptionName();
            Subscription sub = subscriptionHandler.getByName(subName);

            // Do we want this data?
            if (sub == null
                    || !sub.getOfficeIDs().contains(RegistryIdUtil.getId())) {
                logger.info("Skipping Shared Subscription:  " + subName
                        + " WMO Header: "
                        + message.getWmoHeader().getWmoHeader());

                // Remove the shared subscription since it wasn't for the site
                file.delete();
                return;
            }

            logger.info("Processing Shared Subscription:  " + subName
                    + " WMO Header: " + message.getWmoHeader().getWmoHeader());

            Retrieval retrieval = sbnRetrievalResponse
                    .getRetrievalRequestRecord().getRetrievalObj()
                    .asRetrieval();
            IRetrievalResponse retrievalResponse = sbnRetrievalResponse
                    .getResponseWrapper().getRetrievalResponse();

            // generate retrieval
            processor.processRetrievedData(retrieval, retrievalResponse);

            logger.info("Finished Processing Shared Subscription:  " + subName
                    + " WMO Header: " + message.getWmoHeader().getWmoHeader());
        } catch (Exception e) {
            logger.error("Couldn't process SBN shared subscription: "
                    + file.getAbsolutePath(), e);
        }
    }

}
