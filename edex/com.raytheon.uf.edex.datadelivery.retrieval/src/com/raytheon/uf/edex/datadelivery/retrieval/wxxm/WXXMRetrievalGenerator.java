package com.raytheon.uf.edex.datadelivery.retrieval.wxxm;

import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionBundle;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;

/**
 * 
 * WXXM Retrieval Generator.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 25, 2012 955        djohnson     Moved to wxxm specific package.
 * Nov 19, 2012 1166       djohnson     Clean up JAXB representation of registry objects.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
class WXXMRetrievalGenerator<T extends Time, C extends Coverage> extends RetrievalGenerator<T, C> {

    WXXMRetrievalGenerator() {
        super(ServiceType.WXXM);
    }

    @Override
    public List<Retrieval> buildRetrieval(SubscriptionBundle bundle) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RetrievalAdapter<T, C> getServiceRetrievalAdapter() {
        return new WXXMRetrievalAdapter<T, C>();
    }

    @Override
    protected Subscription<T, C> removeDuplicates(Subscription<T, C> sub) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
