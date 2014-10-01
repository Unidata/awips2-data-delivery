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

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

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
 * Sept 04, 2014 3121        dhladky     Some rework on the parsing.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PDAFileMetaDataExtractor extends PDAMetaDataExtractor<String, String> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDAFileMetaDataExtractor.class);
    
    public static final Pattern separator = Pattern.compile("/");
    
    public static final Pattern paramSeperator = Pattern.compile("_");

    public static final Pattern param2Seperator = Pattern.compile("-");
    
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
    String paramterName = ABI-L2-ACMF-(M4) portion parsed out
    String startTime = 20132071413122 portion parsed out starting with lower case "s"
    String endTime = 20132071413122 portion parsed out starting with lower case "e"
    String dataTime = 20132071413122 portion parsed out starting with lower case "c"
     */
    public Map<String, String> extractMetaData(Object file) throws Exception {
               
        Map<String, String> paramMap = new HashMap<String, String>(7);
        String fileName = (String)file;
        fileName = fileName.replace(nc, "");
        String[] separated = separator.split(fileName);

        // we want just the file name at the end
        String[] parsed = paramSeperator.split(separated[separated.length - 1]);

        // parse out fields
        if (parsed != null) {
            // don't care about 1 right now
            paramMap.put("collectionName", parsed[0]);
            String[] paramSeparated = param2Seperator.split(parsed[1]);
            paramMap.put("satelliteName", parsed[2]);
            paramMap.put("dataSetName", paramSeparated[0]+"-"+paramSeparated[1]+"-"+paramSeparated[2]);
            paramMap.put("paramName", paramSeparated[3]);
            // take three, chop off the "s"
            String time = parsed[3].substring(1);
            // take four, chop off the "e"
            String endTime = parsed[4].substring(1);
            // take five, chop off the "c"
            String dataTime = parsed[5].substring(1);
            
            paramMap.put("startTime", time);
            paramMap.put("endTime", endTime);
            paramMap.put("dataTime", dataTime);
           
        } else {
            statusHandler.error("Coudn't create parameter mappings from file!", fileName);
        }
        
        return paramMap;
    }

    @Override
    public void setDataDate() throws Exception {
        // No implementation in PDA file extractor
    }

}
