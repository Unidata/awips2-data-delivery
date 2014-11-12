package com.raytheon.uf.edex.datadelivery.harvester.pda;
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

import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.harvester.PDAAgent;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.PDARequestConnectionUtil;

/**
 * PDA Catalog Service Harvester
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * June 13, 2014 3120      dhladky     Initial creation
 * Nov 10, 2014  3826      dhladky     Added more logging.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */


public class PDACatalogHarvester {
    
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDACatalogHarvester.class);
    
    private HarvesterConfig hc = null;
    
    public PDACatalogHarvester() {
        
    }
    
    /**
     * launcher called constructor
     * @param configFile
     */
    public PDACatalogHarvester(HarvesterConfig hc) {
        this.hc = hc;
    }
  
    /**
     * Get the PDA config
     * @return
     */
    public HarvesterConfig getHc() {
        return hc;
    }
    
    /**
     * Call out to PDA and retrieve latest catalog
     * @return boolean
     */
    public boolean harvest() {
        
        boolean status = false;
        PDAAgent pda = (PDAAgent) getHc().getAgent();
        String catalogServerURL = pda.getCswURL();
        String responseHandlerURL = pda.getResponseHandler();
        String xml = null;

        try {
            PDACatalogRequestBuilder pcrb = new PDACatalogRequestBuilder(
                    responseHandlerURL);
            xml = pcrb.getXMLMessage();

            statusHandler.info("Sending request to Catalog Server: "
                    + catalogServerURL + "\n" + xml);

            String response = PDARequestConnectionUtil.connect(xml,
                    catalogServerURL);

            statusHandler.info("Catalog Server response: \n" + response);
            status = true;

        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Error requesting PDA Catalog: " + catalogServerURL + "\n "
                            + xml, e);
            status = false;
        }

        return status;
    }

}
