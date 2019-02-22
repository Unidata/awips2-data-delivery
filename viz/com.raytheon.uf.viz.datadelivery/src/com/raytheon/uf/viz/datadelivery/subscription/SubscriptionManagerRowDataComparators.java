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
package com.raytheon.uf.viz.datadelivery.subscription;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.viz.datadelivery.common.ui.SortDirection;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils.SubColumnNames;

/**
 * Provide Comparators for sorting a Data objects for the Subscription Manager
 * Dialog.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 03, 2014 3840       ccody     Initial creation
 * 
 * </pre>
 * 
 * @author ccody
 * @version 1.0
 */

public class SubscriptionManagerRowDataComparators {

    private static Map<SubColumnNames, Comparator<SubscriptionManagerRowData>> normalOrderMap = new HashMap<SubColumnNames, Comparator<SubscriptionManagerRowData>>();

    private static Map<SubColumnNames, Comparator<SubscriptionManagerRowData>> reverseOrderMap = new HashMap<SubColumnNames, Comparator<SubscriptionManagerRowData>>();

    protected static int internalStringCompare(String stringComp1,
            String stringComp2) {
        return (stringComp1.compareToIgnoreCase(stringComp2));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static int internalCompare(Comparable comp1, Comparable comp2) {
        // For AWIPS Sorting (Ascending) NULL values will be sorted to the
        // bottom of the list i.e. == 1; greater
        if ((comp1 == null) && (comp2 == null)) {
            return (0);
        }
        if (comp1 == null) {
            return (1);
        }
        if (comp2 == null) {
            return (-1);
        }
        if ((comp1 instanceof String) && (comp2 instanceof String)) {
            return (internalStringCompare(((String) comp1), ((String) comp2)));
        } else {
            return (comp1.compareTo(comp2));
        }
    }

    public static final Comparator<SubscriptionManagerRowData> NAME_COMP = new Comparator<SubscriptionManagerRowData>() {
        @Override
        public int compare(SubscriptionManagerRowData o1,
                SubscriptionManagerRowData o2) {
            return internalCompare(o1.getName(), o2.getName());
        }
    };

    public static final Comparator<SubscriptionManagerRowData> DESCRIPTION_COMP = new Comparator<SubscriptionManagerRowData>() {
        @Override
        public int compare(SubscriptionManagerRowData o1,
                SubscriptionManagerRowData o2) {
            return internalCompare(o1.getDescription(), o2.getDescription());
        }
    };

    public static final Comparator<SubscriptionManagerRowData> OWNER_COMP = new Comparator<SubscriptionManagerRowData>() {
        @Override
        public int compare(SubscriptionManagerRowData o1,
                SubscriptionManagerRowData o2) {
            return internalCompare(o1.getOwner(), o2.getOwner());
        }
    };

    public static final Comparator<SubscriptionManagerRowData> STATUS_COMP = new Comparator<SubscriptionManagerRowData>() {
        @Override
        public int compare(SubscriptionManagerRowData o1,
                SubscriptionManagerRowData o2) {
            return internalCompare(o1.getStatus(), o2.getStatus());
        }
    };

    public static final Comparator<SubscriptionManagerRowData> PRIORITY_COMP = new Comparator<SubscriptionManagerRowData>() {
        @Override
        public int compare(SubscriptionManagerRowData o1,
                SubscriptionManagerRowData o2) {
            return Integer.compare(o1.getPriority(), o2.getPriority());
        }
    };

    public static final Comparator<SubscriptionManagerRowData> SUBSCRIPTION_START_COMP = new Comparator<SubscriptionManagerRowData>() {
        @Override
        public int compare(SubscriptionManagerRowData o1,
                SubscriptionManagerRowData o2) {
            return internalCompare(o1.getSubscriptionStart(),
                    o2.getSubscriptionStart());
        }
    };

    public static final Comparator<SubscriptionManagerRowData> SUBSCRIPTION_EXPIRATION_COMP = new Comparator<SubscriptionManagerRowData>() {
        @Override
        public int compare(SubscriptionManagerRowData o1,
                SubscriptionManagerRowData o2) {
            return internalCompare(o1.getSubscriptionEnd(),
                    o2.getSubscriptionEnd());
        }
    };

    public static final Comparator<SubscriptionManagerRowData> ACTIVE_START_COMP = new Comparator<SubscriptionManagerRowData>() {
        @Override
        public int compare(SubscriptionManagerRowData o1,
                SubscriptionManagerRowData o2) {
            return internalCompare(o1.getActiveStart(), o2.getActiveStart());
        }
    };

    public static final Comparator<SubscriptionManagerRowData> ACTIVE_END_COMP = new Comparator<SubscriptionManagerRowData>() {
        @Override
        public int compare(SubscriptionManagerRowData o1,
                SubscriptionManagerRowData o2) {
            return internalCompare(o1.getActiveEnd(), o2.getActiveEnd());
        }
    };

    public static final Comparator<SubscriptionManagerRowData> OFFICE_ID_COMP = new Comparator<SubscriptionManagerRowData>() {
        @Override
        public int compare(SubscriptionManagerRowData o1,
                SubscriptionManagerRowData o2) {
            Set<String> officeIds1 = o1.getOfficeIds();
            String officeIdsString1 = null;
            if (officeIds1 != null) {
                officeIdsString1 = StringUtil.join(officeIds1, ',');
            }
            Set<String> officeIds2 = o2.getOfficeIds();
            String officeIdsString2 = null;
            if (officeIds2 != null) {
                officeIdsString2 = StringUtil.join(officeIds2, ',');
            }

            return internalCompare(officeIdsString1, officeIdsString2);
        }
    };

    public static final Comparator<SubscriptionManagerRowData> FULL_DATA_SET_COMP = new Comparator<SubscriptionManagerRowData>() {
        @Override
        public int compare(SubscriptionManagerRowData o1,
                SubscriptionManagerRowData o2) {
            return internalCompare(o1.getFullDataSet(), o2.getFullDataSet());
        }
    };

    public static final Comparator<SubscriptionManagerRowData> DATA_SIZE_COMP = new Comparator<SubscriptionManagerRowData>() {
        @Override
        public int compare(SubscriptionManagerRowData o1,
                SubscriptionManagerRowData o2) {
            return Long.compare(o1.getDataSetSize(), o2.getDataSetSize());
        }
    };

    public static final Comparator<SubscriptionManagerRowData> GROUP_NAME_COMP = new Comparator<SubscriptionManagerRowData>() {
        @Override
        public int compare(SubscriptionManagerRowData o1,
                SubscriptionManagerRowData o2) {
            return internalCompare(o1.getGroupName(), o2.getGroupName());
        }
    };
    static {

        normalOrderMap.put(SubColumnNames.NAME, NAME_COMP);
        reverseOrderMap.put(SubColumnNames.NAME,
                Collections.reverseOrder(NAME_COMP));

        normalOrderMap.put(SubColumnNames.DESCRIPTION, DESCRIPTION_COMP);
        reverseOrderMap.put(SubColumnNames.DESCRIPTION,
                Collections.reverseOrder(DESCRIPTION_COMP));

        normalOrderMap.put(SubColumnNames.OWNER, OWNER_COMP);
        reverseOrderMap.put(SubColumnNames.OWNER,
                Collections.reverseOrder(OWNER_COMP));

        normalOrderMap.put(SubColumnNames.STATUS, STATUS_COMP);
        reverseOrderMap.put(SubColumnNames.STATUS,
                Collections.reverseOrder(STATUS_COMP));

        normalOrderMap.put(SubColumnNames.PRIORITY, PRIORITY_COMP);
        reverseOrderMap.put(SubColumnNames.PRIORITY,
                Collections.reverseOrder(PRIORITY_COMP));

        normalOrderMap.put(SubColumnNames.SUBSCRIPTION_START,
                SUBSCRIPTION_START_COMP);
        reverseOrderMap.put(SubColumnNames.SUBSCRIPTION_START,
                Collections.reverseOrder(SUBSCRIPTION_START_COMP));

        normalOrderMap.put(SubColumnNames.SUBSCRIPTION_EXPIRATION,
                SUBSCRIPTION_EXPIRATION_COMP);
        reverseOrderMap.put(SubColumnNames.SUBSCRIPTION_EXPIRATION,
                Collections.reverseOrder(SUBSCRIPTION_EXPIRATION_COMP));

        normalOrderMap.put(SubColumnNames.ACTIVE_START, ACTIVE_START_COMP);
        reverseOrderMap.put(SubColumnNames.ACTIVE_START,
                Collections.reverseOrder(ACTIVE_START_COMP));

        normalOrderMap.put(SubColumnNames.ACTIVE_END, ACTIVE_END_COMP);
        reverseOrderMap.put(SubColumnNames.ACTIVE_END,
                Collections.reverseOrder(ACTIVE_END_COMP));

        normalOrderMap.put(SubColumnNames.OFFICE_ID, OFFICE_ID_COMP);
        reverseOrderMap.put(SubColumnNames.OFFICE_ID,
                Collections.reverseOrder(OFFICE_ID_COMP));

        normalOrderMap.put(SubColumnNames.FULL_DATA_SET, FULL_DATA_SET_COMP);
        reverseOrderMap.put(SubColumnNames.FULL_DATA_SET,
                Collections.reverseOrder(FULL_DATA_SET_COMP));

        normalOrderMap.put(SubColumnNames.DATA_SIZE, DATA_SIZE_COMP);
        reverseOrderMap.put(SubColumnNames.DATA_SIZE,
                Collections.reverseOrder(DATA_SIZE_COMP));

        normalOrderMap.put(SubColumnNames.GROUP_NAME, GROUP_NAME_COMP);
        reverseOrderMap.put(SubColumnNames.GROUP_NAME,
                Collections.reverseOrder(GROUP_NAME_COMP));

    }

    public static Comparator<SubscriptionManagerRowData> getComparator(
            String columnName, SortDirection sortDirection) {
        if (columnName != null) {
            SubColumnNames columnEnum = SubColumnNames
                    .fromDisplayString(columnName);
            return (getComparator(columnEnum, sortDirection));
        }
        return (null);
    }

    public static Comparator<SubscriptionManagerRowData> getComparator(
            SubColumnNames columnEnum, SortDirection sortDirection) {
        if ((sortDirection == null)
                || (sortDirection == SortDirection.ASCENDING)) {
            return normalOrderMap.get(columnEnum);
        } else {
            return reverseOrderMap.get(columnEnum);
        }
    }

}
