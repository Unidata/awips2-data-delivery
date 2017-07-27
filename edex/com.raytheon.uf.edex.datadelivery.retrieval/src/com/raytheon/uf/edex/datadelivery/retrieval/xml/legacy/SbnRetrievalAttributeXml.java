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
