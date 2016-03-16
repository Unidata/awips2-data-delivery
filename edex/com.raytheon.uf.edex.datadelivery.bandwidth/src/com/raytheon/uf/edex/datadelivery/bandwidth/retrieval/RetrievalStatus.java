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
package com.raytheon.uf.edex.datadelivery.bandwidth.retrieval;

import com.raytheon.uf.edex.datadelivery.bandwidth.BandwidthManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;

/**
 * 
 * Enumeration of the various stages {@link BandwidthManager} uses during the
 * processing of a {@link BandwidthAllocation}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 26, 2012 1286       djohnson     Added SW history and UNSCHEDULED.
 * 
 * </pre>
 * 
 * @author jspinks
 * @version 1.0
 */
public enum RetrievalStatus {
    // Intermediate state during processing
    PROCESSING,
    // In process of being retrieved
    RETRIEVAL,
    // Retrieval completed
    FULFILLED,
    // Canceled by user request
    CANCELLED,
    // BandwidthAllocation has been successfully scheduled.
    SCHEDULED,
    // Processing of the BandwidthAllocation failed
    FAILED,
    // BandwidthAllocation is ready to be processed.
    READY,
    // BandwidthAllocation has been rescheduled as a result of processing
    RESCHEDULE,
    // Reserve space in the RetrievalPlan for BandwidthAllocations
    // that do not fit into one BandwidthBucket.
    RESERVED,
    // The allocation has been postponed as the requested time is outside
    // the current plan time.
    DEFERRED,
    // The allocation was unable to be scheduled, or was otherwise removed from
    // the retrieval plan
    UNSCHEDULED;
}
