
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
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.util.app.AppInfo;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalTranslator;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ResponseProcessingUtilities;
import com.raytheon.uf.edex.plugin.datadelivery.retrieval.dist.DecodeInfo;

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
 * Jul 25, 2017  6186     rjpeter   Use Retrieval
 * Nov 15, 2017  6498     tjensen   Use inherited logger for logging
 *
 * </pre>
 *
 * @author dhladky
 */

public class PDATranslator
        extends RetrievalTranslator<Time, Coverage, PluginDataObject> {

    public PDATranslator(Retrieval<Time, Coverage> retrieval)
            throws InstantiationException {
        super(retrieval);
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
    public void storeAndProcess(PDARetrievalResponse response,
            DataSet dataSet) {
        String storeFileName = null;

        try {
            String responseFileName = response.getFileName();
            String appVersion = AppInfo.getInstance().getVersion();
            VersionData vd = dataSet.getVersionDataByVersion(appVersion);
            if (vd == null) {
                logger.error("Unable to process file '" + responseFileName
                        + "': dataset '" + dataSet.getDataSetName()
                        + "' not supported in version '" + appVersion + "'");
                return;
            }

            if (responseFileName != null) {
                // Write file to data store
                Path filePath = Paths.get(responseFileName);

                ServiceConfig serviceConfig = HarvesterServiceManager
                        .getInstance().getServiceConfig(ServiceType.PDA);
                storeFileName = System.getProperty("DATA_ARCHIVE_ROOT")
                        + File.separator
                        + serviceConfig.getConstantValue("FTPS_DROP_DIR")
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
                    // If file already exist at the destination, remove it first
                    if (storeFile.exists()) {
                        FileUtils.deleteQuietly(storeFile);
                    }
                    FileUtils.moveFile(ftpsFile, storeFile);
                }

                // Populate DecodeInfo for transfer
                DecodeInfo decodeInfo = new DecodeInfo();
                decodeInfo.setSubscriptionName(retrieval.getSubscriptionName());
                decodeInfo.setSubscriptionOwner(retrieval.getOwner());
                decodeInfo.setPathToFile(storeFileName);
                decodeInfo.setRouteId(vd.getRoute());
                decodeInfo.setDataType("PDA");
                decodeInfo.setEnqueTime(System.currentTimeMillis());

                // Send off to Ingest for processing
                logger.info("Sending PDA retrieval file to Ingest: "
                        + storeFileName);
                EDEXUtil.getMessageProducer().sendAsyncUri(
                        "jms-durable:queue:Ingest.DataDelivery.Decode",
                        SerializationUtil.transformToThrift(decodeInfo));

            } else {
                logger.error(
                        "Unable to process file: null file name received.");
            }
        } catch (Exception e) {
            logger.error("Unable to decode PDA file objects for DataSet '"
                    + dataSet.getDataSetName() + "' for subscription '"
                    + retrieval.getSubscriptionName() + "'!", e);
        }
    }

}
