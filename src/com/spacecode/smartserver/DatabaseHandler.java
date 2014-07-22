package com.spacecode.smartserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * MySQL DB Wrapper
 */
public class DatabaseHandler
{
    private final static String DB_HOST         = "localhost:3306";
    private final static String DB_NAME         = "test";
    private final static String DB_USER         = "root";
    private final static String DB_PASSWORD     = "";

    /** Must not be instantiated. */
    private DatabaseHandler()
    {
    }

    /**
     * @return A java.sql.Connection instance, provided by the local H2 JDBC connection pool.
     */
    public static Connection getConnection()
    {
        try
        {
            return DriverManager
                    .getConnection("jdbc:MySql://"+DB_HOST+"/"+DB_NAME+"?user="+DB_USER+"&password="+DB_PASSWORD);
        } catch (SQLException e)
        {
            ConsoleLogger.warning("Unable to connect to the database.", e);
            return null;
        }
    }

    /**
     * Release the connection pool.
     */
    public static void close()
    {
        //_connectionPool.dispose();
    }
}
