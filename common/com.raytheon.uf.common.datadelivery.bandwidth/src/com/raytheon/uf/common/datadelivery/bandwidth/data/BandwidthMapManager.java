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
package com.raytheon.uf.common.datadelivery.bandwidth.data;

import java.io.File;
import java.util.Map;

import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.ILocalizationFileObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.util.FileUtil;

/**
 * The {@link BandwidthMap} file manager class for scheduling.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 6, 2014            mpduff     Initial creation
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class BandwidthMapManager {

    /** Singleton instance */
    private static BandwidthMapManager instance;

    /** Config file backing the {@link BandwidthMap} */
    private static final String CONFIG_FILE = FileUtil.join("datadelivery",
            "bandwidthmap.xml");

    /** Config Localization file */
    private LocalizationFile locFile;

    /** The {@link BandwidthMap} */
    private BandwidthMap bandwidthMap;

    /**
     * private constructor
     */
    private BandwidthMapManager() {
        readBandwidthMapConfig();
    }

    /**
     * Get an instance.
     * 
     * @return the instance
     */
    public static synchronized final BandwidthMapManager getInstance() {
        if (instance == null) {
            instance = new BandwidthMapManager();
        }

        return instance;
    }

    /**
     * Read the config file and populate the {@link BandwidthMap} object
     */
    private void readBandwidthMapConfig() {
        IPathManager pm = PathManagerFactory.getPathManager();
        Map<LocalizationLevel, LocalizationFile> fileMap = pm
                .getTieredLocalizationFile(LocalizationType.COMMON_STATIC,
                        CONFIG_FILE);
        locFile = fileMap.get(LocalizationLevel.SITE);
        if (locFile == null) {
            locFile = fileMap.get(LocalizationLevel.BASE);
        }
        locFile.addFileUpdatedObserver(new ILocalizationFileObserver() {
            @Override
            public void fileUpdated(FileUpdatedMessage message) {
                if (message.getFileName().equals(CONFIG_FILE)) {
                    readBandwidthMapConfig();
                }
            }
        });

        File file = locFile.getFile();
        this.bandwidthMap = BandwidthMap.load(file);
    }

    /**
     * Get the {@link BandwidthMap} data object.
     * 
     * @return The data object
     */
    public BandwidthMap getBandwidthMap() {
        return bandwidthMap;
    }
}
