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
package com.raytheon.uf.edex.datadelivery.retrieval.opendap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import opendap.dap.DataDDS;

import com.raytheon.edex.util.Util;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Utilities for working with net.dods.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 12, 2013 1543       djohnson     Initial creation
 * Apr 12, 2015 4400       dhladky      Upgraded to DAP2 with backward compatibility.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class DodsUtils {

    
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DodsUtils.class);

    /** Version of OpenDAP server **/
    private static final String VERSION = "DAP_VERSION";
    
    /** DAP 1 (XDODS) version **/
    private static final float DAP1_VERSION = 1.0f;

    /** DAP version **/
    private static boolean isOldVersion;

    /** These will rarely if ever change so, made static. **/
    static {
        ServiceConfig config = HarvesterServiceManager.getInstance()
                .getServiceConfig(ServiceType.OPENDAP);

        Float version = Float.valueOf(config.getConstantByName(VERSION)
                .getValue());

        if (version == DAP1_VERSION) {
            isOldVersion = true;
        } else {
            isOldVersion = false;
        }
    }

    /**
     * Prevent construction.
     */
    private DodsUtils() {
    }

    /**
     * Determines if the opendap service is encoded as the new DAP2 or older XDODS version.
     * @return
     */
    public static boolean isOlderXDODSVersion() {
        return isOldVersion; 
    }
    
    /**
     * Convert the DataDDS instance to a byte array.
     * 
     * @param dataDds
     *            the DataDDS instance
     * @return the byte array
     * @throws SerializationException
     *             on error converting to a byte array
     */
    public static byte[] convertDataDdsToByteArray(Object dataDds)
            throws SerializationException {
        ByteArrayOutputStream os = null;

        try {
            os = new ByteArrayOutputStream(700);
            
            if (dataDds instanceof dods.dap.DataDDS) {
                ((dods.dap.DataDDS)dataDds).externalize(os, true, true);
            } else if (dataDds instanceof opendap.dap.DataDDS) {
                ((opendap.dap.DataDDS)dataDds).externalize(os, true, true);
            } else {
                throw new SerializationException(
                        "Unknown type for DDS serialization. "+dataDds.getClass().getName());
            }
     
            return os.toByteArray();
            
        } catch (IOException e) {
            throw new SerializationException(
                    "Unable to externalize the DataDDS instance.", e);
        } finally {
            Util.close(os);
        }
    }

    /**
     * Restore the {@link DataDDS} from the byte array.
     * We try both types before we throw an exception
     * This preserves backward compatibility between versions 
     * of XDODS/DAP.
     * 
     * @param byteArray
     * @return the DataDDS instance (object)
     * @throws SerializationException
     *             on error restoring the DataDDS
     */
    public static Object restoreDataDdsFromByteArray(byte[] byteArray)
            throws SerializationException {

        Object data = null;

        try {
            // try the version you are stated to be creating first
            if (isOlderXDODSVersion()) {
                try {
                    data = getOlderVersion(byteArray);
                } catch (Exception e1) {
                    // Fails DAP2 so try DAP1/XDODS just in case.
                    // This generally shouldn't happen.
                    statusHandler
                            .debug("DDS object not stated DAP1/XDODS version, trying DAP2 version...");
                    data = getCurrentVersion(byteArray);
                }
            } else {
                // The general case is a central registry still broadcasting
                // older DAP1/XDODS objects
                // So we try both ways if it fails with DAP2
                try {
                    data = getCurrentVersion(byteArray);
                } catch (Exception e) {
                    statusHandler
                            .debug("DDS object not stated DAP2 version, trying DAP1/XDODS version...");
                    data = getOlderVersion(byteArray);
                }
            }
        } catch (Exception e) {
            throw new SerializationException(
                    "Unable to deserialize the DataDDS instance.", e);
        }

        return data;
    }
    /**
     * Try older XDODS version
     * @param byteArray
     * @return
     * @throws Exception
     */
    private static dods.dap.DataDDS getOlderVersion(byte[] byteArray) throws Exception {
        dods.dap.DConnect dconnect = new dods.dap.DConnect(
                new ByteArrayInputStream(byteArray));
        return dconnect.getData(null);
    }
    
    /**
     * Try new DAP2 version
     * @param byteArray
     * @return
     * @throws Exception
     */
    private static opendap.dap.DataDDS getCurrentVersion(byte[] byteArray) throws Exception {
        opendap.dap.DConnect dconnect = new opendap.dap.DConnect(
                new ByteArrayInputStream(byteArray));
        return dconnect.getData(null);
    }

}
