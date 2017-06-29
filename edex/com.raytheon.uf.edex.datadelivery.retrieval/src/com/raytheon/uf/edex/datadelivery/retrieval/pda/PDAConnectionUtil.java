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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.registry.Connection;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.ProviderCredentials;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.SizeUtil;
import com.raytheon.uf.common.util.rate.TokenBucket;
import com.raytheon.uf.common.util.stream.CountingInputStream;
import com.raytheon.uf.common.util.stream.RateLimitingInputStream;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ProviderCredentialsUtil;
import com.raytheon.uf.edex.security.SecurityConfiguration;

/**
 * Utility for FTPS connection
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 12, 2014  3012     dhladky   initial release
 * Sep 14, 2104  3121     dhladky   Added binary transfer switch
 * Oct 14, 2014  3127     dhladky   Fine tuning for FTPS
 * Nov 10, 2014  3826     dhladky   Added more logging.
 * Aug 02, 2015  4881     dhladky   Disable Remote verification, FTPS comms are
 *                                  proxied.
 * Dec 05, 2015  5209     dhladky   Remove relative URL workaround.  Relative
 *                                  and actual URL are equal now.
 * Jan 20, 2016  5280     dhladky   Added FTP type configuration, added
 *                                  SecureDataChannel overrides.
 * Sep 16, 2016  5762     tjensen   Remove Camel from FTPS calls
 * Sep 30, 2016  5762     tjensen   Improve Error Handling
 * Nov 17, 2016  6002     tjensen   Added FTP connection timeouts
 * Jun 06, 2017  6222     tgurney   Use token bucket to rate-limit requests
 * Jun 22, 2017  6222     tgurney   Log time taken and size of downloads
 * Jun 23, 2017  6322     tgurney   ftpsConnect throws Exception
 * Jun 29, 2017  6130     tjensen   Add visibility to
 *                                  separateRemoteFileDirectoryAndFileName()
 *
 * </pre>
 *
 * @author dhladky
 */

public class PDAConnectionUtil {

    /** The logger */
    private static final Logger logger = LoggerFactory
            .getLogger(PDAConnectionUtil.class);

    private static final Pattern PATH_PATTERN = Pattern.compile(File.separator);

    private static final Pattern PROTOCOL_SUFFIX = Pattern.compile("://");

    private static ServiceConfig serviceConfig;

    private static SecurityConfiguration sc;
    static {
        serviceConfig = HarvesterServiceManager.getInstance()
                .getServiceConfig(ServiceType.PDA);
        sc = getSecurityConfiguration();
    }

    /***
     * Connect using FTPS and grab file
     *
     * @param protocol
     * @param providerConn
     * @param providerName
     * @param remoteFileName
     * @param tokenBucket
     * @return
     */
    public static String ftpsConnect(Connection providerConn,
            String providerName, String remoteFilename) throws Exception {
        return ftpsConnect(providerConn, providerName, remoteFilename, null,
                (int) TokenBucket.DEFAULT_WEIGHT);
    }

    /***
     * Connect using FTPS and grab file
     *
     * @param protocol
     * @param providerConn
     * @param providerName
     * @param remoteFileName
     * @param tokenBucket
     * @param priority
     * @return
     * @throws Exception
     */
    public static String ftpsConnect(Connection providerConn,
            String providerName, String remoteFilename, TokenBucket tokenBucket,
            int priority) throws Exception {
        String localFileName = null;
        ProviderCredentials creds = null;
        try {
            creds = ProviderCredentialsUtil.retrieveCredentials(providerName);
        } catch (Exception e) {
            throw new Exception("Failed to retrieve credentials for provider "
                    + providerName, e);
        }
        Connection localConnection = creds.getConnection();

        if (localConnection == null
                || localConnection.getProviderKey() == null) {
            throw new IllegalArgumentException(
                    "No local connection file available! provider: "
                            + providerName);
        }

        logger.info("Attempting credentialed request: " + providerName);
        /*
         * Local Connection object contains the username, password and
         * encryption method for password storage and decrypt.
         */
        String userName = localConnection.getUnencryptedUsername();
        String password = localConnection.getUnencryptedPassword();

        String[] remotePathAndFile = separateRemoteFileDirectoryAndFileName(
                remoteFilename);
        // Do this after you separate!
        String rootUrl = serviceConfig.getConstantValue("FTPS_REQUEST_URL");
        rootUrl = removeProtocolsAndFilesFromRootUrl(rootUrl);

        logger.info("rootUrl: " + rootUrl);
        String localFileDirectory = serviceConfig
                .getConstantValue("FTPS_DROP_DIR");
        localFileName = localFileDirectory + File.separator
                + remotePathAndFile[1];
        logger.info("Local File Name: " + localFileName);
        int port = new Integer(serviceConfig.getConstantValue("PORT"))
                .intValue();
        boolean doBinaryTransfer = Boolean.parseBoolean(
                serviceConfig.getConstantValue("BINARY_TRANSFER"));
        boolean usePassiveMode = Boolean
                .parseBoolean(serviceConfig.getConstantValue("PASSIVE_MODE"));
        try {
            FTPSClient ftp = createFtpClient();
            ftpsRetrieveFile(ftp, userName, password, rootUrl, port,
                    remotePathAndFile[1], remotePathAndFile[0], localFileName,
                    doBinaryTransfer, usePassiveMode, tokenBucket, priority);
        } catch (Exception e) {
            throw new Exception("Error retrieving file " + remoteFilename
                    + " from FTPS server " + rootUrl, e);
        }
        return localFileName;
    }

    private static void ftpsRetrieveFile(FTPSClient ftp, String userName,
            String password, String rootUrl, int port, String remoteFilename,
            String remoteFilePath, String localFilename,
            boolean doBinaryTransfer, boolean usePassiveMode,
            TokenBucket tokenBucket, int priority)
            throws FileNotFoundException, IOException {

        int reply = 0;
        try (OutputStream fos = new FileOutputStream(localFilename)) {
            ftp.connect(rootUrl, port);
            /*
             * After connection attempt, you should check the reply code to
             * verify success.
             */
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("FTP server refused connection. Reply: "
                        + ftp.getReplyString());
            }

            // Set protocols after connection established.
            String disableSecureDataChannelDefaults = serviceConfig
                    .getConstantValue("DISABLE_SECURE_DATA_CHANNEL_DEFAULTS");
            boolean isDisableSecureDataChannelDefaults = Boolean
                    .parseBoolean(disableSecureDataChannelDefaults);
            if (isDisableSecureDataChannelDefaults) {
                ftp.execPROT(serviceConfig.getConstantValue("EXEC_PROT"));
                ftp.execPBSZ(
                        new Long(serviceConfig.getConstantValue("EXEC_PBSZ"))
                                .longValue());
            }

            // Attempt to login
            if (!ftp.login(userName, password)) {
                ftp.logout();
                throw new IOException(
                        "Unable to login to server as " + userName);
            }

            // Set transfer type and mode
            if (doBinaryTransfer) {
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
            } else {
                ftp.setFileType(FTP.ASCII_FILE_TYPE);
            }
            if (usePassiveMode) {
                ftp.enterLocalPassiveMode();
            } else {
                ftp.enterLocalActiveMode();
            }

            // If we don't have a path, skip changing directory.
            if (!"".equals(remoteFilePath) && remoteFilePath != null) {
                // If debugging, print the directory information
                printDirListing(ftp);

                // Change directories to the location of the file to be
                // transfered.
                while (remoteFilePath.substring(0, 1).equals(File.separator)) {
                    remoteFilePath = remoteFilePath.substring(1,
                            remoteFilePath.length());
                }
                ftp.changeWorkingDirectory(remoteFilePath);
                reply = ftp.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    throw new IOException("Change Working Dir to "
                            + remoteFilePath + " was unsuccessful. Reply: "
                            + ftp.getReplyString());
                }
            }

            // If debugging, print the directory information
            printDirListing(ftp);

            // Download the file
            logger.info("Downloading file " + remoteFilename + " to "
                    + localFilename);
            CountingInputStream cis = null;
            try (InputStream is = ftp.retrieveFileStream(remoteFilename)) {
                if (tokenBucket != null) {
                    cis = new CountingInputStream(new RateLimitingInputStream(
                            is, tokenBucket, 1.0 / priority));
                } else {
                    cis = new CountingInputStream(is);
                }
                IOUtils.copy(cis, fos);
            }

            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("Retrieval was unsuccessful for "
                        + remoteFilename + " Reply: " + ftp.getReplyString());
            }
            logger.info("Successfully retrieved " + remoteFilename + " ("
                    + SizeUtil.prettyByteSize(cis.getBytesRead()) + ") in "
                    + TimeUtil.prettyDuration(cis.getLastReadTimeMillis()
                            - cis.getFirstReadTimeMillis()));
            ftp.logout();
        } finally {
            // If still connected, disconnect.
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException f) {
                    // do nothing
                }
            }
        }
    }

    private static void printDirListing(FTPSClient ftp) throws IOException {
        if (logger.isDebugEnabled()) {
            String[] listNames = ftp.listNames();
            StringBuilder buf = new StringBuilder();
            for (String file : listNames) {
                buf.append(file + "\n");
            }
            logger.debug("Directory Listing for: " + ftp.printWorkingDirectory()
                    + "\n" + buf.toString());
        }
    }

    protected static FTPSClient createFtpClient() throws Exception {
        FTPSClient client = null;

        String protocol = serviceConfig.getConstantValue("PROTOCOL");
        boolean implict = Boolean.parseBoolean(
                serviceConfig.getConstantValue("IMPLICIT_SECURITY"));

        client = new FTPSClient(protocol, implict);

        setFtpsKeyStore(client);

        setFtpsTrustStore(client);

        client.setRemoteVerificationEnabled(Boolean.parseBoolean(
                serviceConfig.getConstantValue("REMOTE_VERIFICATION_ENABLED")));

        /*
         * Set connection timeouts to prevent connections from hanging
         * infinitely. Connect timeout is specific for establishing the initial
         * connection. Data timeout is for maintaining the connection during
         * data transfer.
         */
        client.setConnectTimeout(Integer.parseInt(
                serviceConfig.getConstantValue("FTP_CONNECT_TIMEOUT")) * 1000);
        client.setDataTimeout(Integer.parseInt(
                serviceConfig.getConstantValue("FTP_DATA_TIMEOUT")) * 1000);

        final FTPClientConfig config;
        config = new FTPClientConfig();
        client.configure(config);
        return client;
    }

    private static void setFtpsTrustStore(FTPSClient client)
            throws KeyStoreException, FileNotFoundException, IOException,
            NoSuchAlgorithmException, CertificateException {
        String type = KeyStore.getDefaultType();
        String file = serviceConfig.getConstantValue("TRUSTSTORE_FILE");
        String password = sc.getProperty("edex.security.truststore.password");
        String algorithm = TrustManagerFactory.getDefaultAlgorithm();

        KeyStore trustStore = KeyStore.getInstance(type);

        try (FileInputStream trustStoreFileInputStream = new FileInputStream(
                new File(file))) {
            trustStore.load(trustStoreFileInputStream, password.toCharArray());
        }

        TrustManagerFactory trustMgrFactory = TrustManagerFactory
                .getInstance(algorithm);
        trustMgrFactory.init(trustStore);

        client.setTrustManager(trustMgrFactory.getTrustManagers()[0]);
    }

    private static void setFtpsKeyStore(FTPSClient client)
            throws KeyStoreException, FileNotFoundException, IOException,
            NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException {
        String type = KeyStore.getDefaultType();
        String file = serviceConfig.getConstantValue("KEYSTORE_FILE");
        String algorithm = KeyManagerFactory.getDefaultAlgorithm();
        String keyPassword = sc.getProperty("edex.security.keystore.password");

        KeyStore keyStore = KeyStore.getInstance(type);
        try (FileInputStream keyStoreFileInputStream = new FileInputStream(
                new File(file))) {
            keyStore.load(keyStoreFileInputStream, keyPassword.toCharArray());
        }

        KeyManagerFactory keyMgrFactory = KeyManagerFactory
                .getInstance(algorithm);
        keyMgrFactory.init(keyStore, keyPassword.toCharArray());
        client.setNeedClientAuth(true);
        client.setKeyManager(keyMgrFactory.getKeyManagers()[0]);
    }

    /**
     * Separate the remoteFileDirectory and filename from the remote path. If
     * only a filename is given, remote path will be an empty string
     *
     * @param remoteFilePath
     * @return
     */
    public static String[] separateRemoteFileDirectoryAndFileName(
            String remoteFilePath) {

        String[] returnValues = new String[2];

        // Carve it up and reconstruct it
        String[] parts = PATH_PATTERN.split(remoteFilePath);

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            // this is the fileName
            if (i == parts.length - 1) {
                returnValues[1] = parts[i];
            } else if (i == 0) {
                buf.append(File.separator);
                buf.append(parts[i]);
            } else {
                buf.append(parts[i]);
                buf.append(File.separator);
            }
        }

        returnValues[0] = buf.toString();

        return returnValues;
    }

    /**
     * Cleave off any protocol related stuff on the front of the URL
     *
     * @param rootUrl
     * @return
     */

    private static String removeProtocolsAndFilesFromRootUrl(String rootUrl) {

        // carve off any protocols
        String[] chunks = PROTOCOL_SUFFIX.split(rootUrl);
        String[] chunks2 = PATH_PATTERN.split(chunks[chunks.length - 1]);
        return chunks2[0];
    }

    /**
     * Get the active security configuration
     *
     * @return
     */
    private static SecurityConfiguration getSecurityConfiguration() {
        synchronized (PDAConnectionUtil.class) {
            if (sc == null) {
                try {
                    sc = new SecurityConfiguration();
                } catch (IOException ioe) {
                    logger.error("Couldn't access the security configuration!",
                            ioe);
                }
            }
        }
        return sc;
    }
}
