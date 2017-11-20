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
package com.raytheon.uf.edex.datadelivery.retrieval.xml.legacy;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.Ensemble;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;

/**
 * Used to represent legacy RetrievalAttribute
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 27, 2017  6186     rjpeter   Initial creation.
 * Sep 20, 2017  6413     tjensen   Update for ParameterGroups
 * Nov 15, 2017  6498     tjensen   Remove ParameterGroups to reduce
 *                                  duplication. Will build from parameters
 *                                  until switching to use SbnRetrievalInfoXml
 *
 * </pre>
 *
 * @author rjpeter
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SbnRetrievalAttributeXml<T extends Time, C extends Coverage> {
    @XmlElement
    private Parameter parameter;

    @XmlElement
    private String subName;

    @XmlElement(name = "plugin")
    private String plugin;

    @XmlAnyElement(lax = true)
    private C coverage;

    @XmlElement(name = "time", type = Time.class)
    private T time;

    @XmlElement
    private Ensemble ensemble;

    @XmlElement(name = "provider")
    private String provider;

    public SbnRetrievalAttributeXml() {

    }

    public SbnRetrievalAttributeXml(Retrieval<T, C> retrieval) {
        RetrievalAttribute<T, C> att = retrieval.getAttribute();
        this.parameter = att.getParameter();
        this.subName = retrieval.getSubscriptionName();
        this.plugin = retrieval.getPlugin();
        this.coverage = att.getCoverage();
        this.time = att.getTime();
        this.ensemble = att.getEnsemble();
        this.provider = retrieval.getProvider();
    }

    public C getCoverage() {
        return coverage;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public String getPlugin() {
        return plugin;
    }

    public String getProvider() {
        return provider;
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

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setTime(T time) {
        this.time = time;
    }

    public void setSubName(String subName) {
        this.subName = subName;
    }

    public String getSubName() {
        return subName;
    }

    public Ensemble getEnsemble() {
        return ensemble;
    }

    public void setEnsemble(Ensemble ensemble) {
        this.ensemble = ensemble;
    }
}
