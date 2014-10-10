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

/**
 * Parse PDA metadata
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * June 17, 2014 3120        dhladky     Initial creation
 * Sept 04, 2014 3121        dhladky     Adjustments to parsing, simulation.
 * Sept 27, 2014 3127        dhladky     Added metaDataID for geographic subsetting.
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDAMetaDataParser<O> extends MetaDataParser<BriefRecordType> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDAMetaDataParser.class);
    
    private static final String fillValue = "fillValue";
    private static final String missingValue = "missingValue";
    private static final String units = "units";
    private static final String format = "format";
    private static final String collectionName = "collectionName";
    private static final String satelliteName = "satelliteName";
    private static final String dataSetName = "dataSetName";
    private static final String paramName = "paramName";
    private static final String startTime = "startTime";
    private static final String endTime = "endTime";
    private static final String dataTime = "dataTime";
    
    private String mode = null;
    
    private String dateFormat = null;

    public PDAMetaDataParser() {
        serviceConfig = HarvesterServiceManager.getInstance().getServiceConfig(
                ServiceType.PDA);
        setMode(serviceConfig.getConstantValue("MODE"));
    }
    
    /**
     * PDA extraction mode
     * 
     * <pre>
     * 
     * SOFTWARE HISTORY
     * 
     * Date         Ticket#    Engineer    Description
     * ------------ ---------- ----------- --------------------------
     * Jul 08, 2014  #3120     dhladky     Initial creation
     * 
     * </pre>
     * 
     * @author dhladky
     * @version 1.0
     */
    enum MODE {

        FILE("file"), OGC("ogc");

        private final String mode;

        private MODE(String name) {
            mode = name;
        }

        public String getMode() {
            return mode;
        }
    }
   
    /**
     * Non impl in PDA
     */
    @Override
    public List<DataSetMetaData<?>> parseMetaData(Provider provider,
            BriefRecordType record, Collection collection, String dataDateFormat) {
        throw new UnsupportedOperationException("Not implemented for this type");
    }

    /*
     * (non-Javadoc)
     * @see com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IParseMetaData#parseMetaData(com.raytheon.uf.common.datadelivery.registry.Provider, java.lang.String, java.lang.Object)
     */
    @Override
    public void parseMetaData(Provider provider, String dateFormat,
            BriefRecordType record) {

        Map<String, String> paramMap = null;
        PDAMetaDataExtractor<?,String> extractor = null;
        
        Date arrivalTime = TimeUtil.newGmtCalendar().getTime();
        // Geo coverage of information
        Coverage coverage = null;
        // extract time information for record
        Time time = null;
        // immutable date used for data set metadata
        ImmutableDate idate = null;
        // METADATA ID, unique to each record
        String metaDataID = record.getIdentifier().get(0).getValue().getContent().get(0);
        // physical path relative to root provider URL
        String relativeDataURL = record.getTitle().get(0).getValue().getContent().get(0);
        // metadata URL is the full URL path, (not relative) to the file
        // tack on the root provider URL from the provider connection.
        String metaDataURL = provider.getConnection().getUrl()+relativeDataURL;
        // append the 
        // set date formatter
        setDateFormat(dateFormat);
        
        if (mode.equals(MODE.FILE.getMode())) {
            // interim mode for testing
            extractor = new PDAFileMetaDataExtractor();
             
            try {
                paramMap = extractor.extractMetaData(relativeDataURL);
                // these aren't satisfied with the file format parsing, use defaults and pray
                paramMap.put(fillValue, serviceConfig.getConstantValue("FILL_VALUE"));
                paramMap.put(missingValue, serviceConfig.getConstantValue("MISSING_VALUE"));
                paramMap.put(units, serviceConfig.getConstantValue("UNITS"));
                paramMap.put(format, serviceConfig.getConstantValue("DEFAULT_FORMAT"));
                
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM, "MetaData extraction error, "+relativeDataURL, e);
                // failure return
                return;
            }
           
        } else {
            // assume OGC BriefRecord complete processing
            extractor = new PDARecordMetaDataExtractor();
            
            try {
                paramMap = extractor.extractMetaData(record);
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM, "MetaData extraction error, "+relativeDataURL, e);
                // failure return
                return;
            }
        }
      
        // set the coverage
        if (!record.getBoundingBox().isEmpty()) {
            coverage = getCoverage(record.getBoundingBox().get(0).getValue());
        }
        // These are the params for setting up metadata/datasets/params
        String myCollectionName = paramMap.get(collectionName);
        String relation = paramMap.get(satelliteName);
        String dataSet  = paramMap.get(dataSetName);
        String myParamName  = paramMap.get(paramName);
        String myStartTime = paramMap.get(startTime);
        String myEndTime = paramMap.get(endTime);
        String myDataTime = paramMap.get(dataTime);
        String myFillValue = paramMap.get(fillValue);
        String myMissingValue = paramMap.get(missingValue);
        String myUnits = paramMap.get(units);
        String myFormat = paramMap.get(format);
        
        // set the time object
        if (startTime != null && endTime != null) {
            // If you are using "canned" data, it doesn't work with the subscription creation system
            // based on a 48 hour window of retrievals.  Setup a simulated, recent time that does.
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
                time = getTime(myStartTime, myEndTime);
                
                try {
                    idate = new ImmutableDate(time.parseDate(myDataTime));
                } catch (ParseException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Couldn't parse dataTime, " + relativeDataURL, e);
                }
            }
        }
               
        // create data set Name      
        String dataSetName = createDataSetName(myCollectionName, relation, dataSet, myFormat);
        PDADataSet pdaDataSet = new PDADataSet();
        pdaDataSet.setCollectionName(myCollectionName);
        pdaDataSet.setDataSetName(dataSetName);
        pdaDataSet.setProviderName(provider.getName());
        pdaDataSet.setDataSetType(DataType.PDA);
        pdaDataSet.setTime(time);
        pdaDataSet.setArrivalTime(arrivalTime.getTime());
        // there is only one parameter per briefRecord at this point
        Map<String, Parameter> parameters = getParameters(myParamName, myCollectionName, provider.getName(), myUnits, myFillValue, myMissingValue);
        pdaDataSet.setParameters(parameters);
        // set the coverage
        pdaDataSet.setCoverage(coverage);
        // Store the parameter, data Set name, data Set
        for (Entry<String, Parameter> parm: parameters.entrySet()) {
            storeParameter(parm.getValue());
        }
        storeDataSetName(pdaDataSet);
        storeDataSet(pdaDataSet);
        
        // create Data Set MetaData
        PDADataSetMetaData pdadsmd = new PDADataSetMetaData();
        pdadsmd.setMetaDataID(metaDataID);
        pdadsmd.setArrivalTime(arrivalTime.getTime());
        pdadsmd.setAvailabilityOffset(getDataSetAvailabilityTime(myCollectionName, time.getStart().getTime()));
        pdadsmd.setDataSetName(dataSetName);
        pdadsmd.setDataSetDescription(metaDataID + " " + pdaDataSet.getDataSetName());
        pdadsmd.setDate(idate);
        pdadsmd.setProviderName(provider.getName());
        // in PDA's case it's actually a file name
        pdadsmd.setUrl(metaDataURL);
        
        // store the metaData
        storeMetaData(pdadsmd);
    }

    /**
     * Set the Coverage from the OGC BoundingBox
     * 
     * @param boundingBoxType
     * @param provider
     */
    private Coverage getCoverage(
            net.opengis.ows.v_1_0_0.BoundingBoxType boundingBoxType) {


        if (boundingBoxType.getCrs() == null) {
            boundingBoxType.setCrs(serviceConfig.getConstantValue("DEFAULT_CRS"));
        }
        
        ReferencedEnvelope envelope = null;
        Coverage coverage = new Coverage();

        try {
            envelope = BoundingBoxUtil.convert2D(boundingBoxType);
            coverage.setEnvelope(envelope);

        } catch (OgcException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Couldn't determine BoundingBox envelope!", e);
        }

        return coverage;
    }

    /**
     * Set the time object
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
           statusHandler.handle(Priority.PROBLEM, "Couldn't parse start/end time from format: "+dateFormat, e);
        }

        return time;
    }

    private Map<String, Parameter> getParameters(String channelName, String collectionName,
            String provider, String units, String fillValue, String missingValue) {

        Map<String, Parameter> params = new HashMap<String, Parameter>(1);

        Parameter parm = new Parameter();
        parm = new Parameter();
        String paramName = channelName;
        parm.setName(paramName);
        // in this case there isn't any diff
        parm.setProviderName(channelName);
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
            parm.setMissingValue(serviceConfig.getConstantValue("MISSING_VALUE"));
        } else {
            parm.setMissingValue(missingValue);
        }
        
        // units
        if (units == null) {
            parm.setUnits(serviceConfig.getConstantValue("UNITS"));
        } else {
            parm.setUnits(units);
        }
        
        // it's understood at this moment that all satellite derived PDA types are surface
        // at least for now..... We'll see how long that holds true.
        LevelType type = LevelType.SFC;
        DataLevelType dlt = new DataLevelType(type);
        ArrayList<DataLevelType> types = new ArrayList<DataLevelType>(1);
        types.add(dlt);
        // set the level types
        parm.setLevelType(types);
        // Set the data type
        parm.setDataType(DataType.PDA);
        params.put(collectionName, parm);

        return params;
    }
        
    /**
     * Gets the datasetName, some what descriptive name
     * @param creator
     * @param relation
     * @param dataSet
     * @param channel
     * @param format
     * @return
     */
    private String createDataSetName(String creator, String relation, String dataSet, String format) {

        String separator = serviceConfig.getConstantValue("SEPARATOR");
             
        StringBuilder buf = new StringBuilder();
        buf.append(creator);
        buf.append(separator);
        buf.append(relation);
        buf.append(separator);
        buf.append(dataSet);
        buf.append(separator);
        buf.append(format);
        
        return buf.toString();

    }

    /**
     * Get Extraction Mode
     * @return
     */
    public String getMode() {
        return mode;
    }

    /**
     * Set extraction Mode
     * @param mode
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Get date formatter used to parse dates
     * @return
     */
    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * set Date formatter used to parse dates
     * @param dateFormat
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
   
}
