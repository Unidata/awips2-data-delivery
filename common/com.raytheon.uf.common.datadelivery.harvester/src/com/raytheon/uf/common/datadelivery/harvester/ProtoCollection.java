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
package com.raytheon.uf.common.datadelivery.harvester;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Defines a Proto-Collection that temporarily stores information about a data
 * collection as the crawler runs. This information is then used to create
 * Collection objects after the crawl is finished.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------------------
 * Oct 06, 2012  1038     dhladky   Initial creation
 * Feb 28, 2017  5988     tjensen   Improve error handling on getPeriodicity
 * Oct 04, 2017  6465     tjensen   Remove periodicity. Change date functions
 *
 * </pre>
 *
 * @author dhladky
 */

public class ProtoCollection {

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(ProtoCollection.class);

    private Map<Integer, List<Date>> dates = new LinkedHashMap<>();

    private Map<Integer, SimpleDateFormat> formats = new LinkedHashMap<>();

    private String seedUrl;

    private String url;

    private String urlKey;

    // We grab the name and a sample url
    public ProtoCollection(String seedUrl, String url) {
        this.seedUrl = seedUrl;
        this.url = url;
    }

    /**
     * Standard collection naming format
     *
     * @return
     */
    public String getCollectionName() {
        return getSeedUrl().replaceAll("/", "_");
    }

    /**
     * Build the expected date URL portions
     *
     * @return
     */
    public String getDateFormatString() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (SimpleDateFormat format : getFormats().values()) {
            sb.append(format.toPattern());
            if (i < formats.size() - 1) {
                sb.append("/");
            }
            i++;
        }

        return sb.toString();
    }

    public Map<Integer, List<Date>> getDates() {
        return dates;
    }

    public List<String> getSortedDateStrings() {
        List<Date> dateList = null;
        List<String> dateStrings = new ArrayList<>();

        int maxDepth = getMaxDepthFormat();
        if (maxDepth >= 0) {
            dateList = getDates().get(maxDepth);
            Collections.sort(dateList);

            SimpleDateFormat sdf = new SimpleDateFormat();
            sdf.applyLocalizedPattern(getDateFormatString());
            for (Date date : dateList) {
                dateStrings.add(sdf.format(date));
            }
        }
        return dateStrings;
    }

    public Map<Integer, SimpleDateFormat> getFormats() {
        return formats;
    }

    /**
     * Finds the maximum format depth
     *
     * @return
     */
    public int getMaxDepthFormat() {
        int maxDepth = -1;
        for (Integer i : formats.keySet()) {
            if (i > maxDepth) {
                maxDepth = i;
            }
        }
        return maxDepth;
    }

    public String getSeedUrl() {
        return seedUrl;
    }

    public String getUrl() {
        return url;
    }

    public String getUrlKey() {
        return urlKey;
    }

    public void setDates(Map<Integer, List<Date>> dates) {
        this.dates = dates;
    }

    public void setFormats(Map<Integer, SimpleDateFormat> formats) {
        this.formats = formats;
    }

    public void setSeedUrl(String seedUrl) {
        this.seedUrl = seedUrl;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUrlKey(String urlKey) {
        this.urlKey = urlKey;
    }
}
