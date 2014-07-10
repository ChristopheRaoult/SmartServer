package com.spacecode.smartserver;

import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Vincent on 02/01/14.
 */

/**
 * Provide access to the H2 JDBC Connection Pool in order for SmartServer to process SQL queries without connecting/disconnecting for each operation.
 */
public class DatabaseHandler
{
    private static JdbcConnectionPool _connectionPool = JdbcConnectionPool.create("jdbc:h2:./smartserver", "SmartServer", "_Sp4c3c0d3_sm4rts3rv3r_");

    /**
     * @return A java.sql.Connection instance, provided by the local H2 JDBC connection pool.
     */
    public static Connection getConnection()
    {
        try
        {
            return _connectionPool.getConnection();
        } catch (SQLException sqle)
        {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Unable to get a connection from H2 JDBC connection pool.", sqle);
            return null;
        }
    }

    /**
     * Release the connection pool.
     */
    public static void close()
    {
        _connectionPool.dispose();
    }

    /** Must not be instantiated. */
    private DatabaseHandler()
    {
    }
}
