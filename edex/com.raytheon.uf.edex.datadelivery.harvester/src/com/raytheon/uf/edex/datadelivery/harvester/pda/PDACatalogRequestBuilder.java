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
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDACatalogRequestBuilder {
    
    private String responseURL = null;
    
    private StringBuilder sb = null;
    
    private static String header = createHeader();
    
    private static String footer = createFooter();
    
    public PDACatalogRequestBuilder(String responseURL) {
        this.responseURL = responseURL;
        this.sb = new StringBuilder();
    }
    
    /**
     * Creates the WFS XML Query header
     * @return
     */
    private static String createHeader() {
         StringBuilder sb1 = new StringBuilder();
         sb1.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
         sb1.append("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n");
         sb1.append("<SOAP-ENV:Header/>\n");
         sb1.append("<SOAP-ENV:Body>\n");
         
        return sb1.toString();
    }
    
    /**
     * Create the CSW XML content
     * @return
     */
    private void createContent() {

        sb.append("<csw:GetRecords xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" service=\"CSW\" version=\"2.0.2\">\n");
        sb.append("<csw:Query typeNames=\"csw:Record\"/>\n");
        sb.append("<ResponseHandler>"+ responseURL +"</ResponseHandler>\n");
        sb.append("</csw:GetRecords>\n");
    }
    
    /**
     * Creates the CSW XML Query Footer
     * @return
     */
    private static String createFooter() {
        
        StringBuilder sb2 = new StringBuilder(128);
        sb2.append("</SOAP-ENV:Body>\n");
        sb2.append("</SOAP-ENV:Envelope>\n");
        
        return sb2.toString();
    }
    
    /**
     * Creates the CSW XML Query
     * @return
     */
    public String getXMLMessage() {

        sb.append(header);
        createContent();
        sb.append(footer);
        
        return sb.toString();
    }

}
