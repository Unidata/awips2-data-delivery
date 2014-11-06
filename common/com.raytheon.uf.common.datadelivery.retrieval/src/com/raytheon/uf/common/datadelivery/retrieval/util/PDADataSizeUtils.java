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

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.raytheon.uf.common.datadelivery.registry.EnvelopeUtils;
import com.raytheon.uf.common.datadelivery.registry.PDADataSet;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * PDA implementation for DataSizeUtils.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 20, 2014   3121     dhladky     creation
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
        // Not applicable for point data sets
        return -999;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDataSetSizeInBytes(Subscription<?, ?> subscription) {
        return getDataSetSizeInBytes(subscription.getCoverage()
                .getRequestEnvelope(),
                subscription.getTime().getNumTimes());
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
    public long getDataSetSizeInBytes(ReferencedEnvelope envelope, int interval) {
        Coordinate ur = EnvelopeUtils.getUpperRightLatLon(envelope);
        Coordinate ll = EnvelopeUtils.getLowerLeftLatLon(envelope);
        double lonSpan = Math.abs(ll.x - ur.x);
        double latSpan = Math.abs(ll.y - ur.y);

        return dataSet.getServiceType().getRequestBytesPerLatLonBoxAndTime(
                latSpan, lonSpan, interval);
    }
}

