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
package com.raytheon.uf.edex.datadelivery.retrieval.adapters;

import java.util.Map;

import com.raytheon.uf.common.datadelivery.event.retrieval.AdhocDataRetrievalEvent;
import com.raytheon.uf.common.datadelivery.event.retrieval.DataRetrievalEvent;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval.SubscriptionType;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.event.EventBus;
import com.raytheon.uf.common.util.rate.TokenBucket;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalRequestBuilder;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalResponse;

/**
 * Abstract Provider Retrieval Adapter
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 07, 2011           dhladky   Initial creation
 * May 22, 2017  6130     tjensen   Add RetrievalRequestRecord to
 *                                  processResponse
 * Jun 05, 2017  6222     tgurney   Add rate-limiting fields
 * Jun 23, 2017  6322     tgurney   performRequest() throws Exception
 * Jul 25, 2017  6186     rjpeter   Update signature
 *
 * </pre>
 *
 * @author dhladky
 */

public abstract class RetrievalAdapter<T extends Time, C extends Coverage>
        implements IRetrievalAdapter<T, C> {

    private TokenBucket tokenBucket;

    private int priority;

    @Override
    public abstract IRetrievalRequestBuilder<T, C> createRequestMessage(
            Retrieval<T, C> retrieval);

    @Override
    public abstract RetrievalResponse performRequest(Retrieval<T, C> retrieval,
            IRetrievalRequestBuilder<T, C> request) throws Exception;

    @Override
    public abstract Map<String, PluginDataObject[]> processResponse(
            Retrieval<T, C> retrieval, IRetrievalResponse response)
            throws TranslationException;

    @Override
    public TokenBucket getTokenBucket() {
        if (Boolean.getBoolean("datadelivery.disableRateLimiter")) {
            return null;
        }
        return tokenBucket;
    }

    @Override
    public void setTokenBucket(TokenBucket tokenBucket) {
        this.tokenBucket = tokenBucket;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    protected void generateRetrievalEvent(Retrieval retrieval,
            long bytesDownloaded) {
        boolean isAdhoc = retrieval.getSubscriptionType() != null && retrieval
                .getSubscriptionType().equals(SubscriptionType.AD_HOC);
        DataRetrievalEvent event = isAdhoc ? new AdhocDataRetrievalEvent()
                : new DataRetrievalEvent();
        event.setId(retrieval.getSubscriptionName());
        event.setOwner(retrieval.getOwner());
        event.setNetwork(retrieval.getNetwork().name());
        event.setProvider(retrieval.getProvider());
        event.setBytes(bytesDownloaded);
        EventBus.publish(event);
    }

    public static class TranslationException extends Exception {

        private static final long serialVersionUID = -630731524436352713L;

        public TranslationException() {
            super();
        }

        public TranslationException(String message, Throwable cause) {
            super(message, cause);
        }

        public TranslationException(String message) {
            super(message);
        }

        public TranslationException(Throwable cause) {
            super(cause);
        }
    }
}
