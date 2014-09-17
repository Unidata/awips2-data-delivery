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
package com.raytheon.uf.common.datadelivery.registry;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.raytheon.uf.common.datadelivery.registry.Encryption.Algorithim;
import com.raytheon.uf.common.datadelivery.registry.Encryption.Padding;

/**
 * Test the encryption
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 23, 2013   2180     mpduff      Initial creation
 * Aug 26, 2014   3365     ccody       Separate Data Delivery tests out of AWIPS 2 baseline.
 *                                     Encryption API Has changed since this test was created.
 *                                     Test functionality that is no longer supported by the API (i.e. will not compile)
 *                                     has been "deactivated" (commented out).
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class EncryptionTest {
    private final String password = "P@ssW0rd";

    private static Encryption encryption;

    @BeforeClass
    public static void setupEncryption() {
        encryption = new Encryption();
        encryption.setAlgorithim(Algorithim.AES);
        encryption.setPadding(Padding.AES);
    }

    @Test
    public void testEncryptionCanEncryptAndDecryptWithAes() {
        String encryptionKey = "ThisIsTheEncryptionKey";

        String decryptedPassword = null;
        try {
            
            /* 08-26-2014 Issue 3365 Encryption API has changed.
             * REMOVED BEGIN
             *
            String encryptedPassword = encryption.encrypt(encryptionKey,
                    password);
            decryptedPassword = encryption.decrypt(encryptionKey,
                    encryptedPassword);
             * 08-26-2014 Issue 3365 Encryption API has changed.
             * REMOVED END 
             */
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals(password, decryptedPassword);
    }

    @Test
    public void testEncryptionCanEncryptAndDecryptWithAesAndShortKey() {
        String encryptionKey = "K";

        String decryptedPassword = null;
        try {
            /* 08-26-2014 Issue 3365 Encryption API has changed.
             * REMOVED BEGIN
             *
            String encryptedPassword = encryption.encrypt(encryptionKey,
                    password);
            decryptedPassword = encryption.decrypt(encryptionKey,
                    encryptedPassword);
             * 08-26-2014 Issue 3365 Encryption API has changed.
             * REMOVED END 
             */
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals(password, decryptedPassword);
    }

    @Test
    public void testEncryptionCanEncryptAndDecryptWithAesAndLongKey() {
        String encryptionKey = "ThisIsTheEncryptionKeyAndIsVeryVeryVeryVeryVeryLong!!!!!!!!!!!!!!!!!!!!!!!";

        String decryptedPassword = null;
        try {
            /* 08-26-2014 Issue 3365 Encryption API has changed.
             * REMOVED BEGIN
             *
            String encryptedPassword = encryption.encrypt(encryptionKey,
                    password);
            decryptedPassword = encryption.decrypt(encryptionKey,
                    encryptedPassword);
             * 08-26-2014 Issue 3365 Encryption API has changed.
             * REMOVED END 
             */
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals(password, decryptedPassword);
    }
}
