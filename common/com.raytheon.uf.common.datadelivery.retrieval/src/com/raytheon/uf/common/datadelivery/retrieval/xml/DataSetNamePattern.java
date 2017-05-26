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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

/**
 *
 * DataSetNamePattern XML object
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 9, 2017  6130       tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
@XmlAccessorType(XmlAccessType.NONE)
public class DataSetNamePattern {
    @XmlElements({ @XmlElement(name = "include") })
    private List<String> includeRegex;

    @XmlElements({ @XmlElement(name = "exclude") })
    private List<String> excludeRegex;

    private List<Pattern> includePatterns;

    private List<Pattern> excludePatterns;

    public List<Pattern> getIncludePatterns() {
        if (includePatterns == null || includePatterns.isEmpty()) {
            List<Pattern> newInPatterns = new ArrayList<>();
            for (String inRegex : includeRegex) {
                newInPatterns.add(Pattern.compile(inRegex));
            }
            includePatterns = newInPatterns;
        }
        return includePatterns;
    }

    public List<Pattern> getExcludePatterns() {
        if (excludeRegex == null || excludeRegex.isEmpty()) {
            return Collections.emptyList();
        }

        if (excludePatterns == null) {
            List<Pattern> newExPatterns = new ArrayList<>();
            for (String exRegex : excludeRegex) {
                newExPatterns.add(Pattern.compile(exRegex));
            }
            excludePatterns = newExPatterns;
        }
        return excludePatterns;
    }

    public boolean checkName(String dsName) {
        boolean matches = false;

        // If we don't have any include patterns, return false
        List<Pattern> inPats = getIncludePatterns();
        if (inPats == null || inPats.isEmpty()) {
            return matches;
        }

        /*
         * Loop over all include patterns. If we find a match, set matches to
         * true and break out since we just need one to match.
         */
        for (Pattern inPat : inPats) {
            Matcher in = inPat.matcher(dsName);
            if (in.matches()) {
                matches = true;
                break;
            }
        }

        /*
         * If we have exclude patterns, loop over all of them. If we find a
         * match, set matches to false and break out since if one matches we
         * exclude it.
         */
        List<Pattern> exPats = getExcludePatterns();
        if (exPats != null && !exPats.isEmpty()) {
            for (Pattern exPat : exPats) {
                if (exPat != null) {
                    Matcher ex = exPat.matcher(dsName);
                    if (ex.matches()) {
                        matches = false;
                        break;
                    }
                }
            }
        }

        return matches;
    }

}
