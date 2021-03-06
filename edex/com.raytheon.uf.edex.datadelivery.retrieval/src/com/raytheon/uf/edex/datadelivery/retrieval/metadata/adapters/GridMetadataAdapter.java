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
package com.raytheon.uf.edex.datadelivery.retrieval.metadata.adapters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.grid.GridRecord;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.gridcoverage.GridCoverage;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.GridUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ResponseProcessingUtilities;

/**
 *
 * Convert RetrievalAttribute to GridRecords.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Nov 19, 2012           bsteffen  Initial javadoc
 * Feb 07, 2013  1543     djohnson  Allow package-level overriding of methods
 *                                  for mocking in tests.
 * May 12, 2013  753      dhladky   Altered to be more flexible with other types
 * May 31, 2013  2038     djohnson  Rename setPdos to allocatePdoArray.
 * Sep 25, 2013  1797     dhladky   separated time from gridded time
 * Apr 22, 2014  3046     dhladky   Got rid of duplicate code.
 * Jun 13, 2017  6204     nabowle   Cleanup.
 * Jul 27, 2017  6186     rjpeter   Use Retrieval
 * Sep 20, 2017  6413     tjensen   Update for ParameterGroups
 * Apr 03, 2018  7240     tjensen   Store data by dataset name with DD prefix
 *
 * </pre>
 *
 * @author dhladky
 */
public class GridMetadataAdapter
        extends AbstractMetadataAdapter<Integer, GriddedTime, GriddedCoverage> {

    private static final String DD_MODEL_PREFIX = "DD_";

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GridMetadataAdapter.class);

    public GridMetadataAdapter() {
    }

    @Override
    public void processRetrieval(
            Retrieval<GriddedTime, GriddedCoverage> retrieval)
            throws InstantiationException {
        RetrievalAttribute<GriddedTime, GriddedCoverage> attXML = retrieval
                .getAttribute();
        Level[] levels = getLevels(attXML);
        int size = levels.length;

        List<String> ensembles = null;
        if (attXML.getEnsemble() != null
                && attXML.getEnsemble().hasSelection()) {
            ensembles = attXML.getEnsemble().getSelectedMembers();
            size *= ensembles.size();
        } else {
            ensembles = Arrays.asList((String) null);
        }

        GriddedTime time = attXML.getTime();

        if (time.getSelectedTimeIndices() != null) {
            if (levels.length > 1 || time.getSelectedTimeIndices().size() > 1) {
                size *= time.getSelectedTimeIndices().size();
            }
        }

        allocatePdoArray(size);
        GridCoverage gridCoverage = attXML.getCoverage()
                .getRequestGridCoverage();

        if (time.getSelectedTimeIndices() != null) {
            int bin = 0;
            for (String ensemble : ensembles) {
                for (int i = 0; i < time.getSelectedTimeIndices().size(); i++) {
                    for (Level level : levels) {
                        pdos[bin] = populateGridRecord(
                                DD_MODEL_PREFIX + retrieval.getDataSetName(),
                                attXML.getParameterGroup(), attXML.getEntry(),
                                level, ensemble, gridCoverage);
                        bin++;
                    }
                }
            }
        } else {

            pdos[0] = populateGridRecord(retrieval.getSubscriptionName(),
                    attXML.getParameterGroup(), attXML.getEntry(), levels[0],
                    ensembles.get(0), gridCoverage);

        }
    }

    /**
     * Populate the grid record
     *
     * @param parm
     * @param level
     * @param gridCoverage
     * @return
     */
    private GridRecord populateGridRecord(String name, ParameterGroup parm,
            ParameterLevelEntry entry, Level level, String ensembleId,
            GridCoverage gridCoverage) {

        GridRecord rec = null;

        try {
            rec = ResponseProcessingUtilities.getGridRecord(name, parm, entry,
                    level, ensembleId, gridCoverage);
        } catch (Exception e) {
            statusHandler.error("Couldn't create grid record! ", e);
        }

        return rec;
    }

    private static Level[] getLevels(
            RetrievalAttribute<GriddedTime, GriddedCoverage> attXML) {
        List<Level> levels = new ArrayList<>(1);
        levels.addAll(ResponseProcessingUtilities.getOpenDAPGridLevels(
                attXML.getParameterGroup().getGroupedLevels().values()));

        return levels.toArray(new Level[levels.size()]);
    }

    /**
     * Flips the array in the y direction, needed because AWIPS display is
     * backward from NCEP data
     *
     * @param nx
     * @param ny
     * @param origVals
     * @return
     */
    public static float[] adjustGrid(int nx, int ny, float[] origVals,
            float missingValue, boolean flip) {

        float[] returnVals = new float[(nx * ny)];

        for (int y = 0; y < ny; y++) {
            int revy;
            if (flip) {
                revy = (ny - 1) - y;
            } else {
                revy = y;
            }

            for (int x = 0; x < nx; x++) {
                float value = origVals[(nx * y) + x];
                if (value == missingValue) {
                    value = GridUtil.GRID_FILL_VALUE;
                }
                returnVals[(nx * revy) + x] = value;
            }
        }

        return returnVals;
    }

    /**
     * pre-fill grid and pad when grid from provider is short on values
     *
     * @param nx
     * @param ny
     * @param dnx
     * @param dny
     * @param vals
     * @return
     */
    public static float[] padUpGrid(int nx, int ny, int dnx, int dny,
            float[] subValues) {
        // first fill entire grid the size we want
        float[] vals = new float[(nx * ny)];
        for (int y = 0; y < ny; y++) {
            for (int x = 0; x < nx; x++) {
                vals[(nx * y) + x] = GridUtil.GRID_FILL_VALUE;
            }
        }
        // writes in the values of the sub cut array actually received from
        // provider
        for (int y = 0; y < dny; y++) {
            for (int x = 0; x < dnx; x++) {
                vals[(dnx * y) + x] = subValues[(dnx * y) + x];
            }
        }

        return vals;
    }

    /**
     * pre-fill grid and pad when grid from provider is long on values
     *
     * @param nx
     * @param ny
     * @param dnx
     * @param dny
     * @param vals
     * @return
     */
    public static float[] padDownGrid(int nx, int ny, int dnx, int dny,
            float[] subValues) {
        // fill in extra spaces not filled by provider
        int offset = (nx - dnx);
        float[] vals = new float[(nx * ny)];
        // fill it up
        for (int y = 0; y < ny; y++) {
            for (int x = 0; x < nx; x++) {
                vals[(nx * y) + x] = GridUtil.GRID_FILL_VALUE;
            }
        }
        // writes in the values of the sub cut array
        for (int y = 0; y < dny; y++) {
            for (int x = 0; x < dnx; x++) {
                int bin = (dnx * y) + x;
                vals[bin] = subValues[bin];
                if (x == (dnx - 1)) {
                    for (int i = 0; i < offset; i++) {
                        vals[bin + i] = GridUtil.GRID_FILL_VALUE;
                    }
                }
            }
        }

        return vals;
    }

    @Override
    public PluginDataObject getRecord(Integer index) {

        if (pdos != null && index < pdos.length) {
            return pdos[index];
        }
        return null;
    }

    @Override
    public void allocatePdoArray(int size) {
        pdos = new GridRecord[size];
    }

}
