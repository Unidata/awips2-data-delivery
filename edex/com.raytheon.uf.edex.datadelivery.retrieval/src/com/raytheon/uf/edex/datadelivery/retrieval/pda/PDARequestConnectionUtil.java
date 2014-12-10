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
import java.security.KeyStore;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.raytheon.uf.common.comm.HttpClient;
import com.raytheon.uf.common.comm.HttpClient.HttpClientResponse;
import com.raytheon.uf.common.comm.HttpClientConfigBuilder;
import com.raytheon.uf.common.comm.HttpsUtils;
import com.raytheon.uf.common.comm.IHttpsHandler;
import com.raytheon.uf.common.datadelivery.registry.Connection;
import com.raytheon.uf.common.datadelivery.registry.ProviderCredentials;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ProviderCredentialsUtil;

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
 * Nov  15, 2014 3757       dhladky    More General HTTPS configuration
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
    
    /** trustore path set by Data Delivery in the serviceConfig XML */
    protected static final String truststore = "TRUSTSTORE_FILE";
    
    protected static final String storeType = "pkcs12";
    
    protected static final String providerName = "PDA";
    
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
            builder.setHttpsHandler(httpsHandler);
            httpClient = new HttpClient(builder.build());
        }

        return httpClient;
    }
    
    private static final IHttpsHandler httpsHandler = new IHttpsHandler() {

        final ServiceConfig sc = getServiceConfig();
        final Connection conn = getLocalConnection();
        
        @Override
        public String[] getCredentials(String host, int port, String authValue) {

            // No Implementation Here
            String[] rval = null;
            return rval;
        }

        @Override
        public void credentialsFailed() {
            statusHandler
                    .error("HttpsHandler handler failed to authenticate!");
        }

        @Override
        public KeyStore getTruststore() {

            KeyStore trustStore = null;
            String filePath = sc.getConstantValue(truststore);
            // Check to see if it's enabled
            if (filePath != null && conn != null) {
                try {
                    trustStore = HttpsUtils.loadKeystore(filePath, storeType,
                            conn.getUnencryptedPassword());
                } catch (Exception e) {
                    statusHandler.error("Couldn't load truststore!", e);
                }
            }

            return trustStore;
        }

        @Override
        public boolean isValidateCertificates() {
            
            String filePath = sc.getConstantValue(truststore);
            
            if (filePath != null) {
                return true;
            } else {
                return false;
            }
        }

    };
    
    /**
     * Load the service configuration
     * @return
     */
    private static ServiceConfig getServiceConfig() {
        
        return HarvesterServiceManager.getInstance().getServiceConfig(
                ServiceType.PDA);
    }
    
    /**
     * Get the local encrypted connection object
     * 
     * @return
     */
    private static Connection getLocalConnection() {

        Connection connection = null;

        try {
            ProviderCredentials creds = ProviderCredentialsUtil
                    .retrieveCredentials(providerName);
            connection = creds.getConnection();
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "No local Connection file available! " + providerName);
        }
        
        return connection;
    }

}
