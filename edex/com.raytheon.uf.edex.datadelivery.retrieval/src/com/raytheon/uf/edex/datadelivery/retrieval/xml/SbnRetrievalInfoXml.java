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
package com.raytheon.uf.edex.datadelivery.retrieval.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;

/**
 *
 * Simplified XML containing minimum information needed to construct a
 * retrieval. Sent over SBN by central registry to site registries along with
 * data for shared subscriptions. Site registries use this to build Retrieval
 * objects needed to process the data for the shared subscription.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 15, 2017 6498       tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SbnRetrievalInfoXml {

    @XmlElement
    private String subscriptionId;

    @XmlElement
    private String url;

    @XmlElement
    private ServiceType serviceType;

    @XmlAnyElement(lax = true)
    private IRetrievalResponse retrievalResponse;

    @XmlElement
    private RetrievalAttribute attribute;

    public SbnRetrievalInfoXml() {

    }

    public SbnRetrievalInfoXml(String subscriptionId, String url,
            ServiceType serviceType, IRetrievalResponse retrievalResponse,
            RetrievalAttribute attribute) {
        this.subscriptionId = subscriptionId;
        this.url = url;
        this.serviceType = serviceType;
        this.retrievalResponse = retrievalResponse;
        this.attribute = attribute;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public IRetrievalResponse getRetrievalResponse() {
        return retrievalResponse;
    }

    public void setRetrievalResponse(IRetrievalResponse retrievalResponse) {
        this.retrievalResponse = retrievalResponse;
    }

    public RetrievalAttribute getAttribute() {
        return attribute;
    }

    public void setAttribute(RetrievalAttribute attribute) {
        this.attribute = attribute;
    }

}
