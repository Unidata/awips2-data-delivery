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
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
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
 * Jun 12, 2014 3120        dhladky      created.
 * Sept 04, 2014 3121       dhladky     Clarified and sharpened creation, largely un-implemented at this point.
 * Sept 27, 2014 3127       dhladky     Geographic subsetting.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */


public class PDARequestBuilder extends RequestBuilder<Time,Coverage> {
    
    protected static ServiceConfig pdaServiceConfig;
    
    private String request = null;
    
    public static final String CRS = getServiceConfig().getConstantValue("DEFAULT_CRS");
    
    public static final String BLANK = getServiceConfig().getConstantValue("BLANK");
        
    protected PDARequestBuilder(RetrievalAttribute<Time,Coverage> ra) {

        super(ra);
    }

    @Override
    public String processTime(Time prtXML) {
        throw new UnsupportedOperationException("Not implemented for PDA!");
    }
    
    @Override
    public String processCoverage(Coverage Coverage) {
        throw new UnsupportedOperationException("Not implemented for this portion of PDA!");
    }

    @Override
    public String getRequest() {
        // There are no switches for full data set PDA. 
        return request;
    }
    
    /**
     * Sets the request string
     * @param request
     */
    public void setRequest(String request) {
        this.request = request;
    }

    /**
     * Get the instance of the service config
     * @return
     */
    protected static ServiceConfig getServiceConfig() {
        
        if (pdaServiceConfig == null) {
            pdaServiceConfig = HarvesterServiceManager.getInstance()
            .getServiceConfig(ServiceType.PDA);
        }
        
        return pdaServiceConfig;
    }

    @Override
    public RetrievalAttribute<Time, Coverage> getAttribute() {
        return ra;
    }
   
}
