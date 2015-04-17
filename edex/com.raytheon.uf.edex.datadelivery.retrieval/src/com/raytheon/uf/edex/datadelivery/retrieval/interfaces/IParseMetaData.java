package com.raytheon.uf.edex.datadelivery.retrieval.interfaces;

import java.util.List;

import opendap.dap.NoSuchAttributeException;

import com.raytheon.uf.common.datadelivery.registry.Collection;
import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Provider;

/**
 * Parse Crawl MetaData Interface
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 20, 2011    218      dhladky     Initial creation
 * Sept 30, 2013   1797     dhladky     Generics
 * July 08, 2014   3120     dhladky     Accomodate PDA
 * Apr  14, 2015   4400     dhladky     Updated to DAP2 protocol.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public interface IParseMetaData<O extends Object> {

    /**
     * common store method
     * @param metaDatas
     * @param dataSet
     */
    void storeMetaData(List<DataSetMetaData<?>> metaDatas, DataSet dataSet);
    
    /**
     * Common singular store method
     * @param metaData
     */
    void storeMetaData(final DataSetMetaData<?> metaData);
    
    /**
     * Set the dataset name in the registry
     * @param dataSetToStore
     */
    void storeDataSetName(DataSet dataSetToStore);
    
    /**
     * Store a provider to the registry
     * @param provider
     */
    void storeProvider(final Provider provider);
    
    /**
     * Store a meteorological paramter to the registry
     * @param parameter
     */
    void storeParameter(Parameter parameter);
    
    /**
     * Determine dataset availability
     * @param dataSetName
     * @param startMillis
     * @return
     */
    int getDataSetAvailabilityTime(String dataSetName, long startMillis);
    
    /**
     * Crawler OpenDAP interface for metadata
     * @param provider
     * @param store
     * @param collection
     * @param dataDateFormat
     * @return
     * @throws NoSuchAttributeException 
     */
    List<DataSetMetaData<?>> parseMetaData(Provider provider, O object,
            Collection collection, String dataDateFormat) throws NoSuchAttributeException;
    
    /**
     * OGC interface for metadata parsing
     * @param provider
     * @param dataDateFormat
     * @param object
     * @throws Exception
     */
    void parseMetaData(Provider provider, String dataDateFormat, O object) throws Exception;

}
