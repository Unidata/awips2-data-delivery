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
package com.raytheon.uf.edex.datadelivery.retrieval;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval.SubscriptionType;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.db.RetrievalRequestRecord;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.ServiceTypeFactory;

/**
 * Generate Retrieval
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 07, 2012           dhladky   Initial creation
 * Jul 25, 2012  955      djohnson  Use {@link ServiceTypeFactory}.
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Nov 26, 2012  1340     dhladky   Recognize type of subscriptions for
 *                                  statistics.
 * Sep 30, 2013  1797     dhladky   Made some of the retrieval process flow
 *                                  generic.
 * Jan 18, 2016  5260     dhladky   Exposed access to serviceConfig.
 * Apr 06, 2016  5424     dhladky   Added Retrieval Mode constant,
 *                                  subRetrievalKey
 * Apr 20, 2017  6186     rjpeter   Updated buildRetrieval signature.
 * Jun 13, 2017  6204     nabowle   Use copy constructor in processParameter()
 * Jul 10, 2017  6130     tjensen   Update getRetrievalMode to not look at
 *                                  ServiceConfig
 * Jul 25, 2017  6186     rjpeter   Update signature
 * Aug 02, 2017  6186     rjpeter   Removed SubscriptionBundle
 * Sep 20, 2017  6413     tjensen   Update for ParameterGroups
 * Nov 22, 2017  6484     tjensen   Cleanup getSubscriptionType
 *
 * </pre>
 *
 * @author dhladky
 */
public abstract class RetrievalGenerator<T extends Time, C extends Coverage> {

    /**
     * RETRIEVAL_MODE
     */
    public enum RETRIEVAL_MODE {
        SYNC("SYNC"), ASYNC("ASYNC");

        private final String mode;

        private RETRIEVAL_MODE(String mode) {
            this.mode = mode;
        }

        public String getMode() {
            return mode;
        }
    }

    private final ServiceType serviceType;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private ServiceConfig serviceConfig;

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
     * @param dsmd
     * @param subscription
     * @param provider
     * @return
     */
    public abstract List<Retrieval<T, C>> buildRetrieval(
            DataSetMetaData<T, C> dsmd, Subscription<T, C> subscription,
            Provider provider);

    public abstract RetrievalAdapter<T, C> getServiceRetrievalAdapter();

    /**
     * Gets the type of subscription based on the subscription object type
     *
     * @param sub
     * @return
     */
    public SubscriptionType getSubscriptionType(Subscription<T, C> sub) {

        if (sub instanceof AdhocSubscription) {
            return SubscriptionType.AD_HOC;
        }
        return SubscriptionType.SUBSCRIBED;
    }

    /**
     * clone the param
     *
     * @param original
     *            parameter
     * @return
     */
    protected ParameterGroup processParameter(ParameterGroup origParm) {
        return new ParameterGroup(origParm);
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
     * Returns the Retrieval Mode for the retrievals generated here. Can be
     * overridden in other implementations. Defaults to SYNC.
     *
     * @return RETRIEVAL_MODE
     */
    public RETRIEVAL_MODE getRetrievalMode() {
        return RETRIEVAL_MODE.SYNC;
    }

    /**
     * Perform any post save actions on the RetrievalRequestRecord. Returns a
     * list of RetrievalRequestRecords that need to have updated persisted.
     *
     * @param dsmd
     * @param sub
     * @param records
     * @return
     */
    public List<RetrievalRequestRecord> postSaveActions(
            DataSetMetaData<T, C> dsmd, Subscription<T, C> sub,
            List<RetrievalRequestRecord> records) {
        return Collections.emptyList();
    }
}
