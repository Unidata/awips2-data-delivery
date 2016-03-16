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
package com.raytheon.uf.edex.datadelivery.harvester.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.Link;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.LinkStore;

import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.url.WebURL;

/**
 * Harvester
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 20, 2011 218        dhladky     Initial creation
 * Mar 16, 2016 3919       tjensen     Cleanup unneeded interfaces
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class MainSequenceHarvester extends WebCrawler {

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(MainSequenceHarvester.class);

    private final Pattern FILTERS;

    private final String topurl;

    private final String name;

    private final LinkStore links;

    private List<Pattern> IGNORE_FILTERS = null;

    public MainSequenceHarvester(String name, String topurl, String pageKey,
            LinkStore links, List<String> ignores) {
        this.name = name;
        this.topurl = topurl;
        this.links = links;
        FILTERS = Pattern.compile(pageKey);

        // These are one's we are specifically ignoring
        if (ignores != null && ignores.size() > 0) {
            IGNORE_FILTERS = new ArrayList<Pattern>();
            for (String ignore : ignores) {
                if (ignore != null && !ignore.equals("")) {
                    IGNORE_FILTERS.add(Pattern.compile(ignore));
                }
            }
        }
    }

    /**
     * You should implement this function to specify whether the given url
     * should be crawled or not (based on your crawling logic).
     */
    @Override
    public boolean shouldVisit(WebURL url) {
        String href = url.getURL();

        // Collections we don't care about
        if (IGNORE_FILTERS != null && IGNORE_FILTERS.size() > 0) {
            for (Pattern pat : IGNORE_FILTERS) {
                if (pat.matcher(href).find()) {
                    // URL contains a (name)collection we wish to ignore
                    if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                        statusHandler
                                .debug("Refusing to traverse, IGNORE pattern: "
                                        + pat.toString() + " " + href);
                    }

                    return false;
                }
            }
        }

        if (FILTERS.matcher(href).find()) {
            if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                statusHandler.debug("Storing url: " + href);
            }

            // found link we wanted, store it and don't crawl this URL
            storeLink(url);
            return false;
        }

        if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
            statusHandler.debug("Traverse url? " + href.startsWith(topurl)
                    + " " + href);
        }

        return href.startsWith(topurl);
    }

    public void storeLink(WebURL url) {
        Link link = new Link(name, url.getURL());
        links.addLink(url.getURL(), link);
    }
}