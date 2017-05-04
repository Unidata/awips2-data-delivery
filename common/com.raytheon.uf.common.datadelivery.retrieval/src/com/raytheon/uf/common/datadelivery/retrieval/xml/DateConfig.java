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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Regex date files
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date           Ticket#  Engineer  Description
 * -------------- -------- --------- ---------------------------
 * 12 Sept, 2012  1038     dhladky   Initial creation
 * 07 Nov, 2013   2361     njensen   Remove ISerializableObject
 * Mar 31, 2017   6186     rjpeter   Support incremental override
 *
 * </pre>
 *
 * @author dhladky
 */
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class DateConfig {
    @XmlElement(name = "format", type = String.class)
    @DynamicSerializeElement
    private List<String> formats;

    public DateConfig() {

    }

    public List<String> getFormats() {
        if (formats == null) {
            formats = new ArrayList<>(0);
        }

        return formats;
    }

    public void setFormats(List<String> formats) {
        this.formats = formats;
    }

    /**
     * Adds other to this definition, preferring other over this.
     *
     * @param other
     */
    public void combine(DateConfig other) {
        if (this.formats == null) {
            this.formats = other.formats;
        } else if (other.formats != null && !other.formats.isEmpty()) {
            this.formats.addAll(0, other.formats);
        }
    }
}
