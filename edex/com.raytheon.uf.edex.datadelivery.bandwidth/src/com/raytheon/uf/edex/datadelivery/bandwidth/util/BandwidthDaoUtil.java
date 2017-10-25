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

import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthAllocation;
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
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 24, 2012  1286     djohnson  Extract methods from {@link BandwidthUtil}.
 * Dec 11, 2012  1286     djohnson  FULFILLED allocations are not in the
 *                                  retrieval plan either.
 * Feb 14, 2013  1595     djohnson  Fix not using calendar copies, and backwards
 *                                  max/min operations.
 * Jun 03, 2013  2038     djohnson  Add ability to schedule down to minute
 *                                  granularity.
 * Jun 04, 2013  223      mpduff    Refactor changes.
 * Sep 10, 2013  2351     dhladky   Made adhoc queries for pointdata work
 * Sep 17, 2013  2383     bgonzale  setAdhocMostRecentUrlAndTime returns null if
 *                                  grid and no metadata found.
 * Sep 24, 2013  1797     dhladky   separated time from GriddedTime
 * Oct 10, 2013  1797     bgonzale  Refactored registry Time objects.
 * Oct 30, 2013  2448     dhladky   Fixed pulling data before and after
 *                                  activePeriod starting and ending.
 * Nov 05, 2013  2521     dhladky   Fixed DataSetMetaData update failures for
 *                                  URL's in pointdata.
 * Nov 12, 2013  2448     dhladky   Fixed stop/start subscription scheduling
 *                                  problem.
 * Nov 20, 2013  2448     bgonzale  Fix for subscription start time set to first
 *                                  cycle time. Fix for subscription end time
 *                                  set to end of day.
 * Dec 02, 2013  2545     mpduff    Fix for delay starting retrievals, execute
 *                                  adhoc upon subscribing.
 * Dec 20, 2013  2636     mpduff    Fix dataset offset.
 * Jan 08, 2014  2615     bgonzale  Refactored getRetrievalTimes into
 *                                  RecurringSubscription calculateStart and
 *                                  calculateEnd methods.
 * Jan 24, 2014  2636     mpduff    Refactored retrieval time generation.
 * Jan 24, 2013  2709     bgonzale  Added inActivePeriodWindow check during
 *                                  retrieval time calculations because the
 *                                  calculate start and end time methods no
 *                                  longer use active period.
 * Jan 29, 2014  2636     mpduff    Scheduling refactor.
 * Feb 11, 2014  2636     mpduff    Change how retrieval times are calculated.
 * Apr 15, 2014  3012     dhladky   Fixed improper offsets.
 * Apr 21, 2014  2887     dhladky   Missed start/end in previous call, needs
 *                                  shouldScheduleForTime();
 * Jun 09, 2014  3113     mpduff    Moved getRetrievalTimes to Subscription.
 * Jul 28, 2014  2752     dhladky   Greatly streamlined scheduling.
 * Sep 14, 2014  2131     dhladky   PDA additions
 * Feb 19, 2015  3998     dhladky   Simplified the adhoc subscription process.
 * Apr 13, 2016  5573     dhladky   Recurring subscriptions fail do to null time
 *                                  object.
 * Apr 20, 2017  1045     tjensen   Update for moving datasets
 * Aug 02, 2017  6186     rjpeter   Removed setAdhocMostRecentUrlAndTime.
 * Oct 25, 2017  6484     tjensen   Merged SubscriptionRetrievals and
 *                                  BandwidthAllocations
 *
 * </pre>
 *
 * @author djohnson
 */

public class BandwidthDaoUtil<T extends Time, C extends Coverage> {

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

    public void remove(List<BandwidthAllocation> bandwidthAllocations) {

        for (BandwidthAllocation reservation : bandwidthAllocations) {
            final RetrievalStatus reservationStatus = reservation.getStatus();
            /*
             * Allocations with these statuses are not actively managed by the
             * retrieval manager
             */
            if (RetrievalStatus.UNSCHEDULED != reservationStatus
                    && RetrievalStatus.FULFILLED != reservationStatus) {
                retrievalManager.remove(reservation);
            }
        }
        bandwidthDao.remove(bandwidthAllocations);
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

    public RetrievalPlan getRetrievalPlan(Network network) {
        return this.retrievalManager.getRetrievalPlans().get(network);
    }
}