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
package com.raytheon.uf.edex.ogc.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataLevelType;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.EnvelopeUtils;
import com.raytheon.uf.common.datadelivery.registry.Levels;
import com.raytheon.uf.common.datadelivery.registry.PointDataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.PointTime;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.WFSPointDataSet;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.ogc.common.db.SimpleDimension;
import com.raytheon.uf.edex.ogc.common.db.SimpleLayer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Collector addon for WFS collectors with a single layer that adds time
 * information to registry
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 23, 2013           bclement  Initial creation
 * Aug 08, 2013  2097     dhladky   Made operational
 * Aug 30, 2013  2098     dhladky   Improved
 * Sep 02, 2013  2098     dhladky   Updated how times are managed.
 * Sep 30, 2013  1797     dhladky   Generics
 * Oct 10, 2013  1797     bgonzale  Refactored registry Time objects.
 * Nov 06, 2013  2525     dhladky   Stop appending "/wfs"
 * Jan 13, 2014  2679     dhladky   Multiple ingest layer windows for a single
 *                                  request window.
 * Apr 13, 2014  3012     dhladky   Cleaned up.
 * Apr 05, 2017  1045     tjensen   Add Coverage generics for DataSetMetaData
 * May 25, 2017  6186     rjpeter   Refactored, combined tracking under WfsLayerInfo, correctly follow CollectorAddon pattern.
 * Aug 30, 2017  6412     tjensen   Changed to store insert times in metadata
 *                                  instead of obs times
 * Sep 26, 2017  6416     nabowle   Set availableOffset in metadata.
 *
 *
 * </pre>
 *
 * @author bclement
 */
public abstract class WfsRegistryCollectorAddon<D extends SimpleDimension, L extends SimpleLayer<D>, R extends PluginDataObject>
        extends
        RegistryCollectorAddon<D, L, R, WFSPointDataSet, PointDataSetMetaData> {

    protected class WfsLayerInfo {
        protected final WFSPointDataSet dataSet;

        protected final PointDataSetMetaData dataSetMetaData;

        protected final SortedSet<Date> dataTimes;

        protected Date earliestInsertTime;

        protected Date latestInsertTime;

        public WfsLayerInfo(WFSPointDataSet dataSet,
                PointDataSetMetaData dataSetMetaData) {
            this.dataSet = dataSet;
            this.dataSetMetaData = dataSetMetaData;
            this.dataTimes = new TreeSet<>();
        }

        public void clearData() {
            dataTimes.clear();
            earliestInsertTime = null;
            latestInsertTime = null;
        }

        public void addDataTime(Date time) {
            dataTimes.add(time);
        }

        public void addInsertTime(Date time) {
            if (earliestInsertTime == null || time.before(earliestInsertTime)) {
                earliestInsertTime = time;
            }

            if (latestInsertTime == null || time.after(latestInsertTime)) {
                latestInsertTime = time;
            }
        }
    }

    protected final Map<String, WfsLayerInfo> layers = new HashMap<>(4, 1);

    protected static final int[] X_OFFSETS = new int[] { 0, 360, -360 };

    /** Used to identify breaking character for URL and unique naming key **/
    public static final String UNIQUE_ID_SEPARATOR = ",";

    protected final String baseUrl;

    private static final int START_BUFFER_DEFAULT = 300;

    private static final int END_BUFFER_DEFAULT = 1;

    private static final int DEFAULT_OFFSET_MINS = 5;

    private static final int INSERT_START_TIME_BUFFER_IN_SECS;

    private static final int INSERT_END_TIME_BUFFER_IN_SECS;

    private static final int AVAILABILITY_OFFSET;

    static {
        int start_buffer = Integer.getInteger("dpa.insert-time-buffer.start",
                START_BUFFER_DEFAULT);
        int end_buffer = Integer.getInteger("dpa.insert-time-buffer.end",
                END_BUFFER_DEFAULT);
        if (start_buffer < 0) {
            start_buffer = START_BUFFER_DEFAULT;
        }
        if (end_buffer < 0) {
            end_buffer = END_BUFFER_DEFAULT;
        }

        INSERT_START_TIME_BUFFER_IN_SECS = start_buffer;

        INSERT_END_TIME_BUFFER_IN_SECS = end_buffer;

        ServiceConfig wfsServiceConfig = HarvesterServiceManager.getInstance()
                .getServiceConfig(ServiceType.WFS);

        int defaultOffset = DEFAULT_OFFSET_MINS;
        if (wfsServiceConfig != null) {
            try {
                String sOffset = wfsServiceConfig
                        .getConstantValue("DEFAULT_OFFSET");
                if (sOffset != null) {
                    defaultOffset = Integer.parseInt(sOffset);
                }
            } catch (NumberFormatException e) {
                // ignore.
            }
        }
        AVAILABILITY_OFFSET = defaultOffset;
    }

    public WfsRegistryCollectorAddon() {
        super();
        baseUrl = getConfiguration().getProvider().getConnection().getUrl();
        if (INSERT_END_TIME_BUFFER_IN_SECS
                + INSERT_START_TIME_BUFFER_IN_SECS < 300) {
            logger.warn(
                    "Insert time buffers do not add a minimum of 5 minutes. Start buffer: "
                            + INSERT_START_TIME_BUFFER_IN_SECS
                            + "; End buffer: "
                            + INSERT_END_TIME_BUFFER_IN_SECS);
        }
    }

    @Override
    public void onCollect(L layer, R record) {
        for (String layerName : getValidLayers(record)) {
            WfsLayerInfo layerInfo = layers.get(layerName);
            synchronized (layerInfo) {
                processRecord(layerInfo, record);
            }
        }
    }

    protected void processRecord(WfsLayerInfo layerInfo, R record) {
        layerInfo.addDataTime(getTime(record));
        layerInfo.addInsertTime(record.getInsertTime().getTime());
    }

    /**
     * extract point data time from record
     *
     * @param record
     * @return
     */
    protected Date getTime(R record) {
        /*
         * Track only the insert times for now, as that is what the data is
         * currently queried on.
         */
        Date insertTime = record.getInsertTime().getTime();
        return insertTime;
    }

    /**
     * Generates a layer for tracking and stores the dataSet to the registry.
     *
     * @param layer
     */
    protected void initializeLayerInfo(L layer) {
        String layerName = layer.getName();
        if (!validateLayer(layer)) {
            throw new IllegalArgumentException(
                    "Invalid Layer definition for Layer [" + layerName + "]");
        }

        // create the main point data set
        WFSPointDataSet ds = createDataSet(layerName);
        populateDataSet(ds, layer);
        storeDataSet(ds);

        PointDataSetMetaData dsmd = createDataSetMetaData(layerName);
        populateDataSetMetaData(dsmd, ds, layer);

        WfsLayerInfo layerInfo = new WfsLayerInfo(ds, dsmd);
        layers.put(layerName, layerInfo);
    }

    /**
     * Perform any necessary layer validation. This will ensure that minX < maxX
     * and minY < maxY.
     *
     * @param layer
     * @return
     */
    protected boolean validateLayer(L layer) {
        boolean rval = true;

        if (layer.getTargetMinx() > layer.getTargetMaxx()) {
            logger.error("Layer bounds for [" + layer.getName()
                    + "] are invalid. Minx [" + layer.getTargetMinx()
                    + "] cannot be greater than Maxx [" + layer.getTargetMaxx()
                    + "]");
            rval = false;
        }

        if (layer.getTargetMiny() > layer.getTargetMaxy()) {
            logger.error("Layer bounds for [" + layer.getName()
                    + "] are invalid. Miny [" + layer.getTargetMiny()
                    + "] cannot be greater than Maxy [" + layer.getTargetMaxy()
                    + "]");
            rval = false;
        }

        return rval;
    }

    @Override
    public void onFinish() {
        for (Map.Entry<String, WfsLayerInfo> entry : layers.entrySet()) {
            String layerName = entry.getKey();
            WfsLayerInfo layerInfo = entry.getValue();

            synchronized (layerInfo) {
                if (layerInfo.dataTimes.isEmpty()) {
                    logger.info("No new metadata to update " + layerName);
                    continue;
                }

                logger.info("MetaData update " + layerName + ": times: "
                        + layerInfo.dataTimes.size());

                PointDataSetMetaData dsmd = layerInfo.dataSetMetaData;
                PointTime time = new PointTime();

                List<Date> timeList = new ArrayList<>();
                Date startTime = new Date();
                startTime.setTime(layerInfo.earliestInsertTime.getTime()
                        - (TimeUtil.MILLIS_PER_SECOND
                                * INSERT_START_TIME_BUFFER_IN_SECS));
                Date endTime = new Date();
                endTime.setTime(layerInfo.latestInsertTime.getTime()
                        + (TimeUtil.MILLIS_PER_SECOND
                                * INSERT_END_TIME_BUFFER_IN_SECS));

                timeList.add(startTime);
                timeList.add(endTime);
                time.setTimes(new ArrayList<>(timeList));

                time.setStart(startTime);
                time.setEnd(endTime);
                time.setNumTimes(time.getTimes().size());
                dsmd.setTime(time);
                StringBuilder url = new StringBuilder(160);
                url.append(baseUrl).append(UNIQUE_ID_SEPARATOR)
                        .append(dsmd.getDataSetName())
                        .append(UNIQUE_ID_SEPARATOR)
                        .append(layerInfo.latestInsertTime);
                dsmd.setUrl(url.toString());
                storeMetaData(dsmd);
                layerInfo.clearData();
            }
        }
    }

    @Override
    public WFSPointDataSet createDataSet(String layerName) {
        return new WFSPointDataSet();
    }

    @Override
    protected void populateDataSet(WFSPointDataSet wpds, L layer) {
        Coverage coverage = generateCoverage(layer);
        wpds.setCoverage(coverage);
        wpds.setDataSetName(layer.getName());
        wpds.setProviderName(getConfiguration().getProvider().getName());
        wpds.setCollectionName(layer.getName());
        wpds.setParameters(getParameters(layer));
        wpds.setTime(new PointTime());
    }

    @Override
    protected PointDataSetMetaData createDataSetMetaData(String layerName) {
        return new PointDataSetMetaData();
    }

    @Override
    protected void populateDataSetMetaData(PointDataSetMetaData dataSetMetaData,
            WFSPointDataSet dataSet, L layer) {
        StringBuilder sb = new StringBuilder();
        Envelope env = dataSet.getCoverage().getEnvelope();

        sb.append(dataSet.getDataSetName()).append(" ");
        sb.append(env.getMaxX()).append(", ");
        sb.append(env.getMinY()).append(" : ");
        sb.append(env.getMinX()).append(", ");
        sb.append(env.getMaxY());

        dataSetMetaData.setDataSetName(dataSet.getDataSetName());
        dataSetMetaData.setDataSetDescription(sb.toString());
        dataSetMetaData.setProviderName(dataSet.getProviderName());
        dataSetMetaData.setAvailabilityOffset(AVAILABILITY_OFFSET);
    }

    /**
     * Makes a new coverage. Layer needs to be defined as min < max.
     *
     * @param layer
     * @return Coverage
     */
    protected Coverage generateCoverage(L layer) {
        Coverage coverage = new Coverage();
        Coordinate lowerRight = new Coordinate(layer.getTargetMaxx(),
                layer.getTargetMiny());
        Coordinate upperLeft = new Coordinate(layer.getTargetMinx(),
                layer.getTargetMaxy());
        ReferencedEnvelope re = EnvelopeUtils.createLatLonEnvelope(lowerRight,
                upperLeft);
        coverage.setEnvelope(re);

        return coverage;
    }

    /**
     * WFS uses Point Data type
     *
     * @return
     */
    @Override
    public DataType getDataType() {
        return DataType.POINT;
    }

    @Override
    public Levels getLevels(DataLevelType type, String collectionName) {
        // not implement in point data yet
        return null;
    }

    @Override
    public void onPurgeAll() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onPurgeExpired(Set<Date> timesToKeep) {
        // TODO Auto-generated method stub
    }

    @Override
    protected Coverage getCoverage(String layerName) {
        Coverage rval = null;
        WfsLayerInfo layerInfo = layers.get(layerName);

        if (layerInfo != null) {
            rval = layerInfo.dataSet.getCoverage();
        }

        return rval;
    }

    /**
     * Filter geographically
     *
     * @param pdos
     */
    public PluginDataObject[] filter(PluginDataObject[] pdos) {

        Collection<PluginDataObject> withinGeoConstraint = new ArrayList<>(
                pdos.length);

        for (PluginDataObject record : pdos) {
            if (record == null) {
                continue;
            }

            @SuppressWarnings("unchecked")
            R rec = (R) record;

            if (!getValidLayers(rec).isEmpty()) {
                withinGeoConstraint.add(record);
            }
        }

        int size = withinGeoConstraint.size();
        if (size == pdos.length) {
            return pdos;
        }

        return withinGeoConstraint.toArray(new PluginDataObject[size]);
    }

    @Override
    public Set<String> getValidLayers(R record) {
        Coordinate c = null;
        ISpatialObject spatial = getSpatial(record);

        if (spatial != null) {
            c = spatial.getGeometry().getCoordinate();
        }

        if (c == null) {
            return Collections.emptySet();
        }

        Set<String> rval = new HashSet<>(layers.size(), 1);

        // Figure out which DSMDs to tally this record too.
        for (Map.Entry<String, WfsLayerInfo> entry : layers.entrySet()) {
            WfsLayerInfo layer = entry.getValue();
            Envelope e = layer.dataSet.getCoverage().getEnvelope();

            // handle world wrap scenarios
            for (int offset : X_OFFSETS) {
                Coordinate test = new Coordinate(c.x + offset, c.y, c.z);

                if (e.contains(test)) {
                    rval.add(entry.getKey());
                    break;
                }
            }
        }

        return rval;
    }

}
