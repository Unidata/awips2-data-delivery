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
package com.raytheon.uf.edex.datadelivery.retrieval.pda.metadata;

import java.io.File;

import com.raytheon.edex.plugin.satellite.SatelliteDecoder;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.satellite.SatelliteRecord;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.adapters.AbstractMetadataAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.IPDAMetaDataAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ResponseProcessingUtilities;
import com.raytheon.uf.edex.plugin.goesr.GOESRDecoder;

/**
 * 
 * Convert RetrievalAttribute to Satellite.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 28, 2014 #3121       dhladky     Initial javadoc
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDAMetaDataAdapter extends
        AbstractMetadataAdapter<SatelliteRecord, Time, Coverage> implements IPDAMetaDataAdapter {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDAMetaDataAdapter.class);

    /** goesr data, sat **/
    public static final String goesr = "goesr";

    /** general satellite data **/
    public static final String satellite = "satellite";

    /** goesr decoder **/
    private GOESRDecoder goesrDecoder = null;

    /** satellite decoder **/
    private SatelliteDecoder satelliteDecoder = null;

    // decoder operating type
    private String type = null;

    public PDAMetaDataAdapter() {
    }


    @Override
    public void processAttributeXml(RetrievalAttribute<Time, Coverage> attXML)
            throws InstantiationException {

        this.attXML = attXML;
        // TODO The only data we have right now is GOESR
        // Find a better way to tell the difference between them
        if (attXML.getPlugin().equals("satellite")) {
            type = "goesr";
        }
    }

    /**
     * All of the work for SAT/GOESR DD decoding is done in the NetCDF decoders.
     */
    @Override
    public PluginDataObject[] decodeObjects(String fileName) throws Exception {

        PluginDataObject[] pdos = null;

        try {
            if (type.equals(goesr)) {
                pdos = getGoesrDecoder().decode(
                        ResponseProcessingUtilities.getBytes(fileName));
            } else if (type.equals(satellite)) {
                pdos = getSatDecoder().decode(new File(fileName));
            }
        } catch (Exception e) {
            statusHandler.error("Couldn't decode PDA data! " + fileName, e);
        } finally {
            // Done! dispose of this file
            File file = new File(fileName);
            if (file.exists()) {
                statusHandler.info("Deleting processed retrieval file. "
                        + file.getName());
                file.delete();
            }
        }

        return pdos;
    }

    @Override
    public PluginDataObject getRecord(SatelliteRecord o) {
        // unimplemented by PDA, returns what you give it in this case.
        return o;
    }

    @Override
    public void allocatePdoArray(int size) {
        // unimplemented by PDA, don't know the number of SAT records in the NetCDF file.
    }
    
    /**
     * get an ESB instance of the GOESR decoder
     * @return
     */
    private GOESRDecoder getGoesrDecoder() {
        if (goesrDecoder == null) {
            goesrDecoder = (GOESRDecoder) EDEXUtil.getESBComponent("goesrDecoder");
        }
        return goesrDecoder;
    }
    
    /**
     * get an ESB instance of the SAT decoder
     * @return
     */
    private SatelliteDecoder getSatDecoder() {
        if (satelliteDecoder == null) {
            satelliteDecoder = (SatelliteDecoder) EDEXUtil.getESBComponent("satelliteDecoder");
        }
        return satelliteDecoder;
    }
   
}
