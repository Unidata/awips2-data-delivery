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
package com.raytheon.uf.edex.datadelivery.retrieval;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Configuration XML file for number of retrieval threads per provider
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 21, 2017 6433       tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

@XmlRootElement(name = "retrievalThreadsConfig")
@XmlAccessorType(XmlAccessType.NONE)
public class RetrievalThreadsConfig {

    @XmlElementWrapper(name = "providers", required = true)
    @XmlElement(name = "provider")
    private List<RetrievalThreadsProvider> providers = new ArrayList<>();

    public List<RetrievalThreadsProvider> getProviders() {
        return providers;
    }

    public void setProviders(List<RetrievalThreadsProvider> providers) {
        this.providers = providers;
    }
}
