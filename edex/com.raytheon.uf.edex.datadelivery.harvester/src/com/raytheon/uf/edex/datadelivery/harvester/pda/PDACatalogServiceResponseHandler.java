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
package com.raytheon.uf.edex.datadelivery.harvester.pda;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfigurationManager;
import com.raytheon.uf.common.datadelivery.harvester.PDACatalogServiceResponseWrapper;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.ogc.common.jaxb.OgcJaxbManager;
import com.raytheon.uf.edex.ogc.common.soap.ServiceExceptionReport;

import net.opengis.cat.csw.v_2_0_2.BriefRecordType;
import net.opengis.cat.csw.v_2_0_2.InsertType;
import net.opengis.cat.csw.v_2_0_2.SearchResultsType;
import net.opengis.cat.csw.v_2_0_2.dc.elements.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.dc.elements.SimpleLiteral;
import net.opengis.ows.v_1_0_0.BoundingBoxType;

/**
 * PDA Catalog Service Response Handler
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 16, 2014  3120     dhladky   Initial creation
 * Nov 10, 2014  3826     dhladky   Added more logging.
 * Apr 21, 2015  4435     dhladky   Connecting to PDA transactions
 * Sep 11, 2015  4881     dhladky   Updates to PDA processing, better tracking.
 * Jan 18, 2016  5260     dhladky   FQDN usage to lessen OGC class collisions.
 * Jan 20, 2016  5280     dhladky   Logging improvement.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Apr 05, 2016  5424     dhladky   More logging enhancements.
 * Aug 11, 2016  5752     tjensen   Fix bounding box parsing in parseBriefRecord
 * Feb 17, 2017  6089     tjensen   Fix null pointers with parseBriefRecord
 * 
 * </pre>
 * 
 * @author dhladky
 */
@WebService(name = "PDACatalogServiceResponseHandler", targetNamespace = "http://www.opengis.net/cat/csw/2.0.2")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({ net.opengis.cat.csw.v_2_0_2.ObjectFactory.class,
        net.opengis.gml.v_3_1_1.ObjectFactory.class,
        net.opengis.filter.v_1_1_0.ObjectFactory.class })
public class PDACatalogServiceResponseHandler {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDACatalogServiceResponseHandler.class);

    /** catalog destination queue **/
    private String fileDestinationUri = null;

    /** transaction destination queue **/
    private String transactionUri = null;

    /** JAXB Manager **/
    private OgcJaxbManager jaxbManager = null;

    /** CSW/OWS/GML class factory **/
    private static final Class<?>[] classes = new Class<?>[] {
            net.opengis.cat.csw.v_2_0_2.ObjectFactory.class,
            net.opengis.gml.v_3_1_1.ObjectFactory.class,
            net.opengis.filter.v_1_1_0.ObjectFactory.class,
            net.opengis.ows.v_1_0_0.ObjectFactory.class };

    /** space split pattern **/
    private static final String SPACE = " ";

    /** DEFAULT_CRS, unspecified by provider **/
    private static final String DEFAULT_CRS = "DEFAULT_CRS ";

    /** internal use service config for PDA service **/
    private static ServiceConfig serviceConfig = null;

    /** Simple Literal OWS factory **/
    private static ObjectFactory literalFactory = null;

    /** Version 1.0.0 Bounding Box factory **/
    private static net.opengis.ows.v_1_0_0.ObjectFactory boundingBoxFactory = null;

    private static net.opengis.cat.csw.v_2_0_2.ObjectFactory briefRecordFactory = null;

    /** Variables for BriefRecordType parse **/
    private static final String ID = "identifier";

    private static final String TYPE = "type";

    private static final String TITLE = "title";

    private static final String BOUNDING_BOX = "BoundingBox";

    private static final String LOWER_CORNER = "LowerCorner";

    private static final String UPPER_CORNER = "UpperCorner";

    private static final String CRS = "crs";

    /** DEBUG PDA system **/
    private static final String DEBUG = "DEBUG";

    /** debug state */
    private static Boolean debug = false;

    /** load PDA service config and factories **/
    static {
        serviceConfig = HarvesterServiceManager.getInstance()
                .getServiceConfig(ServiceType.PDA);
        // debugging MetaData parsing.
        String debugVal = serviceConfig.getConstantValue(DEBUG);
        debug = Boolean.valueOf(debugVal);
        literalFactory = new net.opengis.cat.csw.v_2_0_2.dc.elements.ObjectFactory();
        boundingBoxFactory = new net.opengis.ows.v_1_0_0.ObjectFactory();
        briefRecordFactory = new net.opengis.cat.csw.v_2_0_2.ObjectFactory();
    }

    /** Spring constructor **/
    public PDACatalogServiceResponseHandler(String fileDestinationUri,
            String transactionUri) {
        this.fileDestinationUri = fileDestinationUri;
        this.transactionUri = transactionUri;
    }

    /**
     * 
     * @param handleGetRecordsResponse
     * @return returns void
     * @throws ServiceExceptionReport
     */
    @WebMethod
    public void handleGetRecordsResponse(
            @WebParam(name = "GetRecordsResponse", targetNamespace = "http://www.opengis.net/cat/csw/2.0.2", partName = "Body") net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType response)
                    throws ServiceExceptionReport {

        statusHandler.info(
                "-------Incoming GetRecords() response from PDA -----------");

        if (response.getSearchStatus() != null) {
            // we got a valid search response!
            SearchResultsType srt = response.getSearchResults();

            int numberOfRecords = srt.getNumberOfRecordsMatched().intValue();

            if (numberOfRecords > 0) {
                statusHandler.info(
                        "Received " + numberOfRecords + " new records from "
                                + HarvesterConfigurationManager
                                        .getPDAConfiguration().getProvider()
                                        .getName());
                // we have an actual result link
                String recordFilePath = srt.getResultSetId();
                statusHandler.info("Record File Path: " + recordFilePath);
                // send to download the actual file
                try {
                    sendToFileRetrieval(recordFilePath);
                } catch (Exception e) {
                    statusHandler.handle(Priority.ERROR,
                            "Failed to send to PDA catalog file retreival queue!",
                            e);
                }
            }
        }
    }

    /**
     * 
     * @param transaction
     * @throws ServiceExceptionReport
     */
    @WebMethod
    public void handleTransaction(
            @WebParam(name = "Transaction", targetNamespace = "http://www.opengis.net/cat/csw/2.0.2", partName = "Body") net.opengis.cat.csw.v_2_0_2.TransactionType transactions)
                    throws ServiceExceptionReport {

        statusHandler.info("-------Incoming Transaction from PDA -----------");
        List<Object> records = transactions.getInsertOrUpdateOrDelete();

        if (records != null) {

            List<JAXBElement<BriefRecordType>> briefRecords = new ArrayList<>(
                    records.size());

            for (Object o : records) {
                // We only care about insert messages, we delete
                // metadata through our own methods.
                try {
                    JAXBElement<BriefRecordType> brt = parseBriefRecord(o);
                    if (brt != null) {
                        briefRecords.add(brt);
                    }
                } catch (Exception e) {
                    statusHandler.error(
                            "Error parsing Transaction message from PDA.", e);
                }
            }

            statusHandler.handle(Priority.INFO, "Sending " + briefRecords.size()
                    + " Transaction Inserts to MetaDataProcessor.");

            for (JAXBElement<BriefRecordType> briefRecord : briefRecords) {
                try {
                    sendToMetaDataProcessor(briefRecord);
                } catch (Exception e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Unable to send BriefRecord to metadata Processor queue.",
                            e);
                }
            }
        }
    }

    /**
     * 
     * Drops PDA remote file path into queue for retrieval
     * 
     * @param filePath
     * @throws Exception
     */
    private void sendToFileRetrieval(String filePath) throws Exception {

        if (filePath != null) {
            PDACatalogServiceResponseWrapper psrw = new PDACatalogServiceResponseWrapper(
                    filePath);
            byte[] bytes = SerializationUtil.transformToThrift(psrw);
            EDEXUtil.getMessageProducer().sendAsync(fileDestinationUri, bytes);
        }
    }

    /**
     * 
     * Drops PDA transactions to metaData processor
     * 
     * @param BriefRecordType
     *            briefRecord
     * @throws Exception
     */
    private void sendToMetaDataProcessor(
            JAXBElement<BriefRecordType> briefRecord) throws Exception {

        if (briefRecord != null) {

            String xml = getJaxbManager().marshalToXml(briefRecord);
            EDEXUtil.getMessageProducer().sendAsync(transactionUri,
                    xml.getBytes());
        }
    }

    /**
     * Gets the JAXB manager for use with BriefRecord decode
     * 
     * @return
     */
    private OgcJaxbManager getJaxbManager() {

        if (jaxbManager == null) {
            try {
                this.jaxbManager = new OgcJaxbManager(classes);
            } catch (JAXBException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "JaxbManager failed to initialize, cannot deserialize CSW classes.",
                        e);
            }
        }

        return jaxbManager;
    }

    /**
     * Create a BriefRecord from the PDA Insert Transaction message
     * 
     * @param o
     * @return
     */
    private JAXBElement<BriefRecordType> parseBriefRecord(Object o) {

        JAXBElement<BriefRecordType> jaxbBriefRecord = null;
        BriefRecordType brt = null;
        String identifier = null;
        String title = null;
        String type = null;
        String lowerCorner = null;
        String upperCorner = null;
        String crsAttr = null;

        // PDA only sends insert and delete, no update, we don't care about
        // delete.
        if (o instanceof InsertType) {

            InsertType trans = (InsertType) o;
            if (debug) {
                statusHandler.info("Insert Message: Elements present.");
                List<Element> elements = trans.getAny();
                for (Element element : elements) {
                    if (element.getLocalName() != null) {
                        statusHandler
                                .info("Element: " + element.getLocalName());
                    }
                }
            }

            for (int i = 0; i < trans.getAny().size(); i++) {

                Element element = trans.getAny().get(i);

                // If no child nodes set, ignore this element.
                if (element.getFirstChild() != null) {
                    if (element.getLocalName().equals(ID)) {
                        identifier = element.getFirstChild().getNodeValue();
                        if (debug) {
                            statusHandler.info("indentifier: " + identifier);
                        }
                    } else if (element.getLocalName().equals(TITLE)) {
                        title = element.getFirstChild().getNodeValue();
                        if (debug) {
                            statusHandler.info("title: " + title);
                        }
                    } else if (element.getLocalName().equals(TYPE)) {
                        type = element.getFirstChild().getNodeValue();
                        if (debug) {
                            statusHandler.info("type: " + type);
                        }
                    } else if (element.getLocalName().equals(BOUNDING_BOX)) {

                        NodeList nodes = element.getChildNodes();
                        crsAttr = element.getAttribute(CRS);

                        if (debug) {
                            statusHandler.info("CRS: " + crsAttr);
                        }

                        for (int j = 0; j < nodes.getLength(); j++) {

                            Node node = nodes.item(j);

                            if (node.getLocalName().equals(LOWER_CORNER)) {
                                if (node.getFirstChild() != null) {
                                    if (node.getFirstChild()
                                            .getNodeValue() != null) {
                                        lowerCorner = node.getFirstChild()
                                                .getNodeValue();
                                        if (debug) {
                                            statusHandler.info("LOWER_CORNER: "
                                                    + lowerCorner);
                                        }
                                    } else {
                                        statusHandler
                                                .warn("LOWER_CORNER is blank!");
                                    }
                                }
                            } else if (node.getLocalName()
                                    .equals(UPPER_CORNER)) {

                                if (node.getFirstChild() != null) {
                                    if (node.getFirstChild()
                                            .getNodeValue() != null) {
                                        upperCorner = node.getFirstChild()
                                                .getNodeValue();
                                        if (debug) {
                                            statusHandler.info("UPPER_CORNER: "
                                                    + upperCorner);
                                        }
                                    } else {
                                        statusHandler
                                                .warn("UPPER_CORNER is blank!");
                                    }
                                }
                            } else {
                                if (node.getFirstChild() != null) {
                                    if (node.getFirstChild()
                                            .getNodeValue() != null) {
                                        statusHandler.info("Extra node: "
                                                + node.getLocalName()
                                                + " value:"
                                                + node.getFirstChild()
                                                        .getNodeValue());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // process information
            if (identifier != null && title != null) {

                // identifier builder
                List<JAXBElement<SimpleLiteral>> idLiterals = new ArrayList<>(
                        1);
                SimpleLiteral idLiteral = new SimpleLiteral();
                List<String> ids = new ArrayList<>(1);
                ids.add(identifier);
                idLiteral.setContent(ids);
                JAXBElement<SimpleLiteral> jaxbIdLiteral = literalFactory
                        .createIdentifier(idLiteral);
                idLiterals.add(jaxbIdLiteral);

                // title builder
                List<JAXBElement<SimpleLiteral>> titleLiterals = new ArrayList<>(
                        1);
                SimpleLiteral titleLiteral = new SimpleLiteral();
                List<String> titles = new ArrayList<>(1);
                titles.add(title);
                titleLiteral.setContent(titles);
                JAXBElement<SimpleLiteral> jaxbTitleLiteral = literalFactory
                        .createTitle(titleLiteral);
                titleLiterals.add(jaxbTitleLiteral);

                // type builder
                SimpleLiteral typeLiteral = new SimpleLiteral();
                List<String> types = new ArrayList<>(1);
                types.add(type);
                typeLiteral.setContent(types);

                // Bounding Box
                List<JAXBElement<BoundingBoxType>> boundingBoxes = new ArrayList<>(
                        1);
                List<Double> upperVals = null;
                List<Double> lowerVals = null;

                if (upperCorner != null && lowerCorner != null) {
                    String[] uppers = upperCorner.split(SPACE);
                    String[] lowers = lowerCorner.split(SPACE);
                    // uppers
                    upperVals = new ArrayList<>(2);
                    upperVals.add(Double.parseDouble(uppers[0]));
                    upperVals.add(Double.parseDouble(uppers[1]));
                    // lowers
                    lowerVals = new ArrayList<>(2);
                    lowerVals.add(Double.parseDouble(lowers[0]));
                    lowerVals.add(Double.parseDouble(lowers[1]));
                }

                if (crsAttr == null || crsAttr.equals("")) {
                    crsAttr = serviceConfig.getConstantValue(DEFAULT_CRS);
                    statusHandler
                            .warn("Unable to retrieve CRS from bounding box. Using default CRS: "
                                    + crsAttr);
                }

                // create the Box
                BoundingBoxType bbt = new BoundingBoxType();
                bbt.setCrs(crsAttr);

                if (upperCorner != null && lowerCorner != null) {
                    bbt.setLowerCorner(lowerVals);
                    bbt.setUpperCorner(upperVals);
                    // 2 dimensions
                    bbt.setDimensions(
                            BigInteger.valueOf(new Integer(2).intValue()));
                    JAXBElement<BoundingBoxType> jaxbBoundingBox = boundingBoxFactory
                            .createBoundingBox(bbt);
                    boundingBoxes.add(jaxbBoundingBox);
                }

                // Add everything to the BriefRecordType
                brt = new BriefRecordType();
                brt.setBoundingBox(boundingBoxes);
                brt.setIdentifier(idLiterals);
                brt.setTitle(titleLiterals);
                brt.setType(typeLiteral);
                // make the XMLable record
                jaxbBriefRecord = briefRecordFactory.createBriefRecord(brt);

            } else {

                StringBuffer errorMessage = new StringBuffer(255);
                errorMessage.append("Parsing CSW transaction failed: ");
                // ID
                if (identifier != null) {
                    errorMessage.append(identifier + ", ");
                } else {
                    errorMessage.append("identifier is null, ");
                }
                // Title
                if (title != null) {
                    errorMessage.append(title + ", ");
                } else {
                    errorMessage.append("title is null, ");
                }
                // Type
                if (type != null) {
                    errorMessage.append(type);
                } else {
                    errorMessage.append("type is null");
                }

                statusHandler.warn(errorMessage.toString());
            }

        } else {
            statusHandler
                    .debug("Unknown or discarded CSW metadata transaction. Class: "
                            + o.getClass().getName());
        }

        return jaxbBriefRecord;
    }
}
