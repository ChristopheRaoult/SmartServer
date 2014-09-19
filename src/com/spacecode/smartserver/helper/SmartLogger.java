package com.spacecode.smartserver.helper;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.MissingResourceException;
import java.util.logging.*;

/**
 * Wrapper for default Logger
 */
public final class SmartLogger extends Logger
{
    // singleton reference
    private static final SmartLogger LOGGER = new SmartLogger("SmartLogger", null);

    /**
     * Protected method to construct a logger for a named subsystem.
     *
     * The logger will be initially configured with a null Level
     * and with useParentHandlers set to true.
     *
     * @param name               A name for the logger.  This should
     *                           be a dot-separated name and should normally
     *                           be based on the package name or class name
     *                           of the subsystem, such as java.net
     *                           or javax.swing.  It may be null for anonymous Loggers.
     * @param resourceBundleName name of ResourceBundle to be used for localizing
     *                           messages for this logger.  May be null if none
     *                           of the messages require localization.
     * @throws MissingResourceException if the resourceBundleName is non-null and
     *                                  no corresponding resource can be found.
     */
    protected SmartLogger(String name, String resourceBundleName) throws MissingResourceException
    {
        super(name, resourceBundleName);
    }

    /**
     * Add a Logging Handler to the Logger handlers list.
     */
    public static void initialize()
    {
        try
        {
            ConsoleHandler consoleHandler = new ConsoleHandler();
            FileHandler fileHandler = new FileHandler("./smartserver.log");

            ShortFormatter formatter = new ShortFormatter();

            fileHandler.setLevel(Level.WARNING);
            fileHandler.setFormatter(formatter);

            consoleHandler.setFormatter(formatter);

            LOGGER.addHandler(fileHandler);
            LOGGER.addHandler(consoleHandler);
        } catch (IOException ioe)
        {
            LOGGER.log(Level.SEVERE, "Unable to initialize log file.", ioe);
        }
    }

    /**
     * @return Singleton reference.
     */
    public static SmartLogger getLogger()
    {
        return LOGGER;
    }

    /**
     * Custom Formatter for logs display.
     */
    static class ShortFormatter extends Formatter
    {
        private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        /**
         * Format the message/Thrown instance from LogRecord as we please.
         * @param record    LogRecord containing logging message and (eventually) a Thrown instance.
         * @return          A string of the formatted message to be displayed.
         */
        @Override
        public String format(LogRecord record)
        {
            StringBuilder builder = new StringBuilder();
            builder.append(df.format(new Date(record.getMillis()))).append(" ");
            builder.append("[").append(record.getLevel()).append("] ");
            builder.append(formatMessage(record));
            builder.append("\n");

            if(record.getThrown() != null)
            {
                builder.append(record.getThrown().getMessage());
                builder.append("\n");
            }

            return builder.toString();
        }
    }
}

