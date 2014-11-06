package com.raytheon.uf.edex.datadelivery.retrieval.interfaces;

import java.util.Date;
import java.util.Map;

/**
 * Extract MetaData Interface
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 20, 2011    218      dhladky     Initial creation
 * jul 10,  2014  2130      dhladky     Expanded to include PDA
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public interface IExtractMetaData <O extends Object, D extends Object>{

    Map<String, D> extractMetaData(Object O) throws Exception;

    void setDataDate() throws Exception;

    Date getDataDate();

}
