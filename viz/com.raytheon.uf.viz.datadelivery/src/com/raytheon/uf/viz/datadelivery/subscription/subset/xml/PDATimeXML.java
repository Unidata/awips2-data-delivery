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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Point time xml object.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------
 * Aug 20, 2014  3121     dhladky   Initial creation.
 * Aug 17, 2016  5772     rjpeter   Only support latest time.
 * Oct 06, 2016  5883     tjensen   Fixed for serialization
 * 
 * </pre>
 * 
 * @author dhladky
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class PDATimeXML extends TimeXML {

    /*
     * This should always be true for PDA, but needed for serialization to Save
     * Subsets.
     */
    @XmlElement(name = "latestData")
    protected boolean latestData = true;

    /**
     * @return the latestData
     */
    public boolean isLatestData() {
        return latestData;
    }

    /**
     * @param latestData
     *            the latestData to set
     */
    public void setLatestData(boolean latestData) {
        this.latestData = latestData;
    }

    @Override
    public String getPreviewString() {
        return "Requesting Latest Data";
    }

}
