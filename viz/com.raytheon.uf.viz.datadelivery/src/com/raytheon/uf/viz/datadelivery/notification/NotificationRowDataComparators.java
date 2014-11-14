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
package com.raytheon.uf.viz.datadelivery.notification;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.viz.datadelivery.common.ui.SortDirection;

/**
 * Provide Comparators for sorting a Data objects for the Notification Manager
 * Dialog.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 04, 2014  3840       ccody     Initial creation
 * 
 * </pre>
 * 
 * @author ccody
 * @version 1.0
 */

public class NotificationRowDataComparators {

    private static Map<String, Comparator<NotificationRowData>> normalOrderMap = new HashMap<String, Comparator<NotificationRowData>>();

    private static Map<String, Comparator<NotificationRowData>> reverseOrderMap = new HashMap<String, Comparator<NotificationRowData>>();

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

    public static final Comparator<NotificationRowData> TIME_COMP = new Comparator<NotificationRowData>() {
        @Override
        public int compare(NotificationRowData o1, NotificationRowData o2) {
            return internalCompare(o1.getDate(), o2.getDate());
        }
    };

    public static final Comparator<NotificationRowData> PRIORITY_COMP = new Comparator<NotificationRowData>() {
        @Override
        public int compare(NotificationRowData o1, NotificationRowData o2) {
            return Integer.compare(o1.getPriority(), o2.getPriority());
        }
    };

    public static final Comparator<NotificationRowData> CATEGORY_COMP = new Comparator<NotificationRowData>() {
        @Override
        public int compare(NotificationRowData o1, NotificationRowData o2) {
            return internalCompare(o1.getCategory(), o2.getCategory());
        }
    };

    public static final Comparator<NotificationRowData> USER_COMP = new Comparator<NotificationRowData>() {
        @Override
        public int compare(NotificationRowData o1, NotificationRowData o2) {
            return internalCompare(o1.getUser(), o2.getUser());
        }
    };

    public static final Comparator<NotificationRowData> MESSAGE_COMP = new Comparator<NotificationRowData>() {
        @Override
        public int compare(NotificationRowData o1, NotificationRowData o2) {
            return internalCompare(o1.getMessage(), o2.getMessage());
        }
    };

    static {
        normalOrderMap.put("Time", TIME_COMP);
        reverseOrderMap.put("Time", Collections.reverseOrder(TIME_COMP));
        normalOrderMap.put("Priority", PRIORITY_COMP);
        reverseOrderMap
                .put("Priority", Collections.reverseOrder(PRIORITY_COMP));
        normalOrderMap.put("Category", CATEGORY_COMP);
        reverseOrderMap
                .put("Category", Collections.reverseOrder(CATEGORY_COMP));
        normalOrderMap.put("User", USER_COMP);
        reverseOrderMap.put("User", Collections.reverseOrder(USER_COMP));
        normalOrderMap.put("Message", MESSAGE_COMP);
        reverseOrderMap.put("Message", Collections.reverseOrder(MESSAGE_COMP));
    }

    public static Comparator<NotificationRowData> getComparator(
            String columnName, SortDirection sortDirection) {
        if ((sortDirection == null)
                || (sortDirection == SortDirection.ASCENDING)) {
            return normalOrderMap.get(columnName);
        } else {
            return reverseOrderMap.get(columnName);
        }
    }

}
