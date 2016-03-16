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
package com.raytheon.uf.common.datadelivery.bandwidth.datasetlatency;

import com.raytheon.uf.common.auth.req.BasePrivilegedServerService;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Service for interacting with the bandwidth manager.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 01, 2014 3550       ccody       Initial creation
 * Mar 16, 2016 3919       tjensen     Cleanup unneeded interfaces
 * 
 * </pre>
 * 
 * @author ccody
 * @version 1.0
 */
public class DataSetLatencyService extends
        BasePrivilegedServerService<DataSetLatencyRequest> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DataSetLatencyService.class);

    /**
     * Constructor.
     * 
     * @param serviceKey
     */
    public DataSetLatencyService(String serviceKey) {
        super(serviceKey);
    }

    public final boolean deleteByDataSetNameAndProvider(String dataSetName,
            String providerName) {
        boolean successful = true;
        try {
            DataSetLatencyRequest request = new DataSetLatencyRequest();
            request.setType(DataSetLatencyRequest.Type.DELETE);
            request.setDataSetName(dataSetName);
            request.setProviderName(providerName);
            DataSetLatencyResponse response = sendRequest(request,
                    DataSetLatencyResponse.class);

            if (response != null) {
                String msgString = response.getMessageString();
                if (msgString == null) {
                    msgString = "";
                }
                successful = response.getSuccessful();
                if (successful == true) {
                    statusHandler.handle(Priority.DEBUG,
                            "Deleted Data Set Latency for: [Data Set Name: "
                                    + dataSetName + ", Provider: "
                                    + providerName + "]\n " + msgString);
                } else {
                    statusHandler.handle(Priority.PROBLEM,
                            "Unable to delete Data Set Latency for: [Data Set Name: "
                                    + dataSetName + ", Provider: "
                                    + providerName + "]\n " + msgString);
                }
            }
        } catch (Exception e) {
            statusHandler
                    .handle(Priority.PROBLEM,
                            "Unable to delete Data Set Latency for: [Data Set Name: "
                                    + dataSetName + ", Provider: "
                                    + providerName + "]", e);
            return (false);
        }

        return (successful);
    }

}
