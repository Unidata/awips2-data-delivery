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
package com.raytheon.uf.edex.datadelivery.retrieval.pda;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.EnvelopeUtils;
import com.raytheon.uf.common.datadelivery.retrieval.xml.MetaDataPattern;
import com.raytheon.uf.common.datadelivery.retrieval.xml.PatternGroup;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.MetaDataParseException;
import com.raytheon.uf.edex.datadelivery.retrieval.util.PDAMetaDataUtil;
import com.raytheon.uf.edex.ogc.common.OgcException;
import com.raytheon.uf.edex.ogc.common.spatial.BoundingBoxUtil;
import com.vividsolutions.jts.geom.Coordinate;

import net.opengis.cat.csw.v_2_0_2.BriefRecordType;
import net.opengis.ows.v_1_0_0.BoundingBoxType;

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
 * Aug 16, 2016  5752     tjensen   Added options for data set name translation
 * Aug 18, 2016  5752     tjensen   Fix initSatMapping
 * Aug 25, 2016  5752     tjensen   Remove Create Time
 * Sep 01, 2016  5752     tjensen   Exclude data older than retention period
 * Jan 27, 2017  6089     tjensen   Update to work with pipe delimited metadata
 * Mar 31, 2017  6186     rjpeter   Refactored.
 * Jun 29, 2017  6130     tjensen   Remove res from validateParamData call
 *
 * </pre>
 *
 * @author dhladky
 */

public class PDAFileMetaDataExtractor extends PDAMetaDataExtractor {

    public static final String CONFIG = "RECORD_TITLE";

    public static final String PARAM_FORMAT = "PARAM_FORMAT";

    public static final String RES_FORMAT = "RES_FORMAT";

    public PDAFileMetaDataExtractor(String metadataId, String title,
            BoundingBoxType boundingBox) {
        super(PDAMetaDataUtil.getInstance().getMetaDataPattern(CONFIG),
                metadataId, title, boundingBox);
    }

    @Override
    public boolean accept() {
        Pattern titlePattern = metaDataPattern.getPattern();
        Matcher titleMatcher = titlePattern.matcher(title);

        boolean rval = titleMatcher.matches();

        if (!rval) {
            logger.warn("Title [" + title + "] did not match pattern ["
                    + metaDataPattern.getRegex() + "]");
        }

        return rval;
    }

    /**
     * Reads in a file name and extracts metadata from the name.
     *
     * @param brt
     *            The BriefRecordType all the data originated from. Currently
     *            unused and only passed due to interface contract.
     * @return Map of metadata values
     */
    @Override
    public Map<String, String> extractMetaData(BriefRecordType brt)
            throws Exception {
        // starting point for the parsing
        logger.info("Extracting MetaData from title...");

        Map<String, PatternGroup> mdpGroups = metaDataPattern.getGroupMap();
        String paramFormat = mdpGroups.get(PARAM_FORMAT).getValue();
        String resFormat = mdpGroups.get(RES_FORMAT).getValue();
        String satFormat = mdpGroups.get(SAT_FORMAT).getValue();
        String sTimeFormat = mdpGroups.get(START_TIME_FORMAT).getValue();
        String eTimeFormat = mdpGroups.get(END_TIME_FORMAT).getValue();
        String regex = metaDataPattern.getRegex();

        String res = title.replaceAll(regex, resFormat);
        String sat = title.replaceAll(regex, satFormat);
        String param = title.replaceAll(regex, paramFormat);
        String startTime = title.replaceAll(regex, sTimeFormat);
        String endTime = title.replaceAll(regex, eTimeFormat);

        validateParamData(param, sat, startTime, endTime);

        Map<String, String> paramMap = new HashMap<>(4, 1);
        paramMap.put(PARAM_NAME, param);
        paramMap.put(RES_NAME, res);
        paramMap.put(SAT_NAME, sat);

        return paramMap;
    }

    /**
     * Set the Coverage from the OGC BoundingBox
     *
     * @param boundingBoxType
     * @param provider
     */
    @Override
    public Coverage getCoverage() throws MetaDataParseException {
        if (boundingBox == null) {
            throw new MetaDataParseException(
                    "No bounding box parsed from BriefRecord. Cannot generate coverage");
        }

        ReferencedEnvelope envelope = null;
        Coverage coverage = new Coverage();

        try {
            logger.info("Parsed LOWER CORNER: "
                    + boundingBox.getLowerCorner().get(0) + ", "
                    + boundingBox.getLowerCorner().get(1) + " size: "
                    + boundingBox.getLowerCorner().size());
            logger.info("Parsed UPPER CORNER: "
                    + boundingBox.getUpperCorner().get(0) + ", "
                    + boundingBox.getUpperCorner().get(1) + " size: "
                    + boundingBox.getUpperCorner().size());
            logger.info("CRS: " + boundingBox.getCrs());
            logger.info("Dimensions: " + boundingBox.getDimensions());

            envelope = BoundingBoxUtil.convert2D(boundingBox);

            Coordinate ul = EnvelopeUtils.getUpperLeftLatLon(envelope);
            Coordinate lr = EnvelopeUtils.getLowerRightLatLon(envelope);
            logger.info("Envelope Coods: (" + ul.x + "," + ul.y + ") to ("
                    + lr.x + "," + lr.y + ")");

            coverage.setEnvelope(envelope);
        } catch (OgcException e) {
            throw new MetaDataParseException(
                    "Couldn't determine BoundingBox envelope!", e);
        }

        return coverage;
    }

    public static boolean validateMetaDataPatterns(
            Map<String, MetaDataPattern> mdpMap, StringBuilder errors) {
        boolean valid = true;
        MetaDataPattern titleMdp = mdpMap.get(CONFIG);
        if (titleMdp != null) {
            Map<String, PatternGroup> mdpGroups = titleMdp.getGroupMap();
            if (mdpGroups != null && !mdpGroups.isEmpty()) {
                List<String> reqGroups = Arrays.asList(PARAM_FORMAT, RES_FORMAT,
                        SAT_FORMAT, START_TIME_FORMAT, END_TIME_FORMAT);
                for (String group : reqGroups) {
                    if (mdpGroups.get(group) == null) {
                        valid = false;
                        errors.append("'" + group + "' group needed for '"
                                + CONFIG + "' pattern. ");
                    }
                }
            } else {
                valid = false;
                errors.append("No groups found for '" + CONFIG + "' pattern. ");
            }
        } else {
            valid = false;
            errors.append(
                    "A '" + CONFIG + "' pattern is required and missing. ");
        }
        return valid;
    }
}
