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
package com.raytheon.uf.common.datadelivery.registry;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.raytheon.uf.common.serialization.XmlGenericMapAdapter;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 *
 * Mapping all provider parameters and levels to a specific AWIPS parameter
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 14, 2017 6413       tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class ParameterGroup {

    /**
     * AWIPS name for a group of parameters. Corresponds to an abbreviation on
     * common com.raytheon.uf.common.parameter.Parameters
     */
    @XmlAttribute
    @DynamicSerializeElement
    private String abbrev;

    /**
     * Units for all parameters in this group
     */
    @XmlAttribute
    @DynamicSerializeElement
    private String units;

    /**
     * Contains all parameters grouped by the type of levels they are found at.
     */
    @DynamicSerializeElement
    @XmlJavaTypeAdapter(type = Map.class, value = XmlGenericMapAdapter.class)
    private Map<String, LevelGroup> groupedLevels;

    public ParameterGroup() {

    }

    public ParameterGroup(String abbrev, String units) {
        super();
        this.abbrev = abbrev;
        this.units = units;
        this.groupedLevels = new HashMap<>();
    }

    public ParameterGroup(ParameterGroup other) {
        super();
        this.abbrev = other.getAbbrev();
        this.units = other.getUnits();
        this.groupedLevels = other.getGroupedLevels();
    }

    public String getAbbrev() {
        return abbrev;
    }

    public void setAbbrev(String abbrev) {
        this.abbrev = abbrev;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public Map<String, LevelGroup> getGroupedLevels() {
        return groupedLevels;
    }

    public void setGroupedLevels(Map<String, LevelGroup> groupedLevels) {
        this.groupedLevels = groupedLevels;
    }

    public LevelGroup getLevelGroup(String levelKey) {
        return groupedLevels.get(levelKey);
    }

    public void putLevelGroup(LevelGroup lg) {
        groupedLevels.put(lg.getKey(), lg);
    }

    public String getKey() {
        return ParameterUtils.buildKey(abbrev, units);
    }

}
