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
package com.raytheon.uf.edex.datadelivery.retrieval.opendap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeTypeAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalResponse;

/**
 * {@link RetrievalResponse} for OpenDAP.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 12, 2013 1543       djohnson     Initial creation
 * Feb 15, 2013 1543       djohnson     Only allow DataDDS payloads.
 * Apr 14, 2015 4400       dhladky      Updated to DAP2 protocol made backward compatible.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */
@DynamicSerializeTypeAdapter(factory = OpenDapRetrievalResponseSerializer.class)
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class OpenDapRetrievalResponse extends RetrievalResponse<GriddedTime, GriddedCoverage> {

    @XmlElement
    @XmlJavaTypeAdapter(value = OpenDapRetrievalResponseSerializer.class)
    private Object payload;

    /**
     * Constructor.
     */
    public OpenDapRetrievalResponse() {

    }

    /**
     * Constructor.
     * 
     * @param attribute
     */
    public OpenDapRetrievalResponse(RetrievalAttribute<GriddedTime, GriddedCoverage> attribute) {
        super(attribute);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPayLoad(Object payload) {
        
        if (payload != null) {
            if (payload instanceof opendap.dap.DataDDS) {
                this.payload = opendap.dap.DataDDS.class.cast(payload);
            } else if (payload instanceof dods.dap.DataDDS) {
                this.payload = dods.dap.DataDDS.class.cast(payload);
            } else {
                throw new IllegalArgumentException(
                        "Payload must be a DataDDS instance, not "
                                + payload.getClass().getName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getPayLoad() {
        return payload;
    }
}
