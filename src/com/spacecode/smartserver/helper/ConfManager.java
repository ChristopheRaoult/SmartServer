package com.spacecode.smartserver.helper;

import java.io.*;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Singleton providing access (read/write) to the configuration file of SmartServer (smartserver.properties).
 */
public class ConfManager
{
    private final Properties configProp = new Properties();

    private static final String CONFIG_FILE = "./smartserver.properties";

    public static final String DB_HOST     = "db_host";
    public static final String DB_PORT     = "db_port";
    public static final String DB_DBMS     = "db_dbms";
    public static final String DB_NAME     = "db_name";
    public static final String DB_USER     = "db_user";
    public static final String DB_PASSWORD = "db_password";

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

    /** @return Reference to the singleton instance of the Configuration Manager. */
    public static ConfManager getInstance()
    {
        return LazyHolder.INSTANCE;
    }

    /**
     * @param key Name of the setting to be read.
     *
     * @return Value of the property, or null if no matching property exists (unknown key).
     */
    public String getProperty(String key)
    {
        return configProp.getProperty(key);
    }

    /**
     * Allow updating the value of a setting.
     *
     * @param key   Name of the property to be changed.
     * @param value New value.
     */
    public void setProperty(String key, String value)
    {
        configProp.setProperty(key, value);

        try
        {
            FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
            configProp.store(fos, null);
            fos.close();
        } catch (IOException ioe)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "An I/O error occurred while updating properties.", ioe);
        }
    }
}