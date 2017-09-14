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

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 *
 * Information for a specific level of a provider parameter
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 14, 2017 6413       tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class GriddedParameterLevelEntry extends ParameterLevelEntry {

    /**
     * Value to use for missing data.
     */
    @XmlAttribute
    @DynamicSerializeElement
    private String missingValue;

    /**
     * Flag for if this is a level known to the provider. If it is, this means
     * we need to send level information along with our request.
     */
    @XmlAttribute
    @DynamicSerializeElement
    private boolean useProviderLevel;

    public GriddedParameterLevelEntry() {

    }

    public GriddedParameterLevelEntry(String providerName, String description,
            String levelOne) {
        super(providerName, description, levelOne, null);
    }

    public GriddedParameterLevelEntry(String providerName, String description,
            String levelOne, String levelTwo) {
        super(providerName, description, levelOne, levelTwo);
    }

    public GriddedParameterLevelEntry(GriddedParameterLevelEntry other) {
        super(other);
        this.missingValue = other.getMissingValue();
    }

    public String getMissingValue() {
        return missingValue;
    }

    public void setMissingValue(String missingValue) {
        this.missingValue = missingValue;
    }

    public boolean isUseProviderLevel() {
        return useProviderLevel;
    }

    public void setUseProviderLevel(boolean useProviderLevel) {
        this.useProviderLevel = useProviderLevel;
    }

}
