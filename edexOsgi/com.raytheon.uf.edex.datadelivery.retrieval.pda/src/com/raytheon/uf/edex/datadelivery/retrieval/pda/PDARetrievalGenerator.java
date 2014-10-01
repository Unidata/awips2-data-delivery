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
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.ProviderType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionBundle;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
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
        // this is the filepath in PDA retrievals, this is essentially the request!
        retrieval.getConnection().setUrl(sub.getUrl());
        retrieval.setOwner(sub.getOwner());
        retrieval.setSubscriptionType(getSubscriptionType(sub));
        retrieval.setNetwork(sub.getRoute());

        // Coverage and type processing
        Coverage cov = sub.getCoverage();

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

}
