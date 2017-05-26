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
package com.raytheon.uf.edex.plugin.madis.registry;

import java.util.Date;
import java.util.TreeSet;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;

import com.raytheon.uf.common.datadelivery.harvester.ConfigLayer;
import com.raytheon.uf.common.datadelivery.harvester.OGCAgent;
import com.raytheon.uf.common.dataplugin.madis.MadisRecord;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.pointdata.spatial.SurfaceObsLocation;
import com.raytheon.uf.edex.ogc.common.util.PluginIngestFilter;
import com.raytheon.uf.edex.ogc.registry.WfsRegistryCollectorAddon;
import com.raytheon.uf.edex.plugin.madis.ogc.MadisDimension;
import com.raytheon.uf.edex.plugin.madis.ogc.MadisLayer;
import com.vividsolutions.jts.geom.Envelope;

/**
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 24, 2013           bclement  Initial creation
 * Aug 18, 2013  2097     dhladky   Restored original functionality before
 *                                  renaming of this class
 * Aug 30, 2013  2098     dhladky   Incorrect time returned
 * Sep 02, 2013  2098     dhladky   Improved time management.
 * Sep 09, 2013  2351     dhladky   Speed improvements
 * Jan 13, 2014  2679     dhladky   multiple ingest layers for a single request
 *                                  window.
 * Jan 22, 2014  2713     dhladky   Calendar conversion.
 * Feb 16, 2017  6111     njensen   Replaced tabs with spaces
 * May 25, 2017  6186     rjpeter   Added createLayer
 *
 * </pre>
 *
 * @author bclement
 */
public class MadisRegistryCollectorAddon extends
        WfsRegistryCollectorAddon<MadisDimension, MadisLayer, MadisRecord>
        implements PluginIngestFilter {

    /**
     * Constructor
     */
    public MadisRegistryCollectorAddon() {
        super();
        OGCAgent agent = getAgent();

        for (ConfigLayer clayer : agent.getLayers()) {
            initializeLayerInfo(createLayer(clayer));
        }
    }

    protected MadisLayer createLayer(ConfigLayer configLayer) {
        MadisLayer rval = new MadisLayer();
        rval.setName(configLayer.getName());

        double leftx = configLayer.getMinx();
        double rightx = configLayer.getMaxx();

        // adjust lx to be -180 to 180, or 0 - 360
        while (leftx < -180) {
            leftx += 360;
        }

        while (leftx > rightx) {
            rightx += 360;
        }

        double bottomy = configLayer.getMiny();
        double topy = configLayer.getMaxy();

        if (topy < bottomy) {
            double tmp = topy;
            topy = bottomy;
            bottomy = tmp;
        }

        ReferencedEnvelope env = new ReferencedEnvelope(leftx, rightx, bottomy,
                topy, MapUtil.LATLON_PROJECTION);
        rval.setCrs84Bounds(JTS.toGeometry((Envelope) env));
        rval.setTargetCrsCode(configLayer.getCrs());
        rval.setTargetMaxx(rightx);
        rval.setTargetMaxy(topy);
        rval.setTargetMinx(leftx);
        rval.setTargetMiny(bottomy);
        rval.setTimes(new TreeSet<Date>());
        return rval;
    }

    @Override
    protected Date getTime(MadisRecord record) {
        Date time = record.getTimeObs().getTime();
        return time;
    }

    @Override
    public SurfaceObsLocation getSpatial(MadisRecord record) {
        if (record.getLocation() != null) {
            return record.getLocation();
        }
        return null;
    }

}
