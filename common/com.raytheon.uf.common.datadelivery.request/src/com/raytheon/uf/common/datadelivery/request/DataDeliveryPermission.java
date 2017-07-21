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
package com.raytheon.uf.common.datadelivery.request;

import com.raytheon.uf.common.auth.util.PermissionUtils;

/**
 * Consolidate string system role usage into an enum.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 03, 2012  1241     djohnson  Initial creation.
 * May 20, 2013  1040     mpduff    Added Shared Subscription permissions.
 * Jul 18, 2017  6286     randerso  Organized enum for clarity. Removed unused
 *                                  values.
 *
 * </pre>
 *
 * @author djohnson
 */

public enum DataDeliveryPermission {
    NOTIFICATION_VIEW(
            PermissionUtils.buildPermissionString("datadelivery",
                    "notification", "view")),

    SUBSCRIPTION_ACTIVATE(
            PermissionUtils.buildPermissionString("datadelivery",
                    "subscription", "activate")),
    SUBSCRIPTION_CREATE(
            PermissionUtils.buildPermissionString("datadelivery",
                    "subscription", "create")),
    SUBSCRIPTION_DELETE(
            PermissionUtils.buildPermissionString("datadelivery",
                    "subscription", "delete")),
    SUBSCRIPTION_EDIT(
            PermissionUtils.buildPermissionString("datadelivery",
                    "subscription", "edit")),
    SUBSCRIPTION_VIEW(
            PermissionUtils.buildPermissionString("datadelivery",
                    "subscription", "view")),

    SUBSCRIPTION_APPROVE_SITE(
            PermissionUtils.buildPermissionString("datadelivery",
                    "subscription", "approve", "site")),
    SUBSCRIPTION_APPROVE_USER(
            PermissionUtils.buildPermissionString("datadelivery",
                    "subscription", "approve", "user")),
    SUBSCRIPTION_APPROVE_VIEW(
            PermissionUtils.buildPermissionString("datadelivery",
                    "subscription", "approve", "view")),

    SUBSCRIPTION_DATASET_BROWSER(
            PermissionUtils.buildPermissionString("datadelivery",
                    "subscription", "dataset", "browser")),

    SHARED_SUBSCRIPTION_CREATE(
            PermissionUtils.buildPermissionString("datadelivery", "shared",
                    "subscription", "create")),

    SYSTEM_MANAGEMENT_VIEW(
            PermissionUtils.buildPermissionString("datadelivery",
                    "systemmanagement", "view")),
    SYSTEM_MANAGEMENT_CREATE(
            PermissionUtils.buildPermissionString("datadelivery",
                    "systemmanagement", "create"));

    private String stringValue;

    private DataDeliveryPermission(String stringValue) {
        this.stringValue = stringValue;
    }

    @Override
    public String toString() {
        return stringValue;
    }

    /**
     * Retrieve the {@link DataDeliveryPermission} enum from its string value.
     *
     * @param string
     *            the string
     * @return the permission
     * @throws IllegalArgumentException
     *             if no enum has the string value
     */
    public static DataDeliveryPermission fromString(String string) {
        // TODO: Create a Map<String, DataDeliveryPermission> of these for
        // convenience?
        for (DataDeliveryPermission permission : DataDeliveryPermission
                .values()) {
            if (permission.toString().equals(string)) {
                return permission;
            }
        }

        throw new IllegalArgumentException(
                "No enum with toString() value of [" + string + "].");
    }
}
