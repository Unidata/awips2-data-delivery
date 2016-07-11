package com.raytheon.uf.edex.datadelivery.retrieval.pda;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Extract File Name based info for MetaData from PDA
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 08, 2014  3120     dhladky   Initial creation
 * Sep 04, 2014  3121     dhladky   Some rework on the parsing.
 * Nov 10, 2014  3826     dhladky   Added more logging, improved versatility of
 *                                  parser.
 * Apr 27, 2015  4435     dhladky   PDA added more formats of messages.
 * Sep 14, 2015  4881     dhladky   Updates to PDA processing.
 * Jul 13, 2016  5752     tjensen   Refactor extractMetaData
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDAFileMetaDataExtractor extends
        PDAMetaDataExtractor<String, String> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDAFileMetaDataExtractor.class);

    /** remove extension **/
    public static final String timeReplace = "DT_";

    public PDAFileMetaDataExtractor() {
        super();
    }

    /**
     * Reads in a file name and extracts metadata from the name.
     * 
     * @param file
     *            String object containing the file name
     * @return Map of metadata values
     */
    public Map<String, String> extractMetaData(Object file) throws Exception {

        // starting point for the parsing
        Map<String, String> paramMap = new HashMap<>(6);
        String fileName = (String) file;
        statusHandler.info("Extracting MetaData from: " + fileName);

        /*
         * Pull in regex and grouping from the PDA service config file in case
         * PDA changes the format in the future.
         */
        Pattern titlePattern = Pattern.compile(serviceConfig
                .getConstantValue("RECORD_TITLE_REGEX"));
        String collectionNameFormat = serviceConfig
                .getConstantValue("COLLECTION_NAME_FORMAT");
        String paramFormat = serviceConfig.getConstantValue("PARAM_FORMAT");
        String sTimeFormat = serviceConfig
                .getConstantValue("START_TIME_FORMAT");
        String eTimeFormat = serviceConfig.getConstantValue("END_TIME_FORMAT");
        String cTimeFormat = serviceConfig
                .getConstantValue("CREATE_TIME_FORMAT");
        String dsNameFormat = serviceConfig
                .getConstantValue("DATASET_NAME_FORMAT");

        // Make sure the filename matches the expected pattern before parsing
        Matcher titleMatcher = titlePattern.matcher(fileName);
        if (titleMatcher.matches()) {
            paramMap.put("collectionName", fileName.replaceAll(
                    titlePattern.pattern(), collectionNameFormat));
            paramMap.put("paramName",
                    fileName.replaceAll(titlePattern.pattern(), paramFormat));
            paramMap.put("startTime",
                    fileName.replaceAll(titlePattern.pattern(), sTimeFormat));
            paramMap.put("endTime",
                    fileName.replaceAll(titlePattern.pattern(), eTimeFormat));
            paramMap.put("dataTime",
                    fileName.replaceAll(titlePattern.pattern(), cTimeFormat));
            paramMap.put("dataSetName",
                    fileName.replaceAll(titlePattern.pattern(), dsNameFormat));
            if (debug) {
                for (String key : paramMap.keySet()) {
                    statusHandler.info(key + ": " + paramMap.get(key));
                }
            }
        } else {
            statusHandler.error(
                    "Couldn't create parameter mappings from file!", fileName);
            throw new IllegalArgumentException("File name '" + fileName
                    + "' does not match the expected pattern: '"
                    + serviceConfig.getConstantValue("RECORD_TITLE_REGEX")
                    + "'");
        }

        return paramMap;
    }

    @Override
    public void setDataDate() throws Exception {
        // No implementation in PDA file extractor
    }

    /**
     * Test Practice parsing
     * 
     * @param args
     */
    public static void main(String[] args) {
        PDAFileMetaDataExtractor pfmde = new PDAFileMetaDataExtractor();
        try {
            Map<String, String> results = pfmde.extractMetaData(args[0]);

            for (Entry<String, String> entry : results.entrySet()) {
                statusHandler.info("Param: " + entry.getKey() + ": Value: "
                        + entry.getValue());
            }

            if (results.containsKey("startTime")) {
                String time = results.get("startTime");

                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "YYYYMMddHHmmss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                Date date = dateFormat.parse(time);
                statusHandler.info("Date: " + date);
            }

        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR, e.getLocalizedMessage(), e);
        }
    }

}
