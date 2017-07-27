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
package com.raytheon.uf.common.datadelivery.retrieval.xml;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Ensemble;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Attribute, product from provider XML
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 17, 2011  191      dhladky   Initial creation
 * Feb 15, 2013  1543     djohnson  Allow any type of Coverage instance without
 *                                  a JAXB adapter.
 * Oct 01, 2013  1797     dhladky   Generics
 * Nov 07, 2013  2361     njensen   Remove ISerializableObject
 * Jul 20, 2017  6186     rjpeter   Removed redundant fields with retrieval.
 *
 * </pre>
 *
 * @author dhladky
 */
@DynamicSerialize
public class RetrievalAttribute<T extends Time, C extends Coverage> {

    @DynamicSerializeElement
    private Parameter parameter;

    @DynamicSerializeElement
    private C coverage;

    @DynamicSerializeElement
    private T time;

    // TODO: NOMADS only
    @DynamicSerializeElement
    private Ensemble ensemble;

    public C getCoverage() {
        return coverage;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public T getTime() {
        return time;
    }

    public void setCoverage(C coverage) {
        this.coverage = coverage;
    }

    public void setParameter(Parameter parameter) {
        this.parameter = parameter;
    }

    public void setTime(T time) {
        this.time = time;
    }

    public Ensemble getEnsemble() {
        return ensemble;
    }

    public void setEnsemble(Ensemble ensemble) {
        this.ensemble = ensemble;
    }

}
