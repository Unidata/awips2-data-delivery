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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.PDADataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.RecurringSubscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription.SubscriptionType;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataDeliveryHandlers;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.datadelivery.subscription.CreateSubscriptionDlg;

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
 * Jan 25, 2018 6506       nabowle     storeAdhoc renamed to scheduleAdhoc
 * Jul 12, 2018 7358       tjensen     Set url on adhoc subscription
 *
 * </pre>
 *
 * @author tgurney
 */

public class PDACreateAdhocCallback extends CreateAdhocCallback {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDACreateAdhocCallback.class);

    @Override
    public String storeAdhocFromRecurring(
            CreateSubscriptionDlg subscriptionDlg) {
        AdhocSubscription adhoc = new AdhocSubscription(
                (RecurringSubscription) subscriptionDlg.getSubscription());

        List<DataSetMetaData> metaDatas = null;
        String currentUser = LocalizationManager.getInstance().getCurrentUser();

        DataSet dataSet = subscriptionDlg.getDataSet();
        StringBuilder status = new StringBuilder();

        try {
            metaDatas = DataDeliveryHandlers.getDataSetMetaDataHandler()
                    .getDataSetMetaDataByIntersection(dataSet.getDataSetName(),
                            dataSet.getProviderName(),
                            dataSet.getCoverage().getEnvelope(), 0);
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error when retrieving metadata for dataset "
                            + dataSet.getDataSetName(),
                    e);
            return status.toString();
        }

        int subCounter = 1;
        String now = Long.toString(System.currentTimeMillis());
        // Make a separate sub for the latest time for each parameter
        for (Object item : adhoc.getParameterGroups().entrySet()) {
            Entry<String, ParameterGroup> paramGroup = (Entry<String, ParameterGroup>) item;
            AdhocSubscription newAdhoc = createAdhocForOneParameter(adhoc,
                    metaDatas, paramGroup);
            if (newAdhoc != null) {
                /*
                 * Append a unique string to the subscription name to avoid
                 * possible collision
                 */
                newAdhoc.setName(
                        newAdhoc.getName() + "-" + now + "-" + subCounter);
                String retStatus = scheduleAdhoc(subscriptionDlg, newAdhoc);
                if (retStatus != null && !retStatus.isEmpty()) {
                    status.append(retStatus).append(" ");
                }
                subCounter++;
            }
        }

        return status.toString();
    }

    private AdhocSubscription createAdhocForOneParameter(
            AdhocSubscription tmpAdhoc, List<DataSetMetaData> metaDatas,
            Entry<String, ParameterGroup> paramGroup) {
        AdhocSubscription adhoc = new AdhocSubscription(tmpAdhoc);
        Map<String, ParameterGroup> newParamGroups = new HashMap<>();
        newParamGroups.put(paramGroup.getKey(), paramGroup.getValue());
        adhoc.setParameterGroups(newParamGroups);

        PDADataSetMetaData latestMetaData = metaDatas.stream()
                .map(m -> (PDADataSetMetaData) m).filter(m -> {
                    try {
                        return m.satisfiesSubscription(adhoc) == null;
                    } catch (Exception e) {
                        statusHandler.handle(Priority.PROBLEM,
                                "Failed to check metadata " + m.getMetaDataID()
                                        + " against subscription "
                                        + adhoc.getName(),
                                e);
                        return false;
                    }
                }).max((m1, m2) -> m1.getDate().compareTo(m2.getDate()))
                .orElse(null);

        if (latestMetaData != null) {
            adhoc.setTime(latestMetaData.getTime());
            adhoc.setSubscriptionType(SubscriptionType.QUERY);
            adhoc.setUrl(latestMetaData.getUrl());
            return adhoc;
        } else {
            return null;
        }
    }

}
