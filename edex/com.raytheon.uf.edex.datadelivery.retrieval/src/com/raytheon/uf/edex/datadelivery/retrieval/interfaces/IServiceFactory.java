package com.raytheon.uf.edex.datadelivery.retrieval.interfaces;
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

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;

/**
 * The factory implementation to retrieve service specific classes.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 24, 2012 955        djohnson     Initial creation
 * May 31, 2013 2038       djohnson     Add setProvider.
 * Jul 08, 2014  3120      dhladky      Generics
 *
 * </pre>
 *
 * @author djohnson
 * @version 1.0
 */

public interface IServiceFactory<O, D, T extends Time, C extends Coverage> {

    /**
     * Retrieve the metadata parser.
     *
     * @param lastUpdate
     *            the last update time
     * @return the parser
     */
    IParseMetaData<O> getParser();

    /**
     * Retrieve the {@link RetrievalGenerator}.
     *
     * @return the generator
     */
    RetrievalGenerator<T, C> getRetrievalGenerator();
}
