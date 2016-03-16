package com.raytheon.uf.edex.datadelivery.retrieval.metadata;

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

import java.util.Date;

import com.raytheon.uf.common.datadelivery.registry.Connection;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IExtractMetaData;

/**
 * Extract MetaData over the web.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 20, 2011    218      dhladky     Initial creation
 * Aug 08, 2012   1022      djohnson    Add @NotThreadSafe.
 * Jul 08, 2014   3120      dhladky     Generics
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */
public abstract class MetaDataExtractor<O extends Object, D extends Object>
        implements IExtractMetaData<O, D> {

    protected Connection conn = null;

    protected Date dataDate = null;

    protected ServiceConfig serviceConfig = null;

    public MetaDataExtractor(Connection conn) {
        this.conn = conn;
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    public Connection getConn() {
        return conn;
    }

    @Override
    public Date getDataDate() {
        return dataDate;
    }

    public void setDataDate(Date dataDate) {
        this.dataDate = dataDate;
    }

}
