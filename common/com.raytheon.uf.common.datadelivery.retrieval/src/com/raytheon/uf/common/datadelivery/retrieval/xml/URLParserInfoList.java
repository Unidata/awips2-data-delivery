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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.datadelivery.registry.URLParserInfo;

/**
 *
 * List of URLParserInfo objects for XML.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Oct 04, 2017  6465     tjensen   Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
@XmlRootElement(name = "urlParserInfoList")
@XmlAccessorType(XmlAccessType.NONE)
public class URLParserInfoList {

    @XmlElements({
            @XmlElement(name = "urlParserInfo", type = URLParserInfo.class) })
    private List<URLParserInfo> urlParserInfoList;

    public List<URLParserInfo> getUrlParserInfoList() {
        return urlParserInfoList;
    }

    public void setUrlParserInfoList(List<URLParserInfo> urlParserInfoList) {
        this.urlParserInfoList = urlParserInfoList;
    }

}
