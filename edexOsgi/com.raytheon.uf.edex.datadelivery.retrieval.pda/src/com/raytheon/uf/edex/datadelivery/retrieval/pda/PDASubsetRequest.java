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

import net.opengis.gml.v_3_1_1.GridCoverageType;

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
    
    private String metaDataID = null;
    
    /** SOAP HEADER for request **/
    private static final String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "<soapenv:Header/>\n"
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
    
    protected PDASubsetRequest(RetrievalAttribute<Time, Coverage> ra, String metaDataID) {
        super(ra);
        setMetaDataID(metaDataID);
        // create the request
        StringBuilder query = new StringBuilder(512);
        query.append(header);
        query.append(processMetaDataID());
        query.append(processCoverage(ra.getCoverage()));
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
        
        if (coverage != null) {
            try {
                StringBuilder sb = new StringBuilder(256);
                ReferencedEnvelope re = coverage.getRequestEnvelope();
                Coordinate ll = EnvelopeUtils.getLowerLeftLatLon(re);
                Coordinate ur = EnvelopeUtils.getUpperRightLatLon(re);
                // manage the box
                double lowerLon = ll.x;
                double lowerLat = ll.y;
                double upperLon = ur.x;
                double upperLat = ur.y;

                sb.append(WITHIN_OPEN).append(NEW_LINE);
                sb.append(PROPERTTY_OPEN).append(getMetaDataID())
                        .append(PROPERTTY_CLOSE).append(NEW_LINE);
                sb.append(ENVELOPE_OPEN).append(" srsName=\"").append(CRS)
                        .append("\">").append(NEW_LINE);
                sb.append(LOWER_CORNER_OPEN);
                sb.append(lowerLon);
                sb.append(SPACE);
                sb.append(lowerLat);
                sb.append(LOWER_CORNER_CLOSE).append(NEW_LINE);
                sb.append(UPPER_CORNER_OPEN);
                sb.append(upperLon);
                sb.append(SPACE);
                sb.append(upperLat);
                sb.append(UPPER_CORNER_CLOSE).append(NEW_LINE);
                sb.append(ENVELOPE_CLOSE).append(NEW_LINE);
                sb.append(WITHIN_CLOSE).append(NEW_LINE);

                return sb.toString();

            } catch (Exception e) {
                statusHandler.error("Couldn't parse Coverage object.", e);
            }
        }
        
        return BLANK;
    }
 
    /**
     * Makes a geographic subset request to PDA
     * @param request
     * @return
     */
    public String performSubsetRequest() {
       
        String subsetURL = null;
        
        if (subsetRequestURL == null) {
            subsetRequestURL = getServiceConfig().getConstantValue("SUBSET_REQUEST_URL");
        }

        try {
            // Gets an XML string response from the PDA server
            String response = PDARequestConnectionUtil.connect(
                    this.getRequest(), subsetRequestURL);
            GridCoverageType coverage = (GridCoverageType) getManager()
                    .unmarshalFromXml(response);
            // the FTPS URL for the subset
            subsetURL = coverage.getRangeSet().getFile().getFileName();
        } catch (JAXBException e) {
            statusHandler.handle(Priority.ERROR,
                    "Request for subset failed! URL: " + subsetRequestURL
                            + "\n XML:  " + this.getRequest(), e);
        }

        return subsetURL;
    }
    
    /**
     * Use correct OGC libraries for casting.
     * 
     * @throws JAXBException
     */
    public void configureJAXBManager() throws JAXBException {
        if (manager == null) {
            Class<?>[] classes = new Class<?>[] {
                    net.opengis.gml.v_3_2_1.ObjectFactory.class,
                    net.opengis.filter.v_1_1_0.ObjectFactory.class,
                    net.opengis.wcs.v_1_1_2.ObjectFactory.class };

            try {
                manager = new OgcJaxbManager(classes);
            } catch (JAXBException e1) {
                statusHandler.handle(Priority.PROBLEM,
                        e1.getLocalizedMessage(), e1);
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
}
