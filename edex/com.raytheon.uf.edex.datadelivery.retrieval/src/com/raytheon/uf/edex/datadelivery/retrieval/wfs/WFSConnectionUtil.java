package com.raytheon.uf.edex.datadelivery.retrieval.wfs;

import java.io.IOException;
import java.io.InputStream;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.comm.HttpAuthHandler;
import com.raytheon.uf.common.comm.HttpClient;
import com.raytheon.uf.common.comm.HttpClient.IStreamHandler;
import com.raytheon.uf.common.comm.HttpClientConfigBuilder;
import com.raytheon.uf.common.datadelivery.registry.Connection;
import com.raytheon.uf.common.datadelivery.registry.ProviderCredentials;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.ByteArrayOutputStreamPool;
import com.raytheon.uf.common.util.PooledByteArrayOutputStream;
import com.raytheon.uf.common.util.rate.TokenBucket;
import com.raytheon.uf.common.util.stream.RateLimitingInputStream;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ProviderCredentialsUtil;

/**
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
 * Nov 15, 2014 3757       dhladky     General HTTPS configuration
 * Jan 21, 2015 3952       njensen     Updated call to setupCredentials()
 * Jan 26, 2015 3952       njensen     gzip handled by default
 * May 10, 2015 4435       dhladky     Added keyStore retrieval to interface.
 * Dec 07, 2015 4834       njensen     getCredentials() now takes a URI
 * Jun 06, 2017 6222       tgurney     Use token bucket to rate-limit requests
 * 
 * </pre>
 * 
 * @author dhladky
 */

public class WFSConnectionUtil {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(WFSConnectionUtil.class);

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    /**
     * connections indexed by URI host:port keys
     */
    private static final Map<String, Connection> uriConnections = new ConcurrentHashMap<>();

    private static final HttpAuthHandler credentialHandler = new HttpAuthHandler() {

        @Override
        public String[] getCredentials(URI uri, String authValue) {
            String key = createConnectionKey(uri.getHost(), uri.getPort());
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
            statusHandler.error(
                    "Failed to authenticate with supplied username and password");
        }

        @Override
        public KeyStore getTruststore() {
            // TODO may validate with truststore someday.
            return null;
        }

        @Override
        public boolean isValidateCertificates() {
            // TODO may validate with cert someday.
            return false;
        }

        @Override
        public KeyStore getKeystore() {
            // TODO may submit keys someday.
            return null;
        }

        @Override
        public char[] getKeystorePassword() {
            // TODO Auto-generated method stub
            return null;
        }

    };

    private static volatile HttpClient httpClient;

    /**
     * @return cached http client instance
     */
    private static HttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (credentialHandler) {
                if (httpClient == null) {
                    HttpClientConfigBuilder builder = new HttpClientConfigBuilder();
                    builder.setHttpAuthHandler(credentialHandler);
                    httpClient = new HttpClient(builder.build());
                }
            }
        }
        return httpClient;
    }

    private static class RateLimitingStreamHandler implements IStreamHandler {

        public String response = "";

        private TokenBucket tokenBucket;

        private int priority;

        public RateLimitingStreamHandler(TokenBucket tokenBucket,
                int priority) {
            this.tokenBucket = tokenBucket;
            this.priority = priority;
        }

        @Override
        public void handleStream(InputStream is) throws CommunicationException {
            if (tokenBucket != null) {
                is = new RateLimitingInputStream(is, tokenBucket,
                        1.0 / priority);
            }

            try (PooledByteArrayOutputStream baos = ByteArrayOutputStreamPool
                    .getInstance().getStream()) {
                byte[] underlyingArray = baos.getUnderlyingArray();
                int read = 0;
                int index = 0;
                do {
                    try {
                        read = is.read(underlyingArray, index,
                                underlyingArray.length - index);
                    } catch (IOException e) {
                        throw new CommunicationException(
                                "Error reading byte response", e);
                    }

                    if (read > 0) {
                        index += read;
                        if (index == underlyingArray.length) {
                            baos.setCapacity(underlyingArray.length << 1);
                            underlyingArray = baos.getUnderlyingArray();
                        }
                    }
                } while (read > 0);

                baos.setCount(index);
                byte[] byteResult = new byte[index];
                System.arraycopy(underlyingArray, 0, byteResult, 0, index);
                response = new String(byteResult);
            } catch (IOException e) {
                // ignore error on stream close
            }
        }

    }

    /**
     * Connect to the provided URL and return the xml response.
     * 
     * @param request
     *            The request
     * @param providerConn
     *            The Connection object
     * @param providerName
     *            The data provider's name
     * @return xml response
     */
    public static String wfsConnect(String request, Connection providerConn,
            String providerName, TokenBucket tokenBucket,
            int priority) {

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

            if (localConnection != null
                    && localConnection.getProviderKey() != null) {
                statusHandler.handle(Priority.INFO,
                        "Attempting credentialed request: " + providerName);
                // Local Connection object contains the username, password and
                // encryption method for password storage and decrypt.
                String userName = localConnection.getUnencryptedUsername();
                String password = localConnection.getUnencryptedPassword();

                http.setupCredentials(uri.getHost(), uri.getPort(), userName,
                        password);
            }
            post.setEntity(new StringEntity(
                    request, ContentType.TEXT_XML));
            RateLimitingStreamHandler handler = new RateLimitingStreamHandler(
                    tokenBucket, priority);
            http.executeRequest(post, handler);
            xmlResponse = new String(handler.response);

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
