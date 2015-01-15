package com.spacecode.smartserver.helper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Singleton providing access (read/write) to the configuration file of SmartServer (smartserver.conf).
 */
public class ConfManager
{
    private final Properties configProp = new Properties();

    public static final String DB_HOST     = "db_host";
    public static final String DB_PORT     = "db_port";
    public static final String DB_SGBD     = "db_sgbd";
    public static final String DB_NAME     = "db_name";
    public static final String DB_USER     = "db_user";
    public static final String DB_PASSWORD = "db_password";

    private ConfManager()
    {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("smartserver.conf");

        if(in == null)
        {
            // the conf file could not be found
            return;
        }

        try
        {
            configProp.load(in);
        } catch (IOException ioe)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Could not load the configuration file!", ioe);
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
    }
}