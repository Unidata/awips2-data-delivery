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

import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.LevelGroup;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.ProviderType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.registry.handlers.SubscriptionHandler;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval.SubscriptionType;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.wmo.WMOMessage;
import com.raytheon.uf.common.wmo.XmlWMOMessage;
import com.raytheon.uf.edex.datadelivery.retrieval.handlers.StoreRetrievedData;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.opendap.OpenDapRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.PDARetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.wfs.WFSRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.xml.SbnRetrievalInfoXml;
import com.raytheon.uf.edex.datadelivery.retrieval.xml.legacy.SbnRetrievalResponseXml;
import com.raytheon.uf.edex.registry.ebxml.util.RegistryIdUtil;

/**
 * Decodes data delivery retrievals from the SBN feed.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Mar 19, 2013  1648     djohnson  Initial creation
 * Jan 30, 2014  2686     dhladky   Refactor of retrieval.
 * Jul 14, 2017  6186     rjpeter   Refactored to do processing inline
 * Nov 15, 2017  6498     tjensen   Added support for simplified
 *                                  SbnRetrievalInfoXml
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
                    SbnRetrievalInfoXml.class, OpenDapRetrievalResponse.class,
                    WFSRetrievalResponse.class, Coverage.class,
                    PDARetrievalResponse.class, LevelGroup.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Decodes and extracts data from a file sent over the SBN for a shared
     * subscription retrieval. Once extracted, the retrieval information is then
     * sent to be stored.
     *
     * @param file
     *            file containing the data, with necessary headers and retrieval
     *            information
     *
     */
    @SuppressWarnings("rawtypes")
    public void process(File file) {
        String subName = "UNKNOWN";

        try {
            WMOMessage message = new XmlWMOMessage(
                    Files.readAllBytes(Paths.get(file.getAbsolutePath())));
            Subscription sub = null;
            Retrieval retrieval = null;
            IRetrievalResponse retrievalResponse = null;
            String wmoHeader = message.getWmoHeader().getWmoHeader();
            String sbnXmlString = message.getBodyText();

            /*
             * Handle the SbnRetrievalRepsonseXml objects. These are sent for
             * versions previous to 18.1.1. Once all sites are upgraded to
             * 18.1.1 or beyond, the central registry can be configured to send
             * simplified SbnRetrievalInfoXml instead.
             */
            if (sbnXmlString.substring(0, 100)
                    .contains("sbnRetrievalResponseXml")) {
                SbnRetrievalResponseXml sbnRetrievalResponse = jaxbManager
                        .unmarshalFromXml(SbnRetrievalResponseXml.class,
                                sbnXmlString);
                subName = sbnRetrievalResponse.getRequestRecord()
                        .getSubscriptionName();
                sub = subscriptionHandler.getByName(subName);

                if (!isSubForSite(sub, subName, wmoHeader)) {
                    // Remove since it wasn't for the site
                    file.delete();
                    return;
                }
                logger.info("Processing Shared Subscription:  " + subName
                        + " WMO Header: " + wmoHeader);

                retrieval = sbnRetrievalResponse.getRetrievalRequestRecord()
                        .getRetrievalObj().asRetrieval();
                retrievalResponse = sbnRetrievalResponse.getResponseWrapper()
                        .getRetrievalResponse();
            } else {
                SbnRetrievalInfoXml sbnRetrievalInfo = jaxbManager
                        .unmarshalFromXml(SbnRetrievalInfoXml.class,
                                sbnXmlString);
                String subscriptionId = sbnRetrievalInfo.getSubscriptionId();
                sub = subscriptionHandler.getById(subscriptionId);

                if (!isSubForSite(sub, subscriptionId,
                        wmoHeader)) {
                    // Remove since it wasn't for the site
                    file.delete();
                    return;
                }
                subName = sub.getName();

                logger.info("Processing Shared Subscription:  " + subName
                        + " WMO Header: " + wmoHeader);
                retrieval = generateRetrieval(sub, sbnRetrievalInfo.getUrl(),
                        sbnRetrievalInfo.getServiceType(),
                        sbnRetrievalInfo.getAttribute());
                retrievalResponse = sbnRetrievalInfo.getRetrievalResponse();
            }

            // generate retrieval
            processor.processRetrievedData(retrieval, retrievalResponse);

            logger.info("Finished Processing Shared Subscription:  " + subName
                    + " WMO Header: " + wmoHeader);
        } catch (Exception e) {
            logger.error("Couldn't process SBN shared subscription: "
                    + file.getAbsolutePath(), e);
        }
    }

    private boolean isSubForSite(Subscription sub,
            String subString, String wmoHeader) {
        // Do we want this data?
        if (sub == null
                || !sub.getOfficeIDs().contains(RegistryIdUtil.getId())) {
            logger.debug("Skipping Shared Subscription:  " + subString
                    + " WMO Header: " + wmoHeader);
            return false;
        }
        return true;
    }

    private Retrieval generateRetrieval(Subscription sub, String url,
            ServiceType serviceType, RetrievalAttribute att)
            throws RegistryHandlerException {
        Retrieval retrieval = new Retrieval<>();
        retrieval.setSubscriptionName(sub.getName());
        retrieval.setServiceType(serviceType);
        retrieval.setUrl(url);
        retrieval.setOwner(sub.getOwner());
        if (sub instanceof AdhocSubscription) {
            retrieval.setSubscriptionType(SubscriptionType.AD_HOC);
        } else {
            retrieval.setSubscriptionType(SubscriptionType.SUBSCRIBED);
        }
        retrieval.setNetwork(sub.getRoute());
        retrieval.setDataSetName(sub.getDataSetName());

        String providerName = sub.getProvider();
        Provider provider = DataDeliveryHandlers.getProviderHandler()
                .getByName(providerName);
        // Look up the provider's configured plugin for this data type
        ProviderType providerType = provider
                .getProviderType(sub.getDataSetType());
        retrieval.setProvider(providerName);
        retrieval.setPlugin(providerType.getPlugin());
        retrieval.setAttribute(att);

        return retrieval;
    }

}
