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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Maps;
import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthMap;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionBuilder;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.PathManagerFactoryTest;
import com.raytheon.uf.common.registry.handler.RegistryObjectHandlersUtil;
import com.raytheon.uf.common.time.SimulatedTime;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.time.util.DataDeliveryTimeUtilTest;
import com.raytheon.uf.edex.datadelivery.bandwidth.InMemoryBandwidthBucketDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.InMemoryBandwidthBucketAllocationAssociator;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalPlan;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;

/**
 * Test {@link BandwidthDaoUtil}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 24, 2012 1286       djohnson     Initial creation
 * Feb 07, 2013 1543       djohnson     Remove unnecessary test setup methods.
 * Feb 14, 2013 1595       djohnson     Fix retrieval plan/subscription time intersections.
 * Jun 05, 2013 2038       djohnson     Use public API for getting retrieval times.
 * Jun 25, 2013 2106       djohnson     RetrievalPlan uses setters instead of constructor injection now.
 * Sep 25, 2013 1797       dhladky      separated time and gridded time
 * Jan 07, 2014  2636      mpduff       Removed dataset availability offset calculator (not used).
 * Jan 08, 2014 2615       bgonzale     Updated test.
 * Jan 26, 2014 2709       bgonzale     Added tests for year scheduling boundary.
 * Aug 26, 2014 3365       ccody        Separate Data Delivery tests out of AWIPS 2 baseline.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class BandwidthDaoUtilTest {

    private final IBandwidthDao mockDao = mock(IBandwidthDao.class);

    private RetrievalManager retrievalManager;

    private BandwidthDaoUtil bandwidthDaoUtil;

    private BandwidthMap map;

    private RetrievalPlan plan;

    @Before
    public void setUp() {
        DataDeliveryTimeUtilTest.freezeTime(TimeUtil.MILLIS_PER_DAY * 2);

        RegistryObjectHandlersUtil.init();
        PathManagerFactoryTest.initLocalization();

        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext lc = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.BASE);

        LocalizationFile lf = pm.getLocalizationFile(lc,
                "datadelivery/bandwidthmap.xml");

        map = BandwidthMap.load(lf.getFile());
        final InMemoryBandwidthBucketDao bucketsDao = new InMemoryBandwidthBucketDao();
        plan = new RetrievalPlan();
        plan.setNetwork(Network.OPSNET);
        plan.setMap(map);
        plan.setBandwidthDao(mockDao);
        plan.setBucketsDao(bucketsDao);
        plan.setAssociator(new InMemoryBandwidthBucketAllocationAssociator(
                mockDao, bucketsDao));

        Map<Network, RetrievalPlan> retrievalPlans = Maps
                .newEnumMap(Network.class);
        retrievalPlans.put(Network.OPSNET, plan);
        retrievalManager = Mockito.spy(new RetrievalManager(mockDao,
                new Object()));
        bandwidthDaoUtil = new BandwidthDaoUtil(mockDao, retrievalManager);
        when(retrievalManager.getRetrievalPlans()).thenReturn(retrievalPlans);

        // Just used to initialize the retrieval plans that are used on the mock
        // retrieval manager
        final RetrievalManager tmpRetrievalManager = new RetrievalManager(
                mockDao, new Object());
        tmpRetrievalManager.setRetrievalPlans(retrievalPlans);
        tmpRetrievalManager.initRetrievalPlans();
    }

    @After
    public void tearDown() {
        DataDeliveryTimeUtilTest.resumeTime();
    }

    private Calendar createCal(int year, int dayOfYear) {
        Calendar cal = TimeUtil.newCalendar();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
        return cal;
    }

    /*
     * 
     */
    private Subscription createOverYearBoundary(int planStart,
            int planStartYear, int subStart, int subStartYear, int subEnd,
            int subEndYear, int activeStart, int activeStartYear,
            int activeEnd, int activeEndYear, int planDays) {
        Calendar planStartDay = createCal(planStartYear, planStart);
        Calendar subStartDay = createCal(subStartYear, subStart);
        Calendar subEndDay = createCal(subEndYear, subEnd);
        Calendar activeStartDay = createCal(activeStartYear, activeStart);
        Calendar activeEndDay = createCal(activeEndYear, activeEnd);

        // setup plan with specific start time for this test.
        SimulatedTime.getSystemTime().setTime(planStartDay.getTimeInMillis());
        map.getRoute(Network.OPSNET).setPlanDays(planDays);
        retrievalManager.getPlan(Network.OPSNET).setMap(map);
        // re-init plans with new simulated time
        retrievalManager.initRetrievalPlans();
        return new SubscriptionBuilder()
                .withSubscriptionStart(subStartDay.getTime())
                .withActivePeriodStart(activeStartDay.getTime())
                .withActivePeriodEnd(activeEndDay.getTime())
                .withSubscriptionEnd(subEndDay.getTime()).build();
    }

    @Test
    public void testActivePeriodOverYearBoundary() {
        // int y1970 = 1970;
        // int y1971 = 1971;
        // int planStart = 364;
        // int subStart = 364;
        // int activeStart = 365;
        // int activeEnd = 2;
        // int subEnd = 3;
        // int planDays = 5;
        // Subscription subscription = createOverYearBoundary(planStart, y1970,
        // subStart, y1970, subEnd, y1971, activeStart, y1970, activeEnd,
        // y1971, planDays);
        // ((GriddedTime) subscription.getTime()).setCycleTimes(Arrays.asList(
        // Integer.valueOf(9), Integer.valueOf(0)));
        //
        // TreeSet<Integer> cycles = new TreeSet<Integer>(
        // ((GriddedTime) subscription.getTime()).getCycleTimes());
        //
        // SortedSet<Calendar> subscriptionTimes = bandwidthDaoUtil
        // .getRetrievalTimes(subscription, cycles);
        //
        // List<Calendar> calendarDaysOfTheYear =
        // createCalendarListForSpecifiedDaysOfTheYear(
        // Arrays.asList(365), y1970);
        // calendarDaysOfTheYear
        // .addAll(createCalendarListForSpecifiedDaysOfTheYear(
        // Arrays.asList(01), y1971));
        //
        // verifySubscriptionTimesContainsCyclesForSpecifiedCalendarDays(
        // calendarDaysOfTheYear, cycles, subscriptionTimes);
    }

    @Test
    public void testActivePeriodJustBeforeYearBoundary() {
        int y1970 = 1970;
        int y1971 = 1971;
        int planStart = 362;
        int subStart = 362;
        int activeStart = 363;
        int activeEnd = 365;
        int subEnd = 2;
        int planDays = 5;
        // Subscription subscription = createOverYearBoundary(planStart, y1970,
        // subStart, y1970, subEnd, y1971, activeStart, y1970, activeEnd,
        // y1971, planDays);
        // ((GriddedTime) subscription.getTime()).setCycleTimes(Arrays.asList(
        // Integer.valueOf(9), Integer.valueOf(0)));
        //
        // TreeSet<Integer> cycles = new TreeSet<Integer>(
        // ((GriddedTime) subscription.getTime()).getCycleTimes());
        //
        // SortedSet<Calendar> subscriptionTimes = bandwidthDaoUtil
        // .getRetrievalTimes(subscription, cycles);
        //
        // List<Calendar> calendarDaysOfTheYear =
        // createCalendarListForSpecifiedDaysOfTheYear(
        // Arrays.asList(363, 364), y1970);
        //
        // verifySubscriptionTimesContainsCyclesForSpecifiedCalendarDays(
        // calendarDaysOfTheYear, cycles, subscriptionTimes);
    }

    @Test
    public void testActivePeriodJustAfterYearBoundary() {
        int y1970 = 1970;
        int y1971 = 1971;
        int planStart = 364;
        int subStart = 364;
        int activeStart = 1;
        int activeEnd = 3;
        int subEnd = 4;
        int planDays = 6;
        // Subscription subscription = createOverYearBoundary(planStart, y1970,
        // subStart, y1970, subEnd, y1971, activeStart, y1970, activeEnd,
        // y1971, planDays);
        // ((GriddedTime) subscription.getTime()).setCycleTimes(Arrays.asList(
        // Integer.valueOf(9), Integer.valueOf(0)));
        //
        // TreeSet<Integer> cycles = new TreeSet<Integer>(
        // ((GriddedTime) subscription.getTime()).getCycleTimes());
        //
        // SortedSet<Calendar> subscriptionTimes = bandwidthDaoUtil
        // .getRetrievalTimes(subscription, cycles);
        //
        // List<Calendar> calendarDaysOfTheYear =
        // createCalendarListForSpecifiedDaysOfTheYear(
        // Arrays.asList(1, 2), y1971);
        //
        // verifySubscriptionTimesContainsCyclesForSpecifiedCalendarDays(
        // calendarDaysOfTheYear, cycles, subscriptionTimes);
    }

    private List<Calendar> createCalendarListForSpecifiedDaysOfTheYear(
            Collection<Integer> daysOfTheYear, int year) {
        List<Calendar> calendarList = new ArrayList<Calendar>(
                daysOfTheYear.size());
        for (int dayOfTheYear : daysOfTheYear) {
            Calendar cal = TimeUtil.newCalendar();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.DAY_OF_YEAR, dayOfTheYear);
            calendarList.add(cal);
        }
        return calendarList;
    }

    @Test
    public void testGetRetrievalTimesReturnsBaseReferenceTimesInPlanWindow() {
        // Make sure the subscription is "active" within the plan period
        // This test is grid specific
        // Subscription subscription = new SubscriptionBuilder()
        // .withActivePeriodStart(plan.getPlanStart().getTime())
        // .withActivePeriodEnd(plan.getPlanEnd().getTime())
        // .withSubscriptionStart(TimeUtil.newImmutableDate()).build();
        // ((GriddedTime) subscription.getTime()).setCycleTimes(Arrays.asList(
        // Integer.valueOf(9), Integer.valueOf(0)));
        //
        // TreeSet<Integer> cycles = new TreeSet<Integer>(
        // ((GriddedTime) subscription.getTime()).getCycleTimes());
        //
        // SortedSet<Calendar> subscriptionTimes = bandwidthDaoUtil
        // .getRetrievalTimes(subscription, cycles);
        //
        // final List<Integer> daysOfTheYear = Arrays.asList(3);
        // verifySubscriptionTimesContainsCyclesForSpecifiedDays(daysOfTheYear,
        // cycles, subscriptionTimes);
    }

    @Test
    public void testGetRetrievalTimesUsesSubscriptionStartWhenItIsMostRestrictive() {
        final Date startsTwoDaysIntoPlan = new Date(plan.getPlanStart()
                .getTimeInMillis() + TimeUtil.MILLIS_PER_DAY * 2);

        Subscription subscription = new SubscriptionBuilder()
                .withSubscriptionStart(startsTwoDaysIntoPlan)
                .withSubscriptionEnd(plan.getPlanEnd().getTime()).build();
        ((GriddedTime) subscription.getTime()).setCycleTimes(Arrays.asList(
                Integer.valueOf(9), Integer.valueOf(0)));

        TreeSet<Integer> cycles = new TreeSet<Integer>(
                ((GriddedTime) subscription.getTime()).getCycleTimes());

        // SortedSet<Calendar> subscriptionTimes = bandwidthDaoUtil
        // .getRetrievalTimes(subscription, cycles);
        //
        // final List<Integer> daysOfTheYear = Collections.EMPTY_LIST;
        // verifySubscriptionTimesContainsCyclesForSpecifiedDays(daysOfTheYear,
        // cycles, subscriptionTimes);
        // final List<Integer> notScheduledDays = Arrays.asList(3);
        // verifySubscriptionTimesDoesNotContainCyclesForSpecifiedDays(
        // notScheduledDays, cycles, subscriptionTimes);
    }

    @Test
    public void testGetRetrievalTimesUsesSubscriptionEndWhenItIsMostRestrictive() {
        final Date endsOneDayBeforePlan = new Date(plan.getPlanEnd()
                .getTimeInMillis() - TimeUtil.MILLIS_PER_DAY);

        Subscription subscription = new SubscriptionBuilder()
                .withSubscriptionStart(plan.getPlanStart().getTime())
                .withSubscriptionEnd(endsOneDayBeforePlan).build();
        ((GriddedTime) subscription.getTime()).setCycleTimes(Arrays.asList(
                Integer.valueOf(9), Integer.valueOf(0)));

        TreeSet<Integer> cycles = new TreeSet<Integer>(
                ((GriddedTime) subscription.getTime()).getCycleTimes());

        // SortedSet<Calendar> subscriptionTimes = bandwidthDaoUtil
        // .getRetrievalTimes(subscription, cycles);
        //
        // final List<Integer> daysOfTheYear = Arrays.asList(3);
        // verifySubscriptionTimesContainsCyclesForSpecifiedDays(daysOfTheYear,
        // cycles, subscriptionTimes);
        // final List<Integer> notScheduledDays = Arrays.asList(4);
        // verifySubscriptionTimesDoesNotContainCyclesForSpecifiedDays(
        // notScheduledDays, cycles, subscriptionTimes);
    }

    @Test
    public void testRemoveDoesNotRemoveFromRetrievalPlanIfInUnscheduledState() {
        BandwidthSubscription subDao = new BandwidthSubscription();
        subDao.setId(10L);

        BandwidthAllocation alloc1 = new BandwidthAllocation();
        alloc1.setId(1);
        alloc1.setStatus(RetrievalStatus.SCHEDULED);
        BandwidthAllocation alloc2 = new BandwidthAllocation();
        alloc2.setId(2);
        alloc2.setStatus(RetrievalStatus.UNSCHEDULED);

        when(mockDao.getBandwidthAllocations(subDao.getIdentifier()))
                .thenReturn(Arrays.asList(alloc1, alloc2));

        bandwidthDaoUtil.remove(subDao);

        // Remove the scheduled one
        verify(retrievalManager).remove(alloc1);
        // Don't remove the unscheduled one
        verify(retrievalManager, never()).remove(alloc2);
    }

    @Test
    public void testGetRetrievalTimesReturnsEachIntervalMinuteOfEachHourInPlanWindow() {
        Subscription subscription = new SubscriptionBuilder()
                .withActivePeriodStart(plan.getPlanStart().getTime())
                .withActivePeriodEnd(plan.getPlanEnd().getTime())
                .withSubscriptionStart(TimeUtil.newImmutableDate()).build();

        // A 30 minute interval should provide 0 and 30 minutes of every hour
        // // Make sure the subscription is "active" within the plan period
        // final int interval = 30;
        // SortedSet<Calendar> subscriptionTimes = bandwidthDaoUtil
        // .getRetrievalTimes(subscription, interval);
        //
        // // Expected size is two per hour (0 and 30 minutes), for every hour,
        // // over the retrieval plan days (2), minus the hours for the last day
        // // because active period is exclusive of the last day (ending hour
        // // constraint for the last day is 00Z)
        // Calendar activeEnd = (Calendar) plan.getPlanEnd().clone();
        // activeEnd = TimeUtil.minCalendarFields(activeEnd,
        // Calendar.MILLISECOND,
        // Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY);
        // long subPeriodInHours = (activeEnd.getTimeInMillis() - plan
        // .getPlanStart().getTimeInMillis()) / TimeUtil.MILLIS_PER_HOUR;
        // final int expectedSize = (int) (subPeriodInHours * 2);
        // assertThat(subscriptionTimes, hasSize(expectedSize));
        //
        // // Make sure we have the expected number of 0 and 30 minute scheduled
        // // times
        // int numberOfZeroMinuteTimes = 0;
        // int numberOfThirtyMinuteTimes = 0;
        // for (Calendar subscriptionTime : subscriptionTimes) {
        // final int minuteField = subscriptionTime.get(Calendar.MINUTE);
        // if (minuteField == 0) {
        // numberOfZeroMinuteTimes++;
        // } else if (minuteField == 30) {
        // numberOfThirtyMinuteTimes++;
        // }
        // }
        //
        // final int halfTheTimes = subscriptionTimes.size() / 2;
        // assertThat(numberOfZeroMinuteTimes, is(equalTo(halfTheTimes)));
        // assertThat(numberOfThirtyMinuteTimes, is(equalTo(halfTheTimes)));

        // Would be nice to verify the days and hours, but the cycle based tests
        // already do that and the code was reused, maybe add it later
    }

    /**
     * Verifies the subscription times contains the cycles for the specified
     * days.
     * 
     * @param daysOfTheYear
     * @param cycles
     * @param subscriptionTimes
     */
    private void verifySubscriptionTimesContainsCyclesForSpecifiedDays(
            Collection<Integer> daysOfTheYear, Collection<Integer> cycles,
            SortedSet<Calendar> subscriptionTimes) {
        Calendar cal = TimeUtil.newCalendar();
        TimeUtil.minCalendarFields(cal, Calendar.MILLISECOND, Calendar.SECOND,
                Calendar.MINUTE);

        for (Integer dayOfTheYear : daysOfTheYear) {
            cal.set(Calendar.DAY_OF_YEAR, dayOfTheYear);

            for (Integer cycle : cycles) {
                cal.set(Calendar.HOUR_OF_DAY, cycle);
                assertTrue("Expected to find retrieval time of "
                        + BandwidthUtil.format(cal),
                        subscriptionTimes.contains(cal));
            }
        }
    }

    /**
     * Verifies the subscription times contains the cycles for the specified
     * days.
     * 
     * @param daysOfTheYear
     * @param cycles
     * @param subscriptionTimes
     */
    private void verifySubscriptionTimesContainsCyclesForSpecifiedCalendarDays(
            Collection<Calendar> daysOfTheYear, Collection<Integer> cycles,
            SortedSet<Calendar> subscriptionTimes) {
        boolean success = true;
        StringBuilder sb = new StringBuilder(
                "Expected to find retrieval times: ");
        int countOfTimes = 0;

        for (Calendar dayOfTheYear : daysOfTheYear) {
            for (Integer cycle : cycles) {
                Calendar cal = (Calendar) dayOfTheYear.clone();
                TimeUtil.minCalendarFields(cal, Calendar.MILLISECOND,
                        Calendar.SECOND, Calendar.MINUTE);
                cal.set(Calendar.HOUR_OF_DAY, cycle);
                success = success && subscriptionTimes.contains(cal);
                ++countOfTimes;
                sb.append(BandwidthUtil.format(cal)).append(
                        String.format(" %1$tZ  ", cal));
            }
        }
        sb.append("\nIn: ");
        for (Calendar subTime : subscriptionTimes) {
            sb.append(BandwidthUtil.format(subTime)).append(
                    String.format(" %1$tZ  ", subTime));
        }
        assertTrue(sb.toString(), success);
        assertTrue(sb.toString(), countOfTimes == subscriptionTimes.size());
    }

    /**
     * Verifies the subscription times do not contain the cycles for the
     * specified days.
     * 
     * @param daysOfTheYear
     * @param cycles
     * @param subscriptionTimes
     */
    private void verifySubscriptionTimesDoesNotContainCyclesForSpecifiedDays(
            Collection<Integer> daysOfTheYear, Collection<Integer> cycles,
            SortedSet<Calendar> subscriptionTimes) {
        Calendar cal = TimeUtil.newCalendar();
        TimeUtil.minCalendarFields(cal, Calendar.MILLISECOND, Calendar.SECOND,
                Calendar.MINUTE);

        for (Integer dayOfTheYear : daysOfTheYear) {
            cal.set(Calendar.DAY_OF_YEAR, dayOfTheYear);

            for (Integer cycle : cycles) {
                cal.set(Calendar.HOUR_OF_DAY, cycle);
                assertFalse("Expected not to find retrieval time of "
                        + BandwidthUtil.format(cal),
                        subscriptionTimes.contains(cal));
            }
        }

    }
}
