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

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.ogc.common.jaxb.OgcJaxbManager;

/**
 * 
 * PDA synchronous subset Request Builder / Executor. Builds the geographic
 * (Bounding Box) delimited requests for PDA data. Then executes the request to
 * PDA receiving the URL for the subsetted data.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 27, 2014  3127     dhladky   created.
 * Nov 15, 2015  5139     dhladky   PDA changed interface, requires a SOAP
 *                                  header.
 * Jan 16, 2016  5260     dhladky   Fixes to errors found in testing.
 * Apr 06, 2016  5424     dhladky   Moved to sync request. Pulled common
 *                                  portions out to super class.
 * May 03, 2016  5599     tjensen   Added subscription name to PDA requests
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDASyncRequest extends PDAAbstractRequestBuilder {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDASyncRequest.class);

    private static final String extensionHeader = "<wcs:Extension>\n"
            + "<ows:ExtendedCapabilities xmlns:ows=\"http://www.opengis.net/ows\">store</ows:ExtendedCapabilities>\n"
            + "</wcs:Extension>\n";

    /**
     * SYNC request
     * 
     * @param ra
     * @param metaDataID
     */
    public PDASyncRequest(RetrievalAttribute<Time, Coverage> ra,
            String subName, String metaDataID) {

        super(ra, subName, metaDataID);
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
     * Processes the response from PDA, synchronous.
     * 
     * @param response
     * @return
     */
    public String processResponse(String response) {

        String filePath = null;

        try {

            net.opengis.gmlcov.v_1_0.AbstractDiscreteCoverageType coverage = null;
            Object responseObject = getManager().unmarshalFromXml(response);

            if (responseObject instanceof net.opengis.gmlcov.v_1_0.AbstractDiscreteCoverageType) {
                coverage = (net.opengis.gmlcov.v_1_0.AbstractDiscreteCoverageType) responseObject;
                // the FTPS URL for the dataset
                filePath = coverage.getRangeSet().getFile().getFileReference();
            } else {
                throw new Exception("Unexpected response object: "
                        + responseObject.toString());
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
                    net.opengis.swecommon.v_2_0.ObjectFactory.class };

            try {
                manager = new OgcJaxbManager(classes);
            } catch (JAXBException e1) {
                statusHandler.handle(Priority.PROBLEM,
                        "Construction of OGC JaxbManager failed!", e1);
            }
        }
    }
}
