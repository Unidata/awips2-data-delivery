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
package com.raytheon.uf.edex.datadelivery.retrieval.opendap;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.ParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.adapters.GridMetadataAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalTranslator;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ResponseProcessingUtilities;

/**
 * OPenDAP specific translation tools
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 18, 2011           dhladky   Initial creation
 * Feb 07, 2013  1543     djohnson  Allow package-level construction with
 *                                  explicit PDO class name.
 * Sep 25, 2013  1797     dhladky   Separate time from gridded time
 * Apr 12, 2015  4400     dhladky   Upgrade to DAP2 protocol with backward
 *                                  compatibility.
 * Jun 13, 2017  6204     nabowle   Cleanup.
 * Jul 27, 2017  6186     rjpeter   Use Retrieval
 * Sep 20, 2017  6413     tjensen   Update for ParameterGroups
 * Sep 21, 2017  6441     tgurney   Remove references to dods-1.1.7
 * Nov 15, 2017  6498     tjensen   Use inherited logger for logging
 *
 * </pre>
 *
 * @author dhladky
 */

public class OpenDAPTranslator
        extends RetrievalTranslator<GriddedTime, GriddedCoverage, Integer> {

    public OpenDAPTranslator(Retrieval<GriddedTime, GriddedCoverage> retrieval)
            throws InstantiationException {
        super(retrieval);
    }

    public PluginDataObject[] asPluginDataObjects(Object dds) {

        PluginDataObject[] pdos = null;

        for (Enumeration<?> variables = getDDSVariables(dds); variables
                .hasMoreElements();) {

            Object baseType = variables.nextElement();
            Class<?> dclass = getClass(baseType);

            try {
                if (dclass == opendap.dap.DGrid.class) {
                    pdos = translateGrid(baseType);
                } else if (dclass == opendap.dap.DArray.class) {
                    pdos = translateArray(baseType);
                }

            } catch (Exception e) {
                logger.error("Unable to translate OpenDAP Response", e);
            }
        }

        return pdos;
    }

    /**
     * Translates the Grid to something AWIPS will recognize
     *
     * @param dgrid
     * @return
     * @throws Exception
     */
    private PluginDataObject[] translateGrid(Object dgrid) throws Exception {
        try {
            PluginDataObject[] record = null;
            Enumeration<?> e = getGridVariables(dgrid);

            // only one variable at this time
            if (e.hasMoreElements()) {
                record = translateArray(e.nextElement());
            }

            return record;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Translates the DARRAY object to something AWIPS will recognize.
     *
     * @param darray
     * @return
     * @throws Exception
     */
    private PluginDataObject[] translateArray(Object darray) throws Exception {

        int nx = 0;
        int ny = 0;
        int dnx = 0;
        int dny = 0;
        int diffX = 0;
        int numTimes = getSubsetNumTimes();
        int numLevels = getSubsetNumLevels();
        List<DataTime> times = getTimes();

        for (Enumeration<?> e = ((opendap.dap.DArray) darray).getDimensions(); e
                .hasMoreElements();) {
            opendap.dap.DArrayDimension d = (opendap.dap.DArrayDimension) e
                    .nextElement();

            if ("lat".equals(d.getName())) {
                dny = d.getSize();
            } else if ("lon".equals(d.getName())) {
                dnx = d.getSize();
            }
        }

        // retrieve data
        RetrievalAttribute<GriddedTime, GriddedCoverage> attXML = retrieval
                .getAttribute();
        if (attXML.getCoverage() != null) {
            GriddedCoverage gridCoverage = attXML.getCoverage();

            nx = gridCoverage.getRequestGridCoverage().getNx();
            ny = gridCoverage.getRequestGridCoverage().getNy();

            if (dnx != nx || dny != ny) {
                logger.warn("Grid Size Inconsistency found for subscription '"
                        + retrieval.getSubscriptionName() + "'. NX" + nx
                        + " DNX: " + dnx + " NY" + ny + " DNY: " + dny);
            }
        }

        int gridSize = nx * ny;
        float[] values = getValues(darray);

        List<String> ensembles = null;
        if (attXML.getEnsemble() != null
                && attXML.getEnsemble().hasSelection()) {
            ensembles = attXML.getEnsemble().getSelectedMembers();
        } else {
            ensembles = Arrays.asList((String) null);
        }

        // time dependencies
        int start = 0;
        PluginDataObject[] records = new PluginDataObject[numLevels * numTimes
                * ensembles.size()];

        int bin = 0;
        for (int i = 0; i < ensembles.size(); i++) {
            for (DataTime dataTime : times) {
                for (int j = 0; j < numLevels; j++) {
                    PluginDataObject record = getPdo(bin);
                    record.setDataTime(dataTime);

                    int end = start + gridSize;

                    float[] subValues = Arrays.copyOfRange(values, start, end);

                    ParameterLevelEntry entry = attXML.getEntry();
                    if (entry instanceof GriddedParameterLevelEntry) {
                        GriddedParameterLevelEntry gridEntry = (GriddedParameterLevelEntry) entry;
                        subValues = GridMetadataAdapter.adjustGrid(nx, ny,
                                subValues,
                                Float.parseFloat(gridEntry.getMissingValue()),
                                true);

                        record.setMessageData(subValues);
                        record.setOverwriteAllowed(true);
                        records[bin] = record;
                        bin++;
                        logger.info("Creating record: " + record.getDataURI());
                        start = end;
                    }
                }
            }
        }

        return records;
    }

    /** get # of subset times */
    @Override
    protected int getSubsetNumTimes() {
        return ResponseProcessingUtilities
                .getOpenDAPGridNumTimes(retrieval.getAttribute().getTime());
    }

    /** get subset levels */
    @Override
    protected int getSubsetNumLevels() {
        return ResponseProcessingUtilities.getOpenDAPGridNumLevels(
                retrieval.getAttribute().getParameterGroup());
    }

    /** get list of data times from subset */
    @Override
    protected List<DataTime> getTimes() {
        return ResponseProcessingUtilities
                .getOpenDAPGridDataTimes(retrieval.getAttribute().getTime());
    }

    /** Gets the class from the BaseType */
    private Class<?> getClass(Object baseType) {
        return ((opendap.dap.BaseType) baseType).getClass();
    }

    /** Gets the variable enumeration from the DDS object */
    private Enumeration<?> getDDSVariables(Object dds) {
        return ((opendap.dap.DataDDS) dds).getVariables();
    }

    /** Gets the variable enumeration from the DGrid object */
    private Enumeration<?> getGridVariables(Object dgrid) {
        return ((opendap.dap.DGrid) dgrid).getVariables();
    }

    /** Gets the array of values from the DARRAY object */
    private float[] getValues(Object darray) {
        opendap.dap.PrimitiveVector pm = ((opendap.dap.DArray) darray)
                .getPrimitiveVector();
        return (float[]) pm.getInternalStorage();
    }

}
