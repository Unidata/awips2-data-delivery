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

import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.SiteSubscription;
import com.raytheon.uf.common.datadelivery.registry.ebxml.SiteSubscriptionQuery;
import com.raytheon.uf.common.datadelivery.registry.ebxml.SubscriptionFilterableQuery;
import com.raytheon.uf.common.registry.RegistryQueryResponse;
import com.raytheon.uf.common.registry.handler.IRegistryObjectHandler;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;

/**
 * {@link IRegistryObjectHandler} implementation for {@link SiteSubscription}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 17, 2012  1169     djohnson  Initial creation.
 * Sep 24, 2012  1157     mpduff    Change to use InitialPendingSubscription.
 * Oct 17, 2012  726      djohnson  Add {@link #getActiveByDataSetAndProvider}.
 * Mar 29, 2013  1841     djohnson  Renamed from SubscriptionHandler.
 * Apr 05, 2013  1841     djohnson  Extracted core logic to superclass.
 * Apr 27, 2017  6186     rjpeter   Added getActiveByDataSetAndProviderForSite.
 *
 * </pre>
 *
 * @author djohnson
 */
@SuppressWarnings("rawtypes")
public class SiteSubscriptionHandler
        extends SubscriptionTypeHandler<SiteSubscription, SiteSubscriptionQuery>
        implements ISiteSubscriptionHandler {

    @Override
    protected SiteSubscriptionQuery getQuery() {
        return new SiteSubscriptionQuery();
    }

    @Override
    protected Class<SiteSubscription> getRegistryObjectClass() {
        return SiteSubscription.class;
    }

    @Override
    public List<SiteSubscription> getActiveByDataSetAndProviderForSite(
            String dataSetName, String providerName, String officeId)
            throws RegistryHandlerException {
        SubscriptionFilterableQuery<SiteSubscription> query = getQuery();
        query.setDataSetName(dataSetName);
        query.setProviderName(providerName);
        query.setActive(true);
        query.setOfficeId(officeId);

        RegistryQueryResponse<SiteSubscription> response = registryHandler
                .getObjects(query);

        checkResponse(response, "getActiveByDataSetAndProvider");

        return response.getResults();
    }
}
