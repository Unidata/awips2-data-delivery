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
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * DataSetConfigInfoMap XML Object
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 13, 2017 1045       tjensen     Initial creation
 * May 09, 2017 6130       tjensen     Remove unneeded map
 *
 * </pre>
 *
 * @author tjensen
 */

@XmlRootElement(name = "DataSetConfigInfoMap")
@XmlAccessorType(XmlAccessType.NONE)
public class DataSetConfigInfoMap {

    @XmlElements({
            @XmlElement(name = "DataSetConfigInfo", type = DataSetConfigInfo.class) })
    private List<DataSetConfigInfo> dataSetConfigInfoList;

    public List<DataSetConfigInfo> getDataSetConfigInfoList() {
        return dataSetConfigInfoList;
    }

    public void setDataSetConfigInfoList(
            List<DataSetConfigInfo> dataSetConfigInfoList) {
        this.dataSetConfigInfoList = dataSetConfigInfoList;
    }

    public void addDataSets(Collection<DataSetConfigInfo> setsToAdd) {
        if (this.dataSetConfigInfoList == null) {
            this.dataSetConfigInfoList = new ArrayList<>();
        }
        this.dataSetConfigInfoList.addAll(setsToAdd);
    }
}
