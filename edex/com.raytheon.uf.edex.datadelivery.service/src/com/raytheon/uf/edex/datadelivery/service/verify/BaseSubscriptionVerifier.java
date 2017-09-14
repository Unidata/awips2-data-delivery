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

import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.LevelGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.registry.ParameterUtils;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.edex.datadelivery.service.verify.SubscriptionIntegrityVerifier.IVerificationResponse;
import com.raytheon.uf.edex.datadelivery.service.verify.SubscriptionIntegrityVerifier.IVerificationStrategy;

/**
 * Simple implementation of {@link IVerificationStrategy}. Intentionally
 * package-private to enforce dependency injection.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------------
 * Dec 07, 2012  1104     djohnson  Initial creation
 * Jan 30, 2013  1543     djohnson  Use List instead of ArrayList.
 * Sep 12, 2017  6413     tjensen   Updated to support ParameterGroups
 *
 * </pre>
 *
 * @author djohnson
 */
class BaseSubscriptionVerifier implements IVerificationStrategy {

    private static class VerificationResponse implements IVerificationResponse {
        private final String message;

        public VerificationResponse(String message) {
            this.message = message;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasFailedVerification() {
            return message != null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getNotificationMessage() {
            return message;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IVerificationResponse verify(DataSet dataSet,
            Subscription subscription) {

        Map<String, ParameterGroup> subParamGroups = subscription
                .getParameterGroups();
        Map<String, ParameterGroup> dsParamGroups = dataSet
                .getParameterGroups();

        List<ParameterGroup> invalidParameters = ParameterUtils
                .getUnique(subParamGroups, dsParamGroups);

        if (!invalidParameters.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Subscription ").append(subscription.getName()).append(
                    " has failed verification.  The following parameters are no longer available: ");
            String prefix = "";
            for (ParameterGroup param : invalidParameters) {
                for (LevelGroup lg : param.getGroupedLevels().values()) {
                    for (ParameterLevelEntry ple : lg.getLevels()) {
                        sb.append(prefix).append(ple.getProviderName())
                                .append(" (").append(param.getAbbrev())
                                .append(")");
                        prefix = ", ";
                    }
                }
            }

            return new VerificationResponse(sb.toString());
        }
        return new VerificationResponse(null);
    }

}
