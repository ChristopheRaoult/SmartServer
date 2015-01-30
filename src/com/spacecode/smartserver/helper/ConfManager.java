package com.spacecode.smartserver.helper;

import com.spacecode.sdk.network.DbConfiguration;
import com.spacecode.smartserver.SmartServer;

import java.io.*;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Singleton providing access (read/write) to the configuration file of SmartServer (smartserver.properties).
 * <br/>
 * Java "properties" files are constructed following a basic syntaxe: property_name=value
 * <br/>
 * Sample of configuration file:
 * db_name=smartserver<br/>
 * db_host=localhost<br/>
 * db_dbms=mysql<br/>
 * db_port=<br/>
 * db_user=root<br/>
 * db_password=<br/>
 *
 * dev_br_master=/dev/ttyUSB1<br/>
 * dev_br_slave=/dev/ttyUSB2<br/>
 * dev_fpr_master={2FD3A356-F2FF-F243-9B0D-9243C137E641}<br/>
 * dev_fpr_slave={BFCB44E6-EB02-3142-A596-9ED337EACE19}<br/>
 * dev_temperature=on<br/>
 */
public class ConfManager
{
    private final Properties configProp = new Properties();

    private static final String CONFIG_FILENAME = "smartserver.properties";
    public static final String CONFIG_FILE = SmartServer.getWorkingDirectory() + CONFIG_FILENAME;

    public static final String DB_HOST     = "db_host";
    public static final String DB_PORT     = "db_port";
    public static final String DB_DBMS     = "db_dbms";
    public static final String DB_NAME     = "db_name";
    public static final String DB_USER     = "db_user";
    public static final String DB_PASSWORD = "db_password";

    /** Property containing serial-port name of the Master badge reader. */
    public static final String DEV_BR_MASTER    = "dev_br_master";

    /** Property containing serial-port name of the Slave badge reader. */
    public static final String DEV_BR_SLAVE     = "dev_br_slave";

    /** Property containing serial-port name of the Master fingerprint reader. */
    public static final String DEV_FPR_MASTER   = "dev_fpr_master";

    /** Property containing serial-port name of the Slave fingerprint reader. */
    public static final String DEV_FPR_SLAVE    = "dev_fpr_slave";

    /** Property containing "on" if the device is using a temperature probe, "off" otherwise. */
    public static final String DEV_TEMPERATURE  = "dev_temperature";

    private ConfManager()
    {
        try
        {
            File configFile = new File(CONFIG_FILE);

            // return true if the file was created, false otherwise
            if(configFile.createNewFile())
            {
                SmartLogger.getLogger().warning("Configuration file was not present. Now created.");
            }

            InputStreamReader isr = new InputStreamReader(new FileInputStream(CONFIG_FILE), "UTF-8");

            configProp.load(isr);
            isr.close();
        } catch (IOException ioe)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "An I/O error occurred while loading properties.", ioe);
        }
    }

    // Bill Pugh's solution for the singleton pattern
    private static class LazyHolder
    {
        private static final ConfManager INSTANCE = new ConfManager();
    }

    /**
     * @param key Name of the setting to be read.
     *
     * @return Value of the property or null if no matching property exists (unknown key).
     */
    private String getProperty(String key)
    {
        String propertyValue = configProp.getProperty(key);
        return propertyValue == null ? null : propertyValue;
    }

    /**
     * Allow updating the value of a setting.
     *
     * @param key   Name of the property to be changed.
     * @param value New value.
     *
     * @return True if the properties could be updated. False otherwise (I/O error).
     */
    private boolean setProperty(String key, String value)
    {
        configProp.setProperty(key, value == null ? "" : value);

        try
        {
            FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
            configProp.store(fos, null);
            fos.close();
            return true;
        } catch (IOException ioe)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "An I/O error occurred while updating properties.", ioe);
            return false;
        }
    }

    /** Database Host (IP address or DNS) (if set, or empty), or null if the property is not existing. */
    public static String getDbHost()
    {
        return LazyHolder.INSTANCE.getProperty(DB_HOST);
    }

    /** Database TCP port (if set, or empty), or null if the property is not existing. */
    public static String getDbPort()
    {
        return LazyHolder.INSTANCE.getProperty(DB_PORT);
    }

    /** Database Management System (if set, or empty), or null if the property is not existing. */
    public static String getDbDbms()
    {
        return LazyHolder.INSTANCE.getProperty(DB_DBMS);
    }

    /** Database name (if set, or empty), or null if the property is not existing. */
    public static String getDbName()
    {
        return LazyHolder.INSTANCE.getProperty(DB_NAME);
    }

    /** Database user's name (if set, or empty), or null if the property is not existing. */
    public static String getDbUser()
    {
        return LazyHolder.INSTANCE.getProperty(DB_USER);
    }

    /** Database user's password (if set, or empty), or null if the property is not existing. */
    public static String getDbPassword()
    {
        return LazyHolder.INSTANCE.getProperty(DB_PASSWORD);
    }

    /** Serial number of the master badge reader (if set, or empty), or null if the property is not existing. */
    public static String getDevBrMaster()
    {
        return LazyHolder.INSTANCE.getProperty(DEV_BR_MASTER);
    }

    /** Serial number of the slave badge reader (if set, or empty), or null if the property is not existing. */
    public static String getDevBrSlave()
    {
        return LazyHolder.INSTANCE.getProperty(DEV_BR_SLAVE);
    }

    /** Serial number of the master fingerprint reader (if set, or empty), or null if the property is not existing. */
    public static String getDevFprMaster()
    {
        return LazyHolder.INSTANCE.getProperty(DEV_FPR_MASTER);
    }

    /** Serial number of the slave fingerprint reader (if set, or empty), or null if the property is not existing. */
    public static String getDevFprSlave()
    {
        return LazyHolder.INSTANCE.getProperty(DEV_FPR_SLAVE);
    }

    /** @return True if the temperature module is enabled. False otherwise. */
    public static boolean isDevTemperature()
    {
        return "on".equals(LazyHolder.INSTANCE.getProperty(DEV_TEMPERATURE));
    }

    /**
     * Update the property "db_host" in the properties file.
     *
     * @param host New value for the host of the DBMS.
     *
     * @return True if the operation succeeded, false otherwise (I/O error).
     */
    public static boolean setDbHost(String host)
    {
        return LazyHolder.INSTANCE.setProperty(DB_HOST, host);
    }

    /**
     * Update the property "db_port" in the properties file.
     *
     * @param port New value for the port used by the DBMS.
     *
     * @return True if the operation succeeded, false otherwise (I/O error).
     */
    public static boolean setDbPort(String port)
    {
        return LazyHolder.INSTANCE.setProperty(DB_PORT, port);
    }

    /**
     * Update the property "db_name" in the properties file.
     *
     * @param name Name of the database to be used by SmartServer.
     *
     * @return True if the operation succeeded, false otherwise (I/O error).
     */
    public static boolean setDbName(String name)
    {
        return LazyHolder.INSTANCE.setProperty(DB_NAME, name);
    }

    /**
     * Update the property "db_user" in the properties file.
     *
     * @param username New username for the access to the DBMS.
     *
     * @return True if the operation succeeded, false otherwise (I/O error).
     */
    public static boolean setDbUser(String username)
    {
        return LazyHolder.INSTANCE.setProperty(DB_USER, username);
    }

    /**
     * Update the property "db_password" in the properties file.
     *
     * @param password New password for the access to the DBMS.
     *
     * @return True if the operation succeeded, false otherwise (I/O error).
     */
    public static boolean setDbPassword(String password)
    {
        return LazyHolder.INSTANCE.setProperty(DB_PASSWORD, password);
    }

    /**
     * Update the property "db_dbms" in the properties file.
     *
     * @param dbms DBMS to be used.
     *
     * @return True if the operation succeeded, false otherwise (I/O error).
     */
    public static boolean setDbDbms(String dbms)
    {
        return LazyHolder.INSTANCE.setProperty(DB_DBMS, dbms);
    }

    /**
     * Update the DB properties "all at once" using a DbConfiguration instance.
     *
     * @param dbConfiguration DbConfiguration containing all new settings.
     *
     * @return True if the operation succeeded, false otherwise (I/O error).
     */
    public static boolean setDbConfiguration(DbConfiguration dbConfiguration)
    {
        return  setDbHost(dbConfiguration.getHost()) &&
                setDbPort(String.valueOf(dbConfiguration.getPort())) &&
                setDbName(dbConfiguration.getName()) &&
                setDbUser(dbConfiguration.getUser()) &&
                setDbPassword(dbConfiguration.getPassword()) &&
                setDbDbms(dbConfiguration.getDbms());
    }
}