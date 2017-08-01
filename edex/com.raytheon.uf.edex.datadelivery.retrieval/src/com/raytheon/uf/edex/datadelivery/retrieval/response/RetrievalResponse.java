package com.raytheon.uf.edex.datadelivery.retrieval.response;

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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord.State;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;

/**
 * Provider Retrieval Response wrapper -extend this class if you wish
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 07, 2011           dhladky   Initial creation
 * Feb 12, 2013  1543     djohnson  Abstract class now.
 * Feb 15, 2013  1543     djohnson  Sub-classes must implement payload methods,
 *                                  make JAXBable.
 * Aug 02, 2017  6186     rjpeter   Implemented getNextState.
 *
 * </pre>
 *
 * /
 *
 * @author dhladky
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class RetrievalResponse implements IRetrievalResponse {
    @Override
    public State getNextState() {
        return State.COMPLETED;
    }

    @Override
    public void prepareForSerialization() throws Exception {
        // empty impl
    }
}
