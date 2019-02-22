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

import com.raytheon.uf.common.registry.handler.IRegistryObjectHandler;
import com.raytheon.uf.common.registry.handler.RegistryObjectHandlers;

/**
 * Utility class to retrieve {@link IRegistryObjectHandler} implementations for
 * data delivery.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 3, 2012  1241      djohnson     Initial creation
 * Dec 11, 2012 1403      djohnson     Adhoc subscriptions no longer go to the registry.
 * Oct 12, 2013 2046      dhladky      Restored Adhoc's at WFO level
 * Mar 16, 2016 3919      tjensen      Cleanup unneeded interfaces
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
public final class DataDeliveryHandlers {

    /**
     * Private constructor.
     */
    private DataDeliveryHandlers() {
    }

    /**
     * Retrieve the {@link SubscriptionHandler}.
     * 
     * @return the handler
     */
    public static SubscriptionHandler getSubscriptionHandler() {
        return RegistryObjectHandlers.get(SubscriptionHandler.class);
    }

    /**
     * Retrieve the {@link PendingSubscriptionHandler}.
     * 
     * @return the handler
     */
    public static PendingSubscriptionHandler getPendingSubscriptionHandler() {
        return RegistryObjectHandlers.get(PendingSubscriptionHandler.class);
    }

    /**
     * Retrieve the {@link GroupDefinitionHandler}.
     * 
     * @return the handler
     */
    public static GroupDefinitionHandler getGroupDefinitionHandler() {
        return RegistryObjectHandlers.get(GroupDefinitionHandler.class);
    }

    /**
     * Retrieve the {@link ProviderHandler}.
     * 
     * @return the handler
     */
    public static ProviderHandler getProviderHandler() {
        return RegistryObjectHandlers.get(ProviderHandler.class);
    }

    /**
     * Retrieve the {@link DataSetNameHandler}.
     * 
     * @return the handler
     */
    public static DataSetNameHandler getDataSetNameHandler() {
        return RegistryObjectHandlers.get(DataSetNameHandler.class);
    }

    /**
     * Retrieve the {@link ParameterHandler}.
     * 
     * @return the handler
     */
    public static ParameterHandler getParameterHandler() {
        return RegistryObjectHandlers.get(ParameterHandler.class);
    }

    /**
     * Retrieve the {@link ParameterLevelHandler}.
     * 
     * @return the handler
     */
    public static ParameterLevelHandler getParameterLevelHandler() {
        return RegistryObjectHandlers.get(ParameterLevelHandler.class);
    }

    /**
     * Retrieve the {@link DataSetMetaDataHandler}.
     * 
     * @return the handler
     */
    public static DataSetMetaDataHandler getDataSetMetaDataHandler() {
        return RegistryObjectHandlers.get(DataSetMetaDataHandler.class);
    }

    /**
     * Retrieve the {@link GriddedDataSetMetaDataHandler}.
     * 
     * @return the handler
     */
    public static GriddedDataSetMetaDataHandler getGriddedDataSetMetaDataHandler() {
        return RegistryObjectHandlers.get(GriddedDataSetMetaDataHandler.class);
    }

    /**
     * Retrieve the {@link DataSetHandler}.
     * 
     * @return the handler
     */
    public static DataSetHandler getDataSetHandler() {
        return RegistryObjectHandlers.get(DataSetHandler.class);
    }

    /**
     * Retrieve the {@link IAdhocSubscriptionHandler}.
     * 
     * @return the handler
     */

    public static IAdhocSubscriptionHandler getAdhocSubscriptionHandler() {
        return RegistryObjectHandlers.get(IAdhocSubscriptionHandler.class);
    }

}
