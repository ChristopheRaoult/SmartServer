package com.spacecode.smartserver.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.spacecode.sdk.user.UserGrant;
import com.spacecode.smartserver.ConsoleLogger;
import com.spacecode.smartserver.database.entity.*;

import java.sql.SQLException;

/**
 * ORMLite DB Wrapper
 */
public class DatabaseHandler
{
    private static final String DB_HOST             = "localhost:3306";
    private static final String DB_NAME             = "test";
    private static final String DB_USER             = "root";
    private static final String DB_PASSWORD         = "";
    private static final String CONNECTION_STRING   = "jdbc:mysql://"+DB_HOST+"/"+DB_NAME+"?user="+DB_USER+"&password="+DB_PASSWORD;

    private static JdbcPooledConnectionSource           _connectionSource;
    private static Dao<AccessType, Integer>             _daoAccessType;
    private static Dao<DeviceConfiguration, Integer>    _daoDeviceConfiguration;
    private static Dao<Authentication, Integer>         _daoAuthentication;
    private static Dao<Fingerprint, Integer>            _daoFingerprint;
    private static Dao<GrantedAccess, Integer>          _daoGrantedAccess;
    private static Dao<GrantedUser, Integer>            _daoGrantedUser;
    private static Dao<GrantType, Integer>              _daoGrantType;
    private static Dao<Inventory, Integer>              _daoInventory;
    private static Dao<InventoryRfidtag, Integer>       _daoInventoryRfidTag;
    private static Dao<RfidTag, Integer>                _daoRfidTag;
    private static Dao<Temperature, Integer>            _daoTemperature;

    /** Must not be instantiated. */
    private DatabaseHandler()
    {
    }

    /**
     * Initialize Connection Pool and create Schema (if not created).
     * @return JdbcPooledConnectionSource instance if succeeds, null otherwise.
     */
    public static JdbcPooledConnectionSource initializeDatabase()
    {
        try
        {
            _connectionSource =
                    new JdbcPooledConnectionSource(CONNECTION_STRING);

            _connectionSource.setMaxConnectionAgeMillis(5 * 60 * 1000);

            _daoAccessType          = DaoManager.createDao(_connectionSource, AccessType.class);
            _daoAuthentication      = DaoManager.createDao(_connectionSource, Authentication.class);
            _daoDeviceConfiguration = DaoManager.createDao(_connectionSource, DeviceConfiguration.class);
            _daoFingerprint         = DaoManager.createDao(_connectionSource, Fingerprint.class);
            _daoGrantedAccess       = DaoManager.createDao(_connectionSource, GrantedAccess.class);
            _daoGrantedUser         = DaoManager.createDao(_connectionSource, GrantedUser.class);
            _daoGrantType           = DaoManager.createDao(_connectionSource, GrantType.class);
            _daoInventory           = DaoManager.createDao(_connectionSource, Inventory.class);
            _daoInventoryRfidTag    = DaoManager.createDao(_connectionSource, InventoryRfidtag.class);
            _daoRfidTag             = DaoManager.createDao(_connectionSource, RfidTag.class);
            _daoTemperature         = DaoManager.createDao(_connectionSource, Temperature.class);

            if(!_daoAccessType.isTableExists())
            {
                TableUtils.createTable(_connectionSource, AccessType.class);
                _daoAccessType.create(new AccessType(com.spacecode.sdk.user.AccessType.UNDEFINED.name()));
                _daoAccessType.create(new AccessType(com.spacecode.sdk.user.AccessType.BADGE.name()));
                _daoAccessType.create(new AccessType(com.spacecode.sdk.user.AccessType.FINGERPRINT.name()));
            }

            TableUtils.createTableIfNotExists(_connectionSource, Authentication.class);
            TableUtils.createTableIfNotExists(_connectionSource, DeviceConfiguration.class);
            TableUtils.createTableIfNotExists(_connectionSource, Fingerprint.class);
            TableUtils.createTableIfNotExists(_connectionSource, GrantedAccess.class);
            TableUtils.createTableIfNotExists(_connectionSource, GrantedUser.class);

            if(!_daoGrantType.isTableExists())
            {
                TableUtils.createTable(_connectionSource, GrantType.class);
                _daoGrantType.create(new GrantType(UserGrant.SLAVE.name()));
                _daoGrantType.create(new GrantType(UserGrant.MASTER.name()));
                _daoGrantType.create(new GrantType(UserGrant.ALL.name()));
            }

            TableUtils.createTableIfNotExists(_connectionSource, Inventory.class);
            TableUtils.createTableIfNotExists(_connectionSource, InventoryRfidtag.class);
            TableUtils.createTableIfNotExists(_connectionSource, RfidTag.class);
            TableUtils.createTableIfNotExists(_connectionSource, Temperature.class);
        } catch (SQLException sqle)
        {
            ConsoleLogger.warning("Unable to connect to the database, or initialize ORM.", sqle);
            return null;
        }

        return _connectionSource;
    }

    /**
     * @return A JdbcPooledConnectionSource (ORMLite) instance for DAO's.
     */
    public static JdbcPooledConnectionSource getConnectionSource()
    {
        return _connectionSource;
    }

    /**
     * Allow closing the connection pool.
     */
    public static void close()
    {
        if(_connectionSource != null)
        {
            try
            {
                _connectionSource.close();
            } catch (SQLException sqle)
            {
                ConsoleLogger.warning("Unable to close connection pool.", sqle);
            }
        }
    }

    public static Dao<DeviceConfiguration, Integer> getDaoDeviceConfiguration()
    {
        return _daoDeviceConfiguration;
    }
}
