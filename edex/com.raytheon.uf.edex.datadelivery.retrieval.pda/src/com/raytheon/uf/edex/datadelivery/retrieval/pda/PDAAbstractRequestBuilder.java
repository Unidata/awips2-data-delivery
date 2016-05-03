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

import javax.xml.bind.JAXBException;

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.ogc.common.jaxb.OgcJaxbManager;

/**
 * 
 * PDA abstract requestBuilder. Builds the geographic (Bounding Box) delimited
 * requests for PDA data. Then executes the request to PDA.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------
 * Apr 12, 2016  5424     dhladky   created.
 * Apr 21, 2016  5424     dhladky   Fixes from initial testing.
 * May 03, 2016  5599     tjensen   Added subscription name to PDA requests
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public abstract class PDAAbstractRequestBuilder extends PDARequestBuilder {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDAAbstractRequestBuilder.class);

    protected String metaDataID = null;

    protected OgcJaxbManager manager = null;

    public static final String CRS = getServiceConfig().getConstantValue(
            "DEFAULT_CRS");

    public static final String BLANK = getServiceConfig().getConstantValue(
            "BLANK");

    protected static final String SOAP_ACTION = "urn:getCoverage";

    /** SOAP HEADER for request **/
    protected static final String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "<soapenv:Header/>\n"
            + "<soapenv:Body>\n"
            + "<wcs:GetCoverage xmlns:wcs=\"http://www.opengis.net/wcs/2.0\" service=\"WCS\" version=\"2.0.1\" \n"
            + "xmlns:gml=\"http://www.opengis.net/gml\" \n"
            + "xmlns:ogc=\"http://www.opengis.net/ogc\" \n"
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
            + "xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 http://schemas.opengis.net/wcs/2.0/wcsAll.xsd\">\n";

    /** SOAP FOOTER for request **/
    protected static final String footer = "</wcs:GetCoverage>\n"
            + "</soapenv:Body>\n" + "</soapenv:Envelope>\n";

    /**
     * RetrievalGenerator used constructor
     * 
     * @param ra
     * @param metaDataID
     */
    protected PDAAbstractRequestBuilder(RetrievalAttribute<Time, Coverage> ra,
            String subName, String metaDataID) {

        super(ra, subName);
        setMetaDataID(metaDataID);
    }

    /**
     * Performs the request and returns response.
     * 
     * @return
     */
    protected String performRequest() {

        String response = null;

        if (subsetRequestURL == null) {
            subsetRequestURL = getServiceConfig().getConstantValue(
                    "SUBSET_REQUEST_URL");
        }

        try {
            // Gets an XML string response from the PDA server
            response = PDARequestConnectionUtil.connect(getRequest(),
                    SOAP_ACTION, subsetRequestURL);
            response = stripSoapHeaders(response);
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Request for datset failed! URL: " + subsetRequestURL
                            + "\n XML:  " + getRequest(), e);
        }

        return processResponse(response);
    }

    /**
     * Get Key to the PDA parent dataSet
     * 
     * @return
     */
    protected String getMetaDataID() {

        return metaDataID;
    }

    /**
     * Set key to PDA parent dataSet
     * 
     * @param metaDataID
     */
    protected void setMetaDataID(String metaDataID) {

        /*
         * PDA sends metadata ID's without the "C" but then requires the C for
         * queries. They should really just add the "C" to all of their metadata
         * ID's.
         */
        if (!metaDataID.startsWith("C")) {
            metaDataID = "C" + metaDataID;
            statusHandler.handle(Priority.INFO, "Added 'C' to metaDataID! "
                    + metaDataID);
        }

        this.metaDataID = metaDataID;
    }

    /**
     * Check to see if this coverage is subsetted
     * 
     * @param coverage
     * @return
     */
    protected boolean isSubsetted(Coverage coverage) {

        ReferencedEnvelope requestEnv = coverage.getRequestEnvelope();
        ReferencedEnvelope fullEnv = coverage.getEnvelope();

        if (!fullEnv.equals(requestEnv)) {
            return true;
        }

        return false;
    }

    /**
     * Add metaDataID to query
     * 
     * @return
     */
    protected String processMetaDataID() {

        StringBuilder sb = new StringBuilder(128);
        sb.append("<wcs:CoverageId>");
        sb.append(getMetaDataID());
        sb.append("</wcs:CoverageId>\n");

        return sb.toString();
    }

    /**
     * Strip un-needed SOAP headers
     * 
     * @param response
     * @return
     */
    protected String stripSoapHeaders(String response) {

        if (response != null) {
            // temporary code, remove the soap tags, we don't want them.
            if (response
                    .contains("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">")) {
                response = response
                        .replaceAll(
                                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">",
                                "");
            }

            if (response.contains("<soapenv:Body>")) {
                response = response.replaceAll("<soapenv:Body>", "");
            }

            if (response.contains("</soapenv:Body>")) {
                response = response.replaceAll("</soapenv:Body>", "");
            }

            if (response.contains("</soapenv:Envelope>")) {
                response = response.replaceAll("</soapenv:Envelope>", "");
            }
            statusHandler.handle(Priority.INFO,
                    "Stripped <soapenv> tags from PDA response object: "
                            + response);
        }

        return response;
    }

    /**
     * Get the manager for JAXB work
     * 
     * @return
     */
    protected OgcJaxbManager getManager() {

        if (manager == null) {
            try {
                configureJAXBManager();
            } catch (JAXBException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to create the OGC JAXBManager.", e);
            }
        }

        return manager;
    }

    /**
     * Configure need classes
     * 
     * Create JAXB manager
     * 
     * @throws JAXBException
     */
    protected abstract void configureJAXBManager() throws JAXBException;

    /**
     * Process the response from PDA
     * 
     * @param response
     * @return
     */
    protected abstract String processResponse(String response);

}
