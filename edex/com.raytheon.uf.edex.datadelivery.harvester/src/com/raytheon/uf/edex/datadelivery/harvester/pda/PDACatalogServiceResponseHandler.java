package com.raytheon.uf.edex.datadelivery.harvester.pda;

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

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

import net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType;
import net.opengis.cat.csw.v_2_0_2.SearchResultsType;

import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfigurationManager;
import com.raytheon.uf.common.datadelivery.harvester.PDACatalogServiceResponseWrapper;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.datadelivery.harvester.interfaces.IPDACatalogServiceResponseHandler;
import com.raytheon.uf.edex.ogc.common.soap.ServiceExceptionReport;

/**
 * PDA Catalog Service Response Handler
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 16, 2014 3120       dhladky     Initial creation
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

@WebService(name = "PDACatalogServiceResponseHandler", targetNamespace = "http://www.opengis.net/cat/csw/2.0.2")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({net.opengis.cat.csw.v_2_0_2.ObjectFactory.class, net.opengis.gml.v_3_1_1.ObjectFactory.class, net.opengis.filter.v_1_1_0.ObjectFactory.class})
public class PDACatalogServiceResponseHandler implements IPDACatalogServiceResponseHandler {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDACatalogServiceResponseHandler.class);

    private String destinationUri = null;
    
    /** Spring constructor **/
    public PDACatalogServiceResponseHandler(String destinationUri) {
        this.destinationUri = destinationUri;
    }
    
    @Override
    @WebMethod
    public void handleGetRecordsResponse(
            @WebParam(name = "GetRecordsResponse", targetNamespace = "http://www.opengis.net/cat/csw/2.0.2", partName = "Body")
            GetRecordsResponseType response) throws ServiceExceptionReport {
        
        if (response.getSearchStatus() != null) {
            // we got a valid search response!
            SearchResultsType srt = response.getSearchResults();
            
            int numberOfRecords = srt.getNumberOfRecordsMatched().intValue();

            if (numberOfRecords > 0) {
                statusHandler.info("Recieved " + numberOfRecords
                        + " new records from "
                        + HarvesterConfigurationManager.getPDAConfiguration().getProvider().getName());
                // we have an actual result link
                String recordFilePath = srt.getResultSetId();
                // send to download the actual file
                try {
                    sendToFileRetrieval(recordFilePath);
                } catch (Exception e) {
                    statusHandler
                            .handle(Priority.ERROR,
                                    "Failed to send to PDA catalog file retreival queue!",
                                    e);
                }
            }
        }
    }

    /**
     * 
     * Drops PDA remote file path into queue for retrieval
     * 
     * @param destinationUri
     * @param filePath
     * @throws Exception
     */
    private void sendToFileRetrieval(String filePath) throws Exception {

        if (filePath != null) {

            PDACatalogServiceResponseWrapper psrw = new PDACatalogServiceResponseWrapper(filePath);
            byte[] bytes = SerializationUtil.transformToThrift(psrw);
            EDEXUtil.getMessageProducer().sendAsync(destinationUri, bytes);
        }
    }

}
