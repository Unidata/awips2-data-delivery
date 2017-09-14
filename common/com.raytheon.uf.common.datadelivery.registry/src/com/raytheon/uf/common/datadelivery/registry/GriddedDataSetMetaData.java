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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.raytheon.uf.common.registry.annotations.RegistryObject;
import com.raytheon.uf.common.registry.annotations.SlotAttribute;
import com.raytheon.uf.common.serialization.XmlGenericMapAdapter;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Gridded Meta Data object
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 20, 2011  218      dhladky   Initial creation
 * Jul 24, 2012  955      djohnson  Add {@link RegistryObject}.
 * Aug 20, 2012  743      djohnson  Store cycle in a slot.
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Sep 30, 2013  1797     dhladky   Generics
 * Dec 20, 2013  2636     mpduff    Add equals/hashcode.
 * Apr 05, 2017  1045     tjensen   Add Coverage generics DataSetMetaData
 * May 24, 2017  6186     rjpeter   Overrode satisfiesSubscription to handle
 *                                  cycle checking.
 * Aug 29, 2017  6186     rjpeter   Fix version compatibility with cycle
 *                                  checking
 * Sep 20, 2017  6413     tjensen   Added provider levels
 *
 * </pre>
 *
 * @author dhladky
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso(OpenDapGriddedDataSetMetaData.class)
public abstract class GriddedDataSetMetaData
        extends DataSetMetaData<GriddedTime, GriddedCoverage> {

    public static final String CYCLE_SLOT = "cycle";

    public static final int NO_CYCLE = -1;

    public GriddedDataSetMetaData() {
    }

    /**
     * map of the level types available in set
     */
    @DynamicSerializeElement
    @XmlJavaTypeAdapter(type = Map.class, value = XmlGenericMapAdapter.class)
    private Map<DataLevelType, Levels> levelTypes = new HashMap<>();

    /**
     * map of the level types available in set
     */
    @DynamicSerializeElement
    @XmlAttribute
    @SlotAttribute(CYCLE_SLOT)
    private int cycle = NO_CYCLE;

    /**
     * List of levels from the provider tied to this DataSet's collectionName
     * (ie. 'Pressure Level' levels)
     */
    @DynamicSerializeElement
    @XmlElements({ @XmlElement })
    private List<Double> providerLevels;

    public void setLevelTypes(Map<DataLevelType, Levels> levelTypes) {
        this.levelTypes = levelTypes;
    }

    public Map<DataLevelType, Levels> getLevelTypes() {
        return levelTypes;
    }

    public void addLevelType(DataLevelType type, Levels levels) {
        if (levelTypes == null) {
            levelTypes = new HashMap<>();
        }
        if (!levelTypes.containsKey(type)) {
            levelTypes.put(type, levels);
        }
    }

    /**
     * Deprecated, use GriddedTime.cycleTimes.
     *
     * @param cycle
     */
    @Deprecated
    public void setCycle(int cycle) {
        this.cycle = cycle;
    }

    /**
     * Deprecated, use GriddedTime.cycleTimes.
     *
     * @return
     */
    @Deprecated
    public int getCycle() {
        List<Integer> cycleTimes = null;
        GriddedTime gTime = getTime();
        if (gTime != null) {
            cycleTimes = gTime.getCycleTimes();
        }
        if (cycleTimes == null || cycleTimes.isEmpty()) {
            /*
             * fall back to the cycle field for version compatibility
             */
            return cycle;
        }

        return cycleTimes.get(0);
    }

    @Override
    public String satisfiesSubscription(
            Subscription<GriddedTime, GriddedCoverage> sub) throws Exception {
        String rval = super.satisfiesSubscription(sub);

        if (rval == null) {
            List<Integer> subCycleTimes = sub.getTime().getCycleTimes();
            List<Integer> dsCycleTimes = getTime().getCycleTimes();

            /*
             * If the subscription doesn't have cycle times subscribed to, then
             * add the NO_CYCLE marker cycle
             */
            boolean foundCycle = false;
            if (dsCycleTimes == null || dsCycleTimes.isEmpty()) {
                // check cyle for version compatibility
                if (cycle == NO_CYCLE) {
                    foundCycle = subCycleTimes.isEmpty();
                } else {
                    foundCycle = subCycleTimes.contains(cycle);
                }
            } else {
                foundCycle = !Collections.disjoint(subCycleTimes,
                        getTime().getCycleTimes());
            }

            if (!foundCycle) {
                rval = sub.getName() + " is not subscribed to cycle [" + cycle
                        + "]";
            }
        }

        return rval;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + cycle;
        result = prime * result
                + ((levelTypes == null) ? 0 : levelTypes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        GriddedDataSetMetaData other = (GriddedDataSetMetaData) obj;
        if (cycle != other.cycle) {
            return false;
        }
        if (levelTypes == null) {
            if (other.levelTypes != null) {
                return false;
            }
        } else if (!levelTypes.equals(other.levelTypes)) {
            return false;
        }
        return true;
    }

    public List<Double> getProviderLevels() {
        return providerLevels;
    }

    public void setProviderLevels(List<Double> providerLevels) {
        this.providerLevels = providerLevels;
    }

    public int findProviderLevelIndex(double value) {
        int index = 0;
        for (Double level : getProviderLevels()) {
            if (level == value) {
                break;
            }
            index++;
        }
        return index;
    }
}
