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
package com.raytheon.uf.common.datadelivery.retrieval.xml;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * ProviderRetrieval XML
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 07, 2011  191      dhladky   Initial creation
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Feb 07, 2013  1543     djohnson  Never have null attributes.
 * 07 Nov, 2013  2361     njensen   Remove ISerializableObject
 * Jul 25, 2014  2732     ccody     Add Date Time to SubscriptionRetrievalEvent
 *                                  message.
 * Jul 25, 2017  6186     rjpeter   Only one attribute per retrieval
 *
 * </pre>
 *
 * @author dhladky
 */
@DynamicSerialize
public class Retrieval<T extends Time, C extends Coverage> {

    @XmlEnum
    public enum SubscriptionType {
        @XmlEnumValue("AD_HOC")
        AD_HOC, @XmlEnumValue("Subscribed")
        SUBSCRIBED, @XmlEnumValue("Pending")
        PENDING
    }

    @DynamicSerializeElement
    private String subscriptionName;

    @DynamicSerializeElement
    private String dataSetName;

    @DynamicSerializeElement
    private String provider;

    @DynamicSerializeElement
    private String plugin;

    @DynamicSerializeElement
    private String url;

    @DynamicSerializeElement
    private ServiceType serviceType;

    @DynamicSerializeElement
    private String owner;

    @DynamicSerializeElement
    private DataType dataType;

    @DynamicSerializeElement
    private SubscriptionType subscriptionType;

    @DynamicSerializeElement
    private Network network;

    @DynamicSerializeElement
    private Long requestRetrievalTime;

    @DynamicSerializeElement
    private RetrievalAttribute<T, C> attribute;

    public RetrievalAttribute<T, C> getAttribute() {
        return attribute;
    }

    public void setAttribute(RetrievalAttribute<T, C> attribute) {
        this.attribute = attribute;
    }

    public String getOwner() {
        return owner;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    public void setDataType(DataType providerType) {
        this.dataType = providerType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public Network getNetwork() {
        return network;
    }

    public Long getRequestRetrievalTime() {
        if (this.requestRetrievalTime == null) {
            this.requestRetrievalTime = Long.valueOf(0L);
        }
        return requestRetrievalTime;
    }

    public void setRequestRetrievalTime(Long requestRetrievalTime) {
        if (requestRetrievalTime == null) {
            requestRetrievalTime = Long.valueOf(0L);
        }
        this.requestRetrievalTime = requestRetrievalTime;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public T getTime() {
        return (attribute != null ? attribute.getTime() : null);
    }

    public C getCoverage() {
        return (attribute != null ? attribute.getCoverage() : null);
    }

    @Override
    public String toString() {
        return "Subscription: " + subscriptionName + ", dataSet: "
                + dataSetName;
    }
}
