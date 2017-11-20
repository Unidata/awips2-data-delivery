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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Ensemble;
import com.raytheon.uf.common.datadelivery.registry.LevelGroup;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.ParameterGroup;
import com.raytheon.uf.common.datadelivery.registry.ParameterLevelEntry;
import com.raytheon.uf.common.datadelivery.registry.ParameterUtils;
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
 * Sep 20, 2017  6413     tjensen   Updated for ParameterGroups
 * Nov 15, 2017  6498     tjensen   Updated to build ParameterGroups from
 *                                  Parameters if not available.
 *
 * </pre>
 *
 * @author dhladky
 */
@DynamicSerialize
public class RetrievalAttribute<T extends Time, C extends Coverage> {

    @DynamicSerializeElement
    @Deprecated
    private Parameter parameter;

    @DynamicSerializeElement
    private ParameterGroup parameterGroup;

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

    public ParameterGroup getParameterGroup() {
        if (parameterGroup == null) {
            List<Parameter> parameters = new ArrayList<>();
            parameters.add(parameter);
            Map<String, ParameterGroup> parameterGroupMap = ParameterUtils
                    .generateParameterGroupsFromParameters(parameters);
            parameterGroup = parameterGroupMap.values().iterator().next();
        }
        return parameterGroup;
    }

    public void setParameterGroup(ParameterGroup parameterGroup) {
        this.parameterGroup = parameterGroup;
    }

    /**
     * The ParameterGroup on retrieval attributes should contain a single level
     * group. If it does, return that level group. Else throw an exception
     *
     * @return the single LevelGroup in the ParameterGroup
     */
    public LevelGroup getLevelGroup() {
        Map<String, LevelGroup> groupedLevels = parameterGroup
                .getGroupedLevels();
        if (groupedLevels.size() != 1) {
            throw new IllegalStateException(
                    "Retrieval Attribute for parameter '"
                            + parameterGroup.getAbbrev()
                            + "' contains multiple level groups.");
        }
        return groupedLevels.values().iterator().next();
    }

    /**
     * The ParameterGroup on retrieval attributes should contain a single level
     * group. If it does, return that level group. Else throw an exception
     *
     * @return the single LevelGroup in the ParameterGroup
     */
    public ParameterLevelEntry getEntry() {
        LevelGroup lg = getLevelGroup();
        if (lg != null) {
            List<ParameterLevelEntry> levels = lg.getLevels();
            if (levels.size() == 1) {
                return levels.get(0);
            }
        }
        throw new IllegalStateException("Retrieval Attribute for parameter '"
                + parameterGroup.getAbbrev()
                + "' contains multiple level entries.");
    }

}
