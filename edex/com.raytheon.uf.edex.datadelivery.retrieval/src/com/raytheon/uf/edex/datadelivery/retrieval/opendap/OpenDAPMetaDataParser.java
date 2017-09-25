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
package com.raytheon.uf.edex.datadelivery.retrieval.opendap;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.datadelivery.registry.Collection;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataLevelType;
import com.raytheon.uf.common.datadelivery.registry.DataLevelType.LevelType;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedDataSet;
import com.raytheon.uf.common.datadelivery.registry.GriddedDataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.Levels;
import com.raytheon.uf.common.datadelivery.registry.OpenDapGriddedDataSet;
import com.raytheon.uf.common.datadelivery.registry.OpenDapGriddedDataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.util.LookupManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterLevelRegex;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterMapping;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ParameterNameRegex;
import com.raytheon.uf.common.gridcoverage.Corner;
import com.raytheon.uf.common.gridcoverage.GridCoverage;
import com.raytheon.uf.common.gridcoverage.exception.GridCoverageException;
import com.raytheon.uf.common.time.util.ImmutableDate;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.common.util.GridUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.Link;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.MetaDataParser;
import com.raytheon.uf.edex.datadelivery.retrieval.opendap.OpenDAPMetaDataExtractor.DAP_TYPE;
import com.vividsolutions.jts.geom.Coordinate;

import opendap.dap.AttributeTable;
import opendap.dap.DAS;
import opendap.dap.NoSuchAttributeException;

/**
 * Parse OpenDAP MetaData. This class should remain package-private, all access
 * should be limited through the {@link OpenDapServiceFactory}.
 *
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#     Engineer  Description
 * ------------- ----------- --------- -----------------------------------------
 * Feb 20, 2011  218         dhladky   Initial creation
 * Jul 23, 2012              djohnson  Use collection name for dataset name.
 * Aug 10, 2012  1022        djohnson  {@link DataSetMetaData} requires provider
 *                                     name.
 * Aug 15, 2012  743         djohnson  Set the date on {@link DataSetMetaData}.
 * Aug 22, 2012  743         djohnson  Store data type as an enum.
 * Aug 31, 2012  1125        djohnson  Rename getCollectionAndCycle() to
 *                                     getDataSetNameAndCycle().
 * Sep 24, 2012  1209        djohnson  If no parseable cycle found, set
 *                                     NO_CYCLE.
 * Oct 20, 2012  1163        dhladky   Updated lookup table generation
 * Nov 19, 2012  1166        djohnson  Clean up JAXB representation of registry
 *                                     objects.
 * Dec 12, 2012  supplement  dhladky   Restored operation of ensembles.
 * Dec 10, 2012  1259        bsteffen  Switch Data Delivery from LatLon to
 *                                     referenced envelopes.
 * Jan 08, 2013              dhladky   Performance enhancements, specific model
 *                                     fixes.
 * Jan 18, 2013  1513        dhladky   Level look up improvements.
 * Jan 24, 2013  1527        dhladky   Changed 0DEG to FRZ
 * Sep 25, 2013  1797        dhladky   separated time from gridded time
 * Oct 10, 2013  1797        bgonzale  Refactored registry Time objects.
 * Dec 18, 2013  2636        mpduff    Calculate a data availability delay for
 *                                     the dataset and dataset meta data.
 * Jan 14, 2014              dhladky   Data set info used for availability delay
 *                                     calculations.
 * Jun 09, 2014  3113        mpduff    Save the ArrivalTime.
 * Jul 08, 2014  3120        dhladky   Generics, interface realignment
 * Apr 12, 2015  4400        dhladky   Switched over to DAP2 protocol.
 * Jul 13, 2015  4566        dhladky   Added the AWIPS parameter name to the
 *                                     Dataset when present.
 * Feb 16, 2016  5365        dhladky   Interface change.
 * Nov 09, 2016  5988        tjensen   Update for Friendly naming for NOMADS
 * Jan 05, 2017  5988        tjensen   Updated for new parameter lookups and
 *                                     regexes
 * Mar 02, 2017  5988        tjensen   Update level population for friendly
 *                                     naming
 * Mar 08, 2017  6089        tjensen   Drop date format from parseMetadata calls
 * Apr 05, 2017  1045        tjensen   Update for moving datasets
 * May 04, 2017  6186        rjpeter   Utilize logger.
 * Jul 12, 2017  6178        tgurney   Updates for database-based link storage
 * Aug 02, 2017  6186        rjpeter   Fixed cycle handling for DSMD.
 * Aug 31, 2017  6430        rjpeter   Fixed to not use dataSet as map key and
 *                                     stored parameters once.
 * Sep 25, 2017  6178        tgurney   parseMetaData generate link key from url
 *
 * </pre>
 *
 * @author dhladky
 */

class OpenDAPMetaDataParser extends MetaDataParser<List<Link>> {

    OpenDAPMetaDataParser() {
        serviceConfig = HarvesterServiceManager.getInstance()
                .getServiceConfig(ServiceType.OPENDAP);
    }

    /**
     * Gets the levels for types we are aware of. {@link DataLevelType} Right
     * now it can only recognize MB (pressure levels) for DAP types. It also
     * rudimentarily recognizes Heights Above Sea Level, SEAB. dz = number of
     * levels levMin = minimum level value levMax = maximum level value
     *
     * @param type
     * @param collectionName
     * @param gdsmd
     * @param dz
     * @param levMin
     * @param levMax
     * @return
     */
    private Levels getLevels(DataLevelType type, String collectionName,
            GriddedDataSetMetaData gdsmd, Double dz, Float levMin,
            Float levMax) {

        Levels levels = new Levels();

        final LevelType levelType = type.getType();
        try {

            levels.setName(levelType.toString());
            levels.setLevelType(type.getId());

            if (!LookupManager.getInstance()
                    .levelLookupExists(collectionName)) {
                // create new default lookups
                if (levelType.equals(LevelType.MB)
                        || levelType.equals(LevelType.SEAB)) {

                    List<Double> levelList = OpenDAPParseUtility.getInstance()
                            .parseLevels(gdsmd.getUrl(),
                                    serviceConfig.getConstantValue("LEV"));
                    LookupManager.getInstance().modifyLevelLookups(
                            collectionName, dz, levMin, levMax, levelList);
                }
            }

            if (levelType.equals(LevelType.MB)
                    || levelType.equals(LevelType.SEAB)) {
                List<Double> levelList = LookupManager.getInstance()
                        .getLevels(collectionName).getLevelXml();
                levels.setLevel(levelList);
            } else {
                // default added when only one
                levels.addLevel(Double.NaN);
            }

            levels.setDz(dz);

            // TODO: Add more level type, if and when they exist
            if (!gdsmd.getLevelTypes().containsKey(type)) {
                gdsmd.addLevelType(type, levels);
            }

        } catch (Exception e) {
            logger.error(" Couldn't parse Level info: " + levelType.toString()
                    + " dataset: " + collectionName + " url: " + gdsmd.getUrl(),
                    e);
        }

        return levels;
    }

    /**
     * Process parameters against lookups and DAP constants
     *
     * @param das
     * @param dataSet
     * @param gdsmd
     * @param link
     * @param collection
     * @param dataDateFormat
     * @return
     * @throws NoSuchAttributeException
     */
    private Map<String, Parameter> getParameters(DAS das,
            GriddedDataSet dataSet, GriddedDataSetMetaData gdsmd,
            String subName, Collection collection, String dataDateFormat)
            throws NoSuchAttributeException {

        final String collectionName = dataSet.getCollectionName();
        final String url = gdsmd.getUrl();
        double dz = 0.00;
        float levMin = 0.0f;
        float levMax = 0.0f;
        final Coordinate upperLeft = new Coordinate();
        final Coordinate lowerRight = new Coordinate();
        final GriddedCoverage griddedCoverage = new GriddedCoverage();
        dataSet.setCoverage(griddedCoverage);

        final GridCoverage gridCoverage = collection.getProjection()
                .getGridCoverage();
        // TODO: haven't figure out how to tell the difference on these from
        // the provider metadata yet
        gridCoverage.setSpacingUnit(serviceConfig.getConstantValue("DEGREE"));
        gridCoverage.setFirstGridPointCorner(Corner.LowerLeft);

        Map<String, Parameter> parameters = new HashMap<>();
        final String timecon = serviceConfig.getConstantValue("TIME");
        final String size = serviceConfig.getConstantValue("SIZE");
        final String minimum = serviceConfig.getConstantValue("MINIMUM");
        final String maximum = serviceConfig.getConstantValue("MAXIMUM");
        final String time_step = serviceConfig.getConstantValue("TIME_STEP");
        final String lat = serviceConfig.getConstantValue("LAT");
        final String lon = serviceConfig.getConstantValue("LON");
        final String resolution = serviceConfig.getConstantValue("RESOLUTION");
        final String lev = serviceConfig.getConstantValue("LEV");
        final String nc_global = serviceConfig.getConstantValue("NC_GLOBAL");
        final String data_type = serviceConfig.getConstantValue("DATA_TYPE");
        final String title = serviceConfig.getConstantValue("TITLE");
        final String ens = serviceConfig.getConstantValue("ENS");
        final String long_name = serviceConfig.getConstantValue("LONG_NAME");
        final String missing_value = serviceConfig
                .getConstantValue("MISSING_VALUE");
        final String fill_value = serviceConfig.getConstantValue("FILL_VALUE");
        final String fill = Float.toString(GridUtil.GRID_FILL_VALUE);

        // process globals first
        // process time
        if (das.getAttributeTable(timecon) != null) {
            try {
                AttributeTable at = das.getAttributeTable(timecon);
                GriddedTime time = new GriddedTime();
                // format of time
                time.setFormat(dataDateFormat);
                // number of times
                time.setNumTimes(new Integer(OpenDAPParseUtility.getInstance()
                        .trim(at.getAttribute(size).getValueAt(0))).intValue());
                // minimum time val
                time.setStartDate(OpenDAPParseUtility.getInstance()
                        .parseDate(at.getAttribute(minimum).getValueAt(0)));
                // maximum time val
                time.setEndDate(OpenDAPParseUtility.getInstance()
                        .parseDate(at.getAttribute(maximum).getValueAt(0)));
                // step
                List<String> step = OpenDAPParseUtility.getInstance()
                        .parseTimeStep(
                                at.getAttribute(time_step).getValueAt(0));
                time.setStep(new Double(step.get(0)).doubleValue());
                time.setStepUnit(
                        Time.findStepUnit(step.get(1)).getDurationUnit());
                gdsmd.setTime(time);

            } catch (Exception le) {
                logger.error(" Couldn't parse Time: " + timecon + " dataset: "
                        + collectionName + " url: " + url, le);
            }
        }
        // process latitude
        if (das.getAttributeTable(lat) != null) {
            try {
                AttributeTable at = das.getAttributeTable(lat);
                // ny
                gridCoverage.setNy(new Integer(OpenDAPParseUtility.getInstance()
                        .trim(at.getAttribute(size).getValueAt(0))).intValue());
                // dy
                gridCoverage.setDy(new Float(OpenDAPParseUtility.getInstance()
                        .trim(at.getAttribute(resolution).getValueAt(0)))
                                .floatValue());
                // first latitude point
                gridCoverage.setLa1(new Double(OpenDAPParseUtility.getInstance()
                        .trim(at.getAttribute(minimum).getValueAt(0)))
                                .doubleValue());

                upperLeft.y = Double
                        .parseDouble(OpenDAPParseUtility.getInstance()
                                .trim(at.getAttribute(maximum).getValueAt(0)));

                lowerRight.y = Double
                        .parseDouble(OpenDAPParseUtility.getInstance()
                                .trim(at.getAttribute(minimum).getValueAt(0)));

            } catch (Exception le) {
                logger.error(" Couldn't parse Latitude: " + lat + " dataset: "
                        + collectionName + " url: " + url, le);
            }
        }
        // process longitude
        if (das.getAttributeTable(lon) != null) {
            try {
                AttributeTable at = das.getAttributeTable(lon);
                // nx
                gridCoverage.setNx(new Integer(OpenDAPParseUtility.getInstance()
                        .trim(at.getAttribute(size).getValueAt(0))).intValue());
                // dx
                gridCoverage.setDx(new Float(OpenDAPParseUtility.getInstance()
                        .trim(at.getAttribute(resolution).getValueAt(0)))
                                .floatValue());
                // min Lon
                double minLon = new Double(OpenDAPParseUtility.getInstance()
                        .trim(at.getAttribute(minimum).getValueAt(0)))
                                .doubleValue();
                // max Lon
                double maxLon = new Double(OpenDAPParseUtility.getInstance()
                        .trim(at.getAttribute(maximum).getValueAt(0)))
                                .doubleValue();

                gridCoverage.setLo1(minLon);
                upperLeft.x = minLon;
                lowerRight.x = maxLon;

            } catch (Exception le) {
                logger.error(" Couldn't parse Longitude: " + lon + " dataset: "
                        + collectionName + " url: " + url, le);
            }
        }
        // process level settings
        if (das.getAttributeTable(lev) != null) {
            try {
                AttributeTable at = das.getAttributeTable(lev);
                dz = new Double(OpenDAPParseUtility.getInstance()
                        .trim(at.getAttribute(resolution).getValueAt(0)))
                                .doubleValue();
                levMin = new Float(OpenDAPParseUtility.getInstance()
                        .trim(at.getAttribute(minimum).getValueAt(0)))
                                .floatValue();
                levMax = new Float(OpenDAPParseUtility.getInstance()
                        .trim(at.getAttribute(maximum).getValueAt(0)))
                                .floatValue();

            } catch (Exception le) {
                logger.error(" Couldn't parse Levels: " + lev + " dataset: "
                        + collectionName + " url: " + url, le);
            }
        }
        // process any other globals
        if (das.getAttributeTable(nc_global) != null) {
            try {
                AttributeTable at = das.getAttributeTable(nc_global);
                dataSet.setDataSetType(DataType.valueOfIgnoreCase(
                        OpenDAPParseUtility.getInstance().trim(
                                at.getAttribute(data_type).getValueAt(0))));
                String description = at.getAttribute(title).getValueAt(0);
                gdsmd.setDataSetDescription(
                        OpenDAPParseUtility.getInstance().trim(description));
            } catch (Exception ne) {
                logger.error(" Couldn't parse Global Dataset Info: " + nc_global
                        + " dataset: " + collectionName + " url: " + url, ne);
            }
        }
        // process ensembles
        if (das.getAttributeTable(ens) != null) {
            try {
                AttributeTable at = das.getAttributeTable(ens);
                dataSet.setEnsemble(
                        OpenDAPParseUtility.getInstance().parseEnsemble(at));
            } catch (Exception en) {
                logger.error(" Couldn't parse Ensemble: " + ens + " dataset: "
                        + collectionName + " url: " + url, en);
            }
        }

        // process the parameters
        for (Enumeration<?> e = das.getNames(); e.hasMoreElements();) {

            String providerName = (String) e.nextElement();
            // filter out globals
            if (!providerName.equals(ens) && !providerName.equals(nc_global)
                    && !providerName.equals(lev) && !providerName.equals(lon)
                    && !providerName.equals(lat)
                    && !providerName.equals(timecon)) {

                // regular parameter parsing
                try {

                    AttributeTable at = das.getAttributeTable(providerName);
                    Parameter parm = new Parameter();
                    parm.setDataType(dataSet.getDataSetType());

                    // UNKNOWN DESCRIPTION
                    String description = "unknown description";
                    try {
                        description = OpenDAPParseUtility.getInstance()
                                .trim(at.getAttribute(long_name).getValueAt(0));

                    } catch (Exception iae) {
                        logger.error("Invalid DAP description block! "
                                + providerName, iae);
                    }
                    // Clean up description stuff
                    description = description.replaceAll("^[* ]+", "");

                    parm.setDefinition(description);
                    parm.setUnits(OpenDAPParseUtility.getInstance()
                            .parseUnits(description));

                    // Check for an AWIPS name
                    String displayName = providerName;
                    String awipsName = null;

                    /*
                     * Check for mapping of specific grads names to see if we
                     * match one of those. First check for overrides that apply
                     * to specific data sets. If none found, check for general
                     * overrides.
                     */
                    ParameterMapping specificMap = LookupManager.getInstance()
                            .getDataSetParameter(dataSet.getDataSetName(),
                                    providerName);
                    if (specificMap == null) {
                        specificMap = LookupManager.getInstance()
                                .getGeneralParameter(providerName);
                    }

                    if (specificMap != null) {
                        displayName = specificMap.getDisplay();
                        awipsName = specificMap.getAwips();
                    } else {
                        /*
                         * If we didn't find a specific override, compare the
                         * description to the known patterns
                         */
                        awipsName = parseParamName(providerName, description);
                        if (!awipsName.equals(providerName)) {
                            String levelInfo = parseLevelName(description);
                            displayName = awipsName + " (" + levelInfo + ")";
                        }
                    }

                    parm.setName(displayName);
                    parm.setAwipsName(awipsName);
                    parm.setProviderName(providerName);

                    try {
                        parm.setMissingValue(OpenDAPParseUtility.getInstance()
                                .trim(at.getAttribute(missing_value)
                                        .getValueAt(0)));
                    } catch (Exception iae) {
                        logger.error("Invalid DAP missing value block! "
                                + providerName, iae);
                        parm.setMissingValue(fill);
                    }

                    try {
                        parm.setFillValue(OpenDAPParseUtility.getInstance()
                                .trim(at.getAttribute(fill_value)
                                        .getValueAt(0)));
                    } catch (Exception iae) {
                        logger.error(
                                "Invalid DAP fill value block! " + providerName,
                                iae);
                        parm.setMissingValue(fill);
                    }

                    DataLevelType type = parseLevelType(parm);
                    parm.setLevels(getLevels(type, collectionName, gdsmd, dz,
                            levMin, levMax));
                    parm.addLevelType(type);
                    // add to map
                    parameters.put(parm.getName(), parm);

                } catch (Exception le) {
                    logger.error(" Couldn't parse Parameter: " + providerName
                            + " dataset: " + collectionName + " url: " + url,
                            le);
                }
            }
        }

        String nameAndDescription = subName + "_Coverage_"
                + gridCoverage.getNx() + "_X_" + gridCoverage.getNy() + "_Y_"
                + gridCoverage.getProjectionType();
        gridCoverage.setName(nameAndDescription);

        try {
            gridCoverage.initialize();
        } catch (GridCoverageException e) {
            logger.error("Error initializing grid coverage ["
                    + nameAndDescription + "] for dataSet [" + url + "]", e);
        }

        griddedCoverage.setGridCoverage(gridCoverage);
        griddedCoverage.generateEnvelopeFromGridCoverage();

        return parameters;
    }

    /**
     * Parses the parameter description to try and determine the AWIPS name from
     * known Parameter Regexes. If a match is found, the AWIPS name for the
     * matching pattern is returned. Else the current parameter name is
     * returned.
     *
     * @param name
     *            Current Parameter name. Used as a default return value
     * @param description
     *            Parameter description
     * @return AWIPS parameter name
     */
    private static String parseParamName(String name, String description) {

        // Default paramName to grads name
        String paramName = name;

        Map<String, ParameterLevelRegex> plr = LookupManager.getInstance()
                .getParamLevelRegexes();
        Map<String, ParameterNameRegex> pnr = LookupManager.getInstance()
                .getParamNameRegexes();

        if (plr != null && pnr != null) {
            /*
             * Loop over known surface descriptions. If a match is found at the
             * beginning of the string, remove it from the description string
             * and exit the loop.
             */
            Matcher m = null;
            String tempDescription = description;
            for (ParameterLevelRegex myPLR : plr.values()) {
                m = myPLR.getPattern().matcher(tempDescription);
                if (m.find()) {
                    tempDescription = m.replaceFirst("");
                    tempDescription = tempDescription.replaceAll("^ +", "");
                    break;
                }
            }

            /*
             * Loop over known regexes for descriptions. If a match is found at
             * the begining of the string, set the param name to the name for
             * that regex.
             */
            for (ParameterNameRegex nameRegex : pnr.values()) {
                m = nameRegex.getPattern().matcher(tempDescription);
                if (m.find()) {
                    paramName = nameRegex.getAwips();
                    break;
                }
            }
        }

        return paramName;
    }

    /**
     * Uses the parameter description to determine the Level information for a
     * given parameter. This can include the specific level, the units, and/or
     * the level type (as applicable).
     *
     * @param description
     *            parameter description.
     * @return String describing the level information for this parameter
     */
    private static String parseLevelName(String description) {
        Map<String, ParameterLevelRegex> plr = LookupManager.getInstance()
                .getParamLevelRegexes();
        Map<String, ParameterNameRegex> pnr = LookupManager.getInstance()
                .getParamNameRegexes();

        StringBuilder sb = new StringBuilder();

        if (plr != null && pnr != null) {
            Matcher m = null;
            String tempDescription = description;
            for (ParameterLevelRegex myPLR : plr.values()) {
                m = myPLR.getPattern().matcher(tempDescription);
                if (m.find()) {
                    // Save off the level info
                    if (myPLR.getLevelGroup() != null) {
                        sb.append(m.group(0).replaceAll(myPLR.getRegex(),
                                myPLR.getLevelGroup()));
                    }
                    if (myPLR.getUnits() != null) {
                        sb.append(myPLR.getUnits());
                    }
                    DataLevelType type = new DataLevelType(
                            LevelType.fromDescription(myPLR.getLevel()));
                    sb.append(" " + type.getType().toString());
                    break;
                }
            }
        }
        String levelInfo = sb.toString();

        return levelInfo.trim();
    }

    /**
     * Get the correct level type
     *
     * @param param
     * @return
     */
    private DataLevelType parseLevelType(Parameter param) {

        DataLevelType type = null;
        // SEA ICE special case
        if (param.getDefinition().contains(LevelType.SEAB.getLevelType())) {
            type = new DataLevelType(LevelType.SEAB);
        } else {
            Map<String, ParameterLevelRegex> plr = LookupManager.getInstance()
                    .getParamLevelRegexes();

            Matcher m = null;
            String tempDescription = param.getDefinition();
            for (ParameterLevelRegex myPLR : plr.values()) {
                m = myPLR.getPattern().matcher(tempDescription);
                if (m.find()) {
                    type = new DataLevelType(
                            LevelType.fromDescription(myPLR.getLevel()));
                    type.setUnit(myPLR.getUnits());
                    break;
                }
            }
        }

        if (type == null) {
            logger.warn("Unable to determine level type for: "
                    + param.getDefinition());
            type = new DataLevelType(LevelType.UNKNOWN);
        }

        return type;
    }

    @Override
    public List<DataSetMetaData<?, ?>> parseMetaData(Provider provider,
            List<Link> links, Collection collection, String dataDateFormat)
            throws NoSuchAttributeException {

        Map<String, Parameter> parameters = new HashMap<>();
        Map<String, OpenDapGriddedDataSet> dataSets = new HashMap<>();
        Map<String, List<DataSetMetaData<?, ?>>> metaDatas = new HashMap<>();

        if (CollectionUtil.isNullOrEmpty(links)) {
            return null;
        }

        for (Link link : links) {
            List<String> vals = null;
            try {
                vals = OpenDAPParseUtility.getInstance()
                        .getDataSetNameAndCycle(link.getLinkKey(), collection);
            } catch (Exception e1) {
                logger.error("Failed to get cycle and dataset name set...", e1);
                continue;
            }

            String dataSetName = vals.get(0);
            OpenDapGriddedDataSet dataSet = new OpenDapGriddedDataSet();
            dataSet.setCollectionName(collection.getName());
            dataSet.setProviderName(provider.getName());
            dataSet.setDataSetName(dataSetName);

            final GriddedDataSetMetaData gdsmd = new OpenDapGriddedDataSetMetaData();
            gdsmd.setDataSetName(dataSet.getDataSetName());
            String providerName = dataSet.getProviderName();
            gdsmd.setProviderName(providerName);

            DAS das = link.getMetadata().get(DAP_TYPE.DAS.getDapType());
            // set url first, used for level lookups
            gdsmd.setUrl(link.getUrl().replace(
                    serviceConfig.getConstantValue("META_DATA_SUFFIX"),
                    serviceConfig.getConstantValue("BLANK")));
            Map<String, Parameter> paramMap = getParameters(das, dataSet, gdsmd,
                    link.getSubName(), collection, dataDateFormat);
            dataSet.setParameters(paramMap);
            parameters.putAll(paramMap);

            GriddedTime dataSetTime = gdsmd.getTime();
            if (dataSetTime == null) {
                throw new IllegalStateException(
                        "The time cannot be null for a DataSetMetaData object!");
            }
            dataSet.setTime(dataSetTime);
            gdsmd.setDate(new ImmutableDate(dataSetTime.getStart()));

            List<Integer> forecastHoursAsInteger = new ArrayList<>();
            for (String forecastHour : gdsmd.getTime().getFcstHours()) {
                try {
                    forecastHoursAsInteger.add(Integer.parseInt(forecastHour));
                } catch (NumberFormatException nfe) {
                    logger.warn("Unable to parse [" + forecastHour
                            + "] as an integer!", nfe);
                }
            }
            dataSet.getForecastHours().addAll(forecastHoursAsInteger);

            // The opendap specific info
            Map<String, String> cyclesToUrls = new HashMap<>();
            String cycle = vals.get(1);
            cyclesToUrls.put(cycle, gdsmd.getUrl());

            String cycleAsParseableNum = vals.get(2);

            int cycleAsNum = GriddedDataSetMetaData.NO_CYCLE;
            if (cycleAsParseableNum != null) {
                try {
                    cycleAsNum = Integer.parseInt(cycleAsParseableNum);

                    // Only add the cycle to the dataset if it is NOT the
                    // NO_CYCLE, since that is the object the GUI uses
                    dataSet.getCycles().add(cycleAsNum);
                } catch (NumberFormatException nfe) {
                    // This should never happen since we check for it in the
                    // OpenDAPConstants class, but that is why we freak out if
                    // it DOES happen
                    throw new IllegalArgumentException(nfe.getMessage(), nfe);
                }
            }

            // The NO_CYCLE constant will always update the single url for daily
            // release models, otherwise the parsed cycle is used
            gdsmd.setCycle(cycleAsNum);
            dataSetTime.addCycleTime(cycleAsNum);
            dataSet.cycleUpdated(cycleAsNum);
            // TODO: Delete post 18.1.1
            dataSet.getCyclesToUrls().put(cycleAsNum, gdsmd.getUrl());

            if (dataSet.getTime() == null) {
                throw new IllegalStateException(
                        "The time cannot be null for a DataSet object!");
            }

            int offset = getDataSetAvailabilityTime(gdsmd.getDataSetName(),
                    gdsmd.getTime().getStart().getTime());
            long arrivalTime = TimeUtil.currentTimeMillis();
            dataSet.setArrivalTime(arrivalTime);
            dataSet.setAvailabilityOffset(offset);
            gdsmd.setAvailabilityOffset(offset);
            gdsmd.setArrivalTime(arrivalTime);

            String dsName = dataSet.getDataSetName();
            GriddedCoverage parentCov = new GriddedCoverage();

            boolean isMoving = getIsMovingFromConfig(dsName, providerName);
            if (isMoving) {
                /*
                 * If this is a moving dataset, save the coverage for this
                 * specific instance of the product in the metadata instead of
                 * on the dataset itself. Coverage on the dataset will store the
                 * 'parent coverage' (the outer bounds of where this product can
                 * be positioned).
                 */
                gdsmd.setInstanceCoverage(dataSet.getCoverage());

                /*
                 * The coverage in the dataset for moving products needs to have
                 * a GridCoverage to avoid issues in CAVE. However, this value
                 * will not be used by moving products, so just put in the one
                 * from this metadata instance.
                 */
                parentCov.setGridCoverage(
                        dataSet.getCoverage().getGridCoverage());
                Coverage cov;
                try {
                    cov = getParentBoundsFromConfig(dsName, providerName);
                } catch (FactoryException | TransformException e) {
                    throw new IllegalStateException(
                            "Unable to get Parent Bounds for DataSet '" + dsName
                                    + "' from " + provider,
                            e);
                }
                parentCov.setEnvelope(cov.getEnvelope());
            }

            dataSet.applyInfoFromConfig(isMoving, parentCov,
                    getSizeEstFromConfig(dsName, providerName));

            if (logger.isDebugEnabled()) {
                logger.debug("Dataset Name: " + dsName);
                logger.debug("StartTime:    " + gdsmd.getTime());
                logger.debug(
                        "Offset:       " + dataSet.getAvailabilityOffset());
                logger.debug("Arrival Time: " + dataSet.getArrivalTime());
            }

            if (dataSets.containsKey(dataSetName)) {
                dataSets.get(dataSetName).combine(dataSet);
            } else {
                dataSets.put(dataSetName, dataSet);
            }

            List<DataSetMetaData<?, ?>> toStore = metaDatas.get(dataSetName);
            if (toStore == null) {
                toStore = new ArrayList<>();
                metaDatas.put(dataSetName, toStore);
            }

            toStore.add(gdsmd);
        }

        List<DataSetMetaData<?, ?>> parsedMetadatas = new ArrayList<>();

        logger.info("Processing [" + parameters.size()
                + "] parameters for Collection [" + collection.getName()
                + "] ...");
        // Store the Parameters
        for (Parameter parm : parameters.values()) {
            storeParameter(parm);
        }

        // Store DataSets
        for (Entry<String, List<DataSetMetaData<?, ?>>> entry : metaDatas
                .entrySet()) {
            logger.info("Processing DataSet [" + entry.getKey() + "] with ["
                    + entry.getValue().size()
                    + "] DataSetMetaData entries ...");
            OpenDapGriddedDataSet dataSet = dataSets.get(entry.getKey());
            storeDataSet(dataSet);
            storeMetaData(entry.getValue(), dataSet);
            parsedMetadatas.addAll(entry.getValue());
        }

        return parsedMetadatas;
    }

    @Override
    public void parseMetaData(Provider provider, List<Link> object,
            boolean isFile) throws Exception {
        throw new UnsupportedOperationException(
                "Not implemented for this type");

    }

}
