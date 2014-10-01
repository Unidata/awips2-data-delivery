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

import java.text.ParseException;
import java.util.Map;

import net.opengis.cat.csw.v_2_0_2.BriefRecordType;
import net.opengis.gml.v_3_2_1.TimePeriodType;

import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Extract BriefRecord based info (future)
 * 
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

public class PDARecordMetaDataExtractor extends PDAMetaDataExtractor<BriefRecordType, String> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PDARecordMetaDataExtractor.class);
    
    
    public PDARecordMetaDataExtractor() {
       
    }

    @Override
    /**
     * This will be the real metaData extraction once it is determined 
     * what the content will look like.
     * 
     * We are going on this assumption, this is what a briefRecord will
     * eventually look like <csw:BriefRecord>
     * <dc:identifier>7572</dc:identifier>
     * <dc:title>/mnt/NAS/virtualDirs/GOES
     * -R/GOES-16/ABI-L2-ACMF/20130726/DT_ABI
     * -L2-ACMF-M4_G16_s20132071413122_e20121221305002_c20121221305003
     * .nc</dc:title> <dc:type>ABI-L2-ACMF</dc:type>
     * 
     * <ows:BoundingBox> <ows:LowerCorner>-40.7 100.16</ows:LowerCorner>
     * <ows:UpperCorner>-38.0 110.5</ows:UpperCorner> </ows:BoundingBox>
     * 
     * <gml:TimePeriod gml:id="CollectionStartEnd">
     * <gml:beginPosition>2013-02-07T14:13:12.2</gml:beginPosition>
     * <gml:endPosition>2012-01-22T13:05:00.2</gml:endPosition>
     * </gml:TimePeriod>
     * 
     * <dc:date>2012-01-22T13:05:00.3</dc:date>
     * <dc:creator>GOES-16</dc:creator> <dc:source>Test</dc:source>
     * <dc:relation>Development</dc:relation>
     * <dc:dataSet>Full-Disk</dc:dataSet> <dc:channel>Mode: 4</dc:channel>
     * <dc:format>NetCDF</dc:format> <dc:fillValue>NetCDF</dc:fillValue>
     * <dc:missingValue>NetCDF</dc:missingValue> <dc:units>Units</dc:units>
     * </csw:BriefRecord>
     */

    public Map<String, String> extractMetaData(Object record) throws Exception {
        
        // future use
        
     /* Store extra data in content of type
        List<String> extraContent = record.getType().getContent();

        // TODO: these all have to be faked for now!
        TimePeriodType period = new TimePeriodType();
        // fake start time
        List<String> startVals = new ArrayList<String>();
        startVals.add("2013-02-07T14:13:12.2");
        TimePositionType timeStart = new TimePositionType();
        timeStart.setValue(startVals);
        period.setBeginPosition(timeStart);
        // fake end time
        List<String> endVals = new ArrayList<String>();
        endVals.add("2012-01-22T13:05:00.2");
        TimePositionType timeEnd = new TimePositionType();
        timeStart.setValue(endVals);
        period.setBeginPosition(timeEnd);

       
        // if (!record.getTimePeriod().isEmpty) {
        time = getTime(dateFormat, period);
        ImmutableDate idate = new ImmutableDate();
        idate.setTime(time.getEnd().getTime());
        //}
 
        // this is all hypothetical for this
        String creator = extraContent.get(0);
        String relation = extraContent.get(1);
        String dataSet = extraContent.get(2);
        String channel = extraContent.get(3);
        String format = extraContent.get(4);
        String fillValue = extraContent.get(5);
        String missingValue = extraContent.get(6);
        String units = extraContent.get(7);
        */
        return null;
    }

    @Override
    public void setDataDate() throws Exception {
        // TODO Auto-generated method stub
        
    }
    
    /**
     * Extract the time info from the OGC TimePeriod
     * @param dateFormat
     * @param period
     * @return
     */
    private Time getTime(String dateFormat, TimePeriodType period) {

        Time time = new Time();
        time.setFormat(dateFormat);
        
        try {
            time.setStartDate(period.getBeginPosition().getValue().get(0));
            time.setEndDate(period.getEndPosition().getValue().get(0));
        } catch (ParseException e) {
            statusHandler.handle(Priority.PROBLEM, "Couldn't parse start/end time from format: "+dateFormat, e);
        }

        return time;
    }

}
