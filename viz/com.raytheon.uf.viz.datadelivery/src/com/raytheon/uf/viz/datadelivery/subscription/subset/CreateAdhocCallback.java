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
package com.raytheon.uf.viz.datadelivery.subscription.subset;

import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.RecurringSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription.SubscriptionType;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.datadelivery.services.DataDeliveryServices;
import com.raytheon.uf.viz.datadelivery.subscription.CancelForceApplyAndIncreaseLatencyDisplayText;
import com.raytheon.uf.viz.datadelivery.subscription.CreateSubscriptionDlg;
import com.raytheon.uf.viz.datadelivery.subscription.CreateSubscriptionDlg.ICreateAdhocCallback;
import com.raytheon.uf.viz.datadelivery.subscription.SubscriptionServiceResult;

/**
 * Callback for creating an adhoc subscription after creating a recurring
 * subscription
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 25, 2017 6461       tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

public class CreateAdhocCallback implements ICreateAdhocCallback {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(CreateAdhocCallback.class);

    @Override
    public String storeAdhocFromRecurring(
            CreateSubscriptionDlg subscriptionDlg) {
        AdhocSubscription adhoc = new AdhocSubscription(
                (RecurringSubscription) subscriptionDlg.getSubscription());
        DataSetMetaData dataSetMetaData = null;

        try {
            if (subscriptionDlg.getDataSet().isMoving()) {
                dataSetMetaData = DataDeliveryHandlers
                        .getDataSetMetaDataHandler()
                        .getMostRecentDataSetMetaDataByIntersection(
                                adhoc.getDataSetName(), adhoc.getProvider(),
                                adhoc.getCoverage().getRequestEnvelope());
            } else {
                dataSetMetaData = DataDeliveryHandlers
                        .getDataSetMetaDataHandler()
                        .getMostRecentDataSetMetaData(adhoc.getDataSetName(),
                                adhoc.getProvider());
            }
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "No DataSetMetaData matching query! DataSetName: "
                            + adhoc.getDataSetName() + " Provider: "
                            + adhoc.getProvider(),
                    e);
        }
        if (dataSetMetaData == null) {
            statusHandler.info(String.format(
                    "No dataset metadata for dataset[%s] intersect subscription area for subscription [%s].",
                    adhoc.getDataSetName(), adhoc.getName()));
            return "";
        }
        statusHandler.info(String.format(
                "Found most recent metadata for adhoc subscription [%s], using url [%s]",
                adhoc.getName(), dataSetMetaData.getUrl()));
        adhoc.setUrl(dataSetMetaData.getUrl());

        /*
         * Time for recurring subscription was previously set by
         * SubsetManagerDlg, so we have the right type of Time object created.
         * Just need to change start and end times.
         */
        adhoc.getTime().setStart(dataSetMetaData.getDate());
        if (adhoc.getTime().getEnd() == null) {
            adhoc.getTime().setEnd(dataSetMetaData.getDate());
        }
        adhoc.setSubscriptionType(SubscriptionType.QUERY);
        return storeAdhoc(subscriptionDlg, adhoc);

    }

    protected String storeAdhoc(CreateSubscriptionDlg subscriptionDlg,
            AdhocSubscription adhoc) {
        String status = "";
        try {
            String currentUser = LocalizationManager.getInstance()
                    .getCurrentUser();
            SubscriptionServiceResult result = DataDeliveryServices
                    .getSubscriptionService().store(currentUser, adhoc,
                            new CancelForceApplyAndIncreaseLatencyDisplayText(
                                    "create", subscriptionDlg.getShell()));

            if (result.hasMessageToDisplay()) {
                status = result.getMessage();

                /*
                 * Log the results, but don't need a dialog notice since there
                 * already is a dialog confirming the creation of the recurring
                 * subscription
                 */
                statusHandler.info("Query Scheduled: " + result.getMessage());
            }
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error requesting adhoc data.", e);
        }
        return status;
    }

}
