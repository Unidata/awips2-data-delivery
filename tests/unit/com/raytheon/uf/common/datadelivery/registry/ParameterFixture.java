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

import java.util.Arrays;
import java.util.Random;

import com.raytheon.uf.common.util.AbstractFixture;

/**
 * {@link AbstractFixture} implementation for {@link Parameter} objects.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 07, 2012 1104       djohnson     Initial creation
 * Feb 07, 2013 1543       djohnson     Missing value must be a numeric value.
 * Feb 13, 2014 2386       bgonzale     Added provider name and units.
 * Aug 26, 2014   3365     ccody       Separate Data Delivery tests out of AWIPS 2 baseline.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class ParameterFixture extends AbstractFixture<Parameter> {

    public static final ParameterFixture INSTANCE = new ParameterFixture();

    /**
     * Disabled constructor.
     */
    private ParameterFixture() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter getInstance(long seedValue, Random random) {
        Parameter obj = new Parameter();
        obj.setBaseType("baseType" + seedValue);
        obj.setDataType(DataType.GRID);
        obj.setDefinition("definition" + seedValue);
        obj.setFillValue("fillValue" + seedValue);
        obj.setLevels(LevelsFixture.INSTANCE.get(seedValue));
        obj.setLevelType(Arrays.asList(DataLevelTypeFixture.INSTANCE
                .get(seedValue)));
        obj.setMissingValue("" + seedValue);
        obj.setName("name" + seedValue);
        obj.setProviderName("ProviderName" + seedValue);
        obj.setUnits("Units" + seedValue);
        return obj;
    }

}
