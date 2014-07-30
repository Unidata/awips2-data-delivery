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
import java.util.List;
import java.util.Set;

import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.registry.handler.IRegistryObjectHandler;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.time.util.ImmutableDate;

/**
 * A base registry handler for DataSetMetaData objects.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 04, 2012 1241       djohnson     Initial creation
 * Oct 17, 2012 0726       djohnson     Move in {@link #getByDataSet}.
 * Jul 28, 2014 2752       dhladky      Added new methods.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public interface IBaseDataSetMetaDataHandler<T extends DataSetMetaData> extends
        IRegistryObjectHandler<T> {

    /**
     * Return the set of available dates for a data set name and provider name.
     * 
     * @param dataSetName
     *            the data set name
     * @param providerName
     *            the provider name
     * @return the set of dates
     * @throws RegistryHandlerException
     *             on error
     */
    Set<ImmutableDate> getDatesForDataSet(String dataSetName,
            String providerName) throws RegistryHandlerException;

    /**
     * Retrieve the list of DataSetMetaData instances for the specified dataset.
     * 
     * @param dataSetName
     *            the name of the dataset
     * @param providerName
     *            the provider name
     * @return the list of DataSetMetaDatas
     * @throws RegistryHandlerException
     *             on error
     */
    List<T> getByDataSet(String dataSetName, String providerName)
            throws RegistryHandlerException;
    
    /**
     * Retrieve the DataSetMetaData instance for the specified date.
     * 
     * @param dataSetName
     *            the name of the dataset
     * @param providerName
     *            the provider name
     * @param date
     * @return DataSetMetaData
     * @throws RegistryHandlerException
     *             on error
     */
    T getByDataSetDate(String dataSetName, String providerName, Date date) throws RegistryHandlerException;

    /**
     * Retrieve the DataSetMetaData up to the Retrieval Plan end.
     * 
     * @param dataSetName
     *            the name of the dataset
     * @param providerName
     *            the provider name
     * @param date (end of plan date)
     * @return the list of DataSetMetaDatas ordered by Date
     * @throws RegistryHandlerException
     *             on error
     */
    List<T> getDataSetMetaDataToDate(String dataSetName, String providerName,
            Date planEnd) throws RegistryHandlerException;
    
}
