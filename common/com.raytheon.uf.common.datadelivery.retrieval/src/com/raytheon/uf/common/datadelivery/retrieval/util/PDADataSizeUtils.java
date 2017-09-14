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

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.EnvelopeUtils;
import com.raytheon.uf.common.datadelivery.registry.PDADataSet;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * PDA implementation for DataSizeUtils.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 20, 2014  3121     dhladky   creation
 * Apr 19, 2016  5424     dhladky   Re-visited sizing.
 * Apr 05, 2017  1045     tjensen   Add support for estimated size for moving
 *                                  datasets
 * Sep 12, 2017  6413     tjensen   Updated to support ParameterGroups
 *
 * </pre>
 *
 * @author dhladky
 * @version 1.0
 */

public class PDADataSizeUtils extends DataSizeUtils<PDADataSet> {

    /**
     * Constructor.
     *
     * @param dataSet
     *            The dataSet
     */
    public PDADataSizeUtils(PDADataSet dataSet) {
        this.dataSet = dataSet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getFullSizeInBytes() {

        if (dataSet != null) {
            if (fullSize == -999) {
                Coverage coverage = dataSet.getCoverage();
                ReferencedEnvelope envelope = coverage.getEnvelope();
                /**
                 * Since we don't have any NX/NY resolution info provided in the
                 * metadata for PDA. The only "Scientific" estimate we can do
                 * for actual size of requests is estimate based on the LAT LON
                 * span of the request multiplied by a constant. This estimate
                 * will have to do until we have REAL values for PDA after EOP
                 * is implemented.
                 *
                 * The constant 5 is chosen because 5/5 in the
                 * getDataSetSizeInBytes calculation will yield a "1". Then this
                 * is multiplied by the constant 5000 bytes assumed as overhead
                 * for PDA. So, a 1X1 degree grid span is around 5000 bytes of
                 * data and calculated progressively from there.
                 */
                fullSize = getDataSetSizeInBytes(dataSet.getParameterGroups(),
                        envelope);
            }
        }

        return fullSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDataSetSizeInBytes(Subscription<?, ?> subscription) {
        return getDataSetSizeInBytes(subscription.getParameterGroups(),
                subscription.getCoverage().getRequestEnvelope());
    }

    /**
     * Get the data set size.
     *
     * @param envelope
     *            The areal envelope
     * @param interval
     *            The data retrieval interval
     * @return Data set size in bytes
     */
    private long getDataSetSizePerParamInBytes(ReferencedEnvelope envelope) {
        if (dataSet.isMoving()) {
            return dataSet.getEstimatedSize();
        }

        /**
         * interval is overridden here, there is only one time per PDA retrieval
         */
        Coordinate ur = EnvelopeUtils.getUpperRightLatLon(envelope);
        Coordinate ll = EnvelopeUtils.getLowerLeftLatLon(envelope);
        double lonSpan = Math.abs(ll.x - ur.x);
        double latSpan = Math.abs(ll.y - ur.y);

        return dataSet.getServiceType().getRequestBytesPerLatLonBoxAndTime(
                latSpan, lonSpan, TIME_SIZE);
    }

    public long getDataSetSizeInBytes(Map<String, ParameterGroup> paramMap,
            ReferencedEnvelope envelope) {
        long sizePerParam = 0;
        sizePerParam = getDataSetSizePerParamInBytes(envelope);
        int totalParameterLevels = getNumberParameterLevels(paramMap);

        return totalParameterLevels * sizePerParam;

    }
}
