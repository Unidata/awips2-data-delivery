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
package com.raytheon.uf.edex.datadelivery.retrieval.metadata;

import java.util.EnumMap;

import org.apache.commons.lang3.Validate;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.ReflectionUtil;
import com.raytheon.uf.common.util.ServiceLoaderUtil;
import com.raytheon.uf.common.util.registry.GenericRegistry;
import com.raytheon.uf.common.util.registry.RegistryException;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IServiceFactory;

/**
 * Retrieve {@link ServiceType} specific implementations of interfaces.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 24, 2012 955        djohnson     Initial creation
 * Nov 19, 2012 1166       djohnson     Clean up JAXB representation of registry objects.
 * Mar 21, 2013 1794       djohnson     ServiceLoaderUtil now requires the requesting class.
 * May 31, 2013 2038       djohnson     Plugin contributable registry.
 * Sep 27, 2014 3121       dhladky      Make service type generics better.  Need to work on raw types more.
 * Mar 16, 2016 3919       tjensen      Cleanup unneeded interfaces
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public final class ServiceTypeFactory<O, D, T extends Time, C extends Coverage> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(ServiceTypeFactory.class);

    @SuppressWarnings("rawtypes")
    // TODO Figure a way to not have to do raw types with IServiceFactory
    private static class ServiceTypeRegistry extends
            GenericRegistry<ServiceType, Class<IServiceFactory>> {

        private <O, D, T, C> ServiceTypeRegistry() {
            super(new EnumMap<ServiceType, Class<IServiceFactory>>(
                    ServiceType.class));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object register(ServiceType t, Class<IServiceFactory> s)
                throws RegistryException {

            Validate.notNull(t);
            Validate.notNull(s);

            statusHandler.info("Registered service type factory ["
                    + s.getName() + "] for service type [" + t + "]");

            return super.register(t, s);
        }
    };

    private static final ServiceTypeRegistry serviceTypeRegistry = new ServiceTypeRegistry();

    @SuppressWarnings("rawtypes")
    // TODO Figure a way to not have to do raw types with IServiceFactory
    private static class ServiceTypeFactoryLookup<O, D, T, C> {

        /**
         * Retrieve the {@link IServiceFactory} for a {@link Provider}.
         * 
         * @param provider
         *            the provider
         * @return the {@link IServiceFactory}
         */
        public IServiceFactory getProviderServiceFactory(Provider provider) {
            final ServiceType serviceType = provider.getServiceType();
            final Class<IServiceFactory> serviceFactoryClass = serviceTypeRegistry
                    .getRegisteredObject(serviceType);
            if (serviceFactoryClass == null) {
                throw new IllegalArgumentException(String.format(
                        "No %s available to handle service type [%s]!",
                        IServiceFactory.class.getSimpleName(), serviceType));
            }

            // Must create a new instance because the implementations are not
            // thread safe
            IServiceFactory serviceFactory = ReflectionUtil
                    .newInstanceOfAssignableType(IServiceFactory.class,
                            serviceFactoryClass);
            serviceFactory.setProvider(provider);
            return serviceFactory;
        }
    }

    @SuppressWarnings("rawtypes")
    private static ServiceTypeFactoryLookup SERVICE_FACTORY_LOOKUP = ServiceLoaderUtil
            .load(ServiceTypeFactory.class, ServiceTypeFactoryLookup.class,
                    new ServiceTypeFactoryLookup());

    private ServiceTypeFactory() {

    }

    /**
     * Retrieve the {@link IServiceFactory} to handle a specific
     * {@link Provider} .
     * 
     * @param provider
     *            the provider
     * @return the factory
     */

    @SuppressWarnings("rawtypes")
    // TODO Figure a way to not have to do raw types with IServiceFactory
    public static <O, D, T, C> IServiceFactory retrieveServiceFactory(
            Provider provider) {
        return SERVICE_FACTORY_LOOKUP.getProviderServiceFactory(provider);
    }

    /**
     * Retrieve the {@link RetrievalAdapter} implementation for this service
     * type.
     * 
     * @param serviceType
     *            the service type
     * @return the retrieval adapter
     */
    @SuppressWarnings("rawtypes")
    // TODO Figure why T,C can not be parameterized when erasure extends Time
    // and Coverage
    public static <T, C> RetrievalAdapter retrieveServiceRetrievalAdapter(
            ServiceType serviceType) {
        Provider provider = new Provider();
        provider.setServiceType(serviceType);
        return retrieveServiceFactory(provider).getRetrievalGenerator()
                .getServiceRetrievalAdapter();
    }

    /**
     * Get the service type registry.
     * 
     * @return the registry
     */
    @SuppressWarnings("rawtypes")
    // TODO Figure a way to not have to do raw types with IServiceFactory
    public static GenericRegistry<ServiceType, Class<IServiceFactory>> getServiceTypeRegistry() {
        return serviceTypeRegistry;
    }
}
