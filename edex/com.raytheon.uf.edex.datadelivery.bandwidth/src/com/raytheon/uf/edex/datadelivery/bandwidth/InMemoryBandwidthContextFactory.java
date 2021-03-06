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
package com.raytheon.uf.edex.datadelivery.bandwidth;

import java.io.File;

import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthMap;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.bandwidth.InMemoryBandwidthManager.InMemoryBandwidthInitializer;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthContextFactory;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthBucketDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDbInit;
import com.raytheon.uf.edex.datadelivery.bandwidth.interfaces.IBandwidthInitializer;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthDaoUtil;
import com.raytheon.uf.edex.registry.ebxml.util.RegistryIdUtil;

/**
 * Implementation of {@link BandwidthContextFactory} that returns DAO classes
 * for in-memory use. Intentionally package-private as only the Edex bandwidth
 * manager should be using it.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------
 * Oct 24, 2012  1286     djohnson  Initial creation
 * Jun 24, 2013  2106     djohnson  Add {@link #getBandwidthBucketDao()}.
 * Apr 22, 2014  2992     dhladky   Added IdUtil for siteList
 * May 22, 2014  2808     dhladky   Scheduling unscheduled
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Feb 02, 2018  6471     tjensen   Improve configuration file management
 *
 * </pre>
 *
 * @author djohnson
 */

class InMemoryBandwidthContextFactory implements BandwidthContextFactory {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(InMemoryBandwidthContextFactory.class);

    private static final ThreadLocal<File> BANDWIDTH_MAP_CONFIG_FILES = new ThreadLocal<>();

    private final IBandwidthDao dao = new InMemoryBandwidthDao();

    private final IBandwidthBucketDao bandwidthBucketsDao = new InMemoryBandwidthBucketDao();

    private final IBandwidthDbInit dbInit = new InMemoryBandwidthDbInit();

    private final IBandwidthInitializer initializer = new InMemoryBandwidthInitializer();

    @Override
    public IBandwidthDbInit getBandwidthDbInit() {
        return dbInit;
    }

    @Override
    public IBandwidthDao getBandwidthDao() {
        return dao;
    }

    @Override
    public IBandwidthInitializer getBandwidthInitializer() {
        return initializer;
    }

    public static File getBandwidthMapConfig() {
        File file = InMemoryBandwidthContextFactory.BANDWIDTH_MAP_CONFIG_FILES
                .get();

        if (file == null || !file.exists()) {
            statusHandler.warn(
                    "Unable to find expected in-memory bandwidth map configuration file... loading from the localization version.");
            file = EdexBandwidthContextFactory.getBandwidthMapConfig();
        }

        if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
            statusHandler.debug("Returning file reference ["
                    + file.getAbsolutePath()
                    + "] for in-memory bandwidth manager bandwidth map configuration.");
        }

        return file;
    }

    @Override
    public File getBandwidthMapConfigFile() {
        return getBandwidthMapConfig();
    }

    @Override
    public BandwidthManager getBandwidthManager(IBandwidthDbInit dbInit,
            IBandwidthDao bandwidthDao, RetrievalManager retrievalManager,
            BandwidthDaoUtil bandwidthDaoUtil, RegistryIdUtil idUtil) {
        return new InMemoryBandwidthManager(dbInit, bandwidthDao,
                retrievalManager, bandwidthDaoUtil, idUtil);
    }

    /**
     * Set the in-memory bandwidthmap config file. Package-level access on
     * purpose for access from {@link BandwidthManager}.
     *
     * @param bandwidthMap
     *            the bandwidthMap to use to create the file
     */
    static void setInMemoryBandwidthConfigFile(BandwidthMap bandwidthMap) {
        try {
            File tempFile = File.createTempFile("propose_bandwidth", ".xml");
            bandwidthMap.save(tempFile);
            InMemoryBandwidthContextFactory.BANDWIDTH_MAP_CONFIG_FILES
                    .set(tempFile);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Exception preparing the in-memory bandwidth map configuration, results will be non-deterministic",
                    e);
        }
    }

    /**
     * Deletes the in-memory bandwidthmap config file. Package-level access on
     * purpose for access from {@link BandwidthManager}.
     */
    static void deleteInMemoryBandwidthConfigFile() {
        File file = BANDWIDTH_MAP_CONFIG_FILES.get();
        if (file != null) {
            // No big deal if the file doesn't delete, since we created it in a
            // temp directory anyways
            file.delete();
            InMemoryBandwidthContextFactory.BANDWIDTH_MAP_CONFIG_FILES
                    .set(null);
        }
    }

    @Override
    public IBandwidthBucketDao getBandwidthBucketDao() {
        return bandwidthBucketsDao;
    }
}
