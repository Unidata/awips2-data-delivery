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
package com.raytheon.uf.edex.datadelivery.retrieval.interfaces;

import java.util.Date;
import java.util.Map;

/**
 * Extract MetaData Interface
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------------
 * Feb 20, 2011  218      dhladky   Initial creation
 * Jul 10, 2014  2130     dhladky   Expanded to include PDA
 * Mar 31, 2017  6186     rjpeter   Fixed Generics
 *
 * </pre>
 *
 * @author dhladky
 */
public interface IExtractMetaData<O extends Object, D extends Object> {

    Map<String, D> extractMetaData(O obj) throws Exception;

    void setDataDate() throws Exception;

    Date getDataDate();

}
