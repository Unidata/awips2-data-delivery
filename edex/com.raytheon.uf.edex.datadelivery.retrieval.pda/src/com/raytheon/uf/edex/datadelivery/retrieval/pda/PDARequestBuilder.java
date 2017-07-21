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
import com.raytheon.uf.common.datadelivery.registry.EnvelopeUtils;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecordPK;
import com.raytheon.uf.edex.datadelivery.retrieval.request.RequestBuilder;
import com.raytheon.uf.edex.ogc.common.jaxb.OgcJaxbManager;
import com.vividsolutions.jts.geom.Coordinate;

/**
 *
 * PDA Request Builder.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 12, 2014  3120     dhladky   created.
 * Sep 04, 2014  3121     dhladky   Clarified and sharpened creation, largely
 *                                  un-implemented at this point.
 * Sep 27, 2014  3127     dhladky   Geographic subsetting.
 * Apr 06, 2016  5424     dhladky   Dual ASYNC and SYNC processing.
 * May 03, 2016  5599     tjensen   Added subscription name to PDA requests
 * Jul 20, 2017  6130     tjensen   Consolidate PDAAbstractRequestBuilder and
 *                                  PDAAsyncRequest into PDARequestBuilder
 *
 * </pre>
 *
 * @author dhladky
 * @version 1.0
 */

public class PDARequestBuilder extends RequestBuilder<Time, Coverage> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDARequestBuilder.class);

    protected static volatile ServiceConfig pdaServiceConfig = null;

    protected static volatile String subsetRequestURL = null;

    protected static String EXCEPTION_REPORT_COMPARE_PREFIX = "Coverage Response will be sent to ";

    /** ASYNC response handler return address **/
    protected static String RESPONSE_HANDLER_ADDRESS = null;

    /** ASYNC exception report comparison string **/
    protected static String EXCEPTION_REPORT_COMPARE = null;

    protected String request = null;

    protected String subName = null;

    private OgcJaxbManager manager = null;

    private RetrievalRequestRecordPK retrievalID = null;

    protected String metaDataID = null;

    protected static final String DIMENSION_LON = "Long";

    protected static final String DIMENSION_LAT = "Lat";

    protected static final String DIMENSION_TRIM_OPEN = "<wcs:DimensionTrim>";

    protected static final String DIMENSION_OPEN = "<wcs:Dimension>";

    protected static final String DIMENSION_TRIM_CLOSE = "</wcs:DimensionTrim>";

    protected static final String DIMENSION_CLOSE = "</wcs:Dimension>";

    protected static final String TRIM_HIGH_OPEN = "<wcs:TrimHigh>";

    protected static final String TRIM_HIGH_CLOSE = "</wcs:TrimHigh>";

    protected static final String TRIM_LOW_OPEN = "<wcs:TrimLow>";

    protected static final String TRIM_LOW_CLOSE = "</wcs:TrimLow>";

    public static final String CRS = getServiceConfig()
            .getConstantValue("DEFAULT_CRS");

    public static final String BLANK = getServiceConfig()
            .getConstantValue("BLANK");

    protected static final String SOAP_ACTION = "urn:getCoverage";

    /** Constant name **/
    protected static final String ASYNC_RESPONSE_HANDLER = "ASYNC_RESPONSE_HANDLER";

    /** SOAP HEADER for request **/
    protected static final String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "<soapenv:Header/>\n" + "<soapenv:Body>\n"
            + "<wcs:GetCoverage xmlns:wcs=\"http://www.opengis.net/wcs/2.0\" service=\"WCS\" version=\"2.0.1\" \n"
            + "xmlns:gml=\"http://www.opengis.net/gml\" \n"
            + "xmlns:ogc=\"http://www.opengis.net/ogc\" \n"
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
            + "xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 http://schemas.opengis.net/wcs/2.0/wcsAll.xsd\">\n";

    /** SOAP FOOTER for request **/
    protected static final String footer = "</wcs:GetCoverage>\n"
            + "</soapenv:Body>\n" + "</soapenv:Envelope>\n";
    static {
        if (getServiceConfig()
                .getConstantValue(ASYNC_RESPONSE_HANDLER) != null) {
            RESPONSE_HANDLER_ADDRESS = getServiceConfig()
                    .getConstantValue(ASYNC_RESPONSE_HANDLER);
            EXCEPTION_REPORT_COMPARE = EXCEPTION_REPORT_COMPARE_PREFIX
                    + RESPONSE_HANDLER_ADDRESS;
            statusHandler.info("Async retrievals will be processed at: "
                    + RESPONSE_HANDLER_ADDRESS);
        } else {
            throw new IllegalArgumentException(
                    "ASYNC_RESPONSE_HANDLER must be set in the PDAServiceConfig.xml for ASYNC requests to work.");
        }
    }

    /**
     * ASYNC request
     *
     * @param ra
     * @param metaDataID
     * @param retrievalID
     */
    public PDARequestBuilder(RetrievalAttribute<Time, Coverage> ra,
            String subName, String metaDataID,
            RetrievalRequestRecordPK retrievalId) {
        super(ra);
        this.subName = subName;
        setMetaDataID(metaDataID);

        setRetrievalID(retrievalId);
        // create the request
        StringBuilder query = new StringBuilder(512);
        query.append(header);
        query.append(getExtension());
        query.append(processMetaDataID());
        if (isSubsetted(ra.getCoverage())) {
            query.append(processCoverage(ra.getCoverage()));
        }
        query.append(footer);
        // set the request
        setRequest(query.toString());
    }

    /**
     * Retrieval Adapter constructor
     *
     * @param ra
     */
    protected PDARequestBuilder(RetrievalAttribute<Time, Coverage> ra,
            String subName) {
        super(ra);
        this.subName = subName;
    }

    @Override
    public String processTime(Time prtXML) {
        throw new UnsupportedOperationException("Not implemented for PDA!");
    }

    @Override
    public String getRequest() {
        // There are no switches for full data set PDA.
        return request;
    }

    /**
     * Sets the request string
     *
     * @param request
     */
    public void setRequest(String request) {
        this.request = request;
    }

    /**
     * Get Subscription name
     *
     * @return
     */
    public String getSubName() {
        return subName;
    }

    /**
     * Sets Subscription name
     *
     * @param subName
     *            Name of the subscription
     */
    public void setSubName(String subName) {
        this.subName = subName;
    }

    /**
     * Get the instance of the service config
     *
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

    /**
     * Adds the coverage for the subset
     *
     * @param Coverage
     */
    @Override
    public String processCoverage(Coverage coverage) {

        StringBuilder sb = new StringBuilder(256);

        if (coverage != null) {
            try {
                ReferencedEnvelope re = coverage.getRequestEnvelope();
                Coordinate ll = EnvelopeUtils.getLowerLeftLatLon(re);
                Coordinate ur = EnvelopeUtils.getUpperRightLatLon(re);
                // manage the box
                double lowerLon = ll.x;
                double lowerLat = ll.y;
                double upperLon = ur.x;
                double upperLat = ur.y;

                // longitude dimension
                sb.append(DIMENSION_TRIM_OPEN);
                sb.append(NEW_LINE);
                sb.append(DIMENSION_OPEN);
                sb.append(DIMENSION_LON);
                sb.append(DIMENSION_CLOSE);
                sb.append(NEW_LINE);
                // longitude low
                sb.append(TRIM_LOW_OPEN);
                sb.append(lowerLon);
                sb.append(TRIM_LOW_CLOSE);
                sb.append(NEW_LINE);
                // longitude high
                sb.append(TRIM_HIGH_OPEN);
                sb.append(upperLon);
                sb.append(TRIM_HIGH_CLOSE);
                sb.append(NEW_LINE);
                sb.append(DIMENSION_TRIM_CLOSE);
                sb.append(NEW_LINE);

                // latitude dimension
                sb.append(DIMENSION_TRIM_OPEN);
                sb.append(NEW_LINE);
                sb.append(DIMENSION_OPEN);
                sb.append(DIMENSION_LAT);
                sb.append(DIMENSION_CLOSE);
                sb.append(NEW_LINE);
                // latitude low
                sb.append(TRIM_LOW_OPEN);
                sb.append(lowerLat);
                sb.append(TRIM_LOW_CLOSE);
                sb.append(NEW_LINE);
                // latitude high
                sb.append(TRIM_HIGH_OPEN);
                sb.append(upperLat);
                sb.append(TRIM_HIGH_CLOSE);
                sb.append(NEW_LINE);
                sb.append(DIMENSION_TRIM_CLOSE);
                sb.append(NEW_LINE);

            } catch (Exception e) {
                statusHandler.error("Couldn't parse Coverage object.", e);
            }
        }

        return sb.toString();
    }

    /**
     * Performs the request and returns response.
     *
     * @return
     */
    protected String performRequest() {

        String response = null;

        if (subsetRequestURL == null) {
            subsetRequestURL = getServiceConfig()
                    .getConstantValue("SUBSET_REQUEST_URL");
        }

        try {
            // Gets an XML string response from the PDA server
            response = PDARequestConnectionUtil.connect(getRequest(),
                    SOAP_ACTION, subsetRequestURL);
            response = stripSoapHeaders(response);
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Request for datset failed! URL: " + subsetRequestURL
                            + "\n XML:  " + getRequest(),
                    e);
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
            statusHandler.handle(Priority.INFO,
                    "Added 'C' to metaDataID! " + metaDataID);
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
            if (response.contains(
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">")) {
                response = response.replaceAll(
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
     * Processes the response from PDA, asynchronous.
     *
     * @param response
     * @return
     */
    public String processResponse(String response) {
        String report = null;

        if (response.contains(EXCEPTION_REPORT_COMPARE)) {
            statusHandler.info("Successful asynchronous request: " + response);
            report = response;

        } else {
            statusHandler
                    .error("PDA asynchronous request has failed. " + response);
        }

        return report;
    }

    /**
     * Use correct OGC libraries for casting.
     *
     * @throws JAXBException
     */
    public void configureJAXBManager() throws JAXBException {
        if (manager == null) {
            Class<?>[] classes = new Class<?>[] {
                    net.opengis.filter.v_1_1_0.ObjectFactory.class,
                    net.opengis.wcs.v_1_1_2.ObjectFactory.class,
                    net.opengis.gmlcov.v_1_0.ObjectFactory.class,
                    net.opengis.sensorml.v_2_0.ObjectFactory.class,
                    net.opengis.swecommon.v_2_0.ObjectFactory.class,
                    net.opengis.ows.v_2_0.ObjectFactory.class };

            try {
                manager = new OgcJaxbManager(classes);
            } catch (JAXBException e1) {
                statusHandler.handle(Priority.PROBLEM,
                        "Construction of OGC JaxbManager failed!", e1);
            }
        }
    }

    private RetrievalRequestRecordPK getRetrievalID() {
        return retrievalID;
    }

    protected void setRetrievalID(RetrievalRequestRecordPK retrievalID) {
        this.retrievalID = retrievalID;
    }

    /**
     * Gets the retreivalID and response handler for the retrieval
     *
     * @return
     */
    protected String getExtension() {

        StringBuilder sb = new StringBuilder(256);
        sb.append("<wcs:Extension>\n");
        sb.append(
                "<ows:ExtendedCapabilities xmlns:ows=\"http://www.opengis.net/ows\" responseHandler=\""
                        + RESPONSE_HANDLER_ADDRESS + "\">\n");
        sb.append("<ows:Identifier codeSpace=\"locator\">" + getRetrievalID()
                + "</ows:Identifier>\n");
        sb.append("</ows:ExtendedCapabilities>\n");
        sb.append("</wcs:Extension>\n");

        return sb.toString();
    }

}
