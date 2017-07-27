package com.raytheon.uf.edex.datadelivery.retrieval.interfaces;

/**
 * Interface for Provider Response Builder
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 16, 2011           dhladky   Initial creation
 * Feb 12, 2013  1543     djohnson  The payload can just be an arbitrary object,
 *                                  implementations can define an array if
 *                                  required.
 * Feb 15, 2013  1543     djohnson  Expose the setAttributes method.
 * Jul 27, 2017  6186     rjpeter   Removed payload methods
 *
 * </pre>
 *
 * @author dhladky
 */
public interface IRetrievalResponse {
    void prepareForSerialization() throws Exception;
}
