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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.time.util.ITimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.rate.TokenBucket;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalEvent;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalRequestBuilder;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.ServiceTypeFactory;

/**
 * Process subscription retrievals.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 07, 2011           dhladky   Initial creation
 * Aug 15, 2012  1022     djohnson  Moved from inner to class proper.
 * Aug 22, 2012  743      djohnson  Continue processing retrievals until there
 *                                  are no more.
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Jan 30, 2013  1543     djohnson  Constrain to the network retrievals are
 *                                  pulled for.
 * Feb 15, 2013  1543     djohnson  Using xml for retrievals now.
 * Mar 05, 2013  1647     djohnson  Change no retrievals found message to debug.
 * Aug 09, 2013  1822     bgonzale  Added parameters to
 *                                  processRetrievedPluginDataObjects.
 * Oct 01, 2013  2267     bgonzale  Removed request parameter and IRetrievalDao
 *                                  field.
 * Jan 30, 2014  2686     dhladky   refactor of retrieval.
 * Jul 27, 2017  6186     rjpeter   Refactored
 *
 * </pre>
 *
 * @author dhladky
 */
public abstract class RetrievalTask {

    protected static final int maxTries;

    protected static final long retryIntervalMs;

    static {
        int tmp = Integer.getInteger("retrieval.retry.count", 2);
        if (tmp < 0) {
            LoggerFactory.getLogger(RetrievalTask.class)
                    .error("Retry count property retrieval.retry.count [" + tmp
                            + "], cannot be less than zero, setting to zero");
            tmp = 0;
        }

        maxTries = tmp + 1;

        long tmpL = Long.getLong("retrieval.retry.millis", 5_000);
        if (tmpL < 0) {
            LoggerFactory.getLogger(RetrievalTask.class)
                    .error("Retry retrieval wait period retrieval.retry.millis ["
                            + tmpL + "], cannot be less than zero, setting to zero");
            tmpL = 0;
        }

        retryIntervalMs = tmpL;
    }

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final TokenBucket tokenBucket;

    public RetrievalTask(TokenBucket tokenBucket) {
        this.tokenBucket = tokenBucket;
    }

    public boolean processRetrieval(Retrieval retrieval, int priority) {
        boolean success = false;

        try {
            // send this retrieval to be processed
            IRetrievalResponse retrievalResponse = retrieveData(retrieval,
                    priority);

            if (retrievalResponse != null) {
                success = processRetrievedData(retrieval, retrievalResponse);
            } else {
                logger.error("Retrieval failed for retrieval [" + retrieval
                        + "]. No PDOs to store.");
            }
        } catch (Exception e) {
            logger.error("Error ocurred during retrieval [" + retrieval + "]",
                    e);
        }
        return success;
    }

    protected IRetrievalResponse retrieveData(Retrieval retrieval, int priority)
            throws Exception {
        ITimer timer = TimeUtil.getTimer();
        timer.start();
        IRetrievalResponse response = null;
        if (logger.isDebugEnabled()) {
            logger.debug("Starting Retrieval: Subscription: "
                    + retrieval.getSubscriptionName());
        }
        ServiceType serviceType = retrieval.getServiceType();

        RetrievalAdapter pra = ServiceTypeFactory
                .retrieveServiceRetrievalAdapter(serviceType);
        pra.setPriority(priority);
        pra.setTokenBucket(tokenBucket);

        // Perform the actual retrievals and transforms to plugin data
        // objects
        final RetrievalAttribute attXML = retrieval.getAttribute();

        IRetrievalRequestBuilder request = pra.createRequestMessage(retrieval);
        if (request != null) {

            logger.info("Translated provider attribute Request XML: "
                    + request.getRequest());

            int tries = 0;

            while (tries < maxTries) {
                try {
                    response = pra.performRequest(retrieval, request);
                    if (response != null) {
                        // success
                        break;
                    }
                } catch (Exception e) {
                    EventBus.publish(new RetrievalEvent(e.getMessage()));
                    logger.error("Error while processing retrieval request "
                            + request.getRequest() + "(tries = " + (tries + 1)
                            + ", " + retrieval + ")", e);
                }
                tries += 1;
                try {
                    Thread.sleep(retryIntervalMs);
                } catch (InterruptedException e2) {
                    break;
                }
            }
        } else {
            // null response
            throw new IllegalStateException("Null response for service: "
                    + serviceType + " original: " + attXML.toString());
        }

        timer.stop();
        logger.info("Retrieval Processing for [" + retrieval + "] took "
                + timer.getElapsedTime() + " ms");

        return response;
    }

    /**
     * Perform network specific routing.
     *
     * @param retrieval
     * @param retrievalResponse
     * @return
     */
    protected abstract boolean processRetrievedData(Retrieval retrieval,
            IRetrievalResponse retrievalResponse) throws Exception;
}
