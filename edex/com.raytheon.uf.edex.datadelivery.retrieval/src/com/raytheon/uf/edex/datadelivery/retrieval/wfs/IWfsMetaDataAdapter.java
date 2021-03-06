package com.raytheon.uf.edex.datadelivery.retrieval.wfs;

import javax.xml.bind.JAXBException;

import net.opengis.gml.v_3_1_1.AbstractFeatureType;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;

/**
 * 
 * Interface for creating WFS derived objects
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 22, 2013  753          dhladky    Initial javadoc
 * June 11, 2013 1763        dhladky     Moved and updated.
 * July 24, 2014 3441        dhladky     Made interface less rigid
 * Sept 22, 2014 3121        dhladky     Adjusting generics.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public interface IWfsMetaDataAdapter {

    public Object getData(String payload) throws JAXBException;
    
    public PluginDataObject[] setPointData(PluginDataObject[] pdos) throws PluginException;

    public PluginDataObject translateFeature(AbstractFeatureType feature);

}
