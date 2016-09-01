package com.raytheon.uf.edex.datadelivery.retrieval.util;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * 
 * Contains a map of parameters to their descriptions
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 11, 2016 5752       tjensen     Initial creation
 * 
 * </pre>
 * 
 * @author tjensen
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
public class PDADataSetNameMap {

    /** The parameter of the DataSet */
    @XmlElement
    private String parameter;

    /** The description of the DataSet */
    @XmlElement
    private String description;

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
