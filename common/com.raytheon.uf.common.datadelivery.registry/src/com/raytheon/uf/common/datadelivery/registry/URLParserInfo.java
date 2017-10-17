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

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Object used to store URL parsing information for a given model. This
 * information is then used when parsing metadata of that model.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 12, 2012  1038     dhladky   Initial creation
 * Oct 04, 2012           dhladky   Initial creation of Periodicity
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Jan 07, 2013  1451     djohnson  Use TimeUtil.newGmtCalendar().
 * Nov 09, 2016  5988     tjensen   Add pattern and dataSetNaming overrides to
 *                                  collection
 * Oct 04, 2017  6465     tjensen   Remove unneeded fields. Renamed from Collection
 *
 * </pre>
 *
 * @author dhladky
 */

@XmlRootElement(name = "urlParserInfo")
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class URLParserInfo {

    @XmlAttribute(name = "name", required = true)
    @DynamicSerializeElement
    private String name;

    @XmlAttribute(name = "ignore")
    @DynamicSerializeElement
    private boolean ignore = false;

    @XmlElement(name = "seedUrl", required = true)
    @DynamicSerializeElement
    private String seedUrl;

    @XmlElement(name = "urlKey", required = true)
    @DynamicSerializeElement
    private String urlKey;

    @XmlElement(name = "dateFormat", required = true)
    @DynamicSerializeElement
    private String dateFormat = "yyyyMMdd";

    @XmlElement(name = "pattern")
    @DynamicSerializeElement
    private com.raytheon.uf.common.datadelivery.registry.Pattern pattern;

    @XmlElement(name = "dataSetNaming")
    @DynamicSerializeElement
    private DataSetNaming dataSetNaming;

    public URLParserInfo() {

    }

    /**
     * Created from seed scans. Used to parse URLs when crawling for DataSet
     * metadata.
     *
     * @param name
     * @param seedUrl
     * @param dateFormat
     */
    public URLParserInfo(String name, String seedUrl, String dateFormat) {
        this.name = name;
        this.seedUrl = seedUrl;
        this.dateFormat = dateFormat;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public String getName() {
        return name;
    }

    public String getSeedUrl() {
        return seedUrl;
    }

    /**
     * This is the FULL date used by the crawler
     *
     * @param inDate
     * @return
     */
    public String getUrlDate(Date inDate) {
        // we return a "blank" for collections that don't
        // have dates or date formats
        String urlDate = "";
        if (getDateFormat() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat();
            sdf.applyLocalizedPattern(getDateFormat());
            urlDate = sdf.format(inDate);
        }
        return urlDate;
    }

    public String getUrlKey() {
        return urlKey;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSeedUrl(String seedUrl) {
        this.seedUrl = seedUrl;
    }

    public void setUrlKey(String urlKey) {
        this.urlKey = urlKey;
    }

    public com.raytheon.uf.common.datadelivery.registry.Pattern getPattern() {
        return pattern;
    }

    public void setPattern(
            com.raytheon.uf.common.datadelivery.registry.Pattern pattern) {
        this.pattern = pattern;
    }

    public DataSetNaming getDataSetNaming() {
        return dataSetNaming;
    }

    public void setDataSetNaming(DataSetNaming dataSetNaming) {
        this.dataSetNaming = dataSetNaming;
    }
}
