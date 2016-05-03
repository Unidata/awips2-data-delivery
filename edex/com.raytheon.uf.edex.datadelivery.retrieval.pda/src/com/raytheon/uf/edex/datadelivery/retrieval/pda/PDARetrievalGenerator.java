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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.PDADataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.ProviderType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionBundle;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig.RETRIEVAL_MODE;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
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
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDARetrievalGenerator extends RetrievalGenerator<Time, Coverage> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDARetrievalGenerator.class);

    public PDARetrievalGenerator(ServiceType serviceType) {
        super(serviceType);
    }

    public PDARetrievalGenerator() {

        super(ServiceType.PDA);
    }

    @Override
    public List<Retrieval> buildRetrieval(SubscriptionBundle bundle) {

        List<Retrieval> retrievals = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Subscription<Time, Coverage> sub = bundle.getSubscription();

        if (sub.getUrl() == null) {
            statusHandler
                    .info("Skipping subscription "
                            + sub.getName()
                            + " that is unfulfillable with the current metadata (null URL.)");
            return Collections.emptyList();
        }

        // TODO since we have no switches this is all meaningless right now
        Time subTime = sub.getTime();
        // Gets the most recent time, which is kept as the end time.
        Date endDate = subTime.getEnd();
        // create the request start and end times.
        subTime.setRequestEnd(endDate);
        Date requestStartDate = new Date(endDate.getTime());
        subTime.setRequestStart(requestStartDate);

        // With PDA it's one param at a time. so always 0.
        Parameter param = null;

        if (sub.getParameter() != null) {
            param = (Parameter) sub.getParameter().get(0);
        }

        try {
            Retrieval retrieval = getRetrieval(sub, bundle, param, subTime);
            retrievals.add(retrieval);
        } catch (Exception e) {
            statusHandler.error("PDA Retrieval building has failed." + e);
            return Collections.emptyList();
        }

        return retrievals;
    }

    /**
     * Get the retrieval
     * 
     * @param sub
     * @param bundle
     * @param param
     * @param level
     * @param Time
     * @return
     */
    private Retrieval getRetrieval(Subscription<Time, Coverage> sub,
            SubscriptionBundle bundle, Parameter param, Time time)
            throws Exception {

        Retrieval retrieval = new Retrieval();
        retrieval.setSubscriptionName(sub.getName());
        retrieval.setServiceType(getServiceType());
        retrieval.setConnection(bundle.getConnection());
        retrieval.setOwner(sub.getOwner());
        Coverage cov = sub.getCoverage();
        // null out the URL, we will request the actual one.
        retrieval.getConnection().setUrl(null);

        if (cov instanceof Coverage) {
            retrieval.setDataType(DataType.PDA);
        } else {
            throw new UnsupportedOperationException(
                    "PDA retrieval does not support coverages of type:  "
                            + cov.getClass().getName());
        }

        final ProviderType providerType = bundle.getProvider().getProviderType(
                bundle.getDataType());
        final String plugin = providerType.getPlugin();

        // Attribute processing
        RetrievalAttribute<Time, Coverage> att = new RetrievalAttribute<>();
        att.setCoverage(cov);

        /**
         * Build RetrievalRecordPK key, PDA processes one at a time. So the
         * index will always be just zero.
         */
        int index = 0;
        String retrievalID = sub.getName() + "/" + index;
        String coverageReturn = null;

        try {
            coverageReturn = getCoverageUrl(sub, time.getRequestStart(), att,
                    retrievalID);
        } catch (Exception e) {
            throw new Exception(
                    "PDA dataset coverage query failed! Subscription: "
                            + sub.getName() + "DataSet: "
                            + sub.getDataSetName() + " RetrievalID: "
                            + retrievalID, e);
        }

        if (coverageReturn != null) {
            statusHandler.handle(Priority.INFO, "Coverage response: "
                    + coverageReturn);
            /**
             * Sets for async as well as sync, just a placeholder for async. It
             * will be over written with the real value when the async response
             * is returned.
             */
            retrieval.getConnection().setUrl(coverageReturn);
        }

        retrieval.setSubscriptionType(getSubscriptionType(sub));
        retrieval.setNetwork(sub.getRoute());

        if (param != null) {

            Parameter lparam = processParameter(param);
            lparam.setLevels(param.getLevels());
            att.setParameter(lparam);
        }

        att.setTime(time);
        att.setSubName(retrieval.getSubscriptionName());
        att.setPlugin(plugin);
        att.setProvider(sub.getProvider());
        retrieval.addAttribute(att);

        return retrieval;
    }

    @Override
    public RetrievalAdapter<Time, Coverage> getServiceRetrievalAdapter() {
        return new PDARetrievalAdapter();
    }

    @Override
    protected Subscription<Time, Coverage> removeDuplicates(
            Subscription<Time, Coverage> sub) {
        throw new UnsupportedOperationException(
                "Not implemented for PDA at this time!");
    }

    /**
     * Retrieve the Coverage for this Retrieval Attribute
     * 
     * @param sub
     * @param dataSetTime
     * @param ra
     * @param retrievalID
     * @return String (Filepath or Message)
     * @throws Exception
     */
    private String getCoverageUrl(Subscription<Time, Coverage> sub,
            Date dataSetTime, RetrievalAttribute<Time, Coverage> ra,
            String retrievalID) throws Exception {

        String retVal = null;
        String metaDataKey = null;
        PDAAbstractRequestBuilder request = null;

        statusHandler.handle(Priority.INFO, "Time of Subset request: "
                + dataSetTime.toString());
        PDADataSetMetaData pdadsmd = (PDADataSetMetaData) DataDeliveryHandlers
                .getDataSetMetaDataHandler().getMostRecentDataSetMetaData(
                        sub.getDataSetName(), sub.getProvider());

        if (pdadsmd != null) {
            statusHandler.handle(Priority.INFO,
                    "DataSetMetaData: " + pdadsmd.getDataSetDescription());
            statusHandler.handle(Priority.INFO,
                    "MetaDataID: " + pdadsmd.getMetaDataID());
        } else {
            throw new IllegalArgumentException(
                    "No DataSetMetaData matches query criteria!");
        }

        metaDataKey = pdadsmd.getMetaDataID();

        if (this.getRetrievalMode() == RETRIEVAL_MODE.SYNC) {
            request = new PDASyncRequest(ra, sub.getName(), metaDataKey);
        } else {
            request = new PDAAsyncRequest(ra, sub.getName(), metaDataKey,
                    retrievalID);
        }

        // Make the request then process the response.
        retVal = request.performRequest();

        return retVal;
    }

}
