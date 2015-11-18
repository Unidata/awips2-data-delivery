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

import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import com.raytheon.uf.edex.security.SecurityConfiguration;

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
 * Jan  26, 2015 3952       njensen    gzip handled by default
 * May  08. 2015 4435       dhladky    Fixed type on truststore, JKS, added Keystore 
 * Nov  18, 2015 5139       dhladky    PDA altered their interface yet again, requiring SOAP headers now.
 * 
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

    /** truststore path set by Data Delivery in the serviceConfig XML */
    protected static final String truststore = "TRUSTSTORE_FILE";
    
    /** keystore path set by Data Delivery in the serviceConfig XML */
    protected static final String keystore = "KEYSTORE_FILE";

    /** truststore type, AWIPS2 uses JKS by default **/
    protected static final String storeType = "JKS";

    /** provider **/
    protected static final String providerName = "PDA";
    
    /** SOAPACtion header is required for talking to PDA **/
    protected static final String SOAP_ACTION = "SOAPAction";

    /** HTTP connection client **/
    private static volatile HttpClient httpClient;
    
    /**
     * connections indexed by URI host:port keys
     */
    private static final Map<String, Connection> uriConnections = new ConcurrentHashMap<String, Connection>();

    /** truststore password AES encrypted in security properties file **/
    private static final String TRUSTSTORE_PASS = "edex.security.truststore.password";
    
    /** keystore password AES encrypted in security properties file **/
    private static final String KEYSTORE_PASS = "edex.security.keystore.password";
    

    /**
     * Connect to the PDA service
     * 
     * @param request (XML)
     * @param soapAction
     * @param serviceURL
     * @return
     */
    public static String connect(String request, String soapAction, String serviceURL) {

        String xmlResponse = null;

        try {

            statusHandler.info("Connecting to: " + serviceURL);
            httpClient = getHttpClient();
            URI uri = new URI(serviceURL);
            // check for the need to do a username password auth check
            Connection localConnection = getLocalConnection(uri, providerName);
            
            if (localConnection != null
                    && localConnection.getProviderKey() != null) {
                statusHandler.handle(Priority.INFO,
                        "Attempting credentialed request: " + providerName);
                // Local Connection object contains the username, password and
                // encryption method for password storage and decrypt.
                String userName = localConnection.getUnencryptedUsername();
                String password = localConnection.getUnencryptedPassword();

                httpClient.setupCredentials(uri.getHost(), uri.getPort(), userName,
                        password);
            }
                       
            HttpPost post = new HttpPost(uri);
            
            // Add soap action if necessary.
            if (soapAction != null) {
                post.addHeader(SOAP_ACTION, soapAction);
            }
            
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
            builder.setHttpsHandler(httpsHandler);
            httpClient = new HttpClient(builder.build());
        }

        return httpClient;
    }

    /**
     * HTTPS handler for PDA
     */
    private static final IHttpsHandler httpsHandler = new IHttpsHandler() {

        final ServiceConfig serviceConfig = getServiceConfig();

        final SecurityConfiguration sc = getSecurityConfiguration();

        @Override
        public String[] getCredentials(String host, int port, String authValue) {
            
            String key = createConnectionKey(host, port);
            Connection connection = uriConnections.get(key);
            String[] rval = null;
            if (connection != null) {
                rval = new String[2];
                rval[0] = connection.getUnencryptedUsername();
                rval[1] = connection.getUnencryptedPassword();
            } else {
                statusHandler.warn("Missing credentials for service at " + key);
            }
            
            return rval;
        }

        @Override
        public void credentialsFailed() {
            statusHandler.error("HttpsHandler handler failed to authenticate!");
        }

        @Override
        public KeyStore getTruststore() {

            KeyStore trustStore = null;
            String filePath = serviceConfig.getConstantValue(truststore);
            // Check to see if it's enabled
            if (filePath != null && sc != null) {
                try {
                    trustStore = HttpsUtils.loadKeystore(filePath, storeType,
                            sc.getProperty(TRUSTSTORE_PASS));
                    statusHandler.info("Loaded the truststore! "+filePath);    
                } catch (Exception e) {
                    statusHandler.error("Couldn't load truststore! "+filePath, e);
                }
            }

            return trustStore;
        }
        
        @Override
        public KeyStore getKeystore() {
            KeyStore keyStore = null;
            String filePath = serviceConfig.getConstantValue(keystore);
            // Check to see if it's enabled
            if (filePath != null && sc != null) {
                try {
                    keyStore = HttpsUtils.loadKeystore(filePath, storeType,
                            sc.getProperty(KEYSTORE_PASS));
                    statusHandler.info("Loaded the keystore! "+filePath);    
                } catch (Exception e) {
                    statusHandler.error("Couldn't load keystore! "+filePath, e);
                }
            }

            return keyStore;
        }

        @Override
        public boolean isValidateCertificates() {

            String filePath = serviceConfig.getConstantValue(truststore);

            if (filePath != null) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public char[] getKeystorePassword() {
            return sc.getProperty(KEYSTORE_PASS).toCharArray();
        }

    };

    /**
     * Load the service configuration
     * 
     * @return
     */
    private static ServiceConfig getServiceConfig() {

        return HarvesterServiceManager.getInstance().getServiceConfig(
                ServiceType.PDA);
    }

    /**
     * Get the active security configuration
     * 
     * @return
     */
    private static SecurityConfiguration getSecurityConfiguration() {

        SecurityConfiguration sc = null;

        try {
            sc = new SecurityConfiguration();
        } catch (IOException ioe) {
            statusHandler.handle(Priority.PROBLEM,
                    "Couldn't access the security configuration!", ioe);
        }

        return sc;
    }
    
    /**
     * @param uri
     * @param providerName
     * @return cached local connection for provider
     * @throws Exception
     */
    private static Connection getLocalConnection(URI uri, String providerName)
            throws Exception {
        String key = createConnectionKey(uri.getHost(), uri.getPort());
        Connection rval = uriConnections.get(key);
        if (rval == null) {
            ProviderCredentials creds = ProviderCredentialsUtil
                    .retrieveCredentials(providerName);
            rval = creds.getConnection();

            if (rval != null) {
                uriConnections.put(key, rval);
            }
        }
        return rval;
    }
    
    /**
     * @param host
     * @param port
     * @return key to {@link #uriConnections} map
     */
    private static String createConnectionKey(String host, int port) {
        return host + ":" + port;
    }

}
