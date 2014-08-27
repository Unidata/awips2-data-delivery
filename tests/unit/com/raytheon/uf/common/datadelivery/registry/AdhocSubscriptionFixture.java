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

import com.raytheon.uf.common.util.AbstractFixture;

/**
 * {@link AbstractFixture} for {@link AdhocSubscription}s.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 7, 2012  1286      djohnson     Initial creation
 * Oct 2, 2013  1797      dhladky      Generics
 * Aug 26, 2014   3365     ccody       Separate Data Delivery tests out of AWIPS 2 baseline.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class AdhocSubscriptionFixture extends
        BaseSiteSubscriptionFixture<AdhocSubscription<Time, Coverage>> {

    public static final AdhocSubscriptionFixture INSTANCE = new AdhocSubscriptionFixture();

    /**
     * Private constructor.
     */
    private AdhocSubscriptionFixture() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AdhocSubscription<Time, Coverage> getSubscription() {
        return new AdhocSubscription<Time, Coverage>();
    }

}
