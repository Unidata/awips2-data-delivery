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
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
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
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDARequestBuilder extends RequestBuilder<Time,Coverage> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDARequestBuilder.class);
    
    protected static ServiceConfig pdaServiceConfig;
    
    private String request = null;
    
    protected PDARequestBuilder(RetrievalAttribute<Time,Coverage> ra) {

        super(ra);
    }

    @Override
    public String processTime(Time prtXML) {

        // Currently in PDA, Nothing to process
        return null;
    }

    @Override
    public String processCoverage(Coverage Coverage) {
        // Currently in PDA, Nothing to process
        return null;
    }

    @Override
    public String getRequest() {

        //TODO Since there exists no ability to subset by time or by coverage.
        // the only thing that differentiates a PDA dataset is the parameter.
        // So, at this point all the request is is a essentially the FTP(S) 
        // filepath that comes with the BriefRecord. This is totally lame.
        
        // Someday...In the near future, this would be anticipated to be a 
        // WCS request that might return an FTP path or even better, the actual 
        // data.  For now it's just an FTP filepath with no switches at all.
        
        return request;
    }
    
    /**
     * Sets the request string
     * This is temporary, until PDA becomes a real service
     * with actual switches for coverage, time, etc
     * @param request
     */
    public void setRequest(String request) {
        this.request = request;
    }

    /**
     * Get the instance of the service config
     * @return
     */
    private static ServiceConfig getServiceConfig() {
        
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
