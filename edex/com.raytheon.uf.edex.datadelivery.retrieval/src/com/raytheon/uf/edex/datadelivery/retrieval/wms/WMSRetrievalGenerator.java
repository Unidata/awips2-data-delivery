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
package com.raytheon.uf.edex.datadelivery.retrieval.wms;

import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionBundle;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;

/**
 *
 * WMS Retrieval Generator.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 25, 2012  955      djohnson  Moved to wms specific package.
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Apr 20, 2017  6186     rjpeter   Update buildRetrieval signature.
 *
 * </pre>
 *
 * @author djohnson
 */
class WMSRetrievalGenerator<T extends Time, C extends Coverage>
        extends RetrievalGenerator<T, C> {

    WMSRetrievalGenerator() {
        super(ServiceType.WMS);
    }

    @Override
    public List<Retrieval> buildRetrieval(DataSetMetaData<T, C> dsmd,
            SubscriptionBundle bundle) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override

    public RetrievalAdapter<T, C> getServiceRetrievalAdapter() {
        return new WMSRetrievalAdapter<>();
    }
}
