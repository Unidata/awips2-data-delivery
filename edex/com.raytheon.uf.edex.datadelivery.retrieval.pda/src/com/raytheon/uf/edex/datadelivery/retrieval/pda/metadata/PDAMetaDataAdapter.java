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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileDeleteStrategy;

import com.raytheon.edex.esb.Headers;
import com.raytheon.edex.plugin.goessounding.GOESSndgSeparatorFactory;
import com.raytheon.edex.plugin.goessounding.GOESSoundingDecoder;
import com.raytheon.edex.plugin.goessounding.GOESSoundingSeparator;
import com.raytheon.edex.plugin.goessounding.GoesSoundingInput;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.satellite.SatelliteRecord;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.wmo.WMOHeader;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.adapters.AbstractMetadataAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ResponseProcessingUtilities;
import com.raytheon.uf.edex.netcdf.decoder.NetcdfRecordInfo;
import com.raytheon.uf.edex.plugin.goesr.GoesrNetcdfDecoder;
import com.raytheon.uf.edex.plugin.satellite.gini.GiniSatelliteDecoder;

/**
 *
 * Convert RetrievalAttribute to Satellite/GOESSounding objects.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 28, 2014  3121     dhladky   Initial javadoc
 * Oct 14, 2014  3127     dhladky   Improved deletion of files
 * Nov 20, 2014  3127     dhladky   GOES Sounding processing.
 * Dec 02, 2014  3826     dhladky   PDA test code
 * Jan 28, 2016  5299     dhladky   PDA testing related fixes.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * May 03, 2016  5599     tjensen   Pass subscription name to GoesrDecoder to
 *                                  override sectorID.
 * May 16, 2016  5599     tjensen   Refresh dataURI after overriding sectorID.
 * Jun 27, 2016  5584     nabowle   Netcdf decoder consolidation.
 *
 * </pre>
 *
 * @author dhladky
 * @version 1.0
 */

public class PDAMetaDataAdapter extends
        AbstractMetadataAdapter<PluginDataObject, Time, Coverage> {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDAMetaDataAdapter.class);

    /** goesr data, sat **/
    public static final String goesr = "goesr";

    /** generic satellite data **/
    public static final String satellite = "satellite";

    /** goes sounding data **/
    public static final String goessounding = "goessounding";

    /** goesr decoder **/
    private GoesrNetcdfDecoder goesrDecoder = null;

    /** goes sounding decoder **/
    private GOESSoundingDecoder goessoundingDecoder = null;

    /** satellite decoder **/
    private GiniSatelliteDecoder satelliteDecoder = null;

    // decoder operating type
    private String type = null;

    /** decoder type default **/
    private static final String DEFAULT_TYPE = "DEFAULT_TYPE";

    public PDAMetaDataAdapter() {

    }

    @Override
    public void processAttributeXml(RetrievalAttribute<Time, Coverage> attXML)
            throws InstantiationException {

        this.attXML = attXML;
        ServiceType serviceType = ServiceType.PDA;
        /**
         * TODO The only data we have right now is GOESR imagery. Need to find a
         * better way to tell the difference between GOESR and GOESSounding
         * data, since PDA has no GOESSounding data at this time, I am unable to
         * tell how the metadata will be different. This is a work in progress.
         * We need to be able to dynamically switch decoder based on attribute.
         */
        type = getServiceConfig(serviceType).getConstantValue(DEFAULT_TYPE);
    }

    /**
     * Gets the correct satellite decoder. All of the work for SAT/GOESR DD
     * decoding is done in the NetCDF decoders.
     *
     * @return
     * @throws Exception
     */
    public PluginDataObject[] decodeObjects(String fileName, String subName)
            throws Exception {

        PluginDataObject[] pdos = null;

        try {
            if (type.equals(goesr)) {
                statusHandler.debug("Processing as GOESR imagery.....");
                pdos = processGoesr(fileName);
            } else if (type.equals(satellite)) {
                statusHandler.debug("Processing as legacy SAT imagery.....");
                pdos = getSatDecoder().decode(new File(fileName));
            } else if (type.equals(goessounding)) {
                statusHandler.debug("Processing as legacy GOES sounding .....");
                pdos = processGoesSounding(fileName);
            } else {
                throw new IllegalArgumentException("Unknown type detected. "
                        + type);
            }

            postProcessPdos(pdos, subName);
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

    /**
     * Performs any necessary post processing on decoded pdos before they are
     * persisted.
     * 
     * @param pdos
     *            Array of decoded pdos to be processed
     * @param subName
     *            subscription name
     */
    private void postProcessPdos(PluginDataObject[] pdos, String subName) {
        /*
         * For all PDOs that are SatelliteRecords, replace the sectorId with the
         * subscription name so we can tie the record to the subscription.
         */
        for (PluginDataObject pdo : pdos) {
            if (pdo instanceof SatelliteRecord) {
                SatelliteRecord sr = (SatelliteRecord) pdo;
                sr.setSectorID(subName);
                /*
                 * Set DataURI to null, then call getDataURI to recreate it with
                 * the new sectorId.
                 */
                sr.setDataURI(null);
                sr.getDataURI();
            }
        }
    }

    @Override
    public PluginDataObject getRecord(PluginDataObject o) {
        // unimplemented by PDA, returns what you give it in this case.
        return o;
    }

    @Override
    public void allocatePdoArray(int size) {
        /*
         * unimplemented by PDA, don't know the number of SAT records in the
         * NetCDF file.
         */
    }

    /**
     * get an ESB instance of the GOESR decoder
     *
     * @return
     */
    private GoesrNetcdfDecoder getGoesrDecoder() {
        if (goesrDecoder == null) {
            goesrDecoder = (GoesrNetcdfDecoder) EDEXUtil
                    .getESBComponent("goesrDecoder");
        }
        return goesrDecoder;
    }

    /**
     * Get the GOES sounding decoder from the ESB
     *
     * @return
     */
    private GOESSoundingDecoder getGoesSoundingDecoder() {
        if (goessoundingDecoder == null) {
            goessoundingDecoder = (GOESSoundingDecoder) EDEXUtil
                    .getESBComponent("goessoundingDecoder");
        }
        return goessoundingDecoder;
    }

    /**
     * get an ESB instance of the SAT decoder
     *
     * @return
     */
    private GiniSatelliteDecoder getSatDecoder() {
        if (satelliteDecoder == null) {
            satelliteDecoder = (GiniSatelliteDecoder) EDEXUtil
                    .getESBComponent("giniDecoder");
        }
        return satelliteDecoder;
    }

    /**
     * Process the incoming GOES Sounding files
     *
     * @param fileName
     * @return
     */
    private PluginDataObject[] processGoesSounding(String fileName) {

        Headers headers = null;
        List<PluginDataObject> pdos = new ArrayList<>(0);

        if (fileName != null) {
            // Add the WMO headers necessary for ingest
            headers = new Headers();
            headers.put(WMOHeader.INGEST_FILE_NAME, fileName);
        }

        if (headers != null) {
            try {
                // convert back to byte array to conform to expected interface
                GOESSoundingSeparator separator = GOESSndgSeparatorFactory
                        .getSeparator(
                                ResponseProcessingUtilities.getBytes(fileName),
                                headers);
                // walk the iterator
                while (separator.hasNext()) {
                    GoesSoundingInput gsi = separator.next();
                    // call the decoder
                    PluginDataObject[] separatorPdos = getGoesSoundingDecoder()
                            .decode(gsi, headers);
                    // add to main pdo array
                    if (separatorPdos != null) {
                        for (int i = 0; i < separatorPdos.length - 1; i++) {
                            pdos.add(separatorPdos[i]);
                        }
                    }
                }
            } catch (Exception e) {
                statusHandler.handle(Priority.ERROR,
                        "Failed to process GOESSounding file! " + fileName, e);
            }
        } else {
            throw new IllegalArgumentException(
                    "Filename doesn't create a good header, " + fileName);
        }

        return pdos.toArray(new PluginDataObject[pdos.size()]);
    }

    /**
     * Process incoming GOES-R files.
     *
     * @param fileName
     *            The name of the file.
     * @return The pdos
     */
    public PluginDataObject[] processGoesr(String fileName) {
        List<PluginDataObject> pdoList = new ArrayList<>();
        try {
            Iterator<NetcdfRecordInfo> infoIter = getGoesrDecoder().split(
                    new File(fileName));
            while (infoIter.hasNext()) {
                PluginDataObject[] splitPdos = getGoesrDecoder().decode(
                        infoIter.next());
                for (PluginDataObject pdo : splitPdos) {
                    pdoList.add(pdo);
                }
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Failed to process GOES-R file " + fileName, e);
        }
        return pdoList.toArray(new PluginDataObject[0]);
    }

}
