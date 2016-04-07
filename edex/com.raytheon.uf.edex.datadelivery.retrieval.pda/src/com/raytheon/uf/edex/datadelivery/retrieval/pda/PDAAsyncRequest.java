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

import net.opengis.ows.v_2_0.ExceptionReport;
import net.opengis.ows.v_2_0.ExceptionType;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.ogc.common.jaxb.OgcJaxbManager;

/**
 * 
 * PDA asynchronous subset Request Builder / Executor.
 * Builds the geographic (Bounding Box) delimited
 * requests for PDA data. Then executes the request to PDA
 * receiving a message waiting for the async response.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 06, 2016 5424        dhladky      created.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */


public class PDAAsyncRequest extends PDAAbstractRequestBuilder {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDAAsyncRequest.class);

    private static String EXCEPTION_REPORT_COMPARE_PREFIX = "Coverage Response will be sent to ";

    private OgcJaxbManager manager = null;

    private String retrievalID = null;

    /** Constant name **/
    private static final String ASYNC_RESPONSE_HANDLER = "ASYNC_RESPONSE_HANDLER";

    /** ASYNC response handler return address **/
    private static String RESPONSE_HANDLER_ADDRESS = null;

    /** ASYNC exception report comparison string **/
    private static String EXCEPTION_REPORT_COMPARE = null;

    static {
        if (getServiceConfig().getConstantValue(ASYNC_RESPONSE_HANDLER) != null) {
            RESPONSE_HANDLER_ADDRESS = getServiceConfig().getConstantValue(
                    ASYNC_RESPONSE_HANDLER);
            EXCEPTION_REPORT_COMPARE = EXCEPTION_REPORT_COMPARE_PREFIX
                    + RESPONSE_HANDLER_ADDRESS;
        } else {
            throw new IllegalArgumentException(
                    "ASYNC_RESPONSE_HANDLER must be set in the PDAServiceConfig.xml for ASYNC requests to work.");
        }
    }

    /**
     * ASYNC request
     * @param ra
     * @param metaDataID
     * @param retrievalID
     */
    public PDAAsyncRequest(RetrievalAttribute<Time, Coverage> ra,
            String metaDataID, String retrievalID) {
        super(ra, metaDataID);

        setRetrievalID(retrievalID);
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
     * Processes the response from PDA, asynchronous.
     * @param response
     * @return
     */
    public String processResponse(String response) {
       
        String report = null;

        try {
            // Gets an XML string response from the PDA server
            Object responseObject = getManager().unmarshalFromXml(response);

            if (responseObject instanceof ExceptionReport) {
                ExceptionReport exceptionReport = (ExceptionReport) responseObject;

                // Determine if it's a good or a bad exception
                for (ExceptionType exception : ((ExceptionReport) exceptionReport)
                        .getException()) {
                    for (String r : exception.getExceptionText()) {
                        if (PDAAsyncRequest.EXCEPTION_REPORT_COMPARE.equals(r)) {
                            statusHandler
                                    .info("Successful asynchronous request: "
                                            + r);
                            report = r;
                            break;
                        } else {
                            statusHandler.error("PDA asynchronous request has failed. " +r);
                        }
                    }
                }

            } else {
                throw new Exception("Unexpected response object: "+responseObject.toString());
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Request for datset failed! URL: " + subsetRequestURL
                            + "\n XML:  " + this.getRequest(), e);
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
                    net.opengis.ows.v_2_0.ObjectFactory.class};

            try {
                manager = new OgcJaxbManager(classes);
            } catch (JAXBException e1) {
                statusHandler.handle(Priority.PROBLEM,
                        "Construction of OGC JaxbManager failed!", e1);
            }
        }
    }

    private String getRetrievalID() {
        return retrievalID;
    }

    private void setRetrievalID(String retrievalID) {
        this.retrievalID = retrievalID;
    }
    
    /**
     * Gets the retreivalID and response handler for the retrieval
     * @return
     */
    private String getExtension() {
        
        StringBuilder sb = new StringBuilder(256);
        sb.append("<wcs:Extension>\n");
        sb.append("<ows:ExtendedCapabilities xmlns:ows=\"http://www.opengis.net/ows\" responseHandler=\""+RESPONSE_HANDLER_ADDRESS+"\">\n");
        sb.append("<ows:Identifier codeSpace=\"locator\">"+getRetrievalID()+"</ows:Identifier>\n");
        sb.append("</ows:ExtendedCapabilities>\n");
        sb.append("</wcs:Extension>\n");
        
        return sb.toString();
    }
}
