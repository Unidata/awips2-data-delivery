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
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * 
 * Parameter Regexes XML object
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Dec 02, 2016  5988     tjensen   Initial creation
 * 
 * </pre>
 *
 * @author tjensen
 */

@XmlRootElement(name = "ParameterRegexes")
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class ParameterRegexes {

    /**  */
    @XmlElements({ @XmlElement(name = "levelRegex", type = String.class) })
    @DynamicSerializeElement
    private List<String> levelRegexes;

    @XmlElements({
            @XmlElement(name = "nameRegex", type = ParameterNameRegex.class) })
    @DynamicSerializeElement
    private List<ParameterNameRegex> nameRegexes;

    private List<Pattern> levelPatterns;

    public List<String> getLevelRegexes() {
        return levelRegexes;
    }

    public void setLevelRegexes(List<String> levelRegexes) {
        this.levelRegexes = levelRegexes;
    }

    public List<ParameterNameRegex> getNameRegexes() {
        return nameRegexes;
    }

    public void setNameRegexes(List<ParameterNameRegex> nameRegexes) {
        this.nameRegexes = nameRegexes;
    }

    public List<Pattern> getLevelPatterns() {

        if (levelPatterns == null) {
            levelPatterns = new ArrayList<>();
            for (String levelRegex : getLevelRegexes()) {
                levelPatterns.add(Pattern.compile("^" + levelRegex));
            }
        }
        return levelPatterns;
    }
}
