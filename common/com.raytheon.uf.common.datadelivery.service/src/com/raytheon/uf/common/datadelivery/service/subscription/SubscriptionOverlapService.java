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
package com.raytheon.uf.common.datadelivery.service.subscription;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.SaveableOutputStream;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.FileUtil;

/**
 * Read/Write subscription overlap config files.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 07, 2013  2000      djohnson     Initial creation
 * Jun 04, 2013  223       mpduff       Get base file if site doesn't exist.
 * Sep 23, 2013  2283      dhladky      Updated for multiple configs
 * Oct 03, 2013  2386      mpduff       Moved the subscription overlap rules files into the rules directory.
 * Oct 25, 2013  2292      mpduff       Move overlap checks to edex.
 * Nov 12, 2013  2361      njensen      Made JAXBManager static and initialized on first use
 * Nov 10, 2015  4644      dhladky      Added PDA overlap strategies
 * Jan 18, 2016  5260      dhladky      Updated with better values.
 * Jan 20, 2016  5244      njensen      Replaced calls to deprecated LocalizationFile methods
 * Mar 16, 2016  3919      tjensen      Cleanup unneeded interfaces
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class SubscriptionOverlapService<T extends Time, C extends Coverage> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SubscriptionOverlapService.class);

    private static final String UNABLE_TO_UNMARSHAL = "Unable to unmarshal the configuration file.  "
            + "No subscriptions will be considered to overlap!";

    private static final String SUBSCRIPTION_OVERLAP_CONFIG_FILE_ROOT = "SubscriptionOverlapRules.xml";

    private static final String SUBSCRIPTION_OVERLAP_CONFIG_FILE_PATH = FileUtil
            .join("datadelivery", "systemManagement", "rules", File.separator);

    private static JAXBManager jaxbManager;

    private static synchronized JAXBManager getJaxbManager() {
        if (jaxbManager == null) {
            try {
                Class<?>[] clazzes = new Class<?>[] {
                        SubscriptionOverlapConfig.class,
                        GridSubscriptionOverlapConfig.class,
                        PointSubscriptionOverlapConfig.class,
                        PDASubscriptionOverlapConfig.class };
                jaxbManager = new JAXBManager(clazzes);
            } catch (JAXBException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        return jaxbManager;
    }

    /**
     * Constructor.
     */
    public SubscriptionOverlapService() {

    }

    /**
     * Writes a new configuration file.
     * 
     * @param config
     *            the configuration
     * @throws LocalizationException
     *             on error saving the configuration
     */
    public void writeConfig(SubscriptionOverlapConfig config)
            throws LocalizationException {
        final IPathManager pathManager = PathManagerFactory.getPathManager();
        LocalizationContext context = pathManager.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.SITE);

        String fileName = null;

        if (config instanceof PointSubscriptionOverlapConfig) {
            fileName = SUBSCRIPTION_OVERLAP_CONFIG_FILE_PATH
                    + DataType.POINT.name()
                    + SUBSCRIPTION_OVERLAP_CONFIG_FILE_ROOT;
        } else if (config instanceof GridSubscriptionOverlapConfig) {
            fileName = SUBSCRIPTION_OVERLAP_CONFIG_FILE_PATH
                    + DataType.GRID.name()
                    + SUBSCRIPTION_OVERLAP_CONFIG_FILE_ROOT;
        } else if (config instanceof PDASubscriptionOverlapConfig) {
            fileName = SUBSCRIPTION_OVERLAP_CONFIG_FILE_PATH
                    + DataType.PDA.name()
                    + SUBSCRIPTION_OVERLAP_CONFIG_FILE_ROOT;
        } else {
            throw new IllegalArgumentException(config.getClass()
                    + " Doesn't have any implementation in use");
        }

        ILocalizationFile configFile = pathManager.getLocalizationFile(context,
                fileName);
        JAXBManager jaxb = getJaxbManager();
        try (SaveableOutputStream sos = configFile.openOutputStream()) {
            jaxb.marshalToStream(config, sos);
            sos.save();
        } catch (IOException | SerializationException e) {
            throw new LocalizationException("Error saving config file "
                    + configFile.getPath(), e);
        }
    }

    /**
     * Get the overlap config file for this data type.
     * 
     * @param type
     *            The data type
     * @return the config file for the data type
     */
    public SubscriptionOverlapConfig getConfigFile(DataType type) {
        final IPathManager pathManager = PathManagerFactory.getPathManager();
        SubscriptionOverlapConfig config = null;
        ILocalizationFile localizationFile = pathManager
                .getStaticLocalizationFile(SUBSCRIPTION_OVERLAP_CONFIG_FILE_PATH
                        + type.name() + SUBSCRIPTION_OVERLAP_CONFIG_FILE_ROOT);

        try {
            if (!localizationFile.exists()) {
                throw new MissingResourceException(localizationFile.getPath()
                        + " does not exist.",
                        SubscriptionOverlapConfig.class.getName(),
                        "Not yet implemented!");
            }

            JAXBManager jaxb = getJaxbManager();
            try (InputStream is = localizationFile.openInputStream()) {
                Object obj = jaxb.unmarshalFromInputStream(is);
                switch (type) {
                case GRID:
                    config = (GridSubscriptionOverlapConfig) obj;
                    break;
                case POINT:
                    config = (PointSubscriptionOverlapConfig) obj;
                    break;
                case PDA:
                    config = (PDASubscriptionOverlapConfig) obj;
                    break;
                }
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, UNABLE_TO_UNMARSHAL,
                    e.getLocalizedMessage());
            switch (type) {
            // this a fall back so at least some checking gets done
            case GRID:
                config = new GridSubscriptionOverlapConfig().getNeverOverlaps();
                break;
            case POINT:
                config = new PointSubscriptionOverlapConfig()
                        .getNeverOverlaps();
                break;
            case PDA:
                config = new PDASubscriptionOverlapConfig().getNeverOverlaps();
                break;
            }
        }

        return config;
    }
}
