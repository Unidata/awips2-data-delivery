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

import java.text.ParseException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.util.app.Version;

/**
 *
 * Version Data XML Objects
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 03, 2017  6130     tjensen   Initial creation
 * Aug 03, 2017  6352     tgurney   Moved parseVersion() to Version.fromString()
 *
 * </pre>
 *
 * @author tjensen
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class VersionData {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @XmlAttribute(name = "start")
    @DynamicSerializeElement
    private String start;

    @XmlAttribute(name = "end")
    @DynamicSerializeElement
    private String end;

    @XmlElement
    @DynamicSerializeElement
    private String route;

    private Version startVersion;

    private Version endVersion;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public boolean checkVersion(String version) throws ParseException {
        boolean matches = false;
        Version ver = Version.fromString(version);
        if (ver.compareTo(getStartVersion()) >= 0) {
            matches = true;

            // Only compare against end version if it is set
            if (getEndVersion() != null) {
                if (ver.compareTo(getEndVersion()) > 0) {
                    matches = false;
                }
            }
        }

        return matches;
    }

    public Version getStartVersion() {
        if (startVersion == null) {
            try {
                startVersion = Version.fromString(start);
            } catch (Exception e) {
                logger.error("Error setting startVersion from  '" + start + "'",
                        e);
            }
        }

        return startVersion;
    }

    public Version getEndVersion() {
        if (end != null && !"".equals(end) && endVersion == null) {
            try {
                endVersion = Version.fromString(end);
            } catch (Exception e) {
                logger.error("Error setting endVersion from  '" + end + "'", e);
            }
        }
        return endVersion;
    }

    public String validate() {
        StringBuilder sb = new StringBuilder();
        Version myStartVersion = getStartVersion();
        if (myStartVersion == null) {
            sb.append("Invalid start version of '" + getStart() + "'. ");
        }
        Version myEndVersion = getEndVersion();
        if (myStartVersion != null && myEndVersion != null
                && myStartVersion.compareTo(myEndVersion) > 0) {
            sb.append("Start version '" + getStart()
                    + "' is greater than end version '" + getEnd() + "'");
        }

        return sb.toString();
    }
}
