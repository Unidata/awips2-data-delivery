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

package com.raytheon.uf.edex.datadelivery.retrieval.opendap;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.opendap.InputStreamWrapper;
import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.common.util.SizeUtil;
import com.raytheon.uf.common.util.rate.TokenBucket;
import com.raytheon.uf.common.util.stream.CountingInputStream;
import com.raytheon.uf.common.util.stream.RateLimitingInputStream;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalRequestBuilder;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalResponse;

/**
 * OpenDAP Provider Retrieval Adapter
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 07, 2011           dhladky   Initial creation
 * Jun 28, 2012  819      djohnson  Use utility class for DConnect.
 * Jul 25, 2012  955      djohnson  Make package-private.
 * Feb 05, 2013  1580     mpduff    EventBus refactor.
 * Feb 12, 2013  1543     djohnson  The payload can just be an arbitrary object,
 *                                  implementations can define an array if
 *                                  required.
 * Sep 19, 0201  2388     dhladky   Logging for failed requests.
 * Apr 12, 2015  4400     dhladky   Upgraded to DAP2 and preserved backward
 *                                  compatibility.
 * Mar 23, 2017  5988     tjensen   Improved logging
 * May 22, 2017  6130     tjensen   Add RetrievalRequestRecord to
 *                                  processResponse
 * Jun 22, 2017  6222     tgurney   Use token bucket to rate-limit requests
 * Jun 23, 2017  6322     tgurney   performRequest() throws Exception
 * Jul 27, 2017  6186     rjpeter   Use Retrieval
 *
 * </pre>
 *
 * @author dhladky
 */

class OpenDAPRetrievalAdapter
        extends RetrievalAdapter<GriddedTime, GriddedCoverage> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(OpenDAPRetrievalAdapter.class);

    /** convert if older XDODS version **/
    private final boolean isDods = DodsUtils.isOlderXDODSVersion();

    /**
     * Wraps an input stream with counting and (optionally) rate limiting
     * streams.
     */
    private static class CountingInputStreamWrapper
            implements InputStreamWrapper {

        private TokenBucket tokenBucket;

        private double priority = TokenBucket.DEFAULT_WEIGHT;

        private CountingInputStream countingStream;

        public CountingInputStreamWrapper(TokenBucket tokenBucket,
                double priority) {
            this.tokenBucket = tokenBucket;
            this.priority = priority;
        }

        @Override
        public InputStream wrapStream(InputStream wrappedStream) {
            if (tokenBucket != null) {
                wrappedStream = new RateLimitingInputStream(wrappedStream,
                        tokenBucket, 1.0 / priority);
            }
            countingStream = new CountingInputStream(wrappedStream);
            return countingStream;
        }

        public long getTimeTakenMillis() {
            return countingStream.getLastReadTimeMillis()
                    - countingStream.getFirstReadTimeMillis();
        }

        public long getBytesRead() {
            return countingStream.getBytesRead();
        }
    }

    @Override
    public OpenDAPRequestBuilder createRequestMessage(
            Retrieval<GriddedTime, GriddedCoverage> retrieval) {
        return new OpenDAPRequestBuilder(retrieval);
    }

    @Override
    public RetrievalResponse performRequest(
            Retrieval<GriddedTime, GriddedCoverage> retrieval,
            IRetrievalRequestBuilder<GriddedTime, GriddedCoverage> request)
            throws Exception {

        Object data = null;
        if (isDods) {
            dods.dap.DConnect connect = OpenDAPConnectionUtil
                    .getDConnectDODS(request.getRequest());
            data = connect.getData(null);
        } else {
            CountingInputStreamWrapper streamWrapper = new CountingInputStreamWrapper(
                    getTokenBucket(), getPriority());
            opendap.dap.DConnect connect = OpenDAPConnectionUtil
                    .getDConnectDAP2(request.getRequest(), streamWrapper);
            data = connect.getData(null);
            statusHandler.info("Downloaded "
                    + SizeUtil.prettyByteSize(streamWrapper.getBytesRead())
                    + " in " + TimeUtil.prettyDuration(
                            streamWrapper.getTimeTakenMillis()));
            generateRetrievalEvent(retrieval, streamWrapper.getBytesRead());
        }

        OpenDapRetrievalResponse rval = null;

        if (data != null) {
            rval = new OpenDapRetrievalResponse();
            rval.setPayLoad(data);
        }

        return rval;
    }

    @Override
    public Map<String, PluginDataObject[]> processResponse(
            Retrieval<GriddedTime, GriddedCoverage> retrieval,
            IRetrievalResponse response) throws TranslationException {
        Map<String, PluginDataObject[]> map = new HashMap<>();

        OpenDAPTranslator translator;
        try {
            translator = getOpenDapTranslator(retrieval);
        } catch (InstantiationException e) {
            throw new TranslationException(
                    "Unable to instantiate a required class!", e);
        }

        OpenDapRetrievalResponse odResponse = (OpenDapRetrievalResponse) response;
        Object payload = odResponse.getPayLoad();

        if (payload != null) {
            PluginDataObject[] pdos = translator.asPluginDataObjects(payload);

            if (!CollectionUtil.isNullOrEmpty(pdos)) {
                String pluginName = pdos[0].getPluginName();
                map.put(pluginName, CollectionUtil.combine(
                        PluginDataObject.class, map.get(pluginName), pdos));
            }
        }

        return map;
    }

    OpenDAPTranslator getOpenDapTranslator(
            Retrieval<GriddedTime, GriddedCoverage> retrieval)
            throws InstantiationException {
        return new OpenDAPTranslator(retrieval);
    }
}
