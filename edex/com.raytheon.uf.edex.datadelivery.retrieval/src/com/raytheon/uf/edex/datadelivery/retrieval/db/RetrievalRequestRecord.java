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
package com.raytheon.uf.edex.datadelivery.retrieval.db;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.dataplugin.persist.IPersistableDataObject;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.time.util.TimeUtil;

/**
 * Retrieval Request Record
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * May 09, 2012           rjpeter   Initial creation
 * Oct 10, 2012  726      djohnson  Add {@link #subRetrievalKey}.
 * Nov 26, 2012  1340     dhladky   Added additional fields for tracking
 *                                  subscriptions
 * Jan 30, 2013  1543     djohnson  Add PENDING_SBN, give retrieval column a
 *                                  length.
 * Jan 30, 2014  2686     dhladky   refactor of retrieval.
 * May 11, 2015  6186     rjpeter   Updated constructor.
 * May 22, 2017  6130     tjensen   Add DataSetName
 * Jul 31, 2017  6186     rjpeter   Refactored to be auto id.
 * Aug 02, 2017  6186     rjpeter   Added latencyExpireTime
 * Oct 23, 2017  6415     nabowle   Added latencyMinutes.
 *
 *
 * </pre>
 *
 * @author rjpeter
 */
@Entity
@Table(name = "subscription_retrieval")
@SequenceGenerator(initialValue = 1, allocationSize = 1, name = "SubscriptionRetrieval", sequenceName = "subscription_retrieval_seq")
public class RetrievalRequestRecord implements IPersistableDataObject<Integer> {

    public enum State {
        WAITING_RESPONSE, PENDING, RUNNING, FAILED, COMPLETED;
    };

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SubscriptionRetrieval")
    protected int id;

    @Column(nullable = false)
    private String dsmdUrl;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String subscriptionName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private State state;

    @Column(nullable = false)
    private int priority = Integer.MAX_VALUE;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String dataSetName;

    @Column(nullable = false)
    private Date insertTime;

    @Column(nullable = false)
    private Date latencyExpireTime;

    @Column(nullable = false)
    private int latencyMinutes;

    @Column(nullable = false, length = 100_000)
    private byte[] retrieval;

    @Transient
    private Retrieval retrievalObj;

    public RetrievalRequestRecord() {
    }

    public RetrievalRequestRecord(Retrieval retrieval, String dsmdUrl,
            State state, int priority) {
        this.dsmdUrl = dsmdUrl;
        owner = retrieval.getOwner();
        subscriptionName = retrieval.getSubscriptionName();
        this.state = state;
        this.priority = priority;
        provider = retrieval.getProvider();
        dataSetName = retrieval.getDataSetName();
        insertTime = TimeUtil.newDate();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDsmdUrl() {
        return dsmdUrl;
    }

    public void setDsmdUrl(String dsmdUrl) {
        this.dsmdUrl = dsmdUrl;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    public Date getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(Date insertTime) {
        this.insertTime = insertTime;
    }

    public byte[] getRetrieval() {
        return retrieval;
    }

    public void setRetrieval(byte[] retrieval) {
        this.retrieval = retrieval;
    }

    /**
     * Convenience method to set the retrieval byte array from an object.
     *
     * @param retrieval
     *            the retrieval
     * @throws SerializationException
     *             on error serializing the retrieval
     */
    public void setRetrievalObj(Retrieval retrieval)
            throws SerializationException {
        this.retrievalObj = retrieval;
        this.retrieval = SerializationUtil.transformToThrift(retrieval);
    }

    /**
     * Convenience method to get the retrieval as an object.
     *
     * @return the retrievalObj
     * @throws SerializationException
     *             on error deserializing the retrieval
     */
    public Retrieval getRetrievalObj() throws SerializationException {
        if (retrievalObj == null && retrieval != null) {
            retrievalObj = SerializationUtil
                    .transformFromThrift(Retrieval.class, retrieval);
        }
        return retrievalObj;
    }

    @Override
    public String toString() {
        return "Id: " + id + ", Subscription: " + subscriptionName
                + ", dataSet: " + dataSetName + ", dsmdUrl: " + dsmdUrl;
    }

    @Override
    public Integer getIdentifier() {
        return id;
    }

    public Date getLatencyExpireTime() {
        return latencyExpireTime;
    }

    public void setLatencyExpireTime(Date latencyExpireTime) {
        this.latencyExpireTime = latencyExpireTime;
    }

    public int getLatencyMinutes() {
        return latencyMinutes;
    }

    public void setLatencyMinutes(int latencyMinutes) {
        this.latencyMinutes = latencyMinutes;
    }

}