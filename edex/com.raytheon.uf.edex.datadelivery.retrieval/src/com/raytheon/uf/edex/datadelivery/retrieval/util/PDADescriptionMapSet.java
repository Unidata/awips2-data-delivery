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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * 
 * A JAXBable set of {@link PDADescriptionMap}s.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 11, 2016 5752       tjensen     Initial creation
 * Aug 18, 2016 5752       tjensen     Use correct xml tags
 * Oct 23, 2017 6185       bsteffen    Support dynamic mappings.
 * 
 * </pre>
 * 
 * @author tjensen
 */
@XmlRootElement(name = "pdaDescriptionMapSet")
@XmlAccessorType(XmlAccessType.NONE)
public class PDADescriptionMapSet {

    /**
     * List of Resolution Mappings from the XML.
     */
    @XmlElements({
            @XmlElement(name = "pdaDescriptionMap", type = PDADescriptionMap.class) })
    private List<PDADescriptionMap> maps;

    @XmlElements({
            @XmlElement(name = "pdaDynDescMap", type = PDADynDescMap.class) })
    private List<PDADynDescMap> dynMaps;

    private transient Map<String, String> cachedMapping;

    public List<PDADescriptionMap> getMaps() {
        return maps;
    }

    public void setMaps(List<PDADescriptionMap> maps) {
        this.maps = maps;
    }

    public List<PDADynDescMap> getDynMaps() {
        return dynMaps;
    }

    public void setDynMaps(List<PDADynDescMap> dynMaps) {
        this.dynMaps = dynMaps;
    }

    /**
     * Add the mappings from another set to this set. Entries in the other map
     * set will take priority over existing entries in this set.
     * 
     * @param other
     *            another PDADescriptionMapSet
     */
    public void extend(PDADescriptionMapSet other) {
        List<PDADescriptionMap> otherMaps = other.getMaps();
        if (otherMaps != null) {
            if (this.maps == null) {
                this.maps = new ArrayList<>();
            }
            this.maps.addAll(otherMaps);
        }
        List<PDADynDescMap> otherDynMaps = other.getDynMaps();
        if (otherDynMaps != null) {
            if (this.dynMaps == null) {
                this.dynMaps = new ArrayList<>();
            }
            this.dynMaps.addAll(0, otherDynMaps);
        }
        cachedMapping = null;
    }

    public boolean setMapping(String key, String description) {
        String test = doMapping(key);
        if (description.equals(test)) {
            return false;
        } else if (test == null) {
            if (this.maps == null) {
                this.maps = new ArrayList<>();
            }
            PDADescriptionMap map = new PDADescriptionMap();
            map.setKey(key);
            map.setDescription(description);
            this.maps.add(map);
        } else {
            for (PDADescriptionMap map : this.maps) {
                if (map.getKey().equals(key)) {
                    map.setDescription(description);
                    break;
                }
            }
        }
        cachedMapping.put(key, description);
        return true;
    }

    public String doMapping(String key) {
        Map<String, String> mapping = cachedMapping;
        if (mapping == null) {
            mapping = new HashMap<>();
            if (maps != null) {
                for (PDADescriptionMap map : maps) {
                    mapping.put(map.getKey(), map.getDescription());
                }
            }
            cachedMapping = mapping;
        }
        return mapping.get(key);
    }

    public String doDynamicMapping(String key, String resolution,
            ReferencedEnvelope area) {
        if (dynMaps == null) {
            return null;
        }
        for (PDADynDescMap map : dynMaps) {
            if (map.matches(key, resolution, area)) {
                return map.getDescription();
            }
        }

        return null;
    }
}
