package com.raytheon.uf.edex.datadelivery.retrieval.wfs;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalResponse;

/**
 * {@link RetrievalResponse} for WFS.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 12, 2013 753        dhladky     Initial creation
 * May 31, 2013 2038       djohnson    Move to correct repo.
 * Oct 04, 2013 2267       bgonzale    Added default constructor.
 *
 * </pre>
 *
 * @author dhladky
 * @version 1.0
 */

@DynamicSerialize
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class WFSRetrievalResponse extends RetrievalResponse {

    @XmlElement
    private String payload;

    public void setPayLoad(String payLoad) {
        this.payload = payLoad;
    }

    public String getPayLoad() {
        return payload;
    }

}
