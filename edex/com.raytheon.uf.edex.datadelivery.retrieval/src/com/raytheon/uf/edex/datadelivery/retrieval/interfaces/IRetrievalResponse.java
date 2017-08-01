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

import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord.State;

/**
 * Interface for Provider Response Builder
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 16, 2011           dhladky   Initial creation
 * Feb 12, 2013  1543     djohnson  The payload can just be an arbitrary object,
 *                                  implementations can define an array if
 *                                  required.
 * Feb 15, 2013  1543     djohnson  Expose the setAttributes method.
 * Jul 27, 2017  6186     rjpeter   Removed payload methods
 * Aug 02, 2017  6186     rjpeter   Added getNextState
 *
 * </pre>
 *
 * @author dhladky
 */
public interface IRetrievalResponse {
    State getNextState();

    void prepareForSerialization() throws Exception;
}
