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
package com.raytheon.uf.common.datadelivery.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.raytheon.uf.common.datadelivery.registry.DataLevelType.LevelType;
import com.raytheon.uf.common.parameter.lookup.ParameterLookup;

/**
 *
 * Utilities for Parameters and ParameterGroups
 *
 * Contains various methods for converting, comparing, or extracting specific
 * data from Parameters and ParameterGroups.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 14, 2017 6413       tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
public class ParameterUtils {

    /**
     * Builds a key from a param/level name and its units
     *
     * @param name
     * @param units
     * @return
     */
    public static String buildKey(String name, String units) {
        String key = name;
        if (units != null && !"".equals(units)) {
            key = name + " (" + units + ")";
        }
        return key;
    }

    /**
     * Converts a list of Parameters to a map of ParameterGroups. Used for
     * Compatibility with older versions. OBE once all sites are at 18.1.1 or
     * beyond.
     *
     * @param parameters
     * @return
     */
    public static Map<String, ParameterGroup> generateParameterGroupsFromParameters(
            Collection<Parameter> parameters) {
        Map<String, ParameterGroup> pgs = new HashMap<>(1);

        for (Parameter param : parameters) {

            String awipsName = param.getAwipsName();
            if (awipsName == null) {
                awipsName = param.getName();
            }
            String units = param.getUnits();
            ParameterGroup pg = pgs.get(buildKey(awipsName, units));
            if (pg == null) {
                pg = new ParameterGroup(awipsName, units);
                pgs.put(pg.getKey(), pg);
            }

            /*
             * Parameters store level information in one of 3 different ways
             * that we need handle.
             */
            for (DataLevelType type : param.getLevelType()) {
                LevelGroup lg = pg.getLevelGroup(
                        ParameterUtils.buildKey(type.getDescription(), null));
                if (lg == null) {
                    lg = new LevelGroup(type.getDescription(), null);
                    if (type.getType() != null) {
                        lg.setMasterKey(type.getType().toString());
                    }
                    pg.putLevelGroup(lg);
                }
                /*
                 * GRID data is the only type that stores its level information
                 * in Levels objects, so if it has Levels, assume it's Grid.
                 */
                Levels levels = param.getLevels();
                if (levels != null) {
                    boolean providerLevels = false;
                    if (param.getLevelType().get(0).getType() == LevelType.MB
                            || param.getLevelType().get(0)
                                    .getType() == LevelType.SEAB) {
                        providerLevels = true;
                    }
                    /*
                     * If we have selected indices, we need to just create
                     * entries for the ones selected. Otherwise create an entry
                     * for each level.
                     */
                    List<Integer> selectedLevelIndices = levels
                            .getSelectedLevelIndices();
                    if (selectedLevelIndices != null
                            && !selectedLevelIndices.isEmpty()) {
                        for (Integer index : selectedLevelIndices) {
                            GriddedParameterLevelEntry ple = new GriddedParameterLevelEntry(
                                    param.getProviderName(),
                                    param.getDefinition(),
                                    levels.getLevelAt(index).toString());
                            ple.setMissingValue(param.getMissingValue());
                            ple.setUseProviderLevel(providerLevels);
                            lg.addLevel(ple);
                        }
                    } else {
                        for (Double level : levels.getLevel()) {
                            String levelStr = null;
                            if (level != Double.NaN) {
                                levelStr = level.toString();
                            }
                            GriddedParameterLevelEntry ple = new GriddedParameterLevelEntry(
                                    param.getProviderName(),
                                    param.getDefinition(), levelStr);
                            ple.setMissingValue(param.getMissingValue());
                            ple.setUseProviderLevel(providerLevels);
                            lg.addLevel(ple);
                        }
                    }

                } else {
                    ParameterLevelEntry ple = new ParameterLevelEntry(
                            param.getProviderName(), param.getDefinition(),
                            null);
                    lg.addLevel(ple);
                }
            }
        }
        return pgs;
    }

    /**
     * Converts a map of ParameterGroups to a map of Parameters. Used for
     * Compatibility with older versions. OBE once all sites are at 18.1.1 or
     * beyond.
     *
     * @param paramGroups
     *            ParameterGroups to convert
     * @param type
     *            DataType of the Parameters to be created
     * @param dsmd
     *            DataSetMetaData. Only used for GRID ParameterGroups
     * @return
     */
    public static Map<String, Parameter> generateParametersFromGroups(
            Map<String, ParameterGroup> paramGroups, DataType type,
            DataSetMetaData<?, ?> dsmd) {

        Map<String, Parameter> parameters = new HashMap<>();

        for (ParameterGroup pg : paramGroups.values()) {
            parameters.putAll(generateParametersFromGroup(pg, type, dsmd));
        }

        return parameters;
    }

    /**
     * Converts a single ParameterGroups to a map of Parameters. Used for
     * Compatibility with older versions. OBE once all sites are at 18.1.1 or
     * beyond.
     *
     * @param paramGroups
     *            ParameterGroup to convert
     * @param type
     *            DataType of the Parameters to be created
     * @param dsmd
     *            DataSetMetaData. Only used for GRID ParameterGroups
     * @return
     */
    public static Map<String, Parameter> generateParametersFromGroup(
            ParameterGroup pg, DataType type, DataSetMetaData<?, ?> dsmd) {
        Map<String, Parameter> parameters = new HashMap<>();

        for (LevelGroup lg : pg.getGroupedLevels().values()) {
            Set<String> providerNames = new HashSet<>();
            for (ParameterLevelEntry level : lg.getLevels()) {
                providerNames.add(level.getProviderName());
            }
            for (String providerName : providerNames) {
                Parameter parm = new Parameter();
                parm.setDataType(type);
                parm.setUnits(pg.getUnits());
                String displayName = providerName;
                DataLevelType levelType = new DataLevelType(
                        LevelType.fromDescription(lg.getName()));

                parm.setAwipsName(pg.getAbbrev());
                parm.setProviderName(providerName);
                parm.addLevelType(levelType);
                SortedSet<Integer> selectedIndices = new TreeSet<>();
                Levels levels = new Levels();
                levels.setName(lg.getName());
                levels.setLevelType(levelType.getType().getLevelTypeId());

                for (ParameterLevelEntry level : lg.getLevels()) {
                    if (level.getProviderName().equals(providerName)) {
                        boolean providerLevels = false;
                        if (level instanceof GriddedParameterLevelEntry) {
                            GriddedParameterLevelEntry griddedLevel = (GriddedParameterLevelEntry) level;
                            providerLevels = griddedLevel.isUseProviderLevel();
                            if (providerLevels) {
                                if (dsmd instanceof GriddedDataSetMetaData) {
                                    GriddedDataSetMetaData gdsmd = (GriddedDataSetMetaData) dsmd;
                                    selectedIndices.add(
                                            gdsmd.findProviderLevelIndex(Double
                                                    .parseDouble(griddedLevel
                                                            .getLevelOne())));
                                    levels.setLevel(gdsmd.getProviderLevels());
                                }

                                parm.setMissingValue(
                                        griddedLevel.getMissingValue());
                                parm.setFillValue(
                                        griddedLevel.getMissingValue());
                            }
                        } else {
                            parm.setMissingValue("0");
                            parm.setFillValue("0");
                        }
                        if (!displayName.equals(pg.getAbbrev())) {
                            StringBuilder sb = new StringBuilder();
                            if (!providerLevels
                                    && level.getDisplayString() != null) {
                                sb.append(level.getDisplayString());
                            }
                            if (lg.getUnits() != null) {
                                sb.append(lg.getUnits());
                            }
                            sb.append(" " + levelType.getType().toString());
                            String levelInfo = sb.toString().trim();
                            displayName = pg.getAbbrev() + " (" + levelInfo
                                    + ")";
                        }
                        parm.setName(displayName);
                        parm.setDefinition(level.getDescription());
                    }
                    if (levels.getLevel() == null
                            || levels.getLevel().isEmpty()) {
                        levels.addLevel(Double.NaN);
                    }
                    if (!selectedIndices.isEmpty()) {
                        levels.setSelectedLevelIndices(
                                new ArrayList<>(selectedIndices));
                    }
                    parm.setLevels(levels);
                }
                parameters.put(parm.getName(), parm);
            }
        }
        return parameters;
    }

    /**
     * Flattens a map of ParameterGroups into a List of ParameterGroups that
     * each contain exactly one ParameterLevelEntry. Used to process provider
     * parameters one at a time when doing retrievals.
     *
     * @param pgMap
     * @return
     */
    public static List<ParameterGroup> createSingleParameterLevelList(
            Map<String, ParameterGroup> pgMap) {
        List<ParameterGroup> pgList = new ArrayList<>();

        for (ParameterGroup pg : pgMap.values()) {
            for (LevelGroup lg : pg.getGroupedLevels().values()) {
                for (ParameterLevelEntry ple : lg.getLevels()) {
                    ParameterGroup newPg = new ParameterGroup(pg.getAbbrev(),
                            pg.getUnits());
                    LevelGroup newLg = new LevelGroup(lg.getName(),
                            lg.getUnits());
                    newLg.setMasterKey(lg.getMasterKey());
                    newLg.addLevel(ple);
                    newPg.putLevelGroup(newLg);
                    pgList.add(newPg);
                }
            }
        }
        return pgList;
    }

    /**
     * Returns a list of all ParameterGroups that have entries at a given level.
     *
     * @param levelKey
     * @param parameterGroups
     * @return
     */
    public static List<ParameterGroup> getParameterForLevel(String levelKey,
            Map<String, ParameterGroup> parameterGroups) {
        // Populate as a set to ensure order and uniqueness
        List<ParameterGroup> returnList = new ArrayList<>();

        for (ParameterGroup pg : parameterGroups.values()) {
            LevelGroup lg = pg.getLevelGroup(levelKey);

            if (lg != null) {
                returnList.add(pg);
            }
        }
        return returnList;
    }

    /**
     * Create a mapping of ParameterGroups by display name by level name.
     *
     * @param parameterGroups
     *            Map of ParameterGroups to reorganize.
     * @return
     */
    public static Map<String, Map<String, ParameterGroup>> getParameterLevelMap(
            Map<String, ParameterGroup> parameterGroups) {
        Map<String, Map<String, ParameterGroup>> paramsByNameByLevel = new TreeMap<>();

        // Find all levels
        Set<String> levelDisplaySet = new TreeSet<>();
        for (ParameterGroup pg : parameterGroups.values()) {
            for (LevelGroup levelType : pg.getGroupedLevels().values()) {
                levelDisplaySet.add(levelType.getKey());
            }
        }

        /*
         * For each level, find all ParameterGroups with entries for that level.
         * If a ParameterGroup corresponds to a common Parameter, map it by the
         * common Parameter's name. Else use the ParameterGroup's key.
         */
        for (String levelLabel : levelDisplaySet) {
            Map<String, ParameterGroup> paramsByName = new TreeMap<>();
            List<ParameterGroup> levelParams = ParameterUtils
                    .getParameterForLevel(levelLabel, parameterGroups);
            for (ParameterGroup param : levelParams) {
                String paramName = param.getKey();
                com.raytheon.uf.common.parameter.Parameter commonParam = ParameterLookup
                        .getInstance().getParameter(param.getAbbrev());
                if (commonParam != null) {
                    paramName = commonParam.getName();
                }
                paramsByName.put(paramName, param);
            }
            paramsByNameByLevel.put(levelLabel, paramsByName);
        }
        return paramsByNameByLevel;
    }

    /**
     * Returns a list of all Level entry descriptions from all ParameterGroups
     * for a given level.
     *
     * @param levelKey
     * @param parameterGroups
     * @return
     */
    public static List<String> getLevelNamesForLevel(String levelKey,
            Map<String, ParameterGroup> parameterGroups) {
        boolean reverse = false;
        List<String> returnList = new ArrayList<>();
        // Populate as a set to ensure order and uniqueness
        Set<ParameterLevelEntry> paramLevels = new TreeSet<>();
        for (ParameterGroup pg : parameterGroups.values()) {
            LevelGroup lg = pg.getLevelGroup(levelKey);

            if (lg != null) {
                // Should be the same for all ParameterGroups
                reverse = lg.isReverseOrder();
                for (ParameterLevelEntry level : lg.getLevels()) {
                    paramLevels.add(level);
                }
            }
        }
        for (ParameterLevelEntry ple : paramLevels) {
            String displayString = ple.getDisplayString();
            if (displayString != null && !"".equals(displayString)
                    && !returnList.contains(displayString)) {
                returnList.add(displayString);
            }
        }

        if (reverse) {
            Collections.reverse(returnList);
        }
        return returnList;
    }

    /**
     * Compares two maps of ParameterGroups to see if there is any overlap in
     * the entries between them.
     *
     * @param primaryMap
     * @param subMap
     * @return
     */
    public static boolean intersects(Map<String, ParameterGroup> primaryMap,
            Map<String, ParameterGroup> subMap) {
        for (Entry<String, ParameterGroup> pgEntry : primaryMap.entrySet()) {
            ParameterGroup subPg = subMap.get(pgEntry.getKey());
            if (subPg != null) {
                for (Entry<String, LevelGroup> lgEntry : pgEntry.getValue()
                        .getGroupedLevels().entrySet()) {
                    LevelGroup subLg = subPg.getLevelGroup(lgEntry.getKey());
                    if (subLg != null) {
                        for (ParameterLevelEntry ple : lgEntry.getValue()
                                .getLevels()) {
                            for (ParameterLevelEntry subPle : subLg
                                    .getLevels()) {
                                if (subPle.equals(ple)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Compares two maps of ParameterGroups. Returns a list containing a
     * ParameterGroup for each entry that is the primary map but not in the sub
     * map.
     *
     * @param primaryMap
     * @param subMap
     * @return
     */
    public static List<ParameterGroup> getUnique(
            Map<String, ParameterGroup> primaryMap,
            Map<String, ParameterGroup> subMap) {
        List<ParameterGroup> uniqueParams = new ArrayList<>(1);

        for (ParameterGroup pg : primaryMap.values()) {
            ParameterGroup subPg = subMap.get(pg.getKey());
            for (LevelGroup lg : pg.getGroupedLevels().values()) {
                for (ParameterLevelEntry ple : lg.getLevels()) {
                    boolean match = false;
                    if (subPg != null) {
                        LevelGroup subLg = subPg.getLevelGroup(lg.getKey());
                        if (subLg != null) {
                            for (ParameterLevelEntry subPle : subLg
                                    .getLevels()) {
                                if (subPle.equals(ple)) {
                                    match = true;
                                }
                            }
                        }
                    }
                    if (!match) {
                        ParameterGroup newPg = new ParameterGroup(
                                pg.getAbbrev(), pg.getUnits());
                        LevelGroup newLg = new LevelGroup(lg.getName(),
                                lg.getUnits());
                        newLg.setMasterKey(lg.getMasterKey());
                        ParameterLevelEntry newPle = new ParameterLevelEntry(
                                ple);
                        newLg.addLevel(newPle);
                        newPg.putLevelGroup(newLg);
                        uniqueParams.add(newPg);
                    }
                }
            }
        }
        return uniqueParams;
    }

}
