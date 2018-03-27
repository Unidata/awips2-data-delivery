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

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.ILocalizationPathObserver;
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
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 06, 2014           mpduff    Initial creation
 * Dec 14, 2015  5204     dhladky   Update to new ILocalizationPathObserver
 *                                  pattern.
 * Feb 02, 2018  6471     tjensen   Improved handling of configuation levels
 *
 * </pre>
 *
 * @author mpduff
 */

public class BandwidthMapManager implements ILocalizationPathObserver {

    /** Singleton instance */
    private static BandwidthMapManager instance;

    /** Config file backing the {@link BandwidthMap} */
    public static final String CONFIG_FILE = FileUtil.join("datadelivery",
            "bandwidthmap.xml");

    /** The {@link BandwidthMap} */
    private BandwidthMap bandwidthMap;

    /** Flags whether BandwidthMapManager has been initialized **/
    private boolean isInit = false;

    /**
     * private constructor
     */
    private BandwidthMapManager() {

    }

    /**
     * Get an instance.
     *
     * @return the instance
     */
    public static final synchronized BandwidthMapManager getInstance() {
        if (instance == null) {
            instance = new BandwidthMapManager();
            instance.readBandwidthMapConfig();
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
        LocalizationFile locFile = fileMap.get(LocalizationLevel.SITE);

        if (locFile == null) {
            locFile = fileMap.get(LocalizationLevel.BASE);
        }

        if (!isInit) {
            pm.addLocalizationPathObserver(locFile.getPath(), instance);
            isInit = true;
        }

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

    @Override
    public void fileChanged(ILocalizationFile file) {
        readBandwidthMapConfig();
    }
}
