package com.spacecode.smartserver.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.spacecode.smartserver.ConsoleLogger;
import com.spacecode.smartserver.database.entity.*;
import com.spacecode.smartserver.database.repository.Repository;
import com.spacecode.smartserver.database.repository.RepositoryFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

    private static Map<String, Dao> _classNameToDao = new HashMap<>();
    private static Map<String, Repository> _classNameToRepository = new HashMap<>();

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

            // a connection should not stay opened more than 10 minutes
            _connectionSource.setMaxConnectionAgeMillis(10 * 60 * 1000);

            Dao<AccessTypeEntity, Integer> daoAccessType = DaoManager.createDao(_connectionSource, AccessTypeEntity.class);
            Dao<GrantTypeEntity, Integer> daoGrantType = DaoManager.createDao(_connectionSource, GrantTypeEntity.class);

            // create model (if necessary)
            TableUtils.createTableIfNotExists(_connectionSource, AuthenticationEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, DeviceConfigurationEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, FingerprintEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, GrantedAccessEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, GrantedUserEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, InventoryEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, InventoryRfidtag.class);
            TableUtils.createTableIfNotExists(_connectionSource, RfidTagEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, TemperatureMeasurementEntity.class);

            if(!daoAccessType.isTableExists())
            {
                // create table and fill with constants
                TableUtils.createTable(_connectionSource, AccessTypeEntity.class);
                daoAccessType.create(new AccessTypeEntity(com.spacecode.sdk.user.AccessType.UNDEFINED.name()));
                daoAccessType.create(new AccessTypeEntity(com.spacecode.sdk.user.AccessType.BADGE.name()));
                daoAccessType.create(new AccessTypeEntity(com.spacecode.sdk.user.AccessType.FINGERPRINT.name()));
            }

            if(!daoGrantType.isTableExists())
            {
                // create table and fill with constants
                TableUtils.createTable(_connectionSource, GrantTypeEntity.class);
                daoGrantType.create(new GrantTypeEntity(com.spacecode.sdk.user.GrantType.SLAVE.name()));
                daoGrantType.create(new GrantTypeEntity(com.spacecode.sdk.user.GrantType.MASTER.name()));
                daoGrantType.create(new GrantTypeEntity(com.spacecode.sdk.user.GrantType.ALL.name()));
            }
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

    /**
     * Get the first item of DeviceConfiguration table (should be the only one).
     * @return  Instance of DeviceConfigurationEntity class.
     */
    public static DeviceConfigurationEntity getDeviceConfiguration()
    {
        Dao<DeviceConfigurationEntity, Integer> deviceConfigDao = getDao(DeviceConfigurationEntity.class);

        if(deviceConfigDao != null)
        {
            try
            {
                return deviceConfigDao.queryBuilder().queryForFirst();
            } catch (SQLException e)
            {
                ConsoleLogger.warning("Unable to get DeviceConfiguration item.");
            }
        }

        return null;
    }

    /**
     * Provide an easy access to DAO's.
     * @param entityClass   Class instance of the Entity class to be used.
     * @return              Dao instance, or null if something went wrong (unknown entity class, SQLException...).
     */
    public static Dao getDao(Class entityClass)
    {
        Dao dao = _classNameToDao.get(entityClass.getName());

        if(dao == null)
        {
            try
            {
                _classNameToDao.put(entityClass.getName(), DaoManager.createDao(_connectionSource, entityClass));
            } catch (SQLException e)
            {
                return null;
            }
        }

        return _classNameToDao.get(entityClass.getName());
    }

    /**
     * Provide an easy access to repositories.
     * @param entityClass   Entity class to be used.
     * @return              Repository instance, or null if something went wrong  (unknown entity or null DAO).
     */
    public static Repository getRepository(Class entityClass)
    {
        String className = entityClass.getName();
        Repository repository = _classNameToRepository.get(className);

        if(repository == null)
        {
            Dao dao = getDao(entityClass);

            if(dao == null)
            {
                return null;
            }

            // RepositoryFactory can possibly return null if the class name is not known
            // then returning _classNameToRepository.get(className) will be equivalent to returning null
            _classNameToRepository.put(className, RepositoryFactory.getRepository(className, dao));
        }

        return _classNameToRepository.get(className);
    }
}
