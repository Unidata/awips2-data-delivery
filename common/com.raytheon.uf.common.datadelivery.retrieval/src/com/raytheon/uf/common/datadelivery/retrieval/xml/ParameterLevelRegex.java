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

import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 *
 * Parameter Level Regex XML object
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Dec 02, 2016  5988     tjensen   Initial creation
 * Mar 02, 2017  5988     tjensen   Add information to be used for level parsing
 * Sep 12, 2017  6413     tjensen   Added providerLevels and reverseOrder flags
 *
 * </pre>
 *
 * @author tjensen
 */

@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class ParameterLevelRegex implements Comparable<ParameterLevelRegex> {

    @XmlAttribute(name = "id")
    @DynamicSerializeElement
    private String id;

    @XmlAttribute(name = "order")
    @DynamicSerializeElement
    private float order;

    @XmlAttribute(name = "pattern")
    @DynamicSerializeElement
    private String regex;

    @XmlAttribute(name = "level")
    @DynamicSerializeElement
    private String level;

    @XmlAttribute(name = "masterKey")
    @DynamicSerializeElement
    private String masterKey;

    @XmlAttribute(name = "levelGroup")
    @DynamicSerializeElement
    private String levelGroup;

    @XmlAttribute(name = "units")
    @DynamicSerializeElement
    private String units;

    @XmlAttribute(name = "providerLevels")
    @DynamicSerializeElement
    private boolean providerLevels;

    @XmlAttribute(name = "reverseOrder")
    @DynamicSerializeElement
    private boolean reverseOrder;

    @XmlAttribute(name = "matchAnywhere")
    @DynamicSerializeElement
    private boolean matchAnywhere;

    private Pattern pattern;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public Pattern getPattern() {
        if (pattern == null) {
            if (getMatchAnywhere()) {
                pattern = Pattern.compile(getRegex());
            } else {
                pattern = Pattern.compile("^" + getRegex());
            }
        }
        return pattern;
    }

    @Override
    public int compareTo(ParameterLevelRegex other) {
        int orderDiff = Float.compare(order, other.getOrder());
        if (orderDiff != 0) {
            return orderDiff;
        }
        return id.compareTo(other.getId());
    }

    public float getOrder() {
        return order;
    }

    public void setOrder(float order) {
        this.order = order;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevelGroup() {
        return levelGroup;
    }

    public void setLevelGroup(String levelGroup) {
        this.levelGroup = levelGroup;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public boolean hasProviderLevels() {
        return providerLevels;
    }

    public void setProviderLevels(boolean providerLevels) {
        this.providerLevels = providerLevels;
    }

    public boolean getReverseOrder() {
        return reverseOrder;
    }

    public void setReverseOrder(boolean reverseOrder) {
        this.reverseOrder = reverseOrder;
    }

    public String getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(String masterKey) {
        this.masterKey = masterKey;
    }

    public boolean getMatchAnywhere() {
        return matchAnywhere;
    }

    public void setMatchAnywhere(boolean matchAnywhere) {
        this.matchAnywhere = matchAnywhere;
    }
}
