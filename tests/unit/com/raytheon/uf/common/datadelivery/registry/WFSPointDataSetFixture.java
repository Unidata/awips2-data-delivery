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

import java.util.Collections;
import java.util.Random;

import com.raytheon.uf.common.util.AbstractFixture;

/**
 * {@link AbstractFixture} implementation for {@link WFSPointDataSet} objects.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 05, 2013 2038      djohnson     Initial creation
 * Aug 26, 2014   3365     ccody       Separate Data Delivery tests out of AWIPS 2 baseline.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class WFSPointDataSetFixture extends AbstractFixture<WFSPointDataSet> {

    public static final WFSPointDataSetFixture INSTANCE = new WFSPointDataSetFixture();

    /**
     * Disabled constructor.
     */
    private WFSPointDataSetFixture() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WFSPointDataSet getInstance(long seedValue, Random random) {
        WFSPointDataSet obj = new WFSPointDataSet();
        obj.setCollectionName("collectionName-" + seedValue);
        obj.setDataSetName("dataSetName" + seedValue);
        obj.setDataSetType(DataType.POINT);
        obj.setTime(PointTimeFixture.INSTANCE.get(seedValue));
        obj.setParameters(Collections.<String, Parameter> emptyMap());
        obj.setProviderName(ProviderFixture.INSTANCE.get(seedValue).getName());

        return obj;
    }

}
