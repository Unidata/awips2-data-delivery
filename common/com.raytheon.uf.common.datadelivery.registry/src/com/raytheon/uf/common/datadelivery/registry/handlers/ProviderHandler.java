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
package com.raytheon.uf.common.datadelivery.registry.handlers;

import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.ebxml.ProviderQuery;
import com.raytheon.uf.common.registry.RegistryQueryResponse;
import com.raytheon.uf.common.registry.handler.BaseRegistryObjectHandler;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;

/**
 * Registry handler for providers.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 03, 2012 1241       djohnson     Initial creation
 * Jun 24, 2013 2106       djohnson     Now composes a registryHandler.
 * Mar 16, 2016 3919       tjensen      Cleanup unneeded interfaces
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class ProviderHandler extends
        BaseRegistryObjectHandler<Provider, ProviderQuery> {

    /**
     * Retrieve a {@link Provider} by its name.
     * 
     * @param providerName
     *            the name of the provider
     * @return the provider, or null if the provider can't be found
     * @throws RegistryHandlerException
     *             on error
     */
    public Provider getByName(String providerName)
            throws RegistryHandlerException {
        ProviderQuery gQuery = getQuery();
        gQuery.setProviderName(providerName);

        RegistryQueryResponse<Provider> response = registryHandler
                .getObjects(gQuery);

        checkResponse(response, "getByName");

        return response.getSingleResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<Provider> getRegistryObjectClass() {
        return Provider.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ProviderQuery getQuery() {
        return new ProviderQuery();
    }

}
