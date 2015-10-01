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

import com.raytheon.uf.common.datadelivery.registry.Connection;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.MetaDataExtractor;

/**
 * Abstract Class for PDA MetaData extraction
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * July 08, 2014 3120        dhladky     Initial creation
 * Sept 11, 2015 4881        dhladky     Improved debug tracking.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 * @param <D>
 */

public abstract class PDAMetaDataExtractor<O, D> extends MetaDataExtractor<O, D> {
    
    /** DEBUG PDA system **/
    private static final String DEBUG = "DEBUG";
    
    /** debug state */
    protected Boolean debug = false;
    
    public PDAMetaDataExtractor() {
        super(new Connection());
        
        serviceConfig = HarvesterServiceManager.getInstance().getServiceConfig(
                ServiceType.PDA);
        // debugging MetaData parsing.
        String debugVal = serviceConfig.getConstantValue(DEBUG);
        debug = Boolean.valueOf(debugVal);
    }

}
