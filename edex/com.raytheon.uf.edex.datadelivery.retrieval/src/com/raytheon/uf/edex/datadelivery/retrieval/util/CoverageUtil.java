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
package com.raytheon.uf.edex.datadelivery.retrieval.util;

import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetBounds;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * 
 * Utility class for building Coverage objects
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 10, 2017 1045       tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
public class CoverageUtil {

    /**
     * Creates a Coverage object from list of coordinates and a coordinate
     * reference system.
     * 
     * @param list
     *            List of Coordinates
     * @param crs
     *            The CRS
     * @return Coverage with the correct Bounding Box
     */
    public static Coverage buildCoverage(List<Coordinate> list,
            CoordinateReferenceSystem crs) {
        Polygon poly = buildPolygon(list);
        Coverage coverage = buildCoverageFromEnvelope(crs, poly.getEnvelope());
        return coverage;
    }

    /**
     * Creates a polygon from a list of Coordinates. If last coordinate does not
     * equal the first, another coordinate will be added to close the polygon.
     * 
     * @param list
     *            List of Coordinates
     * @return the polygon
     */
    public static Polygon buildPolygon(List<Coordinate> list) {
        GeometryFactory factory = new GeometryFactory();

        if (!list.get(0).equals(list.get(list.size() - 1))) {
            list.add(list.get(0));
        }

        /*
         * coorsList.toArray() is unable to cast objects to Coordinates, so do
         * it manually.
         */
        Coordinate[] coords = new Coordinate[list.size()];
        for (int c = 0; c < coords.length; c++) {
            coords[c] = list.get(c);
        }
        LinearRing lr = factory.createLinearRing(coords);
        return factory.createPolygon(lr, null);
    }

    /**
     * Creates a Coverage from an envelope and a CRS. Use the upper and lower
     * corners of that envelope and the CSR to create a ReferencedEnvelope for
     * the Coverage area.
     * 
     * @param crs
     * @param envelope
     * @return Coverage with the correct Bounding Box.
     */
    public static Coverage buildCoverageFromEnvelope(
            CoordinateReferenceSystem crs, Geometry envelope) {
        Coordinate[] corners = envelope.getCoordinates();
        Coverage coverage = new Coverage();

        Envelope unrefEnvelope = new Envelope(corners[0], corners[2]);
        coverage.setEnvelope(new ReferencedEnvelope(unrefEnvelope, crs));

        return coverage;
    }

    /**
     * Creates a Coverage with the default corners of (-90, -180) and (90, 180)
     * for a given Coordinate Reference System
     * 
     * @param crs
     *            the CRS
     * @return Coverage with the correct Bounding Box.
     */
    public static Coverage buildDefaultCoverage(CoordinateReferenceSystem crs) {
        DataSetBounds defaultBounds = new DataSetBounds();
        defaultBounds.useDefaults();
        return buildCoverage(defaultBounds.getCoordList(), crs);
    }
}
