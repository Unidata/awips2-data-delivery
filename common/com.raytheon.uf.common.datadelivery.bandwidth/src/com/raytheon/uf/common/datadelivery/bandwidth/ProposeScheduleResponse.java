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
package com.raytheon.uf.common.datadelivery.bandwidth;

import java.util.Collections;
import java.util.Set;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Response for a propose schedule operation.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 29, 2012 1286       djohnson     Initial creation
 * May 28, 2013 1650       djohnson     More information when failing to schedule.
 * Jun 20, 2017 6299       tgurney      Remove IProposeScheduleResponse
 *
 * </pre>
 *
 * @author djohnson
 */
@DynamicSerialize
public class ProposeScheduleResponse {

    public static final int VALUE_NOT_SET = -1;

    @DynamicSerializeElement
    private Set<String> unscheduledSubscriptions = Collections.emptySet();

    @DynamicSerializeElement
    private int requiredLatency = VALUE_NOT_SET;

    @DynamicSerializeElement
    private long requiredDataSetSize = VALUE_NOT_SET;

    /**
     * Get the set of subscription names that would be unscheduled if the
     * proposed schedule were to be force applied.
     *
     * @return the subscription names
     */
    public Set<String> getUnscheduledSubscriptions() {
        return unscheduledSubscriptions;
    }

    public void setUnscheduledSubscriptions(
            Set<String> unscheduledSubscriptions) {
        this.unscheduledSubscriptions = unscheduledSubscriptions;
    }

    public void setRequiredLatency(int requiredLatency) {
        this.requiredLatency = requiredLatency;
    }

    /**
     * Get the required latency for the subscription to not unschedule any
     * subscriptions.
     *
     * @return the required latency
     */
    public int getRequiredLatency() {
        return requiredLatency;
    }

    public void setRequiredDataSetSize(long requiredDataSetSize) {
        this.requiredDataSetSize = requiredDataSetSize;
    }

    /**
     * Get the required dataset size for the subscription to not unschedule any
     * subscriptions.
     *
     * @return the required dataset size, in kilobytes
     */
    public long getRequiredDataSetSize() {
        return requiredDataSetSize;
    }

}
