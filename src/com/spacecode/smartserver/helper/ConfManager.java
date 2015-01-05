package com.spacecode.smartserver.helper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Singleton offering access (read/write) to the configuration file of SmartServer (smartserver.conf).
 */
public class ConfManager
{
    private final Properties configProp = new Properties();

    private ConfManager()
    {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("app.properties");

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

    public static ConfManager getInstance()
    {
        return LazyHolder.INSTANCE;
    }

    public String getProperty(String key)
    {
        return configProp.getProperty(key);
    }

    public void setProperty(String key, String value)
    {
        configProp.setProperty(key, value);
    }
}