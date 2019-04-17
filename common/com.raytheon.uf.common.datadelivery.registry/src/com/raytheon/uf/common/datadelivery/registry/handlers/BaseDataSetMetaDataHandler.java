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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.ebxml.DataSetMetaDataDatesQuery;
import com.raytheon.uf.common.datadelivery.registry.ebxml.DataSetMetaDataFilterableQuery;
import com.raytheon.uf.common.datadelivery.registry.ebxml.DataSetMetaDataQuery;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.registry.RegistryQueryResponse;
import com.raytheon.uf.common.registry.handler.BaseRegistryObjectHandler;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.ImmutableDate;

/**
 * DataSetMetaData registry handler.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 03, 2012  1241     djohnson  Initial creation
 * Oct 17, 2012  726      djohnson  Move in {@link #getByDataSet}.
 * Jun 24, 2013  2106     djohnson  Now composes a registryHandler.
 * Jul 28, 2014  2752     dhladky   Added new method functions for more
 *                                  efficient querying.
 * Feb 19, 2015  3998     dhladky   Streamlined adhoc sub processing.
 * Apr 05, 2017  1045     tjensen   Add Coverage generics DataSetMetaData
 * Apr 27, 2017  1045     tjensen   Add methods for moving datasets to check
 *                                  intersections
 * Apr 17, 2019  7755     skabasele Introduced a null check in  getByDataSetDate 
 *                                  to ensure that the method doesn't
 *                                  throw an null exception for cases when calling classes internally
 *                                  pass null as Date.
 *
 * </pre>
 *
 * @author djohnson
 * @version 1.0
 */

public abstract class BaseDataSetMetaDataHandler<T extends DataSetMetaData<?, ?>, QUERY extends DataSetMetaDataFilterableQuery<T>>
        extends BaseRegistryObjectHandler<T, QUERY>
        implements IBaseDataSetMetaDataHandler<T> {
    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(BaseDataSetMetaDataHandler.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ImmutableDate> getDatesForDataSet(String dataSetName,
            String providerName) throws RegistryHandlerException {
        DataSetMetaDataDatesQuery query = new DataSetMetaDataDatesQuery();
        query.setDataSetName(dataSetName);
        query.setProviderName(providerName);

        RegistryQueryResponse<ImmutableDate> response = registryHandler
                .getObjects(query);

        checkResponse(response, "getDatesForDataSet");

        return new HashSet<>(response.getResults());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> getByDataSet(String dataSetName, String providerName)
            throws RegistryHandlerException {
        QUERY query = getQuery();
        query.setDataSetName(dataSetName);
        query.setProviderName(providerName);

        RegistryQueryResponse<T> response = registryHandler.getObjects(query);

        checkResponse(response, "getByDataSet");

        return response.getResults();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public T getByDataSetDate(String dataSetName, String providerName,
            Date date) throws RegistryHandlerException {
        DataSetMetaDataQuery query = new DataSetMetaDataQuery();
        query.setDataSetName(dataSetName);
        query.setProviderName(providerName);
        if (date != null) {
            query.setDate(new ImmutableDate(date));
        }
        RegistryQueryResponse<DataSetMetaData> response = registryHandler
                .getObjects(query);

        checkResponse(response, "getByDataSetDate");

        return (T) response.getSingleResult();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<T> getDataSetMetaDataToDate(String dataSetName,
            String providerName, Date planEnd) throws RegistryHandlerException {

        /*
         * First, get date list for this dataSet. Then add only dates before
         * planEnd.
         */
        Set<ImmutableDate> dates = getDatesForDataSet(dataSetName,
                providerName);
        List<ImmutableDate> dateList = new ArrayList<>();
        for (ImmutableDate id : dates) {
            if (id.before(planEnd)) {
                dateList.add(id);
            }
        }

        // ensure proper sorting
        Collections.sort(dateList);

        /*
         * Now make our actual query for DSMD's, will return them in ordered by
         * date.
         */
        DataSetMetaDataQuery query = new DataSetMetaDataQuery();
        query.setDataSetName(dataSetName);
        query.setProviderName(providerName);
        query.setDates(dateList);

        RegistryQueryResponse<DataSetMetaData> response = registryHandler
                .getObjects(query);

        checkResponse(response, "getDataSetMetaToDate");

        return (List<T>) response.getResults();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getMostRecentDataSetMetaData(String dataSetName,
            String providerName) throws RegistryHandlerException {

        // First, get date list for this dataSet.
        Set<ImmutableDate> dates = getDatesForDataSet(dataSetName,
                providerName);
        // Then get the most recent one.
        ImmutableDate mostRecent = null;
        for (ImmutableDate id : dates) {
            if (mostRecent == null || id.after(mostRecent)) {
                mostRecent = id;
            }
        }
        // Now return a single DSMD object which is the most recent.
        return getByDataSetDate(dataSetName, providerName, mostRecent);
    }

    /**
     * Return the list of DataSetMetaData that match the given dataSetName and
     * provider name, and has a coverage area that intersect with the given
     * envelope. Results can be limited to improve performance
     *
     * @param dataSetName
     *            data set name
     * @param providerName
     *            provider name
     * @param subEnvelope
     *            envelope to intersect with
     * @param limit
     *            Number of results to return. If 0 or below, return all
     *            matches.
     * @return
     * @throws RegistryHandlerException
     */
    public List<T> getDataSetMetaDataByIntersection(String dataSetName,
            String providerName, ReferencedEnvelope subEnvelope, int limit)
            throws RegistryHandlerException {
        // Get all DataSetMetaData
        List<T> allMetaData = getDataSetMetaDataToDate(dataSetName,
                providerName, new Date());

        // Sort by date, with most recent first
        Collections.sort(allMetaData, DataSetMetaData.DATE_COMPARATOR);
        Collections.reverse(allMetaData);

        // Check for coverage envelope for intersection.
        List<T> matchingMetaData = new ArrayList<>();
        Map<Coverage, Boolean> intersectionMap = new HashMap<>(2);

        for (T myMetaData : allMetaData) {
            Coverage instanceCoverage = myMetaData.getInstanceCoverage();
            if (instanceCoverage != null) {
                /*
                 * Store results of intersections in a map, so if multiple
                 * metadata have the same coverage, we only have to perform the
                 * intersection once.
                 */
                Boolean coverageCheck = intersectionMap.get(instanceCoverage);
                if (coverageCheck == null) {
                    boolean intersects = false;
                    ReferencedEnvelope intersection;
                    try {
                        /*
                         * If performance becomes an issue, investigate caching
                         * the transforms by CRS for a faster reprojection.
                         */
                        intersection = MapUtil.reprojectAndIntersect(
                                subEnvelope, instanceCoverage.getEnvelope());

                        if (!intersection.isNull() && !intersection.isEmpty()) {
                            intersects = true;
                        }
                    } catch (TransformException e) {
                        // If intersection fails, mark it as not intersecting
                        statusHandler
                                .error("Error checking moving intersection with "
                                        + myMetaData.getUrl(), e);
                    }
                    intersectionMap.put(instanceCoverage, intersects);
                    coverageCheck = intersects;
                }

                if (coverageCheck.booleanValue()) {
                    matchingMetaData.add(myMetaData);
                }
            } else {
                /*
                 * If no instance coverage, it's not a moving dataset so assume
                 * all will match.
                 */
                matchingMetaData.add(myMetaData);
            }
            if (limit > 0 && matchingMetaData.size() >= limit) {
                break;
            }
        }

        return matchingMetaData;
    }

    /**
     * Return the most recent DataSetMetaData that match the given dataSetName
     * and provider name, and has a coverage area that intersect with the given
     * envelope.
     *
     * @param dataSetName
     *            data set name
     * @param providerName
     *            provider name
     * @param subEnvelope
     *            envelope to intersect with
     * @return
     * @throws RegistryHandlerException
     */
    public T getMostRecentDataSetMetaDataByIntersection(String dataSetName,
            String providerName, ReferencedEnvelope subEnvelope)
            throws RegistryHandlerException {
        List<T> mostRecent = getDataSetMetaDataByIntersection(dataSetName,
                providerName, subEnvelope, 1);
        if (mostRecent.isEmpty()) {
            return null;
        }
        return mostRecent.get(0);
    }

    /**
     * Return the list of Dates for the DataSetMetaData that match the given
     * dataSetName and provider name, and has a coverage area that intersect
     * with the given envelope.
     *
     * @param dataSetName
     *            data set name
     * @param providerName
     *            provider name
     * @param subEnvelope
     *            envelope to intersect with
     * @return
     * @throws RegistryHandlerException
     */
    public Set<ImmutableDate> getDatesForDataSetByIntersection(
            String dataSetName, String providerName,
            ReferencedEnvelope subEnvelope) throws RegistryHandlerException {
        Set<ImmutableDate> matchingDates = new HashSet<>();
        List<T> intersectingDataSets = getDataSetMetaDataByIntersection(
                dataSetName, providerName, subEnvelope, 0);
        for (T ids : intersectingDataSets) {
            matchingDates.add(ids.getDate());
        }
        return matchingDates;
    }
}
