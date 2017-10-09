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

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.time.util.TimeUtil;

/**
 * Object that stores information about provider levels for a single data
 * collection. Information is also saved to a configuration file. Updated
 * periodically to remain in sync with the provider.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Mar 01, 2012           jpiatt    Initial creation.
 * Nov 07, 2013  2361     njensen   Remove ISerializableObject
 * Oct 12, 2017  6440     bsteffen  Refresh level lookups.
 * 
 * </pre>
 * 
 * @author jpiatt
 */
@XmlRootElement(name = "LevelLookup")
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class LevelLookup {

    private static final long EXPIRE_INTERVAL = Long
            .getLong("datadelivery.levellookup.refresh.hours", 24)
            * TimeUtil.MILLIS_PER_HOUR;

    @XmlAttribute
    @DynamicSerializeElement
    protected Date lastChecked;

    @XmlElement(name = "Level", type = Double.class)
    @DynamicSerializeElement
    protected List<Double> levelXml;

    public List<Double> getLevelXml() {
        return levelXml;
    }

    public void setLevelXml(List<Double> levelXml) {
        this.levelXml = levelXml;
    }

    public Date getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(Date lastChecked) {
        this.lastChecked = lastChecked;

    }

    public boolean isCurrent() {
        if (lastChecked == null) {
            return false;
        }
        long expireTime = lastChecked.getTime() + EXPIRE_INTERVAL;
        return expireTime > System.currentTimeMillis();
    }

}
