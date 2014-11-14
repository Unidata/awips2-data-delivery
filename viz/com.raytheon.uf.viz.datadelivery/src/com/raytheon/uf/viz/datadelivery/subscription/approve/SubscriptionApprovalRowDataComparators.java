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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.viz.datadelivery.common.ui.SortDirection;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils.PendingSubColumnNames;

/**
 * Provide Comparators for sorting a Data objects in the Subscription Approval
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

public class SubscriptionApprovalRowDataComparators {

    private static Map<PendingSubColumnNames, Comparator<SubscriptionApprovalRowData>> normalOrderMap = new HashMap<PendingSubColumnNames, Comparator<SubscriptionApprovalRowData>>();

    private static Map<PendingSubColumnNames, Comparator<SubscriptionApprovalRowData>> reverseOrderMap = new HashMap<PendingSubColumnNames, Comparator<SubscriptionApprovalRowData>>();

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

    public static final Comparator<SubscriptionApprovalRowData> NAME_COMP = new Comparator<SubscriptionApprovalRowData>() {
        @Override
        public int compare(SubscriptionApprovalRowData o1,
                SubscriptionApprovalRowData o2) {
            return internalCompare(o1.getSubName(), o2.getSubName());
        }
    };

    public static final Comparator<SubscriptionApprovalRowData> DESCRIPTION_COMP = new Comparator<SubscriptionApprovalRowData>() {
        @Override
        public int compare(SubscriptionApprovalRowData o1,
                SubscriptionApprovalRowData o2) {
            return internalCompare(o1.getDescription(), o2.getDescription());
        }
    };

    public static final Comparator<SubscriptionApprovalRowData> OWNER_COMP = new Comparator<SubscriptionApprovalRowData>() {
        @Override
        public int compare(SubscriptionApprovalRowData o1,
                SubscriptionApprovalRowData o2) {
            return internalCompare(o1.getOwner(), o2.getOwner());
        }
    };

    public static final Comparator<SubscriptionApprovalRowData> CHANGE_ID_COMP = new Comparator<SubscriptionApprovalRowData>() {
        @Override
        public int compare(SubscriptionApprovalRowData o1,
                SubscriptionApprovalRowData o2) {
            return internalCompare(o1.getChangeOwner(), o2.getChangeOwner());
        }
    };

    public static final Comparator<SubscriptionApprovalRowData> ACTION_COMP = new Comparator<SubscriptionApprovalRowData>() {
        @Override
        public int compare(SubscriptionApprovalRowData o1,
                SubscriptionApprovalRowData o2) {
            return internalCompare(o1.getAction(), o2.getAction());
        }
    };

    public static final Comparator<SubscriptionApprovalRowData> OFFICE_COMP = new Comparator<SubscriptionApprovalRowData>() {
        @Override
        public int compare(SubscriptionApprovalRowData o1,
                SubscriptionApprovalRowData o2) {
            return internalCompare(o1.getOfficeIdsAsString(),
                    o2.getOfficeIdsAsString());
        }
    };

    static {
        normalOrderMap.put(PendingSubColumnNames.NAME, NAME_COMP);
        reverseOrderMap.put(PendingSubColumnNames.NAME,
                Collections.reverseOrder(NAME_COMP));

        normalOrderMap.put(PendingSubColumnNames.DESCRIPTION, DESCRIPTION_COMP);
        reverseOrderMap.put(PendingSubColumnNames.DESCRIPTION,
                Collections.reverseOrder(DESCRIPTION_COMP));

        normalOrderMap.put(PendingSubColumnNames.OWNER, OWNER_COMP);
        reverseOrderMap.put(PendingSubColumnNames.OWNER,
                Collections.reverseOrder(OWNER_COMP));

        normalOrderMap.put(PendingSubColumnNames.CHANGE_ID, CHANGE_ID_COMP);
        reverseOrderMap.put(PendingSubColumnNames.CHANGE_ID,
                Collections.reverseOrder(CHANGE_ID_COMP));

        normalOrderMap.put(PendingSubColumnNames.ACTION, ACTION_COMP);
        reverseOrderMap.put(PendingSubColumnNames.ACTION,
                Collections.reverseOrder(ACTION_COMP));

        normalOrderMap.put(PendingSubColumnNames.OFFICE, OFFICE_COMP);
        reverseOrderMap.put(PendingSubColumnNames.OFFICE,
                Collections.reverseOrder(OFFICE_COMP));
    }

    public static Comparator<SubscriptionApprovalRowData> getComparator(
            String columnName, SortDirection sortDirection) {
        if (columnName != null) {
            PendingSubColumnNames columnEnum = PendingSubColumnNames
                    .valueOfColumnName(columnName);
            return (getComparator(columnEnum, sortDirection));
        }
        return (null);
    }

    public static Comparator<SubscriptionApprovalRowData> getComparator(
            PendingSubColumnNames columnEnum, SortDirection sortDirection) {
        if ((sortDirection == null)
                || (sortDirection == SortDirection.ASCENDING)) {
            return normalOrderMap.get(columnEnum);
        } else {
            return reverseOrderMap.get(columnEnum);
        }
    }
}
