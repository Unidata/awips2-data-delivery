package com.raytheon.uf.edex.datadelivery.retrieval.util;

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