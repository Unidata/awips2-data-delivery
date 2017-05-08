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
package com.raytheon.uf.common.datadelivery.retrieval.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.ILocalizationPathObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Regex manager
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 27, 2012  1163     dhladky   Initial creation
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Nov 07, 2013  2361     njensen   Use JAXBManager for XML
 * Mar 31, 2017  6186     rjpeter   Allow incremental override for ServiceConfig, fix notification.
 *
 * </pre>
 *
 * @author dhladky
 */
public class HarvesterServiceManager implements ILocalizationPathObserver {

    /** Path to Prefix config. */
    private static final String CONFIG_FILE_NAME_PREFIX = "datadelivery"
            + File.separatorChar;

    /** Path to suffix config. */
    private static final String CONFIG_FILE_NAME_SUFFIX = "ServiceConfig.xml";

    private static final SingleTypeJAXBManager<ServiceConfig> jaxb = SingleTypeJAXBManager
            .createWithoutException(ServiceConfig.class);

    /**
     * Get an instance of this singleton.
     *
     * @return Instance of this class
     * @throws FileNotFoundException
     */
    public static HarvesterServiceManager getInstance() {
        return instance;
    }

    private final Map<ServiceType, ServiceConfig> services = Collections
            .synchronizedMap(new EnumMap<>(ServiceType.class));

    /** Singleton instance of this class */
    private static final HarvesterServiceManager instance = new HarvesterServiceManager();

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(HarvesterServiceManager.class);

    /* Private Constructor */
    private HarvesterServiceManager() {
        try {
            readConfigXml();
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR, "Error reading config files: ",
                    e);
        }

        for (ServiceType serviceType : services.keySet()) {
            PathManagerFactory.getPathManager().addLocalizationPathObserver(
                    getConfigFileName(serviceType), this);
        }
    }

    @Override
    public void fileChanged(ILocalizationFile file) {
        try {
            readConfigXml();
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Serialization error reading file: ", e);
        }
    }

    /**
     * Gets the resultant configFileName
     *
     * @param serviceType
     * @return
     */
    public String getConfigFileName(ServiceType serviceType) {
        return CONFIG_FILE_NAME_PREFIX + serviceType + CONFIG_FILE_NAME_SUFFIX;
    }

    /**
     * Get the Service Configuration
     *
     * @param serviceType
     * @return
     */
    public ServiceConfig getServiceConfig(ServiceType serviceType) {
        ServiceConfig config = services.get(serviceType);

        if (config == null) {
            // try and load it
            try {
                readConfigXml();
                config = services.get(serviceType);
                // If you haven't found it by now, your just out of luck
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        }

        return config;
    }

    /**
     * Read the XML configuration data for the current XML files.
     */
    private void readConfigXml() throws Exception {
        for (ServiceType st : ServiceType.values()) {
            IPathManager pm = PathManagerFactory.getPathManager();
            SortedMap<LocalizationLevel, LocalizationFile> files = new TreeMap<>(
                    pm.getTieredLocalizationFile(LocalizationType.COMMON_STATIC,
                            getConfigFileName(st)));

            ServiceConfig sc = null;

            for (LocalizationFile lf : files.values()) {
                if (lf != null) {
                    try {
                        ServiceConfig tmpsc = readServiceConfigXml(lf);

                        if (sc == null) {
                            sc = tmpsc;
                        } else {
                            sc.combine(tmpsc);
                        }

                    } catch (Exception e) {
                        statusHandler.handle(Priority.PROBLEM,
                                "Couldn't read the Service Config file: "
                                        + lf.toString(),
                                e);
                    }
                }
            }

            if (sc != null) {
                services.put(st, sc);
            }
        }
    }

    /**
     * Read in service config XML
     *
     * @param file
     * @return
     */
    private ServiceConfig readServiceConfigXml(LocalizationFile file)
            throws Exception {
        ServiceConfig service = null;

        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                service = jaxb.unmarshalFromInputStream(is);
            }
        }

        return service;
    }

}
