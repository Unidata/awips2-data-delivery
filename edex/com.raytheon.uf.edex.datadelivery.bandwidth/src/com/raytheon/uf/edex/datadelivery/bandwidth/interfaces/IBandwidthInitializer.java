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
package com.raytheon.uf.edex.datadelivery.bandwidth.interfaces;

import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.edex.datadelivery.bandwidth.BandwidthManager;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthDbInit;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalManager;
import com.raytheon.uf.edex.registry.ebxml.init.RegistryInitializedListener;

/**
 * * An interface for initialization of the BandwidthManager instance. The
 * implementation of this interface will be passed a reference to the instance
 * of the BandwidthManager.
 *
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 26, 2012  1286     djohnson  Initial creation
 * Apr 16, 2013  1906     djohnson  Extends RegistryInitializedListener.
 * Jun 25, 2013  2106     djohnson  init() now takes a {@link RetrievalManager}.
 * May 22, 2014  2808     dhladky   Fixed naming
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Nov 22, 2017  6484     tjensen   Remove unused init return value
 *
 * </pre>
 *
 * @author djohnson
 * @version 1.0
 */
public interface IBandwidthInitializer extends RegistryInitializedListener {

    /**
     * Initialize the instance of BandwidthManager.
     *
     * @param instance
     *            A reference to the instance of the BandwidthManager to
     *            initialize.
     * @param dbInit
     *            a reference to the {@link IBandwidthDbInit} instance
     * @param retrievalManager
     *            the {@link RetrievalManager} instance
     *
     */
    void init(BandwidthManager instance, IBandwidthDbInit dbInit,
            RetrievalManager retrievalManager);

    /**
     * Gets a list of active subscriptions for the available routes.
     *
     * @return
     */
    Map<Network, List<Subscription>> getSubMapByRoute() throws Exception;
}
