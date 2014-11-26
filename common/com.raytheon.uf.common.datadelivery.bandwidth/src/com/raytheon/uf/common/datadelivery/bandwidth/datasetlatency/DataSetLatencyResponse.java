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

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Response object for the Data Set Latency messaging.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Dec 01, 2014  3550       ccody       Initial creation
 * 
 * </pre>
 * 
 * @author ccody
 * @version 1.0
 */
@DynamicSerialize
public class DataSetLatencyResponse implements ISerializableObject {

    @DynamicSerializeElement
    private boolean successful;

    @DynamicSerializeElement
    private String messageString;

    /**
     * Constructor.
     */
    public DataSetLatencyResponse() {
        successful = true;
    }

    /**
     * @param successful
     *            the messageString to set
     */
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    /**
     * @return the successful
     */
    public boolean getSuccessful() {
        return successful;
    }

    /**
     * @param messageString
     *            the messageString to set
     */
    public void setMessageString(String messageString) {
        this.messageString = messageString;
    }

    /**
     * @return the messageString
     */
    public String getMessageString() {
        return messageString;
    }

}
