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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Extract File Name based info for MetaData in PDA
 * Fill in for Fall 2014 test, should NEVER go to production
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * July 08, 2014 3120        dhladky     Initial creation
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDAFileMetaDataExtractor<O> extends PDAMetaDataExtractor<String, String> {

    public static final Pattern separator = Pattern.compile("/");
    
    public static final Pattern paramSeperator = Pattern.compile("_");
    
    public static final String nc = ".nc";
    
    public PDAFileMetaDataExtractor() {
    
    }
   
    /** 
     File Name parsing method of metadata extraction as proposed by Solers/PDA
     It is throw away code that needs to be here for the fall 2014 test only.
    
     <dc:title>/mnt/NAS/virtualDirs/GOES-R/GOES-16/ABI-L2-ACMF/20130726/DT_ABI
    -L2-ACMF-M4_G16_s20132071413122_e20131221305002_c20131221305003
    .nc</dc:title> 
    
    Most of this data is thrown away but you can gather certain parameters from it
    
    String collectionName = GOES-R portion
    String satelliteName = GOES-16 portion
    String dataSetName = ABI-L2-ACMF portion
    String paramterName = ABI-L2-ACMF-M4 portion parsed out
    String startTime = 20132071413122 portion parsed out starting with lower case "s"
    String endTime = 20132071413122 portion parsed out starting with lower case "e"
    String dataTime = 20132071413122 portion parsed out starting with lower case "c"
     */
    public Map<String, String> extractMetaData(Object file) throws Exception {
               
        Map<String, String> paramMap = new HashMap<String, String>(7);
        String fileName = (String)file;
        fileName = fileName.replace(nc, "");
        String[] separated = separator.split(fileName);

        // parse out fields
        if (separated != null && separated.length > 0) {
            // don't care about 0 -3
            paramMap.put("collectionName", separated[4]);
            paramMap.put("satelliteName", separated[5]);
            paramMap.put("dataSetName", separated[6]);
            // don't care about 7
            String[] paramSeparated = paramSeperator.split(separated[8]);
            // don't care about 0
            paramMap.put("paramName", paramSeparated[1]);
            // don't care about 2
            // take three, chop off the "s"
            String time = paramSeparated[3].substring(1);
            // take four, chop off the "e"
            String endTime = paramSeparated[4].substring(1);
            // take five, chop off the "c"
            String dataTime = paramSeparated[5].substring(1);
            
            paramMap.put("startTime", time);
            paramMap.put("endTime", endTime);
            paramMap.put("dataTime", dataTime);
           
        }
        
        return paramMap;
    }

    @Override
    public void setDataDate() throws Exception {
        // No implementation in PDA file extractor
    }

}
