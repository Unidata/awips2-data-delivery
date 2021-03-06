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
package com.raytheon.uf.edex.datadelivery.retrieval.wfs;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.PointTime;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IParseMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IServiceFactory;

/**
 * {@link IServiceFactory} implementation for WFS.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 25, 2012 955        djohnson     Initial creation
 * May 12, 2013 753        dhladky      implementation
 * May 31, 2013 2038       djohnson     Add setProvider.
 * Jun 11, 2013 1763       dhladky      Service provider impl for WFS
 *
 * </pre>
 *
 * @author djohnson
 */
public class WfsServiceFactory<O extends Object, D extends Object>
        implements IServiceFactory<O, D, PointTime, Coverage> {

    @Override
    public IParseMetaData<O> getParser() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public RetrievalGenerator<PointTime, Coverage> getRetrievalGenerator() {
        return new WfsRetrievalGenerator();
    }
}
