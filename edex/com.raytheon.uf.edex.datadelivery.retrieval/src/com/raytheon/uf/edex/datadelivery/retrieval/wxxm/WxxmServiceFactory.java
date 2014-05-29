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
package com.raytheon.uf.edex.datadelivery.retrieval.wxxm;

import java.util.Date;

import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.edex.datadelivery.retrieval.IExtractMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.IParseMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.ServiceFactory;

/**
 * {@link ServiceFactory} implementation for WXXM.
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
public class WxxmServiceFactory implements ServiceFactory {

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.edex.datadelivery.retrieval.ServiceFactory#getExtractor()
     */
    @Override
    public IExtractMetaData getExtractor() {
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
    public IParseMetaData getParser(Date lastUpdate) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RetrievalGenerator getRetrievalGenerator() {
        return new WXXMRetrievalGenerator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProvider(Provider provider) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
