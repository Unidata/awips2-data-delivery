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

package com.raytheon.uf.common.datadelivery.retrieval.xml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Parameter Lookup XML Object.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------------
 * Mar 01, 2012           jpiatt    Initial creation.
 * Oct 20, 2012  1163     dhladky   speed it up
 * Nov 07, 2013  2361     njensen   Remove ISerializableObject
 * Jan 05, 2017  5988     tjensen   Updated for new parameter lookups
 * 
 * </pre>
 * 
 * @author jpiatt
 * @version 1.0
 */

@XmlRootElement(name = "ParameterLookup")
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class ParameterLookup {

    @XmlElements({
            @XmlElement(name = "parameterMapping", type = ParameterMapping.class) })
    @DynamicSerializeElement
    private List<ParameterMapping> parameterMappings;

    /**
     * Map of the entries by provider name
     */
    private Map<String, ParameterMapping> providerParameters = null;

    /**
     * Map of the entries by awips name
     */
    private Map<String, ParameterMapping> awipsParameters = null;

    /**
     * Creates the awips map for speed
     */
    private void createAwipsMap() {
        if (awipsParameters == null) {
            awipsParameters = new HashMap<>();
            List<ParameterMapping> configs = getParameterMappings();
            if (configs != null) {
                for (ParameterMapping parm : configs) {
                    awipsParameters.put(parm.getAwips(), parm);
                }
            }
        }
    }

    /**
     * Creates the provider map for speed
     */
    private void createProviderMap() {
        if (providerParameters == null) {
            providerParameters = new HashMap<>();
            List<ParameterMapping> configs = getParameterMappings();
            if (configs != null) {
                for (ParameterMapping parm : configs) {
                    providerParameters.put(parm.getGrads(), parm);
                }
            }
        }
    }

    /**
     * Gets the parameter by the AWIPS name
     * 
     * @param awipsName
     * @return
     */
    public ParameterMapping getParameterByAwipsName(String awipsName) {
        if (awipsParameters == null) {
            createAwipsMap();
        }

        return awipsParameters.get(awipsName);
    }

    /**
     * Gets the parameter by the provider name
     * 
     * @param awipsName
     * @return
     */
    public ParameterMapping getParameterByProviderName(String providerName) {

        if (providerParameters == null) {
            createProviderMap();
        }

        return providerParameters.get(providerName);
    }

    public List<ParameterMapping> getParameterMappings() {
        return parameterMappings;
    }

    public void setParameterMappings(List<ParameterMapping> parameterMappings) {
        this.parameterMappings = parameterMappings;
    }

}
