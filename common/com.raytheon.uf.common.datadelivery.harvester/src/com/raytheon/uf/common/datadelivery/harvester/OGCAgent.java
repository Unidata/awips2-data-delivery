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

package com.raytheon.uf.common.datadelivery.harvester;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Agent for a Harvester of OGC data. Defined by a HarvesterConfig. Used for
 * configuration specific to this harvester.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------
 * Sep 12, 2012  1038     dhladky   Initial creation
 * Oct 12, 2017  6413     tjensen   Remove ConfigLayers
 *
 * </pre>
 *
 * @author dhladky
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class OGCAgent extends Agent {

    /**
     * name path used for WFS
     */
    @XmlElement(name = "wfs", required = false)
    @DynamicSerializeElement
    private String wfs = "wfs";

    /**
     * name path used for WMS
     */
    @XmlElement(name = "wms", required = false)
    @DynamicSerializeElement
    private String wms = "wms";

    /**
     * name path used for WCS
     */
    @XmlElement(name = "wcs", required = false)
    @DynamicSerializeElement
    private String wcs = "wcs";

    public String getWcs() {
        return wcs;
    }

    public String getWfs() {
        return wfs;
    }

    public String getWms() {
        return wms;
    }

    public void setWcs(String wcs) {
        this.wcs = wcs;
    }

    public void setWfs(String wfs) {
        this.wfs = wfs;
    }

    public void setWms(String wms) {
        this.wms = wms;
    }
}
