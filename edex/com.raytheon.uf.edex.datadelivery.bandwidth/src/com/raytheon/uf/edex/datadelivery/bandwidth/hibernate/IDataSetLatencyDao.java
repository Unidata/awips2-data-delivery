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
package com.raytheon.uf.edex.datadelivery.bandwidth.hibernate;

import java.util.List;

import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.DataSetLatency;

/**
 * Interface for a DAO that manages {@link DataSetLatency} instances.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 01, 2013 3550       ccody        Initial creation
 * 
 * </pre>
 * 
 * @author ccody
 * @version 1.0
 */

public interface IDataSetLatencyDao {

    /**
     * Create the DataSetLatency object.
     * 
     * @param dataSetLatency
     */
    void create(DataSetLatency dataSetLatency);

    /**
     * Create or Update the DataSetLatency object.
     * 
     * @param dataSetLatency
     */
    void createOrUpdate(DataSetLatency dataSetLatency);

    /**
     * Update the DataSetLatency object.
     * 
     * @param dataSetLatency
     */
    void update(DataSetLatency dataSetLatency);

    /**
     * Delete the DataSetLatency object.
     * 
     * @param dataSetLatency
     */
    void delete(DataSetLatency dataSetLatency);

    /**
     * Delete all DataSetLatency objects up to and including the specified time.
     * 
     * @param timeToDeleteUpTo
     * @throws DataAccessLayerException
     */
    void deleteUpToBaseRefTime(long timeToDeleteUpTo)
            throws DataAccessLayerException;

    /**
     * Delete DataSetLatency objects up to and including the specified time.
     * 
     * @param dataSetName
     *            Data Set Name column value
     * @param providerName
     *            Data Set Provider Name column value
     * @throws DataAccessLayerException
     */
    public void deleteByDataSetNameAndProvider(String dataSetName,
            String providerName) throws DataAccessLayerException;

    /**
     * Get {@link BandwidthDataSet} instances by the DataSetLatency object's
     * DataSet Name and DataSet Provider.
     * 
     * @param dataSetName
     * @param providerName
     * @return
     */
    DataSetLatency getByDataSetNameAndProvider(String dataSetName,
            String providerName);

    /**
     * Get all DataSetLatency objects.
     * 
     * @return all DataSetLatency objects for the network
     */
    List<DataSetLatency> getAll();

    /**
     * Copy the state from another DataSetLatency dao.
     * 
     * @param dataSetLatencyDao
     */
    void copyState(IDataSetLatencyDao dataSetLatencyDao);

}
