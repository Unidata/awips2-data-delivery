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
import com.raytheon.uf.common.dataplugin.PluginDataObject;

/**
 * translation interface
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jan 18, 2011           dhladky   Initial creation
 * Oct 07, 2013           dhladky   More generics
 * Jul 27, 2017  6186     rjpeter   Removed RetrievalAttribute
 *
 * </pre>
 *
 * @author dhladky
 */

public interface IRetrievalTranslator<T extends Time, C extends Coverage, RecordKey extends Object> {
    public Class<?> getPdoClass();

    public void setPdoClass(String clazz) throws ClassNotFoundException;

    public PluginDataObject getPdo(RecordKey key)
            throws InstantiationException, IllegalAccessException;

}
