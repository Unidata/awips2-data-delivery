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

/**
 * PDA CSW Request builder
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * June 20, 2014 3120      dhladky     Initial creation
 * July 28, 2015 4881      dhladky     Tweaks done to conform to PDA web service.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDACatalogRequestBuilder {
    
    private String responseURL = null;
    
    private StringBuilder sb = null;
    
    public PDACatalogRequestBuilder(String responseURL) {
        this.responseURL = responseURL;
        this.sb = new StringBuilder();
    }
    
    /**
     * Create the CSW XML content
     * @return
     */
    private void createContent() {

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<csw:GetRecords xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" service=\"CSW\" version=\"2.0.2\">\n");
        sb.append("<csw:ResponseHandler>"+ responseURL +"</csw:ResponseHandler>\n");
        sb.append("<csw:Query typeNames=\"csw:Record\">\n");
        sb.append("<csw:ElementSetName typeNames=\"csw:Record\">brief</csw:ElementSetName>\n");
        sb.append("</csw:Query>\n");
        sb.append("</csw:GetRecords>\n");
    }
     
    /**
     * Creates the CSW XML Query
     * @return
     */
    public String getXMLMessage() {
        createContent();
        return sb.toString();
    }

}
