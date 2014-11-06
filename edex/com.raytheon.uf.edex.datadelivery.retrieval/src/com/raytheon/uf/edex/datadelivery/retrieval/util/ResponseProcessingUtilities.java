package com.raytheon.uf.edex.datadelivery.retrieval.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

import com.google.common.io.Files;
import com.raytheon.uf.common.datadelivery.registry.DataLevelType.LevelType;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.Levels;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
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

/**
 * Response processing related Utilities
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 07, 2011            dhladky     Initial creation
 * Aug 20, 2012 0743       djohnson    Fix cache lookup to use the model name
 *                                     and not hashcode.
 * Nov 19, 2012 1166       djohnson    Clean up JAXB representation of registry
 *                                     objects.
 * Jan 30, 2013 1543       djohnson    Log exception stacktrace.
 * Aug 30, 2013 2298       rjpeter     Make getPluginName abstract
 * Sept 25, 2013 1797      dhladky     separated time from gridded time
 * Oct 10, 2013 1797       bgonzale    Refactored registry Time objects.
 * Apr 22, 2014 3046       dhladky     Sample URI creation for DD dupe check got broken.
 * Sep 14, 2014 2131       dhladky     PDA file compression, decompression, writing and reading tools.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class ResponseProcessingUtilities {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(ResponseProcessingUtilities.class);

    public static GridRecord getGridRecord(String name, Parameter parm,
            Level level, String ensembleId, GridCoverage gridCoverage) {

        com.raytheon.uf.common.parameter.Parameter parameter = new com.raytheon.uf.common.parameter.Parameter();
        parameter.setAbbreviation(parm.getName());
        parameter.setName(parm.getDefinition());
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
    public static int getOpenDAPGridNumLevels(Parameter parm) {
        if (parm.getLevels().getSelectedLevelIndices() == null) {
            return 1;
        }
        int numLevs = parm.getLevels().getSelectedLevelIndices().size();
        if (numLevs == 0) {
            numLevs = 1;
        }
        return numLevs;
    }

    /**
     * Gather the prospective data times for a request
     * 
     * @param time
     * @return
     */
    public static ArrayList<DataTime> getOpenDAPGridDataTimes(GriddedTime time) {

        ArrayList<DataTime> dt = new ArrayList<DataTime>();

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
    public static ArrayList<Level> getOpenDAPGridLevels(Levels plevels) {

        ArrayList<Level> levels = new ArrayList<Level>();

        try {
            int startLevel = plevels.getRequestLevelStart();
            int endLevel = plevels.getRequestLevelEnd();

            for (int index = startLevel; index <= endLevel; index++) {
                double levelOneValue = plevels.getLevelAt(index);
                String masterLevelName = LevelType.getLevelTypeIdName(plevels
                        .getLevelType());
                MasterLevel masterLevel = LevelFactory.getInstance()
                        .getMasterLevel(masterLevelName);
                Level level = LevelFactory.getInstance().getLevel(
                        masterLevelName, levelOneValue);
                level.setMasterLevel(masterLevel);
                levels.add(level);
            }
        } catch (Exception e) {
            statusHandler.error(
                    "Couldn't retrieve the levels : " + plevels.getLevelType(),
                    e);
        }

        return levels;

    }
    
    /**
     * Simple way to convert a file into a byte[]
     * 
     * @param fileName
     * @return
     */
    public static byte[] getBytes(String fileName)
            throws Exception {

        File f = new File(fileName);
        return Files.toByteArray(f);
    }

    /**
     * Reads a file, compresses it.
     * 
     * @param fileName
     * @return
     */
    public static byte[] getCompressedFile(String fileName)
            throws Exception {

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
    public static void writeFileBytes(byte[] bytes, File file) throws Exception {

        Files.write(bytes, file);
    }

    /**
     * Used by PDA to compress file byte[] for SBN delivery
     * 
     * @param content
     * @return
     */
    public static byte[] compress(byte[] content) throws Exception {

        GZIPOutputStream gzipOutputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;

        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
            gzipOutputStream.write(content);

        } finally {
            if (gzipOutputStream != null) {
                gzipOutputStream.flush();
                gzipOutputStream.close();
            }
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

        ByteArrayOutputStream out = null;
        
        try {
            out = new ByteArrayOutputStream();
            ByteArrayInputStream byteStreamer = new ByteArrayInputStream(
                    contentBytes);
            GZIPInputStream gzipper = new GZIPInputStream(byteStreamer);
            IOUtils.copy(gzipper, out);
            
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }

        return out.toByteArray();
    }

}
