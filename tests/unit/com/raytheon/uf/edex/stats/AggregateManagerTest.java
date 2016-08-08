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
package com.raytheon.uf.edex.stats;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.PathManagerFactoryTest;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.stats.StatsGrouping;
import com.raytheon.uf.common.stats.StatsGroupingColumn;
import com.raytheon.uf.common.stats.xml.StatisticsConfig;
import com.raytheon.uf.common.stats.xml.StatisticsEventConfig;
import com.raytheon.uf.edex.stats.util.ConfigLoader;

/**
 * Test {@link AggregateManager}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 15, 2013  1487     djohnson  Initial creation
 * Aug 26, 2014  3365     ccody     Separate Data Delivery tests out of AWIPS 2
 *                                  baseline.
 * Aug 08, 2016  5744     mapeters  Stats config file moved from edex_static to
 *                                  common_static
 * 
 * </pre>
 * 
 * @author djohnson
 */

public class AggregateManagerTest {
    private static SingleTypeJAXBManager<StatisticsConfig> jaxbManager;

    @BeforeClass
    public static void classSetUp() throws JAXBException {
        jaxbManager = new SingleTypeJAXBManager<>(StatisticsConfig.class);
    }

    @Before
    public void setUp() {
        PathManagerFactoryTest.initLocalization();
    }

    @Test
    public void testDeterminingGroupForEvent() throws Exception {
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext ctx = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.BASE);
        String path = "stats" + IPathManager.SEPARATOR + "mockStats.xml";
        final ILocalizationFile lf = pm.getLocalizationFile(ctx, path);

        StatisticsConfig statisticsConfig = null;
        try (InputStream is = lf.openInputStream()) {
            statisticsConfig = jaxbManager.unmarshalFromInputStream(is);
        }

        ConfigLoader.validate(
                Maps.<String, StatisticsEventConfig> newHashMap(),
                statisticsConfig);

        MockEvent mockEvent = new MockEvent();
        mockEvent.setPluginName("somePlugin");
        mockEvent.setFileName("someFileName");
        mockEvent.setProcessingTime(1000L);
        mockEvent.setProcessingLatency(500L);

        List<StatsGrouping> groupList = new ArrayList<>();
        groupList.add(new StatsGrouping("pluginName", "somePlugin"));
        groupList.add(new StatsGrouping("fileName", "someFileName"));
        StatsGroupingColumn expectedGroupingColumn = new StatsGroupingColumn();
        expectedGroupingColumn.setGroup(groupList);

        final StatsGroupingColumn actualGroupingColumn = AggregateManager
                .determineGroupRepresentationForEvent(statisticsConfig
                        .getEvents().iterator().next(), mockEvent);
        Assert.assertThat(actualGroupingColumn,
                CoreMatchers.is(CoreMatchers.equalTo(expectedGroupingColumn)));
    }
}
