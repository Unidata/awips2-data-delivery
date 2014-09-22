package com.raytheon.uf.edex.datadelivery.retrieval.util;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import com.raytheon.uf.common.comm.HttpClient;
import com.raytheon.uf.common.comm.HttpClient.HttpClientResponse;
import com.raytheon.uf.common.comm.HttpClientConfigBuilder;
import com.raytheon.uf.common.comm.IHttpsCredentialsHandler;
import com.raytheon.uf.common.datadelivery.registry.Connection;
import com.raytheon.uf.common.datadelivery.registry.ProviderCredentials;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * 
 * WFS Connection.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 12, 2013 753        dhladky     created.
 * May 31, 2013 1763       dhladky     refined.
 * Jun 17, 2013 2106       djohnson    Use getUnencryptedPassword().
 * Jun 18, 2013 2120       dhladky     Times fixes and SSL changes
 * Jul 10, 2013 2180       dhladky     Updated credential requests
 * Aug 23, 2013 2180       mpduff      Implement changes to ProviderCredentialsUtil
 * Aug 06, 2013 2097       dhladky     WFS 2.0 compliance upgrade and switched to POST
 * Nov 20, 2013 2554       dhladky     Added GZIP capability to WFS requests.
 * Jan 13, 2014 2697       dhladky     Added util to strip unique Id field from URL.
 * Aub 20, 2014 3564       dhladky     Allow for un-authenicated HTTPS
 * Sep 03, 2014 3570       bclement    http client API changes
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class WfsConnectionUtil {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(WfsConnectionUtil.class);

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    
    /**
     * connections indexed by URI host:port keys
     */
    private static final Map<String, Connection> uriConnections = new ConcurrentHashMap<String, Connection>();

    private static final IHttpsCredentialsHandler credentialHandler = new IHttpsCredentialsHandler() {

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
            statusHandler
                    .error("Failed to authenticate with supplied username and password");
        }

    };
    
    private static volatile HttpClient httpClient;
    
    /**
     * @return cached http client instance
     */
    private static HttpClient getHttpClient(){
        if (httpClient == null){
            synchronized (credentialHandler) {
                if (httpClient == null){
                    HttpClientConfigBuilder builder = new HttpClientConfigBuilder();
                    // accept gzipped data for WFS
                    builder.setHandlingGzipResponses(true);
                    builder.setHttpsHandler(credentialHandler);
                    httpClient = new HttpClient(builder.build());
                }
            }
        }
        return httpClient;
    }

    /**
     * Connect to the provided URL and return the xml response.
     * 
     * @param url
     *            The URL
     * @param providerConn
     *            The Connection object
     * @param providerName
     *            The data provider's name
     * @return xml response
     */
    public static String wfsConnect(String request, Connection providerConn,
            String providerName) {

        String xmlResponse = null;
        HttpClient http = null;
        String rootUrl = null;

        try {

            rootUrl = getCleanUrl(providerConn.getUrl());
            http = getHttpClient();
            URI uri = new URI(rootUrl);
            HttpPost post = new HttpPost(uri);
            // check for the need to do a username password auth check
            Connection localConnection = getLocalConnection(uri, providerName);
     //       http.setHttpsConfiguration(new WfsHttpsConfiguration(uri));

            if (localConnection != null
                    && localConnection.getProviderKey() != null) {
                statusHandler.handle(Priority.INFO,
                        "Attempting credentialed request: " + providerName);
                // Local Connection object contains the username, password and
                // encryption method for password storage and decrypt.
                String userName = localConnection.getUnencryptedUsername();
                String password = localConnection.getUnencryptedPassword();
                
                http.setCredentials(uri.getHost(), uri.getPort(), providerName,
                        userName, password);
            }

            post.setEntity(new StringEntity(request, ContentType.TEXT_XML));
            HttpClientResponse response = http.executeRequest(post);
            xmlResponse = new String(response.data);

        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Couldn't connect to WFS server: " + rootUrl, e);
        }

        return xmlResponse;
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
     * Removes un-needed unique Identifier from PointDataSetMetaData derived
     * URL's
     * 
     * @param rootUrl
     * @return
     */
    private static String getCleanUrl(String providerUrl) {
        return COMMA_PATTERN.split(providerUrl)[0];
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
