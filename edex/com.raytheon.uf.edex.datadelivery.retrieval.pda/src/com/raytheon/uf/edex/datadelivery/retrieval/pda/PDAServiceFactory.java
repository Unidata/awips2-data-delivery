package com.raytheon.uf.edex.datadelivery.retrieval.pda;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IParseMetaData;
import com.raytheon.uf.edex.datadelivery.retrieval.interfaces.IServiceFactory;

import net.opengis.cat.csw.v_2_0_2.BriefRecordType;

/**
 * {@link IServiceFactory} implementation for PDA.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------------
 * Jun 13, 2014  3120     dhladky   Initial creation
 * Oct 14, 2014  3127     dhladky   Additional types added.
 * Mar 31, 2017  6186     rjpeter   Fixed generics.
 * </pre>
 *
 * @author dhladky
 */
public class PDAServiceFactory
        implements IServiceFactory<BriefRecordType, String, Time, Coverage> {

    public PDAServiceFactory() {
    }

    @Override
    public IParseMetaData<BriefRecordType> getParser() {
        return new PDAMetaDataParser();
    }

    @Override
    public RetrievalGenerator<Time, Coverage> getRetrievalGenerator() {
        return new PDARetrievalGenerator();
    }

}
