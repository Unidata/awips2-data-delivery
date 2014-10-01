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
package com.raytheon.uf.viz.datadelivery.subscription.subset.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.common.util.FileUtil;

/**
 * Point time xml object.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 20, 2014   3121     dhladky      Initial creation.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class PDATimeXML extends TimeXML {
   
    
    @XmlElement(name = "latestData")
    protected boolean latestData;
    
    @XmlElements({ @XmlElement(name = "time", type = String.class) })
    protected List<String> timeList = new ArrayList<String>();

    /**
     * @return the latestData
     */
    public boolean isLatestData() {
        return latestData;
    }
    
     /**
     * @return the fcstHour
     */
    public List<String> getTimes() {
        return timeList;
    }

    /**
     * @param times
     *            the times to set
     */
    public void setTimes(List<String> times) {
        this.timeList = times;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getPreviewString() {
        StringBuilder sb = new StringBuilder();

        sb.append(FileUtil.EOL);
        if (latestData) {
            sb.append("Requesting Latest Data");
        } else {
            // TODO not sure what to do here?
            //sb.append(getNonLatestData());
        }
        sb.append(FileUtil.EOL);

        if (!CollectionUtil.isNullOrEmpty(timeList)) {
            sb.append("Times:").append(FileUtil.EOL);
            for (String fcst : timeList) {
                sb.append(" ").append(fcst);
            }
            sb.append(FileUtil.EOL);
        }
        return sb.toString();
    }
   
}

