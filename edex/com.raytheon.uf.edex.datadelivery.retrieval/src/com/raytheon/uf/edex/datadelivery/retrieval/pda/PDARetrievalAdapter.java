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
package com.raytheon.uf.edex.datadelivery.retrieval.pda;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalRequestBuilder;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IServiceFactory;

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
 * Jun 23, 2017  6322     tgurney   performRequest() throws Exception
 * Jun 29, 2017  6130     tjensen   Add support for local testing
 * Jul 25, 2017  6186     rjpeter   Use Retrieval
 *
 * </pre>
 *
 * @author dhladky
 */
public class PDARetrievalAdapter extends RetrievalAdapter<Time, Coverage> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public IRetrievalRequestBuilder<Time, Coverage> createRequestMessage(
            Retrieval<Time, Coverage> retrieval) {
        return new PDARequestBuilder(retrieval);
    }

    @Override
    public PDARetrievalResponse performRequest(
            Retrieval<Time, Coverage> retrieval,
            IRetrievalRequestBuilder<Time, Coverage> request) throws Exception {

        String localFilePath = null;
        String fileName = null;
        String url = request.getRequest();

        if (url != null && !url.isEmpty()) {
            try {
                String providerName = retrieval.getProvider();
                if (Boolean
                        .parseBoolean(System.getProperty("LOCAL_DATA_TEST"))) {
                    // Find file in Local Test dir
                    String[] remotePathAndFile = PDAConnectionUtil
                            .separateRemoteFileDirectoryAndFileName(url);
                    fileName = System.getProperty("LOCAL_DATA_DIR")
                            + File.separator + remotePathAndFile[1];

                } else {
                    /*
                     * Have to re-write the URL for the connection to the FTPS
                     * root
                     */
                    localFilePath = PDAConnectionUtil.ftpsConnect(providerName,
                            url, getTokenBucket(), getPriority());

                    if (localFilePath != null) {
                        logger.info(
                                "Received file from PDA, stored to location: "
                                        + localFilePath);
                        fileName = localFilePath;
                    }
                }
            } catch (Exception e) {
                throw new Exception("FTPS error occurred", e);
            }
        } else {
            throw new Exception("FTPS URL for DataSet is empty");
        }

        if (fileName == null) {
            throw new Exception(
                    "Filename for object pulled from PDA server is null!");
        }

        File pdaFile = new File(fileName);
        if (!pdaFile.exists()) {
            throw new Exception(
                    "File downloaded from PDA server does not exist: "
                            + fileName);
        }

        if (pdaFile.length() == 0) {
            throw new Exception(
                    "File downloaded from PDA server is empty: " + fileName);
        }

        PDARetrievalResponse rval = new PDARetrievalResponse();
        rval.setFileName(fileName);

        // generate statistic
        generateRetrievalEvent(retrieval, pdaFile.length());
        return rval;
    }

    @Override
    public Map<String, PluginDataObject[]> processResponse(
            Retrieval<Time, Coverage> retrieval, IRetrievalResponse response)
            throws TranslationException {
        PDATranslator translator;

        try {
            translator = getPDATranslator(retrieval);
        } catch (InstantiationException e) {
            throw new TranslationException(
                    "Unable to instantiate a required class!", e);
        }

        try {
            DataSet dataSet = DataDeliveryHandlers.getDataSetHandler()
                    .getByNameAndProvider(retrieval.getDataSetName(),
                            retrieval.getProvider());

            translator.storeAndProcess((PDARetrievalResponse) response,
                    dataSet);
        } catch (ClassCastException | RegistryHandlerException e) {
            throw new TranslationException(e);
        }

        /*
         * Return an empty map, as PDA Retrieval processing is finished in
         * Ingest.
         */
        return Collections.emptyMap();
    }

    /**
     * @param retrieval
     * @return
     */
    private static PDATranslator getPDATranslator(
            Retrieval<Time, Coverage> retrieval) throws InstantiationException {
        return new PDATranslator(retrieval);
    }

}
