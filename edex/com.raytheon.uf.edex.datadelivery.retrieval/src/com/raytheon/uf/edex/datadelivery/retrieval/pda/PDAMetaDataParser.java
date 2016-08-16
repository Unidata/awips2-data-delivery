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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.opengis.cat.csw.v_2_0_2.BriefRecordType;

import org.geotools.geometry.jts.ReferencedEnvelope;

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
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.ImmutableDate;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.MetaDataParser;
import com.raytheon.uf.edex.ogc.common.OgcException;
import com.raytheon.uf.edex.ogc.common.spatial.BoundingBoxUtil;
import com.vividsolutions.jts.geom.Coordinate;

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

    private static final String DATA_TIME = "dataTime";

    /** DEBUG PDA system **/
    private static final String DEBUG = "DEBUG";

    /** debug state */
    protected Boolean debug = false;

    private String dateFormat = null;

    public PDAMetaDataParser() {
        serviceConfig = HarvesterServiceManager.getInstance().getServiceConfig(
                ServiceType.PDA);
        // debugging MetaData parsing.
        String debugVal = serviceConfig.getConstantValue(DEBUG);
        debug = Boolean.valueOf(debugVal);
    }

    /**
     * Non impl in PDA
     */
    @Override
    public List<DataSetMetaData<?>> parseMetaData(Provider provider,
            BriefRecordType record, Collection collection, String dataDateFormat) {
        throw new UnsupportedOperationException("Not implemented for this type");
    }

    @Override
    public void parseMetaData(Provider provider, String dateFormat,
            BriefRecordType record, boolean isMetaData) {

        Map<String, String> paramMap = null;
        PDAMetaDataExtractor<?, String> extractor = null;

        Date arrivalTime = TimeUtil.newGmtCalendar().getTime();
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

        if (debug == true) {
            statusHandler.info("metaDataID: " + metaDataID);
            statusHandler.info("relativeURL: " + relativeDataURL);
            statusHandler.info("metaDataURL: " + metaDataURL);
        }
        // append the
        // set date formatter
        setDateFormat(dateFormat);

        extractor = PDAFileMetaDataExtractor.getInstance();

        try {
            paramMap = extractor.extractMetaData(relativeDataURL);
            // these aren't satisfied with the file format parsing, use
            // defaults and pray
            paramMap.put(FILL_VALUE,
                    serviceConfig.getConstantValue("FILL_VALUE"));
            paramMap.put(MISSING_VALUE,
                    serviceConfig.getConstantValue("MISSING_VALUE"));
            paramMap.put(UNITS, serviceConfig.getConstantValue("UNITS"));
            paramMap.put(FORMAT,
                    serviceConfig.getConstantValue("DEFAULT_FORMAT"));

        } catch (MetaDataExtractionException e) {
            // Don't need to print a stack trace for this.
            statusHandler
                    .error("MetaData extraction error, " + relativeDataURL);
            // failure return
            return;
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "MetaData extraction error, " + relativeDataURL, e);
            // failure return
            return;
        }
        if ("false".equals(paramMap.get("ignoreData"))) {

            // set the time object
            /*
             * If you are using "canned" data, it doesn't work with the
             * subscription creation system based on a 48 hour window of
             * retrievals. Setup a simulated, recent time that does.
             */
            if (serviceConfig.getConstantValue("SIMULATE").equals("true")) {
                // make up a recent time
                long currtime = TimeUtil.currentTimeMillis();
                // make it 5 minutes old
                long etime = currtime - (1000 * 60 * 5);
                // make it 10 minutes old
                long stime = currtime - (1000 * 60 * 10);

                time = new Time();
                time.setFormat(getDateFormat());
                time.setStart(new Date(stime));
                time.setEnd(new Date(etime));

                idate = new ImmutableDate(time.getStart());

            } else {
                // use real time parsed from file
                time = getTime(paramMap.get(START_TIME), paramMap.get(END_TIME));

                try {
                    idate = new ImmutableDate(time.parseDate(paramMap
                            .get(DATA_TIME)));
                } catch (ParseException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Couldn't parse dataTime, " + relativeDataURL, e);
                }
            }

            // Lookup for coverage information
            if (!record.getBoundingBox().isEmpty()) {
                coverage = getCoverage(record.getBoundingBox().get(0)
                        .getValue());
            } else {
                statusHandler.warn("Bounding box is empty for dataset '"
                        + paramMap.get(DATASET_NAME) + "'");
            }

            /**
             * This portion of the MetaData parser is only used when a coverage
             * exists.
             */
            if (coverage != null) {

                statusHandler.info("Preparing to store DataSet: "
                        + paramMap.get(DATASET_NAME));
                PDADataSet pdaDataSet = new PDADataSet();
                pdaDataSet.setCollectionName(paramMap.get(COLLECTION_NAME));
                pdaDataSet.setDataSetName(paramMap.get(DATASET_NAME));
                pdaDataSet.setProviderName(provider.getName());
                pdaDataSet.setDataSetType(DataType.PDA);
                pdaDataSet.setTime(time);
                pdaDataSet.setArrivalTime(arrivalTime.getTime());
                // there is only one parameter per briefRecord at this point
                Map<String, Parameter> parameters = getParameters(
                        paramMap.get(PARAM_NAME),
                        paramMap.get(COLLECTION_NAME), provider.getName(),
                        paramMap.get(UNITS), paramMap.get(FILL_VALUE),
                        paramMap.get(MISSING_VALUE));
                pdaDataSet.setParameters(parameters);
                // set the coverage
                pdaDataSet.setCoverage(coverage);
                // Store the parameter, data Set name, data Set
                for (Entry<String, Parameter> parm : parameters.entrySet()) {
                    storeParameter(parm.getValue());
                }

                storeDataSet(pdaDataSet);
            } else {
                statusHandler.warn("Coverage is null for: "
                        + paramMap.get(DATASET_NAME));
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
                        paramMap.get(COLLECTION_NAME), time.getStart()
                                .getTime()));
                pdadsmd.setTime(time);
                pdadsmd.setDataSetName(paramMap.get(DATASET_NAME));
                pdadsmd.setDataSetDescription(metaDataID + " " + DATASET_NAME);
                pdadsmd.setDate(idate);
                pdadsmd.setProviderName(provider.getName());
                // In PDA's case it's actually a file name
                pdadsmd.setUrl(metaDataURL);

                storeMetaData(pdadsmd);
            }
        } else {
            statusHandler.info("Skipping DataSet '"
                    + paramMap.get(DATASET_NAME)
                    + "' due to parameter being in exclusion list");
        }
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

            if (debug == true) {
                statusHandler.info("Parsed LOWER CORNER: "
                        + boundingBoxType.getLowerCorner().get(0) + ", "
                        + boundingBoxType.getLowerCorner().get(1) + " size: "
                        + boundingBoxType.getLowerCorner().size());
                statusHandler.info("Parsed UPPER CORNER: "
                        + boundingBoxType.getUpperCorner().get(0) + ", "
                        + boundingBoxType.getUpperCorner().get(1) + " size: "
                        + boundingBoxType.getUpperCorner().size());
                statusHandler.info("CRS: " + boundingBoxType.getCrs());
                statusHandler.info("Dimensions: "
                        + boundingBoxType.getDimensions());
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
     */
    private Time getTime(String startTime, String endTime) {

        Time time = new Time();
        time.setFormat(getDateFormat());

        try {
            time.setStartDate(startTime);
            time.setEndDate(endTime);
        } catch (ParseException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Couldn't parse start/end time from format: " + dateFormat,
                    e);
        }

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
            parm.setMissingValue(serviceConfig
                    .getConstantValue("MISSING_VALUE"));
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

    /**
     * Get date formatter used to parse dates
     * 
     * @return
     */
    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * set Date formatter used to parse dates
     * 
     * @param dateFormat
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

}
