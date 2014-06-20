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
import java.util.Date;

import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IExtractMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IParseMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IServiceFactory;
import com.raytheon.uf.edex.datadelivery.retrieval.pda.PDAMetaDataParser;

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

public class PDAServiceFactory implements IServiceFactory {

    
    private Provider provider;

    public PDAServiceFactory() {
    }
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.edex.datadelivery.retrieval.ServiceFactory#getExtractor()
     */
    @Override
    public IExtractMetaData getExtractor() {
        throw new UnsupportedOperationException("No Extractor Implemented");
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
        // pda doesn't care about the date
        return new PDAMetaDataParser();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RetrievalGenerator getRetrievalGenerator() {
        return new PDARetrievalGenerator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProvider(Provider provider) {
        this.provider = provider;
    }

}
