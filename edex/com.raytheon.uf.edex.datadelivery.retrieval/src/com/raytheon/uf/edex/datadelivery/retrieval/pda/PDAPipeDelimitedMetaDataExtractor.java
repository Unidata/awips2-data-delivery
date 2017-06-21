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
package com.raytheon.uf.edex.datadelivery.retrieval.pda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.retrieval.xml.MetaDataPattern;
import com.raytheon.uf.common.datadelivery.retrieval.xml.PatternGroup;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.MetaDataParseException;
import com.raytheon.uf.edex.datadelivery.retrieval.util.PDAMetaDataUtil;
import com.raytheon.uf.edex.ogc.common.OgcException;
import com.raytheon.uf.edex.ogc.common.spatial.BoundingBoxUtil;
import com.raytheon.uf.edex.ogc.common.spatial.CrsLookup;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import net.opengis.cat.csw.v_2_0_2.BriefRecordType;
import net.opengis.ows.v_1_0_0.BoundingBoxType;

/**
 * Extract MetaData from PDA using Pipe-delimited format in metadataId field of
 * BriefRecord.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------------------
 * Mar 31, 2017  6186     rjpeter   Extracted from PDAFileMetaDataExtractor.
 *
 * </pre>
 *
 * @author rjpeter
 */
public class PDAPipeDelimitedMetaDataExtractor extends PDAMetaDataExtractor {
    public static final String CONFIG = "RECORD_ID";

    private static final String ID_FORMAT = "ID_FORMAT";

    private static final String URN_FORMAT = "URN_FORMAT";

    private static final String POLYGON_FORMAT = "POLYGON_FORMAT";

    private static final String SAT_FORMAT = "SAT_FORMAT";

    private static final String RECEIVED_TIME_FORMAT = "RECEIVED_TIME_FORMAT";

    private static final String CREATE_TIME_FORMAT = "CREATE_TIME_FORMAT";

    private static final String END_TIME_FORMAT = "END_TIME_FORMAT";

    private static final String START_TIME_FORMAT = "START_TIME_FORMAT";

    private static final String SHORT_NAME_FORMAT = "SHORT_NAME_FORMAT";

    private String polygonPoints;

    public PDAPipeDelimitedMetaDataExtractor(String metadataId, String title,
            BoundingBoxType boundingBox) {
        /*
         * boundingBox will always be null for this use case, consider refactor
         * to remove unused entries from super class.
         */
        super(PDAMetaDataUtil.getInstance().getMetaDataPattern(CONFIG),
                metadataId, title, boundingBox);
    }

    @Override
    public boolean accept() {
        Pattern pattern = metaDataPattern.getPattern();
        Matcher metadataIdMatcher = pattern.matcher(metadataId);
        boolean rval = metadataIdMatcher.matches();

        if (!rval) {
            logger.warn(
                    "Identifier [" + metadataId + "] did not match pattern ["
                            + metaDataPattern.getRegex() + "]");
        }

        return rval;
    }

    /**
     * Extracts the data from the id field.
     *
     * @param brt
     *            The BriefRecordType all the data originated from. Currently
     *            unused and only passed due to interface contract.
     * @return Map of metadata values
     */
    @Override
    public Map<String, String> extractMetaData(BriefRecordType brt)
            throws Exception {
        logger.info("Extracting metadata from metadataID...");

        Map<String, PatternGroup> mdpGroups = metaDataPattern.getGroupMap();
        String idFormat = mdpGroups.get(ID_FORMAT).getValue();
        String snFormat = mdpGroups.get(SHORT_NAME_FORMAT).getValue();
        String sTimeFormat = mdpGroups.get(START_TIME_FORMAT).getValue();
        String eTimeFormat = mdpGroups.get(END_TIME_FORMAT).getValue();
        String cTimeFormat = mdpGroups.get(CREATE_TIME_FORMAT).getValue();
        String rTimeFormat = mdpGroups.get(RECEIVED_TIME_FORMAT).getValue();
        String satFormat = mdpGroups.get(SAT_FORMAT).getValue();
        String polyFormat = mdpGroups.get(POLYGON_FORMAT).getValue();
        String regex = metaDataPattern.getRegex();

        String id = metadataId.replaceAll(regex, idFormat);
        String sat = metadataId.replaceAll(regex, satFormat);
        String startTime = metadataId.replaceAll(regex, sTimeFormat);
        String endTime = metadataId.replaceAll(regex, eTimeFormat);
        String createTime = metadataId.replaceAll(regex, cTimeFormat);
        String receiveTime = metadataId.replaceAll(regex, rTimeFormat);
        polygonPoints = metadataId.replaceAll(regex, polyFormat);

        /*
         * The only time that is guaranteed to be provided is the received time.
         * If start time is not available, first try the create time. If not,
         * fall back to received time
         */
        if (startTime == null || "".equals(startTime)) {
            if (createTime == null || "".equals(createTime)) {
                startTime = receiveTime;
            } else {
                startTime = createTime;
            }
        }

        /*
         * If end time is not provided, set it to equal the start time.
         */
        if (endTime == null || "".equals(endTime)) {
            endTime = startTime;
        }

        String providerShortName = metadataId.replaceAll(regex, snFormat);
        String param = metadataUtil.getParamFromShortName(providerShortName);
        String res = metadataUtil.getResFromShortName(providerShortName);

        validateParamData(param, res, sat, startTime, endTime);
        Map<String, String> paramMap = new HashMap<>(4, 1);
        paramMap.put(METADATA_ID, id);
        paramMap.put(PARAM_NAME, param);
        paramMap.put(RES_NAME, res);
        paramMap.put(SAT_NAME, sat);

        return paramMap;
    }

    @Override
    public Coverage getCoverage() throws MetaDataParseException {
        Coverage coverage = new Coverage();
        GeometryFactory factory = new GeometryFactory();
        CoordinateReferenceSystem crs;
        String defaultCRS = serviceConfig.getConstantValue("DEFAULT_CRS");
        try {
            crs = BoundingBoxUtil.getCrs(defaultCRS);
        } catch (OgcException e1) {
            throw new MetaDataParseException(
                    "Couldn't determine CRS from value: " + defaultCRS, e1);
        }

        // Trim the leading and trailing parens, then split on parens
        polygonPoints = polygonPoints.replaceAll("^\\(", "");
        polygonPoints = polygonPoints.replaceAll("\\)$", "");

        /*
         * String should contain list of points for one or more polygon. Split
         * the string first by polygon delimited with ")(", then by coordinate
         * delimited with ",", then to lat and lon delimited by spaces. Then
         * work in reverse to build Coordinates from the lat and lon, then
         * Polygons from the Coordinates.
         */
        String[] polyList = polygonPoints.split("\\)\\(");
        Polygon[] polys = new Polygon[polyList.length];
        int p = 0;

        for (String polyCoords : polyList) {
            String[] polyPoints = polyCoords.split(",");
            if (polyPoints.length < 3) {
                throw new MetaDataParseException(
                        "Invalid number of coordinates received for a polygon. Minimum 3 required; received: "
                                + polygonPoints);
            }

            List<Coordinate> coorsList = new ArrayList<>();
            for (String point : polyPoints) {
                String[] coord = point.split(" ");
                /*
                 * EPSG uses lat/lon instead of lon/lat. A similar check exists
                 * in the BoundingBoxUtils class that will flip the values when
                 * they are read back out, so put them in backwards here.
                 */
                if (CrsLookup.isEpsgGeoCrs(crs)) {
                    coorsList.add(new Coordinate(Double.parseDouble(coord[0]),
                            Double.parseDouble(coord[1])));
                } else {
                    coorsList.add(new Coordinate(Double.parseDouble(coord[1]),
                            Double.parseDouble(coord[0])));
                }
            }
            // Check to make sure the polygon is closed. If not, close it.
            if (!coorsList.get(0).equals(coorsList.get(coorsList.size() - 1))) {
                logger.info(
                        "Polygon not closed. Adding closing point to polygon points list: "
                                + polygonPoints);
                coorsList.add(coorsList.get(0));
            }
            /*
             * coorsList.toArray() is unable to cast objects to Coordinates, so
             * do it manually.
             */
            Coordinate[] coors = new Coordinate[coorsList.size()];
            for (int c = 0; c < coors.length; c++) {
                coors[c] = coorsList.get(c);
            }
            LinearRing lr = factory.createLinearRing(coors);
            polys[p] = factory.createPolygon(lr, null);
            p++;
        }

        if (polyList.length > 1) {
            logger.warn("Encountered metadata with multiple polygons ("
                    + polyList.length + ") making up its Geometry! "
                    + "Envelope used will be the envelope containing all polygons.");
        }

        /*
         * Combine all Polygons into a MultiPolyon, then get the envelope
         * containing the polygons. Use the upper and lower corners of that
         * envelope and the crs to create a ReferencedEnvelope for the Coverage
         * area.
         */
        MultiPolygon multiPoly = new MultiPolygon(polys, factory);
        Geometry envelope = multiPoly.getEnvelope();
        Coordinate[] corners = envelope.getCoordinates();
        logger.info("Envelope Coods: (" + corners[0].x + "," + corners[0].y
                + ") to (" + corners[2].x + "," + corners[2].y + ")");

        List<Double> lc = Arrays.asList(corners[0].x, corners[0].y);
        List<Double> uc = Arrays.asList(corners[2].x, corners[2].y);
        DirectPosition min = BoundingBoxUtil.convert(lc);
        DirectPosition max = BoundingBoxUtil.convert(uc);

        try {
            coverage.setEnvelope(
                    BoundingBoxUtil.convert2D(min, max, defaultCRS));
        } catch (OgcException e) {
            throw new MetaDataParseException(
                    "Couldn't determine BoundingBox envelope!", e);
        }

        return coverage;
    }

    /**
     * Validate the MetaDataPatterns required for this MetaDataExtractor.
     *
     * @param mdpMap
     * @param errors
     * @return
     */
    public static boolean validateMetaDataPatterns(
            Map<String, MetaDataPattern> mdpMap, StringBuilder errors) {
        boolean valid = true;
        MetaDataPattern idMdp = mdpMap.get(CONFIG);
        if (idMdp != null) {
            Map<String, PatternGroup> mdpGroups = idMdp.getGroupMap();
            if (mdpGroups != null && !mdpGroups.isEmpty()) {
                List<String> reqGroups = Arrays.asList(SHORT_NAME_FORMAT,
                        START_TIME_FORMAT, END_TIME_FORMAT, CREATE_TIME_FORMAT,
                        RECEIVED_TIME_FORMAT, SAT_FORMAT, POLYGON_FORMAT,
                        URN_FORMAT);
                for (String group : reqGroups) {
                    if (mdpGroups.get(group) == null) {
                        valid = false;
                        errors.append("'" + group + "' group needed for '"
                                + CONFIG + "' pattern. ");
                    }
                }
            } else {
                valid = false;
                errors.append("No groups found for '" + CONFIG + "' pattern. ");
            }
        } else {
            valid = false;
            errors.append(
                    "A '" + CONFIG + "' pattern is required and missing. ");
        }
        return valid;
    }
}
