package com.raytheon.uf.edex.datadelivery.retrieval.util;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * 
 * Contains a mapping of PDA keys to descriptions
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 15, 2016 5752        tjensen     Initial creation
 * 
 * </pre>
 * 
 * @author tjensen
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
public class PDADescriptionMap {
    /** The key for the resolution */
    @XmlElement
    private String key;

    /** The description of the resolution */
    @XmlElement
    private String description;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
