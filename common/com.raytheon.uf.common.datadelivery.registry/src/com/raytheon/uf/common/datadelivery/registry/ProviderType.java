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
package com.raytheon.uf.common.datadelivery.registry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.registry.annotations.RegistryObjectVersion;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Provider configuration for a supported data type.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 07, 2013 2038       djohnson     Initial creation
 * Mar 26, 2014 2789       dhladky      Versioned.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
@RegistryObjectVersion(value = 1.0f)
public class ProviderType {

    @XmlAttribute
    @DynamicSerializeElement
    private DataType dataType;

    @XmlAttribute
    @DynamicSerializeElement
    private String plugin;

    @XmlAttribute
    @DynamicSerializeElement
    private int availabilityDelay;

    /**
     * Constructor.
     */
    public ProviderType() {
    }

    /**
     * Convenience constructor.
     * 
     * @param dataType
     * @param plugin
     * @param availabilityDelay
     */
    public ProviderType(DataType dataType, String plugin, int availabilityDelay) {
        this.dataType = dataType;
        this.plugin = plugin;
        this.availabilityDelay = availabilityDelay;
    }

    /**
     * @return the dataType
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * @param dataType
     *            the dataType to set
     */
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    /**
     * @return the plugin
     */
    public String getPlugin() {
        return plugin;
    }

    /**
     * @param plugin
     *            the plugin to set
     */
    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    /**
     * @return the availabilityDelay
     */
    public int getAvailabilityDelay() {
        return availabilityDelay;
    }

    /**
     * @param availabilityDelay
     *            the availabilityDelay to set
     */
    public void setAvailabilityDelay(int availabilityDelay) {
        this.availabilityDelay = availabilityDelay;
    }

}
