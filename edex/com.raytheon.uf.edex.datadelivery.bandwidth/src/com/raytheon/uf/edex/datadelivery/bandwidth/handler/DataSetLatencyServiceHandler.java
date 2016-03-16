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
package com.raytheon.uf.edex.datadelivery.bandwidth.handler;

import com.raytheon.uf.common.auth.exception.AuthorizationException;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.datadelivery.bandwidth.datasetlatency.DataSetLatencyRequest;
import com.raytheon.uf.common.datadelivery.bandwidth.datasetlatency.DataSetLatencyResponse;
import com.raytheon.uf.common.datadelivery.bandwidth.datasetlatency.DataSetLatencyService;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.auth.req.AbstractPrivilegedRequestHandler;
import com.raytheon.uf.edex.auth.resp.AuthorizationResponse;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.datadelivery.bandwidth.hibernate.DataSetLatencyDao;

/**
 * Handles request from the {@link DataSetLatencyService}.
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

public class DataSetLatencyServiceHandler extends
        AbstractPrivilegedRequestHandler<DataSetLatencyRequest> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DataSetLatencyServiceHandler.class);

    /** Persistence DAO for DataSetLatency **/
    private DataSetLatencyDao dataSetLatencyDao;

    /**
     * Constructor.
     * 
     */
    public DataSetLatencyServiceHandler() {
    }

    /**
     * dataSetLatencyDao Property setter .
     * 
     * @param dataSetLatencyDao
     *            the DataSetLatency DAO
     */
    public void setDataSetLatencyDao(DataSetLatencyDao dataSetLatencyDao) {
        this.dataSetLatencyDao = dataSetLatencyDao;
    }

    /**
     * dataSetLatencyDao Property getter .
     * 
     * @return dataSetLatencyDao the DataSetLatency DAO
     */
    public DataSetLatencyDao getDataSetLatencyDao() {
        return (this.dataSetLatencyDao);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSetLatencyResponse handleRequest(DataSetLatencyRequest request)
            throws Exception {
        final IUser user = request.getUser();
        DataSetLatencyResponse response = new DataSetLatencyResponse();
        switch (request.getType()) {
        case DELETE:
            String dataSetName = request.getDataSetName();
            String providerName = request.getProviderName();
            response = handleDelete(dataSetName, providerName, user);
            break;
        }
        return response;
    }

    /**
     * Handles the delete of a DataSetLatency.
     * 
     * @param dataSetName
     *            DataSetLatency Data Set Name
     * @param dataSetName
     *            DataSetLatency Provider Name
     * @param user
     * @return DataSetLatencyResponse;
     * @throws RegistryHandlerException
     */
    private DataSetLatencyResponse handleDelete(String dataSetName,
            String providerName, IUser user) throws RegistryHandlerException {

        DataSetLatencyResponse response = new DataSetLatencyResponse();

        try {
            dataSetLatencyDao.deleteByDataSetNameAndProvider(dataSetName,
                    providerName);
            response.setSuccessful(true);
        } catch (DataAccessLayerException ex) {
            String messageString = ex.getMessage();
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to Delete DataSetLatency for [DataSetName: "
                            + dataSetName + ", Provider: " + providerName
                            + "]\n" + messageString, ex);

            response.setSuccessful(false);
            response.setMessageString(messageString);
        }

        return (response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthorizationResponse authorized(IUser user,
            DataSetLatencyRequest request) throws AuthorizationException {
        return new AuthorizationResponse(true);
    }

}
