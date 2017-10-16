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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.datadelivery.registry.Collection;
import com.raytheon.uf.common.datadelivery.registry.LevelGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.retrieval.xml.CollectionList;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ConfigLayerList;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetConfigInfoMap;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetInformation;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetInformationLookup;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetVersionInfoMap;
import com.raytheon.uf.common.datadelivery.retrieval.xml.LevelLookup;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterLevelRegex;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterLookup;
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

/**
 *
 * Utility class for Lookup Manager.
 *
 * Contains static methods for reading and writing lookup information from
 * localization files.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 10, 2017 6465       tjensen     Initial creation
 * Oct 12, 2017 6413       tjensen     Added ConfigLayer lookups
 *
 * </pre>
 *
 * @author tjensen
 */
public class LookupManagerUtils {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(LookupManagerUtils.class);

    /** Path to Lookup tables. */
    private static final String CONFIG_FILE_ROOT = "datadelivery"
            + IPathManager.SEPARATOR + "lookups" + IPathManager.SEPARATOR;

    private static final String CONFIG_FILE_PARAM = "ParameterLookup.xml";

    private static final String CONFIG_FILE_REGEX = "ParameterRegexes.xml";

    private static final String CONFIG_FILE_DATASETCONFIG = "DataSetConfigInfo.xml";

    private static final String CONFIG_FILE_DATASETINFO = "DataSetInformationLookup.xml";

    private static final String CONFIG_FILE_LEVEL = "LevelLookup.xml";

    private static final String CONFIG_FILE_DATASETVERSION = "DataSetVersionInfo.xml";

    private static final String CONFIG_FILE_COLLECTIONS = "Collections.xml";

    private static final String CONFIG_FILE_CONFIG_LAYERS = "Layers.xml";

    private static JAXBManager jaxb;

    private static synchronized JAXBManager getJaxbManager()
            throws JAXBException {
        if (jaxb == null) {
            jaxb = new JAXBManager(LevelLookup.class, ParameterLookup.class,
                    ParameterRegexes.class, DataSetInformationLookup.class,
                    DataSetInformation.class, DataSetConfigInfoMap.class,
                    DataSetVersionInfoMap.class, CollectionList.class,
                    ParameterGroup.class, LevelGroup.class,
                    ParameterLevelEntry.class, ConfigLayerList.class);
        }

        return jaxb;
    }

    public static SortedMap<LocalizationLevel, LocalizationFile> getLocalizationFiles(
            String fileName) {
        IPathManager pm = PathManagerFactory.getPathManager();
        SortedMap<LocalizationLevel, LocalizationFile> files = new TreeMap<>(
                pm.getTieredLocalizationFile(LocalizationType.COMMON_STATIC,
                        fileName));
        return files;
    }

    /**
     * Level file name
     *
     * @param modelName
     * @return
     */
    public static String getLevelFileName(String modelName) {
        return CONFIG_FILE_ROOT + modelName + CONFIG_FILE_LEVEL;
    }

    /**
     * Read levels out of file
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static LevelLookup readLevelXml(ILocalizationFile file)
            throws Exception {
        LevelLookup levelXml = null;

        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                levelXml = (LevelLookup) getJaxbManager()
                        .unmarshalFromInputStream(is);
            }
        }

        return levelXml;
    }

    public static LevelLookup getLevelsFromFile(String modelName) {
        LevelLookup levelLookup = null;
        ILocalizationFile file = null;
        String fileName = getLevelFileName(modelName);

        try {
            IPathManager pm = PathManagerFactory.getPathManager();
            file = pm.getStaticLocalizationFile(fileName);
        } catch (Exception e) {
            statusHandler.error(" Failed to Level Lookup table: " + fileName,
                    e);
        }

        if (file != null) {
            try {
                levelLookup = readLevelXml(file);
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Failed to Read Level Lookup from file: "
                                + file.getPath(),
                        e);
            }
        }

        return levelLookup;
    }

    /**
     * Does a a particular lookup exist?
     *
     * @param modelName
     * @return
     */
    public static boolean levelLookupExists(String modelName) {
        ILocalizationFile file = null;
        String fileName = getLevelFileName(modelName);

        try {
            IPathManager pm = PathManagerFactory.getPathManager();
            file = pm.getStaticLocalizationFile(fileName);
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
     * Writes the level lookup to XML
     *
     * @param ll
     * @param modelName
     * @throws Exception
     */
    public static void writeLevelXml(LevelLookup ll, String modelName)
            throws Exception {
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext lc = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.SITE);
        String fileName = getLevelFileName(modelName);
        ILocalizationFile lf = pm.getLocalizationFile(lc, fileName);
        try (SaveableOutputStream sos = lf.openOutputStream()) {
            getJaxbManager().marshalToStream(ll, sos);
            sos.save();
        }
    }

    /**
     * param file name
     *
     * @param modelName
     * @return
     */
    public static String getParamFileName() {
        return CONFIG_FILE_ROOT + CONFIG_FILE_PARAM;
    }

    /**
     * Read parameter lookup
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static ParameterLookup readParameterXml(ILocalizationFile file)
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
    public static ParameterRegexes readParameterRegexes(ILocalizationFile file)
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

    public static void populateParameterRegexes(ParameterRegexes tmpRegex,
            Map<String, ParameterNameRegex> newParamNameRegexes,
            Map<String, ParameterLevelRegex> newParamLevelRegexes) {
        for (ParameterNameRegex nameRegex : tmpRegex.getNameRegexes()) {
            newParamNameRegexes.put(nameRegex.getId(), nameRegex);
        }
        for (ParameterLevelRegex levelRegex : tmpRegex.getLevelRegexes()) {
            newParamLevelRegexes.put(levelRegex.getId(), levelRegex);
        }
    }

    /**
     * DataSet Information file name
     *
     * @param modelName
     * @return
     */
    public static String getDataSetInformationFileName() {
        return CONFIG_FILE_ROOT + CONFIG_FILE_DATASETINFO;
    }

    /**
     * Read Data Set Information lookup
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static DataSetInformationLookup readDataSetInformationXml(
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
     * Writes the DataSetInformation lookup to XML
     *
     * @param aol
     * @param modelName
     * @throws Exception
     */
    public static void writeDataSetInformationXml(DataSetInformationLookup dsi)
            throws Exception {
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext lc = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.SITE);
        String fileName = getDataSetInformationFileName();
        ILocalizationFile lf = pm.getLocalizationFile(lc, fileName);
        try (SaveableOutputStream sos = lf.openOutputStream()) {
            getJaxbManager().marshalToStream(dsi, sos);
            sos.save();
        }
    }

    public static DataSetInformationLookup getDataSetInformationLookupFromFile() {
        DataSetInformationLookup dataSetInformationLookup = null;
        ILocalizationFile file = null;
        String fileName = getDataSetInformationFileName();

        try {
            IPathManager pm = PathManagerFactory.getPathManager();
            file = pm.getStaticLocalizationFile(fileName);
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
     * Does a Dataset Information lookup exist?
     *
     * @return
     */
    public static boolean dataSetInformationLookupExists() {
        ILocalizationFile file = null;
        String fileName = getDataSetInformationFileName();
        try {

            IPathManager pm = PathManagerFactory.getPathManager();
            file = pm.getStaticLocalizationFile(fileName);
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

    public static String getDataSetConfigInfoFileName(String providerName) {
        return CONFIG_FILE_ROOT + providerName + "_"
                + CONFIG_FILE_DATASETCONFIG;
    }

    /**
     * Read Data Set Version Info
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static DataSetVersionInfoMap readDataSetVersionInfoXml(
            ILocalizationFile file) throws Exception {
        DataSetVersionInfoMap dscim = null;

        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                dscim = (DataSetVersionInfoMap) getJaxbManager()
                        .unmarshalFromInputStream(is);
            }
        }

        return dscim;
    }

    /**
     * Read Data Set Config Info lookup
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static DataSetConfigInfoMap readDataSetConfigInfoXml(
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
     * regex file name
     *
     * @param modelName
     * @return
     */
    public static String getRegexFileName() {
        return CONFIG_FILE_ROOT + CONFIG_FILE_REGEX;
    }

    public static String getDataSetVersionInfoFileName(String providerName) {
        return CONFIG_FILE_ROOT + providerName + "_"
                + CONFIG_FILE_DATASETVERSION;
    }

    /**
     * Collections file name
     *
     * @param modelName
     * @return
     */
    public static String getCollectionsFileName(String providerName) {
        return CONFIG_FILE_ROOT + providerName + "_" + CONFIG_FILE_COLLECTIONS;
    }

    /**
     * Read Collections List
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static CollectionList readCollectionsXml(ILocalizationFile file)
            throws Exception {
        CollectionList collList = null;

        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                collList = (CollectionList) getJaxbManager()
                        .unmarshalFromInputStream(is);
            }
        }

        return collList;
    }

    /**
     * Write Collections to site localization file.
     *
     * @param providerName
     * @param newCollections
     */
    public static synchronized void writeCollectionUpdates(String providerName,
            List<Collection> newCollections) {
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext lc = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.SITE);
        String fileName = getCollectionsFileName(providerName);
        ILocalizationFile lf = pm.getLocalizationFile(lc, fileName);
        Map<String, Collection> newCollectionMap = new HashMap<>(2);

        // Load the collections currently in the file
        if (lf.exists()) {
            try {
                CollectionList tmpCollections = readCollectionsXml(lf);
                for (Collection tmpColl : tmpCollections.getCollectionList()) {
                    newCollectionMap.put(tmpColl.getName(), tmpColl);
                }
            } catch (Exception e) {
                statusHandler.error(
                        "Unable to read the " + lf.getPath()
                                + " configuration! Save of new collections failed",
                        e);
                return;
            }
        }
        try (SaveableOutputStream sos = lf.openOutputStream()) {

            // Load the new ones, updating any that already were there.
            for (Collection tmpColl : newCollections) {
                newCollectionMap.put(tmpColl.getName(), tmpColl);
            }

            // Build CollectionList
            CollectionList collList = new CollectionList();
            collList.setCollectionList(
                    new ArrayList<>(newCollectionMap.values()));
            getJaxbManager().marshalToStream(collList, sos);
            sos.save();
        } catch (Exception e) {
            statusHandler.error(
                    "Unable to recreate the " + lf.getPath()
                            + " configuration! Save of new collections failed",
                    e);
        }
    }

    /**
     * Config Layer file name
     *
     * @param modelName
     * @return
     */
    public static String getLayerFileName(String providerName) {
        return CONFIG_FILE_ROOT + providerName + "_"
                + CONFIG_FILE_CONFIG_LAYERS;
    }

    /**
     * Read Data Set Information lookup
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static ConfigLayerList readLayersXml(ILocalizationFile file)
            throws Exception {
        ConfigLayerList layerList = null;

        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                layerList = (ConfigLayerList) getJaxbManager()
                        .unmarshalFromInputStream(is);
            }
        }

        return layerList;
    }
}
