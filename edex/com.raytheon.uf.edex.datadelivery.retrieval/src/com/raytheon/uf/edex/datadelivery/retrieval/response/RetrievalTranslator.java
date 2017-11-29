package com.raytheon.uf.edex.datadelivery.retrieval.response;

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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.database.plugin.PluginFactory;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IRetrievalTranslator;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.adapters.AbstractMetadataAdapter;

/**
 * Abstract Translator
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 18, 2011  191      dhladky   Initial creation
 * Feb 07, 2013  1543     djohnson  Allow overriding of methods for mocking in
 *                                  tests.
 * Feb 12, 2013  1543     djohnson  Pass the exception as the cause for
 *                                  instantiation exceptions.
 * May 31, 2013  2038     djohnson  Protected access for constructor.
 * Jun 11, 2013  2101     dhladky   Imports
 * Sep 24, 2014  3121     dhladky   try to improve generic usage.
 * Jul 27, 2017  6186     rjpeter   Use Retrieval
 * Nov 15, 2017  6498     tjensen   Improved logging on InstantiationException
 *
 * </pre>
 *
 * @author dhladky
 */

public abstract class RetrievalTranslator<T extends Time, C extends Coverage, RecordKey>
        implements IRetrievalTranslator<T, C, RecordKey> {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected Class<?> recordClass;

    protected Retrieval<T, C> retrieval;

    protected AbstractMetadataAdapter<RecordKey, T, C> metadataAdapter;

    /**
     * Used by all translators
     *
     * @param attXML
     */
    public RetrievalTranslator(Retrieval<T, C> retrieval)
            throws InstantiationException {
        this.retrieval = retrieval;

        try {
            PluginFactory factory = PluginFactory.getInstance();
            String clazz = factory
                    .getPluginRecordClassName(retrieval.getPlugin());
            configureFromPdoClassName(clazz);
        } catch (Exception e) {
            logger.error("Failed to instantiate RetrievalTranslator for plugin "
                    + retrieval.getPlugin(), e);
            throw new InstantiationException(
                    "Failed to instantiate RetrievalTranslator for plugin ["
                            + retrieval.getPlugin() + "]. Reason ["
                            + e.toString() + "]");
        }
    }

    @SuppressWarnings("unchecked")
    protected void configureFromPdoClassName(String className)
            throws InstantiationException, ClassNotFoundException {
        setPdoClass(className);
        metadataAdapter = (AbstractMetadataAdapter<RecordKey, T, C>) AbstractMetadataAdapter
                .getMetadataAdapter(getPdoClass(), retrieval);

    }

    @Override
    public Class<?> getPdoClass() {
        return recordClass;
    }

    @Override
    public void setPdoClass(String clazz) throws ClassNotFoundException {
        this.recordClass = Class.forName(clazz);
    }

    @Override
    public PluginDataObject getPdo(RecordKey key)
            throws InstantiationException, IllegalAccessException {
        PluginDataObject pdo = null;

        if (metadataAdapter != null) {
            pdo = metadataAdapter.getRecord(key);
        }

        return pdo;
    }

    /**
     * Get the number of subset times for your retrieval
     *
     * @return
     */
    protected abstract int getSubsetNumTimes();

    /**
     * Get the levels for your retrieval
     *
     * @return
     */
    protected abstract int getSubsetNumLevels();

    /**
     * Get a list of data times for your retrieval
     *
     * @return
     */
    protected abstract List<DataTime> getTimes();

}
