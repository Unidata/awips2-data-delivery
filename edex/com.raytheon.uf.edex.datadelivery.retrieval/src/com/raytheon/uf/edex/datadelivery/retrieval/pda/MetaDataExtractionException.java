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
package com.raytheon.uf.edex.datadelivery.retrieval.pda;

/**
 * 
 * Exception for when errors occur extracting metadata
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 17, 2016            tjensen     Initial creation
 * 
 * </pre>
 * 
 * @author tjensen
 * @version 1.0
 */
public class MetaDataExtractionException extends Exception {

    private static final long serialVersionUID = 1955281866716862669L;

    public MetaDataExtractionException(String string) {
        super(string);
    }

    public MetaDataExtractionException(String string, Exception e) {
        super(string, e);
    }
}