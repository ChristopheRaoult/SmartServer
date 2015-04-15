package com.spacecode.smartserver.helper;

import com.spacecode.smartserver.SmartServer;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.MissingResourceException;
import java.util.logging.*;

/**
 * Thin internal wrapper for default Logger.
 */
public final class SmartLogger extends Logger
{
    private static final String LOG_FILENAME = "smartserver.log";
    private static final String LOG_FILE = SmartServer.getWorkingDirectory() + LOG_FILENAME;

    // singleton reference
    private static final SmartLogger LOGGER = new SmartLogger();

    /**
     * Protected method to construct a logger for a named subsystem.
     *
     * The logger will be initially configured with a null Level
     * and with useParentHandlers set to true.
     *
     * @throws MissingResourceException if the resourceBundleName is non-null and
     *                                  no corresponding resource can be found.
     */
    protected SmartLogger() throws MissingResourceException
    {
        super("SmartLogger", null);
    }

    /**
     * Add a Logging Handler to the Logger handlers list.
     */
    public static void initialize()
    {
        try
        {
            LogManager.getLogManager().reset();
            
            ConsoleHandler consoleHandler = new SmartConsoleHandler();
            FileHandler fileHandler = new FileHandler(LOG_FILE, true);

            ShortFormatter formatter = new ShortFormatter();

            fileHandler.setLevel(Level.WARNING);
            fileHandler.setFormatter(formatter);

            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(formatter);
            
            LOGGER.addHandler(fileHandler);
            LOGGER.addHandler(consoleHandler);
        } catch (IOException | SecurityException e)
        {
            LOGGER.log(Level.SEVERE, "Unable to initialize SmartLogger.", e);
        }
    }

    /** @return Singleton reference. */
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

    /**
     * Custom ConsoleHandler used to redirect logs to STDOUT.
     */
    static class SmartConsoleHandler extends ConsoleHandler
    {
        public SmartConsoleHandler()
        {
            super.setOutputStream(System.out);
        }
    }
}

