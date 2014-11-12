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

import java.net.URI;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.raytheon.uf.common.comm.HttpClient;
import com.raytheon.uf.common.comm.HttpClientConfigBuilder;
import com.raytheon.uf.common.comm.IHttpsCredentialsHandler;
import com.raytheon.uf.common.comm.HttpClient.HttpClientResponse;
import com.raytheon.uf.common.datadelivery.registry.Connection;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * 
 * Make requests to PDA for Data (Catalog/Subset/etc) etc.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * June 14, 2014 3120       dhladky     created.
 * Sept 27, 2014 3127       dhladky    updated deprecation methods.
 * Nov  10, 2014 3826       dhladky    Need HTTPS for connection to PDA, have to create dummy HTTPS handler for that.
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */


public class PDARequestConnectionUtil {
    
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDARequestConnectionUtil.class);
    
    protected static final String charset = "ISO-8859-1";
    
    protected static final String contentType = "text/xml";
    
    private static volatile HttpClient httpClient;
    
    /**
     * Connect to the PDA service
     * @param request (XML)
     * @param serviceURL
     * @return
     */
    public static String connect(String request, String serviceURL) {

        String xmlResponse = null;

        try {

            statusHandler.info("Connecting to: "+serviceURL);
            httpClient = getHttpClient();
            URI uri = new URI(serviceURL);
            HttpPost post = new HttpPost(uri);
            StringEntity entity = new StringEntity(request, charset);
            entity.setContentType(contentType);
            post.setEntity(entity);
            HttpClientResponse response = httpClient.executeRequest(post);
            xmlResponse = new String(response.data);

        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Couldn't connect to PDA server: " + serviceURL, e);
        }

        return xmlResponse;
    }

    /**
     * @return cached http client instance
     */
    private static HttpClient getHttpClient() {

        if (httpClient == null) {
            HttpClientConfigBuilder builder = new HttpClientConfigBuilder();
            // accept gzipped data for PDA
            builder.setHandlingGzipResponses(true);
            builder.setHttpsHandler(dummyCredentialsHandler);
            httpClient = new HttpClient(builder.build());
        }

        return httpClient;
    }
    
    private static final IHttpsCredentialsHandler dummyCredentialsHandler = new IHttpsCredentialsHandler() {

        @Override
        public String[] getCredentials(String host, int port, String authValue) {

            // We don't check anything, we just need it to create an HTTPS connection.
            String[] rval = null;
            return rval;
        }

        @Override
        public void credentialsFailed() {
            statusHandler
                    .error("Dummy cedentials handler failed to authenticate!");
        }

    };

}
