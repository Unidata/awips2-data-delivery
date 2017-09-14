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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.io.Files;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.LevelGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterLevelEntry;
import com.raytheon.uf.common.dataplugin.grid.GridRecord;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataplugin.level.LevelFactory;
import com.raytheon.uf.common.dataplugin.level.MasterLevel;
import com.raytheon.uf.common.gridcoverage.GridCoverage;
import com.raytheon.uf.common.gridcoverage.lookup.GridCoverageLookup;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;

/**
 * Response processing related Utilities
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 07, 2011           dhladky   Initial creation
 * Aug 20, 2012  743      djohnson  Fix cache lookup to use the model name and
 *                                  not hashcode.
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Jan 30, 2013  1543     djohnson  Log exception stacktrace.
 * Aug 30, 2013  2298     rjpeter   Make getPluginName abstract
 * Sep 25, 2013  1797     dhladky   separated time from gridded time
 * Oct 10, 2013  1797     bgonzale  Refactored registry Time objects.
 * Apr 22, 2014  3046     dhladky   Sample URI creation for DD dupe check got
 *                                  broken.
 * Sep 14, 2014  2131     dhladky   PDA file compression, decompression, writing
 *                                  and reading tools.
 * Jun 13, 2017  6204     nabowle   Use awipsName for parameter abbreviation
 *                                  when available. Cleanup.
 * Sep 12, 2017  6413     tjensen   Removed unnecessary requestLevelStart and
 *                                  End
 *
 * </pre>
 *
 * @author dhladky
 */

public class ResponseProcessingUtilities {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(ResponseProcessingUtilities.class);

    public static GridRecord getGridRecord(String name, ParameterGroup parm,
            ParameterLevelEntry entry, Level level, String ensembleId,
            GridCoverage gridCoverage) {

        com.raytheon.uf.common.parameter.Parameter parameter = new com.raytheon.uf.common.parameter.Parameter();

        parameter.setAbbreviation(parm.getAbbrev());
        parameter.setName(entry.getDescription());
        parameter.setUnitString(parm.getUnits());

        GridRecord record = new GridRecord();
        gridCoverage = getCoverageFromCache(gridCoverage);
        record.setLocation(gridCoverage);
        record.setLevel(level);
        record.setParameter(parameter);
        record.setDatasetId(name);
        record.setEnsembleId(ensembleId);
        return record;
    }

    /**
     * Gets a grid coverage from the DB cache system wide.
     *
     * @param coverage
     * @return
     */
    public static GridCoverage getCoverageFromCache(GridCoverage coverage) {
        return GridCoverageLookup.getInstance().getCoverage(coverage, true);
    }

    /**
     * get the number of times in a request
     *
     * @param time
     * @return
     */
    public static int getOpenDAPGridNumTimes(GriddedTime time) {

        int start = time.getRequestStartTimeAsInt();
        int end = time.getRequestEndTimeAsInt();

        return (end - start) + 1;
    }

    /**
     * get number of levels in a request
     *
     * @param parm
     * @return
     */
    public static int getOpenDAPGridNumLevels(ParameterGroup paramGroup) {
        int numLevs = 0;
        for (LevelGroup ple : paramGroup.getGroupedLevels().values()) {
            numLevs += ple.getLevels().size();
        }
        return numLevs;
    }

    /**
     * Gather the prospective data times for a request
     *
     * @param time
     * @return
     */
    public static List<DataTime> getOpenDAPGridDataTimes(GriddedTime time) {

        List<DataTime> dt = new ArrayList<>();

        int numTimes = getOpenDAPGridNumTimes(time);
        int reqStartInt = time.getRequestStartTimeAsInt();

        for (int i = 0; i < numTimes; i++) {

            int increment = time.findForecastStepUnit() * reqStartInt;
            DataTime dataTime = null;
            dataTime = new DataTime(time.getStart(), increment);
            dt.add(dataTime);
            reqStartInt++;
        }

        return dt;
    }

    /**
     * determine levels for this parameter
     *
     * @param parm
     * @return
     */
    public static List<Level> getOpenDAPGridLevels(LevelGroup lg) {

        List<Level> levels = new ArrayList<>(lg.getLevels().size());
        String masterLevelName = lg.getMasterKey();
        MasterLevel masterLevel = LevelFactory.getInstance()
                .getMasterLevel(masterLevelName);

        try {
            for (ParameterLevelEntry ple : lg.getLevels()) {
                double levelOneValue = Double.NaN;
                double levelTwoValue = Level.getInvalidLevelValue();
                if (NumberUtils.isNumber(ple.getLevelOne())) {
                    levelOneValue = Double.parseDouble(ple.getLevelOne());
                }
                if (NumberUtils.isNumber(ple.getLevelTwo())) {
                    levelTwoValue = Double.parseDouble(ple.getLevelTwo());
                }

                Level level = LevelFactory.getInstance().getLevel(
                        masterLevelName, levelOneValue, levelTwoValue);
                level.setMasterLevel(masterLevel);
                levels.add(level);
            }
        } catch (Exception e) {
            statusHandler.error("Couldn't retrieve the levels : " + lg.getKey(),
                    e);
        }

        return levels;

    }

    public static List<Level> getOpenDAPGridLevels(
            Collection<LevelGroup> levelGroups) {
        List<Level> levels = new ArrayList<>(levelGroups.size());
        for (LevelGroup lg : levelGroups) {
            levels.addAll(getOpenDAPGridLevels(lg));
        }
        return levels;
    }

    /**
     * Simple way to convert a file into a byte[]
     *
     * @param fileName
     * @return
     */
    public static byte[] getBytes(String fileName) throws Exception {
        File f = new File(fileName);
        return Files.toByteArray(f);
    }

    /**
     * Reads a file, compresses it.
     *
     * @param fileName
     * @return
     */
    public static byte[] getCompressedFile(String fileName) throws Exception {

        byte[] bytes = getBytes(fileName);
        return compress(bytes);
    }

    /**
     * Decodes, de-compresses and writes the gzipped bytes.
     *
     * @param encodedFile
     * @param filePath
     * @throws Exception
     */
    public static void writeCompressedFile(byte[] zippedFile, String filePath)
            throws Exception {

        File file = new File(filePath);
        byte[] fileBytes = decompress(zippedFile);
        writeFileBytes(fileBytes, file);

    }

    /**
     * Decodes and writes the byte[] encoded into file
     *
     * @param encodedFile
     * @param file
     * @throws Exception
     */
    public static void writeFileBytes(byte[] bytes, File file)
            throws Exception {
        Files.write(bytes, file);
    }

    /**
     * Used by PDA to compress file byte[] for SBN delivery
     *
     * @param content
     * @return
     */
    public static byte[] compress(byte[] content) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(
                byteArrayOutputStream)) {
            gzipOutputStream.write(content);
            gzipOutputStream.flush();
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Used by PDA to de-compress file byte[] from SBN
     *
     * @param contentBytes
     * @return
     */
    public static byte[] decompress(byte[] contentBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ByteArrayInputStream byteStreamer = new ByteArrayInputStream(
                contentBytes);
                GZIPInputStream gzipper = new GZIPInputStream(byteStreamer)) {
            IOUtils.copy(gzipper, out);
        }
        return out.toByteArray();
    }

}
