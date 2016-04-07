package com.raytheon.uf.edex.datadelivery.retrieval;

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

import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.PendingSubscription;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionBundle;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval.SubscriptionType;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig.RETRIEVAL_MODE;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.ServiceTypeFactory;

/**
 * Generate Retrieval
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 07, 2012            dhladky      Initial creation
 * Jul 25, 2012 955        djohnson     Use {@link ServiceTypeFactory}.
 * Nov 19, 2012 1166       djohnson     Clean up JAXB representation of registry objects.
 * Nov 26, 2012 1340       dhladky      Recognize type of subscriptions for statistics.
 * Sept 30, 2013 1797      dhladky      Made some of the retrieval process flow generic.
 * Jan 18, 2016 5260       dhladky      Exposed access to serviceConfig.
 * Apr 06, 2016 5424       dhladky      Added Retrieval Mode constant, subRetrievalKey
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */
public abstract class RetrievalGenerator<T extends Time, C extends Coverage> {

       
    /** retrieval mode constant from config **/
    public final static String RETRIEVAL_MODE_CONSTANT = "RETRIEVAL_MODE";
    
    private final ServiceType serviceType;

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(RetrievalGenerator.class);
    
    private static ServiceConfig serviceConfig;

    public RetrievalGenerator(ServiceType serviceType) {
        this.serviceType = serviceType;
    }
 
    /**
     * Gets the service adapter.
     * 
     * @param serviceType
     * @return
     */
    protected ServiceType getServiceType() {
        return serviceType;
    }

    /***
     * Build the necessary retrieval objects
     * 
     * @param bundle
     * @return
     * @return
     */
    public abstract List<Retrieval> buildRetrieval(SubscriptionBundle bundle);

    public abstract RetrievalAdapter<T, C> getServiceRetrievalAdapter();

    /**
     * Check for duplicates;
     * 
     * @return
     */
    protected abstract Subscription<T, C> removeDuplicates(Subscription<T, C> sub);

    /**
     * Gets the type of subscription based on the subscription object type
     * 
     * @param sub
     * @return
     */
    public SubscriptionType getSubscriptionType(Subscription<T, C> sub) {
        
        if (sub instanceof AdhocSubscription) {
            return SubscriptionType.AD_HOC;
        } else if (sub instanceof Subscription) {
            return SubscriptionType.SUBSCRIBED;
        } else if (sub instanceof PendingSubscription) {
            // I don't really think we'll use this but......
            return SubscriptionType.PENDING;
        } else {
            statusHandler.error("Unknown Type of subscription! "
                    + sub.getName());
            return null;
        }
    }
    
    /**
     * clone the param
     * 
     * @param original
     *            parameter
     * @return
     */
    protected Parameter processParameter(Parameter origParm) {

        Parameter param = new Parameter();
        param.setName(origParm.getName());
        param.setBaseType(origParm.getBaseType());
        param.setDataType(origParm.getDataType());
        param.setDefinition(origParm.getDefinition());
        param.setFillValue(origParm.getFillValue());
        param.setLevelType(origParm.getLevelType());
        param.setMissingValue(origParm.getMissingValue());
        param.setProviderName(origParm.getProviderName());
        param.setUnits(origParm.getUnits());

        return param;

    }

    /**
     * Get the instance of the service config
     * 
     * @return
     */
    protected ServiceConfig getServiceConfig() {

        if (serviceConfig == null) {
            serviceConfig = HarvesterServiceManager.getInstance()
                    .getServiceConfig(getServiceType());
        }

        return serviceConfig;
    }

    /**
     * Returns the Retrieval Mode for the retrievals generated here. This can be
     * configured specific to each provider. Defaults to SYNC
     * 
     * @return RETRIEVAL_MODE
     */
    protected RETRIEVAL_MODE getRetrievalMode() {

        RETRIEVAL_MODE mode = RETRIEVAL_MODE.SYNC;

        ServiceConfig sc = getServiceConfig();

        if (sc.getConstantValue(RETRIEVAL_MODE_CONSTANT) != null) {
            String mode_constant = sc.getConstantValue(RETRIEVAL_MODE_CONSTANT);
            mode = RETRIEVAL_MODE.valueOf(mode_constant);
        }

        return mode;
    }

}
