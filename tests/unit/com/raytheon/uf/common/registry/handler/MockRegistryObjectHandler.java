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
package com.raytheon.uf.common.registry.handler;

import org.junit.Ignore;

import com.raytheon.uf.common.registry.MockRegistryObject;

/**
 * {@link IRegistryObjectHandler} for {@link MockRegistryObject}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 17, 2012 1169       djohnson     Initial creation
 * Aug 26, 2014 3365       ccody        Separate Data Delivery tests out of AWIPS 2 baseline.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
@Ignore
public class MockRegistryObjectHandler extends
        BaseRegistryObjectHandler<MockRegistryObject, MockRegistryObjectQuery>
        implements
        IMockRegistryObjectHandler {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<MockRegistryObject> getRegistryObjectClass() {
        return MockRegistryObject.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MockRegistryObjectQuery getQuery() {
        return new MockRegistryObjectQuery();
    }
}
