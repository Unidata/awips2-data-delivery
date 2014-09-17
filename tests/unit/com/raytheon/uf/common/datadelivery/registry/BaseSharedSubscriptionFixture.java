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

import java.util.Random;

import com.raytheon.uf.common.registry.ebxml.RegistryUtil;

/**
 * Base fixture for {@link SharedSubscription} objects.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 29, 2013 1650       djohnson     Initial creation
 * Oct 1, 2013  1797       dhladky      Updated to work with generic changes
 * Oct 21, 2013   2292     mpduff       Implement multiple data types
 * Aug 26, 2014   3365     ccody       Separate Data Delivery tests out of AWIPS 2 baseline.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public abstract class BaseSharedSubscriptionFixture<T extends SharedSubscription<Time, Coverage>>
        extends BaseSubscriptionFixture<T> {

    /**
     * Constructor.
     */
    protected BaseSharedSubscriptionFixture() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getInstance(long seedValue, Random random, DataType dataType) {
        T subscription = super.getInstance(seedValue, random, dataType);

        subscription.setId(RegistryUtil.getRegistryObjectKey(subscription));

        return subscription;
    }

}
