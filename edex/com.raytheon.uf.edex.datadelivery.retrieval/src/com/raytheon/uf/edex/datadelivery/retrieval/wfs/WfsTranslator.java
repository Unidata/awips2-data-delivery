package com.raytheon.uf.edex.datadelivery.retrieval.wfs;

import java.util.List;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.PointTime;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalTranslator;

import net.opengis.gml.v_3_1_1.AbstractFeatureType;
import net.opengis.gml.v_3_1_1.FeaturePropertyType;
import net.opengis.ows.v_1_0_0.ExceptionReport;
import net.opengis.ows.v_1_0_0.ExceptionType;
import net.opengis.wfs.v_1_1_0.FeatureCollectionType;

/**
 *
 * Translate WFS retrievals into PDOs
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------------
 * May 12, 2013  753      dhladky   Initial javadoc
 * May 31, 2013  2038     djohnson  Move to correct repo.
 * May 31, 2013  1763     dhladky   Fixed up merge, made operational.
 * Jul 24, 2014  3441     dhladky   Sharpen error handling for WFS
 * Sep 22, 2014  3121     dhladky   Generic clean up.
 * Jul 25, 2017  6186     rjpeter   Update signature
 * Nov 15, 2017  6498     tjensen   Use inherited logger for logging.
 *
 * </pre>
 *
 * @author dhladky
 */

public class WfsTranslator
        extends RetrievalTranslator<PointTime, Coverage, AbstractFeatureType> {

    public WfsTranslator(Retrieval<PointTime, Coverage> retrieval)
            throws InstantiationException {
        super(retrieval);
    }

    @Override
    protected int getSubsetNumTimes() {
        // unimplemented by WFS
        return 0;
    }

    @Override
    protected int getSubsetNumLevels() {
        // unimplemented by WFS
        return 0;
    }

    @Override
    protected List<DataTime> getTimes() {
        // unimplemented by WFS
        return null;
    }

    /**
     * XML string into abstract features then to PDOs
     *
     * @param payload
     * @return
     */

    public PluginDataObject[] asPluginDataObjects(String payload) {

        try {
            IWfsMetaDataAdapter wfsAdapter = (IWfsMetaDataAdapter) metadataAdapter;
            Object o = wfsAdapter.getData(payload);

            if (o instanceof FeatureCollectionType) {
                FeatureCollectionType featureCollection = (FeatureCollectionType) o;

                if (featureCollection.getNumberOfFeatures().intValue() > 0) {
                    int i = 0;
                    metadataAdapter.allocatePdoArray(
                            featureCollection.getNumberOfFeatures().intValue());
                    for (FeaturePropertyType type : featureCollection
                            .getFeatureMember()) {
                        AbstractFeatureType feature = type.getFeature()
                                .getValue();
                        metadataAdapter.getPdos()[i] = wfsAdapter
                                .translateFeature(feature);
                        i++;
                    }
                }

                if (metadataAdapter.isPointData()) {
                    wfsAdapter.setPointData(metadataAdapter.getPdos());
                }
            } else {
                if (o instanceof ExceptionReport) {
                    ExceptionReport exception = (ExceptionReport) o;
                    for (ExceptionType type : exception.getException()) {
                        logger.error("Remote Exception returned: \n"
                                + type.getExceptionText().toString());
                    }
                } else if (o instanceof String) {
                    // failed utterly, but with a raw string explanation.
                    logger.error("Raw Remote Exception: \n" + o.toString());
                } else {
                    throw new IllegalStateException(
                            "Received an invalid WFS return object. "
                                    + o.toString());
                }
            }

        } catch (Exception e) {
            logger.error("Can't create plugins.", e);
        }

        return metadataAdapter.getPdos();
    }

}
