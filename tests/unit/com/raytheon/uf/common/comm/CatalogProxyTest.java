package com.raytheon.uf.common.comm;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class CatalogProxyTest {
    // the keystore (with one key) we'll use to make the connection with the
    // broker
    private final static String KEYSTORE_LOCATION = "/awips2/edex/conf/security/keystore.jks";

    private final static String KEYSTORE_PASS = "password";

    // the truststore we use for our server. This keystore should contain all
    // the keys
    // that are allowed to make a connection to the server
    private final static String TRUSTSTORE_LOCATION = "/awips2/edex/conf/security/truststore.jks";

    private final static String TRUSTSTORE_PASS = "password";

    /**
     * Simple starter for a jetty HTTPS server.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // create a jetty server and setup the SSL context
        Server server = new Server();
        SslContextFactory sslContextFactory = new SslContextFactory(
                KEYSTORE_LOCATION);
        sslContextFactory.setKeyStorePassword(KEYSTORE_PASS);
        sslContextFactory.setTrustStore(TRUSTSTORE_LOCATION);
        sslContextFactory.setTrustStorePassword(TRUSTSTORE_PASS);
        sslContextFactory.setNeedClientAuth(true);
        // create a https connector
        SslSocketConnector connector = new SslSocketConnector(sslContextFactory);
        connector.setPort(8443);
        // register the connector
        server.setConnectors(new Connector[] { connector });
        ServletContextHandler scHandler = new ServletContextHandler(server, "/");
        scHandler.addServlet(CatalogServlet.class, "/");
        server.start();
        server.join();
    }
}
