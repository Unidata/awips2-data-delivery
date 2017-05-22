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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.raytheon.uf.common.datadelivery.registry.VersionData;

/**
 *
 * DataSetVersionInfo XML object
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 3, 2017  6130       tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
@XmlAccessorType(XmlAccessType.NONE)
public class DataSetVersionInfo {

    @XmlAttribute(name = "id")
    private String id;

    @XmlElement(name = "dataSetNamePattern")
    private DataSetNamePattern dataSetNamePattern;

    @XmlElements({
            @XmlElement(name = "versionData", type = VersionData.class) })
    private List<VersionData> versionDataList;

    public DataSetNamePattern getDataSetNamePattern() {
        return dataSetNamePattern;
    }

    public void setDataSetNamePattern(DataSetNamePattern dataSetNamePattern) {
        this.dataSetNamePattern = dataSetNamePattern;
    }

    public List<VersionData> getVersionDataList() {
        return versionDataList;
    }

    public void setVersionDataList(List<VersionData> versionDataList) {
        this.versionDataList = versionDataList;
    }

    public void addVersionData(Collection<VersionData> dataToAdd) {
        if (this.versionDataList == null) {
            this.versionDataList = new ArrayList<>();
        }
        this.versionDataList.addAll(dataToAdd);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String validate() {
        StringBuilder sb = new StringBuilder();
        for (VersionData vd : getVersionDataList()) {
            String vdErrors = vd.validate();
            if (!"".equals(vdErrors)) {
                sb.append(vdErrors);
            }
        }

        return sb.toString();
    }

}
