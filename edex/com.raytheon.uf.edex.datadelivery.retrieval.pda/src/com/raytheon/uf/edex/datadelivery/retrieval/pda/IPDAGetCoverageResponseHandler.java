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

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

import com.raytheon.uf.edex.ogc.common.soap.ServiceExceptionReport;

/**
 * PDA Interface for get Coverage processing.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 26, 2014 5424       dhladky     Initial creation
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */


@WebService(name = "IPDAGetCoverageResponseHandler", targetNamespace = "http://www.opengis.net/wcs/2.0")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({ net.opengis.ows.v_2_0.ObjectFactory.class})
public interface IPDAGetCoverageResponseHandler {

    /**
     * 
     * @param manifestType
     * @return returns void
     * @throws ServiceExceptionReport
     */
    @WebMethod
    public void handleGetCoverageResponse(
            @WebParam(name = "GetCoverage", targetNamespace = "http://www.opengis.net/ows/2.0", partName = "Body")
            net.opengis.ows.v_2_0.ManifestType manifestType)
            throws ServiceExceptionReport;

}
