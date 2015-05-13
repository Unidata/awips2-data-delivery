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
package com.raytheon.uf.edex.datadelivery.retrieval.wcs;

import java.util.Date;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IExtractMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IParseMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IServiceFactory;

/**
 * {@link IServiceFactory} implementation for WCS.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 25, 2012 955        djohnson     Initial creation
 * May 31, 2013 2038       djohnson     Add setProvider.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
public class WCSServiceFactory<O extends Object, D extends Object>
        implements IServiceFactory<O, D, GriddedTime, GriddedCoverage> {

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.edex.datadelivery.retrieval.ServiceFactory#getExtractor()
     */
    @Override
    public IExtractMetaData<O, D> getExtractor() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.edex.datadelivery.retrieval.ServiceFactory#getParser(
     * java.util.Date)
     */
    @Override
    public IParseMetaData<O> getParser(Date lastUpdate) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.edex.datadelivery.retrieval.ServiceFactory#
     * getRetrievalGenerator()
     */
    @Override
    public RetrievalGenerator<GriddedTime, GriddedCoverage> getRetrievalGenerator() {
        return new WCSRetrievalGenerator();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProvider(Provider provider) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Provider getProvider() {
        return null;
    }

}
