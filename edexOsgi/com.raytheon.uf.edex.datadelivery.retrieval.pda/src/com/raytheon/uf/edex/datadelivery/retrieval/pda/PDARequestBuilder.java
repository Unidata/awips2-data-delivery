package com.raytheon.uf.edex.datadelivery.retrieval.pda;
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
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.edex.datadelivery.retrieval.request.RequestBuilder;

/**
 * 
 * PDA Request Builder.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 12, 2013 3120        dhladky      created.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */


public class PDARequestBuilder extends RequestBuilder {

    protected PDARequestBuilder(RetrievalAttribute ra) {
        super(ra);
    }

    @Override
    public String processTime(Time prtXML) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String processCoverage(Coverage Coverage) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRequest() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RetrievalAttribute getAttribute() {
        // TODO Auto-generated method stub
        return null;
    }

}
