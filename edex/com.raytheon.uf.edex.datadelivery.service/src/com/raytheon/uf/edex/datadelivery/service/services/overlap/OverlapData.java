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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.LevelGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.service.subscription.SubscriptionOverlapConfig;
import com.raytheon.uf.common.datadelivery.service.subscription.SubscriptionOverlapMatchStrategy;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.CollectionUtil;

/**
 * Parent Overlap Data Object. This object holds the data needed to calculate
 * overlapping of subscriptions.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 17, 2013  2292     mpduff    Initial creation
 * Feb 13, 2014  2386     bgonzale  Change pass comparisons to >= instead of
 *                                  only >. Renamed sub1 and sub2 to otherSub
 *                                  and sub to make it easier to see what is
 *                                  compared against.
 * Sep 12, 2017  6413     tjensen   Updated to support ParameterGroups
 * Nov 15, 2017  6498     tjensen   Added null check for potential empty
 *                                  ParameterGroup levels
 *
 * </pre>
 *
 * @author mpduff
 * @param <T>
 * @param <C>
 */

public abstract class OverlapData<T extends Time, C extends Coverage> {
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(OverlapData.class);

    protected static final int ONE_HUNDRED_PERCENT = 100;

    private static final String UNABLE_TO_DETERMINE_SPATIAL_OVERLAP = "Unable to determine spatial overlap.  "
            + "Subscriptions will not be considered to be overlapping spatially.";

    /** The number of common attributes (in this class) */
    protected static final int numberOfCommonAttributes = 2;

    /** Spatial duplication percent */
    protected int spatialDuplication = -999;

    /** parameter duplication percent */
    protected int parameterDuplication = -999;

    /** Spatial pass flag */
    protected boolean spatialPass = false;

    /** Parameter pass flag */
    protected boolean parameterPass = false;

    /** The subscription match strategy */
    protected SubscriptionOverlapMatchStrategy matchStrategy;

    /** The subscription overlap config object */
    protected SubscriptionOverlapConfig config;

    /** Primary Subscription */
    protected Subscription<T, C> sub;

    /** Other Subscription to compare against */
    protected Subscription<T, C> otherSub;

    /**
     * Constructor.
     *
     * @param sub
     * @param otherSub
     * @param config
     */
    public OverlapData(Subscription<T, C> sub, Subscription<T, C> otherSub,
            SubscriptionOverlapConfig config) {
        this.otherSub = otherSub;
        this.sub = sub;
        this.config = config;
        this.matchStrategy = config.getMatchStrategy();
    }

    /**
     * Calculates the percent, 0-100, of how much spatial coverage from sub is
     * satisfied by otherSub.
     *
     * @param sub
     * @param otherSub
     */
    protected void calculateSpatialDuplicationPercent(Subscription<T, C> sub,
            Subscription<T, C> otherSub) {

        final Coverage otherSubCoverage = otherSub.getCoverage();
        final Coverage subCoverage = sub.getCoverage();

        if (otherSubCoverage != null && subCoverage != null) {
            final ReferencedEnvelope otherSubEnvelope = otherSubCoverage
                    .getRequestEnvelope();
            final ReferencedEnvelope subEnvelope = subCoverage
                    .getRequestEnvelope();

            if (otherSubEnvelope != null && subEnvelope != null) {
                // try {
                ReferencedEnvelope intersection;
                try {
                    intersection = MapUtil.reprojectAndIntersect(
                            otherSubEnvelope, subEnvelope);
                    final double intersectionArea = intersection.getArea();
                    spatialDuplication = (int) ((intersectionArea * 100)
                            / subEnvelope.getArea());
                } catch (TransformException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            UNABLE_TO_DETERMINE_SPATIAL_OVERLAP, e);
                }
            }
        }
    }

    /**
     * Calculates the percent, 0-100, of how many parameters from sub are
     * satisfied by otherSub.
     *
     * @param sub
     * @param otherSub
     */
    protected void calculateParameterDuplicationPercent(Subscription<T, C> sub,
            Subscription<T, C> otherSub) {
        int totalParameterLevels = 0;
        int overlapParameterLevels = 0;

        /*
         * Add all entries from the subscription to a set and record the total
         * size. Then remove all entries from the other sub. The difference in
         * size from the previous total is the overlap amount.
         */
        Map<String, ParameterGroup> subPgs = sub.getParameterGroups();
        Map<String, ParameterGroup> otherPgs = otherSub.getParameterGroups();
        Set<ParameterLevelEntry> subSet = new HashSet<>();

        for (ParameterGroup subPg : subPgs.values()) {
            for (LevelGroup slg : subPg.getGroupedLevels().values()) {
                subSet.addAll(slg.getLevels());
            }
        }
        totalParameterLevels = subSet.size();
        for (ParameterGroup otherPg : otherPgs.values()) {
            for (LevelGroup olg : otherPg.getGroupedLevels().values()) {
                List<ParameterLevelEntry> olevels = olg.getLevels();
                if (olevels != null && !olevels.isEmpty()) {
                    subSet.removeAll(olevels);
                }
            }
        }
        overlapParameterLevels = totalParameterLevels - subSet.size();

        parameterDuplication = (overlapParameterLevels * 100)
                / totalParameterLevels;
    }

    /**
     * Determine the overlap values
     */
    protected void determineOverlapping() {
        calculateParameterDuplicationPercent(sub, otherSub);
        calculateSpatialDuplicationPercent(sub, otherSub);
        this.parameterPass = this.parameterDuplication >= config
                .getMaxAllowedParameterDuplication();

        this.spatialPass = this.spatialDuplication >= config
                .getMaxAllowedSpatialDuplication();
    }

    /**
     * Calculate the duplication percent of the two collections.
     *
     * @param coll1
     * @param coll2
     * @return
     */
    protected <M> int getDuplicationPercent(Collection<M> coll1,
            Collection<M> coll2) {

        int numberSatisfiedByFirstCollection = 0;
        if (!CollectionUtil.isNullOrEmpty(coll1)
                && !CollectionUtil.isNullOrEmpty(coll2)) {
            for (M entry : coll2) {
                if (coll1.contains(entry)) {
                    numberSatisfiedByFirstCollection++;
                }
            }
            // Convert percent to 0-100
            return (numberSatisfiedByFirstCollection * 100) / (coll2.size());
        }

        return 0;
    }

    /**
     * Are the two subscriptions considered overlapping
     *
     * @return true if overlapping
     */
    public abstract boolean isOverlapping();

    /**
     * Are the two subscriptions duplicates
     *
     * @return true if duplicates
     */
    public abstract boolean isDuplicate();
}
