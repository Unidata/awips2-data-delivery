package com.raytheon.uf.common.datadelivery.retrieval.xml;

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.datadelivery.registry.Connection;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * ProviderRetrieval XML
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 07, 2011  191       dhladky     Initial creation
 * Nov 19, 2012 1166       djohnson    Clean up JAXB representation of registry objects.
 * Feb 07, 2013 1543       djohnson    Never have null attributes.
 * 07 Nov, 2013 2361       njensen     Remove ISerializableObject
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class Retrieval implements Serializable {

    private static final long serialVersionUID = -1294387473701456892L;

    @XmlElement
    @DynamicSerializeElement
    private String subscriptionName;

    @XmlElement(name = "connection", type = Connection.class)
    @DynamicSerializeElement
    private Connection connection;

    @XmlElement
    @DynamicSerializeElement
    private ServiceType serviceType;

    @XmlElement
    @DynamicSerializeElement
    private String owner;

    @XmlElement
    @DynamicSerializeElement
    private DataType dataType;

    @XmlElement
    @DynamicSerializeElement
    private SubscriptionType subscriptionType;

    @XmlElement
    @DynamicSerializeElement
    private Network network;

    @XmlElements({ @XmlElement(name = "attribute", type = RetrievalAttribute.class) })
    @DynamicSerializeElement
    private List<RetrievalAttribute> attributes = new ArrayList<RetrievalAttribute>();

    /**
     * Add another Attribute entry
     * 
     * @param pdo
     */
    public void addAttribute(RetrievalAttribute att) {
        attributes.add(att);
    }

    public List<RetrievalAttribute> getAttributes() {
        return attributes;
    }

    public Connection getConnection() {
        return connection;
    }

    public String getOwner() {
        return owner;
    }

    /**
     * Get a particular attribute
     * 
     * @param name
     * @return
     */
    public RetrievalAttribute getProviderAttributeByName(String name) {
        for (RetrievalAttribute att : getAttributes()) {
            if (att.getParameter().getName().equals(att)) {
                return att;
            }
        }

        return null;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    /**
     * Remove Attribute entry
     * 
     * @param pdo
     */
    public void removeAttribute(RetrievalAttribute att) {
        attributes.remove(att);
    }

    public void setAttributes(List<RetrievalAttribute> attribute) {
        this.attributes = attribute;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
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

    /**
     * Enumeration of subscription types, we know so far
     * 
     * <pre>
     * 
     * SOFTWARE HISTORY
     * 
     * Date         Ticket#    Engineer    Description
     * ------------ ---------- ----------- --------------------------
     * Nov 26, 2012  1340       dhladky     Initial creation
     * 
     * </pre>
     * 
     * @author dhladky
     * @version 1.0
     */
    @XmlEnum
    public enum SubscriptionType {
        @XmlEnumValue("AD_HOC")
        AD_HOC, @XmlEnumValue("Subscribed")
        SUBSCRIBED, @XmlEnumValue("Pending")
        PENDING
    }

}
