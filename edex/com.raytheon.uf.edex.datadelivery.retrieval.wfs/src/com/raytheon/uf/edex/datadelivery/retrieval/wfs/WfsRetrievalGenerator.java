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
package com.raytheon.uf.edex.datadelivery.retrieval.wfs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.PointTime;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.ProviderType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.SubscriptionBundle;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;

/**
 *
 * WFS Retrieval Generator.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 25, 2012  955      djohnson  Moved to wfs specific package.
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * May 12, 2013  753      dhladky   Implemented
 * May 31, 2013  2038     djohnson  Move to correct repo.
 * Jun 04, 2013  1763     dhladky   Readied for WFS Retrievals.
 * Jun 18, 2013  2120     dhladky   Fixed times.
 * Sep 18, 2013  2383     bgonzale  Added subscription name to log output.
 * Oct 02, 2013  1797     dhladky   Generics time gridded time separation.
 * Oct 28, 2013  2448     dhladky   Request start time incorrectly used
 *                                  subscription start time.
 * Apr 20, 2017  6186     rjpeter   Update buildRetrieval signature.
 * Jul 25, 2017  6186     rjpeter   Use Retrieval
 *
 * </pre>
 *
 * @author djohnson
 */
class WfsRetrievalGenerator extends RetrievalGenerator<PointTime, Coverage> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(WfsRetrievalGenerator.class);

    WfsRetrievalGenerator(Provider provider) {
        super(ServiceType.WFS);
    }

    @Override
    public List<Retrieval<PointTime, Coverage>> buildRetrieval(
            DataSetMetaData<PointTime, Coverage> dsmd,
            SubscriptionBundle bundle) {

        List<Retrieval<PointTime, Coverage>> retrievals = Collections
                .emptyList();
        switch (bundle.getDataType()) {
        case POINT:
            retrievals = getPointRetrievals(bundle);
            break;
        default:
            statusHandler.error("Grid DATA WFS NOT YET IMPLEMENTED");
            throw new IllegalArgumentException(
                    "Grid DATA WFS NOT YET IMPLEMENTED");
        }

        return retrievals;
    }

    /**
     * create the grid type retrievals
     *
     * @param bundle
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<Retrieval<PointTime, Coverage>> getPointRetrievals(
            SubscriptionBundle bundle) {

        List<Retrieval<PointTime, Coverage>> retrievals = new ArrayList<>(1);
        Subscription<PointTime, Coverage> sub = (Subscription<PointTime, Coverage>) bundle
                .getSubscription();

        if (sub != null) {

            if (sub.getUrl() == null) {
                statusHandler.info("Skipping subscription " + sub.getName()
                        + " that is unfulfillable with the current metadata (null URL.)");
                return Collections.emptyList();
            }

            PointTime subTime = sub.getTime();
            // Gets the most recent time, which is kept as the end time.
            Date endDate = subTime.getEnd();
            // We add a little extra padding in the interval to prevent gaps in
            // data.
            long intervalMillis = (long) (subTime.getInterval()
                    * TimeUtil.MILLIS_PER_MINUTE * 1.5);
            // create the request start and end times.
            subTime.setRequestEnd(endDate);
            Date requestStartDate = new Date(
                    endDate.getTime() - intervalMillis);
            subTime.setRequestStart(requestStartDate);

            // with point data they all have the same data
            Parameter param = null;

            if (sub.getParameter() != null) {
                param = sub.getParameter().get(0);
            }

            Retrieval<PointTime, Coverage> retrieval = getRetrieval(sub, bundle,
                    param, subTime);
            retrievals.add(retrieval);
        }

        return retrievals;
    }

    /**
     * Get the retrieval
     *
     * @param sub
     * @param bundle
     * @param param
     * @param level
     * @param Time
     * @return
     */
    private Retrieval<PointTime, Coverage> getRetrieval(
            Subscription<PointTime, Coverage> sub, SubscriptionBundle bundle,
            Parameter param, PointTime time) {

        Retrieval<PointTime, Coverage> retrieval = new Retrieval<>();
        retrieval.setSubscriptionName(sub.getName());
        retrieval.setServiceType(getServiceType());
        retrieval.setUrl(sub.getUrl());
        retrieval.setOwner(sub.getOwner());
        retrieval.setSubscriptionType(getSubscriptionType(sub));
        retrieval.setNetwork(sub.getRoute());

        // Coverage and type processing
        Coverage cov = sub.getCoverage();

        if (cov instanceof Coverage) {
            retrieval.setDataType(DataType.POINT);
        } else {
            throw new UnsupportedOperationException(
                    "WFS retrieval does not support coverages/types other than Point. ");
        }

        final ProviderType providerType = bundle.getProvider()
                .getProviderType(bundle.getDataType());
        final String plugin = providerType.getPlugin();

        // Attribute processing
        RetrievalAttribute<PointTime, Coverage> att = new RetrievalAttribute<>();
        att.setCoverage(cov);

        if (param != null) {
            Parameter lparam = processParameter(param);
            lparam.setLevels(param.getLevels());
            att.setParameter(lparam);
        }

        att.setTime(time);
        retrieval.setPlugin(plugin);
        retrieval.setProvider(sub.getProvider());
        retrieval.setAttribute(att);

        return retrieval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RetrievalAdapter<PointTime, Coverage> getServiceRetrievalAdapter() {
        return new WfsRetrievalAdapter();
    }
}
