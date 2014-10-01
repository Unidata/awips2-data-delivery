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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalEvent;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalRequestBuilder;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IServiceFactory;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.PDARetrievalResponse.FILE;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ResponseProcessingUtilities;


/**
 * {@link IServiceFactory} implementation for PDA.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 13, 2014 3120       dhladky     Initial creation
 * Sept 04, 2014 3121      dhladky     Sharpened the retrieval mechanism.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */
public class PDARetrievalAdapter extends RetrievalAdapter<Time, Coverage> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDARetrievalAdapter.class);
    
    private static final String fileExtension = ".nc";
    
    @Override
    public IRetrievalRequestBuilder<Time, Coverage> createRequestMessage(
            RetrievalAttribute<Time, Coverage> prxml) {
        // this is actually pretty useless right now, 
        // it doesn't really create anything because PDA is very limited.
        // Right now all we need is the filepath URL from the metadata object
        PDARequestBuilder reqBuilder = new PDARequestBuilder(prxml);
        reqBuilder.setRequest(this.getProviderRetrievalXMl().getConnection().getUrl());
        return reqBuilder;
    }

    @Override
    public RetrievalResponse<Time, Coverage> performRequest(IRetrievalRequestBuilder<Time, Coverage> request) {
        
        String localFilePath = null;
        File directory = null;
        String fileName = null;
        
        try {
            //TODO THIS IS ONLY DONE UNTIL WE HAVE REAL REQUEST SWITCHES
            // Right now all we have is an FTP(S) filepath for each type.
            // So we take that path and set it as the request.  Simple.
            String providerName = request.getAttribute().getProvider();
            localFilePath = PDAConnectionUtil.ftpsConnect(this.getProviderRetrievalXMl().getConnection(),
                    providerName, request.getRequest());
            
            // camel embeds the file in a directory of same name, extract.
            if (localFilePath != null) {
                directory = new File(localFilePath);
                if (directory.isDirectory()) {
                    for (File file : directory.listFiles()) {
                        if (file.getName().endsWith(fileExtension)) {
                            // you will only ever find one file with .nc extension.
                            fileName = file.getAbsolutePath();
                            break;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR, e.getLocalizedMessage(), e);
            EventBus.publish(new RetrievalEvent(e.getMessage()));
        }

        // We have to read in the file and store in RetrievalResponse as a byte[]
        // We have to do this in order to allow for Shared Subscription delivery of 
        // PDA data which must be serialized and delivered via SBN, which isn't going
        // to give us a nice pretty file we can access.
         PDARetrievalResponse pr = new PDARetrievalResponse(
                request.getAttribute());

        if (fileName != null) {
            // convert to byte[] and compress for friendly keeping
            try {
                pr.setFileBytes((ResponseProcessingUtilities
                        .getCompressedFile(fileName)));
                pr.setFileName(fileName);

            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Problem setting payload object for PDA!", e);
            }
        }
       
        return pr;
    }

    @Override
    public Map<String, PluginDataObject[]> processResponse(

            IRetrievalResponse<Time, Coverage> response) throws TranslationException {

        Map<String, PluginDataObject[]> map = new HashMap<String, PluginDataObject[]>();
        PDATranslator translator;
        try {
            translator = getPDATranslator(response.getAttribute());
        } catch (InstantiationException e) {
            throw new TranslationException(
                    "Unable to instantiate a required class!", e);
        }

        try {
            
            @SuppressWarnings("unchecked")
            Map<FILE, Object> payload = (Map<FILE, Object>) response.getPayLoad();
            
            if (payload != null) {
                PluginDataObject[] pdos = translator
                        .asPluginDataObjects(payload);

                if (!CollectionUtil.isNullOrEmpty(pdos)) {
                    String pluginName = pdos[0].getPluginName();
                    map.put(pluginName,
                            CollectionUtil.combine(PluginDataObject.class,
                                    map.get(pluginName), pdos));
                }
            }
        } catch (ClassCastException e) {
            throw new TranslationException(e);
        }

        return map;

    }

    /**
     * @param attribute
     * @return
     */
    PDATranslator getPDATranslator(RetrievalAttribute<Time, Coverage> attribute)
            throws InstantiationException {
        return new PDATranslator(attribute);
    }

}
