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

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.raytheon.uf.common.registry.annotations.RegistryObjectVersion;
import com.raytheon.uf.common.serialization.XmlGenericMapAdapter;
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
 * ------------- -------- --------- --------------------------------------------
 * Jun 14, 2014  3120     dhladky   Initial creation
 * Sep 17, 2014  3127     dhladky   Added for geographic subsetting.
 * Apr 05, 2017  1045     tjensen   Add Coverage generics DataSetMetaData
 * Aug 02, 2017  6186     rjpeter   Support dataSetMetaData being a partial
 *                                  dataSet.
 * Sep 12, 2017  6413     tjensen   Updated to support ParameterGroups
 * Nov 15, 2017  6498     tjensen   Remove unneeded parameters
 *
 * </pre>
 *
 * @author dhladky
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@RegistryObjectVersion(value = 1.0f)
@DynamicSerialize
public class PDADataSetMetaData extends DataSetMetaData<Time, Coverage> {

    @XmlAttribute
    @DynamicSerializeElement
    private String metaDataID;

    @DynamicSerializeElement
    @XmlJavaTypeAdapter(type = Map.class, value = XmlGenericMapAdapter.class)
    protected Map<String, ParameterGroup> parameterGroups = new HashMap<>();

    public PDADataSetMetaData() {

    }

    public String getMetaDataID() {
        return metaDataID;
    }

    public void setMetaDataID(String metaDataID) {
        this.metaDataID = metaDataID;
    }

    @Override
    public String satisfiesSubscription(Subscription<Time, Coverage> sub)
            throws Exception {
        String rval = super.satisfiesSubscription(sub);

        if (rval == null) {
            // determine if parameters intersect
            boolean intersect = ParameterUtils.intersects(parameterGroups,
                    sub.getParameterGroups());

            if (!intersect) {
                rval = "the subscription is not subscribed to parameters in this dataset metadata";
            }
        }

        return rval;
    }

    public Map<String, ParameterGroup> getParameterGroups() {
        return parameterGroups;
    }

    public void setParameterGroups(
            Map<String, ParameterGroup> parameterGroups) {
        this.parameterGroups = parameterGroups;
    }

}
