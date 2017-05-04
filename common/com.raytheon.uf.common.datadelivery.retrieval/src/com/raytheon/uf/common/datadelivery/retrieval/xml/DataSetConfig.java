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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

/**
 * dataset config class for Service Config
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ---------------------------
 * Oct 20, 2012  1163     dhladky   Initial creation
 * Nov 07, 2013  2361     njensen   Remove ISerializableObject
 * Nov 09, 2016  5988     tjensen   Remove DataSetNaming
 * Mar 31, 2017  6186     rjpeter   Support incremental override.
 *
 * </pre>
 *
 * @author dhladky
 */
@XmlRootElement(name = "dataSetConfig")
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class DataSetConfig {

    @XmlElements({
            @XmlElement(name = "pattern", type = com.raytheon.uf.common.datadelivery.registry.Pattern.class) })
    private List<com.raytheon.uf.common.datadelivery.registry.Pattern> patterns;

    private Map<com.raytheon.uf.common.datadelivery.registry.Pattern, Pattern> patternMap = null;

    public DataSetConfig() {

    }

    /**
     * Gets the usable patterns for matching
     *
     * @return
     */
    public Map<com.raytheon.uf.common.datadelivery.registry.Pattern, Pattern> getPatternMap() {

        if (patternMap == null) {
            patternMap = new HashMap<>();
            for (com.raytheon.uf.common.datadelivery.registry.Pattern pat : getPatterns()) {
                patternMap.put(pat, Pattern.compile(pat.getRegex()));
            }
        }

        return patternMap;
    }

    /**
     * Gets the pattern list
     *
     * @return
     */
    public List<com.raytheon.uf.common.datadelivery.registry.Pattern> getPatterns() {
        if (patterns == null) {
            patterns = new ArrayList<>(0);
        }

        return patterns;
    }

    /**
     * Sets the patterns
     *
     * @param pattern
     */
    public void setPatterns(
            List<com.raytheon.uf.common.datadelivery.registry.Pattern> patterns) {
        this.patterns = patterns;
    }

    /**
     * Adds other to this definition, preferring other over this.
     *
     * @param other
     */
    public void combine(DataSetConfig other) {
        patternMap = null;

        if (this.patterns == null) {
            this.patterns = other.patterns;
        } else if (other.patterns != null && !other.patterns.isEmpty()) {
            this.patterns.addAll(0, other.patterns);
        }
    }
}
