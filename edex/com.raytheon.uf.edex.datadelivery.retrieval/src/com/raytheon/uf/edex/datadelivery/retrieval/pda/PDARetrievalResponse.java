package com.raytheon.uf.edex.datadelivery.retrieval.pda;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.edex.datadelivery.retrieval.response.RetrievalResponse;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ResponseProcessingUtilities;

/**
 * {@link ServiceFactory} implementation for PDA.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Jun 13, 2014  3120     dhladky   Initial creation
 * Sep 14, 2104  3121     dhladky   Serialization adjustments
 * May 03, 2016  5599     tjensen   Added subscription name.
 * Jul 27, 2017  6186     rjpeter   Don't use payload
 *
 * </pre>
 *
 * @author dhladky
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class PDARetrievalResponse extends RetrievalResponse {
    @XmlElement
    private byte[] fileBytes;

    @XmlElement
    private String fileName;

    // public constructor
    public PDARetrievalResponse() {

    }

    public enum FILE {
        FILE_BYTES, FILE_NAME, SUBSCRIPTION_NAME;
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

    @Override
    public void prepareForSerialization() throws Exception {
        // add file bytes to map
        setFileBytes(ResponseProcessingUtilities.getCompressedFile(fileName));
    }
}
