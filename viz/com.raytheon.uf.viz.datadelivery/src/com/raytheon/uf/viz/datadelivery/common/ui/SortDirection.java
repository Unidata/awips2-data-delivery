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
 * Interface used when sorting a table.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 08, 2014  3840      ccody       Initial creation
 * 
 * </pre>
 * 
 * @author ccody
 * @version 1.0
 */

/**
 * Sort direction enumeration that identifies which image to use.
 */
public enum SortDirection {
    ASCENDING {
        @Override
        public SortDirection reverse() {
            return SortDirection.DESCENDING;
        }
    },
    DESCENDING {
        @Override
        public SortDirection reverse() {
            return SortDirection.ASCENDING;
        }
    };

    /**
     * Reverse the sorting direction.
     * 
     * @return the reverse direction
     */
    public abstract SortDirection reverse();
}
