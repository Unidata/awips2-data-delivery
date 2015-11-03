package com.raytheon.uf.edex.datadelivery.retrieval.metadata;

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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import opendap.dap.DAS;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Link object
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 17, 2011    218      dhladky     Initial creation
 * Jul 24, 2012    955      djohnson    Use Map instead of HashMap.
 * Sep 10, 2012 1154        djohnson    Add JAXB annotations.
 * Jul 08, 2014   3120      dhladky     More generic
 * Apr 12, 2015  4400       dhladky     Upgraded to DAP2
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

@XmlRootElement(name = "link")
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class Link {

    @XmlAttribute(name = "name")
    @DynamicSerializeElement
    private String name;

    @XmlAttribute(name = "url")
    @DynamicSerializeElement
    private String url;

    @Transient
    private Map<String, DAS> links = new HashMap<String, DAS>();

    public Link() {

    }

    public Link(String name, String url) {
        this.url = url;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, DAS> getLinks() {
        return links;
    }

    public void setLinks(Map<String, DAS> map) {
        this.links = map;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Link) {
            Link other = (Link) obj;

            EqualsBuilder eqBuilder = new EqualsBuilder();
            eqBuilder.append(this.name, other.name);
            eqBuilder.append(this.url, other.url);

            return eqBuilder.isEquals();
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hcBuilder = new HashCodeBuilder();
        hcBuilder.append(this.name);
        hcBuilder.append(this.url);

        return hcBuilder.toHashCode();
    }
}
