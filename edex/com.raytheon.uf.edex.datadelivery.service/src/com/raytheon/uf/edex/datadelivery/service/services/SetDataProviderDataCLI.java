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
package com.raytheon.uf.edex.datadelivery.service.services;

import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.edex.utility.EDEXLocalizationAdapter;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.PathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.security.encryption.AESEncryptor;

/**
 * This class is used as a stand-alone application which is called from:
 * /awips2/edex/bin/centralRegistryProviderCredentials.sh. It is used to set the
 * connection and connection credentials for a data provider for Central
 * Registry Deployments.
 * <p>
 * This class will mimic the output from CAVE: Data Delivery System Management:
 * Data Provider Password dialog.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 18, 2014 3839       ccody       Initial creation
 * Feb 13, 2015 3839       dhladky     Original impl didn't write providerkey to DB.
 * </pre>
 * 
 * @author ccody
 * @version 1.0
 */

public class SetDataProviderDataCLI {
    
    public static final String INVALID_DIR = "<INVALID_DIR>";

    /** Default Encryption Algorithm */
    private static final String DefaultEncryptionAlgorithm = "AES";

    /** Default Encryption Algorithm Padding */
    private static final String DefaultEncryptionPadding = "AES/CFB8/NoPadding";
    
    /** Default DB URL **/
    private static final String DefaultDbURL = "127.0.0.1";

    /** Internal Tag Marker Start */
    private static final String TAG_START = "[[[";

    /** Internal Tag Marker End */
    private static final String TAG_END = "]]]";

    /** Connection File (name) suffix. This is added to the Provider Name. */
    private static final String CONNECTION_FILE_SUFFIX = "-connection.xml";

    /** Internal XML User Name Tag */
    private static final String USER_NAME_TAG = TAG_START + "USER_NAME"
            + TAG_END;

    /** Internal XML Password Tag */
    private static final String PASSWORD_TAG = TAG_START + "PASSWORD" + TAG_END;

    /** Internal XML Encryption Algorithm Name Tag */
    private static final String ALGORITHM_TAG = TAG_START + "ALGORITHM"
            + TAG_END;

    /** Internal XML Encryption Algorithm Padding Tag */
    private static final String PADDING_TAG = TAG_START + "PADDING" + TAG_END;

    /** Internal XML URL Tag */
    private static final String URL_TAG = TAG_START + "URL" + TAG_END;

    private static final String DATA_PROVIDER_XML_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<connection>\n"
            + "\t<userName>"
            + USER_NAME_TAG
            + "\t</userName>\n"
            + "\t<password>"
            + PASSWORD_TAG
            + "\t</password>\n"
            + "\t<encryption>\n"
            + "\t\t<algorithim>"
            + ALGORITHM_TAG
            + "</algorithim>\n"
            + "\t\t<padding>"
            + PADDING_TAG
            + "</padding>\n"
            + "\t</encryption>\n"
            + "\t<url>"
            + URL_TAG
            + "</url>\n" + "</connection>\n";

    protected class DataProviderData {
        protected String connectionDir = null;
        
        protected String dburl = null;
        
        protected String dbpass = null;

        protected String providerName = null;

        protected String providerKey = null;

        protected String encryption = null;

        protected String padding = null;

        protected String url = null;

        protected String userName = null;

        protected String password = null;

        public String getConnectionDir() {
            return (this.connectionDir);
        }

        public void setConnectionDir(String newConnectionDir) {
            this.connectionDir = newConnectionDir;
        }
        
        public void setDburl(String dburl) {
            this.dburl = dburl;
        }
        
        public String getDburl() {
            return dburl;
        }
        
        public void setDbpass(String dbpass) {
            this.dbpass = dbpass;
        }
        
        public String getDbpass() {
            return dbpass;
        }

        public String getProviderName() {
            return (this.providerName);
        }

        public void setProviderName(String newProviderName) {
            this.providerName = newProviderName;
        }

        public String getProviderKey() {
            return (this.providerKey);
        }

        public void setProviderKey(String newProviderKey) {
            this.providerKey = newProviderKey;
        }

        public String getUserName() {
            return (this.userName);
        }

        public void setUserName(String newUserName) {
            this.userName = newUserName;
        }

        public String getPassword() {
            return (this.password);
        }

        public void setPassword(String newPassword) {
            this.password = newPassword;
        }

        public String getEncryption() {
            return (this.encryption);
        }

        public void setEncryption(String newEncryption) {
            this.encryption = newEncryption;
        }

        public String getPadding() {
            return (this.padding);
        }

        public void setPadding(String newPadding) {
            this.padding = newPadding;
        }

        public String getUrl() {
            return (this.url);
        }

        public void setUrl(String newUrl) {
            this.url = newUrl;
        }

        public void print() {
            System.out.println("Provider Data:");
            System.out.println("Connection Directory: " + this.connectionDir);
            System.out.println("DB URL:          " + this.dburl);
            System.out.println("DB Password:     " + this.dbpass);
            System.out.println("Provider Name:   " + this.providerName);
            System.out.println("Provider Key:    " + this.providerKey);
            System.out.println("User Name        " + this.userName);
            System.out.println("Password         " + this.password);
            System.out.println("Encryption Alg:  " + this.encryption);
            System.out.println("Encrypt Padding: " + this.padding);
            System.out.println("URL:             " + this.url);
        }

    }

    private DataProviderData dataProviderData = null;

    private boolean isQuiet = false;

    private boolean isVerbose = false;

    /**
     * Default constructor.
     * 
     */
    public SetDataProviderDataCLI() {

    }

    /**
     * Main Processing.
     * 
     * @param argumentMap
     *            Map containing command line config options
     */
    public void process(Map<String, String> argumentMap) {
        boolean okContinue = true;

        okContinue = init(argumentMap);

        if (okContinue == true) {
            if (isQuiet == false) {
                getUserInput();
            } else {
                okContinue = checkData();
            }
        }

        if (okContinue == true) {
            writeDataToFile();
        }
    }

    /**
     * Encrypt data with providerKey
     * 
     * Taken from {@link
     * com.raytheon.uf.common.datadelivery.registry.Connection.encryptPassword()
     * * }
     * 
     * @param providerKey
     *            Encryption public key
     * @param plainText
     *            Text to encrypt
     * @return
     */
    private String encryptData(String providerKey, String plainText) {

        String encryptPassword = null;

        if (plainText != null && providerKey != null) {
            try {
                AESEncryptor encryptionProcessor = new AESEncryptor();
                encryptPassword = encryptionProcessor.encrypt(providerKey,
                        plainText);
            } catch (Exception e) {
                System.out.println("Unable to encrypt password!");
                if (isVerbose == true) {
                    e.printStackTrace();
                }
            }
        }
        return (encryptPassword);
    }

    /**
     * Attempt to build a Default Connection Directory path. Default Path is:
     * <edex
     * .home>/data/utility/<localizationContextPath>/datadelivery/connection/
     * <edex.home> = /awips2.edex/" <localizationContextPath> =
     * "common_static/<site>/"
     * 
     * @return Localization Base Path
     */
    private String buildConnectionDir() {
        String connectionDir = null;
        StringBuilder connectionDirSB = new StringBuilder();

        try {
            String edexHome = System.getProperty("edex.home");
            if ((edexHome == null) || (edexHome.isEmpty() == true)) {
                edexHome = File.separator + "awips2" + File.separator + "edex"
                        + File.separator;
            } else if (edexHome.endsWith(File.separator) == false) {
                edexHome += File.separator;
            }

            /*
             * Initialize PathManagerFactory.
             */
            /*
             * Imported foss libraries print a stack trace to stdout without
             * throwing an Exception. This section redirects stdout to prevent
             * displaying false errors to the user.
             */
            PrintStream original = System.out;
            try {
                System.setOut(new PrintStream(new OutputStream() {
                    public void write(int b) {
                        // DO NOTHING
                    }

                    public void write(byte[] b, int off, int len) {
                        // DO NOTHING
                    }

                    @SuppressWarnings("unused")
                    public void writeTo(OutputStream out) throws IOException {
                        // DO NOTHING
                    }
                }));
                PathManagerFactory.setAdapter(new EDEXLocalizationAdapter());
                PathManager pathMgr = (PathManager) PathManagerFactory
                        .getPathManager();
                LocalizationContext ctx = pathMgr.getContext(
                        LocalizationType.COMMON_STATIC, LocalizationLevel.SITE);

                System.setOut(original);

                connectionDirSB.append(edexHome);
                connectionDirSB.append("data");
                connectionDirSB.append(File.separator);
                connectionDirSB.append("utility");
                connectionDirSB.append(File.separator);
                connectionDirSB.append(ctx.toPath());
                connectionDirSB.append(File.separator);
                connectionDirSB.append("datadelivery");
                connectionDirSB.append(File.separator);
                connectionDirSB.append("connection");
                connectionDirSB.append(File.separator);
                connectionDir = connectionDirSB.toString();
            } catch (Exception ex) {
                System.setOut(original);
                System.out
                        .println("An unexpected exception was encountered building the output directory from AWIPS Localization tools.\n"
                                + "Connection xml Output directory will need to be set manually.");
                if (isVerbose == true) {
                    ex.printStackTrace();
                }
            } finally {
                System.setOut(original);
            }
            if (checkDirectory(connectionDir) == false) {
                System.out
                        .println("Unable to build Connection Directory from Localization\n"
                                + "Check Environment Variables: EDEX_HOME, CLUSTER_ID, AW_SITE_IDENTIFIER");
                connectionDir = INVALID_DIR;
            }
        } catch (Exception ex) {
            System.out
                    .println("Exception error building or accessing Directory ["
                            + connectionDir + "]. Check path and permissions.");
            if (isVerbose == true) {
                ex.printStackTrace();
            }
            connectionDir = INVALID_DIR;
        }
        return (connectionDir);
    }

    /**
     * Initialize Data Provider Data with command line args (if provided).
     * 
     * @param argumentMap
     *            Input Arg Parameter map
     * @return boolean success (true) or fail (false)
     */
    private boolean init(Map<String, String> argumentMap) {
        boolean okContinue = true;

        dataProviderData = new DataProviderData();
        boolean useOverrideDirectory = false;
        String tempString = null;
        if (argumentMap != null) {

            if (argumentMap.keySet().contains("quiet") == true) {
                isQuiet = true;
            }
            if (argumentMap.keySet().contains("verbose") == true) {
                isQuiet = false;
                isVerbose = true;
            }

            // Data Provider Name
            tempString = argumentMap.get("connectionDirectory");
            if ((tempString != null) && (tempString.isEmpty() == false)) {
                if (checkDirectory(tempString) == true) {
                    dataProviderData.setConnectionDir(tempString);
                    useOverrideDirectory = true;
                } else {
                    System.out
                            .println("Overridden Connection Directory is invalid.");
                    dataProviderData.setConnectionDir(INVALID_DIR);
                }
            }
            
            // DB URL
            tempString = argumentMap.get("dburl");
            if ((tempString != null) && (tempString.isEmpty() == false)) {
                dataProviderData.setDburl(tempString);
            } else {
                dataProviderData.setDburl(DefaultDbURL);
            }
            
            // DB Password
            tempString = argumentMap.get("dbpass");
            if ((tempString != null) && (tempString.isEmpty() == false)) {
                dataProviderData.setProviderName(tempString);
            }

            // Data Provider Name
            tempString = argumentMap.get("provider");
            if ((tempString != null) && (tempString.isEmpty() == false)) {
                dataProviderData.setProviderName(tempString);
            }

            // Data Provider Key
            tempString = argumentMap.get("key");
            if ((tempString != null) && (tempString.isEmpty() == false)) {
                dataProviderData.setProviderKey(tempString);
            }

            // Encryption Algorithm
            tempString = argumentMap.get("encrypt");
            if ((tempString != null) && (tempString.isEmpty() == false)) {
                dataProviderData.setEncryption(tempString);
            } else {
                dataProviderData.setEncryption(DefaultEncryptionAlgorithm);
            }

            // Encryption Algorithm Padding
            tempString = argumentMap.get("padding");
            if ((tempString != null) && (tempString.isEmpty() == false)) {
                dataProviderData.setPadding(tempString);
            } else {
                dataProviderData.setPadding(DefaultEncryptionPadding);
            }

            // Provider URL
            tempString = argumentMap.get("url");
            if ((tempString != null) && (tempString.isEmpty() == false)) {
                dataProviderData.setUrl(tempString);
            }

            // User Name (plain text at init)
            tempString = argumentMap.get("user");
            if ((tempString != null) && (tempString.isEmpty() == false)) {
                dataProviderData.setUserName(tempString);
            }

            // Password (plain text at init)
            tempString = argumentMap.get("password");
            if ((tempString != null) && (tempString.isEmpty() == false)) {

                dataProviderData.setPassword(tempString);
            }
        }
        if (useOverrideDirectory == false) {
            dataProviderData.setConnectionDir(buildConnectionDir());
        }

        return (okContinue);
    }

    /**
     * Retrieve user input for connection properties.
     * 
     */
    private void getUserInput() {
        String curConnectionDir = dataProviderData.getConnectionDir();
        String curDburl = dataProviderData.getDburl();
        String curDbpass = dataProviderData.getDbpass();
        String curProviderName = dataProviderData.getProviderName();
        String curProviderKey = dataProviderData.getProviderKey();
        String curEncryption = dataProviderData.getEncryption();
        String curPadding = dataProviderData.getPadding();
        String curUrl = dataProviderData.getUrl();
        String curUserName = dataProviderData.getUserName();
        String curPassword = dataProviderData.getPassword();

        System.out.println("Enter Data Provider Data:");

        if ((curConnectionDir != null)
                && (!curConnectionDir.equals(INVALID_DIR))) {
            System.out.println("Connection Directory: " + curConnectionDir);
        } else {
            curConnectionDir = null;
            boolean validDir = false;
            while (validDir == false) {
                curConnectionDir = null;
                curConnectionDir = getInput("Connection Directory",
                        curConnectionDir, false);
                if (checkDirectory(curConnectionDir)) {
                    validDir = true;
                    dataProviderData.setConnectionDir(curConnectionDir);
                }
            }
        }
        
        curDburl = getInput("Registry DB URL", curDburl, false);
        dataProviderData.setDburl(curDburl);
        
        curDbpass = getInput("Registry DB Password", curDbpass, false);
        dataProviderData.setDbpass(curDbpass);
                
        curProviderName = getInput("Provider Name", curProviderName, false);
        dataProviderData.setProviderName(curProviderName);

        curProviderKey = getInput("Provider Key", curProviderKey, false);
        dataProviderData.setProviderKey(curProviderKey);

        curUserName = getInput("User Name", curUserName, false);
        curUserName = encryptData(curProviderKey, curUserName);
        dataProviderData.setUserName(curUserName);

        curPassword = getInput("Password", curPassword, false);
        curPassword = encryptData(curProviderKey, curPassword);
        dataProviderData.setPassword(curPassword);

        curEncryption = getInput("Encryption Algorithm", curEncryption, false);
        dataProviderData.setEncryption(curEncryption);

        curPadding = getInput("Encryption Padding", curPadding, false);
        dataProviderData.setPadding(curPadding);

        curUrl = getInput("URL", curUrl, false);
        dataProviderData.setUrl(curUrl);
    }

    /**
     * Retrieve user input property from console.
     * 
     * @param propertyName
     *            Name of field to retrieve
     * @param existingValue
     *            Existing value of property (if any)
     * @param isDir
     *            Is this property a Directory path (validate)
     * @return User Input value
     */
    private String getInput(String propertyName, String existingValue,
            boolean isDir) {
        String inputValue = null;
        boolean haveValue = false;
        String prompt = "";
        Console console = System.console();

        while (haveValue == false) {
            if ((existingValue == null) || (existingValue.isEmpty() == true)) {
                prompt = propertyName + ": ";
            } else {
                prompt = propertyName + " [" + existingValue + "] : ";
            }

            inputValue = console.readLine(prompt);

            if (isDir == true) {
                haveValue = checkDirectory(inputValue);
            } else if (inputValue.isEmpty() == false) {
                haveValue = true;
            } else if ((existingValue != null)
                    && (existingValue.isEmpty() == false)) {
                inputValue = existingValue;
                haveValue = true;
            } else {
                haveValue = false;
            }
        }

        return (inputValue);
    }

    /**
     * Verify input data.
     * 
     * @return boolean success (true) or fail (false)
     */
    private boolean checkData() {
        boolean okContinue = true;

        String curCheckString = null;

        curCheckString = dataProviderData.getConnectionDir();
        if ((curCheckString != null) && (curCheckString.isEmpty() == false)) {
            okContinue = checkDirectory(curCheckString);
        } else {
            okContinue = false;
        }

        curCheckString = dataProviderData.getDburl();
        if ((curCheckString == null) || (curCheckString.isEmpty() == true)) {
            System.out.println("DB URL [" + curCheckString
                    + "]  is empty or invalid.");
            okContinue = false;
        }
        curCheckString = dataProviderData.getDbpass();
        if ((curCheckString == null) || (curCheckString.isEmpty() == true)) {
            System.out.println("DB Password [" + curCheckString
                    + "]  is empty or invalid.");
            okContinue = false;
        }
        curCheckString = dataProviderData.getProviderName();
        if ((curCheckString == null) || (curCheckString.isEmpty() == true)) {
            System.out.println("Provider Name [" + curCheckString
                    + "]  is empty or invalid.");
            okContinue = false;
        }
        curCheckString = dataProviderData.getProviderKey();
        if ((curCheckString == null) || (curCheckString.isEmpty() == true)) {
            System.out.println("Provider Key [" + curCheckString
                    + "]  is empty or invalid.");
            okContinue = false;
        }
        curCheckString = dataProviderData.getUserName();
        if ((curCheckString == null) || (curCheckString.isEmpty() == true)) {
            System.out.println("User Name [" + curCheckString
                    + "]  is empty or invalid.");
            okContinue = false;
        }
        curCheckString = dataProviderData.getPassword();
        if ((curCheckString == null) || (curCheckString.isEmpty() == true)) {
            System.out.println("Password is empty or invalid.");
            okContinue = false;
        }
        curCheckString = dataProviderData.getEncryption();
        if ((curCheckString == null) || (curCheckString.isEmpty() == true)) {
            System.out.println("Encryption Algorithm [" + curCheckString
                    + "]  is empty or invalid.");
            okContinue = false;
        }
        curCheckString = dataProviderData.getPadding();
        if ((curCheckString == null) || (curCheckString.isEmpty() == true)) {
            System.out.println("Encryption Algorithm Padding ["
                    + curCheckString + "]  is empty or invalid.");
            okContinue = false;
        }
        curCheckString = dataProviderData.getUrl();
        if ((curCheckString == null) || (curCheckString.isEmpty() == true)) {
            System.out.println("URL [" + curCheckString
                    + "]  is empty or invalid.");
            okContinue = false;
        }

        return (okContinue);
    }

    /**
     * Check to see if given directory path exists and is writable.
     * 
     * @param directory
     *            directory path
     * @return boolean success (true) or fail (false)
     */
    private boolean checkDirectory(String directory) {
        boolean okContinue = true;

        if ((directory != null) && (directory.isEmpty() == false)) {
            try {
                File file = new File(directory);
                if (file.exists() == true) {
                    okContinue = file.canWrite();
                    if (okContinue == false) {
                        System.out
                                .println("User does not have write permissions for Directory ["
                                        + directory + "]");
                    }
                } else {
                    System.out.println("Directory [" + directory
                            + "] does not exist.");
                    okContinue = false;
                }
            } catch (SecurityException se) {
                System.out.println("Unable to find or access Directory ["
                        + directory + "]. Check path and permissions.");
                if (isVerbose == true) {
                    se.printStackTrace();
                }
                okContinue = false;
            }
        } else {
            System.out.println("Unable to find or access Directory ["
                    + directory + "]. Check path and permissions.");
            okContinue = false;
        }

        return (okContinue);
    }

    /**
     * Merge Data with XML Template and write to file.
     * 
     * @return boolean success (true) or fail (false)
     */
    private boolean writeDataToFile() {

        // Integrate data into XML Template String and save provider/providerkey to DB
        String outputString = mergeDataIntoXml();

        if (isVerbose == true) {
            System.out.println("Connection File Data:\n" + outputString);
        }
        
        try {
            
            Connection connection = null;

            try {

                connection = DriverManager.getConnection(
                        "jdbc:postgresql://"+dataProviderData.getDburl()+":5432/metadata", "awips",
                        dataProviderData.getDbpass());

                PreparedStatement ps = connection
                        .prepareStatement("INSERT INTO awips.datadeliveryproviderkey VALUES (?, ?)");
                ps.setString(1, dataProviderData.getProviderName());
                ps.setString(2, dataProviderData.getProviderKey());
                ps.executeUpdate();

            } catch (SQLException e) {
                // entry already exists for this provider, update
                if (e.getMessage()
                        .contains(
                                "ERROR: duplicate key value violates unique constraint")) {
                    PreparedStatement ps2 = connection
                            .prepareStatement("UPDATE awips.datadeliveryproviderkey set providerkey = ? where providername = ?");
                    ps2.setString(1, dataProviderData.getProviderKey());
                    ps2.setString(2, dataProviderData.getProviderName());
                    ps2.executeUpdate();

                } else {

                    System.out
                            .println("Connection Failed! Check output console");
                    e.printStackTrace();
                }
            }

            if (isVerbose == true) {
                System.out.println("Saved provider/providerKey to DB:\n" + dataProviderData.getProviderName());
            }
            
        } catch (Exception e) {
            System.out.println("Failed! Couldn't store provider/providerKey record!");
            e.printStackTrace();
        }

        return (writeDataFile(outputString));
    }

    /**
     * Place User Input Data into Connection XML template.
     * 
     * @return Connection XML with embedded user data
     */
    private String mergeDataIntoXml() {

        StringBuilder sb = new StringBuilder(DATA_PROVIDER_XML_TEMPLATE);
        int start = 0;
        int end = 0;

        start = sb.indexOf(USER_NAME_TAG);
        end = start + USER_NAME_TAG.length();
        sb.replace(start, end, this.dataProviderData.getUserName());

        start = sb.indexOf(PASSWORD_TAG);
        end = start + PASSWORD_TAG.length();
        sb.replace(start, end, this.dataProviderData.getPassword());

        start = sb.indexOf(ALGORITHM_TAG);
        end = start + ALGORITHM_TAG.length();
        sb.replace(start, end, this.dataProviderData.getEncryption());

        start = sb.indexOf(PADDING_TAG);
        end = start + PADDING_TAG.length();
        sb.replace(start, end, this.dataProviderData.getPadding());

        start = sb.indexOf(URL_TAG);
        end = start + URL_TAG.length();
        sb.replace(start, end, this.dataProviderData.getUrl());

        return (sb.toString());
    }

    /**
     * Write connection output file
     * 
     * Write to:
     * <localizationDir>/datadelivery/connection/<providerName>-connection.xml";
     * 
     * @param outputString
     *            XML Connextion string to write
     * @return boolean success (true) or fail (false)
     */
    private boolean writeDataFile(String outputString) {
        boolean okContinue = true;
        String connectionDir = this.dataProviderData.getConnectionDir();
        String providerName = this.dataProviderData.getProviderName();
        String providerDataFileName = providerName + CONNECTION_FILE_SUFFIX;
        String fullFileAndPath = connectionDir + providerDataFileName;
        File file = new File(fullFileAndPath);

        if (file.exists() == true) {
            try {
                if (file.delete() == false) {
                    System.out
                            .println("The Provider Data File ["
                                    + fullFileAndPath
                                    + "] currently exists and cannot be deleted by the current user.");
                    okContinue = false;
                }
            } catch (SecurityException se) {
                System.out
                        .println("Security Exception: Error deleting Provider Data File ["
                                + fullFileAndPath + "]");
                if (isVerbose == true) {
                    se.printStackTrace();
                }
                okContinue = false;
            }
        }

        if (okContinue == true) {
            if (isQuiet == false) {
                System.out
                        .println("Writing to output file: " + fullFileAndPath);
            }

            try {
                FileWriter fw = new FileWriter(file, false);
                fw.write(outputString);
                fw.flush();
                fw.close();
            } catch (IOException ioe) {
                System.out
                        .println("IO Exceotion: Error Writing Provider Data File ["
                                + fullFileAndPath + "]");
                if (isVerbose == true) {
                    ioe.printStackTrace();
                }
                okContinue = false;
            }
        }

        return (okContinue);
    }

    /**
     * Parse input command line arguments.
     * 
     * @param args
     *            String array of command line arguments
     * @return Map of parameters
     */
    private static Map<String, String> parseArguments(String[] args) {

        Map<String, String> argumentMap = new HashMap<>();
        if ((args != null && args.length > 0)) {
            for (int i = 0; i < args.length; ++i) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    if ((args.length > (i + 1))
                            && (args[i + 1].startsWith("-") == false)) {
                        argumentMap.put(arg.substring(1), args[i + 1]);
                        ++i;
                    } else {
                        argumentMap.put(arg.substring(1), "");
                    }
                }
            }
        }
        return (argumentMap);
    }

    /**
     * Print utility help info.
     */
    private static void printHelp() {
        System.out
                .println("SetDataProviderDataCLI [-quiet -dburl <DB URL> -dbpass <Db Password> -provider <Provider Name> -key <Provider Key> -encrypt <Encryption Algorithm> -padding <Encryption Padding> -user <User Name> -password <Password>");
        System.out.println("\t-help prints out this help message");
        System.out.println("\tAll args are optional:");
        System.out
                .println("\t-quiet  Used when all parameters are specified. No prompts and only error messages are displayed.");
        System.out
                .println("\t-dburl <Db URL> specifies the URL of the Central Registry database");
        System.out
                .println("\t-dbpass <Db Password> specifies the password of the Central Registry database");
        System.out
                .println("\t-provider <Provider Name> specifies the name of the Data Provider");
        System.out
                .println("\t-key <Provider Key>  specifies the Encryption Key of the Data Provider");
        System.out
                .println("\t-encrypt <Encryption Algorithm>  specifies the Encryption Algorithm of the Data Provider");
        System.out.println("\t\tDefault is: " + DefaultEncryptionAlgorithm);
        System.out
                .println("\t-padding <Encryption Padding>  specifies the Encryption Algorithm Padding of the Data Provider");
        System.out.println("\t\tDefault is: " + DefaultEncryptionPadding);
        System.out
                .println("\t-user <User Name>  specifies the User Name for the Provider Connection");
        System.out
                .println("\t-password <Password>  specifies the Password for the Provider Connection");
        System.out
                .println("\t-connectionDirectory <Connection Directory>  Override localization and manually set a destination directory.");
    }

    /**
     * Get isVerbose flag.
     * 
     * @return isVerbose boolean
     */
    public boolean getIsVerbose() {
        return (isVerbose);
    }

    /**
     * Main.
     * 
     * @param args
     */
    public static void main(String[] args) {

        Map<String, String> argumentMap = parseArguments(args);

        if (argumentMap.get("help") != null) {
            printHelp();
            return;
        }

        SetDataProviderDataCLI setDataProviderData = new SetDataProviderDataCLI();
        
        try {
            setDataProviderData.process(argumentMap);
        } catch (Exception ex) {
            if (setDataProviderData.getIsVerbose() == true) {
                ex.printStackTrace();
            } else {
                System.out
                        .println("Exception failure. Run with -verbose for stack trace");
            }
        } 
    }
    
    

}