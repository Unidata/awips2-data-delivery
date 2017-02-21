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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
 * Meta Data Pattern object
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 19, 2017 6089       tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
@XmlRootElement(name = "metaDataPattern")
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class MetaDataPattern {
    @XmlAttribute(name = "name")
    @DynamicSerializeElement
    private String name;

    @XmlElement(name = "regex", type = String.class)
    @DynamicSerializeElement
    protected String regex;

    @XmlElement(name = "dateFormat", type = String.class)
    @DynamicSerializeElement
    protected String dateFormat;

    private Pattern pattern;

    @XmlElements({ @XmlElement(name = "group", type = PatternGroup.class) })
    @DynamicSerializeElement
    private List<PatternGroup> groups;

    Map<String, PatternGroup> groupMap;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
        pattern = Pattern.compile(regex);
    }

    public List<PatternGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<PatternGroup> groups) {
        this.groups = groups;
    }

    public Pattern getPattern() {
        if (pattern == null) {
            pattern = Pattern.compile(regex);
        }
        return pattern;
    }

    public Map<String, PatternGroup> getGroupMap() {
        if ((groupMap == null) || groupMap.isEmpty()) {
            Map<String, PatternGroup> newMap = new HashMap<>();
            for (PatternGroup pg : getGroups()) {
                newMap.put(pg.getName(), pg);
            }
            groupMap = newMap;
        }
        return groupMap;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

}
