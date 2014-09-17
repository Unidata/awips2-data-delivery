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

import java.util.Arrays;
import java.util.Random;

import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.util.AbstractFixture;

/**
 * {@link AbstractFixture} for {@link Provider}s.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 05, 2012 1102       djohnson     Initial creation
 * Nov 19, 2012 1166       djohnson     Clean up JAXB representation of registry objects.
 * Jan 30, 2013 1543       djohnson     Add connection data.
 * Aug 26, 2014   3365     ccody       Separate Data Delivery tests out of AWIPS 2 baseline.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class ProviderFixture extends AbstractFixture<Provider> {

    public static final ProviderFixture INSTANCE = new ProviderFixture();

    /**
     * Disabled constructor.
     */
    private ProviderFixture() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Provider getInstance(long seedValue, Random random) {
        Provider provider = new Provider();
        provider.setConnection(ConnectionFixture.INSTANCE.get(seedValue));
        provider.setErrorResponsePattern("error");
        provider.setName("providerName" + seedValue);
        // TODO: ProjectionFixture
        // provider.setProjection(ProjectionFixture.INSTANCE.get(seedValue));
        provider.setServiceType(ServiceType.OPENDAP);
        provider.setProviderType(Arrays.<ProviderType> asList(new ProviderType(
                DataType.GRID, "grid", 100)));

        return provider;
    }

}
