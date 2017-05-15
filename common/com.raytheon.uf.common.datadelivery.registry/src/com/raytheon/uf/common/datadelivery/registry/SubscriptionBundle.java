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
package com.raytheon.uf.common.datadelivery.registry;

import com.raytheon.uf.common.datadelivery.registry.Subscription.SubscriptionPriority;

/**
 *
 * A bundle of {@link Subscription}s.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Mar 29, 2013  1841     djohnson  Remove JAXB annotations.
 * Jul 11, 2013  2106     djohnson  Use SubscriptionPriority.
 * Sep 05, 2014  2131     dhladky   re-did the types.
 * Apr 19, 2017  6186     rjpeter   Removed unused fields.
 *
 * </pre>
 *
 * @author djohnson
 */
public class SubscriptionBundle {
    private final Subscription<?, ?> subscription;

    private final Connection connection;

    private final Provider provider;

    public SubscriptionBundle(Subscription<?, ?> subscription,
            Connection connection, Provider provider) {
        this.subscription = subscription;
        this.connection = connection;
        this.provider = provider;
    }

    public Subscription<?, ?> getSubscription() {
        return subscription;
    }

    public SubscriptionPriority getPriority() {
        return subscription.getPriority();
    }

    public Connection getConnection() {
        return connection;
    }

    public Provider getProvider() {
        return provider;
    }

    /**
     * Get the data type from the bundle.
     *
     * @return the type
     */
    public DataType getDataType() {
        return subscription.getDataSetType();
    }
}
