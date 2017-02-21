package com.raytheon.uf.edex.datadelivery.retrieval.util;

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
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * A JAXBable set of {@link PDADataSetNameMap}s.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 11, 2016 5752       tjensen     Initial creation
 * 
 * </pre>
 * 
 * @author tjensen
 * @version 1.0
 */
@XmlRootElement(name = "pdaDataSetNameMapSet")
@XmlAccessorType(XmlAccessType.NONE)
public class PDADataSetNameMapSet {

    /**
     * List of DataSet Mappings from the XML.
     */
    @XmlElements({
            @XmlElement(name = "pdaDataSetNameMap", type = PDADataSetNameMap.class) })
    private ArrayList<PDADataSetNameMap> maps;

    public ArrayList<PDADataSetNameMap> getMaps() {
        return maps;
    }

    public void setMaps(ArrayList<PDADataSetNameMap> maps) {
        this.maps = maps;
    }

    public void addMaps(Collection<PDADataSetNameMap> mapsToAdd) {
        if (this.maps == null) {
            this.maps = new ArrayList<>();
        }
        this.maps.addAll(mapsToAdd);
    }
}
