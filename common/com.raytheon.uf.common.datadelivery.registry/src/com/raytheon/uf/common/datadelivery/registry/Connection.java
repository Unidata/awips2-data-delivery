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

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.security.encryption.AESEncryptor;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Connection XML
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 17, 2011  191      dhladky   Initial creation
 * Jun 28, 2012  819      djohnson  Remove proxy information.
 * Jul 24, 2012  955      djohnson  Add copy constructor.
 * Jun 11, 2013  1763     dhladky   Added AESEncryptor type
 * Jun 17, 2013  2106     djohnson  Check for encryption to not be null,
 *                                  getPassword() must be left alone for dynamic
 *                                  serialize.
 * Aug 08, 2013  2108     mpduff    Serialize the provider key.
 * Jul 10, 2014  1717     bphillip  Changed import of relocated AESEncryptor
 *                                  class
 * Aug 19, 2014  3120     dhladky   URL remapping for properties
 * Mar 04, 2016  5388     dhladky   Changed AESEncryptor constructor
 * Dec 21. 2016    5684     tgurney     Fix URL variable replacement
 * Feb 03, 2017  6089     tjensen   Updated to support generic system properties
 *
 * </pre>
 *
 * @author dhladky
 */
@XmlRootElement(name = "connection")
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class Connection implements Serializable {
    private static final long serialVersionUID = 8223819912383198409L;

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(Connection.class);

    private AESEncryptor encryptionProcessor = null;

    public Connection() {

    }

    /**
     * Copy constructor.
     *
     * @param connection
     *            the connection to copy
     */
    public Connection(Connection connection) {
        setPassword(connection.getPassword());
        setUrl(connection.getUrl());
        setUserName(connection.getUserName());
        setEncryption(connection.getEncryption());
    }

    @XmlElement(name = "userName")
    @DynamicSerializeElement
    private String userName;

    @XmlElement(name = "password")
    @DynamicSerializeElement
    private String password;

    @DynamicSerializeElement
    private String providerKey;

    @XmlElement(name = "encryption")
    @DynamicSerializeElement
    private Encryption encryption;

    @DynamicSerializeElement
    private String url;

    @XmlElement(name = "url")
    public String getUrl() {
        return Utils.resolveSystemProperties(url);
    }

    public void setUrl(String url) {
        this.url = Utils.resolveSystemProperties(url);
    }

    public String getUserName() {
        return userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Creates encryption object
     *
     * @return
     */
    public AESEncryptor getEncryptor() {
        if (providerKey != null && encryptionProcessor == null) {
            encryptionProcessor = new AESEncryptor(providerKey);
        }

        return encryptionProcessor;
    }

    /**
     * You pass in the providerKey to the local DD client The reason for this is
     * you don't want the key and password ever stored in the same place.
     * providerKey is kept in the metadata database at the WFO. The password is
     * stored encrypted in a connection object file stored in localization. You
     *
     * @param providerKey
     * @return
     */
    public String getUnencryptedPassword() {

        if (password != null && providerKey != null) {

            try {
                return getEncryptor().decrypt(password);
            } catch (Exception e) {
                statusHandler.error("Unable to decrypt password!", e);
            }
        }

        return null;
    }

    /**
     * encrypt password with providerKey
     *
     *
     * @param providerKey
     * @return
     */
    public void encryptPassword() {

        String encryptPassword = null;

        if (password != null && providerKey != null) {

            try {
                encryptPassword = getEncryptor().encrypt(password);
                setPassword(encryptPassword);
            } catch (Exception e) {
                statusHandler.error("Unable to crypt password!", e);
            }
        }
    }

    /**
     * You pass in the providerKey to the local DD client The reason for this is
     * you don't want the key and password ever stored in the same place.
     * providerKey is kept in the metadata database at the WFO. The password is
     * stored encrypted in a connection object file stored in localization. You
     * can only decrypt when they come together in code here.
     *
     *
     * @param providerKey
     * @return
     */
    public String getUnencryptedUsername() {

        if (userName != null && providerKey != null) {

            try {
                return getEncryptor().decrypt(userName);
            } catch (Exception e) {
                statusHandler.error("Unable to decrypt userName!", e);
            }
        }

        return null;
    }

    /**
     * encrypt userName with providerKey
     *
     *
     * @param providerKey
     * @return
     */
    public void encryptUserName() {

        String encryptUserName = null;

        if (userName != null && providerKey != null) {

            try {
                encryptUserName = getEncryptor().encrypt(userName);
                setUserName(encryptUserName);
            } catch (Exception e) {
                statusHandler.error("Unable to crypt userName!", e);
            }
        }
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Encryption getEncryption() {
        return encryption;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
    }

}
