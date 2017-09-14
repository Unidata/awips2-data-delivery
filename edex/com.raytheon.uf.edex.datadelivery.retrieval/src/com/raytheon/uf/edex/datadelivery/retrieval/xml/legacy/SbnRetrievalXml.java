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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval.SubscriptionType;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;

/**
 * Used to represent legacy Retrieval
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 27, 2017  6186     rjpeter   Initial creation.
 * Sep 20, 2017  6413     tjensen   Update for ParameterGroups
 *
 * </pre>
 *
 * @author rjpeter
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SbnRetrievalXml {
    @XmlElement
    private String subscriptionName;

    @XmlElement
    private ServiceType serviceType;

    @XmlElement
    private String owner;

    @XmlElement
    private DataType dataType;

    @XmlElement
    private SubscriptionType subscriptionType;

    @XmlElement
    private Network network;

    @XmlElement
    private Long requestRetrievalTime;

    @XmlElement
    private SbnRetrievalAttributeXml attribute;

    /* sent instead of connection */
    @XmlElement
    private String url;

    /* extra as of 18.1.1 */
    @XmlElement
    private String dataSetName;

    public SbnRetrievalXml() {

    }

    public SbnRetrievalXml(Retrieval retrieval) {
        this.subscriptionName = retrieval.getSubscriptionName();
        this.serviceType = retrieval.getServiceType();
        this.owner = retrieval.getOwner();
        this.dataType = retrieval.getDataType();
        this.subscriptionType = retrieval.getSubscriptionType();
        this.network = retrieval.getNetwork();
        this.requestRetrievalTime = retrieval.getRequestRetrievalTime();
        this.url = retrieval.getUrl();
        this.dataSetName = retrieval.getDataSetName();
        this.attribute = new SbnRetrievalAttributeXml(retrieval);
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public Long getRequestRetrievalTime() {
        return requestRetrievalTime;
    }

    public void setRequestRetrievalTime(Long requestRetrievalTime) {
        this.requestRetrievalTime = requestRetrievalTime;
    }

    public SbnRetrievalAttributeXml getAttribute() {
        return attribute;
    }

    public void setAttribute(SbnRetrievalAttributeXml attribute) {
        this.attribute = attribute;
    }

    public Retrieval asRetrieval() {
        Retrieval retrieval = new Retrieval();
        retrieval.setSubscriptionName(subscriptionName);
        retrieval.setDataSetName(dataSetName);
        retrieval.setProvider(attribute.getProvider());
        retrieval.setPlugin(attribute.getPlugin());
        retrieval.setUrl(url);
        retrieval.setServiceType(serviceType);
        retrieval.setOwner(owner);
        retrieval.setDataType(dataType);
        retrieval.setSubscriptionType(subscriptionType);
        retrieval.setNetwork(network);
        retrieval.setRequestRetrievalTime(requestRetrievalTime);
        RetrievalAttribute att = new RetrievalAttribute();
        att.setCoverage(attribute.getCoverage());
        att.setEnsemble(attribute.getEnsemble());
        att.setParameter(attribute.getParameter());
        att.setParameterGroup(attribute.getParameterGroup());
        att.setTime(attribute.getTime());
        retrieval.setAttribute(att);
        return retrieval;
    }
}
