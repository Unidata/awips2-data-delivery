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
package com.raytheon.uf.edex.datadelivery.bandwidth;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;

/**
 * Implementation of {@link AbstractBandwidthDaoTest} for
 * {@link InMemoryBandwidthDao}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 13, 2012 1286       djohnson     Initial creation
 * Oct 3,  2013 1797       dhladky      crazy generics
 * Aug 26, 2014 3365       ccody        Separate Data Delivery tests out of AWIPS 2 baseline.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class InMemoryBandwidthDaoTest<T extends Time, C extends Coverage> extends
        AbstractBandwidthDaoTest<T, C, InMemoryBandwidthDao<T, C>> {
    /**
     * {@inheritDoc}
     */
    @Override
    protected InMemoryBandwidthDao<T, C> getDao() {
        return new InMemoryBandwidthDao<T, C>();
    }
}
