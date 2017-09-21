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
package com.raytheon.uf.edex.datadelivery.retrieval.opendap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.raytheon.uf.common.serialization.SerializationException;

/**
 * Serializer for OpenDAP retrieval responses.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 12, 2013  1543     djohnson  Initial creation
 * Feb 15, 2013  1543     djohnson  Also can be used as JAXB adapter for
 *                                  DataDDS.
 * Apr 15, 2015  4400     dhladky   Updated for DAP2 protocol and backward
 *                                  compatibility.
 * Jul 27, 2017  6186     rjpeter   Remove Thrift Serialization.
 * Sep 21, 2017  6441     tgurney   Remove references to dods-1.1.7
 *
 * </pre>
 *
 * @author djohnson
 */
public class OpenDapRetrievalResponseSerializer
        extends XmlAdapter<byte[], Object> {
    @Override
    public Object unmarshal(byte[] v) throws Exception {
        opendap.dap.DConnect dconnect = new opendap.dap.DConnect(
                new ByteArrayInputStream(v));
        return dconnect.getData(null);
    }

    @Override
    public byte[] marshal(Object v) throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream(700)) {

            if (v instanceof opendap.dap.DataDDS) {
                ((opendap.dap.DataDDS) v).externalize(os, true, true);
            } else {
                throw new SerializationException(
                        "Unknown type for DDS serialization. "
                                + v.getClass().getName());
            }

            return os.toByteArray();

        } catch (IOException e) {
            throw new SerializationException(
                    "Unable to externalize the DataDDS instance.", e);
        }
    }
}
