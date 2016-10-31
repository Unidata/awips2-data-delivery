package com.raytheon.uf.edex.datadelivery.retrieval.util;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * A JAXBable set of {@link PDADataSetNameMap}s.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 11, 2016 5752       tjensen     Initial creation
 * 
 * </pre>
 * 
 * @author tjensen
 * @version 1.0
 */
@XmlRootElement(name = "pdaDataSetNameMapSet")
@XmlAccessorType(XmlAccessType.NONE)
public class PDADataSetNameMapSet {

    /**
     * List of DataSet Mappings from the XML.
     */
    @XmlElements({ @XmlElement(name = "pdaDataSetNameMap", type = PDADataSetNameMap.class) })
    private ArrayList<PDADataSetNameMap> maps;

    public ArrayList<PDADataSetNameMap> getMaps() {
        return maps;
    }

    public void setMaps(ArrayList<PDADataSetNameMap> maps) {
        this.maps = maps;
    }

    public void addMaps(Collection<PDADataSetNameMap> mapsToAdd) {
        if (this.maps == null) {
            this.maps = new ArrayList<>();
        }
        this.maps.addAll(mapsToAdd);
    }
}
