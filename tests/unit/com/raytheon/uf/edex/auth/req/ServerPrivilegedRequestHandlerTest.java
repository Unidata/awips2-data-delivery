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
package com.raytheon.uf.edex.auth.req;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.raytheon.uf.common.auth.resp.SuccessfulExecution;
import com.raytheon.uf.common.datadelivery.bandwidth.BandwidthRequest;
import com.raytheon.uf.common.localization.msgs.GetServersRequest;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.common.serialization.comm.IServerRequest;
import com.raytheon.uf.edex.requestsrv.HandlerRegistry;
import com.raytheon.uf.edex.requestsrv.RequestServiceExecutor;
import com.raytheon.uf.edex.requestsrv.request.ServerPrivilegedRequestHandler;
import com.raytheon.uf.edex.requestsrv.request.ServerPrivilegedRequestHandler.ServerPrivilegedRequest;

/**
 * Test {@link ServerPrivilegedRequestHandler}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 15, 2012 1286       djohnson     Initial creation
 * Aug 26, 2014 3365       ccody        Separate Data Delivery tests out of AWIPS 2 baseline.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ServerPrivilegedRequestHandlerTest {

    private static final BandwidthRequest BANDWIDTH_REQUEST = new BandwidthRequest();

    private final HandlerRegistry registry = mock(HandlerRegistry.class);

    private final IRequestHandler bandwidthHandler = mock(IRequestHandler.class);

    private final ServerPrivilegedRequestHandler handler = new ServerPrivilegedRequestHandler(
            registry);

    @Before
    public void setUp() {
        //REMOVED RemoteRequestServer.getInstance().setRegistry(registry);
        //This will set the private final Registry for the RequestServiceExecutor instance
        RequestServiceExecutor rse = new RequestServiceExecutor(registry);

        when(
                registry.getRequestHandler(BandwidthRequest.class
                        .getCanonicalName())).thenReturn(bandwidthHandler);
    }

    @Test
    public void testUnwrapsActualRequestAndSendsToHandler() throws Exception {
        // This handler should never be called, used as a sanity check
        final IRequestHandler getServersHandler = mock(IRequestHandler.class);
        when(
                registry.getRequestHandler(GetServersRequest.class
                        .getCanonicalName())).thenReturn(getServersHandler);

        ServerPrivilegedRequest request = new ServerPrivilegedRequest(
                BANDWIDTH_REQUEST);

        handler.handleRequest(request);

        verify(bandwidthHandler).handleRequest(BANDWIDTH_REQUEST);
        verify(getServersHandler, never()).handleRequest(
                any(IServerRequest.class));
    }

    @Test
    public void testActualResponseIsWrappedInSuccessfulExecution()
            throws Exception {
        // When a bandwidth handler request is made, return a flag object for a
        // response
        Object actualResponse = new Object();
        when(bandwidthHandler.handleRequest(any(IServerRequest.class)))
                .thenReturn(actualResponse);

        ServerPrivilegedRequest request = new ServerPrivilegedRequest(
                BANDWIDTH_REQUEST);

        // Process through the server privileged request handler
        Object response = handler.handleRequest(request);

        // We expect the actual response to have been wrapped
        assertTrue(
                "Expected the actual response to be wrapped in a SuccessfulResponse object!",
                response instanceof SuccessfulExecution);
        SuccessfulExecution successfulExecution = (SuccessfulExecution) response;

        // Now make sure the actual response flag object was wrapped
        assertSame(
                "Expected the successful execution to contain the actual response!",
                actualResponse, successfulExecution.getResponse());
    }
}
