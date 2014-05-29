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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.Sets;
import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.PointTime;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthDataSetUpdate;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthSubscription;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalPlan;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalStatus;

/**
 * Utility class that maintains state between in-memory objects and database
 * versions.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 24, 2012 1286       djohnson     Extract methods from {@link BandwidthUtil}.
 * Dec 11, 2012 1286       djohnson     FULFILLED allocations are not in the retrieval plan either.
 * Feb 14, 2013 1595       djohnson     Fix not using calendar copies, and backwards max/min operations.
 * Jun 03, 2013 2038       djohnson     Add ability to schedule down to minute granularity.
 * Jun 04, 2013  223       mpduff       Refactor changes.
 * Sept 10, 2013 2351      dhladky      Made adhoc queries for pointdata work
 * Sept 17, 2013 2383      bgonzale     setAdhocMostRecentUrlAndTime returns null if grid and
 *                                      no metadata found.
 * Sept 24, 2013 1797      dhladky      separated time from GriddedTime
 * Oct 10, 2013 1797       bgonzale     Refactored registry Time objects.
 * Oct 30, 2013  2448      dhladky      Fixed pulling data before and after activePeriod starting and ending.
 * Nov 5, 2013  2521       dhladky      Fixed DataSetMetaData update failures for URL's in pointdata.
 * Nov 12, 2013 2448       dhladky      Fixed stop/start subscription scheduling problem.
 * Nov 20, 2013 2448       bgonzale     Fix for subscription start time set to first cycle time.
 *                                      Fix for subscription end time set to end of day.
 * Dec 02, 2013 2545       mpduff       Fix for delay starting retrievals, execute adhoc upon subscribing.
 * Dec 20, 2013 2636       mpduff       Fix dataset offset.
 * Jan 08, 2014 2615       bgonzale     Refactored getRetrievalTimes into RecurringSubscription
 *                                      calculateStart and calculateEnd methods.
 * Jan 24, 2014 2636       mpduff       Refactored retrieval time generation.
 * Jan 24, 2013 2709       bgonzale     Added inActivePeriodWindow check during retrieval time calculations
 *                                      because the calculate start and end time methods no longer use
 *                                      active period.
 * Jan 29, 2014 2636       mpduff       Scheduling refactor.
 * Feb 11, 2014 2636       mpduff       Change how retrieval times are calculated.
 * Apr 15, 2014 3012       dhladky      Fixed improper offsets.
 * Apr 21, 2014 2887       dhladky      Missed start/end in previous call, needs shouldScheduleForTime();
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class BandwidthDaoUtil<T extends Time, C extends Coverage> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(BandwidthDaoUtil.class);

    private final IBandwidthDao<T, C> bandwidthDao;

    private final RetrievalManager retrievalManager;

    /**
     * Constructor.
     * 
     * @param bandwidthDao
     *            the bandwidth dao
     * @param retrievalManager
     *            the retrieval manager
     */
    public BandwidthDaoUtil(IBandwidthDao<T, C> bandwidthDao,
            RetrievalManager retrievalManager) {
        this.bandwidthDao = bandwidthDao;
        this.retrievalManager = retrievalManager;
    }

    /**
     * Calculate all the retrieval times for a subscription that should be in
     * the current retrieval plan for the specified subscription.
     * 
     * @param subscription
     * @param cycles
     * @return
     */
    public SortedSet<Calendar> getRetrievalTimes(
            Subscription<T, C> subscription, SortedSet<Integer> cycles) {
        return getRetrievalTimes(subscription, cycles,
                Sets.newTreeSet(Arrays.asList(0)));
    }

    /**
     * Calculate all the retrieval times for a subscription that should be in
     * the current retrieval plan for the specified subscription.
     * 
     * @param subscription
     * @param retrievalInterval
     *            the retrieval interval
     * @return the retrieval times
     */
    public SortedSet<Calendar> getRetrievalTimes(
            Subscription<T, C> subscription, int retrievalInterval) {
        // Add all hours of the days
        final SortedSet<Integer> hours = Sets.newTreeSet();
        for (int i = 0; i < TimeUtil.HOURS_PER_DAY; i++) {
            hours.add(i);
        }

        // Add every minute of the hour that is a multiple of the retrieval
        // interval
        final SortedSet<Integer> minutes = Sets.newTreeSet();
        for (int i = 0; i < TimeUtil.MINUTES_PER_HOUR; i += retrievalInterval) {
            minutes.add(i);
        }

        return getRetrievalTimes(subscription, hours, minutes);
    }

    /**
     * Calculate all the retrieval times for a subscription that should be in
     * the current retrieval plan for the specified subscription.
     * 
     * @param subscription
     *            The subscription
     * @param hours
     *            The set of hours
     * @param minutes
     *            The set of minutes
     * @return Set of retrieval times
     */
    private SortedSet<Calendar> getRetrievalTimes(
            Subscription<T, C> subscription, SortedSet<Integer> hours,
            SortedSet<Integer> minutes) {
        SortedSet<Calendar> subscriptionTimes = new TreeSet<Calendar>();

        RetrievalPlan plan = retrievalManager.getPlan(subscription.getRoute());
        Calendar planStart = plan.getPlanStart();
        Calendar planEnd = plan.getPlanEnd();

        // starting time when when subscription is first valid for scheduling
        // based on plan start and subscription start.
        Calendar subscriptionCalculatedStart = subscription
                .calculateStart(planStart);

        // end time when when subscription is last valid for scheduling based on
        // plan end and subscription end.
        Calendar subscriptionCalculatedEnd = subscription.calculateEnd(planEnd);
        if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
            statusHandler.debug("**** PlanStart: " + planStart.getTime());
            statusHandler.debug("**** PlanEnd  : " + planEnd.getTime());
            statusHandler.debug("**** CalculatedStart: "
                    + subscriptionCalculatedStart.getTime());
            statusHandler.debug("**** CalculatedEnd  : "
                    + subscriptionCalculatedEnd.getTime());
        }

        subscriptionCalculatedStart = TimeUtil.minCalendarFields(
                subscriptionCalculatedStart, Calendar.MINUTE, Calendar.SECOND,
                Calendar.MILLISECOND);

        // drop the start time by 6 hours to account for 4 cycle/day models
        subscriptionCalculatedStart.add(Calendar.HOUR_OF_DAY, -6);
        Calendar start = TimeUtil.newGmtCalendar(subscriptionCalculatedStart
                .getTime());

        while (!start.after(subscriptionCalculatedEnd)) {
            for (Integer cycle : hours) {
                start.set(Calendar.HOUR_OF_DAY, cycle);

                // calculate the offset, every hour
                int availabilityOffset = 0;
                try {
                    availabilityOffset = BandwidthUtil
                            .getDataSetAvailablityOffset(subscription, start);
                } catch (RegistryHandlerException e) {
                    // Error occurred querying the registry. Log and continue on
                    statusHandler
                            .handle(Priority.PROBLEM,
                                    "Unable to retrieve data availability offset, using 0 for the offset.",
                                    e);
                }

                for (Integer minute : minutes) {

                    start.set(Calendar.MINUTE, minute);
                    Calendar baseRefTime = TimeUtil.newGmtCalendar();
                    baseRefTime.setTimeInMillis(start.getTimeInMillis());

                    // add the offset and check if it falls within window
                    Calendar offsetBaseRefTime = TimeUtil
                            .newGmtCalendar(baseRefTime.getTime());
                    offsetBaseRefTime.add(Calendar.MINUTE, availabilityOffset);

                    if (offsetBaseRefTime.after(planStart)
                            && offsetBaseRefTime.before(planEnd)) {
                        /*
                         * Fine grain check by hour and minute, for
                         * subscription(start/end), activePeriod(start/end)
                         */

                        if (!subscription.shouldScheduleForTime(baseRefTime)) {
                            // don't schedule this retrieval time,
                            // outside subscription window
                            continue;
                        }
                        subscriptionTimes.add(baseRefTime);
                    }
                }
            }

            // Start the next day..
            start.add(Calendar.DAY_OF_YEAR, 1);
            start.set(Calendar.HOUR_OF_DAY, hours.first());
        }

        return subscriptionTimes;
    }

    public void remove(BandwidthSubscription dao) {

        List<BandwidthAllocation> reservations = bandwidthDao
                .getBandwidthAllocations(dao.getIdentifier());

        // Unschedule all the BandwidthReservations for the subscription..
        for (BandwidthAllocation reservation : reservations) {
            final RetrievalStatus reservationStatus = reservation.getStatus();
            // Allocations with these statuses are not actively managed by
            // the retrieval manager
            if (RetrievalStatus.UNSCHEDULED == reservationStatus
                    || RetrievalStatus.FULFILLED == reservationStatus) {
                continue;
            }
            retrievalManager.remove(reservation);
        }

        bandwidthDao.remove(dao);
    }

    /**
     * Handle updating the database and propagate the update of any status
     * changes to the RetrievalManager.
     * 
     * @param allocation
     */
    public void update(BandwidthAllocation allocation) {

        bandwidthDao.createOrUpdate(allocation);
        retrievalManager.updateBandwidthAllocation(allocation);
    }

    /**
     * Add the url and start time for the most recent dataset metadata update
     * that has arrived, which matches the requested cycle of the
     * {@link AdhocSubscription}.
     * 
     * @param adhoc
     *            the adhoc subscription, with its {@link Time} object's cycles
     *            populated
     * @param mostRecent
     *            if true, then any base reference time will match
     * @return the adhoc subscription, or null if no matching metadata could be
     *         found
     */
    @SuppressWarnings("rawtypes")
    public AdhocSubscription<T, C> setAdhocMostRecentUrlAndTime(
            AdhocSubscription<T, C> adhoc, boolean mostRecent) {
        AdhocSubscription<T, C> retVal = null;

        if (adhoc.getDataSetType() == DataType.POINT) {

            List<DataSetMetaData> dataSetMetaDatas = null;
            try {
                dataSetMetaDatas = DataDeliveryHandlers
                        .getDataSetMetaDataHandler().getByDataSet(
                                adhoc.getDataSetName(), adhoc.getProvider());
            } catch (RegistryHandlerException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "No DataSetMetaData matching query! DataSetName: "
                                + adhoc.getDataSetName() + " Provider: "
                                + adhoc.getProvider(), e);
            }

            if (dataSetMetaDatas != null && !dataSetMetaDatas.isEmpty()) {
                // No guarantee on ordering, have to find most recent time
                @SuppressWarnings("unchecked")
                DataSetMetaData<PointTime> selectedDataSet = dataSetMetaDatas
                        .get(0);
                Date checkDate = selectedDataSet.getDate();

                for (DataSetMetaData<PointTime> dsmd : dataSetMetaDatas) {
                    if (dsmd.getDate().after(checkDate)) {
                        checkDate = dsmd.getDate();
                        selectedDataSet = dsmd;
                    }
                }

                adhoc.setUrl(selectedDataSet.getUrl());
                retVal = adhoc;
            }

        } else if (adhoc.getDataSetType() == DataType.GRID) {

            // if the start time is there, it already has it, skip
            if (adhoc.getTime().getStart() == null) {

                List<BandwidthDataSetUpdate> dataSetMetaDataUpdates = bandwidthDao
                        .getBandwidthDataSetUpdate(adhoc.getProvider(),
                                adhoc.getDataSetName());

                if (dataSetMetaDataUpdates != null
                        && !dataSetMetaDataUpdates.isEmpty()) {
                    // getDataSetMetaData returns the dataset meta-data in
                    // descending
                    // order of time, so walk the iterator finding the first
                    // subscribed
                    // to cycle
                    BandwidthDataSetUpdate daoToUse = null;
                    Time adhocTime = adhoc.getTime();
                    for (BandwidthDataSetUpdate current : dataSetMetaDataUpdates) {
                        if (mostRecent
                                || ((GriddedTime) adhocTime)
                                        .getCycleTimes()
                                        .contains(
                                                current.getDataSetBaseTime()
                                                        .get(Calendar.HOUR_OF_DAY))) {
                            daoToUse = current;
                            break;
                        }
                    }

                    if (daoToUse == null) {
                        if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                            statusHandler
                                    .debug(String
                                            .format("There wasn't applicable most recent dataset metadata to use for the adhoc subscription [%s].",
                                                    adhoc.getName()));
                        }
                    } else {
                        if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                            statusHandler
                                    .debug(String
                                            .format("Found most recent metadata for adhoc subscription [%s], using url [%s]",
                                                    adhoc.getName(),
                                                    daoToUse.getUrl()));
                        }

                        adhoc.setUrl(daoToUse.getUrl());
                        adhocTime.setStart(daoToUse.getDataSetBaseTime()
                                .getTime());

                        retVal = adhoc;
                    }
                }
            } else {
                retVal = adhoc;
            }
        } else {
            throw new IllegalArgumentException("DataType: "
                    + adhoc.getDataSetType()
                    + " Not yet implemented for adhoc subscriptions");
        }

        return retVal;
    }
}
