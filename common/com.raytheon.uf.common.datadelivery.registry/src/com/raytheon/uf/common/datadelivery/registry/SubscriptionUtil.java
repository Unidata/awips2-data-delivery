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
package com.raytheon.uf.common.datadelivery.registry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.registry.util.AveragingAvailablityCalculator;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;

/**
 * Common subscription utilities.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 19, 2014   3113     mpduff      Initial creation
 * 8/29/2014     3446      bphillip     SubscriptionUtil is now a singleton
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class SubscriptionUtil {
    public static final int MISSING = -999;

    private static final int MAX_ALLOWED_FOR_AVERAGE = 10;

    private static final long WEEK_THRESHOLD = TimeUtil.MILLIS_PER_WEEK;

    private final AveragingAvailablityCalculator dataSetAvailabilityCalculator;
    
    private static final SubscriptionUtil instance = new SubscriptionUtil();

    private SubscriptionUtil() {
        dataSetAvailabilityCalculator = new AveragingAvailablityCalculator(
                DataDeliveryHandlers.getDataSetMetaDataHandler());
    }
    
    public static SubscriptionUtil getInstance(){
        return instance;
    }

    /**
     * Calculates an average interval between DataSetMetaData objects in minutes
     * using up to 10 values.
     * 
     * @param dsmdList
     *            DataSetMetaData list
     * @return average in minutes of the last 10 or MISSING if unable to
     *         calculate average
     */
    public int calculateInterval(List<DataSetMetaData> dsmdList) {
        long interval = MISSING;
        if (CollectionUtil.isNullOrEmpty(dsmdList)) {
            return MISSING;
        }

        List<Date> dateList = getSortedArrivalTimes(dsmdList);

        if (CollectionUtil.isNullOrEmpty(dateList)) {
            return MISSING;
        }

        long total = 0;
        int count = 0;
        Date prev = dateList.get(dateList.size() - 1);
        for (int i = dateList.size() - 2; i >= 0; i--) {
            Date d = dateList.get(i);
            total += prev.getTime() - d.getTime();
            prev = d;
            count++;
            if (count > MAX_ALLOWED_FOR_AVERAGE) {
                break;
            }
        }

        if (count > 0) {
            interval = total / count;
            return (int) (interval / TimeUtil.MILLIS_PER_MINUTE);
        }

        return MISSING;
    }

    /**
     * Get a list of sorted arrival times.
     * 
     * @param dsmdList
     *            List of DataSetMetaData objects
     * @return List of sorted arrival times
     */
    private List<Date> getSortedArrivalTimes(List<DataSetMetaData> dsmdList) {
        List<Date> dateList = new ArrayList<Date>(dsmdList.size());
        for (DataSetMetaData dsmd : dsmdList) {
            if (dsmd.getArrivalTime() == 0) {
                continue;
            }
            Date d = new Date(dsmd.getArrivalTime());
            if (TimeUtil.currentTimeMillis() - d.getTime() > WEEK_THRESHOLD) {
                continue;
            }
            dateList.add(new Date(dsmd.getArrivalTime()));
        }

        Collections.sort(dateList);

        return dateList;
    }

    /**
     * Calculate the number of minutes of offset between a Subscriptions base
     * reference time and the time the data should be available.
     * 
     * @param subscription
     *            The Subscription Object to obtain the availability for.
     * @param referenceTime
     *            Data reference time
     * @return The offset in minutes.
     * @throws RegistryHandlerException
     */
    public int getDataSetAvailablityOffset(Subscription<?, ?> subscription,
            Calendar referenceTime) throws RegistryHandlerException {
        return dataSetAvailabilityCalculator.getDataSetAvailablityOffset(
                subscription, referenceTime);
    }

    /**
     * Get the latest arrival time from the list of {@link DataSetMetaData}
     * objects.
     * 
     * @param dsmdList
     *            LIst of DataSetMetaData objects
     * @return The latest arrival time or null if list is empty
     */
    public Date getLatestArrivalTime(List<DataSetMetaData> dsmdList) {
        List<Date> dateList = getSortedArrivalTimes(dsmdList);
        if (CollectionUtil.isNullOrEmpty(dateList)) {
            return null;
        }

        return dateList.get(dateList.size() - 1);
    }
}