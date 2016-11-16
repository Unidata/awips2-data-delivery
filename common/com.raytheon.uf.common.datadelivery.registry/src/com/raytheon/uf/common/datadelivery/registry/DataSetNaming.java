package com.raytheon.uf.common.datadelivery.registry;

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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Dataset naming config class for Collection
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ---------------------------
 * Oct 20, 2012  1163     dhladky   Initial creation
 * Nov 07, 2013  2361     njensen   Remove ISerializableObject
 * Nov 09, 2016  5988     tjensen   Remove name
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class DataSetNaming {

    @XmlAttribute(name = "separator")
    @DynamicSerializeElement
    private String separator;

    @XmlAttribute(name = "expression")
    @DynamicSerializeElement
    private String expression;

    public DataSetNaming() {

    }

    public String getExpression() {
        return expression;
    }

    public String getSeparator() {
        return separator;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

}
