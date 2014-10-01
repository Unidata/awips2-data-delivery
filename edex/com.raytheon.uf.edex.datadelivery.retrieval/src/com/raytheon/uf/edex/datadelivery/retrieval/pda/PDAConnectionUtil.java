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
import java.util.regex.Pattern;

import com.raytheon.uf.common.datadelivery.registry.Connection;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.ProviderCredentials;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ProviderCredentialsUtil;
import com.raytheon.uf.edex.esb.camel.ftp.FTP;
import com.raytheon.uf.edex.esb.camel.ftp.FTPRequest;
import com.raytheon.uf.edex.esb.camel.ftp.FTPRequest.FTPType;

/**
 * Utility for FTPS connection
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 12, 2014 3012         dhladky    initial release
 * Sep 14, 2104  3121        dhladky    Added binary transfer switch
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDAConnectionUtil {
    
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDAConnectionUtil.class);
    
    private static final Pattern PATH_PATTERN = Pattern.compile(File.separator);
    
    private static final Pattern PROTOCOL_SUFFIX = Pattern.compile("://");
    
    private static ServiceConfig serviceConfig;
    
    /* Private Constructor */
    private PDAConnectionUtil() {
        serviceConfig = HarvesterServiceManager.getInstance().getServiceConfig(
                ServiceType.PDA);

    }

    /**
     * Get an instance of this singleton.
     * 
     * @return Instance of this class
     */
    public static PDAConnectionUtil getInstance() {
        return instance;
    }
    
    /** Singleton instance of this class */
    private static PDAConnectionUtil instance = new PDAConnectionUtil();

    /***
     * Connect using FTPS and grab file
     * @param protocol
     * @param providerConn
     * @param providerName
     * @param remoteFileName
     * @return
     */
    public static String ftpsConnect(Connection providerConn,
            String providerName, String remoteFilename) {

        String password = null; 
        String userName = null;
        FTPRequest ftpRequest = null;
        String rootUrl = null;
        String localFileDirectory = null;
        String remoteFileDirectory = null;
        String fileName = null;
        String localFileName = null;

        try {

            rootUrl = providerConn.getUrl();
            ProviderCredentials creds = ProviderCredentialsUtil
                    .retrieveCredentials(providerName);
            Connection localConnection = creds.getConnection();
            
            if (localConnection != null
                    && localConnection.getProviderKey() != null) {
                statusHandler.handle(Priority.INFO,
                        "Attempting credentialed request: " + providerName);
                // Local Connection object contains the username, password and
                // encryption method for password storage and decrypt.
                userName = localConnection.getUnencryptedUsername();
                password = localConnection.getUnencryptedPassword();
                String[] remotePathAndFile = separateRemoteFileDirectoryAndFileName(remoteFilename, rootUrl);
                // Do this after you separate!
                rootUrl = removeProtocolsAndFilesFromRootUrl(rootUrl);
                remoteFileDirectory = remotePathAndFile[0];
                fileName = remotePathAndFile[1];
                localFileDirectory = serviceConfig.getConstantValue("FTPS_DROP_DIR");
                String protocol = serviceConfig.getConstantValue("PROTOCOL");
                String port = serviceConfig.getConstantValue("PORT");
                ftpRequest = new FTPRequest(FTPType.FTP, rootUrl, userName, password, port);

                // NO SSL yet, this is how you add additional parameters
                //ftpRequest.addAdditionalParameter("securityProtocol", protocol);
                ftpRequest.setDestinationDirectoryPath(localFileDirectory);
                ftpRequest.setRemoteDirectoryPath(remoteFileDirectory);
                ftpRequest.setFileName(fileName);
                // PDA downloads are all binary
                ftpRequest.addAdditionalParameter("binary", serviceConfig.getConstantValue("BINARY_TRANSFER"));
                
                FTP ftp = new FTP(ftpRequest);
                localFileName = ftp.executeConsumer();

            } else {
                throw new IllegalArgumentException(
                        "No username and password for FTPS server available! "
                                + rootUrl + " provider: " + providerName);
            }
            
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Couldn't connect to FTPS server: " + rootUrl, e);
        }

        return localFileName;
    }
    
    /**
     * Separate the remoteFileDirectory and filename from the remote path
     * @param remoteFilePath
     * @param rootUrl
     * @return
     */
    private static String[] separateRemoteFileDirectoryAndFileName(String remoteFilePath, String rootUrl) {
        
        String[] returnValues = new String[2];
        
        try {

            int j;
            
            if (remoteFilePath.equals(rootUrl)) {
                // skip first 2
                j = 3;
            } else {
                // carve off the rootURL if it is listed as part of the remoteFilename
                remoteFilePath = remoteFilePath.replaceAll(rootUrl, "");
                j = 0;
            }
            // then carve into it's pieces
            String[] parts = remoteFilePath.split(PATH_PATTERN.pattern());
            
            StringBuilder buf = new StringBuilder();

            if (j == 3) {
               buf.append(File.separator);
            }
                        
            for (int i = j;i < parts.length; i++) {
                // this is the fileName
                if (i == 0) {
                    // ignore first part
                } else if (i == parts.length-1) {  
                    returnValues[1] = parts[i];
                } else {
                    // this is the path
                    if (i == 1) {
                        buf.append(File.separator);
                    }
                    
                    buf.append(parts[i]);
                    buf.append(File.separator);
                }
            }
            
            // hack off any straggling path separators
            String directory = buf.toString();
            //if (directory.startsWith(File.separator)) {
            //    directory = directory.substring(1, directory.length());
            //}
            //if (directory.endsWith(File.separator)) {
            //    directory = directory.substring(0, directory.length()-1);
            //}
           
            returnValues[0] = directory;
            
            if (returnValues[0].endsWith(returnValues[1])) {
                returnValues[0] = returnValues[0].replaceAll(returnValues[1], "");
            }

        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR, "Couldn't properly parse remoteFilePath: "+remoteFilePath, e);
        }
         
        return returnValues;
    }
    
    /**
     * Cleave off any protocol related stuff on the front of the URL
     * @param rootUrl
     * @return
     */

    private static String removeProtocolsAndFilesFromRootUrl(String rootUrl) {
        
        //carve off any protocols
        String[] chunks = PROTOCOL_SUFFIX.split(rootUrl);
        String[] chunks2 = PATH_PATTERN.split(chunks[chunks.length-1]);
        return chunks2[0];
    }
    
}
