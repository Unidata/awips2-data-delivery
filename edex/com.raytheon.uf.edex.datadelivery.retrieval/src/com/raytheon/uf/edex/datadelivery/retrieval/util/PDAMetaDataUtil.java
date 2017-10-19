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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.retrieval.xml.MetaDataPattern;
import com.raytheon.uf.common.datadelivery.retrieval.xml.PDAMetaDataConfig;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.SaveableOutputStream;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.PDAFileMetaDataExtractor;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.PDAPipeDelimitedMetaDataExtractor;

/**
 * Utility class for accessing localization files for pda metadata parsing.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Mar 31, 2017  6186     rjpeter   Initial creation
 * Oct 23, 2017  6185     bsteffen  Use area and resolution to get sat provider.
 *
 * </pre>
 *
 * @author rjpeter
 */
public class PDAMetaDataUtil {
    private static final String MAPPING_PATH = "datadelivery"
            + File.separatorChar + "mappings" + File.separatorChar;

    private static final String SHORT_NAME_PATH = MAPPING_PATH
            + "ShortNameMappings.xml";

    private static final String PARAMETER_EXCLUSION_PATH = MAPPING_PATH
            + "ParameterExclusions.xml";

    private static final String SATELLITE_PATH = MAPPING_PATH
            + "SatelliteMappings.xml";

    // TODO: Rename actual file to parameter mapping
    private static final String PARAMETER_NAME_PATH = MAPPING_PATH
            + "DataNameMappings.xml";

    private static final String RESOLUTION_PATH = MAPPING_PATH
            + "ResolutionMappings.xml";

    private static final String METADATA_CONFIG_PATH = MAPPING_PATH
            + "PDAMetaDataConfig.xml";

    private static final String HARVESTER_PATH = "datadelivery"
            + File.separatorChar + "harvester" + File.separatorChar;

    private static final String HARVESTER_CONFIG_PATH = HARVESTER_PATH
            + "PDA-Harvester.xml";

    private static final PDAMetaDataUtil instance = new PDAMetaDataUtil();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConcurrentMap<String, Long> updateTimeMap = new ConcurrentHashMap<>(
            8);

    private final Map<String, Object> lockMap = new HashMap<>(8);

    private PDADescriptionMapSet satelliteMapping = null;

    private Map<String, String> parameterMapping = new HashMap<>();

    private Map<String, String> resolutionMapping = new HashMap<>();

    private Map<String, PDAShortNameMap> shortNameMapping = new HashMap<>();

    private Set<String> excludes = new HashSet<>();

    private static Map<String, MetaDataPattern> metaDataPatterns = new HashMap<>();

    private static Map<String, String> providerRetention = new HashMap<>();

    public static PDAMetaDataUtil getInstance() {
        return instance;
    }

    public PDAMetaDataUtil() {
        String[] keys = { PARAMETER_NAME_PATH, PARAMETER_EXCLUSION_PATH,
                RESOLUTION_PATH, SATELLITE_PATH, SHORT_NAME_PATH,
                METADATA_CONFIG_PATH, HARVESTER_CONFIG_PATH };
        for (String key : keys) {
            lockMap.put(key, new Object());
        }
    }

    public String getParameterName(String providerParam) {
        String rval = getParameterMapping().get(providerParam);
        return rval != null ? rval : providerParam;
    }

    public String getResName(String providerRes) {
        String rval = getResolutionMapping().get(providerRes);
        return rval != null ? rval : providerRes;
    }

    public String getSatName(String providerSat, String providerRes,
            ReferencedEnvelope envelope) {
        PDADescriptionMapSet mapping = getSatMapping();
        String rval = mapping.doDynamicMapping(providerSat, providerRes,
                envelope);
        if (rval == null) {
            rval = mapping.doMapping(providerSat);
            if (rval == null) {
                rval = providerSat;
            }
        } else if (mapping.setMapping(providerSat, rval)) {
            /*
             * When a dynamic mapping matches, save the mapping of key to
             * description at the configured level. This supports the ability of
             * GOES satellites to shift between east, west, and center. When a
             * new full disk arrives at the new location then the configured
             * mapping is updated and all resolutions will register the new
             * position.
             */
            synchronized (lockMap.get(SATELLITE_PATH)) {
                try {
                    IPathManager pm = PathManagerFactory.getPathManager();
                    JAXBManager jaxb = new JAXBManager(
                            PDADescriptionMapSet.class);
                    LocalizationContext context = pm.getContext(
                            LocalizationType.COMMON_STATIC,
                            LocalizationLevel.CONFIGURED);
                    ILocalizationFile lf = pm.getLocalizationFile(context,
                            SATELLITE_PATH);
                    PDADescriptionMapSet mapSet;
                    if (lf.exists()) {
                        mapSet = loadFile(jaxb, lf, PDADescriptionMapSet.class);
                    } else {
                        mapSet = new PDADescriptionMapSet();
                    }
                    if (mapSet.setMapping(providerSat, rval)) {
                        try (SaveableOutputStream os = lf.openOutputStream()) {
                            jaxb.marshalToStream(mapSet, os);
                            os.save();
                        }
                    }
                } catch (JAXBException | SerializationException | IOException
                        | LocalizationException e) {
                    logger.error("Unable to update satellite mapping", e);
                }
            }

        }
        return rval;
    }

    public boolean isExcluded(String paramShortName) {
        return getExcludes().contains(paramShortName);
    }

    public String getParamFromShortName(String paramShortName) {
        // default param to just be the short name
        String param = paramShortName;

        for (PDAShortNameMap snm : getShortNameMapping().values()) {
            Pattern snPattern = snm.getPattern();
            Matcher snMatcher = snPattern.matcher(paramShortName);
            if (snMatcher.matches()) {
                param = paramShortName.replaceAll(snPattern.pattern(),
                        snm.getParamGroup());
            }

        }
        return param;
    }

    public String getResFromShortName(String paramShortName) {
        // default res to be full disk
        String res = "F";

        for (PDAShortNameMap snm : getShortNameMapping().values()) {
            Pattern snPattern = snm.getPattern();
            Matcher snMatcher = snPattern.matcher(paramShortName);
            if (snMatcher.matches()) {
                res = paramShortName.replaceAll(snPattern.pattern(),
                        snm.getResGroup());
            }

        }
        return res;
    }

    public long getRetentionThreshold(String provider) {
        String retentionVal = getProviderRetention().get(provider);
        long rval = 0;

        if (retentionVal != null) {
            Number retention = Double.valueOf(retentionVal);
            if (retention.doubleValue() > 0) {
                int hours = (int) (retention.doubleValue() * (-1)
                        * TimeUtil.HOURS_PER_DAY);

                // we are subtracting from current
                Calendar thresholdTime = TimeUtil.newGmtCalendar();
                thresholdTime.add(Calendar.HOUR_OF_DAY, hours);
                rval = thresholdTime.getTimeInMillis();
            }
        }

        return rval;
    }

    public MetaDataPattern getMetaDataPattern(String name) {
        return getMetaDataConfig().get(name);
    }

    private Map<String, String> getParameterMapping() {
        String fileName = PARAMETER_NAME_PATH;
        SortedMap<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);

        if (parameterMapping.isEmpty() || shouldUpdate(locFiles.values())) {
            logger.info("Initializing Parameter mappings...");
            PDADataSetNameMapSet mapSet = new PDADataSetNameMapSet();
            synchronized (lockMap.get(PARAMETER_NAME_PATH)) {
                try {
                    JAXBManager jaxb = new JAXBManager(
                            PDADataSetNameMapSet.class);
                    for (LocalizationFile file : locFiles.values()) {
                        PDADataSetNameMapSet fileSet = loadFile(jaxb, file,
                                PDADataSetNameMapSet.class);
                        if (fileSet != null) {
                            mapSet.addMaps(fileSet.getMaps());
                        }
                    }
                    Map<String, String> newMap = new HashMap<>(1);
                    for (PDADataSetNameMap map : mapSet.getMaps()) {
                        newMap.put(map.getParameter(), map.getDescription());
                    }
                    parameterMapping = newMap;
                    updateTimes(locFiles.values());
                } catch (JAXBException | SerializationException | IOException
                        | LocalizationException e) {
                    logger.error("Unable to initialize parameter mapping", e);
                }
            }
        }

        return parameterMapping;
    }

    private Map<String, String> getResolutionMapping() {
        String fileName = RESOLUTION_PATH;
        SortedMap<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);

        if (resolutionMapping.isEmpty() || shouldUpdate(locFiles.values())) {
            logger.info("Initializing Resolution mappings...");
            PDADescriptionMapSet mapSet = new PDADescriptionMapSet();
            synchronized (lockMap.get(RESOLUTION_PATH)) {
                try {
                    JAXBManager jaxb = new JAXBManager(
                            PDADescriptionMapSet.class);
                    for (LocalizationFile file : locFiles.values()) {
                        PDADescriptionMapSet fileSet = loadFile(jaxb, file,
                                PDADescriptionMapSet.class);
                        if (fileSet != null) {
                            mapSet.extend(fileSet);
                        }
                    }
                    Map<String, String> newMap = new HashMap<>(1);
                    for (PDADescriptionMap map : mapSet.getMaps()) {
                        newMap.put(map.getKey(), map.getDescription());
                    }
                    resolutionMapping = newMap;
                    updateTimes(locFiles.values());
                } catch (JAXBException | SerializationException | IOException
                        | LocalizationException e) {
                    logger.error("Unable to initialize resolution mapping", e);
                }
            }
        }

        return resolutionMapping;
    }

    private PDADescriptionMapSet getSatMapping() {
        String fileName = SATELLITE_PATH;
        SortedMap<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);

        if (satelliteMapping == null || shouldUpdate(locFiles.values())) {
            logger.info("Initializing Satellite mappings...");
            PDADescriptionMapSet mapSet = new PDADescriptionMapSet();
            synchronized (lockMap.get(SATELLITE_PATH)) {
                try {
                    JAXBManager jaxb = new JAXBManager(
                            PDADescriptionMapSet.class);
                    for (LocalizationFile file : locFiles.values()) {
                        PDADescriptionMapSet fileSet = loadFile(jaxb, file,
                                PDADescriptionMapSet.class);
                        if (fileSet != null) {
                            mapSet.extend(fileSet);
                        }
                    }
                    satelliteMapping = mapSet;
                    updateTimes(locFiles.values());
                } catch (JAXBException | SerializationException | IOException
                        | LocalizationException e) {
                    logger.error("Unable to initialize satellite mapping", e);
                }
            }
        }

        return satelliteMapping;
    }

    private Set<String> getExcludes() {
        String fileName = PARAMETER_EXCLUSION_PATH;
        SortedMap<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);

        if (excludes.isEmpty() || shouldUpdate(locFiles.values())) {
            logger.info("Initializing Exclusion list...");
            Set<String> newExcludes = new HashSet<>();
            synchronized (lockMap.get(PARAMETER_EXCLUSION_PATH)) {
                try {
                    JAXBManager jaxb = new JAXBManager(
                            PDAParameterExclusions.class);
                    for (LocalizationFile file : locFiles.values()) {
                        PDAParameterExclusions fileSet = loadFile(jaxb, file,
                                PDAParameterExclusions.class);
                        if (fileSet != null) {
                            newExcludes
                                    .addAll(fileSet.getParameterExclusions());
                        }
                    }
                    excludes = newExcludes;
                    updateTimes(locFiles.values());
                } catch (JAXBException | SerializationException | IOException
                        | LocalizationException e) {
                    logger.error("Unable to initialize parameter exclusions",
                            e);
                }
            }
        }

        return excludes;
    }

    private Map<String, PDAShortNameMap> getShortNameMapping() {
        String fileName = SHORT_NAME_PATH;
        SortedMap<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);

        if (shortNameMapping.isEmpty() || shouldUpdate(locFiles.values())) {
            logger.info("Initializing Short Name mappings...");
            PDAShortNameMapSet mapSet = new PDAShortNameMapSet();
            synchronized (lockMap.get(SHORT_NAME_PATH)) {
                try {
                    JAXBManager jaxb = new JAXBManager(
                            PDAShortNameMapSet.class);
                    for (LocalizationFile file : locFiles.values()) {
                        PDAShortNameMapSet fileSet = loadFile(jaxb, file,
                                PDAShortNameMapSet.class);
                        if (fileSet != null) {
                            mapSet.addMaps(fileSet.getMaps());
                        }
                    }
                    Map<String, PDAShortNameMap> newMap = new HashMap<>(1);
                    for (PDAShortNameMap map : mapSet.getMaps()) {
                        newMap.put(map.getId(), map);
                    }
                    shortNameMapping = newMap;
                    updateTimes(locFiles.values());
                } catch (JAXBException | SerializationException | IOException
                        | LocalizationException e) {
                    logger.error("Unable to initialize satellite mapping", e);
                }
            }
        }

        return shortNameMapping;
    }

    private Map<String, String> getProviderRetention() {
        String fileName = HARVESTER_CONFIG_PATH;
        SortedMap<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);

        if (providerRetention.isEmpty() || shouldUpdate(locFiles.values())) {
            logger.info("Initializing Harevester Config...");
            Map<String, String> newMap = new HashMap<>(1);
            synchronized (lockMap.get(HARVESTER_CONFIG_PATH)) {
                try {
                    JAXBManager jaxb = new JAXBManager(HarvesterConfig.class);
                    for (LocalizationFile file : locFiles.values()) {
                        HarvesterConfig fileSet = loadFile(jaxb, file,
                                HarvesterConfig.class);
                        if (fileSet != null) {
                            newMap.put(fileSet.getProvider().getName(),
                                    fileSet.getRetention());
                        }
                    }
                    providerRetention = newMap;
                    updateTimes(locFiles.values());
                } catch (JAXBException | SerializationException | IOException
                        | LocalizationException e) {
                    logger.error("Unable to initialize harvester config", e);
                }
            }
        }

        return providerRetention;
    }

    private Map<String, MetaDataPattern> getMetaDataConfig() {
        String fileName = METADATA_CONFIG_PATH;
        SortedMap<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);

        if (metaDataPatterns.isEmpty() || shouldUpdate(locFiles.values())) {
            logger.info("Initializing MetaData Config...");
            Map<String, MetaDataPattern> newMap = new HashMap<>(1);
            synchronized (lockMap.get(METADATA_CONFIG_PATH)) {
                try {
                    JAXBManager jaxb = new JAXBManager(PDAMetaDataConfig.class);
                    for (LocalizationFile file : locFiles.values()) {
                        PDAMetaDataConfig fileSet = loadFile(jaxb, file,
                                PDAMetaDataConfig.class);
                        if (fileSet != null) {
                            for (MetaDataPattern mdp : fileSet
                                    .getMetaDataPatterns()) {
                                newMap.put(mdp.getName(), mdp);
                            }
                        }
                    }

                    if (validateMetaDataPatterns(newMap)) {
                        metaDataPatterns = newMap;
                        updateTimes(locFiles.values());
                    }

                } catch (JAXBException | SerializationException | IOException
                        | LocalizationException e) {
                    logger.error("Unable to initialize MetaData Config", e);
                }
            }
        }

        return metaDataPatterns;
    }

    private boolean validateMetaDataPatterns(
            Map<String, MetaDataPattern> mdpMap) {
        StringBuilder errors = new StringBuilder();
        if (mdpMap != null && !mdpMap.isEmpty()) {
            PDAFileMetaDataExtractor.validateMetaDataPatterns(mdpMap, errors);
            PDAPipeDelimitedMetaDataExtractor.validateMetaDataPatterns(mdpMap,
                    errors);
        } else {
            errors.append(
                    "No meta data patterns were found in the configuration files. ");
        }

        boolean valid = errors.length() == 0;
        if (!valid) {
            logger.error("Meta Data Patterns information found to be invalid: "
                    + errors);
            logger.error("PDA meta data will not be able to be parsed!");
        }
        return valid;
    }

    private SortedMap<LocalizationLevel, LocalizationFile> getLocalizationFiles(
            String fileName) {
        IPathManager pm = PathManagerFactory.getPathManager();
        SortedMap<LocalizationLevel, LocalizationFile> files = new TreeMap<>(
                pm.getTieredLocalizationFile(LocalizationType.COMMON_STATIC,
                        fileName));
        return files;
    }

    private boolean shouldUpdate(Collection<LocalizationFile> locFiles) {
        boolean update = false;

        for (LocalizationFile lf : locFiles) {
            String key = lf.toString();
            String msg = null;
            Long lastTime = updateTimeMap.get(key);

            if (lastTime == null) {
                msg = key + " has not been loaded";
            } else {
                long fileTime = lf.getTimeStamp().getTime();

                if (fileTime != lastTime.longValue()) {
                    msg = key + " has been modified.";
                }
            }

            if (msg != null) {
                logger.info(msg);
                update = true;
                break;
            }
        }

        return update;
    }

    private <K> K loadFile(JAXBManager jaxb, ILocalizationFile file,
            Class<K> clazz)
            throws IOException, LocalizationException, SerializationException {
        K rval = null;
        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                rval = jaxb.unmarshalFromInputStream(clazz, is);
            }
        }
        return rval;
    }

    private void updateTimes(Collection<LocalizationFile> locFiles) {
        for (LocalizationFile lf : locFiles) {
            updateTimeMap.put(lf.toString(), lf.getTimeStamp().getTime());
        }
    }
}
