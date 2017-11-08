package com.raytheon.uf.edex.datadelivery.retrieval.wfs.metadata;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.PointTime;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.madis.MadisRecord;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.adapters.AbstractMetadataAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.wfs.IWfsMetaDataAdapter;
import com.raytheon.uf.edex.ogc.common.jaxb.OgcJaxbManager;
import com.raytheon.uf.edex.plugin.madis.MadisPointDataTransform;
import com.raytheon.uf.edex.plugin.madis.ogc.feature.Madis;
import com.raytheon.uf.edex.plugin.madis.ogc.feature.MadisObjectFactory;
import com.raytheon.uf.edex.wfs.reg.Unique;

import net.opengis.gml.v_3_1_1.AbstractFeatureType;
import net.opengis.ows.v_1_0_0.ExceptionReport;
import net.opengis.wfs.v_1_1_0.FeatureCollectionType;

/**
 *
 * Convert RetrievalAttribute to MadisRecords.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------
 * May 12, 2013  753      dhladky   Initial javadoc
 * May 31, 2013  2038     djohnson  Move to correct git repo.
 * Jun 03, 2012  1763     dhladky   Made operational, moved to ogc plugin
 * Jul 14, 2014  3373     bclement  jaxb manager api changes
 * Jul 24, 2014  3441     dhladky   Fixed what the previous update broke
 * Jul 25, 2017  6186     rjpeter   Update signature
 *
 * </pre>
 *
 * @author dhladky
 */

public class MadisMetadataAdapter
        extends AbstractMetadataAdapter<Madis, PointTime, Coverage>
        implements IWfsMetaDataAdapter {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(MadisMetadataAdapter.class);

    private OgcJaxbManager marshaller = null;

    @Override
    public PluginDataObject getRecord(Madis madis) {
        return madis.getRecord();
    }

    @Override
    public PluginDataObject translateFeature(AbstractFeatureType feature) {
        // For WFS features this is where the translation takes place.
        if (feature instanceof Madis) {
            return getRecord((Madis) feature);
        } else {
            throw new IllegalArgumentException("Feature is not of type Madis!");
        }
    }

    @Override
    public void allocatePdoArray(int size) {
        pdos = new MadisRecord[size];
    }

    @Override
    public void processRetrieval(Retrieval<PointTime, Coverage> retrieval)
            throws InstantiationException {
    }

    public void configureMarshaller() throws JAXBException {
        if (marshaller == null) {
            Class<?>[] classes = new Class<?>[] {
                    net.opengis.gml.v_3_1_1.ObjectFactory.class,
                    net.opengis.wfs.v_1_1_0.ObjectFactory.class,
                    net.opengis.filter.v_1_1_0.ObjectFactory.class,
                    Unique.class, Madis.class, MadisObjectFactory.class };

            try {
                marshaller = new OgcJaxbManager(classes);
            } catch (JAXBException e1) {
                statusHandler.handle(Priority.PROBLEM, e1.getLocalizedMessage(),
                        e1);
            }
        }
    }

    public OgcJaxbManager getMarshaller() {
        return marshaller;
    }

    @Override
    public Object getData(String payload) throws JAXBException {
        // Madis is pointData
        setIsPointData(true);

        if (marshaller == null) {
            configureMarshaller();
        }

        try {
            FeatureCollectionType collection = (FeatureCollectionType) getMarshaller()
                    .unmarshalFromXml(payload);
            return collection;
        } catch (Exception e) {
            // try as an exception report
            try {
                ExceptionReport exception = (ExceptionReport) getMarshaller()
                        .unmarshalFromXml(payload);
                return exception;
            } catch (JAXBException e2) {
                // Just return the non-jaxed XML
                return payload;
            }
        }
    }

    /**
     * populate pointdata views
     *
     * @param pdos
     * @return
     * @throws PluginException
     */
    @Override
    public PluginDataObject[] setPointData(PluginDataObject[] pdos)
            throws PluginException {

        MadisPointDataTransform mpdt = new MadisPointDataTransform();
        mpdt.toPointData(pdos);
        return pdos;
    }

}
