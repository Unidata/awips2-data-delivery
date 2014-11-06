package com.raytheon.uf.common.datadelivery.harvester;

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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Data Delivery PDA Agent, Agent used in PDA-Harvester.xml
 * to describe service parameters for the PDA.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 13 June, 2014 3120      dhladky     Initial creation
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class PDAAgent extends Agent {
    
    /**
     * URL of your service response handler
     */
    @XmlElement(name = "responseHandler", required = true)
    @DynamicSerializeElement
    private String responseHandler;
    
    /**
     * URL of CSW service
     */
    @XmlElement(name = "cswURL", required = true)
    @DynamicSerializeElement
    private String cswURL;
    
    /**
     * timer for kicking off requests
     */
    @XmlElement(name = "mainScan", required = true)
    @DynamicSerializeElement
    private String mainScan;
   
    public String getMainScan() {
        return mainScan;
    }

    public void setMainScan(String mainScan) {
        this.mainScan = mainScan;
    }

    public String getResponseHandler() {
        if (responseHandler != null && responseHandler.startsWith(externalAddressPattern)){
            responseHandler = responseHandler.replace(externalAddressPattern, externalAddress);
        }
        return responseHandler;
    }

    public void setResponseHandler(String responseHandler) {
        if (responseHandler != null && responseHandler.startsWith(externalAddressPattern)){
            responseHandler = responseHandler.replace(externalAddressPattern, externalAddress);
        }
        this.responseHandler = responseHandler;
    }

    public String getCswURL() {
        return cswURL;
    }

    public void setCswURL(String cswURL) {
        this.cswURL = cswURL;
    }
   
}
