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
 * DataSetVersionInfoMap XML object
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

@XmlRootElement(name = "DataSetVersionInfoMap")
@XmlAccessorType(XmlAccessType.NONE)
public class DataSetVersionInfoMap {

    @XmlElements({
            @XmlElement(name = "VersionInfo", type = DataSetVersionInfo.class) })
    private List<DataSetVersionInfo> dsviList;

    public void addDataSets(Collection<DataSetVersionInfo> infoToAdd) {
        if (this.dsviList == null) {
            this.dsviList = new ArrayList<>();
        }
        this.dsviList.addAll(infoToAdd);
    }

    public List<DataSetVersionInfo> getDsviList() {
        return dsviList;
    }

    public void setDsviList(List<DataSetVersionInfo> dsviList) {
        this.dsviList = dsviList;
    }

    public String validate() {
        StringBuilder sb = new StringBuilder();
        for (DataSetVersionInfo dsvi : getDsviList()) {
            String dsviErrors = dsvi.validate();
            if (!"".equals(dsviErrors)) {
                sb.append("Errors with " + dsvi.getId() + ": ");
                sb.append(dsviErrors);
                sb.append(System.lineSeparator());
            }
        }

        return sb.toString();
    }
}
