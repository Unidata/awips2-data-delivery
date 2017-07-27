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
package com.raytheon.uf.edex.datadelivery.retrieval.pda;

import java.io.File;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlSeeAlso;

import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.retrieval.response.AsyncRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.util.RetrievalGeneratorUtilities;
import com.raytheon.uf.edex.ogc.common.jaxb.OgcJaxbManager;
import com.raytheon.uf.edex.ogc.common.soap.ServiceExceptionReport;

import net.opengis.ows.v_2_0.AbstractReferenceBaseType;
import net.opengis.ows.v_2_0.ManifestType;
import net.opengis.ows.v_2_0.ReferenceGroupType;

/**
 * PDA getCoverage Response Handler
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------------------
 * Mar 16, 2016  5424     dhladky   Initial creation
 * Apr 21, 2016  5424     dhladky   Fixes from initial testing.
 * May 06, 2016  5424     dhladky   Added work around for PDA timing issue.
 * Jul 27, 2017  6186     rjpeter   Updated id parsing
 *
 * </pre>
 *
 * @author dhladky
 */

@WebService(name = "GetCoverageResponseHandler", targetNamespace = "http://www.opengis.net/wcs/2.0")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({ net.opengis.ows.v_2_0.ObjectFactory.class })
public class GetCoverageResponseHandler
        implements IPDAGetCoverageResponseHandler {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GetCoverageResponseHandler.class);

    /** JAXB Manager **/
    private OgcJaxbManager jaxbManager = null;

    /** Async processor queue endpoint */
    private String destinationUri;

    /** OWS class factory **/
    private static final Class<?>[] classes = new Class<?>[] {
            net.opengis.ows.v_2_0.ObjectFactory.class };

    public GetCoverageResponseHandler() {

    }

    /**
     * spring constructor
     *
     * @param destinationUri
     * @param retrievalDao
     */
    public GetCoverageResponseHandler(String destinationUri) {
        this.destinationUri = destinationUri;
    }

    /**
     * Gets the JAXB manager for use with OGC decode
     *
     * @return
     */
    private OgcJaxbManager getJaxbManager() {

        if (jaxbManager == null) {
            try {
                this.jaxbManager = new OgcJaxbManager(classes);
            } catch (JAXBException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "JaxbManager failed to initialize, can not deserialize CSW classes.",
                        e);
            }
        }

        return jaxbManager;
    }

    @Override
    @WebMethod
    public void handleGetCoverageResponse(
            @WebParam(name = "OperationResponse", targetNamespace = "http://www.opengis.net/ows/2.0", partName = "Body") net.opengis.ows.v_2_0.ManifestType manifestType)
            throws ServiceExceptionReport {

        String retrievalID = null;
        String fileLink = null;

        List<ReferenceGroupType> groups = manifestType.getReferenceGroup();

        if (groups != null) {
            // there is only one type/group
            for (ReferenceGroupType type : groups) {
                List<JAXBElement<? extends AbstractReferenceBaseType>> ref = type
                        .getAbstractReferenceBase();

                if (ref != null && ref.size() >= 2) {

                    JAXBElement<? extends AbstractReferenceBaseType> retrieval = ref
                            .get(0);
                    JAXBElement<? extends AbstractReferenceBaseType> fileName = ref
                            .get(1);
                    retrievalID = retrieval.getValue().getHref();
                    fileLink = fileName.getValue().getHref();
                    statusHandler
                            .info("Received async retrievalID: " + retrievalID
                                    + " file link: " + fileLink + " from PDA.");
                } else {
                    throw new IllegalArgumentException(
                            "AbstractReferenceBaseType list is improperly formatted: "
                                    + manifestType.toString());
                }
                // there will only be one.
                break;
            }
        } else {
            throw new IllegalArgumentException(
                    "ReferenceGroupType Group object is improperly formatted: "
                            + manifestType.toString());
        }

        // send to retrieval
        if (retrievalID != null) {
            AsyncRetrievalResponse ars = new AsyncRetrievalResponse();
            ars.setRequestId(retrievalID);
            ars.setFileName(fileLink);

            if (ars != null) {
                try {
                    RetrievalGeneratorUtilities
                            .sendToAsyncRetrieval(destinationUri, ars);
                } catch (Exception e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Couldn't send RetrievalRecords to Async Queue!",
                            e);
                }
            }

        } else {
            statusHandler
                    .warn("Did not recieve a valid retrievalID from PDA. Response ["
                            + manifestType + "]");
        }
    }

    /**
     * Test a sample response message, must strip off SOAP headers if exist.
     *
     * @param args
     */
    public static void main(String[] args) {

        String fileName = args[0];

        File xmlFile = new File(fileName);

        GetCoverageResponseHandler gcrh = new GetCoverageResponseHandler();
        try {
            ManifestType manifestType = gcrh.getJaxbManager()
                    .unmarshalFromXmlFile(
                            net.opengis.ows.v_2_0.ManifestType.class, xmlFile);
            List<ReferenceGroupType> groups = manifestType.getReferenceGroup();
            if (groups != null) {
                for (ReferenceGroupType type : groups) {
                    List<JAXBElement<? extends AbstractReferenceBaseType>> ref = type
                            .getAbstractReferenceBase();
                    if (ref != null && ref.size() >= 2) {
                        JAXBElement<? extends AbstractReferenceBaseType> retrieval = ref
                                .get(0);
                        JAXBElement<? extends AbstractReferenceBaseType> fileHref = ref
                                .get(1);
                        String retrievalID = retrieval.getValue().getHref();
                        String fileLink = fileHref.getValue().getHref();
                        statusHandler.info(retrievalID);
                        statusHandler.info(fileLink);
                    }
                }
            }

        } catch (SerializationException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
    }
}
