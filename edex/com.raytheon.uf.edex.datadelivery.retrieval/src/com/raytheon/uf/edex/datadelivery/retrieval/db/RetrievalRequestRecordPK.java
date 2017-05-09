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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.dataplugin.persist.IPersistableDataObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

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
 * Feb 11, 2013  1543     djohnson  Override equals/hashCode to remove Hibernate
 *                                  warning.
 * Feb 15, 2013  1543     djohnson  Add JAXB annotations.
 * Jan 30, 2014  2686     dhladky   refactor of retrieval.
 * Apr 17, 2017  6186     rjpeter   Add url column
 *
 * </pre>
 *
 * @author rjpeter
 */
@Embeddable
@DynamicSerialize
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class RetrievalRequestRecordPK implements
        IPersistableDataObject<RetrievalRequestRecordPK>, Serializable {
    private static final long serialVersionUID = 1L;

    @Column
    @DynamicSerializeElement
    @XmlAttribute
    private String url;

    @Column
    @DynamicSerializeElement
    @XmlAttribute
    private String subscriptionName;

    @Column
    @DynamicSerializeElement
    @XmlAttribute
    private int index;

    public RetrievalRequestRecordPK() {
    }

    public RetrievalRequestRecordPK(String serialized) {
        if (serialized != null) {
            String[] tokens = serialized.split("/", 3);
            if (tokens.length != 3) {
                throw new IllegalArgumentException(
                        "Incorrect format for RetrievalRequestRecorkPK. Expected [<url>/<subscriptionName>/<index>] received ["
                                + serialized + "]");
            }
            url = tokens[0];
            subscriptionName = tokens[1];
            index = Integer.parseInt(tokens[2]);
        }
    }

    public RetrievalRequestRecordPK(String url, String subscriptionName,
            int index) {
        this.url = url;
        this.subscriptionName = subscriptionName;
        this.index = index;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public RetrievalRequestRecordPK getIdentifier() {
        return this;
    }

    @Override
    public String toString() {
        return url + "/" + subscriptionName + "/" + index;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + index;
        result = prime * result + ((subscriptionName == null) ? 0
                : subscriptionName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RetrievalRequestRecordPK other = (RetrievalRequestRecordPK) obj;
        if (url == null) {
            if (other.url != null) {
                return false;
            }
        } else if (!url.equals(other.url)) {
            return false;
        }
        if (index != other.index) {
            return false;
        }
        if (subscriptionName == null) {
            if (other.subscriptionName != null) {
                return false;
            }
        } else if (!subscriptionName.equals(other.subscriptionName)) {
            return false;
        }
        return true;
    }
}
