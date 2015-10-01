package com.raytheon.uf.edex.datadelivery.harvester.pda;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfigurationManager;
import com.raytheon.uf.common.datadelivery.harvester.PDACatalogServiceResponseWrapper;
import com.raytheon.uf.common.datadelivery.registry.Connection;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.PDAConnectionUtil;

/**
 * Retrieve PDA Catalog Service Results Record
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * June 13, 2014 3120      dhladky     Initial creation
 * Sept 14, 2015 4881      dhladky     Updates to PDA processing/ better debugging.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */


public class RetrievePDACatalogServiceResultsRecord {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDACatalogServiceResponseHandler.class);
    
    /** PDA harvester config **/
    private HarvesterConfig hc = null;
    
    /** DEBUG PDA system **/
    private static final String DEBUG = "DEBUG";
    
    /** PDA service config **/
    private static ServiceConfig pdaServiceConfig;
    
    /** debug state */
    private Boolean debug = false;
    
    /** spring constructor **/
    public RetrievePDACatalogServiceResultsRecord() {
        String dbValue = getServiceConfig().getConstantByName(DEBUG).getValue();
        debug = Boolean.valueOf(dbValue);
    }
    
    /**
     * Read in the config file
     * 
     * @param configFile
     *            the configuration file
     * 
     * @return
     */
    private HarvesterConfig getHarvesterConfig() {

        if (hc == null) {
            try {
                hc = HarvesterConfigurationManager.getPDAConfiguration();
            } catch (Exception e) {
                statusHandler.handle(Priority.ERROR, "Could not retrieve PDA configuration!",
                        e);
            }
        }

        return hc;
    }
    
    /**
     * Spring enabled queue endpoint, called from spring
     * Download the getRecordsResponseResult XML to local disk
     * @param recordFilePath
     * @return
     */
    public byte[] getRecordFile(byte[] inBytes) {

        PDACatalogServiceResponseWrapper psrwOut = null;
        byte[] outBytes = null;
        String localFilePath = null;
        String recordFilePath = null;
        
        if (inBytes != null) {
            
            PDACatalogServiceResponseWrapper psrwIn = null;
            
            try {
                psrwIn = SerializationUtil.transformFromThrift(
                        PDACatalogServiceResponseWrapper.class, inBytes);
            } catch (SerializationException e) {
                statusHandler.handle(Priority.ERROR, "Failed to de-serialize message!", e);
            }
            
            recordFilePath = psrwIn.getFilePath();
        }
        
        
        String providerName = getHarvesterConfig().getProvider().getName();
        Connection providerConn = getHarvesterConfig().getProvider()
                .getConnection();

        if (providerName != null && providerConn != null
                && recordFilePath != null) {
            // FTPS the file down
            localFilePath = PDAConnectionUtil.ftpsConnect(providerConn,
                    providerName, recordFilePath);
            statusHandler
                    .info("Retrieved PDA Catalog Service getRecords() results file: "
                            + localFilePath);
            try {
                if (debug) {
                    // Makes a copy on local file system for comparison.
                    String tmpDir = System.getProperty("java.io.tmpdir");
                    if (tmpDir != null) {
                        FileUtil.copyFile(
                                new File(localFilePath),
                                new File(tmpDir + "/getRecordsRequestFile"
                                        + (System.currentTimeMillis()) + ".xml"));
                        statusHandler
                                .info("Copied file to saved location file: "
                                        + localFilePath);
                    } else {
                        statusHandler
                                .error("Couldn't copy getRecordsRequest File, tmpDir not set!: "
                                        + localFilePath);
                    }

                }
            } catch (IOException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to copy file to saved directory!!!!", e);

            }
            
           
        } else {
            throw new IllegalArgumentException(
                    "Provider information not valid!" + recordFilePath);
        }

        psrwOut = new PDACatalogServiceResponseWrapper(localFilePath);
        
        try {
            outBytes = SerializationUtil.transformToThrift(psrwOut);
        } catch (SerializationException e) {
            statusHandler
                    .handle(Priority.ERROR,
                            "Couldn't transform PDACatalogServiceResponseWrapper to bytes!",
                            e);
        }

        return outBytes;
    }
   
    /**
     * Get the instance of the service config
     * @return
     */
    private static ServiceConfig getServiceConfig() {
        
        if (pdaServiceConfig == null) {
            pdaServiceConfig = HarvesterServiceManager.getInstance()
            .getServiceConfig(ServiceType.PDA);
        }
        
        return pdaServiceConfig;
    }
    
}
