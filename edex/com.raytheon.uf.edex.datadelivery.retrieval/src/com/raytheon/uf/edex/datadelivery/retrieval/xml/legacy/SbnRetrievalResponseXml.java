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
package com.raytheon.uf.edex.datadelivery.retrieval.xml.legacy;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;

/**
 * Used to represent legacy RetrievalResponse
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 27, 2017  6186     rjpeter   Initial creation.
 * Nov 15, 2017  6498     tjensen   Deprecated. Use SbnRetrievalInfoXml once all
 *                                  sites are 18.1.1 or beyond.
 *
 * </pre>
 *
 * @author rjpeter
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class SbnRetrievalResponseXml {
    @XmlElement
    private SbnRetrievalRequestRecordPkXml requestRecord;

    @XmlAttribute
    private boolean success;

    /*
     * previous versions support multiple records per response, but impls only
     * had a single entry
     */
    @XmlElement
    private SbnRetrievalRequestRecordXml retrievalRequestRecord;

    @XmlElement(name = "retrievalResponseWrapper")
    private SbnRetrievalResponseWrapper responseWrapper;

    /**
     * Constructor.
     */
    public SbnRetrievalResponseXml() {
    }

    /**
     * Constructor.
     *
     * @param requestRecord
     * @param retrievalAttributePluginDataObjects
     */
    public SbnRetrievalResponseXml(Retrieval retrieval,
            IRetrievalResponse retrievalResponse, boolean success) {
        this.retrievalRequestRecord = new SbnRetrievalRequestRecordXml(
                retrieval);
        this.responseWrapper = new SbnRetrievalResponseWrapper(
                retrievalResponse);
        this.requestRecord = this.retrievalRequestRecord.getId();
        this.success = success;
    }

    public SbnRetrievalRequestRecordPkXml getRequestRecord() {
        return requestRecord;
    }

    public void setRequestRecord(SbnRetrievalRequestRecordPkXml requestRecord) {
        this.requestRecord = requestRecord;
    }

    public SbnRetrievalResponseWrapper getResponseWrapper() {
        return responseWrapper;
    }

    public void setResponseWrapper(
            SbnRetrievalResponseWrapper responseWrapper) {
        this.responseWrapper = responseWrapper;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public SbnRetrievalRequestRecordXml getRetrievalRequestRecord() {
        return retrievalRequestRecord;
    }

    public void setRetrievalRequestRecord(
            SbnRetrievalRequestRecordXml retrievalRequestRecord) {
        this.retrievalRequestRecord = retrievalRequestRecord;
    }

}
