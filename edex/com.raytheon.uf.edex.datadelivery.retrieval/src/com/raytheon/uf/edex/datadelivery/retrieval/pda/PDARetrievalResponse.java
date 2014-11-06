package com.raytheon.uf.edex.datadelivery.retrieval.pda;

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


import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalResponse;

/**
 * {@link ServiceFactory} implementation for PDA.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 13, 2014 3120       dhladky     Initial creation
 * Sept 14, 2104 3121      dhladky     Serialization adjustments
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

@DynamicSerialize
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class PDARetrievalResponse extends RetrievalResponse<Time, Coverage> {

    // we do not serialize this directly
    private Map<FILE, Object> payload;
    
    @XmlElement
    @DynamicSerializeElement
    private byte[] fileBytes;
    
    @XmlElement
    @DynamicSerializeElement
    private String fileName;
    
    // public constructor
    public PDARetrievalResponse() {
        
    }
    
    // useful constructor
    public PDARetrievalResponse(RetrievalAttribute<Time, Coverage> attribute) {
        super(attribute);
    }

    public enum FILE {
        FILE_BYTES, FILE_NAME;
    }

    @Override
    public void setPayLoad(Object payLoad) {
        
        this.payload = (Map<FILE,Object>)(payload);
        // we only serialize these things
        setFileName((String)payload.get(FILE.FILE_NAME));
        setFileBytes((byte[])payload.get(FILE.FILE_BYTES));
    }

    @Override
    public Map<FILE, Object> getPayLoad() {
        
        if (payload == null) {
            payload = new HashMap<FILE, Object>(2);
            payload.put(FILE.FILE_NAME, getFileName());
            payload.put(FILE.FILE_BYTES, getFileBytes());
        }
        
        return payload;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public void setFileBytes(byte[] fileBytes) {
        this.fileBytes = fileBytes;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

}
