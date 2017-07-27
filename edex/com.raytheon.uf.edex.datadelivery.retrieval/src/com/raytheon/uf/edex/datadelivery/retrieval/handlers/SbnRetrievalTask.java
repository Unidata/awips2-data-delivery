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
package com.raytheon.uf.edex.datadelivery.retrieval.handlers;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.common.util.rate.TokenBucket;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.opendap.OpenDapRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.PDARetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.wfs.WFSRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.xml.legacy.SbnRetrievalResponseXml;

/**
 * RetrievalTask that serializes the retrieved data to a directory for broadcast
 * over SBN.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 01, 2013  1543     djohnson  Initial creation
 * Feb 15, 2013  1543     djohnson  Serialize data out as XML.
 * Mar 05, 2013  1647     djohnson  Apply WMO header.
 * Mar 07, 2013  1647     djohnson  Write out as hidden file, then rename.
 * Aug 09, 2013  1822     bgonzale  Added parameters to
 *                                  IWmoHeaderApplier.applyWmoHeader().
 * Oct 28, 2013  2506     bgonzale  Removed request parameters.  Constructor
 *                                  inject IRetrievalDao.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Mar 20, 2017  6089     tjensen   Add PDARetrievalResponse to JAXBManager
 *
 * </pre>
 *
 * @author djohnson
 */
public class SbnRetrievalTask extends RetrievalTask {

    private final JAXBManager jaxbManager;

    private final File targetDirectory;

    private final IWmoHeaderApplier wmoHeaderWrapper;

    /**
     * @param directory
     */
    public SbnRetrievalTask(TokenBucket tokenBucket, File directory,
            IWmoHeaderApplier wmoHeaderWrapper) {
        super(tokenBucket);
        this.targetDirectory = directory;
        this.wmoHeaderWrapper = wmoHeaderWrapper;
        try {
            this.jaxbManager = new JAXBManager(SbnRetrievalResponseXml.class,
                    OpenDapRetrievalResponse.class, WFSRetrievalResponse.class,
                    PDARetrievalResponse.class, Coverage.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public boolean processRetrievedData(Retrieval retrieval,
            IRetrievalResponse retrievalPluginDataObjects) throws Exception {
        try {
            final String fileName = UUID.randomUUID().toString();
            final File finalFile = new File(targetDirectory, fileName);
            final File tempHiddenFile = new File(finalFile.getParentFile(),
                    "." + finalFile.getName());

            // generate SbnRetrievalResponseXml
            final String xml = jaxbManager
                    .marshalToXml(new SbnRetrievalResponseXml(retrieval,
                            retrievalPluginDataObjects, true));
            final Date date = TimeUtil.newDate();
            final String textForFile = wmoHeaderWrapper.applyWmoHeader(
                    retrieval.getProvider(), retrieval.getPlugin(),
                    getSourceType(retrieval.getProvider()), date, xml);

            // Write as hidden file, this is OS specific, but there is no
            // platform-neutral way to do this with Java
            FileUtil.bytes2File(textForFile.getBytes(), tempHiddenFile);

            // Rename to non-hidden
            if (!tempHiddenFile.renameTo(finalFile)) {
                throw new IOException("Unable to rename hidden file ["
                        + tempHiddenFile.getAbsolutePath() + "] to ["
                        + finalFile.getAbsolutePath() + "]");
            }

            return true;
        } catch (Exception e) {
            throw new Exception(
                    "Error occurred writing retrieval data to directory for retrieval["
                            + retrieval + "]",
                    e);
        }
    }

    /**
     * Determine source type from the request.
     *
     * TODO Simple method that is adequate for now. It will need to be updated
     * for new data from NOMADS, MADIS, and PDA.
     *
     * @return source type string ("MODEL", "OBSERVATION", or "SATELLITE")
     */
    private String getSourceType(String provider) {
        // Default to MODEL
        String sourceType = "MODEL";
        if ("MADIS".equals(provider)) {
            sourceType = "OBSERVATION";
        } else if ("PDA".equals(provider)) {
            sourceType = "SATELLITE";
        }

        return sourceType;
    }
}
