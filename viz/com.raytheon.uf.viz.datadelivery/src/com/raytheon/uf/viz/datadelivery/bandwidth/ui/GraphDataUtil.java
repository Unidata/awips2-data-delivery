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
package com.raytheon.uf.viz.datadelivery.bandwidth.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.raytheon.uf.common.datadelivery.bandwidth.data.BandwidthGraphData;
import com.raytheon.uf.common.datadelivery.bandwidth.request.GraphDataRequest;
import com.raytheon.uf.common.datadelivery.bandwidth.response.GraphDataResponse;
import com.raytheon.uf.common.datadelivery.request.DataDeliveryConstants;
import com.raytheon.uf.common.serialization.comm.RequestRouter;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.exception.VizCommunicationException;

/**
 * 
 * This is a utility class used to get data for the bandwidth graph it
 * implements {@link Runnable} so you can retrieve data on a separate thread to
 * keep the UI thread from being blocked.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 12, 2012   1269     lvenable     Initial creation
 * Feb 14, 2013 1596       djohnson     Remove sysouts, correct statusHandler class, handle null response.
 * Mar 26, 2013 1827       djohnson     Graph data should be requested from data delivery.
 * Jan 29, 2014 2722       mpduff       Callback is now passed in.
 * Oct 03, 2014 2749       ccody        Add clearGraphData method.
 * Feb 03, 2015 4041       dhladky      Restructured to run off UI thread.
 * Mar 16, 2016 3919       tjensen      Cleanup unneeded interfaces
 * Jan 05, 2017 746        bsteffen     Don't ignore updates if job is running.
 * 
 * </pre>
 * 
 * @author lvenable
 * @version 1.0
 */
public class GraphDataUtil extends Job {

    /** UFStatus handler */
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GraphDataUtil.class);

    /** Callback called when the data has been updated */
    private final BandwidthCanvasComp dataUpdatedCB;

    /** Bandwidth graph data */
    private BandwidthGraphData graphData;

    /**
     * Constructor.
     * 
     * @param dataUpdatedCB
     *            Call back called when the data has been updated via separate
     *            thread.
     */
    public GraphDataUtil(BandwidthCanvasComp dataUpdatedCB) {
        super("Bandwidth Utilization Graph Update");
        this.dataUpdatedCB = dataUpdatedCB;
    }

    /**
     * schedule a retrieval
     */
    public void scheduleRetrieval() {
        this.schedule();
    }

    /**
     * Perform a data retrieval on the UI thread.
     */
    private void retrieveData() {
        GraphDataResponse response = sendRequest(new GraphDataRequest());

        if (response != null) {
            graphData = response.getGraphData();
        }
    }

    /**
     * Send a request for the bandwidth graph data.
     * 
     * @param req
     *            Graph data request.
     * @return The graph data response.
     */
    private GraphDataResponse sendRequest(GraphDataRequest req) {
        try {
            return (GraphDataResponse) RequestRouter.route(req,
                    DataDeliveryConstants.DATA_DELIVERY_SERVER);
        } catch (VizCommunicationException vizEx) {
            Throwable vizCause = vizEx.getCause();
            String commMessage = vizCause.getMessage();
            statusHandler.handle(Priority.ERROR, commMessage, vizEx);
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR, "Error Requesting Data", e);
        }

        return null;
    }

    /**
     * Callback will call this to retrieve current GraphData
     * 
     * @return
     */
    public BandwidthGraphData getGraphData() {
        return graphData;
    }

    /**
     * Initial call waits for first GraphData request to finish entirely.
     * 
     * @return
     */
    public BandwidthGraphData getGraphDataSynchronously() {

        retrieveData();
        return graphData;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        retrieveData();

        if (dataUpdatedCB != null) {
            dataUpdatedCB.dataUpdated();
        }

        return Status.OK_STATUS;
    }
}
