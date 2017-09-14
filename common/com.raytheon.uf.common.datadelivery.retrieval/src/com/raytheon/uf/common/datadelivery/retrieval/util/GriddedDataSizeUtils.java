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

import java.util.Map;

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.raytheon.uf.common.datadelivery.registry.Ensemble;
import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedDataSet;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.gridcoverage.GridCoverage;

/**
 * Gridded implementation of DataSizeUtils
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 13, 2013  2108     mpduff    Initial creation.
 * Sep 25, 2013  1797     dhladky   separated time from gridded time
 * Nov 20, 2013  2554     dhladky   Generics
 * Apr 05, 2017  1045     tjensen   Add support for estimated size for moving
 *                                  datasets
 * Sep 12, 2017  6413     tjensen   Updated to support ParameterGroups
 *
 * </pre>
 *
 * @author mpduff
 */

public class GriddedDataSizeUtils extends DataSizeUtils<GriddedDataSet> {

    /**
     * Gridded constructor.
     *
     * @param dataSet
     *            the data set
     */
    public GriddedDataSizeUtils(GriddedDataSet dataSet) {
        this.dataSet = dataSet;
    }

    /**
     * Calculate the number of grid cells for the envelope.
     *
     * @param envelope
     *            The areal envelope
     * @return number of grid cells
     */
    private int calculateGridCells(ReferencedEnvelope envelope) {
        if (dataSet != null) {

            GriddedCoverage griddedCov = dataSet.getCoverage();

            GridCoverage subgridCov = griddedCov
                    .getRequestGridCoverage(envelope);
            if (subgridCov == null) {
                subgridCov = griddedCov.getGridCoverage();
            }
            int nx = subgridCov.getNx();
            int ny = subgridCov.getNy();

            return nx * ny;

        }

        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getFullSizeInBytes() {
        if (dataSet != null) {
            if (fullSize == -999) {

                long numEns = 1;
                long fcstHrs = dataSet.getForecastHours().size();

                if (dataSet.getEnsemble() != null) {
                    numEns = dataSet.getEnsemble().getMemberCount();
                }

                // get the number of grids available
                Map<String, ParameterGroup> paramMap = dataSet
                        .getParameterGroups();
                long numGridsAvailable = getNumberParameterLevels(paramMap);

                long bytesPerParameterPerLevel = 0;
                if (dataSet.isMoving()) {
                    bytesPerParameterPerLevel = dataSet.getEstimatedSize();
                } else {
                    GriddedCoverage griddedCov = dataSet.getCoverage();
                    long numCells = griddedCov.getGridCoverage().getNx()
                            * griddedCov.getGridCoverage().getNy();
                    bytesPerParameterPerLevel = dataSet.getServiceType()
                            .getRequestBytesPerParameterPerLevel(numCells);
                }
                fullSize = numEns * fcstHrs * numGridsAvailable
                        * bytesPerParameterPerLevel;
            }
        }

        return fullSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDataSetSizeInBytes(Subscription<?, ?> subscription) {
        final Ensemble ensemble = subscription.getEnsemble();
        int numEnsemble = (ensemble == null) ? 1 : ensemble.getMemberCount();

        return getDataSetSizeInBytes(subscription.getParameterGroups(),
                ((GriddedTime) subscription.getTime()).getSelectedTimeIndices()
                        .size(),
                numEnsemble, subscription.getCoverage().getRequestEnvelope());
    }

    /**
     * Get the data set size in bytes.
     *
     * @param params
     *            List of parameters
     * @param numFcstHrs
     *            Number of forecast hours
     * @param numEnsembleMembers
     *            Number of ensemble members
     * @param envelope
     *            ReferencedEnvelope
     * @return Number of bytes
     */
    public long getDataSetSizeInBytes(Map<String, ParameterGroup> params,
            int numFcstHrs, int numEnsembleMembers,
            ReferencedEnvelope envelope) {

        int numRequestedGrids = getNumberParameterLevels(params);

        long bytesPerParameterPerLevel = 0;
        if (dataSet.isMoving()) {
            bytesPerParameterPerLevel = dataSet.getEstimatedSize();
        } else {
            bytesPerParameterPerLevel = dataSet.getServiceType()
                    .getRequestBytesPerParameterPerLevel(
                            calculateGridCells(envelope));
        }
        long l = numRequestedGrids * numFcstHrs * numEnsembleMembers
                * bytesPerParameterPerLevel;
        return l;
    }
}
