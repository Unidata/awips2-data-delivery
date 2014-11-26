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
package com.raytheon.uf.edex.datadelivery.bandwidth.dao;

import java.io.Serializable;
import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.raytheon.uf.common.dataplugin.persist.IPersistableDataObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.IDeepCopyable;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;

/**
 * Tracks amount of additional latency given to a Subscription
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 01, 2014 3550       ccody       Initial version
 * 
 * </pre>
 * 
 * @author ccody
 * @version 1.0
 */
@Entity
@Table(name = "dd_data_set_latency", uniqueConstraints = { @UniqueConstraint(columnNames = {
        "datasetname", "provider" }) })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DynamicSerialize
@SequenceGenerator(name = "DATA_SET_LATENCY_SEQ", sequenceName = "data_set_latency_seq", allocationSize = 1, initialValue = 1)
public class DataSetLatency implements Comparable<DataSetLatency>,
        IPersistableDataObject<Long>, Serializable,
        IDeepCopyable<DataSetLatency> {

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 713705044221370831L;

    @Id
    @Column(name = "identifier")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DATA_SET_LATENCY_SEQ")
    @DynamicSerializeElement
    private long id = BandwidthUtil.DEFAULT_IDENTIFIER;

    // Associated Data Set Name
    @Column(name = "datasetname", nullable = false)
    @DynamicSerializeElement
    private String dataSetName;

    // Associated Data Set Provider Name
    @Column(name = "provider", nullable = false)
    @DynamicSerializeElement
    private String providerName;

    // BandwidthAllocation/Subscription Base Reference Timestamp (long)
    @Column(name = "basereferencetime")
    @DynamicSerializeElement
    private Calendar baseReferenceTime;

    // private long baseRefTimestamp;

    // Extended Latency (Total) (in Minutes)
    @Column(name = "extendedlatency")
    @DynamicSerializeElement
    private int extendedLatency;

    public DataSetLatency() {
    }

    public DataSetLatency(String dataSetName, String providerName,
            int extendedLatency, Calendar baseRefTimestamp) {
        this.dataSetName = dataSetName;
        this.providerName = providerName;
        this.extendedLatency = extendedLatency;
        this.baseReferenceTime = baseReferenceTime;
    }

    /**
     * Copy constructor.
     * 
     * @param from
     *            the instance to copy from
     */
    public DataSetLatency(DataSetLatency from) {
        this.dataSetName = from.dataSetName;
        this.providerName = from.providerName;
        this.extendedLatency = from.extendedLatency;
        this.baseReferenceTime = from.baseReferenceTime;
        this.id = from.id;
    }

    public String getDataSetName() {
        return this.dataSetName;
    }

    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    public String getProviderName() {
        return this.providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public int getExtendedLatency() {
        return this.extendedLatency;
    }

    public void setExtendedLatency(int extendedLatency) {
        this.extendedLatency = extendedLatency;
    }

    public Calendar getBaseReferenceTime() {
        return this.baseReferenceTime;
    }

    public void setBaseReferenceTime(Calendar baseReferenceTime) {
        this.baseReferenceTime = baseReferenceTime;
    }

    public long getBaseRefTimestamp() {
        if (baseReferenceTime != null) {
            return (baseReferenceTime.getTimeInMillis());
        }

        return (0);
    }

    public void setBaseRefTimestamp(long baseRefTimestamp) {
        this.baseReferenceTime = TimeUtil.newCalendar();
        this.baseReferenceTime.setTimeInMillis(baseRefTimestamp);
    }

    public long getId() {
        return id;
    }

    @Override
    public Long getIdentifier() {
        return Long.valueOf(id);
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setIdentifier(Long identifier) {
        setId(identifier.longValue());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DataSetLatency [");
        sb.append(" id [").append(id);
        sb.append("] dataSetName [").append(dataSetName);
        sb.append("] providerName [").append(providerName);
        sb.append("] extendedLatency [").append(extendedLatency);
        sb.append("] baseRefTimestamp [").append(
                TimeUtil.formatCalendar(this.baseReferenceTime));
        sb.append("] ]");

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result
                + ((dataSetName == null) ? 0 : dataSetName.hashCode());
        result = prime * result
                + ((providerName == null) ? 0 : providerName.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DataSetLatency other = (DataSetLatency) obj;
        if (dataSetName != null) {
            if (dataSetName.equals(other.dataSetName) == false) {
                return false;
            }
        } else if (other.dataSetName != null) {
            return false;
        }
        if (providerName != null) {
            if (providerName.equals(other.providerName) == false) {
                return false;
            }
        } else if (other.providerName != null) {
            return false;
        }
        if (baseReferenceTime != null) {
            if (baseReferenceTime.equals(other.baseReferenceTime) == false) {
                return false;
            }
        } else if (other.baseReferenceTime != null) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(DataSetLatency o) {
        if (this.dataSetName.compareTo(o.dataSetName) > 0) {
            return 1;
        } else if (this.dataSetName.compareTo(o.dataSetName) < 0) {
            return -1;
        } else {
            if (this.providerName.compareTo(o.providerName) > 0) {
                return 1;
            } else if (this.providerName.compareTo(o.providerName) < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSetLatency copy() {
        return new DataSetLatency(this);
    }

    public long getDataSetLatencyValidUntil() {
        long baseRefTimestamp = 0;
        if (this.baseReferenceTime != null) {
            baseRefTimestamp = this.baseReferenceTime.getTimeInMillis();
        }
        return (baseRefTimestamp + (extendedLatency * TimeUtil.MILLIS_PER_MINUTE));
    }

    public boolean isDataSetLatencyValid(long now) {
        long validUntil = getDataSetLatencyValidUntil();
        if (now <= validUntil) {
            return (true);
        }
        return (false);
    }

}
