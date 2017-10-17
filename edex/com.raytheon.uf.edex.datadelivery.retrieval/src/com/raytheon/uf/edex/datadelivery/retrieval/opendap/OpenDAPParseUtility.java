package com.raytheon.uf.edex.datadelivery.retrieval.opendap;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raytheon.uf.common.datadelivery.registry.DataSetNaming;
import com.raytheon.uf.common.datadelivery.registry.Ensemble;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.URLParserInfo;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Constant;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.units.UnitMapper;
import com.raytheon.uf.common.util.mapping.MultipleMappingException;

import opendap.dap.AttributeTable;
import opendap.dap.DArray;
import opendap.dap.DConnect;
import opendap.dap.DataDDS;
import opendap.dap.NoSuchAttributeException;
import opendap.dap.PrimitiveVector;

/**
 * Constants for working with OpenDAP. This class should remain package-private,
 * all access should be limited to classes in the same package.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 20, 2011  218      dhladky   Initial creation
 * Jul 24, 2012  955      djohnson  Use {@link Pattern}s, simplify logic.
 * Aug 09, 2012  1022     djohnson  Handle correct parsing of wave model dataset
 *                                  names.
 * Aug 31, 2012  1125     djohnson  Rename getCollectionAndCycle() to
 *                                  getDataSetNameAndCycle(), gens related
 *                                  datasets prepend collection name.
 * Sep 06, 2012  1125     djohnson  Also prepend naefs collection names.
 * Oct 28, 2012  1163     dhladky   Largely did away with this Class in lieu of
 *                                  configfile.
 * Nov 09, 2012  1163     dhladky   Made pre-load for service config
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Jan 08, 2013  1466     dhladky   NCOM dataset name parsing fix.
 * Jan 18, 2013  1513     dhladky   Level Lookup improvements.
 * Oct 24, 2013  2454     dhladky   NOMADS change to ensemble configuration.
 * Nov 09, 2016  5988     tjensen   Update for Friendly naming for NOMADS
 * May 10, 2017  6135     nabowle   Replace UnitLookup with UnitMapper.
 * Oct 10, 2017  6465     tjensen   Moved UNIT_MAPPER_NAMESPACE. Rename
 *                                  Collections to URLParserInfo
 *
 * </pre>
 *
 * @author dhladky
 */
public final class OpenDAPParseUtility {

    private static final Pattern QUOTES_PATTERN = Pattern.compile("\"");

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    private static final String UNIT_MAPPER_NAMESPACE = "datadelivery";

    /** Singleton instance of this class */
    private static volatile OpenDAPParseUtility instance = null;

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(OpenDAPParseUtility.class);

    /*
     * Service configuration for OPENDAP
     */
    private final ServiceConfig serviceConfig;

    /* Private Constructor */
    private OpenDAPParseUtility() {
        serviceConfig = HarvesterServiceManager.getInstance()
                .getServiceConfig(ServiceType.OPENDAP);
    }

    /**
     * call this to get your instance
     *
     * @return
     */
    public static OpenDAPParseUtility getInstance() {
        if (instance == null) {
            instance = new OpenDAPParseUtility();
        }
        return instance;
    }

    /**
     * Get the dataset name and cycle.
     *
     * @param linkKey
     *            the linkKey
     * @param urlParserInfo
     *            the urlParserInfo
     * @return the dataset name and cycle
     */
    public List<String> getDataSetNameAndCycle(String linkKey,
            URLParserInfo urlParserInfo) throws Exception {
        String datasetName = null;
        String cycle = null;
        String numCycle = null;

        if (serviceConfig.getDataSetConfig() != null) {
            // cycle defaults to none
            cycle = serviceConfig.getConstantValue("NONE");

            // special processing
            if (urlParserInfo.getPattern() != null) {
                com.raytheon.uf.common.datadelivery.registry.Pattern pat = urlParserInfo
                        .getPattern();
                Pattern innerPattern = Pattern.compile(pat.getRegex());
                String[] chunks = innerPattern.split(linkKey);
                // special RTOFS
                if (pat.getDataSetLocationAt(0) == 0
                        && pat.getCycleLocationAt(0) == 1
                        && chunks.length == 2) {
                    datasetName = chunks[0];
                    cycle = chunks[1];
                    // Special for NCOM, no cycle
                } else if (pat.getDataSetLocationAt(0) == 1
                        && chunks.length == 3) {
                    datasetName = chunks[1];
                } else {
                    // most often used
                    datasetName = linkKey;
                    // there is no cycle;
                }
            } else {
                Map<com.raytheon.uf.common.datadelivery.registry.Pattern, Pattern> patterns = serviceConfig
                        .getDataSetConfig().getPatternMap();
                for (Entry<com.raytheon.uf.common.datadelivery.registry.Pattern, Pattern> entry : patterns
                        .entrySet()) {

                    com.raytheon.uf.common.datadelivery.registry.Pattern pat = entry
                            .getKey();

                    // non specific pattern processing
                    Matcher m = entry.getValue().matcher(linkKey);
                    if (m.find()) {
                        if (m.groupCount() > 0) {
                            if (pat.getDataSetLocationAt(0) == 1
                                    && pat.getCycleLocationAt(0) == 3) {
                                datasetName = m.group(1);
                                cycle = m.group(3);
                            }
                            if (pat.getDataSetLocationAt(0) == 1
                                    && pat.getCycleLocationAt(0) == 2
                                    && m.groupCount() == 2) {

                                datasetName = m.group(1);
                                cycle = m.group(2);
                            }
                            if (pat.getDataSetLocationAt(0) == 1
                                    && pat.getCycleLocationAt(0) == 2
                                    && pat.getDataSetLocationAt(1) == 3
                                    && m.groupCount() > 2) {

                                datasetName = m.group(1) + m.group(3);
                                cycle = m.group(2);
                            }

                            if (datasetName != null) {
                                break;
                            }
                        }
                    }
                }
            }

            // Fall back to the default, collectionName
            if (datasetName == null) {
                datasetName = urlParserInfo.getName();
            }

            if (urlParserInfo.getDataSetNaming() != null) {
                DataSetNaming dsn = urlParserInfo.getDataSetNaming();

                if (dsn != null) {

                    Constant constant = serviceConfig
                            .getNamingSchema(dsn.getExpression());

                    if (constant != null) {
                        if (dsn.getExpression()
                                .equals(serviceConfig.getConstantValue(
                                        "ALTERNATE_NAMING_SCHEMA1"))) {
                            datasetName = urlParserInfo.getName()
                                    + dsn.getSeparator() + datasetName;
                        } else if (dsn.getExpression()
                                .equals(serviceConfig.getConstantValue(
                                        "ALTERNATE_NAMING_SCHEMA2"))) {
                            datasetName = urlParserInfo.getName();
                        } else {
                            statusHandler.handle(Priority.INFO,
                                    dsn.getExpression()
                                            + "Is not a known OPENDAP Alternate naming schema. "
                                            + urlParserInfo);
                        }
                    }
                }
            }

            try {
                numCycle = Integer
                        .valueOf(cycle.substring(0, cycle.length() - 1))
                        .toString();
            } catch (NumberFormatException nfe) {
                // Not a problem, just not a numeric cycle
            }
        }

        return Arrays.asList(datasetName, cycle, numCycle);
    }

    public Pattern getTimeStepPattern() {

        String timeStep = serviceConfig.getConstantValue("TIME_STEP_PATTERN");
        return Pattern.compile(timeStep);
    }

    public Pattern getUnitPattern() {
        String unitPattern = serviceConfig.getConstantValue("UNIT_PATTERN");
        return Pattern.compile(unitPattern);
    }

    public Pattern getZPattern() {

        String z = serviceConfig.getConstantValue("Z_PATTERN");
        return Pattern.compile(z);
    }

    /**
     * Remove the Z from the date
     *
     * @param date
     * @return
     */
    public String parseDate(String date) {
        return trim(getZPattern().matcher(date)
                .replaceAll(serviceConfig.getConstantValue("BLANK")));
    }

    /**
     * parse ensemble model info
     *
     * @param table
     * @return
     * @throws NoSuchAttributeException
     */
    public Ensemble parseEnsemble(AttributeTable table)
            throws NoSuchAttributeException {

        Ensemble ens = new Ensemble();

        /*
         * Ensemble members used to be listed under the attribute "grads_name".
         * Somewhere along the way, GRADS/NOMADS stopped doing that to identify
         * it's ensembles. Both methods are listed, the original and the new as
         * a fall back.
         */

        if (table
                .getAttribute(serviceConfig.getConstantValue("NAME")) != null) {
            String name = trim(
                    table.getAttribute(serviceConfig.getConstantValue("NAME"))
                            .getValueAt(0));
            String[] members = COMMA_PATTERN.split(name);
            ens.setMembers(Arrays.asList(members));
        } else if (table
                .getAttribute(serviceConfig.getConstantValue("SIZE")) != null) {
            int size = Integer.parseInt(trim(
                    table.getAttribute(serviceConfig.getConstantValue("SIZE"))
                            .getValueAt(0)));
            List<String> members = new ArrayList<>(size);
            if (size > 0) {
                for (Integer i = 0; i < size; i++) {
                    members.add(i.toString());
                }
                ens.setMembers(members);
            }
            // empty default ensemble if no size exists
        }

        return ens;

    }

    /**
     * Parses the time steps
     *
     * @param timeStep
     * @return
     */
    public List<String> parseTimeStep(String inStep) {
        List<String> step = new ArrayList<>();

        Matcher matcher = getTimeStepPattern().matcher(trim(inStep));

        if (matcher.find()) {
            step.add(matcher.group(1));
            step.add(matcher.group(2));
        } else {
            throw new IllegalArgumentException(
                    "Unable to find time step with input [" + inStep + "]");
        }

        return step;
    }

    /**
     * Strip off the annoying brackets on the units Fix any units that are not
     * correct with SI
     *
     * @param description
     * @return The parsed unit.
     * @throws MultipleMappingException
     *             if the parsed unit has been mapped to multiple base units.
     */
    public String parseUnits(String description)
            throws MultipleMappingException {

        String runit = serviceConfig.getConstantValue("UNKNOWN");

        // some require no parsing
        String base = UnitMapper.getInstance().lookupBaseNameOrNull(description,
                UNIT_MAPPER_NAMESPACE);
        if (base != null) {
            runit = base;
        } else {
            Matcher m = getUnitPattern().matcher(description);

            if (m.find()) {
                runit = m.group(2);
                base = UnitMapper.getInstance().lookupBaseNameOrNull(runit,
                        UNIT_MAPPER_NAMESPACE);
                if (base != null) {
                    runit = base;
                }
            }
        }
        return runit;
    }

    /**
     * Remove silly quotes
     *
     * @param val
     * @return
     */
    public String trim(String val) {

        return QUOTES_PATTERN.matcher(val)
                .replaceAll(serviceConfig.getConstantValue("BLANK"));
    }

    /**
     * Parse out the levels from the dods
     *
     * @param url
     * @param lev
     * @return
     */
    public List<Double> parseLevels(String url, String lev) {

        List<Double> levels = null;

        try {
            DConnect connect = OpenDAPConnectionUtil
                    .getDConnectDAP2(url + "?" + lev);
            DataDDS data = connect.getData(null);
            DArray array = (DArray) data.getVariable(lev);
            PrimitiveVector pm = array.getPrimitiveVector();
            double[] values = (double[]) pm.getInternalStorage();
            levels = new ArrayList<>();
            for (double value : values) {
                levels.add(value);
            }

        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error downloading/parsing levels: " + url, e);
        }

        return levels;

    }

}
