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
package com.raytheon.uf.edex.datadelivery.retrieval.pda.metadata;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.adapters.AbstractMetadataAdapter;

/**
 *
 * Convert RetrievalAttribute to Satellite/GOESSounding objects.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 28, 2014  3121     dhladky   Initial javadoc
 * Oct 14, 2014  3127     dhladky   Improved deletion of files
 * Nov 20, 2014  3127     dhladky   GOES Sounding processing.
 * Dec 02, 2014  3826     dhladky   PDA test code
 * Jan 28, 2016  5299     dhladky   PDA testing related fixes.
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * May 03, 2016  5599     tjensen   Pass subscription name to GoesrDecoder to
 *                                  override sectorID.
 * May 16, 2016  5599     tjensen   Refresh dataURI after overriding sectorID.
 * Jul 25, 2016  5686     rjpeter   Fix dataURI of messageData.
 * Jun 27, 2016  5584     nabowle   Netcdf decoder consolidation.
 * Aug 22, 2016  5843     tjensen   Stop overriding dataURI
 * Sep 16, 2016  5762     tjensen   Added config value to not delete files
 * Jul 25, 2017  6186     rjpeter   Use Retrieval
 *
 * </pre>
 *
 * @author dhladky
 */
public class PDAMetaDataAdapter
        extends AbstractMetadataAdapter<PluginDataObject, Time, Coverage> {
    public PDAMetaDataAdapter() {

    }

    @Override
    public void processRetrieval(Retrieval<Time, Coverage> retrieval)
            throws InstantiationException {
        // no-op; Called from AbstractMetadataAdapter constructor.
        // throw new InstantiationException(
        // "PDAMetaDataAdapter is not implemented. Retrieval processing should
        // be in ingest jvm.");
    }

    @Override
    public PluginDataObject getRecord(PluginDataObject o) {
        // unimplemented by PDA, returns what you give it in this case.
        return o;
    }

    @Override
    public void allocatePdoArray(int size) {
        /*
         * unimplemented by PDA, don't know the number of SAT records in the
         * NetCDF file.
         */
    }
}
