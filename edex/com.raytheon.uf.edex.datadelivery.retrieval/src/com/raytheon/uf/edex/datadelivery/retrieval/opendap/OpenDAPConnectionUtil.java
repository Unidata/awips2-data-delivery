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
package com.raytheon.uf.edex.datadelivery.retrieval.opendap;

import java.io.FileNotFoundException;

import com.raytheon.opendap.InputStreamWrapper;

/**
 * Utilities for datadelivery connections.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 28, 2012  819      djohnson  Initial creation
 * Apr 01, 2013  1786     mpduff    Pulled proxy settings out to util class.
 * May 12, 2013  753      dhladky   Expanded for use with other connection types
 * Aug 30, 2013  2314     mpduff    Added null checks.
 * Nov 12, 2013           dhladky   Fixed copy paste error
 * Jun 18, 2014  1712     bphillip  Updated Proxy configuration
 * Apr 14, 2015  4400     dhladky   Upgraded to DAP2 with backward
 *                                  compatibility.
 * Jun 06, 2017  6222     tgurney   Use token bucket to rate-limit requests
 * Jun 22, 2017  6222     tgurney   Receive stream wrapper from caller
 * Sep 21, 2017  6441     tgurney   Remove references to dods-1.1.7
 * Jun 12, 2018  7320     rjpeter   Fixed passing of streamWrapper.
 *
 * </pre>
 *
 * @author djohnson
 */

public class OpenDAPConnectionUtil {

    /**
     * Retrieve a DConnect instance.
     *
     * @param urlString
     * @return DConnect instance
     * @throws FileNotFoundException
     *             rethrown from DConnect
     */
    public static opendap.dap.DConnect getDConnectDAP2(String urlString)
            throws FileNotFoundException {
        return getDConnectDAP2(urlString, null);
    }

    /**
     * Retrieve a DConnect instance.
     *
     * @param urlString
     * @return DConnect instance
     * @throws FileNotFoundException
     *             rethrown from DConnect
     */
    public static opendap.dap.DConnect getDConnectDAP2(String urlString,
            InputStreamWrapper streamWrapper) throws FileNotFoundException {
        // new DAP2-serialized version
        return new opendap.dap.DConnect(urlString, streamWrapper);
    }

    /**
     * Package level constructor so test can call.
     */
    OpenDAPConnectionUtil() {
    }
}