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
package com.raytheon.uf.edex.datadelivery.harvester.purge;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfigurationManager;
import com.raytheon.uf.common.datadelivery.registry.DataDeliveryRegistryObjectTypes;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.ITimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.ClusterIdUtil;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.datadelivery.harvester.crawler.CrawlLauncher;

/**
 * Purges {@link DataSetMetaData} instances that are no longer accessible on
 * their originating servers. This class performs the wiring up of specific
 * {@link DataSetMetaData} types with their {@link IServiceDataSetMetaDataPurge}
 * implementations.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 04, 2012  1102     djohnson  Initial creation
 * Oct 05, 2012  1241     djohnson  Replace RegistryManager calls with registry
 *                                  handler calls.
 * Dec 12, 2012  1410     dhladky   multi provider configurations.
 * Sep 30, 2013  1797     dhladky   Generics
 * Apr 12,2014   3012     dhladky   Purge never worked, fixed to make work.
 * Jan 18, 2016  5261     dhladky   Enabled purging for PDA.
 * Feb 18, 2016  5280     dhladky   Metadata not purging enough for PDA.
 * Apr 04, 2016  5424     dhladky   Metadata purge not efficient enough for PDA.
 * Sep 12, 2016  5846     tjensen   Improve purge to be more efficient
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
public class DataSetMetaDataPurgeTaskImpl implements IDataSetMetaDataPurgeTask {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DataSetMetaDataPurgeTaskImpl.class);

    /** Data access object for registry objects */
    private DataSetMetaDataDao dsmdDao;

    /** Purge 100 items at a time so we don't run hibernate out of shared memory **/
    private static int PURGE_BATCH_SIZE = 100;

    /**
     * Delete list of provider registry object Ids.
     * 
     * @param deleteIds
     * @param username
     */
    void purgeMetaData(List<String> deleteIds, String username) {

        try {
            DataDeliveryHandlers.getDataSetMetaDataHandler().deleteByIds(
                    username, deleteIds);
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to delete a DataSetMetaData instance!", e);
        }
    }

    /**
     * Default Constructor.
     */
    public DataSetMetaDataPurgeTaskImpl(DataSetMetaDataDao dsmdDao) {
        this.dsmdDao = dsmdDao;
    }

    /**
     * Returns the Retention times by Provider name.
     * 
     * @return the {@link HarvesterConfig}
     */
    @SuppressWarnings("deprecation")
    @VisibleForTesting
    static Map<String, String> getHarvesterConfigs() {

        // first get the Localization directory and find all harvester configs
        List<HarvesterConfig> configs = new ArrayList<>();

        // if many, start many
        for (LocalizationFile lf : CrawlLauncher.getLocalizedFiles()) {

            HarvesterConfig hc = null;
            try {
                hc = HarvesterConfigurationManager.getHarvesterFile(lf
                        .getFile());
            } catch (Exception se) {
                statusHandler.handle(Priority.PROBLEM,
                        se.getLocalizedMessage(), se);
            }

            // collect files
            if (hc != null) {
                if (hc.getAgent() != null) {
                    configs.add(hc);
                }
            }
        }

        Map<String, String> configMap = null;

        if (!configs.isEmpty()) {
            configMap = new HashMap<>(configs.size());
            for (HarvesterConfig config : configs) {
                configMap.put(config.getProvider().getName(),
                        config.getRetention());
            }
        } else {
            return Collections.emptyMap();
        }

        return configMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {

        statusHandler.info("Starting DataSetMetaData Purge...");
        ITimer timer = TimeUtil.getTimer();
        timer.start();

        Map<String, String> configMap = getHarvesterConfigs();
        int deletes = 0;

        // Query for Data Provider Names
        List<String> providerList = dsmdDao
                .getProviderNames(
                        DataDeliveryRegistryObjectTypes.DATASETMETADATA,
                        "providerName");

        // username is the name of this registry, ("NCF")
        String username = ClusterIdUtil.getId();

        // Loop over all the providers
        for (String provider : providerList) {
            try {
                Number retention = Double.valueOf(configMap.get(provider));

                if (retention == null) {
                    statusHandler
                            .warn("Retention time unreadable for this DataSetMetaData provider! "
                                    + "Provider: " + provider);
                    continue;
                }

                // no purging for this DSMD type
                if (retention.intValue() == -1) {
                    continue;
                }

                /*
                 * Retention is calculated in hours. We consider the whole day
                 * to be 24 hours. So, a value of .25 would be considered 6
                 * hours or, -24 * .25 = -6.0. Or with more than one day it
                 * could be, -24 * 7 = -168. We let Number int conversion round
                 * to nearest whole hour.
                 */
                retention = retention.doubleValue() * (-1)
                        * TimeUtil.HOURS_PER_DAY;

                // we are subtracting from current
                Calendar thresholdTime = TimeUtil.newGmtCalendar();
                thresholdTime.add(Calendar.HOUR_OF_DAY, retention.intValue());

                /*
                 * <pre> Query for the objects that are: 1) DataSetMetaData 2)
                 * Have the selected provider name 3) Have a date older than the
                 * threshold time </pre>
                 */
                BigInteger retentionTime = BigInteger.valueOf(thresholdTime
                        .getTimeInMillis());
                int numIds = PURGE_BATCH_SIZE;
                boolean doShutDown = EDEXUtil.isShuttingDown();
                while (!doShutDown && numIds == PURGE_BATCH_SIZE) {
                    List<String> ids = dsmdDao.getIdsBeyondRetention(
                            DataDeliveryRegistryObjectTypes.DATASETMETADATA,
                            "providerName", provider, "date", retentionTime,
                            PURGE_BATCH_SIZE);
                    numIds = ids.size();
                    purgeMetaData(ids, username);
                    deletes += numIds;
                    doShutDown = EDEXUtil.isShuttingDown();
                }
                if (doShutDown) {
                    statusHandler.warn("Purge interupted by shutdown!");
                }

            } catch (Exception e) {
                statusHandler.error("DataSetMetaData purge gather for "
                        + provider + " failed! ", e);
            }
        }

        timer.stop();
        statusHandler.info(String.format(
                "DataSetMetaData purge completed in %s ms.",
                timer.getElapsedTime() + " deleted: " + deletes));
    }
}
