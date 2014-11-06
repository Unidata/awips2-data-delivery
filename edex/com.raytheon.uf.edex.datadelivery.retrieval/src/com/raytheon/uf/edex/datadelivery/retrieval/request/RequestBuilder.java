package com.raytheon.uf.edex.datadelivery.retrieval.request;

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

import org.apache.http.annotation.Immutable;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalRequestBuilder;

/**
 * Request XML translation related utilities
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 15, 2012            dhladky     Initial creation
 * Aug 12, 2012 1022       djohnson    Add {@link Immutable}.
 * Oct 14, 2014  3127      dhladky     Reorganized Request Builders
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */
public abstract class RequestBuilder<T extends Time, C extends Coverage> implements IRetrievalRequestBuilder<T, C> {

    public static final String AMPERSAND = "&";
    
    public static final String SPACE = " ";
    
    public static final String NEW_LINE = "\n";
    
    public static final String PROPERTTY_OPEN = "<ogc:PropertyName>";
    
    public static final String PROPERTTY_CLOSE = "</ogc:PropertyName>";
    
    public static final String PROPRERTYISGREATERTHAN_OPEN = "<ogc:PropertyIsGreaterThan>";
    
    public static final String PROPRERTYISGREATERTHAN_CLOSE = "</ogc:PropertyIsGreaterThan>";
    
    public static final String PROPRERTYISLESSTHAN_OPEN = "<ogc:PropertyIsLessThan>";
    
    public static final String PROPRERTYISLESSTHAN_CLOSE = "</ogc:PropertyIsLessThan>";
    
    public static final String ISLITERAL_OPEN = "<ogc:Literal>";
    
    public static final String ISLITERAL_CLOSE = "</ogc:Literal>";
    
    public static final String LOWER_CORNER_OPEN = "<gml:lowerCorner>";
    
    public static final String LOWER_CORNER_CLOSE = "</gml:lowerCorner>";
    
    public static final String UPPER_CORNER_OPEN = "<gml:upperCorner>";
    
    public static final String UPPER_CORNER_CLOSE = "</gml:upperCorner>";
    
    public static final String WITHIN_OPEN = "<ogc:Within>";
    
    public static final String WITHIN_CLOSE = "</ogc:Within>";
    
    public static final String AND_OPEN = "<ogc:And>";
    
    public static final String AND_CLOSE = "</ogc:And>";
    
    public static final String FILTER_OPEN = "<ogc:Filter>";
    
    public static final String FILTER_CLOSE = "</ogc:Filter>";
    
    public static final String ENVELOPE_OPEN = "<gml:Envelope";
    
    public static final String ENVELOPE_CLOSE = "</gml:Envelope>";

    protected final RetrievalAttribute<T, C> ra;

    protected RequestBuilder(RetrievalAttribute<T, C> ra) {
        this.ra = ra;
    }

    /**
     * Get the retrieval Attribute
     * @return ra
     */
    public RetrievalAttribute<T, C> getAttribute() {
        return ra;
    }
 }
