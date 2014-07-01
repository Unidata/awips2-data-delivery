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
package com.raytheon.uf.edex.datadelivery.retrieval.util;

import java.io.FileNotFoundException;

import com.raytheon.uf.common.comm.ProxyConfiguration;

import dods.dap.DConnect;

/**
 * Utilities for datadelivery connections.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 28, 2012 819        djohnson     Initial creation
 * Apr 01, 2013 1786       mpduff       Pulled proxy settings out to util class.
 * May 12, 2013 753        dhladky      Expanded for use with other connection types
 * Aug 30, 2013  2314      mpduff       Added null checks.
 * Nov 12, 2013  *         dhladky      Fixed copy paste error
 * 6/18/2014    1712        bphillip    Updated Proxy configuration
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class ConnectionUtil {

    static ConnectionUtil instance = new ConnectionUtil();

    /**
     * Retrieve a DConnect instance.
     * 
     * @param urlString
     * @return DConnect instance
     * @throws FileNotFoundException
     *             rethrown from DConnect
     */
    public static DConnect getDConnect(String urlString)
            throws FileNotFoundException {
        if (ProxyConfiguration.HTTP_PROXY_DEFINED) {
            return new DConnect(urlString,
                    ProxyConfiguration.getHttpProxyHost(),
                    ProxyConfiguration.getHttpProxyPortString());
        } else {
            return new DConnect(urlString);
        }
    }

    /**
     * Package level constructor so test can call.
     */
    ConnectionUtil() {
    }
}