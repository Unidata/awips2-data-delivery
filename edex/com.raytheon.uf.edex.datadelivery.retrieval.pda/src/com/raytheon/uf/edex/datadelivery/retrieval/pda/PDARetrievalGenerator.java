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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.PDADataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.ProviderType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionBundle;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig.RETRIEVAL_MODE;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecordPK;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IServiceFactory;

/**
 * {@link IServiceFactory} implementation for PDA.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------------------
 * Jun 13, 2014  3120     dhladky   Initial creation
 * Sep 14, 2104  3121     dhladky   Sharpened Retrieval generation.
 * Sep 26, 2014  3127     dhladky   Adding geographic subsetting.
 * Jan 18, 2016  5260     dhladky   Testing changes.
 * Jan 20, 2016  5280     dhladky   removed FTPSURL from request URL.
 * May 03, 2016  5599     tjensen   Added subscription name to PDA requests
 * Sep 01, 2016  5762     tjensen   Improved logging
 * Oct 06, 2016  5772     tjensen   Fix Adhocs for older times
 * Mar 31, 2017  6186     rjpeter   Update to handle passed in DataSetMetaData.
 *
 * </pre>
 *
 * @author dhladky
 */
public class PDARetrievalGenerator extends RetrievalGenerator<Time, Coverage> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PDARetrievalGenerator(ServiceType serviceType) {
        super(serviceType);
    }

    public PDARetrievalGenerator() {

        super(ServiceType.PDA);
    }

    @Override
    public List<Retrieval> buildRetrieval(DataSetMetaData<Time, Coverage> dsmd,
            SubscriptionBundle bundle) {
        try {
            PDADataSetMetaData pdadsmd = (PDADataSetMetaData) dsmd;
            @SuppressWarnings("unchecked")
            Subscription<Time, Coverage> sub = (Subscription<Time, Coverage>) bundle
                    .getSubscription();
            Coverage cov = sub.getCoverage();
            int index = 0;

            List<Parameter> parameters = sub.getParameter();
            if (CollectionUtil.isNullOrEmpty(parameters)) {
                logger.info(
                        "Subscription does not contain any parameters, skipping subscription: "
                                + sub.getName());
                return null;
            }

            Time time = pdadsmd.getTime();

            // With PDA it's one param at a time. so always 0.
            Parameter subParam = sub.getParameter().get(0);

            RetrievalAttribute<Time, Coverage> att = new RetrievalAttribute<>();
            Parameter lparam = processParameter(subParam);
            lparam.setLevels(subParam.getLevels());
            att.setParameter(lparam);
            att.setCoverage(cov);
            att.setTime(time);
            att.setSubName(sub.getName());
            final ProviderType providerType = bundle.getProvider()
                    .getProviderType(bundle.getDataType());
            att.setPlugin(providerType.getPlugin());
            att.setProvider(sub.getProvider());

            /*
             * Build RetrievalRecordPK key, PDA processes one at a time.
             */
            RetrievalRequestRecordPK retrievalId = new RetrievalRequestRecordPK();
            retrievalId.setUrl(pdadsmd.getUrl());
            retrievalId.setSubscriptionName(sub.getName());
            retrievalId.setIndex(index);
            String wcsDataURL = null;

            try {
                logger.info("Submitting WCS Request for DataSetMetaData: ["
                        + pdadsmd.getDataSetName() + "] MetaDataID: ["
                        + pdadsmd.getMetaDataID() + "] time ["
                        + time.getStart().toString() + "]");
                wcsDataURL = performWCSRequest(pdadsmd, sub, att, retrievalId);
            } catch (Exception e) {
                throw new Exception("PDA dataset WCS query failed! DataSet: "
                        + sub.getDataSetName() + " url: " + pdadsmd.getUrl()
                        + " subscriptionName: " + sub.getName(), e);
            }

            logger.info("PDA WCS response: " + wcsDataURL);
            Retrieval retrieval = new Retrieval();
            retrieval.setSubscriptionName(sub.getName());
            retrieval.setServiceType(getServiceType());
            retrieval.setConnection(bundle.getConnection());
            retrieval.setOwner(sub.getOwner());

            // null out the URL, we will request the actual one.
            retrieval.getConnection().setUrl(null);
            retrieval.setDataType(DataType.PDA);

            retrieval.setSubscriptionType(getSubscriptionType(sub));
            retrieval.setNetwork(sub.getRoute());
            retrieval.addAttribute(att);

            /*
             * Sets for async as well as sync, just a placeholder for async. It
             * will be over written with the real value when the async response
             * is returned.
             */
            retrieval.getConnection().setUrl(wcsDataURL);
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
     * Submit a WCS Request to PDA returning the webservice response.
     *
     * @param pdadsmd
     * @param sub
     * @param ra
     * @param retrievalID
     * @return String (Filepath or Message)
     * @throws Exception
     */
    private String performWCSRequest(PDADataSetMetaData pdadsmd,
            Subscription<Time, Coverage> sub,
            RetrievalAttribute<Time, Coverage> ra,
            RetrievalRequestRecordPK retrievalId) throws Exception {
        String metaDataKey = pdadsmd.getMetaDataID();
        PDAAbstractRequestBuilder request = null;

        /*
         * TODO: Shouldn't this execute the request on a retrieval thread
         * instead of on the generator thread
         */
        if (this.getRetrievalMode() == RETRIEVAL_MODE.SYNC) {
            request = new PDASyncRequest(ra, sub.getName(), metaDataKey);
        } else {
            request = new PDAAsyncRequest(ra, sub.getName(), metaDataKey,
                    retrievalId);
        }

        // Make the request then process the response.
        String retVal = request.performRequest();

        return retVal;
    }
}
