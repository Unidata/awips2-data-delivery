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
import com.raytheon.uf.common.localization.LocalizationContext;
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
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDAFileMetaDataExtractor extends
        PDAMetaDataExtractor<String, String> {

    private static final String PDA_PROVIDER = "PDA";

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDAFileMetaDataExtractor.class);

    /** remove extension **/
    public static final String timeReplace = "DT_";

    private static PDAFileMetaDataExtractor instance;

    private Map<String, String> dataSetNameMapping = null;

    private Map<String, String> resolutionMapping = null;

    private Map<String, String> satelliteMapping = null;

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
        LocalizationContext commonStaticBase = PathManagerFactory
                .getPathManager().getContext(
                        LocalizationContext.LocalizationType.COMMON_STATIC,
                        LocalizationContext.LocalizationLevel.BASE);

        LocalizationContext commonStaticSite = PathManagerFactory
                .getPathManager().getContext(
                        LocalizationContext.LocalizationType.COMMON_STATIC,
                        LocalizationContext.LocalizationLevel.SITE);

        statusHandler.info("Initializing DataSet Name mapping...");
        initNameMapping(commonStaticBase, commonStaticSite);

        statusHandler.info("Initializing Resolution mapping...");
        initResMapping(commonStaticBase, commonStaticSite);

        statusHandler.info("Initializing Satellite mapping...");
        initSatMapping(commonStaticBase, commonStaticSite);

        statusHandler.info("Initializing Exclusion list...");
        initExclusionList(commonStaticBase, commonStaticSite);

        statusHandler.info("Initializing Harvester Configs...");
        initHarvesterConfigs(commonStaticBase, commonStaticSite);
    }

    private void initNameMapping(LocalizationContext commonStaticBase,
            LocalizationContext commonStaticSite) {
        LocalizationFile[] mappingFiles = PathManagerFactory.getPathManager()
                .listFiles(
                        new LocalizationContext[] { commonStaticBase,
                                commonStaticSite }, "datadelivery/mappings",
                        new String[] { "DataNameMappings.xml" }, true, true);

        PDADataSetNameMapSet nameMapSet = new PDADataSetNameMapSet();

        for (LocalizationFile mappingFile : mappingFiles) {
            try (InputStream inputStream = mappingFile.openInputStream()) {
                JAXBManager jaxb = new JAXBManager(PDADataSetNameMapSet.class);
                PDADataSetNameMapSet fileSet = (PDADataSetNameMapSet) jaxb
                        .unmarshalFromInputStream(inputStream);
                nameMapSet.addMaps(fileSet.getMaps());
            } catch (LocalizationException | IOException
                    | SerializationException | JAXBException e) {
                statusHandler.error(
                        "Unable to unmarshal DataSet name mapping file:"
                                + mappingFile, e);
            }
        }

        addNameMappings(nameMapSet);
    }

    private void initResMapping(LocalizationContext commonStaticBase,
            LocalizationContext commonStaticSite) {
        LocalizationFile[] mappingFiles = PathManagerFactory.getPathManager()
                .listFiles(
                        new LocalizationContext[] { commonStaticBase,
                                commonStaticSite }, "datadelivery/mappings",
                        new String[] { "ResolutionMappings.xml" }, true, true);

        PDADescriptionMapSet resMapSet = new PDADescriptionMapSet();

        for (LocalizationFile mappingFile : mappingFiles) {
            try (InputStream inputStream = mappingFile.openInputStream()) {
                JAXBManager jaxb = new JAXBManager(PDADescriptionMapSet.class);
                PDADescriptionMapSet fileSet = (PDADescriptionMapSet) jaxb
                        .unmarshalFromInputStream(inputStream);
                resMapSet.addMaps(fileSet.getMaps());
            } catch (LocalizationException | IOException
                    | SerializationException | JAXBException e) {
                statusHandler.error(
                        "Unable to unmarshal DataSet name mapping file:"
                                + mappingFile, e);
            }
        }

        addResMappings(resMapSet);
    }

    private void initSatMapping(LocalizationContext commonStaticBase,
            LocalizationContext commonStaticSite) {
        LocalizationFile[] mappingFiles = PathManagerFactory.getPathManager()
                .listFiles(
                        new LocalizationContext[] { commonStaticBase,
                                commonStaticSite }, "datadelivery/mappings",
                        new String[] { "SatelliteMappings.xml" }, true, true);

        PDADescriptionMapSet satMapSet = new PDADescriptionMapSet();

        for (LocalizationFile mappingFile : mappingFiles) {
            try (InputStream inputStream = mappingFile.openInputStream()) {
                JAXBManager jaxb = new JAXBManager(PDADescriptionMapSet.class);
                PDADescriptionMapSet fileSet = (PDADescriptionMapSet) jaxb
                        .unmarshalFromInputStream(inputStream);
                satMapSet.addMaps(fileSet.getMaps());
            } catch (LocalizationException | IOException
                    | SerializationException | JAXBException e) {
                statusHandler.error(
                        "Unable to unmarshal DataSet name mapping file:"
                                + mappingFile, e);
            }
        }

        addSatMappings(satMapSet);
    }

    private void initExclusionList(LocalizationContext commonStaticBase,
            LocalizationContext commonStaticSite) {
        LocalizationFile[] mappingFiles = PathManagerFactory.getPathManager()
                .listFiles(
                        new LocalizationContext[] { commonStaticBase,
                                commonStaticSite }, "datadelivery/mappings",
                        new String[] { "ParameterExclusions.xml" }, true, true);

        for (LocalizationFile mappingFile : mappingFiles) {
            try (InputStream inputStream = mappingFile.openInputStream()) {
                JAXBManager jaxb = new JAXBManager(PDAParameterExclusions.class);
                PDAParameterExclusions fileSet = (PDAParameterExclusions) jaxb
                        .unmarshalFromInputStream(inputStream);
                excludeList.addAll(fileSet.getParameterExclusions());
            } catch (LocalizationException | IOException
                    | SerializationException | JAXBException e) {
                statusHandler.error(
                        "Unable to unmarshal DataSet name mapping file:"
                                + mappingFile, e);
            }
        }
    }

    private void initHarvesterConfigs(LocalizationContext commonStaticBase,
            LocalizationContext commonStaticSite) {
        LocalizationFile[] mappingFiles = PathManagerFactory.getPathManager()
                .listFiles(
                        new LocalizationContext[] { commonStaticBase,
                                commonStaticSite }, "datadelivery/harvester",
                        new String[] { "PDA-Harvester.xml" }, true, true);

        for (LocalizationFile mappingFile : mappingFiles) {
            try (InputStream inputStream = mappingFile.openInputStream()) {
                JAXBManager jaxb = new JAXBManager(HarvesterConfig.class);
                HarvesterConfig fileSet = (HarvesterConfig) jaxb
                        .unmarshalFromInputStream(inputStream);
                dataSetMetadataRetention.put(fileSet.getProvider().getName(),
                        fileSet.getRetention());
                dataSetMetadataDateFormat.put(fileSet.getProvider().getName(),
                        fileSet.getAgent().getDateFormat());
            } catch (LocalizationException | IOException
                    | SerializationException | JAXBException e) {
                statusHandler.error(
                        "Unable to unmarshal DataSet name mapping file:"
                                + mappingFile, e);
            }
        }
    }

    private void addNameMappings(PDADataSetNameMapSet nameMapSet) {
        for (PDADataSetNameMap map : nameMapSet.getMaps()) {
            dataSetNameMapping.put(map.getParameter(), map.getDescription());
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

    /**
     * Reads in a file name and extracts metadata from the name.
     * 
     * @param file
     *            String object containing the file name
     * @return Map of metadata values
     */
    public Map<String, String> extractMetaData(Object file) throws Exception {

        // starting point for the parsing
        Map<String, String> paramMap = new HashMap<>(6);
        String fileName = (String) file;
        statusHandler.info("Extracting MetaData from: " + fileName);

        /*
         * Pull in regex and grouping from the PDA service config file in case
         * PDA changes the format in the future.
         */
        Pattern titlePattern = Pattern.compile(serviceConfig
                .getConstantValue("RECORD_TITLE_REGEX"));
        String paramFormat = serviceConfig.getConstantValue("PARAM_FORMAT");
        String resFormat = serviceConfig.getConstantValue("RES_FORMAT");
        String satFormat = serviceConfig.getConstantValue("SAT_FORMAT");
        String sTimeFormat = serviceConfig
                .getConstantValue("START_TIME_FORMAT");
        String eTimeFormat = serviceConfig.getConstantValue("END_TIME_FORMAT");

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
            boolean excludeParam = checkExcludeList(param);
            boolean oldData = checkRetention(startTime, endTime);

            String ignoreData = "false";
            if (excludeParam || oldData) {
                ignoreData = "true";
            }

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
            statusHandler.error(
                    "Couldn't create parameter mappings from file!", fileName);
            throw new MetaDataExtractionException("File name '" + fileName
                    + "' does not match the expected pattern: '"
                    + serviceConfig.getConstantValue("RECORD_TITLE_REGEX")
                    + "'");
        }

        return paramMap;
    }

    private boolean checkRetention(String startTime, String endTime) {
        boolean oldDate = false;
        Number retention = Double.valueOf(dataSetMetadataRetention
                .get(PDA_PROVIDER));

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
                            + dataSetMetadataDateFormat.get(PDA_PROVIDER), e);
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

    /**
     * Test Practice parsing
     * 
     * @param args
     */
    public static void main(String[] args) {
        PDAFileMetaDataExtractor pfmde = PDAFileMetaDataExtractor.getInstance();
        try {
            Map<String, String> results = pfmde.extractMetaData(args[0]);

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
