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
package com.raytheon.uf.viz.datadelivery.common.ui;

/**
 * This is a generic table data manager that will be used to manage data that will be displayed in a table.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 06, 2012            lvenable     Initial creation
 * Apr 10, 2013   1891     djohnson     Declare variable as List.
 * Feb 07, 2014   2453     mpduff       Added getSize().
 * Apr 18, 2014   3012     dhladky      Null check.
 * Dec 03, 2014   3840     ccody        Correct sorting "contract violation" issue
 * Jun 09, 2015   4047     dhladky      Made thread safe.
 *
 * </pre>
 *
 * @author lvenable
 * @version 1.0	
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils.TABLE_TYPE;

public class TableDataManager<T extends ITableData> {

    /**
     * Array of data.
     */
    private final List<T> tableData;

    /**
     * Constructor.
     * 
     * @param tableType
     *            Type of table the data will be use for.
     * @param defaultSortColumnText
     *            Default field to sort on
     * @param defaultSortOrder
     *            Default order to sort in
     */
    public TableDataManager(TABLE_TYPE tableType) {
        tableData = new ArrayList<T>();
    }

    /**
     * Add a "row" of data that will be displayed in the table.
     * 
     * @param data
     *            Data to be added to the data array.
     */
    public void addDataRow(T data) {
        synchronized (tableData) {
            tableData.add(data);
        }
    }

    /**
     * Get the data array.
     * 
     * @return The data array.
     */
    public List<T> getDataArray() {
        return this.getCopy();
    }

    /**
     * Sort the data array.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void sortData(Comparator comparator) {
        synchronized (tableData) {
            Collections.sort(this.tableData, comparator);
        }
    }

    /**
     * Clear the data array.
     */
    public void clearAll() {
        synchronized (tableData) {
            tableData.clear();
        }
    }

    /**
     * Remove all of the items in the remove list from the data array.
     * 
     * @param removeList
     *            The list of data to remove.
     */
    public void removeAll(List<T> removeList) {
        synchronized (tableData) {
            tableData.removeAll(removeList);
        }
    }

    /**
     * Get the row of data at the specified index.
     * 
     * @param index
     *            Index.
     * @return The data at the the specified index.
     */
    public T getDataRow(int index) {
        synchronized (tableData) {
            if (index >= 0 && index < tableData.size()) {
                return tableData.get(index);
            } else {
                if (!tableData.isEmpty()) {
                    return tableData.get(0);
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Remove data at the specified index.
     * 
     * @param index
     *            Index of the data to be removed.
     */
    public void removeDataRow(int index) {
        synchronized (tableData) {
            if (index >= 0 && index < tableData.size()) {
                tableData.remove(index);
            }
        }
    }

    /**
     * Get the size of the data array.
     * 
     * @return The size
     */
    public int getSize() {
        synchronized (tableData) {
            return this.tableData.size();
        }
    }

    /**
     * Copy for thread safety
     * 
     * @return
     */
    private List<T> getCopy() {

        List<T> tableDataCopy = null;
        synchronized (tableData) {
            tableDataCopy = new ArrayList<T>(tableData.size());
            for (T row : tableData) {
                T rowCopy = row;
                tableDataCopy.add(rowCopy);
            }
        }
        return tableDataCopy;
    }
}
