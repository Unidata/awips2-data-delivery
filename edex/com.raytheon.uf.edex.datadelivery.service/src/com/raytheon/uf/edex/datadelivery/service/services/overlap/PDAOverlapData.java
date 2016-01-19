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
package com.raytheon.uf.edex.datadelivery.service.services.overlap;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.service.subscription.SubscriptionOverlapConfig;

/**
 * PDA overlap data object.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 10, 2015   4464     dhladky      Initial creation
 * Jan 18, 2016   5260     dhladky      testing related updates.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDAOverlapData<T extends Time, C extends Coverage> extends
        OverlapData<Time, Coverage> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public PDAOverlapData(Subscription sub,
            Subscription otherSub,
            SubscriptionOverlapConfig config) {
        super(sub, otherSub, config);
    }


    @Override
    public boolean isOverlapping() {
        // TODO Auto-generated method stub
        // Will implement this when PDA provides concrete data
        return false;
    }

    @Override
    public boolean isDuplicate() {
        // TODO Auto-generated method stub
        // Will implement this when PDA provides concrete data
        return false;
    }
}
