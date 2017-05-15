package com.raytheon.uf.edex.datadelivery.retrieval.wcs;

import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionBundle;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;

/**
 *
 * WCS Retrieval Generator.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 25, 2012  955      djohnson  Moved to wcs specific package.
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Apr 20, 2017  6186     rjpeter   Update buildRetrieval signature.
 *
 * </pre>
 *
 * @author djohnson
 */
class WCSRetrievalGenerator
        extends RetrievalGenerator<GriddedTime, GriddedCoverage> {

    public WCSRetrievalGenerator() {
        super(ServiceType.WCS);
    }

    @Override
    public List<Retrieval> buildRetrieval(
            DataSetMetaData<GriddedTime, GriddedCoverage> dsmd,
            SubscriptionBundle bundle) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public RetrievalAdapter<GriddedTime, GriddedCoverage> getServiceRetrievalAdapter() {
        return new WCSRetrievalAdapter();
    }
}
