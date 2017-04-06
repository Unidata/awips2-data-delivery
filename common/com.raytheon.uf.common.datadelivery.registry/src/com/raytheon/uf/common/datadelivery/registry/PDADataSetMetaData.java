package com.raytheon.uf.common.datadelivery.registry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.registry.annotations.RegistryObjectVersion;
import com.raytheon.uf.common.registry.annotations.SlotAttribute;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * PDA data set metadata
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------
 * Jun 14, 2014  3120     dhladky   Initial creation
 * Sep 17, 2014  3127     dhladky   Added for geographic subsetting.
 * Apr 05, 2017  1045     tjensen   Add Coverage generics DataSetMetaData
 * 
 * </pre>
 * 
 * @version 1.0
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@RegistryObjectVersion(value = 1.0f)
@DynamicSerialize
public class PDADataSetMetaData extends DataSetMetaData<Time, Coverage> {

    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute
    private String metaDataID;

    public PDADataSetMetaData() {

    }

    public String getMetaDataID() {
        return metaDataID;
    }

    public void setMetaDataID(String metaDataID) {
        this.metaDataID = metaDataID;
    }

}
