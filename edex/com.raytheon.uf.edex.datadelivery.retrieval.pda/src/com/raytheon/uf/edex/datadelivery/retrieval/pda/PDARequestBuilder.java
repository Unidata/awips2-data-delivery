package com.raytheon.uf.edex.datadelivery.retrieval.pda;

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
import org.geotools.geometry.jts.ReferencedEnvelope;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.EnvelopeUtils;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.datadelivery.retrieval.request.RequestBuilder;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * 
 * PDA Request Builder.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 12, 2014  3120     dhladky   created.
 * Sep 04, 2014  3121     dhladky   Clarified and sharpened creation, largely
 *                                  un-implemented at this point.
 * Sep 27, 2014  3127     dhladky   Geographic subsetting.
 * Apr 06, 2016  5424     dhladky   Dual ASYNC and SYNC processing.
 * May 03, 2016  5599     tjensen   Added subscription name to PDA requests
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDARequestBuilder extends RequestBuilder<Time, Coverage> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDARequestBuilder.class);

    protected static ServiceConfig pdaServiceConfig;

    protected static String subsetRequestURL = null;

    protected String request = null;

    protected String subName = null;

    protected static final String DIMENSION_LON = "Long";

    protected static final String DIMENSION_LAT = "Lat";

    protected static final String DIMENSION_TRIM_OPEN = "<wcs:DimensionTrim>";

    protected static final String DIMENSION_OPEN = "<wcs:Dimension>";

    protected static final String DIMENSION_TRIM_CLOSE = "</wcs:DimensionTrim>";

    protected static final String DIMENSION_CLOSE = "</wcs:Dimension>";

    protected static final String TRIM_HIGH_OPEN = "<wcs:TrimHigh>";

    protected static final String TRIM_HIGH_CLOSE = "</wcs:TrimHigh>";

    protected static final String TRIM_LOW_OPEN = "<wcs:TrimLow>";

    protected static final String TRIM_LOW_CLOSE = "</wcs:TrimLow>";

    /**
     * Retrieval Adapter constructor
     * 
     * @param ra
     */
    protected PDARequestBuilder(RetrievalAttribute<Time, Coverage> ra,
            String subName) {
        super(ra);
        this.subName = subName;
    }

    @Override
    public String processTime(Time prtXML) {
        throw new UnsupportedOperationException("Not implemented for PDA!");
    }

    @Override
    public String getRequest() {
        // There are no switches for full data set PDA.
        return request;
    }

    /**
     * Sets the request string
     * 
     * @param request
     */
    public void setRequest(String request) {
        this.request = request;
    }

    /**
     * Get Subscription name
     * 
     * @return
     */
    public String getSubName() {
        return subName;
    }

    /**
     * Sets Subscription name
     * 
     * @param subName
     *            Name of the subscription
     */
    public void setSubName(String subName) {
        this.subName = subName;
    }

    /**
     * Get the instance of the service config
     * 
     * @return
     */
    protected static ServiceConfig getServiceConfig() {

        if (pdaServiceConfig == null) {
            pdaServiceConfig = HarvesterServiceManager.getInstance()
                    .getServiceConfig(ServiceType.PDA);
        }

        return pdaServiceConfig;
    }

    @Override
    public RetrievalAttribute<Time, Coverage> getAttribute() {
        return ra;
    }

    /**
     * Adds the coverage for the subset
     * 
     * @param Coverage
     */
    public String processCoverage(Coverage coverage) {

        StringBuilder sb = new StringBuilder(256);

        if (coverage != null) {
            try {
                ReferencedEnvelope re = coverage.getRequestEnvelope();
                Coordinate ll = EnvelopeUtils.getLowerLeftLatLon(re);
                Coordinate ur = EnvelopeUtils.getUpperRightLatLon(re);
                // manage the box
                double lowerLon = ll.x;
                double lowerLat = ll.y;
                double upperLon = ur.x;
                double upperLat = ur.y;

                // longitude dimension
                sb.append(DIMENSION_TRIM_OPEN);
                sb.append(NEW_LINE);
                sb.append(DIMENSION_OPEN);
                sb.append(DIMENSION_LON);
                sb.append(DIMENSION_CLOSE);
                sb.append(NEW_LINE);
                // longitude low
                sb.append(TRIM_LOW_OPEN);
                sb.append(lowerLon);
                sb.append(TRIM_LOW_CLOSE);
                sb.append(NEW_LINE);
                // longitude high
                sb.append(TRIM_HIGH_OPEN);
                sb.append(upperLon);
                sb.append(TRIM_HIGH_CLOSE);
                sb.append(NEW_LINE);
                sb.append(DIMENSION_TRIM_CLOSE);
                sb.append(NEW_LINE);

                // latitude dimension
                sb.append(DIMENSION_TRIM_OPEN);
                sb.append(NEW_LINE);
                sb.append(DIMENSION_OPEN);
                sb.append(DIMENSION_LAT);
                sb.append(DIMENSION_CLOSE);
                sb.append(NEW_LINE);
                // latitude low
                sb.append(TRIM_LOW_OPEN);
                sb.append(lowerLat);
                sb.append(TRIM_LOW_CLOSE);
                sb.append(NEW_LINE);
                // latitude high
                sb.append(TRIM_HIGH_OPEN);
                sb.append(upperLat);
                sb.append(TRIM_HIGH_CLOSE);
                sb.append(NEW_LINE);
                sb.append(DIMENSION_TRIM_CLOSE);
                sb.append(NEW_LINE);

            } catch (Exception e) {
                statusHandler.error("Couldn't parse Coverage object.", e);
            }
        }

        return sb.toString();
    }

}
