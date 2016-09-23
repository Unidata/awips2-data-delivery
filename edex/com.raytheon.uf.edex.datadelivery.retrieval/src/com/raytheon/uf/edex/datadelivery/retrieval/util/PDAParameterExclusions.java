package com.raytheon.uf.edex.datadelivery.retrieval.util;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * Contains a list of PDA parameters to be excluded from storing in the
 * registry.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Aug 16, 2016  5752     tjensen   Initial creation
 * 
 * </pre>
 * 
 * @author tjensen
 * @version 1.0
 */
@XmlRootElement(name = "pdaParameterExclusions")
@XmlAccessorType(XmlAccessType.NONE)
public class PDAParameterExclusions {

    /** The server located upstream from this server */
    @XmlElements({ @XmlElement(name = "parameter") })
    private Set<String> parameterExclusions;

    public Set<String> getParameterExclusions() {
        if (parameterExclusions == null) {
            parameterExclusions = new HashSet<>();
        }
        return parameterExclusions;
    }

    public void setParameterExclusions(Set<String> parameterExclusions) {
        this.parameterExclusions = parameterExclusions;
    }

    public void addParameterExclusion(String registryId) {
        this.getParameterExclusions().add(registryId);
    }

    public void removeParameterExclusion(String registryId) {
        this.getParameterExclusions().remove(registryId);
    }
}