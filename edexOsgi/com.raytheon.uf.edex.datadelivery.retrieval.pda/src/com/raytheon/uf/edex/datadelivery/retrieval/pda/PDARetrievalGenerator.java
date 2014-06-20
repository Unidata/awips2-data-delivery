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
import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionBundle;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
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
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDARetrievalGenerator extends RetrievalGenerator {

    public PDARetrievalGenerator(ServiceType serviceType) {
        super(serviceType);
    }
    
    public PDARetrievalGenerator() {
        super(ServiceType.FTPS);
    }

    @Override
    public List<Retrieval> buildRetrieval(SubscriptionBundle bundle) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RetrievalAdapter getServiceRetrievalAdapter() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Subscription<?, ?> removeDuplicates(Subscription<?, ?> sub) {
        // TODO Auto-generated method stub
        return null;
    }

}
