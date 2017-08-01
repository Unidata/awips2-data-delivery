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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.PDADataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.ProviderType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord.State;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IServiceFactory;

/**
 * {@link IServiceFactory} implementation for PDA.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 13, 2014  3120     dhladky   Initial creation
 * Sep 14, 2104  3121     dhladky   Sharpened Retrieval generation.
 * Sep 26, 2014  3127     dhladky   Adding geographic subsetting.
 * Jan 18, 2016  5260     dhladky   Testing changes.
 * Jan 20, 2016  5280     dhladky   removed FTPSURL from request URL.
 * May 03, 2016  5599     tjensen   Added subscription name to PDA requests
 * Sep 01, 2016  5762     tjensen   Improved logging
 * Oct 06, 2016  5772     tjensen   Fix Adhocs for older times
 * Mar 31, 2017  6186     rjpeter   Update to handle passed in DataSetMetaData.
 * Jun 29, 2017  6130     tjensen   Add support for local PDA testing and
 *                                  override getRetrievalMode
 * Jul 25, 2017  6186     rjpeter   Update to handle Retrieval refactor
 * Aug 02, 2017  6186     rjpeter   Get parameters from DSMD instead of sub.
 *
 * </pre>
 *
 * @author dhladky
 */
public class PDARetrievalGenerator extends RetrievalGenerator<Time, Coverage> {
    public PDARetrievalGenerator(ServiceType serviceType) {
        super(serviceType);
    }

    public PDARetrievalGenerator() {

        super(ServiceType.PDA);
    }

    @Override
    public List<Retrieval<Time, Coverage>> buildRetrieval(
            DataSetMetaData<Time, Coverage> dsmd,
            Subscription<Time, Coverage> sub, Provider provider) {
        try {
            PDADataSetMetaData pdadsmd = (PDADataSetMetaData) dsmd;
            Coverage cov = dsmd.getInstanceCoverage();

            if (cov == null) {
                cov = sub.getCoverage();
            }

            Map<String, Parameter> parameters = pdadsmd.getParameters();
            Time time = pdadsmd.getTime();

            // With PDA it's one param at a time. so always 0.
            Parameter subParam = parameters.values().iterator().next();

            Retrieval<Time, Coverage> retrieval = new Retrieval<>();
            retrieval.setSubscriptionName(sub.getName());
            retrieval.setServiceType(getServiceType());
            retrieval.setProvider(sub.getProvider());
            retrieval.setOwner(sub.getOwner());
            retrieval.setDataType(DataType.PDA);
            retrieval.setSubscriptionType(getSubscriptionType(sub));
            retrieval.setNetwork(sub.getRoute());

            final ProviderType providerType = provider
                    .getProviderType(sub.getDataSetType());
            retrieval.setPlugin(providerType.getPlugin());

            RetrievalAttribute<Time, Coverage> att = new RetrievalAttribute<>();
            Parameter lparam = processParameter(subParam);
            lparam.setLevels(subParam.getLevels());
            att.setParameter(lparam);
            att.setCoverage(cov);
            att.setTime(time);
            retrieval.setAttribute(att);
            return Arrays.asList(retrieval);
        } catch (Exception e) {
            logger.error("PDA Retrieval building has failed." + e);
        }

        return Collections.emptyList();
    }

    @Override
    public RetrievalAdapter<Time, Coverage> getServiceRetrievalAdapter() {
        return new PDARetrievalAdapter();
    }

    /**
     * Submit WCS requests for the retrievals.
     */
    @Override
    public List<RetrievalRequestRecord> postSaveActions(
            DataSetMetaData<Time, Coverage> dsmd,
            Subscription<Time, Coverage> sub,
            List<RetrievalRequestRecord> records) {
        PDADataSetMetaData pdaDsmd = (PDADataSetMetaData) dsmd;
        String metaDataKey = pdaDsmd.getMetaDataID();
        List<RetrievalRequestRecord> rval = new LinkedList<>();

        for (RetrievalRequestRecord record : records) {
            try {
                Retrieval retrieval = record.getRetrievalObj();

                if (Boolean
                        .parseBoolean(System.getProperty("LOCAL_DATA_TEST"))) {
                    // dataSetMetaData url is path to file
                    retrieval.setUrl(pdaDsmd.getUrl());
                    record.setState(State.PENDING);
                    record.setRetrievalObj(retrieval);
                    rval.add(record);
                } else {
                    /*
                     * Note DO NOT return and records on this path to avoid
                     * collision / overwrites with async response thread once
                     * wcs request submitted
                     */

                    /*
                     * TODO: move this to retrieval so that its not occurring on
                     * the generator thread and doesn't cloud the issue of dsmd
                     * url vs retrieval url
                     */
                    logger.info("Submitting WCS Request for Retrieval ["
                            + retrieval + "] MetaDataID: ["
                            + pdaDsmd.getMetaDataID() + "]");
                    PDARequestBuilder request = new PDARequestBuilder(retrieval,
                            metaDataKey, Integer.toString(record.getId()));

                    // Make the request then process the response.
                    String wcsDataURL = request.performRequest();
                    logger.info("PDA WCS response: " + wcsDataURL);

                    // TODO: Parse bad response and fail record immediately?
                }
            } catch (Exception e) {
                logger.error("PDA dataset WCS query failed! DataSet: "
                        + sub.getDataSetName() + " url: " + pdaDsmd.getUrl()
                        + " subscriptionName: " + sub.getName(), e);
                // TODO: Set SubscriptionNotifyTask for all failed
                record.setState(State.FAILED);
                rval.add(record);
            }

        }

        return rval;
    }

    @Override
    public RETRIEVAL_MODE getRetrievalMode() {
        // Default to ASYNC mode for PDA.
        RETRIEVAL_MODE mode = RETRIEVAL_MODE.ASYNC;

        // If doing local testing, use SYNC mode
        if (Boolean.parseBoolean(System.getProperty("LOCAL_DATA_TEST"))) {
            mode = RETRIEVAL_MODE.SYNC;
        }
        return mode;
    }
}
