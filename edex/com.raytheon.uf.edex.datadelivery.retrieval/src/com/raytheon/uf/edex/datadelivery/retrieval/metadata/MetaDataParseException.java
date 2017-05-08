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
package com.raytheon.uf.edex.datadelivery.retrieval.metadata;

/**
 * Exception during parsing of metadata.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Mar 28, 2017  6186     rjpeter   Initial creation
 *
 * </pre>
 *
 * @author rjpeter
 */
public class MetaDataParseException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Default Constructor
     *
     */
    public MetaDataParseException() {
        super();
    }

    /**
     * @param message
     */
    public MetaDataParseException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public MetaDataParseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public MetaDataParseException(Throwable cause) {
        super(cause);
    }

}
