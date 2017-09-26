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
package com.raytheon.uf.edex.datadelivery.retrieval.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendap.dap.DAS;

/**
 * Link
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 7, 2017  6178       tgurney     Initial creation
 * Sep 25, 2017 6178       tgurney     Add getLinkKey()
 *
 * </pre>
 *
 * @author tgurney
 */

public class Link {

    private static final Pattern LINK_KEY_PATTERN = Pattern
            .compile(".*/([^\\.]+)(\\..+)?");

    private String url;

    private String subName;

    private Map<String, DAS> metadata = new HashMap<>();

    public Link() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSubName() {
        return subName;
    }

    public void setSubName(String subName) {
        this.subName = subName;
    }

    public Map<String, DAS> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, DAS> metadata) {
        this.metadata = metadata;
    }

    public String getLinkKey() {
        Matcher m = LINK_KEY_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return url;
    }

}
