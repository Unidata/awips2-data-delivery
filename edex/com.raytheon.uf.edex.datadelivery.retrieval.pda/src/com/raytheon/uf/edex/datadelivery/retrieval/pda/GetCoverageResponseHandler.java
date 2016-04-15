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

import net.opengis.ows.v_2_0.AbstractReferenceBaseType;
import net.opengis.ows.v_2_0.ManifestType;
import net.opengis.ows.v_2_0.ReferenceGroupType;

import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalDao;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecordPK;
import com.raytheon.uf.edex.datadelivery.retrieval.util.RetrievalGeneratorUtilities;
import com.raytheon.uf.edex.ogc.common.jaxb.OgcJaxbManager;
import com.raytheon.uf.edex.ogc.common.soap.ServiceExceptionReport;

/**
 * PDA getCoverage Response Handler
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 16, 2016 5424       dhladky     Initial creation
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

@WebService(name = "GetCoverageResponseHandler", targetNamespace = "http://www.opengis.net/wcs/2.0")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({ net.opengis.ows.v_2_0.ObjectFactory.class })
public class GetCoverageResponseHandler implements
        IPDAGetCoverageResponseHandler {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GetCoverageResponseHandler.class);

    /** JAXB Manager **/
    private OgcJaxbManager jaxbManager = null;
    
    /** retrieval DAO **/
    private RetrievalDao retrievalDao;
    
    /** split on slash for RetrievalPK **/
    private String SLASH = "/";
    
    /** Retrieval queue endpoint */
    private String destinationUri;

    /** OWS class factory **/
    private static final Class<?>[] classes = new Class<?>[] { net.opengis.ows.v_2_0.ObjectFactory.class };

    public GetCoverageResponseHandler() {
        
    }
    
    /**
     * spring constructor
     * 
     * @param destinationUri
     * @param retrievalDao
     */
    public GetCoverageResponseHandler(String destinationUri,
            RetrievalDao retrievalDao) {
        this.destinationUri = destinationUri;
        this.retrievalDao = retrievalDao;
        statusHandler.info("Constructed response handler, Queue:"
                + destinationUri + " DAO:" + retrievalDao.getClass().getName());
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
                statusHandler
                        .handle(Priority.PROBLEM,
                                "JaxbManager failed to initialize, can not deserialize CSW classes.",
                                e);
            }
        }

        return jaxbManager;
    }

    @Override
    @WebMethod
    public void handleGetCoverageResponse(
            @WebParam(name = "GetCoverage", targetNamespace = "http://www.opengis.net/ows/2.0", partName = "Body")
            net.opengis.ows.v_2_0.ManifestType manifestType)
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
                    statusHandler.info("Recieved async retrievalID: "
                            + retrievalID + " file link: " + fileLink
                            + " from PDA.");
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
        if (retrievalID != null && fileLink != null) {

            String[] recordParts = retrievalID.split(SLASH);
            String subscriptionName = recordParts[0];
            Integer index = Integer.valueOf(recordParts[1]);

            RetrievalRequestRecordPK pk = new RetrievalRequestRecordPK(
                    subscriptionName, index);
            RetrievalRequestRecord rrr = retrievalDao.getById(pk);
            Retrieval retrieval = null;

            if (rrr != null) {

                try {
                    retrieval = rrr.getRetrievalObj();
                    // Set the url with the link used in retrieval.
                    retrieval.getConnection().setUrl(fileLink);
                    rrr.setRetrievalObj(retrieval);

                } catch (SerializationException e1) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Could not serialize and set the Subscription Retrieval object. PK:"
                                    + retrievalID, e1);
                }

                // update for posterity
                retrievalDao.update(rrr);

                try {
                    RetrievalGeneratorUtilities.sendToRetrieval(destinationUri,
                            rrr.getNetwork(),
                            new Object[] { rrr.getRetrievalObj() });
                    statusHandler.info("Sent PDA retrieval to queue. "
                            + rrr.getNetwork().toString());
                } catch (Exception e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Couldn't send RetrievalRecord to Queue! ID: "
                                    + retrievalID, e);
                }
            } else {
                statusHandler.error("Unable to lookup retrieval record. PK: "
                        + retrievalID+" No record exists in DB.");
            }
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
