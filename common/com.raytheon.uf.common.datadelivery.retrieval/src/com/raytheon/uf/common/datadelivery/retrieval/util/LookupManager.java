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
package com.raytheon.uf.common.datadelivery.retrieval.util;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetConfigInfo;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetConfigInfoMap;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetInformation;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetInformationLookup;
import com.raytheon.uf.common.datadelivery.retrieval.xml.LevelLookup;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterLevelRegex;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterLookup;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterMapping;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterNameRegex;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterRegexes;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.SaveableOutputStream;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.common.util.ServiceLoaderUtil;

/**
 * Lookup table manager
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Mar 07, 2011  357      dhladky   Initial creation
 * Oct 27, 2012  1163     dhladky   Improved, dynamically create files, Added
 *                                  Units
 * Jan 18, 2013  1513     dhladky   Level lookup refit.
 * Mar 21, 2013  1794     djohnson  ServiceLoaderUtil now requires the
 *                                  requesting class.
 * Nov 07, 2013  2361     njensen   Use JAXBManager for XML
 * Jan 14, 2014           dhladky   AvailabilityOffset calculations
 * Jan 20, 2016  5244     njensen   Replaced calls to deprecated
 *                                  LocalizationFile methods
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Jan 05, 2017  5988     tjensen   Updated for new parameter lookups and
 *                                  regexes
 * Apr 05, 2017  1045     tjensen   Updated for DataSetConfigInfo
 * May 10, 2017  6135     nabowle   Remove UnitLookup in favor of UnitMapper.
 *
 * </pre>
 *
 * @author dhladky
 */
public class LookupManager {

    /**
     * Implementation of the xml writers that writes to localization files.
     */
    private class LocalizationXmlWriter {

        /**
         * Writes the level lookup to XML
         *
         * @param ll
         * @param modelName
         * @throws Exception
         */
        public void writeLevelXml(LevelLookup ll, String modelName)
                throws Exception {
            IPathManager pm = PathManagerFactory.getPathManager();
            LocalizationContext lc = pm.getContext(
                    LocalizationType.COMMON_STATIC, LocalizationLevel.SITE);
            String fileName = getLevelFileName(modelName);
            ILocalizationFile lf = pm.getLocalizationFile(lc, fileName);
            try (SaveableOutputStream sos = lf.openOutputStream()) {
                getJaxbManager().marshalToStream(ll, sos);
                sos.save();
            }
        }

        /**
         * Writes the DataSetInformation lookup to XML
         *
         * @param aol
         * @param modelName
         * @throws Exception
         */
        public void writeDataSetInformationXml(DataSetInformationLookup dsi)
                throws Exception {
            IPathManager pm = PathManagerFactory.getPathManager();
            LocalizationContext lc = pm.getContext(
                    LocalizationType.COMMON_STATIC, LocalizationLevel.SITE);
            String fileName = getDataSetInformationFileName();
            ILocalizationFile lf = pm.getLocalizationFile(lc, fileName);
            try (SaveableOutputStream sos = lf.openOutputStream()) {
                getJaxbManager().marshalToStream(dsi, sos);
                sos.save();
            }
        }
    }

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(LookupManager.class);

    /** Singleton instance of this class */
    private static final LookupManager instance = new LookupManager();

    /** Path to Lookup tables. */
    private static final String CONFIG_FILE_ROOT = "datadelivery"
            + File.separatorChar + "lookups" + File.separatorChar;

    private static final String CONFIG_FILE_PARAM = "ParameterLookup.xml";

    private static final String CONFIG_FILE_REGEX = "ParameterRegexes.xml";

    private static final String CONFIG_FILE_DATASETCONFIG = "DataSetConfigInfo.xml";

    private static final String CONFIG_FILE_DATASETINFO = "DataSetInformationLookup.xml";

    private static final String CONFIG_FILE_LEVEL = "LevelLookup.xml";

    public static final String UNIT_MAPPER_NAMESPACE = "datadelivery";

    /**
     * Get an instance of this singleton.
     *
     * @return Instance of this class
     */
    public static LookupManager getInstance() {
        return instance;
    }

    /** Parameter overrides that apply to all datasets */
    private Map<String, ParameterMapping> generalParameters = new HashMap<>(2);

    /** Parameter overrides that apply to specific datasets */
    private Map<String, Map<String, ParameterMapping>> dataSetParameters = new HashMap<>(
            2);

    private Map<String, ParameterNameRegex> paramNameRegexes = new LinkedHashMap<>(
            2);

    private Map<String, ParameterLevelRegex> paramLevelRegexes = new LinkedHashMap<>(
            2);

    private final Map<String, LevelLookup> levels = new HashMap<>(2);

    private Map<String, DataSetInformation> dataSetInformations = new HashMap<>(
            2);

    private final LocalizationXmlWriter localizationXmlWriter = ServiceLoaderUtil
            .load(LookupManager.class, LocalizationXmlWriter.class,
                    new LocalizationXmlWriter());

    private Date regexFileTime;

    private Date paramFileTime;

    private final Map<String, Map<String, DataSetConfigInfo>> dataSetConfigInfos = new HashMap<>(
            2);

    private static JAXBManager jaxb;

    private static synchronized JAXBManager getJaxbManager()
            throws JAXBException {
        if (jaxb == null) {
            jaxb = new JAXBManager(LevelLookup.class, ParameterLookup.class,
                    ParameterRegexes.class, DataSetInformationLookup.class,
                    DataSetInformation.class, DataSetConfigInfoMap.class);
        }

        return jaxb;
    }

    /* Private Constructor */
    private LookupManager() {

    }

    /**
     * Level file name
     *
     * @param modelName
     * @return
     */
    private static String getLevelFileName(String modelName) {
        return CONFIG_FILE_ROOT + modelName + CONFIG_FILE_LEVEL;
    }

    /**
     * Gets the Model Levels
     *
     * @param modelName
     * @return
     */
    public LevelLookup getLevels(String modelName) {
        LevelLookup modelLevels = null;

        if (levels.containsKey(modelName)) {
            modelLevels = levels.get(modelName);
        } else {

            modelLevels = getLevelsFromFile(modelName);
            levels.put(modelName, modelLevels);
        }

        return modelLevels;
    }

    private LevelLookup getLevelsFromFile(String modelName) {
        LevelLookup levelLoookup = null;
        ILocalizationFile file = null;
        String fileName = getLevelFileName(modelName);

        try {
            file = getLocalizationFile(fileName);
        } catch (Exception e) {
            statusHandler.error(" Failed to Level Lookup table: " + fileName,
                    e);
        }

        if (file != null) {
            try {
                levelLoookup = readLevelXml(file);
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Failed to Read Level Lookup from file: "
                                + file.getPath(),
                        e);
            }
        }

        return levelLoookup;
    }

    /**
     * Gets the localized file
     *
     * @param fileName
     * @return
     */
    private ILocalizationFile getLocalizationFile(String fileName) {
        ILocalizationFile lf = null;
        Map<LocalizationLevel, LocalizationFile> files = getLocalizationFiles(
                fileName);

        if (files.containsKey(LocalizationLevel.SITE)) {
            lf = files.get(LocalizationLevel.SITE);
        } else {
            lf = files.get(LocalizationLevel.BASE);
        }

        return lf;
    }

    private Map<LocalizationLevel, LocalizationFile> getLocalizationFiles(
            String fileName) {
        IPathManager pm = PathManagerFactory.getPathManager();
        Map<LocalizationLevel, LocalizationFile> files = pm
                .getTieredLocalizationFile(LocalizationType.COMMON_STATIC,
                        fileName);
        return files;
    }

    /**
     * Gets the Model parameters
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public void loadParameterRegexes() {
        ILocalizationFile file = null;
        String fileName = getRegexFileName();

        try {
            file = getLocalizationFile(fileName);
        } catch (Exception e) {
            statusHandler.error(
                    " Failed to find Parameter Lookup table: " + fileName, e);
        }

        if (file != null) {
            try {
                /*
                 * If regexes is null or the timestamp on the file has changed
                 * since we last read it, read the parameters from the file.
                 */
                if (paramNameRegexes.isEmpty()
                        || !file.getTimeStamp().equals(regexFileTime)) {
                    ParameterRegexes tmpRegex = null;
                    Map<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                            fileName);

                    // TODO: Come back to and model after PDAMetaDataUtil that
                    // uses a SortedMap for auto iteration order
                    Map<String, ParameterNameRegex> newParamNameRegexes = new LinkedHashMap<>(
                            2);
                    Map<String, ParameterLevelRegex> newParamLevelRegexes = new LinkedHashMap<>(
                            2);

                    for (LocalizationLevel locLevel : new LocalizationLevel[] {
                            LocalizationLevel.BASE, LocalizationLevel.SITE }) {
                        if (locFiles.containsKey(locLevel)) {
                            tmpRegex = readParameterRegexes(
                                    locFiles.get(locLevel));
                            populateParameterRegexes(tmpRegex,
                                    newParamNameRegexes, newParamLevelRegexes);
                        }
                    }
                    paramNameRegexes = sortByValue(newParamNameRegexes);
                    paramLevelRegexes = sortByValue(newParamLevelRegexes);
                    regexFileTime = file.getTimeStamp();
                }
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Failed to Read Parameter Lookup from file: "
                                + file.getPath(),
                        e);
            }
        }
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(
            Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private void populateParameterRegexes(ParameterRegexes tmpRegex,
            Map<String, ParameterNameRegex> newParamNameRegexes,
            Map<String, ParameterLevelRegex> newParamLevelRegexes) {
        for (ParameterNameRegex nameRegex : tmpRegex.getNameRegexes()) {
            newParamNameRegexes.put(nameRegex.getId(), nameRegex);
        }
        for (ParameterLevelRegex levelRegex : tmpRegex.getLevelRegexes()) {
            newParamLevelRegexes.put(levelRegex.getId(), levelRegex);
        }
    }

    public Map<String, ParameterNameRegex> getParamNameRegexes() {
        loadParameterRegexes();
        return paramNameRegexes;
    }

    public Map<String, ParameterLevelRegex> getParamLevelRegexes() {
        loadParameterRegexes();
        return paramLevelRegexes;
    }

    private void loadParameters() {
        ILocalizationFile file = null;
        String fileName = getParamFileName();

        try {
            file = getLocalizationFile(fileName);
        } catch (Exception e) {
            statusHandler
                    .error(" Failed to Parameter Lookup table: " + fileName, e);
        }

        if (file != null) {
            try {
                /*
                 * If regexes is null or the timestamp on the file has changed
                 * since we last read it, read the parameters from the file.
                 */
                if ((generalParameters.isEmpty() && dataSetParameters.isEmpty())
                        || !file.getTimeStamp().equals(paramFileTime)) {
                    readParametersFromFile(file, fileName);
                }

            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Failed to Read Parameter Lookup from file: "
                                + file.getPath(),
                        e);
            }
        }
    }

    public ParameterMapping getDataSetParameter(String dataSet,
            String gradsName) {
        loadParameters();

        ParameterMapping retval = null;
        Map<String, ParameterMapping> dsMap = dataSetParameters.get(dataSet);
        if (dsMap != null) {
            retval = dsMap.get(gradsName);
        }

        return retval;
    }

    public ParameterMapping getGeneralParameter(String gradsName) {
        loadParameters();

        return generalParameters.get(gradsName);
    }

    private void readParametersFromFile(ILocalizationFile file, String fileName)
            throws Exception {
        ParameterLookup tmpLookup = null;
        Map<String, ParameterMapping> newGeneralParameters = new HashMap<>(2);

        Map<String, Map<String, ParameterMapping>> newDataSetParameters = new HashMap<>(
                2);
        Map<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                fileName);
        for (LocalizationLevel locLevel : new LocalizationLevel[] {
                LocalizationLevel.BASE, LocalizationLevel.SITE }) {
            if (locFiles.containsKey(locLevel)) {
                tmpLookup = readParameterXml(locFiles.get(locLevel));
                for (ParameterMapping mapping : tmpLookup
                        .getParameterMappings()) {
                    List<String> dataSets = mapping.getDataSets();
                    if (dataSets != null && !dataSets.isEmpty()) {
                        for (String ds : dataSets) {
                            Map<String, ParameterMapping> dsMap = newDataSetParameters
                                    .get(ds);
                            if (dsMap == null) {
                                dsMap = new HashMap<>(2);
                                newDataSetParameters.put(ds, dsMap);
                            }
                            dsMap.put(mapping.getGrads(), mapping);
                        }
                    } else {
                        newGeneralParameters.put(mapping.getGrads(), mapping);
                    }
                }
            }
        }
        paramFileTime = file.getTimeStamp();

        generalParameters = newGeneralParameters;
        dataSetParameters = newDataSetParameters;
    }

    /**
     * Gets the Data Set Information
     *
     * @param modelName
     * @return
     */
    public DataSetInformation getDataSetInformation(String modelName) {
        DataSetInformation dsi = null;

        if (dataSetInformations.isEmpty()) {
            try {
                Map<String, DataSetInformation> newDSI = new HashMap<>(2);
                DataSetInformationLookup tmpSetInformationLookup = null;
                String fileName = getDataSetInformationFileName();
                Map<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                        fileName);

                for (LocalizationLevel locLevel : new LocalizationLevel[] {
                        LocalizationLevel.BASE, LocalizationLevel.SITE }) {
                    if (locFiles.containsKey(locLevel)) {
                        tmpSetInformationLookup = readDataSetInformationXml(
                                locFiles.get(locLevel));
                        for (DataSetInformation dataSetinfo : tmpSetInformationLookup
                                .getDataSetInformations()) {
                            newDSI.put(dataSetinfo.getModelName(), dataSetinfo);
                        }
                    }
                }
                dataSetInformations = newDSI;
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Failed to read Data Set Lookup from file.", e);
            }
        }

        if (dataSetInformations.containsKey(modelName)) {
            dsi = dataSetInformations.get(modelName);
        }

        return dsi;
    }

    private DataSetInformationLookup getDataSetInformationLookupFromFile() {
        DataSetInformationLookup dataSetInformationLookup = null;
        ILocalizationFile file = null;
        String fileName = getDataSetInformationFileName();

        try {
            file = getLocalizationFile(fileName);
        } catch (Exception e) {
            statusHandler.error(
                    " Failed to read Data Set Information Lookup: " + fileName,
                    e);
        }

        if (file != null) {
            try {
                dataSetInformationLookup = readDataSetInformationXml(file);
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Failed to Read Data Set Information Lookup from file: "
                                + file.getPath(),
                        e);
            }
        }

        return dataSetInformationLookup;
    }

    /**
     * Availability Offset file name
     *
     * @param modelName
     * @return
     */
    private static String getDataSetInformationFileName() {
        return CONFIG_FILE_ROOT + CONFIG_FILE_DATASETINFO;
    }

    /**
     * Read Data Set Information lookup
     *
     * @param file
     * @return
     * @throws Exception
     */
    private DataSetInformationLookup readDataSetInformationXml(
            ILocalizationFile file) throws Exception {
        DataSetInformationLookup dsi = null;

        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                dsi = (DataSetInformationLookup) getJaxbManager()
                        .unmarshalFromInputStream(is);
            }
        }

        return dsi;
    }

    /**
     * Read Data Set Information lookup
     *
     * @param file
     * @return
     * @throws Exception
     */
    private DataSetConfigInfoMap readDataSetConfigInfoXml(
            ILocalizationFile file) throws Exception {
        DataSetConfigInfoMap dscim = null;

        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                dscim = (DataSetConfigInfoMap) getJaxbManager()
                        .unmarshalFromInputStream(is);
            }
        }

        return dscim;
    }

    /**
     * Modify or create a data set information lookup
     *
     * @param dsi
     */
    public void modifyDataSetInformationLookup(DataSetInformation dsi) {

        try {
            DataSetInformationLookup dataSetInformationLookup = null;
            // update map and write out file
            synchronized (dataSetInformations) {

                dataSetInformations.put(dsi.getModelName(), dsi);
                dataSetInformationLookup = getDataSetInformationLookupFromFile();

                if (dataSetInformationLookup == null) {
                    dataSetInformationLookup = new DataSetInformationLookup();
                }

                if (dataSetInformationLookup.getDataSetInformations()
                        .contains(dsi)) {
                    // no changes
                    return;
                } else {
                    // clear the hash and rebuild it with what is set in current
                    dataSetInformationLookup.getDataSetInformations().clear();

                    for (Entry<String, DataSetInformation> entry : dataSetInformations
                            .entrySet()) {
                        dataSetInformationLookup.getDataSetInformations()
                                .add(entry.getValue());
                    }
                }
            }

            localizationXmlWriter
                    .writeDataSetInformationXml(dataSetInformationLookup);

            statusHandler.info("Updated/Created Data Set Information lookup! "
                    + dsi.getModelName());

        } catch (Exception e) {
            statusHandler.error(
                    "Couldn't create/update Data Set Information lookup! ", e);
        }
    }

    /**
     * Does a availability offset lookup exist?
     *
     * @return
     */
    public boolean dataSetInformationLookupExists() {
        ILocalizationFile file = null;
        String fileName = getDataSetInformationFileName();
        try {
            file = getLocalizationFile(fileName);
        } catch (Exception fnfe) {
            statusHandler
                    .error("Failed to lookup Data Set Information localization file: "
                            + fileName, fnfe);
        }
        if (file != null) {
            return file.exists();
        }

        return false;
    }

    /**
     * param file name
     *
     * @param modelName
     * @return
     */
    private static String getParamFileName() {
        return CONFIG_FILE_ROOT + CONFIG_FILE_PARAM;
    }

    /**
     * regex file name
     *
     * @param modelName
     * @return
     */
    private static String getRegexFileName() {
        return CONFIG_FILE_ROOT + CONFIG_FILE_REGEX;
    }

    /**
     * Does a a particular lookup exist?
     *
     * @param modelName
     * @return
     */
    public boolean levelLookupExists(String modelName) {
        ILocalizationFile file = null;
        String fileName = getLevelFileName(modelName);

        try {
            file = getLocalizationFile(fileName);
        } catch (Exception fnfe) {
            statusHandler
                    .error("Failed to lookup Level Lookup localization file: "
                            + fileName, fnfe);
        }

        if (file != null) {
            return file.exists();
        }

        return false;
    }

    /**
     * Modify level lookups
     *
     * @param modelName
     *            - name of model collection
     * @param dz
     *            - delta z (change in level height value from one step to next)
     * @param min
     *            - minimum level height value
     * @param max
     *            - maximum level height value
     * @throws Exception
     */
    public void modifyLevelLookups(String modelName, double dz, float min,
            float max, List<Double> levs) throws Exception {

        LevelLookup ll = null;

        if (levelLookupExists(modelName)) {
            ll = getLevelsFromFile(modelName);
            if (ll.getLevelXml() != null) {
                levs = ll.getLevelXml();
            }
        }

        if (ll == null) {
            ll = new LevelLookup();
        }

        boolean gen = false;

        if (CollectionUtil.isNullOrEmpty(levs)) {
            ll.setLevelXml(new ArrayList<Double>());
            levs = ll.getLevelXml();
            gen = true;
        } else {
            ll.setLevelXml(levs);
        }

        if (gen) {
            int diff = (int) (max - min);
            int total = (int) Math.abs((diff / dz));
            // These add simple place holder level
            // identifiers. It is up to the admin
            // to add the real values
            if (diff < 0) {
                for (int i = 0; i <= total; i++) {
                    double lev = max + i * dz;
                    levs.add(lev);
                }
            } else {
                for (int i = 0; i <= total; i++) {
                    double lev = min + i * dz;
                    levs.add(lev);
                }
            }
        }

        localizationXmlWriter.writeLevelXml(ll, modelName);

        levels.put(modelName, ll);
        statusHandler.info("Updated/Created level lookup! " + modelName);
    }

    /**
     * Read levels out of file
     *
     * @param file
     * @return
     * @throws Exception
     */
    private LevelLookup readLevelXml(ILocalizationFile file) throws Exception {
        LevelLookup levelXml = null;

        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                levelXml = (LevelLookup) getJaxbManager()
                        .unmarshalFromInputStream(is);
            }
        }

        return levelXml;
    }

    /**
     * Read parameter lookup
     *
     * @param file
     * @return
     * @throws Exception
     */
    private ParameterLookup readParameterXml(ILocalizationFile file)
            throws Exception {
        ParameterLookup paramXml = null;

        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                paramXml = (ParameterLookup) getJaxbManager()
                        .unmarshalFromInputStream(is);
            }
        }

        return paramXml;
    }

    /**
     * Read parameter regexes
     *
     * @param file
     * @return
     * @throws Exception
     */
    private ParameterRegexes readParameterRegexes(ILocalizationFile file)
            throws Exception {
        ParameterRegexes regexXml = null;

        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                regexXml = (ParameterRegexes) getJaxbManager()
                        .unmarshalFromInputStream(is);
            }
        }

        return regexXml;
    }

    public DataSetConfigInfo getDataSetConfigInfo(String dataSetName,
            String providerName) {
        DataSetConfigInfo dsci = null;

        Map<String, DataSetConfigInfo> providerMap = dataSetConfigInfos
                .get(providerName);
        if (providerMap == null || providerMap.isEmpty()) {
            try {
                Map<String, DataSetConfigInfo> newDSCI = new HashMap<>(2);
                DataSetConfigInfoMap tmpSetConfigInfoMap = null;
                String fileName = getDataSetConfigInfoFileName(providerName);
                Map<LocalizationLevel, LocalizationFile> locFiles = getLocalizationFiles(
                        fileName);

                for (LocalizationLevel locLevel : new LocalizationLevel[] {
                        LocalizationLevel.BASE, LocalizationLevel.SITE }) {
                    if (locFiles.containsKey(locLevel)) {
                        tmpSetConfigInfoMap = readDataSetConfigInfoXml(
                                locFiles.get(locLevel));
                        for (DataSetConfigInfo dataSetConfigInfo : tmpSetConfigInfoMap
                                .getDataSetConfigInfoList()) {
                            newDSCI.put(dataSetConfigInfo.getDataSetNameRegex(),
                                    dataSetConfigInfo);
                        }
                    }
                }
                dataSetConfigInfos.put(providerName, newDSCI);
                providerMap = newDSCI;
            } catch (Exception e) {
                statusHandler.error("Failed to read Data Set Lookup from file.",
                        e);
            }
        }

        if (providerMap != null) {
            Matcher m;
            for (DataSetConfigInfo tmpDsci : providerMap.values()) {
                m = tmpDsci.getDataSetNamePattern().matcher(dataSetName);
                if (m.matches()) {
                    dsci = tmpDsci;
                    break;
                }
            }
        }

        return dsci;
    }

    private static String getDataSetConfigInfoFileName(String providerName) {
        return CONFIG_FILE_ROOT + providerName + "_"
                + CONFIG_FILE_DATASETCONFIG;
    }

}
