package com.raytheon.uf.edex.datadelivery.retrieval.util;

import java.util.regex.Pattern;

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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.raytheon.uf.common.geospatial.adapter.JTSEnvelopeAdapter;
import com.vividsolutions.jts.geom.Envelope;

/**
 * 
 * Contains data to map a key, resolution, and area to a specific description.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Oct 23, 2017  6185     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
@XmlAccessorType(XmlAccessType.NONE)
public class PDADynDescMap {

    /** The key for the resolution */
    @XmlElement
    private String key;

    /** The key for the resolution */
    @XmlElement
    private String resolution;

    @XmlElement
    @XmlJavaTypeAdapter(value = JTSEnvelopeAdapter.class)
    private Envelope area;

    /** The description of the resolution */
    @XmlElement
    private String description;

    @XmlElement
    private double matchPercent = 95.0;

    private transient Pattern keyPattern;

    private transient Pattern resolutionPattern;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Envelope getArea() {
        return area;
    }

    public void setArea(Envelope area) {
        this.area = area;
    }

    public double getMatchPercent() {
        return matchPercent;
    }

    public void setMatchPercent(double matchPercent) {
        this.matchPercent = matchPercent;
    }

    private Pattern getKeyPattern() {
        if (this.keyPattern == null) {
            this.keyPattern = Pattern.compile(key);
        }
        return this.keyPattern;
    }

    private Pattern getResolutionPattern() {
        if (this.resolutionPattern == null) {
            this.resolutionPattern = Pattern.compile(resolution);
        }
        return this.resolutionPattern;
    }

    public boolean matches(String key, String resolution,
            ReferencedEnvelope area) {

        if (this.key != null && !getKeyPattern().matcher(key).matches()) {
            return false;
        }
        if (this.resolution != null
                && !getResolutionPattern().matcher(resolution).matches()) {
            return false;
        }
        if (this.area != null) {
            double intersection = this.area.intersection(area).getArea();
            boolean overThis = 100 * intersection
                    / this.area.getArea() > matchPercent;
            boolean overOther = 100 * intersection
                    / area.getArea() > matchPercent;
            return overThis && overOther;
        }
        return true;
    }

}
