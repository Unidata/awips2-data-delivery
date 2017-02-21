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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * 
 * Parameter Mapping XML object
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Dec 02, 2016  5988         tjensen   Initial creation
 * 
 * </pre>
 *
 * @author tjensen
 */

@XmlRootElement(name = "ParameterMapping")
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class ParameterMapping {

    @XmlAttribute(name = "id")
    @DynamicSerializeElement
    private String id;

    @XmlAttribute(name = "GrADs")
    @DynamicSerializeElement
    private String grads;

    @XmlAttribute(name = "AWIPS")
    @DynamicSerializeElement
    private String awips;

    /**  */
    @XmlElements({ @XmlElement(name = "dataSet", type = String.class) })
    @DynamicSerializeElement
    private List<String> dataSets;

    public String getGrads() {
        return grads;
    }

    public void setGrads(String grads) {
        this.grads = grads;
    }

    public String getAwips() {
        return awips;
    }

    public void setAwips(String awips) {
        this.awips = awips;
    }

    public List<String> getDataSets() {
        return dataSets;
    }

    public void setDataSets(List<String> dataSets) {
        this.dataSets = dataSets;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
