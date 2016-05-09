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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileDeleteStrategy;

import com.raytheon.edex.esb.Headers;
import com.raytheon.edex.plugin.goessounding.GOESSndgSeparatorFactory;
import com.raytheon.edex.plugin.goessounding.GOESSoundingDecoder;
import com.raytheon.edex.plugin.goessounding.GOESSoundingSeparator;
import com.raytheon.edex.plugin.goessounding.GoesSoundingInput;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.goessounding.GOESSounding;
import com.raytheon.uf.common.dataplugin.satellite.SatelliteRecord;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.wmo.WMOHeader;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.adapters.AbstractMetadataAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.IPDAMetaDataAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ResponseProcessingUtilities;
//import com.raytheon.uf.edex.plugin.goesr.GOESRDecoder;
import com.raytheon.uf.edex.plugin.satellite.gini.GiniSatelliteDecoder;

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
 * Oct 14, 2014  #3127      dhladky     Improved deletion of files
 * Nov 20, 2014  #3127      dhladky     GOES Sounding processing.
 * Dec 02, 2014  #3826      dhladky     PDA test code
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDAMetaDataAdapter extends
        AbstractMetadataAdapter<GOESSounding, Time, Coverage> implements IPDAMetaDataAdapter {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDAMetaDataAdapter.class);

    /** goesr data, sat **/
    public static final String goesr = "goesr";

    /** general satellite data **/
    public static final String satellite = "satellite";
    
    /** goes sounding data **/
    public static final String goessounding = "goessounding";

    /** goesr decoder **/
    //private GOESRDecoder goesrDecoder = null;
    
    private GOESSoundingDecoder goessoundingDecoder = null;

    /** satellite decoder **/
    private GiniSatelliteDecoder satelliteDecoder = null;

    // decoder operating type
    private String type = null;

    public PDAMetaDataAdapter() {
    }


    @Override
    public void processAttributeXml(RetrievalAttribute<Time, Coverage> attXML)
            throws InstantiationException {

        this.attXML = attXML;
        // TODO The only data we have right now is GOESSounding
        // Find a better way to tell the difference between them
        //if (attXML.getPlugin().equals("satellite")) {
            type = "goessounding";
        //}
    }

    /**
     * All of the work for SAT/GOESR DD decoding is done in the NetCDF decoders.
     */
    @Override
    public PluginDataObject[] decodeObjects(String fileName) throws Exception {

        PluginDataObject[] pdos = null;

        try {
            if (type.equals(goesr)) {
            //   pdos = getGoesrDecoder().decode(
            //         ResponseProcessingUtilities.getBytes(fileName));
            } else if (type.equals(satellite)) {
                pdos = getSatDecoder().decode(new File(fileName));
            } else if (type.equals(goessounding)) {
                pdos = processGoesSounding(fileName);
            }
        } catch (Exception e) {
            statusHandler.error("Couldn't decode PDA data! " + fileName, e);
        } finally {
            // Done! dispose of this file and it's directory
            File file = new File(fileName);
            if (file.exists()) {
                File dir = new File(file.getParent());
                if (dir.isDirectory()) {
                    // force delete it, no excuses.
                    statusHandler
                            .info("Deleting processed retrieval file directory. "
                                    + dir.getName());
                    FileDeleteStrategy.FORCE.delete(dir);
                }
            }
        }

        return pdos;
    }

    @Override
    public PluginDataObject getRecord(GOESSounding o) {
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
    
    private GOESRDecoder getGoesrDecoder() {
        if (goesrDecoder == null) {
            goesrDecoder = (GOESRDecoder) EDEXUtil.getESBComponent("goesrDecoder");
        }
        return goesrDecoder;
    }
     */
    
    /**
     * Get the GOES sounding decoder from the ESB
     * @return
     */
    private GOESSoundingDecoder getGoesSoundingDecoder() {
        if (goessoundingDecoder == null) {
            goessoundingDecoder = (GOESSoundingDecoder) EDEXUtil.getESBComponent("goessoundingDecoder");
        }
        return goessoundingDecoder;
    }
    
    /**
     * get an ESB instance of the SAT decoder
     * @return
     */
    private GiniSatelliteDecoder getSatDecoder() {
        if (satelliteDecoder == null) {
            satelliteDecoder = (GiniSatelliteDecoder) EDEXUtil.getESBComponent("giniDecoder");
        }
        return satelliteDecoder;
    }
    
    /**
     * Process the incoming GOES Sounding files
     * @param fileName
     * @return
     */
    private PluginDataObject[] processGoesSounding(String fileName) {
        
        Headers headers = null;
        List<PluginDataObject> pdos = new ArrayList<PluginDataObject>(0);
        
        if (fileName != null) {
            // Add the WMO headers necessary for ingest
            headers = new Headers();
            headers.put(WMOHeader.INGEST_FILE_NAME, fileName);
        }
        
        if (headers != null) {
            try {
                // convert back to byte array to conform to expected interface
                GOESSoundingSeparator separator = GOESSndgSeparatorFactory.getSeparator(ResponseProcessingUtilities.getBytes(fileName), headers);
                // walk the iterator
                while (separator.hasNext()) {
                    GoesSoundingInput gsi = separator.next();
                    // call the decoder
                    PluginDataObject[] separatorPdos = getGoesSoundingDecoder().decode(gsi, headers);
                    // add to main pdo array
                    if (separatorPdos != null) {
                        for (int i = 0; i < separatorPdos.length - 1; i++) {
                            pdos.add(separatorPdos[i]);
                        }
                    }
                }
            } catch (Exception e) {
                statusHandler.handle(Priority.ERROR, "Failed to process GOESSounding file! "+fileName, e);
            }
        } else {
            throw new IllegalArgumentException("Filename doesn't create a good header, "+fileName);
        }
        
        return pdos.toArray(new PluginDataObject[pdos.size()]);
    }
   
}
