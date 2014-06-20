package com.raytheon.uf.common.datadelivery.registry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.registry.annotations.RegistryObjectVersion;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

/**
 * PDA data sets
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 14, 2014 3120        dhladky     Initial creation
 * 
 * </pre>
 * 
 * @version 1.0
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@RegistryObjectVersion(value = 1.0f)
@DynamicSerialize
public class PDADataSet extends DataSet<Time, Coverage> {

    public PDADataSet() {
        this.dataSetType = DataType.PDA;
    }

    @Override
    public ServiceType getServiceType() {
        return ServiceType.FTPS;
    }

}
