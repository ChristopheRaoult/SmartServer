package com.spacecode.smartserver;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Vincent on 30/12/13.
 */
public final class ConsoleLogger
{
    private static final Logger LOGGER = Logger.getLogger(ConsoleLogger.class.getName());

    /**
     * Add a ConsoleHandler to the Logger handlers list.
     */
    public static void initialize()
    {
        LOGGER.addHandler(new ConsoleHandler());
    }

    /**
     * Method chaining: call Logger's info() method.
     * @param message Message to be displayed as an info.
     */
    public static void info(String message)
    {
        if(LOGGER.isLoggable(Level.INFO))
        {
            LOGGER.info(message);
        }
    }

    /**
     * Method chaining: call Logger's log() method with Level.WARNING level.
     * @param message Message to be displayed as a warning.
     */
    public static void warning(String message)
    {
        if(LOGGER.isLoggable(Level.WARNING))
        {
            LOGGER.log(Level.WARNING, message);
        }
    }

    /**
     * Method chaining: call Logger's log() method with Level.WARNING level.
     * @param message   Message to be displayed as a warning.
     * @param e         Exception passed to the logger, to be displayed with the warning message.
     */
    public static void warning(String message, Exception e)
    {
        if(LOGGER.isLoggable(Level.WARNING))
        {
            LOGGER.log(Level.WARNING, message, e);
        }
    }

    /** Must not be instantiated. */
    private ConsoleLogger()
    {
    }
}
