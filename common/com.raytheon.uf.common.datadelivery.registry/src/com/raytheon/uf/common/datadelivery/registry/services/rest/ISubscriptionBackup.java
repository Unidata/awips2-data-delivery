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
package com.raytheon.uf.common.datadelivery.registry.services.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.raytheon.uf.common.registry.RegistryException;

/**
 * <pre>
 * 
 * Registry service interface for subscription backup/restore operations
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 5/11/2015    4448        bphillip    Initial Creation
 * </pre>
 * 
 * @author bphillip
 * @version 1
 **/
@Path("/")
public interface ISubscriptionBackup {

    /**
     * Removes any subscriptions for the given site
     * 
     * @param siteId
     *            The site to remove the subscriptions for
     * @throws RegistryException
     *             If errors occur while removing the subscriptions
     */
    @GET
    @Path("removeSubscriptionsFor/{siteId}")
    public void removeSubscriptionsForSite(@PathParam("siteId") String siteId)
            throws RegistryException;

    /**
     * Gets the subscriptions that are currently in the registry and formats
     * them in HTML for viewing in a web browser
     * 
     * @return The page containing the subscriptions
     */
    @GET
    @Path("getSubscriptions")
    public String getSubscriptions();

    /**
     * 
     * Backs up the specified subscription to be restored at a later time
     * 
     * @param subscriptionName
     *            The subscription to be backed up
     * @return Status message about whether the backup was successful
     */
    @GET
    @Path("backupSubscription/{subscriptionName}")
    public String backupSubscription(
            @PathParam("subscriptionName") String subscriptionName);

    /**
     * Backs up all subscriptions currently in the registry
     * 
     * @return Status message about whether the backup was successful
     */
    @GET
    @Path("backupAllSubscriptions/")
    public String backupAllSubscriptions();

    /**
     * Restores the specified subscription
     * 
     * @param subscriptionName
     *            The name of the subscription to restore
     * @return Status message about whether the backup was successful
     */
    @GET
    @Path("restoreSubscription/{subscriptionName}")
    public String restoreSubscription(
            @PathParam("subscriptionName") String subscriptionName);

    /**
     * Restores any subscriptions that were previously backed up
     * 
     * @return Status messages relating to the success or failure of the restore
     */
    @GET
    @Path("restoreSubscriptions/")
    public String restoreSubscriptions();

    /**
     * Clears the backup file directory
     * 
     * @return Status message
     */
    @GET
    @Path("clearSubscriptionBackupFiles/")
    public String clearSubscriptionBackupFiles();
}
