package com.raytheon.uf.edex.datadelivery.retrieval.pda;

import java.io.File;

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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.MetaDataPattern;
import com.raytheon.uf.common.datadelivery.retrieval.xml.PDAMetaDataConfig;
import com.raytheon.uf.common.datadelivery.retrieval.xml.PatternGroup;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.ImmutableDate;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.util.PDADataSetNameMap;
import com.raytheon.uf.edex.datadelivery.retrieval.util.PDADataSetNameMapSet;
import com.raytheon.uf.edex.datadelivery.retrieval.util.PDADescriptionMap;
import com.raytheon.uf.edex.datadelivery.retrieval.util.PDADescriptionMapSet;
import com.raytheon.uf.edex.datadelivery.retrieval.util.PDAParameterExclusions;
import com.raytheon.uf.edex.datadelivery.retrieval.util.PDAShortNameMap;
import com.raytheon.uf.edex.datadelivery.retrieval.util.PDAShortNameMapSet;

/**
 * Extract File Name based info for MetaData from PDA
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 08, 2014  3120     dhladky   Initial creation
 * Sep 04, 2014  3121     dhladky   Some rework on the parsing.
 * Nov 10, 2014  3826     dhladky   Added more logging, improved versatility of
 *                                  parser.
 * Apr 27, 2015  4435     dhladky   PDA added more formats of messages.
 * Sep 14, 2015  4881     dhladky   Updates to PDA processing.
 * Jul 13, 2016  5752     tjensen   Refactor extractMetaData
 * Aug 16, 2016  5752     tjensen   Added options for data set name translation
 * Aug 18, 2016  5752     tjensen   Fix initSatMapping
 * Aug 25, 2016  5752     tjensen   Remove Create Time
 * Sep 01, 2016  5752     tjensen   Exclude data older than retention period
 * Jan 27, 2017  6089     tjensen   Update to work with pipe delimited metadata
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDAFileMetaDataExtractor
        extends PDAMetaDataExtractor<String, String> {

    private static final String HARVESTER_PATH = "datadelivery"
            + File.separatorChar + "harvester" + File.separatorChar;

    private static final String MAPPING_PATH = "datadelivery"
            + File.separatorChar + "mappings" + File.separatorChar;

    private static final String PDA_PROVIDER = "PDA";

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDAFileMetaDataExtractor.class);

    /** remove extension **/
    public static final String timeReplace = "DT_";

    private static PDAFileMetaDataExtractor instance;

    private Map<String, String> dataSetNameMapping = null;

    private Map<String, String> resolutionMapping = null;

    private Map<String, String> satelliteMapping = null;

    private Map<String, PDAShortNameMap> shortNameMapping = null;

    private Map<String, MetaDataPattern> metaDataPatterns = new HashMap<>(1);

    private Set<String> excludeList = null;

    private Map<String, String> dataSetMetadataRetention = null;

    private Map<String, String> dataSetMetadataDateFormat = null;

    public static synchronized PDAFileMetaDataExtractor getInstance() {
        if (instance == null) {
            instance = new PDAFileMetaDataExtractor();
        }
        return instance;
    }

    private PDAFileMetaDataExtractor() {
        super();
        dataSetNameMapping = new HashMap<>();
        resolutionMapping = new HashMap<>();
        satelliteMapping = new HashMap<>();
        excludeList = new HashSet<>();
        dataSetMetadataDateFormat = new HashMap<>();
        dataSetMetadataRetention = new HashMap<>();

        initMappings();
    }

    private void initMappings() {
        statusHandler.info("Initializing DataSet Name mapping...");
        initNameMapping();

        statusHandler.info("Initializing Resolution mapping...");
        initResMapping();

        statusHandler.info("Initializing Satellite mapping...");
        initSatMapping();

        statusHandler.info("Initializing Exclusion list...");
        initExclusionList();

        statusHandler.info("Initializing Harvester Configs...");
        initHarvesterConfigs();

        statusHandler.info("Initializing Short Name mappings...");
        initShortNameMapping();
    }

    private void initNameMapping() {
        String fileName = MAPPING_PATH + "DataNameMappings.xml";
        Map<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);

        PDADataSetNameMapSet nameMapSet = new PDADataSetNameMapSet();
        try {
            JAXBManager jaxb = new JAXBManager(PDADataSetNameMapSet.class);
            if (locFiles.containsKey(LocalizationLevel.BASE)) {
                LocalizationFile file = locFiles.get(LocalizationLevel.BASE);
                PDADataSetNameMapSet fileSet = readDataSetNameMap(jaxb, file);
                if (fileSet != null) {
                    nameMapSet.addMaps(fileSet.getMaps());
                }
            }
            if (locFiles.containsKey(LocalizationLevel.SITE)) {
                LocalizationFile file = locFiles.get(LocalizationLevel.SITE);
                PDADataSetNameMapSet fileSet = readDataSetNameMap(jaxb, file);
                if (fileSet != null) {
                    nameMapSet.addMaps(fileSet.getMaps());
                }
            }
        } catch (JAXBException | SerializationException | IOException
                | LocalizationException e) {
            statusHandler.error("Unable to initialize DataSet name mapping", e);
        }

        addNameMappings(nameMapSet);
    }

    private PDADataSetNameMapSet readDataSetNameMap(JAXBManager jaxb,
            LocalizationFile file) throws SerializationException, IOException,
                    LocalizationException {
        PDADataSetNameMapSet fileSet = null;
        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                fileSet = (PDADataSetNameMapSet) jaxb
                        .unmarshalFromInputStream(is);
            }
        }
        return fileSet;
    }

    private Map<LocalizationLevel, LocalizationFile> getLocalizationFiles(
            String fileName) {
        IPathManager pm = PathManagerFactory.getPathManager();
        Map<LocalizationLevel, LocalizationFile> files = pm
                .getTieredLocalizationFile(LocalizationType.COMMON_STATIC,
                        fileName);
        return files;
    }

    private void initResMapping() {
        String fileName = MAPPING_PATH + "ResolutionMappings.xml";
        Map<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);

        PDADescriptionMapSet resMapSet = new PDADescriptionMapSet();
        try {
            JAXBManager jaxb = new JAXBManager(PDADescriptionMapSet.class);
            if (locFiles.containsKey(LocalizationLevel.BASE)) {
                LocalizationFile file = locFiles.get(LocalizationLevel.BASE);
                PDADescriptionMapSet fileSet = readDescriptionMap(jaxb, file);
                if (fileSet != null) {
                    resMapSet.addMaps(fileSet.getMaps());
                }
            }
            if (locFiles.containsKey(LocalizationLevel.SITE)) {
                LocalizationFile file = locFiles.get(LocalizationLevel.SITE);
                PDADescriptionMapSet fileSet = readDescriptionMap(jaxb, file);
                if (fileSet != null) {
                    resMapSet.addMaps(fileSet.getMaps());
                }
            }
        } catch (JAXBException | SerializationException | IOException
                | LocalizationException e) {
            statusHandler.error("Unable to initialize resolution mapping", e);
        }

        addResMappings(resMapSet);
    }

    private PDADescriptionMapSet readDescriptionMap(JAXBManager jaxb,
            LocalizationFile file) throws IOException, LocalizationException,
                    SerializationException {
        PDADescriptionMapSet fileSet = null;
        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                fileSet = (PDADescriptionMapSet) jaxb
                        .unmarshalFromInputStream(is);
            }
        }
        return fileSet;
    }

    private void initSatMapping() {
        String fileName = MAPPING_PATH + "SatelliteMappings.xml";
        Map<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);

        PDADescriptionMapSet satMapSet = new PDADescriptionMapSet();
        try {
            JAXBManager jaxb = new JAXBManager(PDADescriptionMapSet.class);
            if (locFiles.containsKey(LocalizationLevel.BASE)) {
                LocalizationFile file = locFiles.get(LocalizationLevel.BASE);
                PDADescriptionMapSet fileSet = readDescriptionMap(jaxb, file);
                if (fileSet != null) {
                    satMapSet.addMaps(fileSet.getMaps());
                }
            }
            if (locFiles.containsKey(LocalizationLevel.SITE)) {
                LocalizationFile file = locFiles.get(LocalizationLevel.SITE);
                PDADescriptionMapSet fileSet = readDescriptionMap(jaxb, file);
                if (fileSet != null) {
                    satMapSet.addMaps(fileSet.getMaps());
                }
            }
        } catch (JAXBException | SerializationException | IOException
                | LocalizationException e) {
            statusHandler.error("Unable to initialize satellite mapping", e);
        }

        addSatMappings(satMapSet);
    }

    private void initExclusionList() {
        String fileName = MAPPING_PATH + "ParameterExclusions.xml";
        Map<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);

        try {
            JAXBManager jaxb = new JAXBManager(PDAParameterExclusions.class);
            if (locFiles.containsKey(LocalizationLevel.BASE)) {
                LocalizationFile file = locFiles.get(LocalizationLevel.BASE);
                PDAParameterExclusions fileSet = readParamExclusions(jaxb,
                        file);
                if (fileSet != null) {
                    excludeList.addAll(fileSet.getParameterExclusions());
                }
            }
            if (locFiles.containsKey(LocalizationLevel.SITE)) {
                LocalizationFile file = locFiles.get(LocalizationLevel.SITE);
                PDAParameterExclusions fileSet = readParamExclusions(jaxb,
                        file);
                if (fileSet != null) {
                    excludeList.addAll(fileSet.getParameterExclusions());
                }
            }
        } catch (JAXBException | SerializationException | IOException
                | LocalizationException e) {
            statusHandler.error("Unable to initialize parameter exclusions", e);
        }

    }

    private PDAParameterExclusions readParamExclusions(JAXBManager jaxb,
            LocalizationFile file) throws IOException, LocalizationException,
                    SerializationException {
        PDAParameterExclusions fileSet = null;
        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                fileSet = (PDAParameterExclusions) jaxb
                        .unmarshalFromInputStream(is);
            }
        }
        return fileSet;
    }

    private void initHarvesterConfigs() {
        String fileName = HARVESTER_PATH + "PDA-Harvester.xml";
        Map<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);

        try {
            JAXBManager jaxb = new JAXBManager(HarvesterConfig.class);
            if (locFiles.containsKey(LocalizationLevel.BASE)) {
                LocalizationFile file = locFiles.get(LocalizationLevel.BASE);
                HarvesterConfig fileSet = readHarvesterConfig(jaxb, file);
                if (fileSet != null) {
                    dataSetMetadataRetention.put(
                            fileSet.getProvider().getName(),
                            fileSet.getRetention());
                    dataSetMetadataDateFormat.put(
                            fileSet.getProvider().getName(),
                            fileSet.getAgent().getDateFormat());
                }
            }
            if (locFiles.containsKey(LocalizationLevel.SITE)) {
                LocalizationFile file = locFiles.get(LocalizationLevel.SITE);
                HarvesterConfig fileSet = readHarvesterConfig(jaxb, file);
                if (fileSet != null) {
                    dataSetMetadataRetention.put(
                            fileSet.getProvider().getName(),
                            fileSet.getRetention());
                    dataSetMetadataDateFormat.put(
                            fileSet.getProvider().getName(),
                            fileSet.getAgent().getDateFormat());
                }
            }
        } catch (JAXBException | SerializationException | IOException
                | LocalizationException e) {
            statusHandler.error("Unable to initialize harvester configs", e);
        }

    }

    private HarvesterConfig readHarvesterConfig(JAXBManager jaxb,
            LocalizationFile file) throws IOException, LocalizationException,
                    SerializationException {
        HarvesterConfig fileSet = null;
        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                fileSet = (HarvesterConfig) jaxb.unmarshalFromInputStream(is);
            }
        }
        return fileSet;
    }

    private void initShortNameMapping() {
        String fileName = MAPPING_PATH + "ShortNameMappings.xml";
        Map<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);

        PDAShortNameMapSet snMapSet = new PDAShortNameMapSet();
        try {
            JAXBManager jaxb = new JAXBManager(PDAShortNameMapSet.class);
            if (locFiles.containsKey(LocalizationLevel.BASE)) {
                LocalizationFile file = locFiles.get(LocalizationLevel.BASE);
                PDAShortNameMapSet fileSet = readShortNameMap(jaxb, file);
                if (fileSet != null) {
                    snMapSet.addMaps(fileSet.getMaps());
                }
            }
            if (locFiles.containsKey(LocalizationLevel.SITE)) {
                LocalizationFile file = locFiles.get(LocalizationLevel.SITE);
                PDAShortNameMapSet fileSet = readShortNameMap(jaxb, file);
                if (fileSet != null) {
                    snMapSet.addMaps(fileSet.getMaps());
                }
            }
        } catch (JAXBException | SerializationException | IOException
                | LocalizationException e) {
            statusHandler.error("Unable to initialize DataSet name mapping", e);
        }

        addShortNameMappings(snMapSet);
    }

    private PDAShortNameMapSet readShortNameMap(JAXBManager jaxb,
            LocalizationFile file) throws IOException, LocalizationException,
                    SerializationException {
        PDAShortNameMapSet fileSet = null;
        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                fileSet = (PDAShortNameMapSet) jaxb
                        .unmarshalFromInputStream(is);
            }
        }
        return fileSet;
    }

    private void addNameMappings(PDADataSetNameMapSet nameMapSet) {
        for (PDADataSetNameMap map : nameMapSet.getMaps()) {
            dataSetNameMapping.put(map.getParameter(), map.getDescription());
        }
    }

    private void addShortNameMappings(PDAShortNameMapSet nameMapSet) {
        for (PDAShortNameMap map : nameMapSet.getMaps()) {
            shortNameMapping.put(map.getId(), map);
        }
    }

    private void addResMappings(PDADescriptionMapSet resMapSet) {
        for (PDADescriptionMap map : resMapSet.getMaps()) {
            resolutionMapping.put(map.getKey(), map.getDescription());
        }
    }

    private void addSatMappings(PDADescriptionMapSet satMapSet) {
        for (PDADescriptionMap map : satMapSet.getMaps()) {
            satelliteMapping.put(map.getKey(), map.getDescription());
        }
    }

    public Map<String, String> extractMetaData(Object metadataId)
            throws Exception {
        String metadataIdStr = (String) metadataId;
        Map<String, String> paramMap = new HashMap<>(16);
        MetaDataPattern mdp = getMetaDataPattern("RECORD_ID");
        Pattern idPattern = mdp.getPattern();
        Map<String, PatternGroup> mdpGroups = mdp.getGroupMap();
        String snFormat = mdpGroups.get("SHORT_NAME_FORMAT").getValue();
        String sTimeFormat = mdpGroups.get("START_TIME_FORMAT").getValue();
        String eTimeFormat = mdpGroups.get("END_TIME_FORMAT").getValue();
        String cTimeFormat = mdpGroups.get("CREATE_TIME_FORMAT").getValue();
        String rTimeFormat = mdpGroups.get("RECEIVED_TIME_FORMAT").getValue();
        String satFormat = mdpGroups.get("SAT_FORMAT").getValue();
        String polyFormat = mdpGroups.get("POLYGON_FORMAT").getValue();
        String resFormat = mdpGroups.get("RES_FORMAT").getValue();
        String urnFormat = mdpGroups.get("URN_FORMAT").getValue();

        String idFormat = mdpGroups.get("ID_FORMAT").getValue();
        String instFormat = mdpGroups.get("INSTRUMENT_FORMAT").getValue();
        String modeFormat = mdpGroups.get("MODE_FORMAT").getValue();
        String bdFormat = mdpGroups.get("BIT_DEPTH_FORMAT").getValue();
        String fNameFormat = mdpGroups.get("FILE_NAME_FORMAT").getValue();
        String ffFormat = mdpGroups.get("FILE_FORMAT_FORMAT").getValue();

        // Make sure the filename matches the expected pattern before parsing
        Matcher titleMatcher = idPattern.matcher(metadataIdStr);
        if (titleMatcher.matches()) {
            String sat = metadataIdStr.replaceAll(idPattern.pattern(),
                    satFormat);
            String startTime = metadataIdStr.replaceAll(idPattern.pattern(),
                    sTimeFormat);
            String endTime = metadataIdStr.replaceAll(idPattern.pattern(),
                    eTimeFormat);
            String createTime = metadataIdStr.replaceAll(idPattern.pattern(),
                    cTimeFormat);
            String receiveTime = metadataIdStr.replaceAll(idPattern.pattern(),
                    rTimeFormat);
            String res = metadataIdStr.replaceAll(idPattern.pattern(),
                    resFormat);
            String polygon = metadataIdStr.replaceAll(idPattern.pattern(),
                    polyFormat);
            String crs = metadataIdStr.replaceAll(idPattern.pattern(),
                    urnFormat);

            /*
             * The only time that is guaranteed to be provided is the received
             * time. If start time is not available, first try the create time.
             * If not, fall back to received time
             */
            if (startTime == null || startTime.equals("")) {
                if (createTime == null || createTime.equals("")) {
                    startTime = receiveTime;
                } else {
                    startTime = createTime;
                }
            }
            /*
             * If end time is not provided, set it to equal the start time.
             */
            if (endTime == null || endTime.equals("")) {
                endTime = startTime;
            }

            String param = parseParamFromShortName(
                    metadataIdStr.replaceAll(idPattern.pattern(), snFormat));
            String dataSetName = createDataSetName(param, res, sat);
            String ignoreData = checkForIgnore(param, startTime, endTime);

            paramMap.put("collectionName", sat);
            paramMap.put("startTime", startTime);
            paramMap.put("endTime", endTime);
            paramMap.put("paramName", param);
            paramMap.put("dataSetName", dataSetName);
            paramMap.put("polygonPoints", polygon);
            paramMap.put("crs", crs);
            paramMap.put("ignoreData", ignoreData);
        }
        return paramMap;
    }

    private String parseParamFromShortName(String shortName) {
        // default param to just be the short name
        String param = shortName;

        for (PDAShortNameMap snm : shortNameMapping.values()) {
            Pattern snPattern = snm.getPattern();
            Matcher snMatcher = snPattern.matcher(shortName);
            if (snMatcher.matches()) {
                param = shortName.replaceAll(snPattern.pattern(),
                        snm.getParamGroup());
            }

        }
        return param;
    }

    /**
     * Reads in a file name and extracts metadata from the name.
     * 
     * @param file
     *            String object containing the file name
     * @return Map of metadata values
     */
    public Map<String, String> extractMetaDataFromTitle(String fileName)
            throws Exception {

        // starting point for the parsing
        Map<String, String> paramMap = new HashMap<>(6);
        statusHandler.info("Extracting MetaData from: " + fileName);

        /*
         * Pull in regex and grouping from the PDA service config file in case
         * PDA changes the format in the future.
         */
        MetaDataPattern mdp = getMetaDataPattern("RECORD_TITLE");
        Pattern titlePattern = mdp.getPattern();
        Map<String, PatternGroup> mdpGroups = mdp.getGroupMap();
        String paramFormat = mdpGroups.get("PARAM_FORMAT").getValue();
        String resFormat = mdpGroups.get("RES_FORMAT").getValue();
        String satFormat = mdpGroups.get("SAT_FORMAT").getValue();
        String sTimeFormat = mdpGroups.get("START_TIME_FORMAT").getValue();
        String eTimeFormat = mdpGroups.get("END_TIME_FORMAT").getValue();

        // Make sure the filename matches the expected pattern before parsing
        Matcher titleMatcher = titlePattern.matcher(fileName);
        if (titleMatcher.matches()) {
            String res = fileName.replaceAll(titlePattern.pattern(), resFormat);
            String sat = fileName.replaceAll(titlePattern.pattern(), satFormat);
            String param = fileName.replaceAll(titlePattern.pattern(),
                    paramFormat);
            String startTime = fileName.replaceAll(titlePattern.pattern(),
                    sTimeFormat);
            String endTime = fileName.replaceAll(titlePattern.pattern(),
                    eTimeFormat);

            String dataSetName = createDataSetName(param, res, sat);
            String collectionName = createCollectionName(sat);
            String ignoreData = checkForIgnore(param, startTime, endTime);

            paramMap.put("collectionName", collectionName);
            paramMap.put("paramName", param);
            paramMap.put("startTime", startTime);

            paramMap.put("endTime", endTime);
            paramMap.put("dataSetName", dataSetName);
            paramMap.put("ignoreData", ignoreData);
            if (debug) {
                for (String key : paramMap.keySet()) {
                    statusHandler.info(key + ": " + paramMap.get(key));
                }
            }
        } else {
            statusHandler.error("Couldn't create parameter mappings from file!",
                    fileName);
            throw new MetaDataExtractionException("File name '" + fileName
                    + "' does not match the expected pattern: '"
                    + serviceConfig.getConstantValue("RECORD_TITLE_REGEX")
                    + "'");
        }

        return paramMap;
    }

    private String checkForIgnore(String param, String startTime,
            String endTime) {
        boolean excludeParam = checkExcludeList(param);
        boolean oldData = checkRetention(startTime, endTime);

        String ignoreData = "false";
        if (excludeParam || oldData) {
            ignoreData = "true";
        }
        return ignoreData;
    }

    private boolean checkRetention(String startTime, String endTime) {
        boolean oldDate = false;
        Number retention = Double
                .valueOf(dataSetMetadataRetention.get(PDA_PROVIDER));

        if (retention != null) {

            if (retention.intValue() != -1) {
                /*
                 * Retention is calculated in hours. We consider the whole day
                 * to be 24 hours. So, a value of .25 would be considered 6
                 * hours or, -24 * .25 = -6.0. Or with more than one day it
                 * could be, -24 * 7 = -168. We let Number int conversion round
                 * to nearest whole hour.
                 */
                retention = retention.doubleValue() * (-1)
                        * TimeUtil.HOURS_PER_DAY;

                // we are subtracting from current
                Calendar thresholdTime = TimeUtil.newGmtCalendar();
                thresholdTime.add(Calendar.HOUR_OF_DAY, retention.intValue());

                Time time = getTime(startTime, endTime);
                ImmutableDate date;
                try {
                    date = new ImmutableDate(time.parseDate(startTime));

                    if (thresholdTime.getTimeInMillis() >= date.getTime()) {
                        oldDate = true;
                    }
                } catch (ParseException e) {
                    statusHandler.error("Error parsing date: ", e);
                }
            }
        } else {
            statusHandler
                    .warn("Retention time unreadable for this DataSetMetaData provider! "
                            + "Provider: " + PDA_PROVIDER);
        }
        return oldDate;
    }

    private Time getTime(String startTime, String endTime) {

        Time time = new Time();
        time.setFormat(dataSetMetadataDateFormat.get(PDA_PROVIDER));

        try {
            time.setStartDate(startTime);
            time.setEndDate(endTime);
        } catch (ParseException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Couldn't parse start/end time from format: "
                            + dataSetMetadataDateFormat.get(PDA_PROVIDER),
                    e);
        }

        return time;
    }

    private String createCollectionName(String sat) {
        String collectionName = satelliteMapping.get(sat);
        if (collectionName == null) {
            collectionName = sat;
        }
        return collectionName;
    }

    private boolean checkExcludeList(String shortName) {
        boolean ignoreData = false;
        if (excludeList.contains(shortName)) {
            ignoreData = true;
        }
        return ignoreData;
    }

    private String createDataSetName(String parameter, String res, String sat) {
        String dataSetName = null;

        /*
         * Convert the short name to a description. If not found in map, default
         * to shortName.
         */
        String shortNamePart = dataSetNameMapping.get(parameter);
        if (shortNamePart == null) {
            shortNamePart = parameter;
        }

        String resPart = resolutionMapping.get(res);
        if (resPart == null) {
            resPart = res;
        }

        String satPart = satelliteMapping.get(sat);
        if (satPart == null) {
            satPart = sat;
        }

        dataSetName = shortNamePart + " " + resPart + " " + satPart;
        return dataSetName;
    }

    @Override
    public void setDataDate() throws Exception {
        // No implementation in PDA file extractor
    }

    public MetaDataPattern getMetaDataPattern(String name) {
        if (metaDataPatterns.isEmpty()) {
            String fileName = HARVESTER_PATH + "PDAMetaDataConfig.xml";

            try {
                Map<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                        fileName);
                PDAMetaDataConfig tmpConfig = null;
                Map<String, MetaDataPattern> newMetaDataPatterns = new HashMap<>(
                        1);

                if (locFiles.containsKey(LocalizationLevel.BASE)) {
                    tmpConfig = readMetaDataConfig(
                            locFiles.get(LocalizationLevel.BASE));
                    for (MetaDataPattern mdp : tmpConfig
                            .getMetaDataPatterns()) {
                        newMetaDataPatterns.put(mdp.getName(), mdp);
                    }
                }
                if (locFiles.containsKey(LocalizationLevel.SITE)) {
                    tmpConfig = readMetaDataConfig(
                            locFiles.get(LocalizationLevel.SITE));
                    for (MetaDataPattern mdp : tmpConfig
                            .getMetaDataPatterns()) {
                        newMetaDataPatterns.put(mdp.getName(), mdp);
                    }
                }
                metaDataPatterns = newMetaDataPatterns;

            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Failed to Read Parameter Lookup from file: "
                                + fileName,
                        e);
            }
        }

        return metaDataPatterns.get(name);
    }

    /**
     * Read MetaData Patterns
     * 
     * @param file
     * @return
     * @throws Exception
     */
    private PDAMetaDataConfig readMetaDataConfig(ILocalizationFile file)
            throws Exception {
        PDAMetaDataConfig configXml = null;
        JAXBManager jaxb = new JAXBManager(PDAMetaDataConfig.class);

        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                configXml = (PDAMetaDataConfig) jaxb
                        .unmarshalFromInputStream(is);
            }
        }

        return configXml;
    }

    /**
     * Test Practice parsing
     * 
     * @param args
     */
    public static void main(String[] args) {
        PDAFileMetaDataExtractor pfmde = PDAFileMetaDataExtractor.getInstance();
        try {
            Map<String, String> results = pfmde
                    .extractMetaDataFromTitle(args[0]);

            for (Entry<String, String> entry : results.entrySet()) {
                statusHandler.info("Param: " + entry.getKey() + ": Value: "
                        + entry.getValue());
            }

            if (results.containsKey("startTime")) {
                String time = results.get("startTime");

                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "YYYYMMddHHmmss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                Date date = dateFormat.parse(time);
                statusHandler.info("Date: " + date);
            }

        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR, e.getLocalizedMessage(), e);
        }
    }
}
