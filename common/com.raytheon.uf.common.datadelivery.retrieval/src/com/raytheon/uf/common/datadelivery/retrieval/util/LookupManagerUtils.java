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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.datadelivery.registry.LevelGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.registry.URLParserInfo;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ConfigLayerList;
import com.raytheon.uf.common.datadelivery.retrieval.xml.CrawlerDates;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetConfigInfoMap;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetInformation;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetInformationLookup;
import com.raytheon.uf.common.datadelivery.retrieval.xml.DataSetVersionInfoMap;
import com.raytheon.uf.common.datadelivery.retrieval.xml.LevelLookup;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterLevelRegex;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterLookup;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterNameRegex;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterRegexes;
import com.raytheon.uf.common.datadelivery.retrieval.xml.URLParserInfoList;
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
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 10, 2017  6465     tjensen   Initial creation
 * Oct 12, 2017  6413     tjensen   Added ConfigLayer lookups
 * Oct 12, 2017  6440     bsteffen  Refresh level lookups.
 * Oct 17, 2017  6465     tjensen   Rename Collections to URLParserInfo. Add
 *                                  CrawlerDates. Improved Locking
 * Jul 11, 2018  7358     tjensen   Added null checking
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

    private static final String CONFIG_FILE_URL_PARSER_INFO = "UrlParser.xml";

    private static final String CONFIG_FILE_CONFIG_LAYERS = "Layers.xml";

    private static final String CONFIG_FILE_CRAWLER_DATES = "CrawlerDates.xml";

    private static final ReadWriteLock crawlerDatesLock = new ReentrantReadWriteLock();

    private static final ReadWriteLock upiLock = new ReentrantReadWriteLock();

    private static final ReadWriteLock dsiLock = new ReentrantReadWriteLock();

    private static final ReadWriteLock levelLock = new ReentrantReadWriteLock();

    private static JAXBManager jaxb;

    private static synchronized JAXBManager getJaxbManager()
            throws JAXBException {
        if (jaxb == null) {
            jaxb = new JAXBManager(LevelLookup.class, ParameterLookup.class,
                    ParameterRegexes.class, DataSetInformationLookup.class,
                    DataSetInformation.class, DataSetConfigInfoMap.class,
                    DataSetVersionInfoMap.class, URLParserInfoList.class,
                    ParameterGroup.class, LevelGroup.class,
                    ParameterLevelEntry.class, ConfigLayerList.class,
                    CrawlerDates.class);
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
            levelLock.readLock().lock();
            try (InputStream is = file.openInputStream()) {
                levelXml = (LevelLookup) getJaxbManager()
                        .unmarshalFromInputStream(is);
            } finally {
                levelLock.readLock().unlock();
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
        LevelLookup lookup = LookupManager.getInstance().getLevels(modelName);
        return lookup != null && lookup.isCurrent();
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
        levelLock.writeLock().lock();
        try (SaveableOutputStream sos = lf.openOutputStream()) {
            getJaxbManager().marshalToStream(ll, sos);
            sos.save();
        } finally {
            levelLock.writeLock().unlock();
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
        if (tmpRegex != null) {
            List<ParameterNameRegex> nameRegexes = tmpRegex.getNameRegexes();
            if (nameRegexes != null && !nameRegexes.isEmpty()) {
                for (ParameterNameRegex nameRegex : nameRegexes) {
                    newParamNameRegexes.put(nameRegex.getId(), nameRegex);
                }
            }

            List<ParameterLevelRegex> levelRegexes = tmpRegex.getLevelRegexes();
            if (levelRegexes != null && !levelRegexes.isEmpty()) {
                for (ParameterLevelRegex levelRegex : levelRegexes) {
                    newParamLevelRegexes.put(levelRegex.getId(), levelRegex);
                }
            }
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
            dsiLock.readLock().lock();
            try (InputStream is = file.openInputStream()) {
                dsi = (DataSetInformationLookup) getJaxbManager()
                        .unmarshalFromInputStream(is);
            } finally {
                dsiLock.readLock().unlock();
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
        dsiLock.writeLock().lock();
        try (SaveableOutputStream sos = lf.openOutputStream()) {
            getJaxbManager().marshalToStream(dsi, sos);
            sos.save();
        } finally {
            dsiLock.writeLock().unlock();
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
     * URLParserInfos file name
     *
     * @param modelName
     * @return
     */
    public static String getURLParserInfoFileName(String providerName) {
        return CONFIG_FILE_ROOT + providerName + "_"
                + CONFIG_FILE_URL_PARSER_INFO;
    }

    /**
     * Read URLParserInfos List
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static URLParserInfoList readURLParserInfosXml(
            ILocalizationFile file) throws Exception {
        URLParserInfoList collList = null;

        if (file != null && file.exists()) {
            upiLock.readLock().lock();
            try (InputStream is = file.openInputStream()) {
                collList = (URLParserInfoList) getJaxbManager()
                        .unmarshalFromInputStream(is);
            } finally {
                upiLock.readLock().unlock();
            }
        }

        return collList;
    }

    /**
     * Write URLParserInfo to site localization file.
     *
     * @param providerName
     * @param newURLParserInfos
     */
    public static void writeURLParserInfoUpdates(String providerName,
            Map<String, URLParserInfo> newURLParserInfos) {
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext lc = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.SITE);
        String fileName = getURLParserInfoFileName(providerName);
        ILocalizationFile lf = pm.getLocalizationFile(lc, fileName);
        Map<String, URLParserInfo> newURLParserInfoMap = new HashMap<>(2);

        upiLock.writeLock().lock();
        try {
            // Load the URLParserInfos currently in the file
            if (lf.exists()) {
                try {
                    /*
                     * This will create a read lock, but since this is a
                     * reentrant lock and in the same thread, it won't block.
                     */
                    URLParserInfoList tmpURLParserInfo = readURLParserInfosXml(
                            lf);
                    for (URLParserInfo tmpUPI : tmpURLParserInfo
                            .getUrlParserInfoList()) {
                        newURLParserInfoMap.put(tmpUPI.getName(), tmpUPI);
                    }
                } catch (Exception e) {
                    statusHandler.error(
                            "Unable to read the " + lf.getPath()
                                    + " configuration! Save of new URLParserInfo failed",
                            e);
                    return;
                }
            }
            try (SaveableOutputStream sos = lf.openOutputStream()) {

                // Load the new ones, updating any that already were there.
                for (URLParserInfo tmpUPI : newURLParserInfos.values()) {
                    newURLParserInfoMap.put(tmpUPI.getName(), tmpUPI);
                }

                // Build URLParserInfoList
                URLParserInfoList upiList = new URLParserInfoList();
                upiList.setUrlParserInfoList(
                        new ArrayList<>(newURLParserInfoMap.values()));
                getJaxbManager().marshalToStream(upiList, sos);
                sos.save();
            } catch (Exception e) {
                statusHandler.error(
                        "Unable to recreate the " + lf.getPath()
                                + " configuration! Save of new URLParserInfo failed",
                        e);
            }
        } finally {
            upiLock.writeLock().unlock();
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
     * Read Config Layer lookup
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

    /**
     * Get CrawlerDates file name for a collection.
     *
     * @param collectionName
     * @return
     */
    public static String getCrawlerDatesFileName(String collectionName) {
        return CONFIG_FILE_ROOT + collectionName + CONFIG_FILE_CRAWLER_DATES;
    }

    /**
     * Read in the CrawlerDates from a localization file. After the file has
     * been read, delete it.
     *
     * @param file
     * @return
     * @throws Exception
     */
    private static CrawlerDates readAndDeleteCrawlerDatesXml(
            ILocalizationFile file) throws Exception {
        CrawlerDates crawlerDates = null;

        if (file != null && file.exists()) {
            try (InputStream is = file.openInputStream()) {
                crawlerDates = (CrawlerDates) getJaxbManager()
                        .unmarshalFromInputStream(is);

                file.delete();
            } catch (LocalizationException e) {
                statusHandler.error(
                        "Unable to delete crawler dates file " + file.getPath(),
                        e);
            }
        }

        return crawlerDates;
    }

    /**
     * Writes out dates found during a seed scan to a localization file. Used
     * during the main scan to add these dates to be processed.
     *
     * @param collectionName
     * @param newDates
     */
    public static void writeCrawlerDates(String collectionName,
            Set<String> newDates) {
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext lc = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.SITE);
        String fileName = getCrawlerDatesFileName(collectionName);
        ILocalizationFile lf = pm.getLocalizationFile(lc, fileName);
        Set<String> datesToWrite = new TreeSet<>();

        crawlerDatesLock.writeLock().lock();
        try {
            // Load the URLParserInfos currently in the file
            if (lf.exists()) {
                try {
                    CrawlerDates tmpCrawlDates = readAndDeleteCrawlerDatesXml(
                            lf);
                    datesToWrite.addAll(tmpCrawlDates.getDates());
                } catch (Exception e) {
                    statusHandler.error(
                            "Unable to read the " + lf.getPath()
                                    + " configuration! Save of new Crawler Dates failed",
                            e);
                    return;
                }
            }
            try (SaveableOutputStream sos = lf.openOutputStream()) {

                // Load the new ones
                datesToWrite.addAll(newDates);

                // Builder CrawlerDates
                CrawlerDates newCrawlDates = new CrawlerDates();
                newCrawlDates.setDates(new ArrayList<>(datesToWrite));
                getJaxbManager().marshalToStream(newCrawlDates, sos);
                sos.save();
            } catch (Exception e) {
                statusHandler.error(
                        "Unable to recreate the " + lf.getPath()
                                + " configuration! Save of new Crawler Dates failed",
                        e);
            }
        } finally {
            crawlerDatesLock.writeLock().unlock();
        }
    }

    /**
     * Parses out the dates from a CrawlerDates file and returns them as list.
     *
     * @param collectionName
     * @param dateFormat
     * @return
     */
    public static List<Date> getCrawlerDates(String collectionName,
            String dateFormat) {
        List<Date> crawlDates = Collections.emptyList();
        ILocalizationFile file = null;
        String fileName = getCrawlerDatesFileName(collectionName);

        try {
            IPathManager pm = PathManagerFactory.getPathManager();
            file = pm.getStaticLocalizationFile(fileName);
        } catch (Exception fnfe) {
            statusHandler
                    .error("Failed to lookup Crawler Dates localization file: "
                            + fileName, fnfe);
        }
        if (file != null) {
            /*
             * Acquiring a write lock here because the file is deleted after
             * it's read
             */
            crawlerDatesLock.writeLock().lock();
            try {
                CrawlerDates cd = readAndDeleteCrawlerDatesXml(file);
                SimpleDateFormat sdf = new SimpleDateFormat();
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                sdf.applyLocalizedPattern(dateFormat);
                List<String> dates = cd.getDates();
                if (dates != null) {
                    List<Date> dateDates = new ArrayList<>(dates.size());
                    for (String dateString : dates) {
                        dateString = dateString.trim();
                        if (!dateString.isEmpty()) {
                            dateDates.add(sdf.parse(dateString));
                        }
                    }
                    crawlDates = dateDates;
                }
            } catch (Exception e) {
                statusHandler.error(
                        "Failed to read Crawler Dates from file " + fileName,
                        e);
            } finally {
                crawlerDatesLock.writeLock().unlock();
            }
        }

        return crawlDates;
    }
}
