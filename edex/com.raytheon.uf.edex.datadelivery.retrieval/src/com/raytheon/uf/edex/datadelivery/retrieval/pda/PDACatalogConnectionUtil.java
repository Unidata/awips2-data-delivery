package com.raytheon.uf.edex.datadelivery.retrieval.pda;

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

import java.net.URI;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.raytheon.uf.common.comm.HttpClient;
import com.raytheon.uf.common.comm.HttpClient.HttpClientResponse;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * 
 * PDA catalog Connection.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * June 14, 2014 3120       dhladky     created.
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */


public class PDACatalogConnectionUtil {
    
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDACatalogConnectionUtil.class);
    /**
     * Connect to the CSW catalog service of PDA
     * @param request
     * @param cswURL
     * @return
     */
    public static String connect(String request, String cswURL) {

        String xmlResponse = null;
        HttpClient http = null;

        try {

            http = HttpClient.getInstance();
            // accept gzipped data for XML response
            http.setGzipResponseHandling(true);
            URI uri = new URI(cswURL);
            HttpPost post = new HttpPost(uri);
            post.setEntity(new StringEntity(request, "text/xml", "ISO-8859-1"));
            HttpClientResponse response = http.executeRequest(post);
            xmlResponse = new String(response.data);

        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Couldn't connect to PDA server: " + cswURL, e);
        }

        return xmlResponse;
    }
    
}
