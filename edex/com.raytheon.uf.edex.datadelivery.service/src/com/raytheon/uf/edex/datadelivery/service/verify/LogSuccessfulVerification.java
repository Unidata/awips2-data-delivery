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
package com.raytheon.uf.edex.datadelivery.service.verify;

import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.service.verify.SubscriptionIntegrityVerifier.IVerificationAction;
import com.raytheon.uf.edex.datadelivery.service.verify.SubscriptionIntegrityVerifier.IVerificationResponse;

/**
 * {@link IVerificationAction} implementation that logs a successful
 * verification. intentionally package-private to force dependency injection.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 10, 2012 1104       djohnson     Initial creation
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
class LogSuccessfulVerification implements IVerificationAction {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(LogSuccessfulVerification.class);

    private final Priority priority;

    /**
     * Constructor.
     * 
     * @param priority
     *            the priority at which logging should be performed
     */
    LogSuccessfulVerification(Priority priority) {
        this.priority = priority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verificationPerformed(Subscription subscription,
            IVerificationResponse response) {
        if (statusHandler.isPriorityEnabled(priority)) {
            statusHandler.handle(priority,
                    "Subscription [" + subscription.getName()
                            + "] has passed verification against dataset ["
                            + subscription.getDataSetName() + "].");
        }
    }

}
