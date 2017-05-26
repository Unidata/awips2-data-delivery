
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.VersionData;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.util.app.AppInfo;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.DecodeInfo;
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
 * May 22, 2017  6130     tjensen   Update for Polar products
 *
 * </pre>
 *
 * @author dhladky
 * @version 1.0
 */

public class PDATranslator
        extends RetrievalTranslator<Time, Coverage, PluginDataObject> {

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
     * Stores the file to the configured directory, then sends it off to be
     * processed by Ingest.
     *
     * @param response
     * @param dataSet
     * @param subOwner
     */
    public void storeAndProcess(PDARetrievalResponse response, DataSet dataSet,
            String subOwner) {

        String storeFileName = null;
        String subName = null;

        try {
            String responseFileName = response.getFileName();
            String appVersion = AppInfo.getInstance().getVersion();
            VersionData vd = dataSet.getVersionDataByVersion(appVersion);
            if (vd == null) {
                statusHandler.error("Unable to process file '"
                        + responseFileName + "': dataset '"
                        + dataSet.getDataSetName()
                        + "' not supported in version '" + appVersion + "'");
                return;
            }

            if (responseFileName != null) {
                // Write file to data store
                Path filePath = Paths.get(responseFileName);

                ServiceConfig serviceConfig = HarvesterServiceManager
                        .getInstance().getServiceConfig(ServiceType.PDA);
                storeFileName = serviceConfig.getConstantValue("FTPS_DROP_DIR")
                        + File.separator + dataSet.getProviderName()
                        + File.separator
                        + dataSet.getDataSetName().replaceAll(" +", "_")
                        + File.separator + filePath.getFileName();

                /*
                 * If we have a file at the location given in the response, then
                 * the file came from FTPS and we just need to move it to the
                 * correct directory. If not, these bytes came via the SBN and
                 * we need to uncompress and write the file.
                 */
                File ftpsFile = new File(responseFileName);
                if (!ftpsFile.exists()) {
                    ResponseProcessingUtilities.writeCompressedFile(
                            response.getFileBytes(), storeFileName);
                } else {
                    File storeFile = new File(storeFileName);
                    FileUtils.moveFile(ftpsFile, storeFile);
                }

                subName = response.getSubName();

                // Populate DecodeInfo for transfer
                DecodeInfo decodeInfo = new DecodeInfo();
                decodeInfo.setSubscriptionName(subName);
                decodeInfo.setSubscriptionOwner(subOwner);
                decodeInfo.setPathToFile(storeFileName);
                decodeInfo.setRouteId(vd.getRoute());

                // Send off to Ingest for processing
                statusHandler.info("Sending PDA retrieval file to Ingest: "
                        + storeFileName);
                EDEXUtil.getMessageProducer()
                        .sendAsync("Ingest.DataDelivery.Decode", decodeInfo);

            } else {
                statusHandler.warn(
                        "Unable to process file: null file name received.");
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to decode PDA file objects!", e);
        }
    }

}
