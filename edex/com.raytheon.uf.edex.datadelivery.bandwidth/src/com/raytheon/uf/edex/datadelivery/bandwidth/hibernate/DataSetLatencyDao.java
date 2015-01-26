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

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.SessionManagedDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.DataSetLatency;

/**
 * Hibernate {@link IDataSetLatencyDao}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 01, 2014 3550       ccody       Initial version
 * 
 * </pre>
 * 
 * @author ccody
 * @version 1.0
 */
public class DataSetLatencyDao extends SessionManagedDao<Long, DataSetLatency>
        implements IDataSetLatencyDao {

    private static final String DELETE_UP_TO_BASE_REF_TIME = "delete from DataSetLatency sl where sl.createdTimestamp = :createdTimestamp";

    private static final String DELETE_BY_NAME_AND_PROVIDER = "delete from DataSetLatency sl where sl.dataSetName = :dataSetName AND sl.providerName = :providerName";

    private static final String GET_FOR_DATA_SET_AND_PROVIDER_NAME = "from DataSetLatency  sl where sl.dataSetName = :dataSetName AND sl.providerName = :providerName";

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public DataSetLatency getByDataSetNameAndProvider(String dataSetName,
            String providerName) {
        return uniqueResult(GET_FOR_DATA_SET_AND_PROVIDER_NAME, "dataSetName",
                dataSetName, "providerName", providerName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public void deleteUpToBaseRefTime(long timeToDeleteUpTo)
            throws DataAccessLayerException {
        executeHQLStatement(DELETE_UP_TO_BASE_REF_TIME, "createdTimestamp",
                timeToDeleteUpTo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public void deleteByDataSetNameAndProvider(String dataSetName,
            String providerName) throws DataAccessLayerException {
        executeHQLStatement(DELETE_BY_NAME_AND_PROVIDER, "dataSetName",
                dataSetName, "providerName", providerName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public void create(final DataSetLatency obj) {
        super.create(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public void createOrUpdate(final DataSetLatency obj) {
        super.createOrUpdate(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public void update(final DataSetLatency obj) {
        super.update(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public void delete(final DataSetLatency obj) {
        super.delete(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<DataSetLatency> getEntityClass() {
        return DataSetLatency.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyState(IDataSetLatencyDao dataSetLatencyDao) {
        deleteAll(getAll());
        for (DataSetLatency dataSetLatency : dataSetLatencyDao.getAll()) {
            create(dataSetLatency.copy());
        }
    }

}