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
import com.raytheon.uf.edex.datadelivery.harvester.crawler.CrawlLauncher;
import com.raytheon.uf.edex.registry.ebxml.dao.RegistryObjectDao;

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
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 04, 2012 1102       djohnson     Initial creation
 * Oct 05, 2012 1241       djohnson     Replace RegistryManager calls with registry handler calls.
 * Dec 12, 2012 1410       dhladky      multi provider configurations.
 * Sept 30, 2013 1797      dhladky      Generics
 * Apr 12,2014   3012     dhladky      Purge never worked, fixed to make work.
 * Jan 18, 2016  5261     dhladky      Enabled purging for PDA.
 * Feb 18, 2016  5280     dhladky      Metadata not purging enough for PDA.
 * Apr 04, 2016  5424     dhladky      Metadata purge not efficient enough for PDA.
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
    private RegistryObjectDao rdo;
    
    /** Purge 100 items at a time so we don't run hibernate out of shared memory **/
    private static int PURGE_BATCH_SIZE = 100;
    
    /**
     * Delete list of provider registry object Ids.
     * @param deleteIds
     * @param username
     */
    static void purgeMetaData(List<String> deleteIds, String username) {

        try {
            DataDeliveryHandlers.getDataSetMetaDataHandler().deleteByIds(username, deleteIds);
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to delete a DataSetMetaData instance!", e);
        }
    }

    /**
     * Default Constructor.
     */
    public DataSetMetaDataPurgeTaskImpl(RegistryObjectDao rdo) {
        this.rdo = rdo;
    }
  
    /**
     * Gets the entire list of DSMD ids from the registry.
     * 
     * @return the map
     */
    @VisibleForTesting
    List<String> getDataSetMetaDataIds() {
        ArrayList<String> ids = null;
        try {
            // Gets the list of all available lids for current DataSetMetaData objects
            ids = (ArrayList<String>) rdo.getRegistryObjectIdsOfType(DataDeliveryRegistryObjectTypes.DATASETMETADATA);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to retrieve DataSetMetaData ids!", e);
            return Collections.emptyList();
        }

        return ids;
    }

    /**
     * Returns the Retention times by Provider name.
     * 
     * @return the {@link HarvesterConfig}
     */
    @SuppressWarnings("deprecation")
    @VisibleForTesting
    static Map<String, String> getHarvesterConfigs() {

        // first get the Localization directory and find all harvester
        // configs
        List<HarvesterConfig> configs = new ArrayList<HarvesterConfig>();

        // if many, start many
        for (LocalizationFile lf : CrawlLauncher.getLocalizedFiles()) {

            HarvesterConfig hc = null;
            try {
                hc = HarvesterConfigurationManager.getHarvesterFile(lf.getFile());
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
            configMap = new HashMap<String, String>(
                    configs.size());
            for (HarvesterConfig config : configs) {
                configMap.put(config.getProvider().getName(), config.getRetention());
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

        ITimer timer = TimeUtil.getTimer();
        timer.start();

        List<String> idList = getDataSetMetaDataIds();
        Map<String, List<String>> deleteMap = new HashMap<String, List<String>>();
        Map<String, String> configMap = getHarvesterConfigs();
        int deletes = 0;
        
        for (String id : idList) {
            try {
                DataSetMetaData<?> metaData = DataDeliveryHandlers
                        .getDataSetMetaDataHandler().getById(id);
                Number retention = Double.valueOf(configMap.get(metaData.getProviderName()));

                if (retention != null) {

                    if (retention.intValue() == -1) {
                        // no purging for this DSMD type
                        continue;
                    } else {
                        /* 
                         * Retention is calculated in hours.
                         * We consider the whole day to be 24 hours.
                         * So, a value of .25 would be considered 6 hours or, -24 * .25 = -6.0.
                         * Or with more than one day it could be, -24 * 7 = -168.
                         * We let Number int conversion round to nearest whole hour.
                         */
                        retention = retention.doubleValue() * (-1) * TimeUtil.HOURS_PER_DAY;
                        
                        // we are subtracting from current
                        Calendar thresholdTime = TimeUtil.newGmtCalendar();
                        thresholdTime.add(Calendar.HOUR_OF_DAY, retention.intValue());

                        if (thresholdTime.getTimeInMillis() >= metaData
                                .getDate().getTime()) {
                            if (deleteMap.containsKey(metaData.getProviderName())) {
                                deleteMap.get(metaData.getProviderName()).add(id);
                            } else {
                                List<String> deleteList = new ArrayList<String>(1);
                                deleteList.add(id);
                                deleteMap.put(metaData.getProviderName(), deleteList);
                            }
                        }
                    } 
                } else {
                    statusHandler
                            .warn("Retention time unreadable for this DataSetMetaData provider! "
                                    + id
                                    + "Provider: "
                                    + metaData.getProviderName());
                }

            } catch (Exception e) {
                statusHandler.error("DataSetMetaData purge gather failed! " + id, e);
            }
        }
        
        if (!deleteMap.isEmpty()) {
            
            // username is the name of this registry, ("NCF") 
            String username = ClusterIdUtil.getId();
            
            for (String providerKey: deleteMap.keySet()) {
                // delete the ids for this provider.
                List<String> ids = deleteMap.get(providerKey);
                
                if (ids.size() <= PURGE_BATCH_SIZE) {
                    purgeMetaData(ids, username);
                    deletes += ids.size();
                } else {
                    // break it into chunks, + 1 if remainder
                    int mod = ids.size() % PURGE_BATCH_SIZE;
                    Float total = (float) (ids.size()/PURGE_BATCH_SIZE);
                    int batches = (int) Math.floor(total);
                    if (mod != 0) {
                        batches = batches + 1;
                    } 

                    int start = 0;
                    int end = PURGE_BATCH_SIZE;

                    for (int i = 0; i < batches; i++) {
                        
                        if (i > 0) {
                            start = end;
                            if ((start + PURGE_BATCH_SIZE) < ids.size()) {
                                end = start + PURGE_BATCH_SIZE;
                            } else {
                                end = ids.size();
                            }
                        }

                        List<String> batch = ids.subList(start, end);
                        purgeMetaData(batch, username);
                        deletes += batch.size();
                    }
                }
            }
        }

        timer.stop();
        statusHandler.info(String.format(
                "DataSetMetaData purge completed in %s ms.",
                timer.getElapsedTime()+" deleted: "+deletes));
    }
}
