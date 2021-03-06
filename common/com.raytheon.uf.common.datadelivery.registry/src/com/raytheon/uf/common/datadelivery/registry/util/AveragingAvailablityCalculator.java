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
package com.raytheon.uf.common.datadelivery.registry.util;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.GriddedDataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.handlers.DataSetMetaDataHandler;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;

/**
 *
 * Implementation of {@link IDataSetAvailablityCalculator}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Nov 09, 2012  1286     djohnson  Add SW history.
 * Jan 06, 2014  2636     mpduff    Changed how offset is determined.
 * May 15, 2014  3113     mpduff    Calculate availability offset for gridded
 *                                  data sets without cycles.
 * Aug 29, 2014  3446     bphillip  Changed cache timeout
 * May 27, 2015  4531     dhladky   Remove excessive Calendar references.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * Aug 02, 2017  6186     rjpeter   Update cycle handling.
 *
 * </pre>
 *
 * @author djohnson
 */
public class AveragingAvailablityCalculator {
    private final LoadingCache<NameProviderKey, List<DataSetMetaData>> cache = CacheBuilder
            .newBuilder().maximumSize(1000)
            .expireAfterWrite(20, TimeUnit.SECONDS)
            .build(new CacheLoader<NameProviderKey, List<DataSetMetaData>>() {
                @Override
                public List<DataSetMetaData> load(NameProviderKey key)
                        throws RegistryHandlerException {
                    return handler.getByDataSet(key.getName(),
                            key.getProvider());
                }
            });

    private final DataSetMetaDataHandler handler;

    /**
     * Constructor.
     *
     * @param handler
     *            The DataSetMetaDataHandler
     */
    public AveragingAvailablityCalculator(DataSetMetaDataHandler handler) {
        this.handler = handler;
    }

    /**
     * Get the average dataset offset for the provided subscription
     *
     * @param subscription
     *            The subscription
     *
     * @param referenceTime
     *            The base reference time
     *
     * @return The number of minutes of latency to expect.
     * @throws RegistryHandlerException
     */
    public int getDataSetAvailablityOffset(Subscription subscription,
            Date referenceTime) throws RegistryHandlerException {
        int offset = 0;
        NameProviderKey key = new NameProviderKey(subscription.getDataSetName(),
                subscription.getProvider());
        List<DataSetMetaData> records = null;

        try {
            records = cache.get(key);
            if (!CollectionUtil.isNullOrEmpty(records)) {
                DataSetMetaData md = records.get(0);
                if (md instanceof GriddedDataSetMetaData) {
                    offset = getOffsetForGrid(records, referenceTime);
                }
                // No availability delay for point data.
            }
        } catch (ExecutionException e) {
            throw new RegistryHandlerException(e);
        }

        return offset;
    }

    /**
     * Get the availability offset for gridded data.
     *
     * @param records
     *            List of DataSetMetaData records
     * @param referenceTime
     *            The data's base reference time
     * @return The offset in minutes
     */
    private int getOffsetForGrid(List<DataSetMetaData> records,
            Date referenceTime) {

        int cycle = TimeUtil.newGmtCalendar(referenceTime)
                .get(Calendar.HOUR_OF_DAY);
        int count = 0;
        int total = 0;
        for (DataSetMetaData md : records) {
            GriddedDataSetMetaData gmd = (GriddedDataSetMetaData) md;
            List<Integer> cycleTimes = gmd.getTime().getCycleTimes();
            if (cycleTimes.isEmpty() || cycleTimes.contains(cycle)) {
                total += gmd.getAvailabilityOffset();
                count++;
                if (count == 10) {
                    break;
                }
            }
        }

        if (count > 0) {
            return total / count;
        }

        return 0;
    }
}