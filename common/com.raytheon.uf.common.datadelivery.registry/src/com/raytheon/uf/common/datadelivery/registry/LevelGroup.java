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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 *
 * Group of Levels for an AWIPS parameter
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
public class LevelGroup {

    /**
     * Name of the level these entries apply to. Used in display.
     */
    @XmlAttribute
    @DynamicSerializeElement
    private String name;

    /**
     * Units to describe all levels in this group
     */
    @XmlAttribute
    @DynamicSerializeElement
    private String units;

    /**
     * Key used to identify the master level to be used for this group.
     */
    @XmlAttribute
    @DynamicSerializeElement
    private String masterKey;

    /**
     * List of entries containing each parameter and specific level combination
     * applicable to this level type.
     */
    @XmlElements({ @XmlElement })
    @DynamicSerializeElement
    private List<ParameterLevelEntry> levels;

    /**
     * Optional flag for if this list should be displayed in reverse order when
     * sorted.
     */
    @XmlAttribute
    @DynamicSerializeElement
    private boolean reverseOrder;

    public LevelGroup() {

    }

    public LevelGroup(String name, String units) {
        this.name = name;
        this.units = units;
        this.levels = new ArrayList<>(1);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public List<ParameterLevelEntry> getLevels() {
        return levels;
    }

    public void setLevels(List<ParameterLevelEntry> levels) {
        this.levels = levels;
    }

    public void addLevel(ParameterLevelEntry newLevel) {
        if (levels == null) {
            this.levels = new ArrayList<>(1);
        }
        levels.add(newLevel);
    }

    public String getKey() {
        return ParameterUtils.buildKey(name, units);
    }

    public boolean isReverseOrder() {
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
}
