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
package com.raytheon.uf.common.datadelivery.service;

import com.raytheon.uf.common.datadelivery.registry.InitialPendingSubscription;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

/**
 * Thrift Request for approving pending subscriptions.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 20, 2012 1413       bgonzale    Initial creation.
 * Apr 01, 2013 1841       djohnson    Changed to use correct response class.
 * </pre>
 * 
 * @author bgonzale
 * @version 1.0
 */
@DynamicSerialize
public class ApprovedPendingSubscriptionNotificationRequest extends
        BaseSubscriptionNotificationRequest<InitialPendingSubscription> {

    /**
     * Default Constructor.
     */
    public ApprovedPendingSubscriptionNotificationRequest() {
    }

    @Override
    public ApprovedPendingSubscriptionNotificationResponse getResponse() {
        return new ApprovedPendingSubscriptionNotificationResponse();
    }
}
