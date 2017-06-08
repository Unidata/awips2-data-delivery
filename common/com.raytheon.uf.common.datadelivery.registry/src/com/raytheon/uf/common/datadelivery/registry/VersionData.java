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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

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

    private static final Pattern versionPattern = Pattern
            .compile("^(\\d+)(\\.(\\d+))?(\\.(\\d+))?");

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
        Version ver = parseVersion(version);
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

    private static Version parseVersion(String version) throws ParseException {
        Version newVer = null;
        Matcher vm = versionPattern.matcher(version);

        /*
         * If version string does not match the expected version string, return
         * null.
         */
        if (vm.find()) {
            newVer = new Version();
            int[] ver = new int[3];

            ver[0] = Integer.parseInt(vm.group(1));
            if (vm.group(3) != null) {
                ver[1] = Integer.parseInt(vm.group(3));
            }
            if (vm.group(5) != null) {
                ver[2] = Integer.parseInt(vm.group(5));
            }
            newVer.setVersionInfo(ver);
        } else {
            throw new ParseException("Unable to parse version from string '"
                    + version + "'. Expected pattern = '"
                    + versionPattern.pattern() + "'", 0);
        }
        return newVer;
    }

    public Version getStartVersion() {
        if (startVersion == null) {
            try {
                startVersion = parseVersion(start);
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
                endVersion = parseVersion(end);
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
