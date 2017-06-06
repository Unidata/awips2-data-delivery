package com.raytheon.uf.edex.datadelivery.retrieval.pda;

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

import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalEvent;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalRequestBuilder;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IServiceFactory;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ResponseProcessingUtilities;

/**
 * {@link IServiceFactory} implementation for PDA.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------------------------------
 * Jun 13, 2014  3120     dhladky   Initial creation
 * Sep 04, 2014  3121     dhladky   Sharpened the retrieval mechanism.
 * Sep 26, 2014  3127     dhladky   Adding geographic subsetting.
 * Dec 03, 2014  3826     dhladky   PDA test readiness
 * Jan 18, 2016  5260     dhladky   Fixes to errors found in testing.
 * May 03, 2016  5599     tjensen   Added subscription name to PDA requests
 * Sep 16, 2016  5762     tjensen   Remove Camel from FTPS calls
 * Sep 30, 2016  5762     tjensen   Improve Error Handling
 * May 22, 2017  6130     tjensen   Update to support polar products from PDA
 * Jun 06, 2017  6222     tgurney   Use token bucket to rate-limit requests
 *
 * </pre>
 *
 * @author dhladky
 */
public class PDARetrievalAdapter extends RetrievalAdapter<Time, Coverage> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDARetrievalAdapter.class);

    @Override
    public IRetrievalRequestBuilder<Time, Coverage> createRequestMessage(
            RetrievalAttribute<Time, Coverage> prxml) {

        PDARequestBuilder reqBuilder = new PDARequestBuilder(prxml,
                getProviderRetrievalXMl().getSubscriptionName());
        reqBuilder.setRequest(
                this.getProviderRetrievalXMl().getConnection().getUrl());

        return reqBuilder;
    }

    @Override
    public RetrievalResponse<Time, Coverage> performRequest(
            IRetrievalRequestBuilder<Time, Coverage> request) {

        String localFilePath = null;
        String fileName = null;

        if (request.getRequest() != null) {

            try {
                String providerName = request.getAttribute().getProvider();
                // Have to re-write the URL for the connection to the FTPS root
                localFilePath = PDAConnectionUtil.ftpsConnect(
                        this.getProviderRetrievalXMl().getConnection(),
                        providerName, request.getRequest(), getTokenBucket(),
                        getPriority());

                if (localFilePath != null) {
                    statusHandler.handle(Priority.INFO,
                            "Received file from PDA, stored to location: "
                                    + localFilePath);
                    fileName = localFilePath;
                }

            } catch (Exception e) {
                statusHandler.handle(Priority.ERROR, "FTPS error occurred!", e);
                EventBus.publish(new RetrievalEvent(e.getMessage()));
            }
        } else {
            statusHandler.handle(Priority.ERROR,
                    "Request URL for Dataset is null!");
            EventBus.publish(
                    new RetrievalEvent("Request URL for Dataset is null!"));
        }

        /*
         * We have to read in the file and store in RetrievalResponse as a
         * byte[] We have to do this in order to allow for Shared Subscription
         * delivery of PDA data which must be serialized and delivered via SBN,
         * which isn't going to give us a nice pretty file we can access.
         */

        PDARetrievalResponse pr = new PDARetrievalResponse(
                request.getAttribute());

        if (fileName != null) {
            // convert to byte[] and compress for friendly keeping
            try {
                pr.setFileBytes((ResponseProcessingUtilities
                        .getCompressedFile(fileName)));
                pr.setFileName(fileName);
                if (request instanceof PDARequestBuilder) {
                    PDARequestBuilder pdaRequest = (PDARequestBuilder) request;
                    pr.setSubName(pdaRequest.getSubName());
                }

            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Problem setting payload object for PDA!", e);
            }
        } else {
            statusHandler.handle(Priority.PROBLEM,
                    "FileName for object pulled from PDA server is null!");
            pr = null;
        }

        return pr;
    }

    @Override
    public Map<String, PluginDataObject[]> processResponse(
            IRetrievalResponse<Time, Coverage> response,
            RetrievalRequestRecord requestRecord) throws TranslationException {

        PDATranslator translator;

        try {
            translator = getPDATranslator(response.getAttribute());
        } catch (InstantiationException e) {
            throw new TranslationException(
                    "Unable to instantiate a required class!", e);
        }

        try {
            DataSet dataSet = DataDeliveryHandlers.getDataSetHandler()
                    .getByNameAndProvider(requestRecord.getDataSetName(),
                            requestRecord.getProvider());

            translator.storeAndProcess((PDARetrievalResponse) response, dataSet,
                    requestRecord.getOwner());

        } catch (ClassCastException | RegistryHandlerException e) {
            throw new TranslationException(e);
        }

        /*
         * Return an empty map, as PDA Retrieval processing is finished in
         * Ingest.
         */
        return new HashMap<>();

    }

    /**
     * @param attribute
     * @return
     */
    private static PDATranslator getPDATranslator(
            RetrievalAttribute<Time, Coverage> attribute)
            throws InstantiationException {
        return new PDATranslator(attribute);
    }

}
