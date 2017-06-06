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
package com.raytheon.uf.edex.datadelivery.retrieval.handlers;

import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.SizeUtil;
import com.raytheon.uf.common.util.rate.TokenBucket;

/**
 * Callback object that updates the common token bucket when bandwidth allowance
 * changes
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 8, 2017  6222      tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

public class TokenBucketUpdater implements IBandwidthChangedCallback {

    private TokenBucket tokenBucket;

    private Network network;

    public TokenBucketUpdater(TokenBucket tokenBucket, Network network) {
        this.tokenBucket = tokenBucket;
        this.network = network;
    }

    /**
     * Set the bandwidth in KBps
     */
    @Override
    public void bandwidthChanged(int bandwidthKBps, Network network) {
        if (network == this.network) {
            int bandwidthBps = (int) (bandwidthKBps * SizeUtil.BYTES_PER_KB);
            double secondsPerInterval = ((double) tokenBucket.getIntervalMs()
                    / (double) TimeUtil.MILLIS_PER_SECOND);
            tokenBucket.setCapacity((int) (bandwidthBps * secondsPerInterval));
        }
    }

}