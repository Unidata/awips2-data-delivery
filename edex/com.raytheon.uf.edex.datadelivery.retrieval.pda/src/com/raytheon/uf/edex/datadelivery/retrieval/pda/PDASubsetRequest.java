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
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.ogc.common.jaxb.OgcJaxbManager;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * 
 * PDA subset Request Builder / Executor.
 * Builds the geographic (Bounding Box) delimited
 * requests for PDA data. Then executes the request to PDA
 * receiving the URL for the subsetted data.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sept 27, 2014 3127        dhladky      created.
 * Nov 15, 2015   5139       dhladky     PDA changed interface, requires a SOAP header.
 * Jan 16, 2016   5260       dhladky     Fixes to errors found in testing.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */


public class PDASubsetRequest extends PDARequestBuilder {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDASubsetRequest.class);
    
    private static String subsetRequestURL = null;
    
    private OgcJaxbManager manager = null;
    
    private static final String SOAP_ACTION = "urn:getCoverage";

    private String metaDataID = null;
    
    private static final String DIMENSION_LON = "Long";
    
    private static final String DIMENSION_LAT = "Lat";
    
    private static final String DIMENSION_TRIM_OPEN = "<wcs:DimensionTrim>";
    
    private static final String DIMENSION_OPEN = "<wcs:Dimension>";
    
    private static final String DIMENSION_TRIM_CLOSE = "</wcs:DimensionTrim>";
    
    private static final String DIMENSION_CLOSE = "</wcs:Dimension>";
    
    private static final String TRIM_HIGH_OPEN = "<wcs:TrimHigh>";
    
    private static final String TRIM_HIGH_CLOSE = "</wcs:TrimHigh>";
    
    private static final String TRIM_LOW_OPEN = "<wcs:TrimLow>";
    
    private static final String TRIM_LOW_CLOSE = "</wcs:TrimLow>";
    
    /** SOAP HEADER for request **/
    private static final String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "<soapenv:Header/>\n"
            + "<soapenv:Body>\n"
            + "<wcs:GetCoverage xmlns:wcs=\"http://www.opengis.net/wcs/2.0\" service=\"WCS\" version=\"2.0.1\" \n"
            + "xmlns:gml=\"http://www.opengis.net/gml\" \n"
            + "xmlns:ogc=\"http://www.opengis.net/ogc\" \n"
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n";
    
    /** SOAP FOOTER for request **/
    private static final String footer = "</wcs:GetCoverage>\n"
            + "</soapenv:Body>\n" 
            + "</soapenv:Envelope>\n";
    
    private static final String extensionHeader =  "<wcs:Extension>\n" 
                                                 + "<ows:ExtendedCapabilities xmlns:ows=\"http://www.opengis.net/ows\">store</ows:ExtendedCapabilities>\n"
                                                 + "</wcs:Extension>\n";
    
    protected PDASubsetRequest(RetrievalAttribute<Time, Coverage> ra, String metaDataID) {
        super(ra);
        
        // PDA sends metadata ID's without the "C" but then requires the C for queries.
        // They should really just add the "C" to all of their metadata ID's.
        if (!metaDataID.startsWith("C")) {
            metaDataID = "C"+metaDataID;
            statusHandler.handle(Priority.INFO, "Added 'C' to metaDataID! "+ metaDataID);
        } 
        
        setMetaDataID(metaDataID);
        // create the request
        StringBuilder query = new StringBuilder(512);
        query.append(header);
        query.append(extensionHeader);
        query.append(processMetaDataID());
        if (isSubsetted(ra.getCoverage())) {
            query.append(processCoverage(ra.getCoverage()));
        }
        query.append(footer);
        // set the request
        setRequest(query.toString());
    }
       
    /**
     * Add metaDataID to query
     * @return
     */
    private String processMetaDataID() {
        
        StringBuilder sb = new StringBuilder(128);
        sb.append("<wcs:CoverageId>");
        sb.append(getMetaDataID());
        sb.append("</wcs:CoverageId>\n");
        
        return sb.toString();
    }
    
    /**
     * Adds the coverage for the subset
     */
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
     * Makes a the dataset request to PDA
     * @param request
     * @return
     */
    public String performRequest() {
       
        String filePath = null;
        
        if (subsetRequestURL == null) {
            subsetRequestURL = getServiceConfig().getConstantValue("SUBSET_REQUEST_URL");
        }

        try {
            // Gets an XML string response from the PDA server
            String response = PDARequestConnectionUtil.connect(
                    this.getRequest(), SOAP_ACTION, subsetRequestURL);
            
            if (response != null) {
                // temporary code, remove the soap tags, we don't want them.
                if (response.contains("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">")) {
                    response = response.replaceAll("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">", "");
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
                        "Stripped <soapenv> tags from PDA response object: " + response);
            }
            
            net.opengis.gmlcov.v_1_0.AbstractDiscreteCoverageType coverage = null;
            Object responseObject = getManager().unmarshalFromXml(response);
            
            if (responseObject instanceof net.opengis.gmlcov.v_1_0.AbstractDiscreteCoverageType) {
                coverage = (net.opengis.gmlcov.v_1_0.AbstractDiscreteCoverageType) responseObject;
                // the FTPS URL for the dataset
                filePath = coverage.getRangeSet().getFile().getFileReference();
            } else {
                throw new Exception(responseObject.toString());
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Request for datset failed! URL: " + subsetRequestURL
                            + "\n XML:  " + this.getRequest(), e);
        }

        return filePath;
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
                    net.opengis.swecommon.v_2_0.ObjectFactory.class};

            try {
                manager = new OgcJaxbManager(classes);
            } catch (JAXBException e1) {
                statusHandler.handle(Priority.PROBLEM,
                        "Construction of OGC JaxbManager failed!", e1);
            }
        }
    }

    /**
     * Get the manager for JAXB work
     * 
     * @return
     */
    public OgcJaxbManager getManager() {
        
        if (manager == null) {
            try {
                configureJAXBManager();
            } catch (JAXBException e) {
                statusHandler.handle(Priority.ERROR, "Unable to configure the OGC manager!", e);
            }
        }
        
        return manager;
    }

    /**
     * Get Key to the PDA parent dataSet
     * @return
     */
    public String getMetaDataID() {
        return metaDataID;
    }

    /**
     * Set key to PDA parent dataSet
     * @param metaDataID
     */
    public void setMetaDataID(String metaDataID) {
        
        this.metaDataID = metaDataID;
    }
    
    /**
     * Check to see if this coverage is subsetted
     * @param coverage
     * @return
     */
    private boolean isSubsetted(Coverage coverage) {
        
        ReferencedEnvelope requestEnv = coverage.getRequestEnvelope();
        ReferencedEnvelope fullEnv = coverage.getEnvelope();
          
        if (!fullEnv.equals(requestEnv)) {
            return true;
        }
        
        return false;
    }

}
