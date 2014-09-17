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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import com.raytheon.uf.common.util.AbstractFixture;
import com.raytheon.uf.common.util.CollectionUtil;

/**
 * {@link AbstractFixture} implementation for {@link OpenDapGriddedDataSet}
 * objects.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 07, 2012 1104      djohnson     Initial creation
 * Sept 30, 2013 1797     dhladky      Generics
 * Aug 26, 2014   3365     ccody       Separate Data Delivery tests out of AWIPS 2 baseline.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class OpenDapGriddedDataSetFixture extends
        AbstractFixture<OpenDapGriddedDataSet> {

    public static final OpenDapGriddedDataSetFixture INSTANCE = new OpenDapGriddedDataSetFixture();

    /**
     * Disabled constructor.
     */
    private OpenDapGriddedDataSetFixture() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenDapGriddedDataSet getInstance(long seedValue, Random random) {
        OpenDapGriddedDataSet obj = new OpenDapGriddedDataSet();
        obj.setCollectionName("collectionName-" + seedValue);
        // TODO: CoverageFixture
        // obj.setCoverage(CoverageFixture.INSTANCE.get(seedValue));
        obj.setCycles(new TreeSet<Integer>(Arrays.asList(GriddedTimeFixture.getCycleForSeed(seedValue))));
        Map<Integer, String> cyclesToUrls = new HashMap<Integer, String>();
        for (Integer cycle : obj.getCycles()) {
            cyclesToUrls.put(cycle, "http://someurl");
            obj.cycleUpdated(cycle);
        }
        obj.setCyclesToUrls(cyclesToUrls);
        obj.setDataSetName("dataSetName" + seedValue);
        obj.setDataSetType(DataType.GRID);
        obj.setForecastHours(CollectionUtil.asSet(0));
        obj.setTime(GriddedTimeFixture.INSTANCE.get(seedValue));
        // TODO: ParameterFixture
        obj.setParameters(Collections.<String, Parameter> emptyMap());
        obj.setProviderName(ProviderFixture.INSTANCE.get(seedValue).getName());

        return obj;
    }

}
