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
package com.raytheon.uf.edex.datadelivery.bandwidth.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.JarUtil;
import com.raytheon.uf.edex.core.modes.EDEXModesUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription;

/**
 * Bandwidth Manager utility methods.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 26, 2012  726      jspinks   Initial creation
 * Oct 10, 2012  726      djohnson  Add bandwidthManagementEnabled, some more
 *                                  utility methods, use availability delay to
 *                                  determine which starting hours to schedule.
 * Nov 09, 2012  1286     djohnson  Separate DAO utility methods from general
 *                                  utility.
 * Dec 11, 2012  1403     djohnson  No longer valid to run without bandwidth
 *                                  management.
 * Feb 14, 2013  1595     djohnson  Use subscription rescheduling strategy.
 * Jun 13, 2013  2095     djohnson  Point subscriptions don't check for dataset
 *                                  updates on aggregation.
 * Jun 25, 2013  2106     djohnson  CheapClone was cheap in ease, not
 *                                  performance.
 * Jul 11, 2013  2106     djohnson  Use SubscriptionPriority enum.
 * Oct 30, 2013  2448     dhladky   Moved methods to TimeUtil.
 * Dec 20, 2013  2636     mpduff    Changed dataset delay to offset.
 * Jan 08, 2014  2615     bgonzale  Moved Calendar min and max methods to
 *                                  TimeUtil.
 * Apr 09, 2014  3012     dhladky   GMT Calendar use.
 * Jun 09, 2014  3113     mpduff    Moved getDataSetAvailablityOffset to
 *                                  SubscriptionUtil.
 * Nov 03, 2014  2414     dhladky   Refactored and moved some BWM methods.
 * May 27, 2015  4531     dhladky   Remove excessive Calendar references. GMT
 *                                  standard the rest.
 * Feb 16, 2017  5899     rjpeter   Removed excessive logging.
 * Apr 05, 2017  1045     tjensen   Add Coverage generics for DataSetMetaData
 * May 26, 2017  6186     rjpeter   Remove BandwidthDataSetUpdate
 * Aug 02, 2017  6186     rjpeter   Moved cycle and dataset logic to datasetMetaData.
 *
 * </pre>
 *
 */
public class BandwidthUtil {

    public static final long BYTES_PER_KILOBYTE = 1024;

    public static final long DEFAULT_IDENTIFIER = -1L;

    protected static final int[] MONTHS_OF_YEAR = { Calendar.JANUARY,
            Calendar.FEBRUARY, Calendar.MARCH, Calendar.APRIL, Calendar.MAY,
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER,
            Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER };

    /** BWM XML pattern **/
    private static final Pattern RES_PATTERN = Pattern.compile("^res");

    // Create an 'instance' Object so that implementations of some
    // algorithms can be injected with Spring..
    private static final BandwidthUtil instance = new BandwidthUtil();

    private ISubscriptionLatencyCalculator subscriptionLatencyCalculator;

    private ISubscriptionRescheduleStrategy subscriptionRescheduleStrategy;

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(BandwidthUtil.class);

    private BandwidthUtil() {
    };

    public static int getSubscriptionLatency(Subscription<?, ?> subscription) {
        return instance.subscriptionLatencyCalculator.getLatency(subscription);
    }

    /**
     * Seconds and milliseconds on a Calendar are not used in bandwidth
     * management and can alter some of the time arithmetic that is used
     * throughout the code. Zero out the seconds and milliseconds values as a
     * convenience
     *
     * @return
     */
    public static Calendar now() {
        Calendar now = TimeUtil.newGmtCalendar();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        return now;
    }

    /**
     * @param subscriptionLatencyCalculator
     *            the subscriptionLatencyCalculator to set
     */
    public void setSubscriptionLatencyCalculator(
            ISubscriptionLatencyCalculator subscriptionLatencyCalculator) {
        this.subscriptionLatencyCalculator = subscriptionLatencyCalculator;
    }

    /**
     * @param subscriptionRescheduleStrategy
     *            the subscriptionRescheduleStrategy to set
     */
    public void setSubscriptionRescheduleStrategy(
            ISubscriptionRescheduleStrategy subscriptionRescheduleStrategy) {
        this.subscriptionRescheduleStrategy = subscriptionRescheduleStrategy;
    }

    /**
     * @return the instance
     */
    public static BandwidthUtil getInstance() {
        return instance;
    }

    /**
     * Format a Calendar Object into a standard String format.
     *
     * @param calendar
     *
     * @return The standard String format of the provided Calendar.
     */
    public static String format(Calendar calendar) {
        return String.format("%1$tY%1$tm%1$td %1$tH:%1$tM:%1$tS", calendar);
    }

    public static String format(Date date) {
        return String.format("%1$tY%1$tm%1$td %1$tH:%1$tM:%1$tS", date);
    }

    public static int minuteOfDay(Calendar calendar) {
        return calendar.get(Calendar.HOUR_OF_DAY) * 60
                + calendar.get(Calendar.MINUTE);
    }

    /**
     * Create a new {@link BandwidthSubscription} Object based on the
     * {@link Subscription} and {@link Calendar} Objects provided.
     *
     * @param subscription
     *            the subscription
     * @param baseReferenceTime
     *            the base reference time
     * @return the {@link BandwidthSubscription}
     * @throws SerializationException
     *             on error serializing the subscription
     */
    public static BandwidthSubscription getSubscriptionDaoForSubscription(
            Subscription<?, ?> subscription, Date baseReferenceTime) {
        BandwidthSubscription dao = new BandwidthSubscription();

        dao.setDataSetName(subscription.getDataSetName());
        dao.setProvider(subscription.getProvider());
        dao.setOwner(subscription.getOwner());
        dao.setName(subscription.getName());
        dao.setEstimatedSize(subscription.getDataSetSize());
        dao.setRoute(subscription.getRoute());
        dao.setBaseReferenceTime(baseReferenceTime);
        dao.setCycle(TimeUtil.newGmtCalendar(baseReferenceTime)
                .get(Calendar.HOUR_OF_DAY));
        dao.setPriority(subscription.getPriority());
        dao.setRegistryId(subscription.getId());
        return dao;
    }

    /**
     * Convert the number of kilobytes per second to bytes for the number of
     * specified minutes.
     *
     * @param kilobytesPerSecond
     *            the kilobytes per second
     * @param numberOfMinutes
     *            the number of minutes
     * @return the bytes per specified number of minutes
     */
    public static long convertKilobytesPerSecondToBytesPerSpecifiedMinutes(
            int kilobytesPerSecond, int numberOfMinutes) {
        return kilobytesPerSecond * BandwidthUtil.BYTES_PER_KILOBYTE
                * numberOfMinutes * TimeUtil.SECONDS_PER_MINUTE;
    }

    /**
     * Convert bytes to kilobytes.
     *
     * @param bytes
     *            the bytes
     * @return the kilobytes
     */
    public static long convertBytesToKilobytes(long bytes) {
        return bytes / BandwidthUtil.BYTES_PER_KILOBYTE;
    }

    /**
     * Check whether a subscription should be rescheduled on an update.
     *
     * @param subscription
     *            the subscription
     * @param old
     *            the old version
     * @return true if the subscription should be rescheduled
     */
    public static boolean subscriptionRequiresReschedule(
            Subscription<?, ?> subscription, Subscription<?, ?> old) {
        return instance.subscriptionRescheduleStrategy
                .subscriptionRequiresReschedule(subscription, old);
    }

    /**
     * Sets up the activePeriod Start/End to plan Start/End calendar
     */
    public static Calendar planToPeriodCompareCalendar(Calendar planCalendar,
            Calendar activePeriod) {

        Calendar cal = TimeUtil.newCalendar(planCalendar);
        cal.set(Calendar.MONTH, activePeriod.get(Calendar.MONTH));
        cal.set(Calendar.DAY_OF_MONTH, activePeriod.get(Calendar.DAY_OF_MONTH));

        return cal;
    }

    /**
     * Get the list of mode configured spring file names for the named mode.
     *
     * @param modeName
     *            retrieve the spring files configured for this mode
     * @return list of spring files configured for the given mode
     */
    public static String[] getSpringFileNamesForMode(String modeName) {
        List<String> fileList = new ArrayList<>();
        try {
            EDEXModesUtil.extractSpringXmlFiles(fileList, modeName);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to determine spring files for mode " + modeName, e);
        }

        String[] result = new String[fileList.size()];
        int i = 0;
        for (String fileName : fileList) {
            String name = RES_PATTERN.matcher(fileName).replaceFirst("");
            name = JarUtil.getResResourcePath(name);
            result[i] = name;
            i++;
        }

        return result;
    }

    /**
     * Convert List of subscriptions to a Map. This is used in request handler
     * portion of the BWM.
     *
     * @param List
     *            <Subscription> list
     * @return Map<String, Subscription>
     */

    public static <T extends Time, C extends Coverage> Map<String, Subscription<T, C>> getMapFromRequestList(
            List<Subscription<T, C>> list) {

        Map<String, Subscription<T, C>> requestMap = null;

        if (list != null) {
            requestMap = new HashMap<>(list.size());

            for (Subscription<T, C> sub : list) {
                requestMap.put(sub.getId(), sub);
            }
        }

        return requestMap;
    }
}