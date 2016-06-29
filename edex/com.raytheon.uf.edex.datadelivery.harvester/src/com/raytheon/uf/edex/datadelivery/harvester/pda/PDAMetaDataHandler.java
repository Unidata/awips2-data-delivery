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
package com.raytheon.uf.edex.datadelivery.harvester.pda;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import net.opengis.cat.csw.v_2_0_2.AbstractRecordType;
import net.opengis.cat.csw.v_2_0_2.BriefRecordType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType;

import org.apache.commons.io.FileDeleteStrategy;

import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfigurationManager;
import com.raytheon.uf.common.datadelivery.harvester.PDAAgent;
import com.raytheon.uf.common.datadelivery.harvester.PDACatalogServiceResponseWrapper;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.ProviderHandler;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.datadelivery.harvester.MetaDataHandler;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IServiceFactory;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.ServiceTypeFactory;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.PDAMetaDataParser;
import com.raytheon.uf.edex.ogc.common.jaxb.OgcJaxbManager;

/**
 * Harvest PDA MetaData
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 13, 2014  3120      dhladky     Initial creation
 * Oct 14, 2014  3127      dhladky     Improved deletion of used files.
 * Apr 21, 2015  4435      dhladky     PDA transaction processing.
 * Feb 16, 2016  5365      dhladky     Streamlined to only process metadata updates with transactions.
 * Mar 16, 2016  3919      tjensen     Cleanup unneeded interfaces
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDAMetaDataHandler extends MetaDataHandler {

    OgcJaxbManager jaxbManager = null;

    private static final String fileExtension = ".xml";

    /** CSW class factory **/
    private static final Class<?>[] classes = new Class<?>[] {
            net.opengis.cat.csw.v_2_0_2.ObjectFactory.class,
            net.opengis.gml.v_3_1_1.ObjectFactory.class,
            net.opengis.filter.v_1_1_0.ObjectFactory.class };

    public PDAMetaDataHandler(ProviderHandler providerHandler) {
        this.providerHandler = providerHandler;
    }

    /**
     * Process PDA metadata records
     * 
     * @param bytes
     * @throws IOException
     */
    @SuppressWarnings("rawtypes")
    public void processFile(byte[] bytes) throws IOException {

        GetRecordsResponseType briefRecords = null;
        String filePath = "unknown";
        String fileName = null;
        File directory = null;
        HarvesterConfig config = HarvesterConfigurationManager
                .getPDAConfiguration();
        PDAAgent agent = (PDAAgent) config.getAgent();
        Provider provider = config.getProvider();
        @SuppressWarnings("unchecked")
        IServiceFactory<BriefRecordType, PDAMetaDataParser, Time, Coverage> serviceFactory = ServiceTypeFactory
                .retrieveServiceFactory(config.getProvider());
        Date lastDate = TimeUtil.newGmtCalendar().getTime();

        try {
            PDACatalogServiceResponseWrapper wrapper = SerializationUtil
                    .transformFromThrift(
                            PDACatalogServiceResponseWrapper.class, bytes);
            filePath = wrapper.getFilePath();

            /*
             * this filePath is actually a directory when pulled by camel
             * extract the actual file
             */
            if (filePath != null) {
                directory = new File(filePath);
                if (directory.isDirectory()) {
                    for (File file : directory.listFiles()) {
                        if (file.getName().endsWith(fileExtension)) {
                            /*
                             * you will only ever find one file with .xml
                             * extension.
                             */
                            fileName = file.getAbsolutePath();
                            break;
                        }
                    }
                }
            }

            if (fileName != null) {
                statusHandler.info("Processing PDA file " + fileName
                        + " for MetaData.....");

                briefRecords = (GetRecordsResponseType) getJaxbManager()
                        .unmarshalFromInputStream(new FileInputStream(fileName));
            } else {
                statusHandler.info("No Files available to Process...");
            }

        } catch (SerializationException e) {
            statusHandler
                    .handle(Priority.ERROR,
                            "Couldn't deserialize PDACatalogServiceResponseWrapper!",
                            e);
        } catch (IOException e) {
            statusHandler.handle(Priority.ERROR, "Couldn't find or read file! "
                    + fileName, e);
        }

        if (briefRecords != null) {
            // Make a parser
            PDAMetaDataParser parser = (PDAMetaDataParser) serviceFactory
                    .getParser(lastDate);
            // extract brief record(s) and send to parser
            List<JAXBElement<? extends AbstractRecordType>> briefs = briefRecords
                    .getSearchResults().getAbstractRecord();
            int count = 0;
            boolean parsed = true;

            for (JAXBElement<? extends AbstractRecordType> brief : briefs) {

                BriefRecordType record = (BriefRecordType) brief.getValue();

                try {
                    // false, only parse dataset, parameter, and datasetname info for getRecords()
                    parser.parseMetaData(provider, agent.getDateFormat(),
                            record, false);
                    count++;
                } catch (Exception e) {
                    statusHandler.handle(Priority.ERROR,
                            "Couldn't parse metadata! "
                                    + record.getTitle().toString(), e);
                    parsed = false;
                }
            }

            statusHandler.info("Parsed and stored  " + count
                    + " metadata definitions from file:" + fileName);

            // If parse was completely successful, cleanup file directory when
            // finished with it. No longer needed.
            if (parsed) {
                File file = new File(fileName);
                if (file.exists()) {
                    File dir = new File(file.getParent());
                    if (dir.isDirectory()) {
                        // force delete it, no excuses.
                        statusHandler
                                .info("Deleting processed metadata file directory. "
                                        + dir.getName());
                        FileDeleteStrategy.FORCE.delete(dir);
                    }
                }
            }
        }
    }

    /**
     * Process PDA transaction based messages
     * 
     * @param bytes
     */
    @SuppressWarnings("rawtypes")
    public void processTransaction(byte[] bytes) {

        if (bytes != null) {

            BriefRecordType briefRecord = null;
            HarvesterConfig config = HarvesterConfigurationManager
                    .getPDAConfiguration();
            PDAAgent agent = (PDAAgent) config.getAgent();
            Provider provider = config.getProvider();
            @SuppressWarnings("unchecked")
            IServiceFactory<BriefRecordType, PDAMetaDataParser, Time, Coverage> serviceFactory = ServiceTypeFactory
                    .retrieveServiceFactory(config.getProvider());
            Date lastDate = TimeUtil.newGmtCalendar().getTime();

            // Make a parser
            PDAMetaDataParser parser = (PDAMetaDataParser) serviceFactory
                    .getParser(lastDate);

            try {
                // true, parse for metaData updates in transactions.
                briefRecord = (BriefRecordType) getJaxbManager()
                        .unmarshalFromXml(new String(bytes));
                parser.parseMetaData(provider, agent.getDateFormat(),
                        briefRecord, true);

            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to process PDA transaction!", e);
            }
        }
    }

    /**
     * Gets the JAXB manager for use with BriefRecord decode
     * 
     * @return
     */
    private OgcJaxbManager getJaxbManager() {

        if (jaxbManager == null) {
            try {
                this.jaxbManager = new OgcJaxbManager(classes);
            } catch (JAXBException e) {
                statusHandler
                        .handle(Priority.PROBLEM,
                                "JaxbManager failed to initialize, can not deserialize CSW classes.",
                                e);
            }
        }

        return jaxbManager;
    }

}
