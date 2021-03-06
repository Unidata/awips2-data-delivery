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
package com.raytheon.uf.viz.datadelivery.subscription.subset.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.raytheon.uf.viz.datadelivery.common.xml.IDisplayXml;

/**
 * XML used to save selections for VerticalSubsetTab
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------------------------------
 * Mar 29, 2012           mpduff    Initial creation.
 * Jun 04, 2013  223      mpudff    Refactored Method name change.
 * Sep 14, 2017  6413     tjensen   Use Lists instead of ArrayLists
 * Dec 19, 2017  6523     tjensen   Add fields for selected parameters/levels
 *
 * </pre>
 *
 * @author mpduff
 */
@XmlAccessorType(XmlAccessType.NONE)
public class VerticalXML implements IDisplayXml {

    @XmlElement(name = "layerType", type = String.class)
    protected String layerType;

    @XmlElements({ @XmlElement(name = "parameters", type = String.class) })
    protected List<String> parameterList = new ArrayList<>();

    @XmlElements({ @XmlElement(name = "levels", type = String.class) })
    protected List<String> levelList = new ArrayList<>();

    @XmlElements({ @XmlElement(name = "selected", type = String.class) })
    protected List<String> selectedList = new ArrayList<>();

    /**
     * @return the layerType
     */
    public String getLayerType() {
        return layerType;
    }

    /**
     * @param layerType
     *            the layerType to set
     */
    public void setLayerType(String layerType) {
        this.layerType = layerType;
    }

    /**
     * @return the parameterList
     */
    public List<String> getParameterList() {
        return parameterList;
    }

    /**
     * @param parameterList
     *            the parameterList to set
     */
    public void setParameterList(List<String> parameterList) {
        this.parameterList = parameterList;
    }

    /**
     * @return the levels
     */
    public List<String> getLevels() {
        return levelList;
    }

    /**
     * @param levels
     *            the levels to set
     */
    public void setLevels(List<String> levels) {
        this.levelList = levels;
    }

    /**
     * @param level
     */
    public void addLevel(String level) {
        this.levelList.add(level);
    }

    /**
     * @param parameter
     */
    public void addParameter(String parameter) {
        this.parameterList.add(parameter);
    }

    @Override
    public String getPreviewString() {
        final String nl = "\n";
        StringBuilder sb = new StringBuilder();
        sb.append(nl);
        sb.append("Layer Type: " + this.layerType + nl);
        if (!levelList.isEmpty()) {
            sb.append("  Levels: ");
            for (String level : levelList) {
                sb.append(level + " ");
            }
            sb.append(nl);
        }
        sb.append("  Parameters: ");
        for (String parameter : parameterList) {
            sb.append(parameter + " ");
        }

        sb.append(nl);

        return sb.toString();
    }

    public List<String> getSelectedList() {
        return selectedList;
    }

    public void setSelectedList(List<String> selectedList) {
        this.selectedList = selectedList;
    }

    public void addSelected(String selected) {
        this.selectedList.add(selected);
    }
}
