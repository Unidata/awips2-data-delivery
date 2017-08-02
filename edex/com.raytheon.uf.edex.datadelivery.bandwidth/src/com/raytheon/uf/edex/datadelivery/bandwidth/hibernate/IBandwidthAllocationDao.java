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
package com.raytheon.uf.edex.datadelivery.bandwidth.hibernate;

import java.util.Date;

import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;

/**
 * BandwidthAllocation dao.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Feb 13, 2013  1543     djohnson  Initial creation
 * Aug 02, 2017  6186     rjpeter   Added purgeAllocations
 *
 * </pre>
 *
 * @author djohnson
 */
interface IBandwidthAllocationDao
        extends IBaseBandwidthAllocationDao<BandwidthAllocation> {

    /**
     * Purge all allocations prior to the threshold.
     *
     * @param purgeThreshold
     */
    void purgeAllocations(Date purgeThreshold);
}