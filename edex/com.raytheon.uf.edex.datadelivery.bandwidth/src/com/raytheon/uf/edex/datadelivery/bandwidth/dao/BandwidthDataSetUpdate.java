package com.raytheon.uf.edex.datadelivery.bandwidth.dao;

import java.io.Serializable;
import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.raytheon.uf.common.dataplugin.persist.IPersistableDataObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.IDeepCopyable;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;

/**
 * 
 * A dataset update.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 19, 2012 0726       djohnson     Added SW history.
 * Jun 24, 2013 2106       djohnson     Add copy constructor.
 * Oct 30, 2013  2448      dhladky      Moved methods to TimeUtil.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@Table(name = "bandwidth_datasetupdate")
@SequenceGenerator(name = "BANDWIDTH_SEQ", sequenceName = "bandwidth_datasetupdate_seq", allocationSize = 1)
@DynamicSerialize
public class BandwidthDataSetUpdate implements IPersistableDataObject<Long>,
        Serializable, IDeepCopyable<BandwidthDataSetUpdate> {

    private static final long serialVersionUID = 20120723L;

    @Column
    @DynamicSerializeElement
    private Calendar dataSetBaseTime;

    @Id
    @Column(name = "identifier")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BANDWIDTH_SEQ")
    @DynamicSerializeElement
    private long id = BandwidthUtil.DEFAULT_IDENTIFIER;

    @Column
    @DynamicSerializeElement
    private Calendar updateTime;

    @Column
    @DynamicSerializeElement
    private String dataSetName;

    @Column
    @DynamicSerializeElement
    private String providerName;

    @Column
    @DynamicSerializeElement
    private String dataSetType;

    @Column
    @DynamicSerializeElement
    private String url;

    /**
     * Constructor.
     */
    public BandwidthDataSetUpdate() {
    }

    /**
     * Copy constructor.
     * 
     * @param bandwidthDataSetUpdate
     *            the instance to copy
     */
    public BandwidthDataSetUpdate(BandwidthDataSetUpdate bandwidthDataSetUpdate) {
        this.dataSetBaseTime = TimeUtil.newCalendar(bandwidthDataSetUpdate.dataSetBaseTime);
        this.dataSetName = bandwidthDataSetUpdate.dataSetName;
        this.dataSetType = bandwidthDataSetUpdate.dataSetType;
        this.id = bandwidthDataSetUpdate.id;
        this.providerName = bandwidthDataSetUpdate.providerName;
        this.updateTime = TimeUtil.newCalendar(bandwidthDataSetUpdate.updateTime);
        this.url = bandwidthDataSetUpdate.url;
    }

    /**
     * @return the dataSetBaseTime
     */
    public Calendar getDataSetBaseTime() {
        return dataSetBaseTime;
    }

    /**
     * @return the dataSetName
     */
    public String getDataSetName() {
        return dataSetName;
    }

    /**
     * @return the dataSetType
     */
    public String getDataSetType() {
        return dataSetType;
    }

    /**
     * @return the id
     */
    @Override
    public Long getIdentifier() {
        return Long.valueOf(id);
    }

    /**
     * @return the providerName
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * @return the updateTime
     */
    public Calendar getUpdateTime() {
        return updateTime;
    }

    /**
     * @param dataSetBaseTime
     *            the dataSetBaseTime to set
     */
    public void setDataSetBaseTime(Calendar dataSetBaseTime) {
        this.dataSetBaseTime = dataSetBaseTime;
    }

    /**
     * @param dataSetName
     *            the dataSetName to set
     */
    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    /**
     * @param dataSetType
     *            the dataSetType to set
     */
    public void setDataSetType(String dataSetType) {
        this.dataSetType = dataSetType;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setIdentifier(long identifier) {
        this.id = identifier;
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @param providerName
     *            the providerName to set
     */
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    /**
     * @param updateTime
     *            the updateTime to set
     */
    public void setUpdateTime(Calendar updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * @param url
     *            the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BandwidthDataSetUpdate copy() {
        return new BandwidthDataSetUpdate(this);
    }

}
