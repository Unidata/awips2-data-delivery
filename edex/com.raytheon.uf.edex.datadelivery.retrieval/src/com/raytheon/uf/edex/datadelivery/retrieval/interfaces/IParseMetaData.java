package com.raytheon.uf.edex.datadelivery.retrieval.interfaces;

import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.DataSet;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.URLParserInfo;

import opendap.dap.NoSuchAttributeException;

/**
 * Parse Crawl MetaData Interface
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------------------------------
 * Feb 20, 2011  218      dhladky   Initial creation
 * Sep 30, 2013  1797     dhladky   Generics
 * Jul 08, 2014  3120     dhladky   Accomodate PDA
 * Apr 14, 2015  4400     dhladky   Updated to DAP2 protocol.
 * Feb 16, 2016  5365     dhladky   Interface update.
 * Mar 08, 2017  6089     tjensen   Drop date format from parseMetaData calls
 * Apr 05, 2017  1045     tjensen   Add Coverage generics for DataSetMetaData
 * Oct 19, 2017  6465     tjensen   Rename Collections to URLParserInfo
 *
 * </pre>
 *
 * @author dhladky
 */

public interface IParseMetaData<O extends Object> {

    /**
     * common store method
     *
     * @param metaDatas
     * @param dataSet
     */
    @SuppressWarnings("rawtypes")
    void storeMetaData(List<DataSetMetaData<?, ?>> metaDatas, DataSet dataSet);

    /**
     * Common singular store method
     *
     * @param metaData
     */
    void storeMetaData(final DataSetMetaData<?, ?> metaData);

    /**
     * Set the dataset name in the registry
     *
     * @param dataSetToStore
     */
    @SuppressWarnings("rawtypes")
    void storeDataSetName(DataSet dataSetToStore);

    /**
     * Store a provider to the registry
     *
     * @param provider
     */
    void storeProvider(final Provider provider);

    /**
     * Store a meteorological paramter to the registry
     *
     * @param parameter
     */
    void storeParameter(Parameter parameter);

    /**
     * Determine dataset availability
     *
     * @param dataSetName
     * @param startMillis
     * @return
     */
    int getDataSetAvailabilityTime(String dataSetName, long startMillis);

    /**
     * Crawler OpenDAP interface for metadata
     *
     * @param provider
     * @param store
     * @param collection
     * @param dataDateFormat
     * @return
     * @throws NoSuchAttributeException
     */
    List<DataSetMetaData<?, ?>> parseMetaData(Provider provider, O object,
            URLParserInfo collection, String dataDateFormat)
            throws NoSuchAttributeException;

    /**
     * OGC interface for metadata parsing
     *
     * @param provider
     * @param object
     * @param isMetaData
     * @throws Exception
     */
    void parseMetaData(Provider provider, O object, boolean isMetaData)
            throws Exception;

}
