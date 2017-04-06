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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.datadelivery.registry.Collection;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataLevelType;
import com.raytheon.uf.common.datadelivery.registry.DataLevelType.LevelType;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.EnvelopeUtils;
import com.raytheon.uf.common.datadelivery.registry.PDADataSet;
import com.raytheon.uf.common.datadelivery.registry.PDADataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.MetaDataPattern;
import com.raytheon.uf.common.dataplugin.exception.MalformedDataException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.ImmutableDate;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.MetaDataParser;
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

/**
 * Parse PDA metadata
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 17, 2014  3120     dhladky   Initial creation
 * Sep 04, 2014  3121     dhladky   Adjustments to parsing, simulation.
 * Sep 27, 2014  3127     dhladky   Added metaDataID for geographic subsetting.
 * Apr 27, 2015  4881     dhladky   PDA changed the structure of the file format
 *                                  messages.
 * Jan 20, 2016  5280     dhladky   Don't send datasetname separately.
 * Feb 16, 2016  5365     dhladky   Streamlined to exclude metaData updates for
 *                                  getRecords().
 * Jul 13, 2016  5752     tjensen   Refactor parseMetaData.
 * Jul 22, 2016  5752     tjensen   Add additional logging information
 * Aug 11, 2016  5752     tjensen   Removed unnecessary reordering in
 *                                  getCoverage
 * Aug 25, 2016  5752     tjensen   Change MetaData date to use start instead of
 *                                  create
 * Jan 27, 2017  6089     tjensen   Update to work with pipe delimited metadata
 * Apr 05, 2017  1045     tjensen   Update for moving datasets
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDAMetaDataParser<O> extends MetaDataParser<BriefRecordType> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDAMetaDataParser.class);

    private static final String FILL_VALUE = "fillValue";

    private static final String MISSING_VALUE = "missingValue";

    private static final String UNITS = "units";

    private static final String FORMAT = "format";

    private static final String COLLECTION_NAME = "collectionName";

    private static final String DATASET_NAME = "dataSetName";

    private static final String PARAM_NAME = "paramName";

    private static final String START_TIME = "startTime";

    private static final String END_TIME = "endTime";

    /** DEBUG PDA system **/
    private static final String DEBUG = "DEBUG";

    /** debug state */
    protected boolean debug = false;

    public PDAMetaDataParser() {
        serviceConfig = HarvesterServiceManager.getInstance()
                .getServiceConfig(ServiceType.PDA);
        // debugging MetaData parsing.
        String debugVal = serviceConfig.getConstantValue(DEBUG);
        debug = Boolean.valueOf(debugVal);
    }

    /**
     * Non impl in PDA
     */
    @Override
    public List<DataSetMetaData<?, ?>> parseMetaData(Provider provider,
            BriefRecordType record, Collection collection,
            String dataDateFormat) {
        throw new UnsupportedOperationException(
                "Not implemented for this type");
    }

    @Override
    public void parseMetaData(Provider provider, BriefRecordType record,
            boolean isMetaData) {

        Map<String, String> paramMap = null;

        Date arrivalTime = TimeUtil.newGmtCalendar().getTime();
        String dsName = null;
        // Geo coverage of information
        Coverage coverage = null;
        // extract time information for record
        Time time = null;
        // immutable date used for data set metadata
        ImmutableDate idate = null;
        // METADATA ID, unique to each record
        String metaDataID = record.getIdentifier().get(0).getValue()
                .getContent().get(0);
        // physical path relative to root provider URL
        String relativeDataURL = record.getTitle().get(0).getValue()
                .getContent().get(0);

        // metadata URL is the full URL path, (not relative) to the file
        // tack on the root provider URL from the provider connection.
        String metaDataURL = provider.getConnection().getUrl() + "/"
                + relativeDataURL;

        if (debug) {
            statusHandler.info("metaDataID: " + metaDataID);
            statusHandler.info("relativeURL: " + relativeDataURL);
            statusHandler.info("metaDataURL: " + metaDataURL);
        }

        PDAFileMetaDataExtractor extractor = PDAFileMetaDataExtractor
                .getInstance();

        // Check to see if pipe delimited id pattern is used.
        MetaDataPattern mdp = extractor.getMetaDataPattern("RECORD_ID");
        if (mdp != null) {
            Pattern p_id = mdp.getPattern();
            Matcher m = p_id.matcher(metaDataID);
            String dateFormat = mdp.getDateFormat();
            if (m.matches()) {
                try {
                    statusHandler
                            .info("Extracting metadata from metadataID...");
                    paramMap = extractor.extractMetaData(metaDataID);
                } catch (Exception e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "MetaData extraction error, " + metaDataID, e);
                    // failure return
                    return;
                }
                dsName = paramMap.get(DATASET_NAME);

                // Only store the metadata Id itself, not the whole field
                String newId = paramMap.get("metadataId");
                if (newId != null && !"".equals(newId)) {
                    metaDataID = newId;
                }

                // Lookup for coverage information
                String polygonPoints = paramMap.get("polygonPoints");
                if (polygonPoints != null && !("".equals(polygonPoints))) {
                    try {
                        coverage = getCoverage(polygonPoints);
                    } catch (MalformedDataException e) {
                        statusHandler.error("Invalid Polygon data", e);
                    }
                } else {
                    statusHandler.warn(
                            "Geometry missing for dataset '" + dsName + "'");
                }
            } else {
                // If not, extract the metadata from the title
                try {
                    statusHandler.info("Extracting metadata from title...");
                    paramMap = extractor
                            .extractMetaDataFromTitle(relativeDataURL);
                } catch (MetaDataExtractionException e) {
                    statusHandler.error("MetaData extraction error on "
                            + relativeDataURL + ".", e);
                    // failure return
                    return;
                } catch (Exception e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "MetaData extraction error, " + relativeDataURL, e);
                    // failure return
                    return;
                }
                dsName = paramMap.get(DATASET_NAME);

                // set the dateFormat to be for this pattern
                MetaDataPattern mdpt = extractor
                        .getMetaDataPattern("RECORD_TITLE");
                dateFormat = mdpt.getDateFormat();

                // Lookup for coverage information
                if (!record.getBoundingBox().isEmpty()) {
                    coverage = getCoverage(
                            record.getBoundingBox().get(0).getValue());
                } else {
                    statusHandler.warn("Bounding box is empty for dataset '"
                            + dsName + "'");
                }
            }

            // Common regardless of extraction method
            if ("true".equals(paramMap.get("ignoreData"))) {
                statusHandler.info("Skipping DataSet '" + dsName + "'");
                return;
            }
            setDefaultParams(paramMap);

            // use real time parsed from file
            try {
                time = getTime(paramMap.get(START_TIME), paramMap.get(END_TIME),
                        dateFormat);
                idate = new ImmutableDate(time.getStart());
            } catch (ParseException e) {
                statusHandler.error(
                        "Couldn't parse start (" + paramMap.get(START_TIME)
                                + ")/end (" + paramMap.get(END_TIME)
                                + ") time from format: " + dateFormat,
                        e);
                return;
            }

            String providerName = provider.getName();
            boolean isMoving = getIsMovingFromConfig(dsName, providerName);

            /**
             * This portion of the MetaData parser is only used when a coverage
             * exists.
             */
            if (coverage != null) {

                statusHandler.info("Preparing to store DataSet: " + dsName);
                PDADataSet pdaDataSet = new PDADataSet();
                pdaDataSet.setCollectionName(paramMap.get(COLLECTION_NAME));
                pdaDataSet.setDataSetName(dsName);
                pdaDataSet.setProviderName(providerName);
                pdaDataSet.setDataSetType(DataType.PDA);
                pdaDataSet.setTime(time);
                pdaDataSet.setArrivalTime(arrivalTime.getTime());
                // there is only one parameter per briefRecord at this point
                Map<String, Parameter> parameters = getParameters(
                        paramMap.get(PARAM_NAME), paramMap.get(COLLECTION_NAME),
                        providerName, paramMap.get(UNITS),
                        paramMap.get(FILL_VALUE), paramMap.get(MISSING_VALUE));
                pdaDataSet.setParameters(parameters);
                // set the coverage
                pdaDataSet.setCoverage(coverage);
                // Store the parameter, data Set name, data Set
                for (Entry<String, Parameter> parm : parameters.entrySet()) {
                    storeParameter(parm.getValue());
                }

                pdaDataSet.applyInfoFromConfig(isMoving,
                        getParentBoundsFromConfig(dsName, providerName,
                                coverage.getEnvelope()
                                        .getCoordinateReferenceSystem()),
                        getSizeEstFromConfig(dsName, providerName));

                storeDataSet(pdaDataSet);
            } else {
                statusHandler.warn("Coverage is null for: " + dsName);
            }

            /*
             * This portion of the code is only used when processing a
             * Transaction.
             */
            if (isMetaData) {

                PDADataSetMetaData pdadsmd = new PDADataSetMetaData();
                pdadsmd.setMetaDataID(metaDataID);
                pdadsmd.setArrivalTime(arrivalTime.getTime());
                pdadsmd.setAvailabilityOffset(getDataSetAvailabilityTime(
                        paramMap.get(COLLECTION_NAME),
                        time.getStart().getTime()));
                pdadsmd.setTime(time);
                pdadsmd.setDataSetName(dsName);
                pdadsmd.setDataSetDescription(metaDataID + " " + DATASET_NAME);
                pdadsmd.setDate(idate);
                pdadsmd.setProviderName(providerName);

                /*
                 * If this is a moving dataset, save the coverage for this
                 * specific instance of the product in the metadata instead of
                 * on the dataset itself. Coverage on the dataset will store the
                 * 'parent coverage' (the outer bounds of where this product can
                 * be positioned).
                 */
                if (isMoving) {
                    pdadsmd.setInstanceCoverage(coverage);
                }
                // In PDA's case it's actually a file name
                pdadsmd.setUrl(metaDataURL);

                storeMetaData(pdadsmd);
            }
        }
    }

    private void setDefaultParams(Map<String, String> paramMap) {
        // these aren't satisfied with the file format parsing, use
        // defaults and pray
        paramMap.put(FILL_VALUE, serviceConfig.getConstantValue("FILL_VALUE"));
        paramMap.put(MISSING_VALUE,
                serviceConfig.getConstantValue("MISSING_VALUE"));
        paramMap.put(UNITS, serviceConfig.getConstantValue("UNITS"));
        paramMap.put(FORMAT, serviceConfig.getConstantValue("DEFAULT_FORMAT"));
    }

    private Coverage getCoverage(String polygonPoints)
            throws MalformedDataException {
        Coverage coverage = new Coverage();
        GeometryFactory factory = new GeometryFactory();
        CoordinateReferenceSystem crs;
        String defaultCRS = serviceConfig.getConstantValue("DEFAULT_CRS");
        try {
            crs = BoundingBoxUtil.getCrs(defaultCRS);
        } catch (OgcException e1) {
            statusHandler.handle(Priority.PROBLEM,
                    "Couldn't determine CRS from value: " + defaultCRS, e1);
            return coverage;
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
                throw new MalformedDataException(
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
                statusHandler
                        .info("Polygon not closed. Adding closing point to polygon points list: "
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
            statusHandler.warn("Encountered metadata with multiple polygons ("
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
        statusHandler
                .info("Envelope Coods: (" + corners[0].x + "," + corners[0].y
                        + ") to (" + corners[2].x + "," + corners[2].y + ")");

        List<Double> lc = Arrays.asList(corners[0].x, corners[0].y);
        List<Double> uc = Arrays.asList(corners[2].x, corners[2].y);
        DirectPosition min = BoundingBoxUtil.convert(lc);
        DirectPosition max = BoundingBoxUtil.convert(uc);

        try {
            coverage.setEnvelope(
                    BoundingBoxUtil.convert2D(min, max, defaultCRS));
        } catch (OgcException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Couldn't determine BoundingBox envelope!", e);
        }

        return coverage;
    }

    /**
     * Set the Coverage from the OGC BoundingBox
     * 
     * @param boundingBoxType
     * @param provider
     */
    private Coverage getCoverage(
            net.opengis.ows.v_1_0_0.BoundingBoxType boundingBoxType) {

        ReferencedEnvelope envelope = null;
        Coverage coverage = new Coverage();

        try {

            if (debug) {
                statusHandler.info("Parsed LOWER CORNER: "
                        + boundingBoxType.getLowerCorner().get(0) + ", "
                        + boundingBoxType.getLowerCorner().get(1) + " size: "
                        + boundingBoxType.getLowerCorner().size());
                statusHandler.info("Parsed UPPER CORNER: "
                        + boundingBoxType.getUpperCorner().get(0) + ", "
                        + boundingBoxType.getUpperCorner().get(1) + " size: "
                        + boundingBoxType.getUpperCorner().size());
                statusHandler.info("CRS: " + boundingBoxType.getCrs());
                statusHandler
                        .info("Dimensions: " + boundingBoxType.getDimensions());
            }

            envelope = BoundingBoxUtil.convert2D(boundingBoxType);

            Coordinate ul = EnvelopeUtils.getUpperLeftLatLon(envelope);
            Coordinate lr = EnvelopeUtils.getLowerRightLatLon(envelope);
            statusHandler.info("Envelope Coods: (" + ul.x + "," + ul.y
                    + ") to (" + lr.x + "," + lr.y + ")");

            coverage.setEnvelope(envelope);

        } catch (OgcException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Couldn't determine BoundingBox envelope!", e);
        }

        return coverage;
    }

    /**
     * Set the time object
     * 
     * @param dateFormat
     * @param startTime
     * @param endTime
     * @return
     * @throws ParseException
     */
    private Time getTime(String startTime, String endTime, String dateFormat)
            throws ParseException {

        Time time = new Time();
        time.setFormat(dateFormat);

        time.setStartDate(startTime);
        time.setEndDate(endTime);

        return time;
    }

    private Map<String, Parameter> getParameters(String paramName,
            String collectionName, String provider, String units,
            String fillValue, String missingValue) {

        Map<String, Parameter> params = new HashMap<>(1);

        Parameter parm = new Parameter();
        parm = new Parameter();
        parm.setName(paramName);
        // in this case there isn't any diff
        parm.setProviderName(provider);
        parm.setDefinition(provider + "-" + collectionName + "-" + paramName);
        parm.setBaseType(serviceConfig.getConstantValue("BASE_TYPE"));

        // fill value
        if (fillValue == null) {
            parm.setFillValue(serviceConfig.getConstantValue("FILL_VALUE"));
        } else {
            parm.setFillValue(fillValue);
        }

        // missing value
        if (missingValue == null) {
            parm.setMissingValue(
                    serviceConfig.getConstantValue("MISSING_VALUE"));
        } else {
            parm.setMissingValue(missingValue);
        }

        // units
        if (units == null) {
            parm.setUnits(serviceConfig.getConstantValue("UNITS"));
        } else {
            parm.setUnits(units);
        }

        /*
         * The standard default for satellite data is EA (Entire Atmosphere).
         */
        LevelType type = LevelType.EA;
        DataLevelType dlt = new DataLevelType(type);
        ArrayList<DataLevelType> types = new ArrayList<>(1);
        types.add(dlt);
        // set the level types
        parm.setLevelType(types);
        // Set the data type
        parm.setDataType(DataType.PDA);
        params.put(collectionName, parm);

        return params;
    }

}
