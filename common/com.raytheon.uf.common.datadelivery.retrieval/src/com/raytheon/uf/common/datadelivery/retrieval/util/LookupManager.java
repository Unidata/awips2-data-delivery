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

import java.io.IOException;
import java.text.ParseException;
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

import com.raytheon.uf.common.datadelivery.registry.Collection;
import com.raytheon.uf.common.datadelivery.retrieval.xml.CollectionList;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetConfigInfo;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetConfigInfoMap;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetInformation;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetInformationLookup;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetNamePattern;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetVersionInfo;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetVersionInfoMap;
import com.raytheon.uf.common.datadelivery.retrieval.xml.LevelLookup;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterLevelRegex;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterLookup;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterMapping;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterNameRegex;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterRegexes;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.CollectionUtil;

/**
 * Manages information stored in lookup configuration files. This information is
 * typically not specific to a single class, so is stored here for more general
 * use.
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
 * May 10, 2017  6130     tjensen   Updated for DataSetVersionInfo
 * Oct 04, 2017  6465     tjensen   Add CollectionLists. Refactor localization
 *                                  file retrieval.
 *
 * </pre>
 *
 * @author dhladky
 */
public class LookupManager {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(LookupManager.class);

    /** Singleton instance of this class */
    private static final LookupManager instance = new LookupManager();

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

    private final Map<String, Map<String, Collection>> collectionMap = new LinkedHashMap<>(
            2);

    private final Map<String, LevelLookup> levels = new HashMap<>(2);

    private Map<String, DataSetInformation> dataSetInformations = new HashMap<>(
            2);

    private Date regexFileTime;

    private Date paramFileTime;

    private final Map<String, Date> collectionFileTimes = new HashMap<>(2);

    private final Map<String, Map<String, DataSetConfigInfo>> dataSetConfigInfos = new HashMap<>(
            2);

    private final Map<String, Map<String, DataSetVersionInfo>> dataSetVersionInfos = new HashMap<>(
            2);

    /* Private Constructor */
    private LookupManager() {

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

            modelLevels = LookupManagerUtils.getLevelsFromFile(modelName);
            levels.put(modelName, modelLevels);
        }

        return modelLevels;
    }

    /**
     * Gets the Model parameters
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public void loadParameterRegexes() {
        ILocalizationFile file = null;
        String fileName = LookupManagerUtils.getRegexFileName();

        try {
            IPathManager pm = PathManagerFactory.getPathManager();
            file = pm.getStaticLocalizationFile(fileName);
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
                    Map<LocalizationLevel, LocalizationFile> locFiles = LookupManagerUtils
                            .getLocalizationFiles(fileName);

                    Map<String, ParameterNameRegex> newParamNameRegexes = new LinkedHashMap<>(
                            2);
                    Map<String, ParameterLevelRegex> newParamLevelRegexes = new LinkedHashMap<>(
                            2);

                    for (LocalizationFile locfile : locFiles.values()) {
                        tmpRegex = LookupManagerUtils
                                .readParameterRegexes(locfile);
                        LookupManagerUtils.populateParameterRegexes(tmpRegex,
                                newParamNameRegexes, newParamLevelRegexes);
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

    public <K, V extends Comparable<? super V>> Map<K, V> sortByValue(
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
        String fileName = LookupManagerUtils.getParamFileName();

        try {
            IPathManager pm = PathManagerFactory.getPathManager();
            file = pm.getStaticLocalizationFile(fileName);
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
        Map<LocalizationLevel, LocalizationFile> locFiles = LookupManagerUtils
                .getLocalizationFiles(fileName);
        for (LocalizationLevel locLevel : new LocalizationLevel[] {
                LocalizationLevel.BASE, LocalizationLevel.SITE }) {
            if (locFiles.containsKey(locLevel)) {
                tmpLookup = LookupManagerUtils
                        .readParameterXml(locFiles.get(locLevel));
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
                String fileName = LookupManagerUtils
                        .getDataSetInformationFileName();
                Map<LocalizationLevel, LocalizationFile> locFiles = LookupManagerUtils
                        .getLocalizationFiles(fileName);

                for (LocalizationFile locfile : locFiles.values()) {
                    tmpSetInformationLookup = LookupManagerUtils
                            .readDataSetInformationXml(locfile);
                    for (DataSetInformation dataSetinfo : tmpSetInformationLookup
                            .getDataSetInformations()) {
                        newDSI.put(dataSetinfo.getModelName(), dataSetinfo);
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
                dataSetInformationLookup = LookupManagerUtils
                        .getDataSetInformationLookupFromFile();

                if (dataSetInformationLookup == null) {
                    dataSetInformationLookup = new DataSetInformationLookup();
                }

                if (dataSetInformationLookup.getDataSetInformations()
                        .contains(dsi)) {
                    // no changes
                    return;
                }
                // clear the hash and rebuild it with what is set in current
                dataSetInformationLookup.getDataSetInformations().clear();

                for (Entry<String, DataSetInformation> entry : dataSetInformations
                        .entrySet()) {
                    dataSetInformationLookup.getDataSetInformations()
                            .add(entry.getValue());
                }
            }

            LookupManagerUtils
                    .writeDataSetInformationXml(dataSetInformationLookup);

            statusHandler.info("Updated/Created Data Set Information lookup! "
                    + dsi.getModelName());

        } catch (Exception e) {
            statusHandler.error(
                    "Couldn't create/update Data Set Information lookup! ", e);
        }
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

        if (LookupManagerUtils.levelLookupExists(modelName)) {
            ll = LookupManagerUtils.getLevelsFromFile(modelName);
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

        LookupManagerUtils.writeLevelXml(ll, modelName);

        levels.put(modelName, ll);
        statusHandler.info("Updated/Created level lookup! " + modelName);
    }

    public DataSetConfigInfo getDataSetConfigInfo(String dataSetName,
            String providerName) {
        DataSetConfigInfo dsci = null;

        Map<String, DataSetConfigInfo> providerMap = dataSetConfigInfos
                .get(providerName);
        if (providerMap == null) {
            try {
                Map<String, DataSetConfigInfo> newDSCI = new HashMap<>(2);
                DataSetConfigInfoMap tmpSetConfigInfoMap = null;
                String fileName = LookupManagerUtils
                        .getDataSetConfigInfoFileName(providerName);
                Map<LocalizationLevel, LocalizationFile> locFiles = LookupManagerUtils
                        .getLocalizationFiles(fileName);

                for (LocalizationFile locfile : locFiles.values()) {
                    tmpSetConfigInfoMap = LookupManagerUtils
                            .readDataSetConfigInfoXml(locfile);
                    if (tmpSetConfigInfoMap != null
                            && tmpSetConfigInfoMap
                                    .getDataSetConfigInfoList() != null
                            && !tmpSetConfigInfoMap.getDataSetConfigInfoList()
                                    .isEmpty()) {
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

    public DataSetVersionInfo getDataSetVersionInfo(String dataSetName,
            String providerName) throws IOException {
        DataSetVersionInfo dsvi = null;

        Map<String, DataSetVersionInfo> providerMap = dataSetVersionInfos
                .get(providerName);
        if (providerMap == null || providerMap.isEmpty()) {
            try {
                Map<String, DataSetVersionInfo> newDSVI = new HashMap<>(2);
                DataSetVersionInfoMap tmpSetVersionInfoMap = null;
                String fileName = LookupManagerUtils
                        .getDataSetVersionInfoFileName(providerName);
                Map<LocalizationLevel, LocalizationFile> locFiles = LookupManagerUtils
                        .getLocalizationFiles(fileName);

                for (LocalizationFile locfile : locFiles.values()) {
                    tmpSetVersionInfoMap = LookupManagerUtils
                            .readDataSetVersionInfoXml(locfile);
                    String validateErrors = tmpSetVersionInfoMap.validate();
                    if ("".equals(validateErrors)) {
                        for (DataSetVersionInfo dataSetVersionInfo : tmpSetVersionInfoMap
                                .getDsviList()) {

                            newDSVI.put(dataSetVersionInfo.getId(),
                                    dataSetVersionInfo);
                        }
                    } else {
                        throw new ParseException("Errors detected in file '"
                                + locfile.getPath() + "': " + validateErrors,
                                0);
                    }
                }

                dataSetVersionInfos.put(providerName, newDSVI);
                providerMap = newDSVI;
            } catch (Exception e) {
                throw new IOException(
                        "Failed to read Data Set Lookup from file.", e);

            }
        }

        /*
         * Loop over all version info for the specified provider and see if the
         * data set name matches the patterns in the map. In the case of
         * multiple valid matches, the first version info that matches will be
         * used.
         */
        for (DataSetVersionInfo tmpDsci : providerMap.values()) {
            DataSetNamePattern dataSetNamePattern = tmpDsci
                    .getDataSetNamePattern();

            if (dataSetNamePattern != null
                    && dataSetNamePattern.checkName(dataSetName)) {
                dsvi = tmpDsci;
                break;
            }
        }

        return dsvi;
    }

    public Map<String, Collection> getCollectionsForProvider(
            String providerName) {
        loadCollectionMap(providerName);
        return collectionMap.get(providerName);
    }

    /**
     * Gets the Collections Map
     *
     * @return
     */
    public void loadCollectionMap(String providerName) {
        ILocalizationFile file = null;
        String fileName = LookupManagerUtils
                .getCollectionsFileName(providerName);

        try {
            IPathManager pm = PathManagerFactory.getPathManager();
            file = pm.getStaticLocalizationFile(fileName);
        } catch (Exception e) {
            statusHandler.error(
                    " Failed to find Parameter Lookup table: " + fileName, e);
        }

        if (file != null) {
            try {
                /*
                 * If map for the provider is null or the timestamp on the file
                 * has changed since we last read it, read the collections from
                 * the file.
                 */
                if (collectionMap.get(providerName) == null
                        || !file.getTimeStamp().equals(
                                collectionFileTimes.get(providerName))) {
                    CollectionList tmpCollections = null;
                    Map<LocalizationLevel, LocalizationFile> locFiles = LookupManagerUtils
                            .getLocalizationFiles(fileName);

                    Map<String, Collection> newCollectionMap = new LinkedHashMap<>(
                            2);

                    for (LocalizationFile locfile : locFiles.values()) {
                        tmpCollections = LookupManagerUtils
                                .readCollectionsXml(locfile);
                        for (Collection tmpColl : tmpCollections
                                .getCollectionList()) {
                            newCollectionMap.put(tmpColl.getName(), tmpColl);
                        }
                    }

                    collectionMap.put(providerName, newCollectionMap);
                    collectionFileTimes.put(providerName, file.getTimeStamp());
                }
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Failed to Collection List from file: "
                                + file.getPath(),
                        e);
            }
        }
    }
}
