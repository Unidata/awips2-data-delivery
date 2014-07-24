package com.raytheon.uf.edex.datadelivery.retrieval.wfs;

import java.util.List;

import net.opengis.gml.v_3_1_1.AbstractFeatureType;
import net.opengis.gml.v_3_1_1.FeaturePropertyType;
import net.opengis.ows.v_1_0_0.ExceptionReport;
import net.opengis.ows.v_1_0_0.ExceptionType;
import net.opengis.wfs.v_1_1_0.FeatureCollectionType;

import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalTranslator;

/**
 * 
 * Translate WFS retrievals into PDOs
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 12, 2013  753       dhladky      Initial javadoc
 * May 31, 2013 2038       djohnson     Move to correct repo.
 * May 31, 2013 1763       dhladky      Fixed up merge, made operational.
 * Jul 24, 2014 3441       dhladky      Sharpen error handling for WFS
 * 
 * </pre>
 * 
 * @author unknown
 * @version 1.0
 */

public class WfsTranslator extends RetrievalTranslator {

    private static final IUFStatusHandler statusHandler = UFStatus
    .getHandler(WfsTranslator.class);
    
    WfsTranslator(RetrievalAttribute attXML, String className)
            throws InstantiationException {
 
        super(attXML, className);
    }

    public WfsTranslator(RetrievalAttribute attXML) throws InstantiationException {
        super(attXML);
    }

    @Override
    protected int getSubsetNumTimes() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected int getSubsetNumLevels() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected List<DataTime> getTimes() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * XML string into abstract features then to PDOs
     * @param payload
     * @return
     */
    @SuppressWarnings("unchecked")
    public PluginDataObject[] asPluginDataObjects(String payload) {
        
        try {
            IWfsMetaDataAdapter wfsAdapter = (IWfsMetaDataAdapter) metadataAdapter;
            Object o = wfsAdapter.getData(payload);

            if (o instanceof FeatureCollectionType) {
                FeatureCollectionType featureCollection = (FeatureCollectionType) o;

                if (featureCollection.getNumberOfFeatures().intValue() > 0) {
                    int i = 0;
                    metadataAdapter.allocatePdoArray(featureCollection
                            .getNumberOfFeatures().intValue());
                    for (FeaturePropertyType type : featureCollection
                            .getFeatureMember()) {
                        AbstractFeatureType feature = type.getFeature()
                                .getValue();
                        metadataAdapter.getPdos()[i] = metadataAdapter
                                .getRecord(feature);
                        i++;
                    }
                }

                if (metadataAdapter.isPointData()) {
                    wfsAdapter.setPointData(metadataAdapter.getPdos());
                }
            } else {
                if (o instanceof ExceptionReport) {
                    ExceptionReport exception = (ExceptionReport) o;
                    if (exception != null) {
                        for (ExceptionType type : exception.getException()) {

                            statusHandler.handle(Priority.ERROR,
                                    "Remote Exception returned: \n"
                                            + type.getExceptionText()
                                                    .toString());
                        }
                    }
                } else if (o instanceof String) {
                    // failed utterly, but with a raw string explanation.
                    statusHandler.handle(Priority.ERROR,
                            "Raw Remote Exception: \n" + o.toString());
                } else {
                    throw new IllegalStateException(
                            "Recieved an invalid WFS return object. "+o.toString());
                }
            }

        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR, "Can't create plugins.", e);
        }
        
        return metadataAdapter.getPdos();
    }

}

