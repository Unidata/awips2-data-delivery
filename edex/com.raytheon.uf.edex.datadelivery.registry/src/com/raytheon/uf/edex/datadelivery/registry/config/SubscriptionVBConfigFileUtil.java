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
package com.raytheon.uf.edex.datadelivery.registry.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.registry.LevelGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataplugin.level.LevelFactory;
import com.raytheon.uf.common.dataplugin.level.mapping.DatabaseLevelMapping;
import com.raytheon.uf.common.dataplugin.level.mapping.LevelMapping;
import com.raytheon.uf.common.dataplugin.level.mapping.LevelMappingFactory;
import com.raytheon.uf.common.dataplugin.level.mapping.LevelMappingFile;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.SaveableOutputStream;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.menus.vb.VbSource;
import com.raytheon.uf.common.menus.vb.VbSourceList;
import com.raytheon.uf.common.menus.vb.ViewMenu;
import com.raytheon.uf.common.menus.xml.CommonAbstractMenuContribution;
import com.raytheon.uf.common.menus.xml.CommonIncludeMenuItem;
import com.raytheon.uf.common.menus.xml.CommonMenuContribution;
import com.raytheon.uf.common.menus.xml.CommonMenuContributionFile;
import com.raytheon.uf.common.menus.xml.CommonToolbarSubmenuContribution;
import com.raytheon.uf.common.menus.xml.MenuTemplateFile;
import com.raytheon.uf.common.parameter.Parameter;
import com.raytheon.uf.common.parameter.lookup.ParameterLookup;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.edex.registry.ebxml.util.RegistryIdUtil;

/**
 * Utility class for maintaining the Volume Browser config files for
 * Subscriptions.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Dec 06, 2017  6355     nabowle   Initial creation
 * Apr 03, 2018  7240     tjensen   Change to group by model name instead of
 *                                  subscription
 *
 * </pre>
 *
 * @author nabowle
 */

public class SubscriptionVBConfigFileUtil {

    private static final String DD_MODEL_PREFIX = "DD_";

    private static final Logger statusHandler = LoggerFactory
            .getLogger(SubscriptionVBConfigFileUtil.class);

    public static final String VBSOURCES_PATH = "volumebrowser"
            + IPathManager.SEPARATOR + "VbSources" + IPathManager.SEPARATOR;

    public static final String LEVEL_MAPPINGS_PATH = "volumebrowser"
            + IPathManager.SEPARATOR + "levelMappings" + IPathManager.SEPARATOR;

    public static final String BASE_SUB_PATH = "menus" + IPathManager.SEPARATOR
            + "datadelivery" + IPathManager.SEPARATOR + "volumebrowser"
            + IPathManager.SEPARATOR;

    public static final String PVTS_FIELDS_FILE = IPathManager.SEPARATOR
            + "pvts_fields.xml";

    public static final String SOUNDING_FIELDS_FILE = IPathManager.SEPARATOR
            + "sounding_fields.xml";

    public static final String TIMEHEIGHT_FIELDS_FILE = IPathManager.SEPARATOR
            + "timeheight_fields.xml";

    public static final String XSECT_FIELDS_FILE = IPathManager.SEPARATOR
            + "xsect_fields.xml";

    public static final String PV_PLANES_FILE = IPathManager.SEPARATOR
            + "pv_planes.xml";

    public static final String PV_SPACE_PLANES_FILE = IPathManager.SEPARATOR
            + "pv_space_planes.xml";

    public static final String XSECT_SPACE_PLANES_FILE = IPathManager.SEPARATOR
            + "xsect_space_planes.xml";

    public static final String XSECT_TIME_PLANES_FILE = IPathManager.SEPARATOR
            + "xsect_time_planes.xml";

    public static final String XSECT_TIME_PLANES_MENU_LOC = "menu:planes.menus.xsect.time.dd";

    public static final String XSECT_SPACE_PLANES_MENU_LOC = "menu:planes.menus.xsect.space.dd";

    public static final String PV_SPACE_PLANES_MENU_LOC = "menu:planes.menus.planview.space.dd";

    public static final String PV_PLANES_MENU_LOC = "menu:planes.menus.planview.dd";

    public static final String XSECT_FIELDS_MENU_LOC = "menu:fields.menus.xsect.dd";

    public static final String TIMEHEIGHT_FIELDS_MENU_LOC = "menu:fields.menus.timeheight.dd";

    public static final String SOUNDING_FIELDS_MENU_LOC = "menu:fields.menus.sounding.dd";

    public static final String PVTS_FIELDS_MENU_LOC = "menu:fields.menus.dd";

    public static final JAXBManager jaxb;

    static {
        JAXBManager jax = null;
        try {
            jax = new JAXBManager(VbSourceList.class, VbSource.class,
                    CommonMenuContributionFile.class,
                    CommonIncludeMenuItem.class, MenuTemplateFile.class,
                    CommonMenuContribution.class,
                    CommonAbstractMenuContribution.class,
                    CommonToolbarSubmenuContribution.class,
                    LevelMappingFile.class, LevelMapping.class,
                    DatabaseLevelMapping.class);
        } catch (JAXBException e) {
            statusHandler.error("Unable to initialize the JAXBManager.", e);
        }
        jaxb = jax;
    }

    private SubscriptionVBConfigFileUtil() {
        super();
    }

    /**
     * Update the Volume Browser config files for a given datasets. Information
     * from any current subscriptions will be used to generate the necessary
     * configuration information. If no subscriptions to the dataset exist, all
     * configuration files for the dataset will be deleted.
     *
     * @param dsName
     *            Name of the DataSet
     * @param provider
     *            Name of the data provider
     */
    public static void updateDataSetFiles(String dsName, String provider) {

        try {
            List<Subscription> dsSubs = DataDeliveryHandlers
                    .getSubscriptionHandler().getByDataSetAndProviderForSite(
                            dsName, provider, RegistryIdUtil.getId());
            if (!dsSubs.isEmpty()) {
                statusHandler
                        .info("Updating the localized Volume Browser config files for dataset '"
                                + dsName + "'");
                Map<String, ParameterGroup> combinedParameterGroups = combineParameterGroups(
                        dsSubs);
                boolean isVertical = checkForVerticalSubs(dsSubs);

                String ddName = DD_MODEL_PREFIX + dsName;

                createVolumeBrowserSource(ddName, isVertical);
                createFieldsFiles(ddName, isVertical, combinedParameterGroups);
                createPlanesFiles(ddName, isVertical, combinedParameterGroups);
                createDataSetIndexXml(ddName, isVertical);
            } else {
                deleteDataSetFiles(dsName);
            }
        } catch (RegistryHandlerException e) {
            statusHandler.error(
                    "Error updating localized Volume Browser config files", e);
        }

    }

    private static boolean checkForVerticalSubs(List<Subscription> dsSubs) {
        boolean isVertical = false;
        for (Subscription sub : dsSubs) {
            if (sub.isVertical()) {
                isVertical = true;
                break;
            }
        }
        return isVertical;
    }

    private static Map<String, ParameterGroup> combineParameterGroups(
            List<Subscription> dsSubs) {
        Map<String, ParameterGroup> combinedParameterGroups = new HashMap<>();
        for (Subscription sub : dsSubs) {
            Map<String, ParameterGroup> subPGs = sub.getParameterGroups();
            for (Entry<String, ParameterGroup> pge : subPGs.entrySet()) {
                String pgKey = pge.getKey();
                ParameterGroup pg = pge.getValue();
                ParameterGroup combinedPG = combinedParameterGroups.get(pgKey);
                if (combinedPG == null) {
                    combinedPG = new ParameterGroup(pg.getAbbrev(),
                            pg.getUnits());
                    combinedParameterGroups.put(combinedPG.getKey(),
                            combinedPG);
                }

                for (LevelGroup lg : pg.getGroupedLevels().values()) {
                    LevelGroup combinedLG = combinedPG
                            .getLevelGroup(lg.getKey());
                    if (combinedLG == null) {
                        combinedLG = new LevelGroup(lg.getName(),
                                lg.getUnits());
                        combinedLG.setMasterKey(lg.getMasterKey());
                        combinedPG.putLevelGroup(combinedLG);
                    }

                    for (ParameterLevelEntry ple : lg.getLevels()) {
                        List<ParameterLevelEntry> combinedLevels = combinedLG
                                .getLevels();
                        if (!combinedLevels.contains(ple)) {
                            combinedLevels.add(new ParameterLevelEntry(ple));
                        }
                    }
                }
            }
        }
        return combinedParameterGroups;
    }

    /**
     * Delete all of the CONFIGURED localized Volume Browser config files for a
     * given dataset.
     *
     * @param dsName
     *            The dataset name.
     */
    private static void deleteDataSetFiles(String dsName) {
        String ddName = DD_MODEL_PREFIX + dsName;
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationContext context = pathMgr.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.CONFIGURED);

        // Remove localization dir
        ILocalizationFile localizationDir = pathMgr.getLocalizationFile(context,
                SubscriptionVBConfigFileUtil.BASE_SUB_PATH + ddName);
        if (localizationDir.exists()) {
            statusHandler
                    .info("Deleting the localized Volume Browser config files for "
                            + dsName);

            ILocalizationFile[] files = PathManagerFactory.getPathManager()
                    .listFiles(context, localizationDir.getPath(), null, false,
                            true);
            for (ILocalizationFile lf : files) {
                try {
                    lf.delete();
                } catch (LocalizationException e) {
                    statusHandler.warn("Unable to delete the file "
                            + lf.getPath() + " for " + dsName + ".", e);
                }
            }
            try {
                localizationDir.delete();
            } catch (LocalizationException e) {
                statusHandler.warn("Unable to delete the directory "
                        + localizationDir.getPath() + " for " + dsName + ".",
                        e);
            }
        }

        // Remove Level Mapping file
        ILocalizationFile levelMappingFile = pathMgr.getLocalizationFile(
                context, SubscriptionVBConfigFileUtil.LEVEL_MAPPINGS_PATH
                        + ddName + ".xml");
        if (levelMappingFile.exists()) {
            try {
                levelMappingFile.delete();
            } catch (LocalizationException e) {
                statusHandler.warn("Unable to delete the Level Mapping file "
                        + levelMappingFile.getPath(), e);
            }
        }

        // Remove Volume Browser Sources
        context = pathMgr.getContext(LocalizationType.CAVE_STATIC,
                LocalizationLevel.CONFIGURED);
        ILocalizationFile vbSourcesFile = pathMgr.getLocalizationFile(context,
                SubscriptionVBConfigFileUtil.VBSOURCES_PATH + ddName + ".xml");
        if (vbSourcesFile.exists()) {
            try {
                vbSourcesFile.delete();
            } catch (LocalizationException e) {
                statusHandler.warn("Unable to delete the VbSources file "
                        + vbSourcesFile.getPath(), e);
            }
        }

    }

    /**
     * Create the VBSource config file for the given model. An existing config
     * file for this model will be overwritten.
     *
     * @param sub
     */
    private static void createVolumeBrowserSource(String ddName,
            boolean isVertical) {
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationContext context = pathMgr.getContext(
                LocalizationType.CAVE_STATIC, LocalizationLevel.CONFIGURED);
        ILocalizationFile vbSourceFile = pathMgr.getLocalizationFile(context,
                VBSOURCES_PATH + ddName + ".xml");

        VbSource source = new VbSource();
        source.setKey(ddName);
        source.setCategory("DataDelivery");
        List<ViewMenu> views;
        if (isVertical) {
            views = Arrays.asList(ViewMenu.values());
        } else {
            views = new ArrayList<>();
            views.add(ViewMenu.PLANVIEW);
            views.add(ViewMenu.TIMESERIES);
        }
        source.setViews(views);
        source.setRemove(false);

        VbSourceList vbSourceList = new VbSourceList();
        vbSourceList.setEntries(Arrays.asList(source));

        try {
            String contents = jaxb.marshalToXml(vbSourceList);
            writeOrDeleteFile(vbSourceFile, contents, true);
        } catch (JAXBException e) {
            statusHandler
                    .warn("Unable to marshal the VolumeBrowser index file for "
                            + ddName, e);
        }
    }

    private static String getFilePath(String ddName, String file) {
        return BASE_SUB_PATH + ddName + (file.startsWith(IPathManager.SEPARATOR)
                ? "" : IPathManager.SEPARATOR) + file;
    }

    /**
     * Creates the index.xml for the given model. An existing index.xml file for
     * this model will be overwritten.
     *
     * @param ddName
     *            Name of the DD Dataset
     * @param isVertical
     *            If subscriptions for this dataset were marked as vertical.
     */
    private static void createDataSetIndexXml(String ddName,
            boolean isVertical) {
        // Fields

        List<CommonIncludeMenuItem> includes = new ArrayList<>();
        CommonIncludeMenuItem cmi;
        String path = getFilePath(ddName, PVTS_FIELDS_FILE);

        /*
         * Verify that the expected file exists. If it doesn't, something went
         * wrong. Display a what we can, but avoid breaking the VolumeBrowser.
         */

        if (fileExists(path)) {
            cmi = new CommonIncludeMenuItem();
            cmi.fileName = new File(path);
            cmi.installationLocation = PVTS_FIELDS_MENU_LOC;
            includes.add(cmi);
        }

        if (isVertical) {
            path = getFilePath(ddName, SOUNDING_FIELDS_FILE);
            if (fileExists(path)) {
                cmi = new CommonIncludeMenuItem();
                cmi.fileName = new File(path);
                cmi.installationLocation = SOUNDING_FIELDS_MENU_LOC;
                includes.add(cmi);
            }

            path = getFilePath(ddName, TIMEHEIGHT_FIELDS_FILE);
            if (fileExists(path)) {
                cmi = new CommonIncludeMenuItem();
                cmi.fileName = new File(path);
                cmi.installationLocation = TIMEHEIGHT_FIELDS_MENU_LOC;
                includes.add(cmi);
            }

            path = getFilePath(ddName, XSECT_FIELDS_FILE);
            if (fileExists(path)) {
                cmi = new CommonIncludeMenuItem();
                cmi.fileName = new File(path);
                cmi.installationLocation = XSECT_FIELDS_MENU_LOC;
                includes.add(cmi);
            }
        }

        // Planes

        path = getFilePath(ddName, PV_PLANES_FILE);
        if (fileExists(path)) {
            cmi = new CommonIncludeMenuItem();
            cmi.fileName = new File(path);
            cmi.installationLocation = PV_PLANES_MENU_LOC;
            includes.add(cmi);
        }

        if (isVertical) {
            path = getFilePath(ddName, PV_SPACE_PLANES_FILE);
            if (fileExists(path)) {
                cmi = new CommonIncludeMenuItem();
                cmi.fileName = new File(path);
                cmi.installationLocation = PV_SPACE_PLANES_MENU_LOC;
                includes.add(cmi);
            }

            path = getFilePath(ddName, XSECT_SPACE_PLANES_FILE);
            if (fileExists(path)) {
                cmi = new CommonIncludeMenuItem();
                cmi.fileName = new File(path);
                cmi.installationLocation = XSECT_SPACE_PLANES_MENU_LOC;
                includes.add(cmi);
            }

            path = getFilePath(ddName, XSECT_TIME_PLANES_FILE);
            if (fileExists(path)) {
                cmi = new CommonIncludeMenuItem();
                cmi.fileName = new File(path);
                cmi.installationLocation = XSECT_TIME_PLANES_MENU_LOC;
                includes.add(cmi);
            }
        }

        /*
         * Something went wrong when creating the includes files. Do not create
         * an index.xml file for this model to avoid breaking the VolumeBrowser.
         */
        if (includes.isEmpty()) {
            statusHandler
                    .warn("The included fields and planes files were not created. Skipping creating the index.xml for "
                            + ddName);
            return;
        }

        CommonMenuContributionFile mcf = new CommonMenuContributionFile();
        mcf.contribution = includes.toArray(new CommonIncludeMenuItem[0]);

        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationContext context = pathMgr.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.CONFIGURED);
        ILocalizationFile indexFile = pathMgr.getLocalizationFile(context,
                getFilePath(ddName, "index.xml"));

        try {
            String contents = jaxb.marshalToXml(mcf);
            writeOrDeleteFile(indexFile, contents, true);
        } catch (JAXBException e) {
            statusHandler
                    .warn("Unable to marshal the VolumeBrowser index file for "
                            + ddName, e);
        }

    }

    private static boolean fileExists(String path) {
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationContext context = pathMgr.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.CONFIGURED);
        ILocalizationFile file = pathMgr.getLocalizationFile(context, path);

        return file.exists();
    }

    /**
     * Create the appropriate fields files for the Volume Browser for this
     * model. Existing fields files for this model will be destroyed.
     *
     * @param sub
     */
    private static void createFieldsFiles(String ddName, boolean isVertical,
            Map<String, ParameterGroup> paramGroups) {
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationContext context = pathMgr.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.CONFIGURED);

        List<CommonMenuContribution> contributions = new ArrayList<>();

        for (ParameterGroup pg : paramGroups.values()) {
            CommonMenuContribution cmc = new CommonMenuContribution();
            Parameter parm = ParameterLookup.getInstance()
                    .getParameter(pg.getAbbrev());
            if (parm == null) {
                cmc.menuText = pg.getAbbrev();
            } else {
                cmc.menuText = parm.getName();
            }
            cmc.key = pg.getAbbrev();
            cmc.indentText = false;
            contributions.add(cmc);
        }

        /*
         * Sort the contributions by their menuText to make browsing the menu
         * easier/less chaotic.
         */
        Collections.sort(contributions, (cmc1, cmc2) -> {
            return cmc1.menuText.compareTo(cmc2.menuText);
        });

        CommonToolbarSubmenuContribution ctsc = new CommonToolbarSubmenuContribution();
        ctsc.menuText = ddName;
        ctsc.contributions = contributions
                .toArray(new CommonAbstractMenuContribution[0]);

        MenuTemplateFile mtf = new MenuTemplateFile();
        mtf.contributions = new CommonAbstractMenuContribution[] { ctsc };
        try {
            String contentsStr = jaxb.marshalToXml(mtf);

            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(ddName, PVTS_FIELDS_FILE)),
                    contentsStr, true);
            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(ddName, SOUNDING_FIELDS_FILE)),
                    contentsStr, isVertical);
            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(ddName, TIMEHEIGHT_FIELDS_FILE)),
                    contentsStr, isVertical);
            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(ddName, XSECT_FIELDS_FILE)),
                    contentsStr, isVertical);
        } catch (JAXBException e) {
            statusHandler.warn(
                    "Unable to marshal the VolumeBrowser fields for " + ddName,
                    e);
        }
    }

    /**
     * Create the appropriate planes and level mapping files for the Volume
     * Browser for this model. Existing planes and level mapping files for this
     * model will be destroyed.
     *
     * @param sub
     */
    private static void createPlanesFiles(String ddName, boolean isVertical,
            Map<String, ParameterGroup> parameterGroups) {
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationContext context = pathMgr.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.CONFIGURED);

        Set<Level> levels = new HashSet<>();
        for (ParameterGroup pg : parameterGroups.values()) {
            for (LevelGroup lg : pg.getGroupedLevels().values()) {
                String masterLevelKey = lg.getMasterKey();

                List<ParameterLevelEntry> lgLevels = lg.getLevels();
                if (lgLevels == null || lgLevels.isEmpty()) {
                    statusHandler
                            .warn("No parameter level entries are available for "
                                    + ddName + ", parameter group "
                                    + pg.getAbbrev() + ", levelGroup "
                                    + lg.getName()
                                    + ". Multiple provider parameters may be mapped to the same Parameter.");
                    continue;
                }
                for (ParameterLevelEntry ple : lgLevels) {
                    double levelOne = getLevelValue(ple.getLevelOne());
                    double levelTwo = getLevelValue(ple.getLevelTwo());
                    if (levelOne == Level.getInvalidLevelValue()) {
                        levelOne = 0.0D;
                    }

                    Level level = LevelFactory.getInstance().getLevel(
                            masterLevelKey, levelOne, levelTwo, lg.getUnits());

                    if (level == null
                            && !StringUtil.isEmptyString(lg.getUnits())) {
                        // attempt without the configured units.
                        level = LevelFactory.getInstance().getLevel(
                                masterLevelKey, levelOne, levelTwo, null);

                        if (level != null) {
                            /*
                             * If a level was found after dropping the units,
                             * the configuration is likely wrong.
                             */
                            statusHandler.warn("The configured level units ["
                                    + lg.getUnits() + "] for '"
                                    + ple.getDescription() + "' conflict with "
                                    + masterLevelKey + "'s database units ["
                                    + level.getMasterLevel().getUnitString()
                                    + "]");
                        }
                    }

                    if (level == null) {
                        statusHandler.warn("Unable to determine the plane for '"
                                + ple.getDescription() + "' for " + ddName);
                        continue;
                    }

                    levels.add(level);
                }
            }
        }

        if (levels.isEmpty()) {
            statusHandler.warn("Unable to determine any planes for  " + ddName
                    + ". The planes files cannot be created.");
        }

        /*
         * Sort levels to hopefully group related levels (e.g. 2m FHAG and 10m
         * FHAG) together for a friendlier experience.
         */
        List<Level> sortedLevels = new ArrayList<>(levels);
        Collections.sort(sortedLevels, (levelA, levelB) -> {
            int comp = levelA.getMasterLevel().getName()
                    .compareTo(levelB.getMasterLevel().getName());
            if (comp != 0) {
                return comp;
            }
            comp = Double.compare(levelA.getLevelonevalue(),
                    levelB.getLevelonevalue());
            if (comp != 0) {
                return comp;
            }
            return Double.compare(levelA.getLeveltwovalue(),
                    levelB.getLeveltwovalue());
        });

        LevelMappingFactory baseLevelMappings = LevelMappingFactory
                .getInstance(LevelMappingFactory.DEFAULT_VB_LEVEL_MAPPING_FILE);
        List<CommonMenuContribution> contributions = new ArrayList<>();
        List<LevelMapping> levelMappings = new ArrayList<>();
        for (Level level : sortedLevels) {
            boolean baseMapped = false;
            String key = level.toString();
            String menuText = key;
            CommonMenuContribution cmc = new CommonMenuContribution();

            if (baseLevelMappings != null) {
                Collection<LevelMapping> mappings = baseLevelMappings
                        .getAllLevelMappingsForLevel(level);
                if (mappings != null) {
                    LevelMapping lm = mappings.iterator().next();
                    key = lm.getKey();
                    menuText = lm.getDisplayName();

                    baseMapped = true;
                }
            }

            cmc.menuText = menuText;
            cmc.key = key;
            cmc.indentText = false;
            cmc.textLookup = "LevelMapping";
            contributions.add(cmc);

            if (!baseMapped) {
                DatabaseLevelMapping dlm = new DatabaseLevelMapping();
                dlm.setLevelName(level.getMasterLevel().getName());
                dlm.setLevelOneValue(level.getLevelOneValueAsString());
                dlm.setLevelTwoValue(level.getLevelTwoValueAsString());

                LevelMapping lm = new LevelMapping();
                lm.setKey(level.toString());
                lm.setDatabaseLevels(Arrays.asList(dlm));
                lm.setDisplayName(level.toString());

                levelMappings.add(lm);
            }
        }

        CommonToolbarSubmenuContribution ctsc = new CommonToolbarSubmenuContribution();
        ctsc.menuText = ddName;
        ctsc.contributions = contributions
                .toArray(new CommonAbstractMenuContribution[0]);

        MenuTemplateFile mtf = new MenuTemplateFile();
        mtf.contributions = new CommonAbstractMenuContribution[] { ctsc };

        try {
            String contentsStr = jaxb.marshalToXml(mtf);

            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(ddName, PV_PLANES_FILE)),
                    contentsStr, true);
            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(ddName, PV_SPACE_PLANES_FILE)),
                    contentsStr, isVertical);
            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(ddName, XSECT_SPACE_PLANES_FILE)),
                    contentsStr, isVertical);
            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(ddName, XSECT_TIME_PLANES_FILE)),
                    contentsStr, isVertical);

        } catch (JAXBException e) {
            statusHandler.warn(
                    "Unable to marshal the VolumeBrowser planes for " + ddName,
                    e);
        }

        ILocalizationFile levelMappingFile = pathMgr.getLocalizationFile(
                context, LEVEL_MAPPINGS_PATH + ddName + ".xml");
        if (!levelMappings.isEmpty()) {
            LevelMappingFile lmf = new LevelMappingFile();
            lmf.setLevelMappingFile(levelMappings);

            try (SaveableOutputStream os = levelMappingFile
                    .openOutputStream()) {
                jaxb.marshalToStream(lmf, os);
                os.save();
            } catch (IOException | LocalizationException
                    | SerializationException e) {
                statusHandler.warn("Unable to write contents of "
                        + levelMappingFile.getPath(), e);
            }
        } else if (levelMappingFile.exists()) {
            try {
                levelMappingFile.delete();
            } catch (LocalizationException e) {
                statusHandler.warn("Unable to delete the Level Mapping file "
                        + levelMappingFile.getPath(), e);
            }
        }
    }

    private static double getLevelValue(String levelValue) {
        try {
            return Double.parseDouble(levelValue);
        } catch (Exception e) {
            statusHandler.warn("Unable to parse valid level value from '"
                    + levelValue + "'.", e);
            return Level.getInvalidLevelValue();
        }
    }

    private static void writeOrDeleteFile(ILocalizationFile file,
            String contents, boolean write) {
        if (write) {
            try (SaveableOutputStream os = file.openOutputStream()) {
                os.write(contents.getBytes());
                os.save();
            } catch (IOException | LocalizationException e) {
                statusHandler.warn(
                        "Unable to write contents of " + file.getPath(), e);
            }
        } else if (file.exists()) {
            try {
                file.delete();
            } catch (LocalizationException e) {
                statusHandler.warn("Unable to delete " + file.getPath(), e);
            }
        }
    }
}
