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

import java.util.Arrays;

/**
 *
 * Class for storing and comparison of the AWIPS base software version
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 09, 2017  6130     tjensen   Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
public class Version implements Comparable<Version> {

    private int[] versionInfo = new int[3];

    public int[] getVersionInfo() {
        return versionInfo;
    }

    /**
     * Set the version info. Argument array must contain exactly 3 integer
     * values.
     *
     * @param versionInfo
     */
    public void setVersionInfo(int[] versionInfo) {
        if (versionInfo.length != 3) {
            throw new IllegalArgumentException(
                    "Invalid version info length received. Expected 3 ints, received "
                            + versionInfo.length);
        }
        this.versionInfo = versionInfo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(versionInfo);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Version other = (Version) obj;
        if (!Arrays.equals(versionInfo, other.versionInfo)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Version o) {
        int retval = Integer.compare(this.versionInfo[0],
                o.getVersionInfo()[0]);
        if (retval == 0) {
            retval = Integer.compare(this.versionInfo[1],
                    o.getVersionInfo()[1]);
            if (retval == 0) {
                retval = Integer.compare(this.versionInfo[2],
                        o.getVersionInfo()[2]);
            }
        }

        return retval;
    }

}
