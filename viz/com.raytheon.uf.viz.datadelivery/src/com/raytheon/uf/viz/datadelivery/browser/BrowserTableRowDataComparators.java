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
package com.raytheon.uf.viz.datadelivery.browser;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.viz.datadelivery.common.ui.SortDirection;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils.BrowserColumnNames;

/**
 * Provide Comparators for sorting a Data objects for the Browser Dialog.
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

public class BrowserTableRowDataComparators {

    private static Map<BrowserColumnNames, Comparator<BrowserTableRowData>> normalOrderMap = new HashMap<BrowserColumnNames, Comparator<BrowserTableRowData>>();

    private static Map<BrowserColumnNames, Comparator<BrowserTableRowData>> reverseOrderMap = new HashMap<BrowserColumnNames, Comparator<BrowserTableRowData>>();

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

    public static final Comparator<BrowserTableRowData> SUBSCRIPTION_COMP = new Comparator<BrowserTableRowData>() {
        @Override
        public int compare(BrowserTableRowData o1, BrowserTableRowData o2) {
            return internalCompare(o1.getSubscriptionName(),
                    o2.getSubscriptionName());
        }
    };

    public static final Comparator<BrowserTableRowData> PROVIDER_COMP = new Comparator<BrowserTableRowData>() {
        @Override
        public int compare(BrowserTableRowData o1, BrowserTableRowData o2) {
            return internalCompare(o1.getProviderName(), o2.getProviderName());
        }
    };

    public static final Comparator<BrowserTableRowData> NAME_COMP = new Comparator<BrowserTableRowData>() {
        @Override
        public int compare(BrowserTableRowData o1, BrowserTableRowData o2) {
            return internalCompare(o1.getDataSetName(), o2.getDataSetName());
        }
    };

    static {
        normalOrderMap.put(BrowserColumnNames.SUBSCRIPTION, SUBSCRIPTION_COMP);
        reverseOrderMap.put(BrowserColumnNames.SUBSCRIPTION,
                Collections.reverseOrder(SUBSCRIPTION_COMP));

        normalOrderMap.put(BrowserColumnNames.PROVIDER, PROVIDER_COMP);
        reverseOrderMap.put(BrowserColumnNames.PROVIDER,
                Collections.reverseOrder(PROVIDER_COMP));

        normalOrderMap.put(BrowserColumnNames.NAME, NAME_COMP);
        reverseOrderMap.put(BrowserColumnNames.NAME,
                Collections.reverseOrder(NAME_COMP));
    }

    public static Comparator<BrowserTableRowData> getComparator(
            String columnName, SortDirection sortDirection) {
        if (columnName != null) {
            BrowserColumnNames columnEnum = BrowserColumnNames
                    .valueOfColumnName(columnName);
            return (getComparator(columnEnum, sortDirection));
        }
        return (null);
    }

    public static Comparator<BrowserTableRowData> getComparator(
            BrowserColumnNames columnEnum, SortDirection sortDirection) {
        if ((sortDirection == null)
                || (sortDirection == SortDirection.ASCENDING)) {
            return normalOrderMap.get(columnEnum);
        } else {
            return reverseOrderMap.get(columnEnum);
        }
    }

}
