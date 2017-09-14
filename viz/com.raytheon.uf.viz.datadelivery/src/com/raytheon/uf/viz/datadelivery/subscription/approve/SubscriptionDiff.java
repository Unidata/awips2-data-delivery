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
package com.raytheon.uf.viz.datadelivery.subscription.approve;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.InitialPendingSubscription;
import com.raytheon.uf.common.datadelivery.registry.LevelGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.registry.ParameterUtils;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Find the differences between a Subscription Object and its
 * PendingSubscription Object.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 20, 2012           mpduff    Initial creation
 * Jul 25, 2012  955      djohnson  Use List instead of ArrayList.
 * Sep 24, 2012  1157     mpduff    Use InitialPendingSubsription.
 * Dec 10, 2012  1259     bsteffen  Switch Data Delivery from LatLon to
 *                                  referenced envelopes.
 * Jan 25, 2013  1528     djohnson  Compare priorities as primitive ints.
 * Jan 30, 2013  1543     djohnson  Use List instead of ArrayList.
 * Apr 08, 2013  1826     djohnson  Remove delivery options.
 * Sep 25, 2013  1797     dhladky   Handle gridded times
 * Oct 10, 2013  1797     bgonzale  Refactored registry Time objects.
 * Jul 08, 2015  4566     dhladky   Use AWIPS naming rather than provider
 *                                  naming.
 * Sep 12, 2017  6413     tjensen   Updated to support ParameterGroups
 *
 * </pre>
 *
 * @author mpduff
 */

public class SubscriptionDiff<T extends Time, C extends Coverage> {

    /** Subscription start/end date format */
    private final SimpleDateFormat format = new SimpleDateFormat(
            "MM/dd/yyyy HH");

    private final DecimalFormat numFormat = new DecimalFormat("###.##");

    /** Active period start/end date format */
    private final SimpleDateFormat activeFormat = new SimpleDateFormat("MM/dd");

    private static final String nl = "\n";

    private final Subscription<T, C> sub;

    private final InitialPendingSubscription<T, C> pendingSub;

    private HashMap<String, Boolean> diffMap;

    /**
     * Constructor.
     *
     * @param subscription
     *            The Subscription object
     * @param pendingSubscription
     *            The PendingSubscription object
     */
    public SubscriptionDiff(Subscription<T, C> subscription,
            InitialPendingSubscription<T, C> pendingSubscription) {
        this.sub = subscription;
        this.pendingSub = pendingSubscription;
    }

    /**
     * Get the differences.
     *
     * @return String detailing the differences
     */
    public String getDifferences() {
        StringBuilder buffer = new StringBuilder(2000);

        getMap();

        if (sub.getPriority() != pendingSub.getPriority()) {
            diffMap.put("priority", true);
        }

        if (sub.isFullDataSet() != pendingSub.isFullDataSet()) {
            diffMap.put("fullDataSet", true);
        }

        if (!(sub.getDescription().equals(pendingSub.getDescription()))) {
            diffMap.put("description", true);
        }

        if (sub.getActivePeriodEnd() != null
                && pendingSub.getActivePeriodEnd() == null
                || sub.getActivePeriodEnd() == null
                        && pendingSub.getActivePeriodEnd() != null) {
            diffMap.put("activePeriodEnd", true);
        } else {
            if (sub.getActivePeriodEnd() != null
                    && pendingSub.getActivePeriodEnd() != null) {
                if (!(sub.getActivePeriodEnd()
                        .equals(pendingSub.getActivePeriodEnd()))) {
                    diffMap.put("activePeriodEnd", true);
                }
            }
        }

        if (sub.getActivePeriodStart() != null
                && pendingSub.getActivePeriodStart() == null
                || sub.getActivePeriodStart() == null
                        && pendingSub.getActivePeriodStart() != null) {
            diffMap.put("activePeriodStart", true);
        } else {
            if (sub.getActivePeriodStart() != null
                    && pendingSub.getActivePeriodStart() != null) {
                if (!(sub.getActivePeriodStart()
                        .equals(pendingSub.getActivePeriodStart()))) {
                    diffMap.put("activePeriodStart", true);
                }
            }
        }

        Time subTime = sub.getTime();
        Time pendingTime = pendingSub.getTime();

        if (subTime.getRequestStart() != null
                && pendingTime.getRequestStart() == null
                || subTime.getRequestStart() == null
                        && pendingTime.getRequestStart() != null) {
            diffMap.put("subscriptionStart", true);
        } else {
            if (subTime.getRequestStart() != null
                    && pendingTime.getRequestStart() != null) {
                if (!(subTime.getRequestStart()
                        .equals(pendingTime.getRequestStart()))) {
                    diffMap.put("subscriptionStart", true);
                }
            }
        }

        if (subTime.getRequestEnd() != null
                && pendingTime.getRequestEnd() == null
                || subTime.getRequestEnd() == null
                        && pendingTime.getRequestEnd() != null) {
            diffMap.put("subscriptionEnd", true);
        } else {
            if (subTime.getRequestEnd() != null
                    && pendingTime.getRequestEnd() != null) {
                if (!(subTime.getRequestEnd()
                        .equals(pendingTime.getRequestEnd()))) {
                    diffMap.put("subscriptionEnd", true);
                }
            }
        }

        List<Integer> subCycles = null;
        List<Integer> pendingCycles = null;
        List<String> subFcstHoursAll = null;
        List<String> pendingFcstHoursAll = null;
        List<String> subFcstHours = new ArrayList<>();
        List<String> pendingFcstHours = new ArrayList<>();

        // handle gridded times
        if (subTime instanceof GriddedTime) {

            GriddedTime gtime = (GriddedTime) subTime;
            GriddedTime pTime = (GriddedTime) pendingTime;
            // Check cycle times
            subCycles = gtime.getCycleTimes();
            pendingCycles = gtime.getCycleTimes();

            if (!subCycles.containsAll(pendingCycles)
                    || !pendingCycles.containsAll(subCycles)) {
                diffMap.put("cycleTimes", true);
            }

            // Check forecast hours
            subFcstHoursAll = gtime.getFcstHours();
            pendingFcstHoursAll = gtime.getFcstHours();

            for (int i : gtime.getSelectedTimeIndices()) {
                subFcstHours.add(subFcstHoursAll.get(i));
            }

            for (int i : pTime.getSelectedTimeIndices()) {
                pendingFcstHours.add(pendingFcstHoursAll.get(i));
            }

            if (!subFcstHours.containsAll(pendingFcstHours)
                    || !pendingFcstHours.containsAll(subFcstHours)) {
                diffMap.put("fcstHours", true);
            }
        }

        Coverage cov = sub.getCoverage();
        Coverage pendingCov = pendingSub.getCoverage();

        ReferencedEnvelope env = cov.getRequestEnvelope();
        ReferencedEnvelope pendingEnv = pendingCov.getRequestEnvelope();

        boolean envEqual = env == null ? pendingEnv == null
                : env.equals(pendingEnv);

        if (!envEqual) {
            diffMap.put("coverage", true);
        }

        Map<String, ParameterGroup> subParamMap = sub.getParameterGroups();
        Map<String, ParameterGroup> pendingParamMap = pendingSub
                .getParameterGroups();

        List<ParameterGroup> addedParameters = ParameterUtils
                .getUnique(pendingParamMap, subParamMap);
        List<ParameterGroup> deletedParameters = ParameterUtils
                .getUnique(subParamMap, pendingParamMap);

        StringBuilder tmpBuffer = listGroupInfo(addedParameters);

        if (tmpBuffer.length() > 0) {
            buffer.append("New Parameters and Levels:").append(nl);
            buffer.append(tmpBuffer.toString());
            buffer.append(nl);
        }

        tmpBuffer = listGroupInfo(deletedParameters);
        if (tmpBuffer.length() > 0) {
            buffer.append("Removed Parameters and Levels:").append(nl);
            buffer.append(tmpBuffer.toString());
            buffer.append(nl);
        }

        if (diffMap.get("priority")) {
            buffer.append("Priority changed from ").append(sub.getPriority());
            buffer.append(" to ").append(pendingSub.getPriority()).append(nl)
                    .append(nl);
        }

        if (diffMap.get("subscriptionStart")) {
            buffer.append("Subscription Start changed from ");
            buffer.append(format.format(sub.getSubscriptionStart()));
            buffer.append(" to ");
            buffer.append(format.format(pendingSub.getSubscriptionStart()))
                    .append(nl).append(nl);
        }

        if (diffMap.get("subscriptionEnd")) {
            if (sub.getActivePeriodEnd() != null) {
                buffer.append("Subscription End changed from ");
                buffer.append(format.format(sub.getSubscriptionEnd()));
                buffer.append(" to ");
            } else {
                buffer.append("Subscription End set to ");
            }

            buffer.append(format.format(pendingSub.getSubscriptionEnd()))
                    .append(nl).append(nl);
        }

        boolean useActiveEnd = true;
        if (diffMap.get("activePeriodStart")) {
            if (sub.getActivePeriodStart() != null
                    && pendingSub.getActivePeriodStart() != null) {
                buffer.append("Subscription Active Period Start:").append(nl);
                buffer.append(activeFormat.format(sub.getActivePeriodStart()))
                        .append(" to ");
                buffer.append(
                        activeFormat.format(pendingSub.getActivePeriodStart()))
                        .append(nl);
            } else if (sub.getActivePeriodStart() == null
                    && pendingSub.getActivePeriodStart() != null) {
                buffer.append("Subscription Active Period Start set to ");
                buffer.append(
                        activeFormat.format(pendingSub.getActivePeriodStart()))
                        .append(nl);
            } else {
                buffer.append("Subscription Active Period has been removed.")
                        .append(nl).append(nl);
                useActiveEnd = false;
            }
        }

        if (diffMap.get("activePeriodEnd")) {
            if (useActiveEnd) {
                if (sub.getActivePeriodEnd() != null
                        && pendingSub.getActivePeriodEnd() != null) {
                    buffer.append("Subscription Active Period End:").append(nl);
                    buffer.append(activeFormat.format(sub.getActivePeriodEnd()))
                            .append(" to ");
                    buffer.append(activeFormat
                            .format(pendingSub.getActivePeriodEnd()))
                            .append(nl);
                } else if (sub.getActivePeriodEnd() == null
                        && pendingSub.getActivePeriodEnd() != null) {
                    buffer.append("Subscription Active Period End set to ");
                    buffer.append(activeFormat
                            .format(pendingSub.getActivePeriodEnd()))
                            .append(nl);
                }
            }
        }

        // handle pending and subscription cycles
        if (subCycles != null && pendingCycles != null) {

            if (diffMap.get("cycleTimes")) {
                buffer.append("Cycle Times changed:").append(nl);
                buffer.append("  New Cycle Times: ");

                for (int i : pendingCycles) {
                    buffer.append(i).append("  ");
                }
                buffer.append(nl);

                String s = this.getDiffs(subCycles, pendingCycles);
                if (s != null) {
                    buffer.append(s).append(nl);
                }
            }
        }

        // handle pending and subscription fcst hours
        if (!subFcstHours.isEmpty() && !pendingFcstHours.isEmpty()) {

            if (diffMap.get("fcstHours")) {
                buffer.append("Forecast Hours changed:").append(nl);
                buffer.append("  New Forecast Hours: ");
                for (String s : pendingFcstHours) {
                    buffer.append(s).append("  ");
                }
                buffer.append(nl);

                String s = this.getDiffs(subFcstHours, pendingFcstHours);
                if (s != null) {
                    buffer.append(s).append(nl);
                }
            }
        }

        if (diffMap.get("fullDataSet")) {
            if (pendingSub.isFullDataSet()) {
                buffer.append("Subscription is now for the Full Data Set");
            } else {
                buffer.append(
                        "Subscription is now a subset of the original area");
            }
            buffer.append(nl).append(nl);
        }

        if (diffMap.get("coverage")) {
            if (pendingEnv == null) {
                buffer.append("Areal coverage changed to full data set")
                        .append(nl);
            } else {
                if (env == null) {
                    buffer.append("Areal coverage changed to a subset:")
                            .append(nl);
                } else {
                    buffer.append("Areal coverage changed to:").append(nl);
                }
                Coordinate ul = pendingCov.getRequestUpperLeft();
                Coordinate lr = pendingCov.getRequestLowerRight();
                buffer.append("  Upper Left : ").append(numFormat.format(ul.x))
                        .append(", ").append(numFormat.format(ul.y)).append(nl);
                buffer.append("  Lower Right: ").append(numFormat.format(lr.x))
                        .append(", ").append(numFormat.format(lr.y)).append(nl);
            }
            buffer.append(nl);
        }

        if (diffMap.get("description")) {
            buffer.append("Original Description: ");
            buffer.append(sub.getDescription()).append(nl);
            buffer.append("New Description: ");
            buffer.append(pendingSub.getDescription()).append(nl).append(nl);
        }

        buffer.append("Data size changed from 575 to 430").append(nl);

        return buffer.toString();
    }

    private StringBuilder listGroupInfo(List<ParameterGroup> parameterGroups) {
        StringBuilder tmpBuffer = new StringBuilder();
        for (ParameterGroup newParam : parameterGroups) {
            tmpBuffer.append("Parameter: ").append(newParam.getKey())
                    .append(nl);
            for (LevelGroup newLg : newParam.getGroupedLevels().values()) {
                tmpBuffer.append("  Level Type: ").append(newLg.getKey())
                        .append(nl);
                for (ParameterLevelEntry newLevel : newLg.getLevels()) {
                    if (newLevel.getDisplayString() != null) {
                        tmpBuffer.append("    Level Info: ")
                                .append(newLevel.getDisplayString()).append(nl);
                    }
                }
            }
        }
        return tmpBuffer;
    }

    private String getDiffs(List<?> originalList, List<?> newList) {
        if (originalList.containsAll(newList)
                && newList.containsAll(originalList)) {
            // lists are the same, return null
            return null;
        }

        List<Object> additionsList = new ArrayList<>();
        List<Object> removedList = new ArrayList<>();

        // Find additions
        if (!originalList.containsAll(newList)) {
            for (Object o : newList) {
                if (!originalList.contains(o)) {
                    additionsList.add(o);
                }
            }
        }

        // Find removals
        if (!newList.containsAll(originalList)) {
            for (Object o : originalList) {
                if (!newList.contains(o)) {
                    removedList.add(o);
                }
            }
        }

        StringBuilder buffer = new StringBuilder("  ");
        if (!additionsList.isEmpty()) {
            buffer.append("Added items: ");
            for (Object o : additionsList) {
                buffer.append(o).append("  ");
            }

            buffer.append(nl);
        }
        if (!removedList.isEmpty()) {
            buffer.append("  Removed items: ");
            for (Object o : removedList) {
                buffer.append(o).append("  ");
            }

            buffer.append(nl);
        }

        return buffer.toString();
    }

    private HashMap<String, Boolean> getMap() {
        if (diffMap == null) {
            diffMap = new HashMap<>();

            diffMap.put("priority", false);
            diffMap.put("subscriptionStart", false);
            diffMap.put("subscriptionEnd", false);
            diffMap.put("activePeriodEnd", false);
            diffMap.put("activePeriodStart", false);
            diffMap.put("cycleTimes", false);
            diffMap.put("fcstHours", false);
            diffMap.put("notify", false);
            diffMap.put("fullDataSet", false);
            diffMap.put("coverage", false);
            diffMap.put("description", false);
            diffMap.put("parameter", false);
        }

        return diffMap;
    }
}
