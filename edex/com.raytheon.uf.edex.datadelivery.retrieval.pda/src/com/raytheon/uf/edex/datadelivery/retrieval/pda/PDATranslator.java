
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

import java.io.File;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.PDARetrievalResponse.FILE;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.metadata.PDAMetaDataAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalTranslator;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ResponseProcessingUtilities;

/**
 * 
 * Translate PDA (FTP) retrievals into PDOs
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------------------------------
 * Sep 12, 2014  3121     dhladky   created.
 * Jan 28, 2016  5299     dhladky   Generic PDO type change.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * May 03, 2016  5599     tjensen   Pass subscription name into decodeObjects
 * Sep 16, 2016  5762     tjensen   Remove Camel from FTPS calls
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDATranslator extends
        RetrievalTranslator<Time, Coverage, PluginDataObject> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDATranslator.class);

    public PDATranslator(RetrievalAttribute<Time, Coverage> attXML)
            throws InstantiationException {
        super(attXML);
    }

    @Override
    protected int getSubsetNumTimes() {
        // TODO Since we can't subset in PDA, all of this is meaningless
        return 0;
    }

    @Override
    protected int getSubsetNumLevels() {
        // TODO Since we can't subset in PDA, all of this is meaningless
        return 0;
    }

    @Override
    protected List<DataTime> getTimes() {
        // TODO Since we can't subset in PDA, all of this is meaningless
        return null;
    }

    /**
     * Map containing fileName and file bytes (compressed) of the file delivered
     * via retrieval. calls out to MetaDataAdapter, which decodes, then returns
     * PDOs.
     * 
     * @param payload
     * @return
     */

    public PluginDataObject[] asPluginDataObjects(
            Map<PDARetrievalResponse.FILE, Object> payload) {

        PluginDataObject[] pdos = null;
        PDAMetaDataAdapter pdaAdapter = (PDAMetaDataAdapter) metadataAdapter;
        String fileName = null;
        String subName = null;

        try {
            /*
             * No reason to write it if it already exists! In the case of the
             * local registry this is un-necessary. Only in the case of SBN
             * delivery does the file have to be written. This is because it has
             * been "delivered" from the central registry and doesn't exist
             * locally.
             */

            fileName = (String) payload.get(FILE.FILE_NAME);
            if (fileName != null) {
                File file = new File(fileName);
                if (!file.exists()) {
                    ResponseProcessingUtilities.writeCompressedFile(
                            (byte[]) payload.get(FILE.FILE_BYTES), fileName);
                }
                subName = (String) payload.get(FILE.SUBSCRIPTION_NAME);
                statusHandler
                        .info("Processing PDA retrieval file: " + fileName);
                pdos = pdaAdapter.decodeObjects(fileName, subName);
            } else {
                statusHandler
                        .warn("Unable to process file: null file name received.");
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to decode PDA file objects!", e);
        }

        return pdos;
    }

}
