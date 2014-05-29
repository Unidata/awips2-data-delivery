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
package com.raytheon.uf.common.datadelivery.request;


/**
 * Consolidates data delivery server constants.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 14, 2012 1286       djohnson     Initial creation
 * Dec 03, 2012 1379       djohnson     Separate registry service keys.
 * Feb 26, 2013 1643       djohnson     Add NCF_BANDWIDTH_MANAGER_SERVICE.
 * Mar 21, 2013 1794       djohnson     Add flag denoting whether phase3 code is enabled.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
public final class DataDeliveryConstants {

    public static final String DATA_DELIVERY_SERVER = "datadelivery.server";

    public static final String NCF_BANDWIDTH_MANAGER_SERVICE = "ncf.bandwidth.manager.service";

    /**
     * Private constructor.
     */
    private DataDeliveryConstants() {

    }
}
