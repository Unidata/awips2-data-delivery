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
package com.raytheon.uf.edex.datadelivery.retrieval.pda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.datadelivery.registry.Collection;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataLevelType;
import com.raytheon.uf.common.datadelivery.registry.DataLevelType.LevelType;
import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.PDADataSet;
import com.raytheon.uf.common.datadelivery.registry.PDADataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.time.util.ImmutableDate;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.MetaDataParser;
import com.raytheon.uf.edex.datadelivery.retrieval.util.PDAMetaDataUtil;

import net.opengis.cat.csw.v_2_0_2.BriefRecordType;
import net.opengis.cat.csw.v_2_0_2.dc.elements.SimpleLiteral;
import net.opengis.ows.v_1_0_0.BoundingBoxType;

/**
 * Parse PDA metadata
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 17, 2014  3120     dhladky   Initial creation
 * Sep 04, 2014  3121     dhladky   Adjustments to parsing, simulation.
 * Sep 27, 2014  3127     dhladky   Added metaDataID for geographic subsetting.
 * Apr 27, 2015  4881     dhladky   PDA changed the structure of the file format
 *                                  messages.
 * Jan 20, 2016  5280     dhladky   Don't send datasetname separately.
 * Feb 16, 2016  5365     dhladky   Streamlined to exclude metaData updates for
 *                                  getRecords().
 * Jul 13, 2016  5752     tjensen   Refactor parseMetaData.
 * Jul 22, 2016  5752     tjensen   Add additional logging information
 * Aug 11, 2016  5752     tjensen   Removed unnecessary reordering in
 *                                  getCoverage
 * Aug 25, 2016  5752     tjensen   Change MetaData date to use start instead of
 *                                  create
 * Jan 27, 2017  6089     tjensen   Update to work with pipe delimited metadata
 * Apr 05, 2017  1045     tjensen   Update for moving datasets
 * Mar 31, 2017  6186     rjpeter   Refactored
 * May 09, 2017  6130     tjensen   Add version data to data sets support
 *                                  routing to ingest
 *
 * </pre>
 *
 * @author dhladky
 */
public class PDAMetaDataParser extends MetaDataParser<BriefRecordType> {

    private static final String FILL_VALUE = "fillValue";

    private static final String MISSING_VALUE = "missingValue";

    private static final String UNITS = "units";

    private static final String FORMAT = "format";

    /** DEBUG PDA system **/
    private static final String DEBUG = "DEBUG";

    /** debug state */
    protected final boolean debug;

    private final PDAMetaDataUtil metadataUtil = PDAMetaDataUtil.getInstance();

    public PDAMetaDataParser() {
        serviceConfig = HarvesterServiceManager.getInstance()
                .getServiceConfig(ServiceType.PDA);
        // debugging MetaData parsing.
        debug = Boolean.valueOf(serviceConfig.getConstantValue(DEBUG));
    }

    /**
     * Non impl in PDA
     */
    @Override
    public List<DataSetMetaData<?, ?>> parseMetaData(Provider provider,
            BriefRecordType record, Collection collection,
            String dataDateFormat) {
        throw new UnsupportedOperationException(
                "Not implemented for this type");
    }

    @Override
    public void parseMetaData(Provider provider, BriefRecordType record,
            boolean isMetaData) {
        if (record == null) {
            logger.error("BriefRecord is null, skipping entry");
            return;
        }

        Date arrivalTime = TimeUtil.newGmtCalendar().getTime();
        String metadataId = getSimpleLiteralValue(record.getIdentifier());
        String title = getSimpleLiteralValue(record.getTitle());
        BoundingBoxType boundingBox = null;

        List<JAXBElement<BoundingBoxType>> bbNodes = record.getBoundingBox();
        if (bbNodes != null && !bbNodes.isEmpty()) {
            JAXBElement<BoundingBoxType> jaxbNode = bbNodes.get(0);

            if (jaxbNode != null) {
                boundingBox = jaxbNode.getValue();
            }
        }

        /*
         * metadata URL is the full URL path, (not relative) to the file tack on
         * the root provider URL from the provider connection.
         */
        String metaDataURL = provider.getConnection().getUrl() + "/" + title;

        logger.info("metaData id: " + metadataId);
        logger.info("title: " + title);
        logger.info("metaDataURL: " + metaDataURL);

        if (metadataId == null) {
            logger.error(
                    "Required field Identifier of BriefRecord is null. Unable to parse dataset");
            return;
        }

        if (title == null) {
            logger.error(
                    "Required field title of BriefRecord is null. Unable to parse dataset");
            return;
        }

        try {
            PDAMetaDataExtractor extractor = PDAMetaDataExtractorFactory
                    .getExtractor(metadataId, title, boundingBox);
            Map<String, String> paramMap = extractor.extractMetaData(record);
            Time time = extractor.getTime();
            ImmutableDate idate = new ImmutableDate(time.getStart());

            String providerMetadataId = paramMap
                    .get(PDAMetaDataExtractor.METADATA_ID);
            String providerParam = paramMap
                    .get(PDAMetaDataExtractor.PARAM_NAME);
            String providerRes = paramMap.get(PDAMetaDataExtractor.RES_NAME);
            String providerSat = paramMap.get(PDAMetaDataExtractor.SAT_NAME);
            String awipsParam = metadataUtil.getParameterName(providerParam);
            String awipsRes = metadataUtil.getResName(providerRes);
            String awipsSat = metadataUtil.getSatName(providerSat);
            String dataSetName = createDataSetName(awipsParam, awipsRes,
                    awipsSat);
            String collectionName = awipsSat + " " + awipsRes;

            // check if there is a override to the metadataId
            if (providerMetadataId != null) {
                metadataId = providerMetadataId;
            }

            // Common regardless of extraction method
            if (checkIgnore(provider, providerParam, time)) {
                logger.info("Skipping DataSet '" + dataSetName + "'");
                return;
            }

            setDefaultParams(paramMap);
            String providerName = provider.getName();
            boolean isMoving = getIsMovingFromConfig(dataSetName, providerName);

            Coverage coverage = null;

            // there is only one parameter per briefRecord at this point
            Map<String, Parameter> parameters = getParameters(providerParam,
                    awipsParam, collectionName, provider.getName(),
                    paramMap.get(UNITS), paramMap.get(FILL_VALUE),
                    paramMap.get(MISSING_VALUE));

            try {
                logger.info("Preparing to store DataSet: " + dataSetName);
                PDADataSet pdaDataSet = new PDADataSet();
                pdaDataSet.setCollectionName(collectionName);
                pdaDataSet.setDataSetName(dataSetName);
                pdaDataSet.setProviderName(provider.getName());
                pdaDataSet.setDataSetType(DataType.PDA);
                pdaDataSet.setTime(time);
                pdaDataSet.setArrivalTime(arrivalTime.getTime());
                pdaDataSet.setParameters(parameters);

                // set the coverage
                coverage = extractor.getCoverage();
                pdaDataSet.setCoverage(coverage);

                // Store the parameter, data Set name, data Set
                for (Entry<String, Parameter> parm : parameters.entrySet()) {
                    storeParameter(parm.getValue());
                }

                pdaDataSet.setVersionData(
                        getVersionData(dataSetName, providerName));

                if (isMoving) {
                    try {
                        pdaDataSet.applyInfoFromConfig(isMoving,
                                getParentBoundsFromConfig(dataSetName,
                                        providerName),
                                getSizeEstFromConfig(dataSetName,
                                        providerName));
                    } catch (FactoryException | TransformException e) {
                        logger.error(
                                "Unable to get Parent Bounds for DataSet '"
                                        + dataSetName + "' from " + provider,
                                e);
                        return;
                    }
                }
                storeDataSet(pdaDataSet);
            } catch (Exception e) {
                logger.error("Error storing dataSet [" + dataSetName + "]", e);
            }

            /*
             * This portion of the code is only used when processing a
             * Transaction.
             */
            if (isMetaData) {
                try {
                    PDADataSetMetaData pdadsmd = new PDADataSetMetaData();
                    pdadsmd.setMetaDataID(metadataId);
                    pdadsmd.setArrivalTime(arrivalTime.getTime());
                    pdadsmd.setAvailabilityOffset(getDataSetAvailabilityTime(
                            collectionName, time.getStart().getTime()));
                    pdadsmd.setTime(time);
                    pdadsmd.setDataSetName(dataSetName);
                    pdadsmd.setDataSetDescription(StringUtil.join(Arrays.asList(
                            metadataId, awipsSat, awipsRes, awipsParam), ' '));
                    pdadsmd.setDate(idate);
                    pdadsmd.setProviderName(provider.getName());

                    /*
                     * If this is a moving dataset, save the coverage for this
                     * specific instance of the product in the metadata instead
                     * of on the dataset itself. Coverage on the dataset will
                     * store the 'parent coverage' (the outer bounds of where
                     * this product can be positioned).
                     */
                    if (isMoving) {
                        pdadsmd.setInstanceCoverage(coverage);
                    }
                    // In PDA's case it's actually a file name
                    pdadsmd.setUrl(metaDataURL);

                    storeMetaData(pdadsmd);
                } catch (Exception e) {
                    logger.error("Error storing dataSetMetaData [" + dataSetName
                            + "]", e);
                }
            }
        } catch (Exception e) {
            logger.error("Unable to parse BriefRecord [" + metadataId + "]", e);
        }
    }

    /**
     * Returns the dataSet name.
     *
     * @param parameter
     * @param res
     * @param sat
     * @return
     */
    protected String createDataSetName(String parameter, String res,
            String sat) {
        String dataSetName = parameter + " " + res + " " + sat;

        return dataSetName;
    }

    /**
     * Sets default parameters from ServiceConfig into paramMap.
     *
     * @param paramMap
     */
    private void setDefaultParams(Map<String, String> paramMap) {
        // these aren't satisfied with any extractor, use defaults
        paramMap.put(FILL_VALUE, serviceConfig.getConstantValue("FILL_VALUE"));
        paramMap.put(MISSING_VALUE,
                serviceConfig.getConstantValue("MISSING_VALUE"));
        paramMap.put(UNITS, serviceConfig.getConstantValue("UNITS"));
        paramMap.put(FORMAT, serviceConfig.getConstantValue("DEFAULT_FORMAT"));
    }

    /**
     * Check if the data should be skipped.
     *
     * @param provider
     * @param param
     * @param time
     * @return
     */
    protected boolean checkIgnore(Provider provider, String param, Time time) {
        boolean rval = checkExcludeList(param);

        if (!rval && Boolean.parseBoolean(
                serviceConfig.getConstantValue("CHECK_DATA_RETENTION_TIME"))) {
            rval = checkRetention(provider, time);
        }

        return rval;
    }

    /**
     * Check if the parameter is excluded.
     *
     * @param providerName
     * @return
     */
    protected boolean checkExcludeList(String providerName) {
        boolean rval = false;

        if (PDAMetaDataUtil.getInstance().isExcluded(providerName)) {
            logger.warn("Excluding metadata due to '" + providerName
                    + "' being on the exclusion list.");
            rval = true;
        }

        return rval;
    }

    /**
     * Check if product is beyond retention for the provider.
     *
     * @param provider
     * @param time
     * @return
     */
    protected boolean checkRetention(Provider provider, Time time) {
        long threshold = metadataUtil.getRetentionThreshold(provider.getName());
        boolean oldDate = false;

        if (threshold > 0) {
            long sTime = time.getStart().getTime();
            if (threshold >= sTime) {
                logger.warn("Excluding metadata due to time of " + sTime
                        + " being older than retention time of " + threshold);
                oldDate = true;
            }
        }

        return oldDate;

    }

    /**
     * Get parameters map for product.
     *
     * @param providerName
     * @param awipsName
     * @param collectionName
     * @param provider
     * @param units
     * @param fillValue
     * @param missingValue
     * @return
     */
    private Map<String, Parameter> getParameters(String providerName,
            String awipsName, String collectionName, String provider,
            String units, String fillValue, String missingValue) {

        Map<String, Parameter> params = new HashMap<>(1);

        Parameter parm = new Parameter();
        parm = new Parameter();
        parm.setName(awipsName);
        parm.setProviderName(provider);
        parm.setDefinition(provider + "-" + collectionName + "-" + awipsName);
        parm.setBaseType(serviceConfig.getConstantValue("BASE_TYPE"));

        parm.setFillValue(fillValue);
        parm.setMissingValue(missingValue);
        parm.setUnits(units);

        /*
         * The standard default for satellite data is EA (Entire Atmosphere).
         */
        LevelType type = LevelType.EA;
        DataLevelType dlt = new DataLevelType(type);
        ArrayList<DataLevelType> types = new ArrayList<>(1);
        types.add(dlt);
        // set the level types
        parm.setLevelType(types);
        // Set the data type
        parm.setDataType(DataType.PDA);
        params.put(parm.getName(), parm);

        return params;
    }

    /**
     * Returns the content of the simpleLiteral of the first node or null if
     * unable to walk data structure.
     *
     * @param node
     * @return
     */
    private String getSimpleLiteralValue(
            List<JAXBElement<SimpleLiteral>> node) {
        if (node == null || node.isEmpty()) {
            return null;
        }

        JAXBElement<SimpleLiteral> jaxbElement = node.get(0);
        if (jaxbElement == null) {
            return null;
        }

        SimpleLiteral elementVal = jaxbElement.getValue();
        if (elementVal == null) {
            return null;
        }
        List<String> content = elementVal.getContent();
        if (content == null || content.isEmpty()) {
            return null;
        }

        return content.get(0);
    }

}
