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

import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * DataSetConfigInfo XML Object
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 14, 2017 1045       tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
@XmlAccessorType(XmlAccessType.NONE)
public class DataSetConfigInfo {
    @XmlElement
    private String dataSetNameRegex;

    @XmlElement
    private boolean moving;

    @XmlElement(name = "sizeEstimatePerParamInKB")
    private long sizeEstimate;

    @XmlElement(name = "parentBounds", type = DataSetBounds.class)
    private DataSetBounds parentBounds;

    private Pattern dataSetNamePattern;

    public String getDataSetNameRegex() {
        return dataSetNameRegex;
    }

    public void setDataSetNameRegex(String dataSetNameRegex) {
        this.dataSetNameRegex = dataSetNameRegex;
    }

    public Pattern getDataSetNamePattern() {
        if (dataSetNamePattern == null) {
            dataSetNamePattern = Pattern.compile(dataSetNameRegex);
        }
        return dataSetNamePattern;
    }

    public long getSizeEstimate() {
        return sizeEstimate;
    }

    public void setSizeEstimate(long sizeEstimate) {
        this.sizeEstimate = sizeEstimate;
    }

    public DataSetBounds getParentBounds() {
        return parentBounds;
    }

    public void setParentBounds(DataSetBounds parentBounds) {
        this.parentBounds = parentBounds;
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean isMoving) {
        this.moving = isMoving;
    }

}
