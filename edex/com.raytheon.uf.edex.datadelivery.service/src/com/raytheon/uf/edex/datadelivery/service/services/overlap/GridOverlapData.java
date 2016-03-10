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
package com.raytheon.uf.edex.datadelivery.service.services.overlap;

import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.service.subscription.GridSubscriptionOverlapConfig;
import com.raytheon.uf.common.datadelivery.service.subscription.SubscriptionOverlapConfig;
import com.raytheon.uf.common.datadelivery.service.subscription.SubscriptionOverlapMatchStrategy;

/**
 * Gridded overlap data object.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 17, 2013   2292     mpduff      Initial creation
 * Feb 13, 2014   2386     bgonzale    Change pass comparisons to >= instead of only >.
 *                                     Renamed sub1 and sub2 to otherSub and sub to make
 *                                     it easier to see what is compared against, and 
 *                                     changed the spatial check to check the intersection
 *                                     coverage of sub, not otherSub.
 * Nov 12, 2015   4622    dhladky      Added Level checks for Grids.
 * Mar 09, 2016   4644    tjensen      Fix comparison of overlapping levels
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 * @param <T>
 */

public class GridOverlapData<T extends GriddedTime, C extends GriddedCoverage>
        extends OverlapData<GriddedTime, GriddedCoverage> {
    /** Number of grid attributes */
    private final int numberOfGridAttributes = 2;

    /** Forecast hour duplication percent */
    private int fcstHrDuplication = -999;

    /** Cycle time duplication percent */
    private int cycleDuplication = -999;

    /** level duplication percent */
    private int levelDuplication = -999;

    /** Forecast hour pass flag */
    protected boolean fcstHrPass = false;

    /** Cycle time pass flag */
    protected boolean cyclePass = false;

    /** Level pass flag */
    protected boolean levelPass = false;

    /**
     * Constructor.
     * 
     * @param sub
     * @param otherSub
     * @param config
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public GridOverlapData(Subscription sub, Subscription otherSub,
            SubscriptionOverlapConfig config) {
        super(sub, otherSub, config);
    }

    /**
     * Calculates the percent, 0-100, of how many cycle hours from sub are
     * satisfied by otherSub.
     * 
     * @param sub
     * @param otherSub
     */
    public void calculateCycleDuplicationPercent(
            Subscription<GriddedTime, GriddedCoverage> sub,
            Subscription<GriddedTime, GriddedCoverage> otherSub) {

        GriddedTime gtimeOther = otherSub.getTime();
        GriddedTime gtime = sub.getTime();

        cycleDuplication = this.getDuplicationPercent(
                gtimeOther.getCycleTimes(), gtime.getCycleTimes());
    }

    /**
     * Calculates the percent, 0-100, of how many forecast hours from sub are
     * satisfied by otherSub.
     * 
     * @param sub
     * @param otherSub
     */
    public void calculateForecastHourDuplicationPercent(
            Subscription<GriddedTime, GriddedCoverage> sub,
            Subscription<GriddedTime, GriddedCoverage> otherSub) {
        GriddedTime gtimeOther = otherSub.getTime();
        GriddedTime gtime = sub.getTime();

        fcstHrDuplication = getDuplicationPercent(
                gtimeOther.getSelectedTimeIndices(),
                gtime.getSelectedTimeIndices());
    }

    /**
     * Calculates the percent, 0-100, of how many levels from sub are the same
     * as otherSub by parameter match.
     * 
     * @param sub
     * @param otherSub
     */

    protected void calculateLevelOverlapPercent(
            Subscription<GriddedTime, GriddedCoverage> sub,
            Subscription<GriddedTime, GriddedCoverage> otherSub) {

        int highestPercentOverlap = 0;
        // Calculate for each parameter in list that is common to each
        for (Parameter parm : sub.getParameter()) {
            for (Parameter otherParm : otherSub.getParameter()) {
                if (parm.getName().equals(otherParm.getName())
                        && parm.getLevels().getSelectedLevelIndices() != null
                        && otherParm.getLevels().getSelectedLevelIndices() != null) {
                    /**
                     * Find the highest overlap percent between any matching
                     * parameters with levels common between the two compared
                     * subscriptions.
                     */
                    int localPercentOverlap = getDuplicationPercent(otherParm
                            .getLevels().getSelectedLevelIndices(), parm
                            .getLevels().getSelectedLevelIndices());

                    if (localPercentOverlap > highestPercentOverlap) {
                        highestPercentOverlap = localPercentOverlap;
                    }
                }
            }
        }

        levelDuplication = highestPercentOverlap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void determineOverlapping() {
        super.determineOverlapping();
        calculateCycleDuplicationPercent(sub, otherSub);
        calculateForecastHourDuplicationPercent(sub, otherSub);
        calculateLevelOverlapPercent(sub, otherSub);

        GridSubscriptionOverlapConfig config = (GridSubscriptionOverlapConfig) this.config;

        fcstHrPass = fcstHrDuplication >= config
                .getMaxAllowedForecastHourDuplication();

        cyclePass = cycleDuplication >= config.getMaxAllowedCycleDuplication();

        levelPass = levelDuplication >= config.getMaxAllowedLevelDuplication();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOverlapping() {
        determineOverlapping();
        boolean response = false;

        if (matchStrategy == SubscriptionOverlapMatchStrategy.MATCH_ALL) {
            response = cyclePass && fcstHrPass && parameterPass && levelPass
                    && spatialPass;
        } else if (matchStrategy == SubscriptionOverlapMatchStrategy.MATCH_ANY) {
            response = cyclePass || fcstHrPass || parameterPass || levelPass
                    || spatialPass;
        } else if (matchStrategy == SubscriptionOverlapMatchStrategy.AT_LEAST_HALF) {
            int halfNumAttrs = (numberOfGridAttributes + numberOfCommonAttributes) / 2;
            List<Boolean> toCheck = new ArrayList<Boolean>();
            toCheck.add(cyclePass);
            toCheck.add(fcstHrPass);
            toCheck.add(parameterPass);
            toCheck.add(levelPass);
            toCheck.add(spatialPass);

            int exceeded = 0;
            for (boolean check : toCheck) {
                if (check) {
                    exceeded++;
                    if (exceeded >= halfNumAttrs) {
                        response = true;
                        break;
                    }
                }
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDuplicate() {
        determineOverlapping();
        return cycleDuplication == ONE_HUNDRED_PERCENT
                && fcstHrDuplication == ONE_HUNDRED_PERCENT
                && parameterDuplication == ONE_HUNDRED_PERCENT
                && levelDuplication == ONE_HUNDRED_PERCENT
                && spatialDuplication == ONE_HUNDRED_PERCENT;
    }
}
