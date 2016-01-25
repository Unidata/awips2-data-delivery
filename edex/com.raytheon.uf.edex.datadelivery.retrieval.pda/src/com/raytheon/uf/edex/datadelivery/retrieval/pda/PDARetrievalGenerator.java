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
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 13, 2014 3120       dhladky     Initial creation
 * Sept 14, 2104 3121      dhladky     Sharpened Retrieval generation.
 * Sept 26, 2014 3127      dhladky     Adding geographic subsetting.
 * Jan 18, 2016  5260      dhladky     Testing changes.
 * Jan 20, 2016  5280      dhladky     removed FTPSURL from request URL.
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
        
        List<Retrieval> retrievals = new ArrayList<Retrieval>();
        @SuppressWarnings("unchecked")
        Subscription<Time, Coverage> sub = bundle.getSubscription();
        
        if (sub.getUrl() == null) {
            statusHandler
                    .info("Skipping subscription "
                            + sub.getName()
                            + " that is unfulfillable with the current metadata (null URL.)");
            return Collections.emptyList();
        }
        
        
        //TODO since we have no switches this is all meaningless right now
        Time subTime = sub.getTime();
        // Gets the most recent time, which is kept as the end time.
        Date endDate = subTime.getEnd();
        // create the request start and end times.
        subTime.setRequestEnd(endDate);
        Date requestStartDate = new Date(endDate.getTime());
        subTime.setRequestStart(requestStartDate);

        // with PDA it's one param at a time
        Parameter param = null;
        
        if (sub.getParameter() != null) {
            param = (Parameter) sub.getParameter().get(0);
        }
        
        Retrieval retrieval = getRetrieval(sub, bundle, param, subTime);
        retrievals.add(retrieval);
        
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
    private Retrieval getRetrieval(Subscription<Time, Coverage> sub, SubscriptionBundle bundle,
            Parameter param, Time time) {

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
                    "PDA retrieval does not support coverages/types of type:  "+cov.getClass().getName());
        }

        final ProviderType providerType = bundle.getProvider().getProviderType(
                bundle.getDataType());
        final String plugin = providerType.getPlugin();

        // Attribute processing
        RetrievalAttribute<Time, Coverage> att = new RetrievalAttribute<Time, Coverage>();
        att.setCoverage(cov);
 
        /*
         * Coverage and type processing.
         * 
         * PDA retrievals are a unique 2 step process. 1.) Request the
         * URL/filePath based on the metaDataID. 2.) Then FTPS the URL/filePath
         * from PDA.
         */
        // This is essentially the request!
        String filePath = getCoverageUrl(sub, time.getRequestStart(), att);
        
        if (filePath != null) {
            statusHandler.handle(Priority.INFO, "Dataset file path: " + filePath);
            retrieval.getConnection().setUrl(filePath);
        } else {
            throw new IllegalArgumentException("PDA dataset filePath query failed!");
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
    protected Subscription<Time, Coverage> removeDuplicates(Subscription<Time, Coverage> sub) {
        throw new UnsupportedOperationException("Not implemented for PDA at this time!");
    }
    
    /**
     * Retrieve the Coverage URL for this Retrieval Attribute
     * 
     * @param sub
     * @param dataSetTime
     * @param ra
     * @return String (Filepath on PDA)
     */
    private String getCoverageUrl(Subscription<Time, Coverage> sub,
            Date dataSetTime, RetrievalAttribute<Time, Coverage> ra) {

        String filePath = null;
        String metaDataKey = null;
        PDASubsetRequest request = null;

        try {
                    
            statusHandler.handle(Priority.INFO,
                    "Time of Subset request: " + dataSetTime.toString());

            /** This query is broken temporarily in 16.2.1.  REMOVE FOR 16.2.2
            PDADataSetMetaData pdadsmd = (PDADataSetMetaData) DataDeliveryHandlers
                    .getDataSetMetaDataHandler().getByDataSetDate(
                            sub.getDataSetName(), sub.getProvider(),
                            dataSetTime);
            */
            PDADataSetMetaData pdadsmd = (PDADataSetMetaData) DataDeliveryHandlers
                   .getDataSetMetaDataHandler().getMostRecentDataSetMetaData(sub.getDataSetName(), sub.getProvider());
            
            if (pdadsmd != null) {
                statusHandler.handle(Priority.INFO, "DataSetMetaData: " + pdadsmd.getDataSetDescription());
                statusHandler.handle(Priority.INFO, "MetaDataID: " + pdadsmd.getMetaDataID()); 
            } else {
                throw new IllegalArgumentException("No DataSetMetaData matches query criteria!");
            }
            
            metaDataKey = pdadsmd.getMetaDataID();
            request = new PDASubsetRequest(ra, metaDataKey);
          
            // make the request
            filePath = request.performRequest();

        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Could not perform dataset request: " + metaDataKey + " : "
                            + sub.getName(), e);
        }

        return filePath;
    }

}
