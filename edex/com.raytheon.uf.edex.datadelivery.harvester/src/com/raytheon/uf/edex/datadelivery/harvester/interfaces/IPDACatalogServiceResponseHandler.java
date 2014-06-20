package com.raytheon.uf.edex.datadelivery.harvester.interfaces;

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

import com.raytheon.uf.edex.ogc.common.soap.ServiceExceptionReport;

/**
 * PDA Catalog Service Response Handler Interface
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
public interface IPDACatalogServiceResponseHandler {

    /**
     * 
     * @param handleGetRecordsResponse
     * @return returns
     *         void
     * @throws Exception
     */
    @WebMethod
    public void handleGetRecordsResponse(
            @WebParam(name = "GetRecordsResponse", targetNamespace = "http://www.opengis.net/cat/csw/2.0.2", partName = "Body") GetRecordsResponseType response)
                    throws ServiceExceptionReport;

    
}
