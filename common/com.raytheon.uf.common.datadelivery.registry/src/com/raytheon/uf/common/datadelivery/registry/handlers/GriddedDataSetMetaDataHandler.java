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
package com.raytheon.uf.common.datadelivery.registry.handlers;

import java.util.Date;

import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.GriddedDataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.ebxml.DataSetMetaDataQuery;
import com.raytheon.uf.common.datadelivery.registry.ebxml.GriddedDataSetMetaDataQuery;
import com.raytheon.uf.common.registry.RegistryQueryResponse;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.time.util.ImmutableDate;

/**
 * GriddedDataSetMetaData registry handler.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 03, 2012 1241      djohnson     Initial creation
 * Jun 24, 2013 2106      djohnson     Now composes a registryHandler.
 * Mar 16, 2016 3919      tjensen      Cleanup unneeded interfaces
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
public class GriddedDataSetMetaDataHandler extends
        BaseDataSetMetaDataHandler<DataSetMetaData, DataSetMetaDataQuery> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<DataSetMetaData> getRegistryObjectClass() {
        return DataSetMetaData.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataSetMetaDataQuery getQuery() {
        return new DataSetMetaDataQuery();
    }

    /**
     * Return the {@link DataSetMetaData} for the dataset, the specified
     * date/time and cycle.
     * 
     * @param dataSetName
     *            the data set name
     * @param providerName
     *            the provider name
     * @param cycle
     *            the cycle
     * @param date
     *            the date
     * @return the object, or null if none found
     * @throws RegistryHandlerException
     *             on error
     */
    public GriddedDataSetMetaData getByDataSetDateAndCycle(String dataSetName,
            String providerName, int cycle, Date date)
            throws RegistryHandlerException {
        GriddedDataSetMetaDataQuery query = new GriddedDataSetMetaDataQuery();
        query.setDataSetName(dataSetName);
        query.setProviderName(providerName);
        query.setCycle(cycle);
        query.setDate(new ImmutableDate(date));

        RegistryQueryResponse<GriddedDataSetMetaData> response = registryHandler
                .getObjects(query);

        checkResponse(response, "getByDataSetDateAndCycle");

        return response.getSingleResult();
    }
}
