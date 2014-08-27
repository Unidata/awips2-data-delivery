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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.raytheon.uf.common.localization.PathManagerFactoryTest;
import com.raytheon.uf.common.util.TestUtil;

/**
 * Test {@link OpenDAPParseUtility}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 24, 2012 955        djohnson     Initial creation
 * Aug 31, 2012 1125       djohnson     Rename test method to match tested class method rename.
 * Nov 13, 2012 1286       djohnson     Ignore two test methods until it can be determined whether they fail because of test or code error.
 * Jan 08, 2013 1466       dhladky      NCOM dataset name parsing fix.
 * Feb 06, 2013 1543       djohnson     Remove test setup methods no longer necessary.
 * Aug 26, 2014 3365       ccody        Separate Data Delivery tests out of AWIPS 2 baseline.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class OpenDAPParseUtilityTest {

    @Before
    public void setUp() {
        PathManagerFactoryTest.initLocalization();
    }

    @Test
    public void testGetDataSetNameAndCycle() throws Exception {
        // In the future the filename below will be renamed, but for the sake of
        // the changeset being easily diffed I am leaving it the same (otherwise
        // it shows as a brand new file)
        List<List<String>> inputsAndOutputs = TestUtil.getInputsAndOutputs(
                OpenDAPParseUtilityTest.class, "getCollectionAndCycle.txt");
        List<String> inputs = inputsAndOutputs.get(0);
        List<String> outputs = inputsAndOutputs.get(1);

        for (int i = 0; i < inputs.size(); i++) {
            String[] input = TestUtil.COMMA_PATTERN.split(inputs.get(i));
            String[] output = TestUtil.COMMA_PATTERN.split(outputs.get(i));
            if ("null".equals(output[2])) {
                output[2] = null;
            }
            String[] actual = null;
            
            if (input[0].equals("ncom_amseas")) {
            	System.out.println("Stop");
            }

            actual = OpenDAPParseUtility.getInstance()
                    .getDataSetNameAndCycle(input[0], input[1]).toArray(
                            new String[0]);

            TestUtil.assertArrayEquals(output, actual);
        }
    }

    @Test
    public void testParseDateRemovesZ() {
        final String expected = "07242012";
        assertEquals(expected, OpenDAPParseUtility.getInstance().parseDate(expected + "z"));
    }

    @Test
    public void testParseTimeStep() throws IOException {
        List<List<String>> inputsAndOutputs = TestUtil.getInputsAndOutputs(
                OpenDAPParseUtilityTest.class, "parseTimeStep.txt");
        List<String> inputs = inputsAndOutputs.get(0);
        List<String> outputs = inputsAndOutputs.get(1);

        for (int i = 0; i < inputs.size(); i++) {
            String[] expected = TestUtil.COMMA_PATTERN.split(outputs.get(i));
            List<String> actual = OpenDAPParseUtility.getInstance().parseTimeStep(inputs.get(i));

            TestUtil.assertArrayEquals(expected,
                    actual.toArray(new String[actual.size()]));
        }
    }

    @Test
    public void testParseUnits() throws IOException {
        List<List<String>> inputsAndOutputs = TestUtil.getInputsAndOutputs(
                OpenDAPParseUtilityTest.class, "parseUnits.txt");
        List<String> inputs = inputsAndOutputs.get(0);
        List<String> outputs = inputsAndOutputs.get(1);

        for (int i = 0; i < inputs.size(); i++) {
            String input = inputs.get(i);
            String expected = outputs.get(i);

            String actual = OpenDAPParseUtility.getInstance().parseUnits(input);

            assertEquals(expected, actual);
        }
    }

    @Test
    public void testTrimDateRemovesQuotes() {
        final String withQuotes = "07\"24\"2012";
        final String expected = "07242012";
        assertEquals(expected, OpenDAPParseUtility.getInstance().trim(withQuotes));
    }
}
