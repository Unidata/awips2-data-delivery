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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import org.apache.commons.lang3.math.NumberUtils;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 *
 * Information for a specific level of a provider parameter
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
public class ParameterLevelEntry implements Comparable<ParameterLevelEntry> {

    /**
     * Name of the parameter used by the provider. Used for retrievals.
     */
    @XmlAttribute
    @DynamicSerializeElement
    private String providerName;

    /**
     * Description of the parameter
     */
    @XmlAttribute
    @DynamicSerializeElement
    private String description;

    /**
     * Description of the base level for this entry. Optional if this parameter
     * doesn't label its level.
     */
    @XmlAttribute
    @DynamicSerializeElement
    private String levelOne;

    /**
     * Description of the secondary level for this entry. Optional if this
     * parameter doesn't label its level or if this level is not a layer
     * (range).
     */
    @XmlAttribute
    @DynamicSerializeElement
    private String levelTwo;

    public ParameterLevelEntry() {

    }

    public ParameterLevelEntry(String providerName, String description,
            String levelOne) {
        this(providerName, description, levelOne, null);
    }

    public ParameterLevelEntry(String providerName, String description,
            String levelOne, String levelTwo) {
        super();
        this.providerName = providerName;
        this.description = description;
        this.levelOne = levelOne;
        this.levelTwo = levelTwo;
    }

    public ParameterLevelEntry(ParameterLevelEntry other) {
        super();
        this.providerName = other.getProviderName();
        this.description = other.getDescription();
        this.levelOne = other.getLevelOne();
        this.levelTwo = other.getLevelTwo();
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLevelOne() {
        return levelOne;
    }

    public void setLevelOne(String levelOne) {
        this.levelOne = levelOne;
    }

    public String getLevelTwo() {
        return levelTwo;
    }

    public void setLevelTwo(String levelTwo) {
        this.levelTwo = levelTwo;
    }

    public String getDisplayString() {
        String rval = null;
        if (levelOne != null) {
            if (levelTwo != null) {
                rval = levelOne + " - " + levelTwo;
            } else {
                rval = levelOne;
            }
        }
        return rval;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        result = prime * result
                + ((levelOne == null) ? 0 : levelOne.hashCode());
        result = prime * result
                + ((levelTwo == null) ? 0 : levelTwo.hashCode());
        result = prime * result
                + ((providerName == null) ? 0 : providerName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ParameterLevelEntry other = (ParameterLevelEntry) obj;
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (levelOne == null) {
            if (other.levelOne != null) {
                return false;
            }
        } else if (!levelOne.equals(other.levelOne)) {
            return false;
        }
        if (levelTwo == null) {
            if (other.levelTwo != null) {
                return false;
            }
        } else if (!levelTwo.equals(other.levelTwo)) {
            return false;
        }
        if (providerName == null) {
            if (other.providerName != null) {
                return false;
            }
        } else if (!providerName.equals(other.providerName)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(ParameterLevelEntry other) {
        String l1 = getLevelOne();
        String l2 = getLevelTwo();
        String ol1 = other.getLevelOne();
        String ol2 = other.getLevelTwo();

        if (l1 == null || "".equals(l1)) {
            if (ol1 == null || "".equals(ol1)) {
                // neither have labeled levels
                return 0;
            }
            // other has a label
            return -1;
        }
        if (ol1 == null || "".equals(ol1)) {
            // this has a label, but other does not
            return 1;
        }

        // both have a label for level 1
        if (l2 == null || "".equals(l2)) {
            if (ol2 == null || "".equals(ol2)) {
                // both have a single level
                if (NumberUtils.isNumber(l1)) {
                    if (NumberUtils.isNumber(ol1)) {
                        // both levels are valid numbers
                        double l1d = Double.parseDouble(l1);
                        double ol1d = Double.parseDouble(ol1);
                        return Double.compare(l1d, ol1d);
                    }
                    // this is valid number. other is not
                    return -1;
                }
                if (NumberUtils.isNumber(ol1)) {
                    // other is valid number. this is not
                    return 1;
                }
                // neither are valid numbers valid numbers
                return l1.compareTo(ol1);
            }
            // this is a single level, other is a layer
            return -1;
        }
        if (ol2 == null || "".equals(ol2)) {
            // this is a layer, other is a single level
            return 1;
        }
        // both are a layer
        if (NumberUtils.isNumber(l1)) {
            if (NumberUtils.isNumber(ol1)) {
                // both levels are valid numbers
                double l1d = Double.parseDouble(l1);
                double ol1d = Double.parseDouble(ol1);
                int l1c = Double.compare(l1d, ol1d);
                if (l1c == 0) {
                    // level one matches, compare level two
                    if (NumberUtils.isNumber(l2)) {
                        if (NumberUtils.isNumber(ol2)) {
                            // both levels are valid numbers
                            double l2d = Double.parseDouble(l2);
                            double ol2d = Double.parseDouble(ol2);
                            return Double.compare(l2d, ol2d);
                        }
                        // this is valid number. other is not
                        return -1;
                    }
                    if (NumberUtils.isNumber(ol2)) {
                        // other is valid number. this is not
                        return 1;
                    }
                    // neither are valid numbers valid numbers
                    return l2.compareTo(ol2);
                }
                return l1c;
            }
            // this is valid number. other is not
            return -1;
        }
        if (NumberUtils.isNumber(ol1)) {
            // other is valid number. this is not
            return 1;
        }
        // neither are valid numbers
        int l1c = l1.compareTo(ol1);
        if (l1c == 0) {
            // level one matches, compare level two
            // if we compared level 1 as strings, do the same for level 2
            return l2.compareTo(ol2);
        }

        return 0;
    }

}
