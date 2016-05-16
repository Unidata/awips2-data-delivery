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
package com.raytheon.uf.edex.datadelivery.retrieval.pda;

import com.raytheon.uf.common.dataplugin.PluginDataObject;

/**
 * 
 * Help convert to good Satellite (PDA) Records.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------------------
 * Aug 28, 2014  3121     dhladky   Initial javadoc
 * May 03, 2016  5599     tjensen   Added subscription name to decodeObjects
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public interface IPDAMetaDataAdapter {

    /**
     * Gets the correct satellite decoder
     * 
     * @param fileName
     *            path to file
     * @param subName
     *            Subscription Name
     * @return
     * @throws Exception
     */
    public PluginDataObject[] decodeObjects(String fileName, String subName)
            throws Exception;

}
