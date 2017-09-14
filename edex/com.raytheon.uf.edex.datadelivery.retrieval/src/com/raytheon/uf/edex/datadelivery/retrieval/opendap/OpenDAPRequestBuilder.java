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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Ensemble;
import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedDataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.GriddedParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.ParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.gridcoverage.GridCoverage;
import com.raytheon.uf.common.gridcoverage.subgrid.SubGrid;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.edex.datadelivery.retrieval.request.RequestBuilder;

/**
 * Request XML translation related utilities
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 07, 2011           dhladky   Initial creation
 * Jul 25, 2012  955      djohnson  Make package-private.
 * Aug 14, 2012  1022     djohnson  Throw exception if invalid OpenDAP grid
 *                                  coordinates specified, make immutable, use
 *                                  StringBuilder instead of StringBuffer.
 * Dec 10, 2012  1259     bsteffen  Switch Data Delivery from LatLon to
 *                                  referenced envelopes.
 * May 12, 2013  753      dhladky   address field
 * Sep 25, 2013  1797     dhladky   separated time from gridded time
 * Sep 27, 2014  2131     dhladky   removed un-needed casting.
 * Oct 14, 2014  3127     dhladky   Fixed stack overflow.
 * Jul 27, 2017  6186     rjpeter   Use Retrieval
 * Sep 12, 2017  6413     tjensen   Removed unnecessary requestLevelStart and
 *                                  End
 *
 * </pre>
 *
 * @author dhladky
 */
class OpenDAPRequestBuilder
        extends RequestBuilder<GriddedTime, GriddedCoverage> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final String openDAPURL;

    /**
     * useful constructor
     *
     * @param prXML
     */
    OpenDAPRequestBuilder(Retrieval<GriddedTime, GriddedCoverage> retrieval) {
        super(retrieval);

        // Create URL
        // this works in this order
        // Ensemble TIME LEVELS Y X
        StringBuilder buffer = new StringBuilder();
        buffer.append(retrieval.getUrl());
        buffer.append("?");
        RetrievalAttribute<GriddedTime, GriddedCoverage> att = retrieval
                .getAttribute();

        buffer.append(att.getEntry().getProviderName());
        // process ensemble first
        buffer.append(processEnsemble());
        // process time second
        buffer.append(processTime(att.getTime()));
        // process the coverage, w/levels
        buffer.append(processCoverage(att.getCoverage()));

        this.openDAPURL = buffer.toString().trim();
    }

    /**
     * Gets a string vertical levels DAP from XML
     *
     * @param string
     *
     * @param prcXML
     * @return
     */
    private String processDAPLevels(ParameterLevelEntry ple, String url) {

        StringBuilder buf = new StringBuilder();

        if (ple instanceof GriddedParameterLevelEntry) {
            GriddedParameterLevelEntry gple = (GriddedParameterLevelEntry) ple;
            if (gple.isUseProviderLevel()) {
                try {
                    DataSetMetaData<?, ?> dsmd = DataDeliveryHandlers
                            .getDataSetMetaDataHandler().getById(url);
                    if (dsmd instanceof GriddedDataSetMetaData) {
                        GriddedDataSetMetaData gdsmd = (GriddedDataSetMetaData) dsmd;
                        buf.append("["
                                + gdsmd.findProviderLevelIndex(
                                        Double.parseDouble(gple.getLevelOne()))
                                + "]");
                    }
                } catch (RegistryHandlerException e) {
                    logger.error(
                            "Error retrieving metadata for url '" + url + "'",
                            e);
                }
            }
        }

        return buf.toString();
    }

    @Override
    public String processTime(GriddedTime time) {

        StringBuilder buf = new StringBuilder();
        GriddedTime gtime = time;

        if (gtime.getNumTimes() == 1) {
            // only one time available
            buf.append("[0]");
        } else {
            // a particular time selected from the list
            if (gtime.getRequestStart().equals(gtime.getRequestEnd())) {
                buf.append("[" + (gtime.getRequestStartTimeAsInt()) + "]");
            }
            // a range of times selected from the list
            else {
                buf.append("[" + (gtime.getRequestStartTimeAsInt()) + ":1:"
                        + (gtime.getRequestEndTimeAsInt()) + "]");
            }
        }

        return buf.toString();
    }

    @Override
    public String processCoverage(GriddedCoverage coverage) {

        StringBuilder sb = new StringBuilder();
        sb.append(processDAPLevels(retrieval.getAttribute().getEntry(),
                retrieval.getUrl()));
        sb.append(getCoverageString(coverage));

        return sb.toString();
    }

    /**
     * @param coverage
     * @return
     */
    @VisibleForTesting
    static String getCoverageString(GriddedCoverage coverage) {
        StringBuilder sb = new StringBuilder();
        // Check for Sub-gridding!!!!!!!!!!!!!!
        // y
        SubGrid requestSubGrid = coverage.getRequestSubGrid();
        if (requestSubGrid != null) {
            GridCoverage fullGridCoverage = coverage.getGridCoverage();

            int startX = requestSubGrid.getUpperLeftX();
            int endX = startX + requestSubGrid.getNX() - 1;

            // Y is more complex because GridCoverages uses 0 at the top
            // increasing down and openDap uses 0 at the botton increasing up so
            // it is necessary to invert the y values.
            int startY = fullGridCoverage.getNy()
                    - requestSubGrid.getUpperLeftY() - requestSubGrid.getNY();
            int endY = fullGridCoverage.getNy() - requestSubGrid.getUpperLeftY()
                    - 1;

            sb.append("[" + startY + ":1:" + endY + "]");
            sb.append("[" + startX + ":1:" + endX + "]");

        }

        return sb.toString();
    }

    /**
     * Take care of ensembles
     *
     * @return
     */
    public String processEnsemble() {

        if (retrieval.getAttribute().getEnsemble() != null) {
            Ensemble e = retrieval.getAttribute().getEnsemble();
            int[] range = e.getSelectedRange();
            if (range[0] == range[1]) {
                return "[" + range[0] + "]";
            }
            return "[" + range[0] + ":" + range[1] + "]";
        }
        return "";
    }

    @Override
    public String getRequest() {
        return openDAPURL;
    }

}
