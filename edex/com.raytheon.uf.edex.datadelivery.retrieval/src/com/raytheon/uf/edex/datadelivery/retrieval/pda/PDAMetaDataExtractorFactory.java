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

import com.raytheon.uf.edex.datadelivery.retrieval.metadata.MetaDataParseException;

import net.opengis.ows.v_1_0_0.BoundingBoxType;

/**
 * Creates the correct extractor for a given pda implementation type.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Mar 31, 2017  6186     rjpeter   Initial creation
 *
 * </pre>
 *
 * @author rjpeter
 */
public class PDAMetaDataExtractorFactory {

    /**
     * Returns the extractor for the PDA rcord type.
     * 
     * @param metadataId
     * @param title
     * @param boundingBox
     * @return
     * @throws MetaDataParseException
     */
    public static PDAMetaDataExtractor getExtractor(String metadataId,
            String title, BoundingBoxType boundingBox)
            throws MetaDataParseException {
        PDAMetaDataExtractor extractor = new PDAPipeDelimitedMetaDataExtractor(
                metadataId, title, boundingBox);
        if (!extractor.accept()) {
            extractor = new PDAFileMetaDataExtractor(metadataId, title,
                    boundingBox);
            if (!extractor.accept()) {
                throw new MetaDataParseException(
                        "No MetaDataExtractor defined for BriefRecord");
            }
        }

        return extractor;
    }
}
