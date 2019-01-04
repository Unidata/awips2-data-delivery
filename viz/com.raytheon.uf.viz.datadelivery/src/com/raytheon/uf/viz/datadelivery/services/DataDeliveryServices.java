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
package com.raytheon.uf.viz.datadelivery.services;

import com.raytheon.uf.common.auth.req.IPermissionsService;
import com.raytheon.uf.common.datadelivery.bandwidth.BandwidthService;
import com.raytheon.uf.common.datadelivery.service.SendToServerSubscriptionNotificationService;
import com.raytheon.uf.common.datadelivery.service.subscription.SubscriptionOverlapService;
import com.raytheon.uf.viz.datadelivery.subscription.SubscriptionService;

/**
 * Provides various Spring injected implementations of services. This class
 * should only be used by "dynamically" constructed objects, such as GUI dialogs
 * that cannot be dependency injected with services via Spring.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -------------------------------------
 * Nov 09, 2012  1286     djohnson  Initial creation
 * May 20, 2013  2000     djohnson  Add subscription overlap service.
 * Jul 26, 2031  2232     mpduff    Moved IPermissionsService to common.
 * Oct 21, 2013  2292     mpduff    Added generics.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Apr 25, 2017  1045     tjensen   Cleanup more unneeded interfaces
 * Jan 03, 2019  7503     troberts  Remove subscription grouping capabilities.
 *
 * </pre>
 *
 * @author djohnson
 * @version 1.0
 */

public final class DataDeliveryServices {

    private static BandwidthService<?, ?> bandwidthService;

    private static SubscriptionService subscriptionService;

    private static SendToServerSubscriptionNotificationService subscriptionNotificationService;

    private static IPermissionsService permissionsService;

    private static SubscriptionOverlapService<?, ?> subscriptionOverlapService;

    /**
     * Spring only constructor. All access should be through static methods.
     */
    private DataDeliveryServices(BandwidthService<?, ?> bandwidthService,
            SubscriptionService subscriptionService,
            SendToServerSubscriptionNotificationService subscriptionNotificationService,
            IPermissionsService permissionsService,
            SubscriptionOverlapService<?, ?> subscriptionOverlapService) {
        DataDeliveryServices.bandwidthService = bandwidthService;
        DataDeliveryServices.subscriptionService = subscriptionService;
        DataDeliveryServices.subscriptionNotificationService = subscriptionNotificationService;
        DataDeliveryServices.permissionsService = permissionsService;
        DataDeliveryServices.subscriptionOverlapService = subscriptionOverlapService;
    }

    /**
     * Get the subscription service.
     *
     * @return the subscriptionService the subscription service
     */
    public static SubscriptionService getSubscriptionService() {
        return DataDeliveryServices.subscriptionService;
    }

    /**
     * Get the bandwidth service.
     *
     * @return the bandwidthService
     */
    public static BandwidthService<?, ?> getBandwidthService() {
        return DataDeliveryServices.bandwidthService;
    }

    /**
     * Get the subscription service.
     *
     * @return the subscription notification service
     */
    public static SendToServerSubscriptionNotificationService getSubscriptionNotificationService() {
        return DataDeliveryServices.subscriptionNotificationService;
    }

    /**
     * Get the permissions service.
     *
     * @return the permissions service
     */
    public static IPermissionsService getPermissionsService() {
        return DataDeliveryServices.permissionsService;
    }

    /**
     * Get the subscription overlap service.
     *
     * @return the subscriptionOverlapService
     */
    public static SubscriptionOverlapService<?, ?> getSubscriptionOverlapService() {
        return DataDeliveryServices.subscriptionOverlapService;
    }
}
