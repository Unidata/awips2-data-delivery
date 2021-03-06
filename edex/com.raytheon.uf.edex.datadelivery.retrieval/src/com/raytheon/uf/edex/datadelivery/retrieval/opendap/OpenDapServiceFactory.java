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
package com.raytheon.uf.edex.datadelivery.retrieval.opendap;

import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IExtractMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IParseMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IServiceFactory;

import opendap.dap.DAS;

/**
 * Implementation of {@link IServiceFactory} that handles OpenDAP. This should
 * be the ONLY non package-private class in this entire package.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 24, 2012 955        djohnson     Initial creation
 * Feb 07, 2013 1543       djohnson     Allow sub-classes.
 * May 31, 2013 2038       djohnson     Add setProvider.
 * Apr 14, 2015 4400       dhladky      Upgraded to DAP2 protocol
 *
 * </pre>
 *
 * @author djohnson
 */

public class OpenDapServiceFactory
        implements IServiceFactory<String, DAS, GriddedTime, GriddedCoverage> {

    private static final OpenDAPMetaDataParser PARSER = new OpenDAPMetaDataParser();

    private Provider provider;

    public OpenDapServiceFactory() {
    }

    /**
     * Retrieve the metadata extractor.
     *
     * @return the metadata extractor
     */
    public IExtractMetaData<String, DAS> getExtractor() {
        return new OpenDAPMetaDataExtractor(provider.getConnection());
    }

    @Override
    public IParseMetaData getParser() {
        return PARSER;
    }

    @Override
    public RetrievalGenerator<GriddedTime, GriddedCoverage> getRetrievalGenerator() {
        return new OpenDAPRetrievalGenerator();
    }

    /**
     * Set the provider.
     *
     * @param provider
     *            the provider
     */
    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    /**
     * Get the provider for this service
     *
     * @return
     */
    public Provider getProvider() {
        return provider;
    }
}
