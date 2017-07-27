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

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.time.util.TimeUtil;

/**
 * Used to represent legacy RetrievalRequestRecord
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 27, 2017  6186     rjpeter   Initial creation.
 *
 * </pre>
 *
 * @author rjpeter
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SbnRetrievalRequestRecordXml {

    @XmlElement
    private SbnRetrievalRequestRecordPkXml id;

    @XmlElement
    private String owner;

    @XmlElement
    private String plugin;

    @XmlElement
    private String provider;

    @XmlElement
    private SbnRetrievalXml retrievalObj;

    // Not used 18.1.1+
    @XmlElement
    private String subscriptionType;

    // Not used 18.1.1+
    @XmlElement
    private Date insertTime;

    // Not used 18.1.1+
    @XmlElement
    private long subRetrievalKey;

    // Not used in 18.1.1+
    @XmlElement
    private String state;

    // Not used in 18.1.1+
    @XmlElement
    private String network;

    // Not used in 18.1.1+
    @XmlElement
    private int priority;

    public SbnRetrievalRequestRecordXml(Retrieval retrieval) {
        id = new SbnRetrievalRequestRecordPkXml();
        id.setSubscriptionName(retrieval.getSubscriptionName());
        id.setIndex(0);
        owner = retrieval.getOwner();
        plugin = retrieval.getPlugin();
        subscriptionType = "Subscribed";
        provider = retrieval.getProvider();
        state = "RUNNING";
        network = retrieval.getNetwork().toString();
        insertTime = TimeUtil.newDate();
        retrievalObj = new SbnRetrievalXml(retrieval);
    }

    public SbnRetrievalRequestRecordXml() {

    }

    public SbnRetrievalRequestRecordPkXml getId() {
        return id;
    }

    public void setId(SbnRetrievalRequestRecordPkXml id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public SbnRetrievalXml getRetrievalObj() {
        return retrievalObj;
    }

    public void setRetrievalObj(SbnRetrievalXml retrievalObj) {
        this.retrievalObj = retrievalObj;
    }

}
