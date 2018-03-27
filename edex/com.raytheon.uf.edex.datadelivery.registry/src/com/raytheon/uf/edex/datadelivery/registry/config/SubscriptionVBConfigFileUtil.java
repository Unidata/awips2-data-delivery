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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.registry.LevelGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
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
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.util.StringUtil;

/**
 * Utility class for maintaining the Volume Browser config files for
 * Subscriptions.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 06, 2017 6355       nabowle     Initial creation
 *
 * </pre>
 *
 * @author nabowle
 */

public class SubscriptionVBConfigFileUtil {

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
     * Create the Volume Browser configuration files for the given subscription.
     * Any existing files will be overwritten or removed if no longer relevant.
     *
     * @param sub
     */
    public static void createSubscriptionFiles(Subscription<?, ?> sub) {
        statusHandler
                .info("Creating the localized Volume Browser config files for the subscription "
                        + sub.getName());
        createVolumeBrowserSource(sub);
        createFieldsFiles(sub);
        createPlanesFiles(sub);
        createSubscriptionIndexXml(sub);
    }

    /**
     * Delete all of the CONFIGURED localized Volume Browser config files for a
     * removed subscription.
     *
     * @param subName
     *            The subscription name.
     */
    public static void deleteSubscriptionFiles(String subName) {
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationContext context = pathMgr.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.CONFIGURED);
        ILocalizationFile localizationDir = pathMgr.getLocalizationFile(context,
                SubscriptionVBConfigFileUtil.BASE_SUB_PATH + subName);

        if (localizationDir.exists()) {
            statusHandler
                    .info("Deleting the localized Volume Browser config files for the removed subscription "
                            + subName);

            ILocalizationFile[] files = PathManagerFactory.getPathManager()
                    .listFiles(context, localizationDir.getPath(), null, false,
                            true);
            for (ILocalizationFile lf : files) {
                try {
                    lf.delete();
                } catch (LocalizationException e) {
                    statusHandler.warn("Unable to delete the file "
                            + lf.getPath() + " for the removed subscription "
                            + subName + ".", e);
                }
            }
            try {
                localizationDir.delete();
            } catch (LocalizationException e) {
                statusHandler.warn("Unable to delete the directory "
                        + localizationDir.getPath()
                        + " for the removed subscription " + subName + ".", e);
            }
        }

        ILocalizationFile levelMappingFile = pathMgr.getLocalizationFile(
                context, SubscriptionVBConfigFileUtil.LEVEL_MAPPINGS_PATH
                        + subName + ".xml");
        if (levelMappingFile.exists()) {
            try {
                levelMappingFile.delete();
            } catch (LocalizationException e) {
                statusHandler.warn("Unable to delete the Level Mapping file "
                        + levelMappingFile.getPath(), e);
            }
        }

        context = pathMgr.getContext(LocalizationType.CAVE_STATIC,
                LocalizationLevel.CONFIGURED);
        ILocalizationFile vbSourcesFile = pathMgr.getLocalizationFile(context,
                SubscriptionVBConfigFileUtil.VBSOURCES_PATH + subName + ".xml");
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
     * Create the VBSource config file for the given subscription. An existing
     * config file for this subscription will be overwritten.
     *
     * @param sub
     */
    private static void createVolumeBrowserSource(Subscription<?, ?> sub) {
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationContext context = pathMgr.getContext(
                LocalizationType.CAVE_STATIC, LocalizationLevel.CONFIGURED);
        ILocalizationFile vbSourceFile = pathMgr.getLocalizationFile(context,
                VBSOURCES_PATH + sub.getName() + ".xml");

        VbSource source = new VbSource();
        source.setKey(sub.getName());
        source.setCategory("DataDelivery");
        List<ViewMenu> views;
        if (sub.isVertical()) {
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
                            + sub.getName(), e);
        }
    }

    private static String getFilePath(Subscription<?, ?> sub, String file) {
        return BASE_SUB_PATH + sub.getName()
                + (file.startsWith(IPathManager.SEPARATOR) ? ""
                        : IPathManager.SEPARATOR)
                + file;
    }

    /**
     * Creates the index.xml for the given subscription. An existing index.xml
     * file for this subscription will be overwritten.
     *
     * @param sub
     * @throws LocalizationException
     * @throws IOException
     */
    private static void createSubscriptionIndexXml(Subscription<?, ?> sub) {
        // Fields

        List<CommonIncludeMenuItem> includes = new ArrayList<>();
        CommonIncludeMenuItem cmi;
        String path = getFilePath(sub, PVTS_FIELDS_FILE);

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

        if (sub.isVertical()) {
            path = getFilePath(sub, SOUNDING_FIELDS_FILE);
            if (fileExists(path)) {
                cmi = new CommonIncludeMenuItem();
                cmi.fileName = new File(path);
                cmi.installationLocation = SOUNDING_FIELDS_MENU_LOC;
                includes.add(cmi);
            }

            path = getFilePath(sub, TIMEHEIGHT_FIELDS_FILE);
            if (fileExists(path)) {
                cmi = new CommonIncludeMenuItem();
                cmi.fileName = new File(path);
                cmi.installationLocation = TIMEHEIGHT_FIELDS_MENU_LOC;
                includes.add(cmi);
            }

            path = getFilePath(sub, XSECT_FIELDS_FILE);
            if (fileExists(path)) {
                cmi = new CommonIncludeMenuItem();
                cmi.fileName = new File(path);
                cmi.installationLocation = XSECT_FIELDS_MENU_LOC;
                includes.add(cmi);
            }
        }

        // Planes

        path = getFilePath(sub, PV_PLANES_FILE);
        if (fileExists(path)) {
            cmi = new CommonIncludeMenuItem();
            cmi.fileName = new File(path);
            cmi.installationLocation = PV_PLANES_MENU_LOC;
            includes.add(cmi);
        }

        if (sub.isVertical()) {
            path = getFilePath(sub, PV_SPACE_PLANES_FILE);
            if (fileExists(path)) {
                cmi = new CommonIncludeMenuItem();
                cmi.fileName = new File(path);
                cmi.installationLocation = PV_SPACE_PLANES_MENU_LOC;
                includes.add(cmi);
            }

            path = getFilePath(sub, XSECT_SPACE_PLANES_FILE);
            if (fileExists(path)) {
                cmi = new CommonIncludeMenuItem();
                cmi.fileName = new File(path);
                cmi.installationLocation = XSECT_SPACE_PLANES_MENU_LOC;
                includes.add(cmi);
            }

            path = getFilePath(sub, XSECT_TIME_PLANES_FILE);
            if (fileExists(path)) {
                cmi = new CommonIncludeMenuItem();
                cmi.fileName = new File(path);
                cmi.installationLocation = XSECT_TIME_PLANES_MENU_LOC;
                includes.add(cmi);
            }
        }

        /*
         * Something went wrong when creating the includes files. Do not create
         * an index.xml file for this subscription to avoid breaking the
         * VolumeBrowser.
         */
        if (includes.isEmpty()) {
            statusHandler
                    .warn("The included fields and planes files were not created. Skipping creating the index.xml for the Subscription "
                            + sub.getName());
            return;
        }

        CommonMenuContributionFile mcf = new CommonMenuContributionFile();
        mcf.contribution = includes.toArray(new CommonIncludeMenuItem[0]);

        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationContext context = pathMgr.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.CONFIGURED);
        ILocalizationFile indexFile = pathMgr.getLocalizationFile(context,
                getFilePath(sub, "index.xml"));

        try {
            String contents = jaxb.marshalToXml(mcf);
            writeOrDeleteFile(indexFile, contents, true);
        } catch (JAXBException e) {
            statusHandler
                    .warn("Unable to marshal the VolumeBrowser index file for "
                            + sub.getName(), e);
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
     * Subscription. Existing fields files for this Subscription will be
     * destroyed.
     *
     * @param sub
     */
    private static void createFieldsFiles(Subscription<?, ?> sub) {
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationContext context = pathMgr.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.CONFIGURED);

        List<CommonMenuContribution> contributions = new ArrayList<>();

        Map<String, ParameterGroup> paramGroups = sub.getParameterGroups();
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
        ctsc.menuText = sub.getName();
        ctsc.contributions = contributions
                .toArray(new CommonAbstractMenuContribution[0]);

        MenuTemplateFile mtf = new MenuTemplateFile();
        mtf.contributions = new CommonAbstractMenuContribution[] { ctsc };
        try {
            String contentsStr = jaxb.marshalToXml(mtf);

            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(sub, PVTS_FIELDS_FILE)),
                    contentsStr, true);
            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(sub, SOUNDING_FIELDS_FILE)),
                    contentsStr, sub.isVertical());
            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(sub, TIMEHEIGHT_FIELDS_FILE)),
                    contentsStr, sub.isVertical());
            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(sub, XSECT_FIELDS_FILE)),
                    contentsStr, sub.isVertical());
        } catch (JAXBException e) {
            statusHandler.warn("Unable to marshal the VolumeBrowser fields for "
                    + sub.getName(), e);
        }
    }

    /**
     * Create the appropriate planes and level mapping files for the Volume
     * Browser for this Subscription. Existing planes and level mapping files
     * for this Subscription will be destroyed.
     *
     * @param sub
     */
    private static void createPlanesFiles(Subscription<?, ?> sub) {
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationContext context = pathMgr.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.CONFIGURED);

        Map<String, ParameterGroup> paramGroups = sub.getParameterGroups();
        Set<Level> levels = new HashSet<>();
        for (ParameterGroup pg : paramGroups.values()) {
            for (LevelGroup lg : pg.getGroupedLevels().values()) {
                String masterLevelKey = lg.getMasterKey();

                List<ParameterLevelEntry> lgLevels = lg.getLevels();
                if (lgLevels == null || lgLevels.isEmpty()) {
                    statusHandler
                            .warn("No parameter level entries are available for subscription "
                                    + sub.getName() + ", parameter group "
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
                                + ple.getDescription() + "' for Subscription "
                                + sub.getName());
                        continue;
                    }

                    levels.add(level);
                }
            }
        }

        if (levels.isEmpty()) {
            statusHandler
                    .warn("Unable to determine any planes for Subscription "
                            + sub.getName()
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
        ctsc.menuText = sub.getName();
        ctsc.contributions = contributions
                .toArray(new CommonAbstractMenuContribution[0]);

        MenuTemplateFile mtf = new MenuTemplateFile();
        mtf.contributions = new CommonAbstractMenuContribution[] { ctsc };

        try {
            String contentsStr = jaxb.marshalToXml(mtf);

            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(sub, PV_PLANES_FILE)),
                    contentsStr, true);
            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(sub, PV_SPACE_PLANES_FILE)),
                    contentsStr, sub.isVertical());
            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(sub, XSECT_SPACE_PLANES_FILE)),
                    contentsStr, sub.isVertical());
            writeOrDeleteFile(
                    pathMgr.getLocalizationFile(context,
                            getFilePath(sub, XSECT_TIME_PLANES_FILE)),
                    contentsStr, sub.isVertical());

        } catch (JAXBException e) {
            statusHandler.warn("Unable to marshal the VolumeBrowser planes for "
                    + sub.getName(), e);
        }

        ILocalizationFile levelMappingFile = pathMgr.getLocalizationFile(
                context, LEVEL_MAPPINGS_PATH + sub.getName() + ".xml");
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
