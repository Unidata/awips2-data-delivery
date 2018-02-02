package com.raytheon.uf.edex.datadelivery.bandwidth.retrieval;

import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;

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

/**
 *
 * Object containing an unscheduled bandwidth allocation and relevant
 * information.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 2, 2018  6471       tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
public class UnscheduledAllocationReport
        implements Comparable<UnscheduledAllocationReport> {

    private BandwidthAllocation unscheduled;

    /** Available schedule in kilobytes for the requested window */
    private long scheduleAvailableInKB;

    /** Start time for the allocation period in milliseconds */
    private long windowStartMillis;

    /** End time for the allocation period in milliseconds */
    private long windowEndMillis;

    public UnscheduledAllocationReport(BandwidthAllocation unscheduled,
            long scheduleAvailableInKB, long windowStartMillis,
            long windowEndMillis) {
        super();
        this.unscheduled = unscheduled;
        this.scheduleAvailableInKB = scheduleAvailableInKB;
        this.windowStartMillis = windowStartMillis;
        this.windowEndMillis = windowEndMillis;
    }

    public BandwidthAllocation getUnscheduled() {
        return unscheduled;
    }

    public void setUnscheduled(BandwidthAllocation unscheduled) {
        this.unscheduled = unscheduled;
    }

    public long getScheduleAvailableInKB() {
        return scheduleAvailableInKB;
    }

    public void setScheduleAvailableInKB(long scheduleAvailableInKB) {
        this.scheduleAvailableInKB = scheduleAvailableInKB;
    }

    public long getWindowStartMillis() {
        return windowStartMillis;
    }

    public void setWindowStartMillis(long windowStartMillis) {
        this.windowStartMillis = windowStartMillis;
    }

    public long getWindowEndMillis() {
        return windowEndMillis;
    }

    public void setWindowEndMillis(long windowEndMillis) {
        this.windowEndMillis = windowEndMillis;
    }

    @Override
    public String toString() {
        return unscheduled.toString();
    }

    @Override
    public int compareTo(UnscheduledAllocationReport o) {
        return unscheduled.compareTo(o.getUnscheduled());

    }
}
