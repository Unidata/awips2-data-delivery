package com.raytheon.uf.common.datadelivery.registry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.registry.annotations.RegistryObject;
import com.raytheon.uf.common.registry.annotations.RegistryObjectDescription;
import com.raytheon.uf.common.registry.annotations.RegistryObjectName;
import com.raytheon.uf.common.registry.annotations.RegistryObjectOwner;
import com.raytheon.uf.common.registry.annotations.RegistryObjectVersion;
import com.raytheon.uf.common.registry.annotations.SlotAttribute;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Abstract Meta Data object
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 20, 2011  218      dhladky   Initial creation
 * May 15, 2012  455      jspinks   Added Registry annotations
 * Aug 03, 2012  724      bphillip  Added more registry annotations
 * Aug 22, 2012  743      djohnson  Store data type as an enum.
 * Sep 07, 2012  1102     djohnson  Add {@code @XmlRootElement}.
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Jan 23, 2013  2584     dhladky   Versions.
 * Sep 12, 2017  6413     tjensen   Removed unused Parameters; Deprecated
 *
 * </pre>
 *
 * @author dhladky
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
@RegistryObject(value = { "providerName", "dataSetType",
        "dataSetName" }, storeContent = false)
@RegistryObjectVersion(value = 1.0f)
@Deprecated
public class DataSetName {
    /*
     * This is no longer used in 18.1.1 and beyond. Still generated for
     * compatibility with older versions. Should be removed once all sites at
     * 18.1.1 or beyond.
     */

    @RegistryObjectOwner
    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute
    protected String providerName;

    @RegistryObjectDescription
    @RegistryObjectName
    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute
    protected String dataSetName;

    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute
    protected DataType dataSetType;

    public DataSetName() {

    }

    public String getDataSetName() {
        return dataSetName;
    }

    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    public DataType getDataSetType() {
        return dataSetType;
    }

    public void setDataSetType(DataType dataSetType) {
        this.dataSetType = dataSetType;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

}
