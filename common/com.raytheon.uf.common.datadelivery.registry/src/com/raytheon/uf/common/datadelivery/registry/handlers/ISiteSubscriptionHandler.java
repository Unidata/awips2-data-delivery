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
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.registry.handler.IRegistryObjectHandler;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;

/**
 * The {@link IRegistryObjectHandler} interface for {@link Subscription}s.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 17, 2012  1169     djohnson  Initial creation
 * Oct 17, 2012  726      djohnson  Add {@link #getActiveByDataSetAndProvider}.
 * Mar 29, 2013  1841     djohnson  Renamed to specify UserSubscription.
 * Apr 27, 2017  6186     rjpeter   Added getActiveByDataSetAndProviderForSite.
 * Apr 03, 2018  7240     tjensen   Added getByDataSetAndProviderForSite
 *
 * </pre>
 *
 * @author djohnson
 */
@SuppressWarnings("rawtypes")
public interface ISiteSubscriptionHandler
        extends ISubscriptionTypeHandler<SiteSubscription> {

    /**
     * Retrieve active subscriptions for the dataset name and provider for the
     * specified site.
     *
     * @param dataSetName
     *            the dataset name
     * @param providerName
     *            the provider name
     * @param officeId
     *            the office id for the site
     */
    List<SiteSubscription> getActiveByDataSetAndProviderForSite(
            String dataSetName, String providerName, String officeId)
            throws RegistryHandlerException;

    /**
     * Retrieve subscriptions for the dataset name and provider for the
     * specified site.
     *
     * @param dataSetName
     *            the dataset name
     * @param providerName
     *            the provider name
     * @param officeId
     *            the office id for the site
     */
    List<SiteSubscription> getByDataSetAndProviderForSite(String dataSetName,
            String providerName, String officeId)
            throws RegistryHandlerException;
}
