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
package com.raytheon.uf.common.datadelivery.bandwidth.datasetlatency;

import com.raytheon.uf.common.auth.req.AbstractPrivilegedRequest;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Request object for client initiated Data Set Latency operations.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 01, 2014   3550     ccody       Initial creation.
 * 
 * </pre>
 * 
 * @author ccody
 * @version 1.0
 */
@DynamicSerialize
public class DataSetLatencyRequest extends AbstractPrivilegedRequest {

    public static enum Type {
        DELETE;
    }

    /** Operation Type */
    @DynamicSerializeElement
    private Type type;

    /** Data Set Name */
    @DynamicSerializeElement
    private String dataSetName;

    /** Data Set Provider Name */
    @DynamicSerializeElement
    private String providerName;

    /**
     * Constructor.
     */
    public DataSetLatencyRequest() {
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @return the dataSetName
     */
    public String getDataSetName() {
        return dataSetName;
    }

    /**
     * @param dataSetName
     *            the dataSetName to set
     */
    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    /**
     * @return the providerName
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * @param providerName
     *            the providerName to set
     */
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

}
