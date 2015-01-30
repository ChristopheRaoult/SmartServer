 package com.spacecode.smartserver.database;

 import com.j256.ormlite.dao.Dao;
 import com.j256.ormlite.dao.DaoManager;
 import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
 import com.j256.ormlite.table.TableUtils;
 import com.spacecode.sdk.network.DbConfiguration;
 import com.spacecode.sdk.network.alert.AlertType;
 import com.spacecode.sdk.user.data.AccessType;
 import com.spacecode.sdk.user.data.GrantType;
 import com.spacecode.smartserver.database.entity.*;
 import com.spacecode.smartserver.database.repository.Repository;
 import com.spacecode.smartserver.database.repository.RepositoryFactory;
 import com.spacecode.smartserver.helper.ConfManager;
 import com.spacecode.smartserver.helper.DeviceHandler;
 import com.spacecode.smartserver.helper.SmartLogger;

 import java.sql.SQLException;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.logging.Level;

/**
 * ORMLite DB Wrapper. Handle interactions with Database.
 */
public class DbManager
{
    // Default configuration. NOT USED if a database is set in smartserver.properties
    private static final String DB_HOST             = "localhost";
    private static final String DB_PORT             = "3306";
    private static final String DB_NAME             = "smartserver";
    // TODO: Do not use root user in production
    private static final String DB_USER             = "root";
    private static final String DB_PASSWORD         = "";

    private static final String CONNECTION_STRING   =
            "jdbc:mysql://"+DB_HOST+":"+DB_PORT+"/"+DB_NAME+"?user="+DB_USER+"&password="+DB_PASSWORD;

    // JDBC connection string used to connect to the database
    private static JdbcPooledConnectionSource _pooledConnectionSrc;

    // DAO's cache
    private static final Map<String, Dao> _classNameToDao = new HashMap<>();

    // Repositories cache
    private static final Map<String, Repository> _classNameToRepository = new HashMap<>();

    // DeviceEntity instance corresponding (by serial number) to the plugged device
    private static DeviceEntity _deviceEntity;

    /** Must not be instantiated. */
    private DbManager()
    {
    }

    /** @return A connection string built with settings from smartserver.properties (or default configuration). */
    private static String getConnectionString()
    {
        String confDbHost = ConfManager.getDbHost();
        String confDbPort = ConfManager.getDbPort();
        String confDbDbms = ConfManager.getDbDbms();
        String confDbName = ConfManager.getDbName();
        String confDbUser = ConfManager.getDbUser();

        // all parameters are mandatory [except Port and Password], otherwise the conf file is not valid
        if( confDbHost == null || confDbDbms == null || confDbName == null || confDbUser == null)
        {
            // default config [embedded mysql]
            return CONNECTION_STRING;
        }

        // used to trigger the DBMS default port if no port is set
        confDbPort = confDbPort == null ? "" : confDbPort;

        switch(confDbDbms)
        {
            case DbConfiguration.SQL_Server:
                confDbPort = "".equals(confDbPort) ? "1433" : confDbPort;
                return String.format("jdbc:%s://%s:%s;databaseName=%s;", confDbDbms, confDbHost, confDbPort, confDbName);

            case DbConfiguration.PostgreSQL:
                confDbPort = "".equals(confDbPort) ? "5432" : confDbPort;
                return String.format("jdbc:%s://%s:%s/%s", confDbDbms, confDbHost, confDbPort, confDbName);

            case DbConfiguration.MySQL:
                confDbPort = "".equals(confDbPort) ? "3306" : confDbPort;
                return String.format("jdbc:%s://%s:%s/%s", confDbDbms, confDbHost, confDbPort, confDbName);

            default:
                return CONNECTION_STRING;
        }
    }

    /**
     * Initialize Connection Pool and create the Schema (if not created).
     *
     * @return True if the connection/initialization succeeded. False otherwise.
     */
    public static boolean initializeDatabase()
    {
        try
        {
            // get connection string, from settings in smartserver.properties OR use the default conf
            String connectionString = getConnectionString();

            if(CONNECTION_STRING.equals(connectionString))
            {
                // if the default conf is used, do not provide user/password as it already contains them
                _pooledConnectionSrc = new JdbcPooledConnectionSource(CONNECTION_STRING);
                SmartLogger.getLogger().warning("Using embedded MySQL database.");
            }

            else
            {
                // dbUser cannot be null (if it was, the default configuration would have been chosen), but anyway...
                String dbUser = ConfManager.getDbUser() == null ? "" : ConfManager.getDbUser();
                String dbPassword = ConfManager.getDbPassword() == null ? "" : ConfManager.getDbPassword();

                _pooledConnectionSrc =
                        new JdbcPooledConnectionSource(connectionString, dbUser, dbPassword);
                SmartLogger.getLogger().info("Connecting to database: " + connectionString);
            }

            // a connection should not stay opened more than 10 minutes
            _pooledConnectionSrc.setMaxConnectionAgeMillis(10 * 60 * 1000);

            createModelIfNotExists();
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to connect to the database, or initialize ORM.", sqle);
            return false;
        }

        return true;
    }

    /** @return A JdbcPooledConnectionSource (ORMLite) instance for DAO's. */
    public static JdbcPooledConnectionSource getConnectionSource()
    {
        return _pooledConnectionSrc;
    }

    /** Allow closing the connection pool. */
    public static void close()
    {
        if(_pooledConnectionSrc != null)
        {
            try
            {
                _pooledConnectionSrc.close();
                _pooledConnectionSrc = null;
            } catch (SQLException sqle)
            {
                SmartLogger.getLogger().log(Level.WARNING, "Unable to close connection pool.", sqle);
            }
        }
    }

    /**
     * Try to create all tables and insert constant values.
     *
     * @throws SQLException If any SQL error occurs.
     */
    private static void createModelIfNotExists() throws SQLException
    {
        List<Class<? extends Entity>> entityClasses = Arrays.asList(
                AccessTypeEntity.class,
                AlertEntity.class,
                AlertHistoryEntity.class,
                AlertTemperatureEntity.class,
                AlertTypeEntity.class,
                AuthenticationEntity.class,
                DeviceEntity.class,
                FingerprintEntity.class,
                GrantedAccessEntity.class,
                GrantTypeEntity.class,
                UserEntity.class,
                InventoryEntity.class,
                InventoryRfidTag.class,
                RfidTagEntity.class,
                SmtpServerEntity.class,
                TemperatureMeasurementEntity.class
        );

        // for each Entity class
        for(Class entityClass : entityClasses)
        {
            String className = entityClass.getName();

            // create the corresponding DAO and put it in the DAO's cache
            Dao<Entity, Integer> dao = DaoManager.createDao(_pooledConnectionSrc, entityClass);
            _classNameToDao.put(className, dao);

            // create the corresponding Repository and put it in the Repositories cache
            _classNameToRepository.put(className, RepositoryFactory.getRepository(className, dao));

            // create the table if it does not exist
            if(!dao.isTableExists())
            {
                // use dao.isTableExists() first, as createTableIfNotExists() fails with postgreSQL 9.1 (OrmLite 4.48)
                TableUtils.createTableIfNotExists(_pooledConnectionSrc, entityClass);

                // check if there is any operation to perform on this class
                onTableCreated(entityClass, dao);
            }
        }
    }

    /**
     * Called when a table is first created in the database, allows (for instance) inserting constant values.
     *
     * @param entityClass   Class instance of the Entity.
     * @param dao           Corresponding DAO, used to perform any operation.
     */
    private static void onTableCreated(Class entityClass, Dao<Entity, Integer> dao) throws SQLException
    {
        if(entityClass.getName().equals(AccessTypeEntity.class.getName()))
        {
            dao.create(new AccessTypeEntity(AccessType.UNDEFINED.name()));
            dao.create(new AccessTypeEntity(AccessType.BADGE.name()));
            dao.create(new AccessTypeEntity(AccessType.FINGERPRINT.name()));
        }

        else if(entityClass.getName().equals(AlertTypeEntity.class.getName()))
        {
            dao.create(new AlertTypeEntity(AlertType.DEVICE_DISCONNECTED.name()));
            dao.create(new AlertTypeEntity(AlertType.DOOR_OPEN_DELAY.name()));
            dao.create(new AlertTypeEntity(AlertType.TEMPERATURE.name()));
            dao.create(new AlertTypeEntity(AlertType.THIEF_FINGER.name()));
        }

        else if(entityClass.getName().equals(GrantTypeEntity.class.getName()))
        {
            dao.create(new GrantTypeEntity(GrantType.UNDEFINED.name()));
            dao.create(new GrantTypeEntity(GrantType.SLAVE.name()));
            dao.create(new GrantTypeEntity(GrantType.MASTER.name()));
            dao.create(new GrantTypeEntity(GrantType.ALL.name()));
        }
    }

    /**
     * Provide an access to DAO's. If the desired DAO does not exist, create it and put it in the cache.
     *
     * @param entityClass   Class instance of the Entity class to be used.
     *
     * @return              Dao instance, or null if something went wrong (unknown entity class, SQLException...).
     */
    public static <E extends Entity> Dao<E, Integer> getDao(Class<E> entityClass)
    {
        if(!_classNameToDao.containsKey(entityClass.getName()))
        {
            try
            {
                _classNameToDao.put(entityClass.getName(), DaoManager.createDao(_pooledConnectionSrc, entityClass));
            } catch (SQLException sqle)
            {
                SmartLogger.getLogger().log(Level.WARNING, "Unable to get requested DAO instance.", sqle);
            }
        }

        return _classNameToDao.get(entityClass.getName());
    }

    /**
     * Provide an access to repositories. If the desired repository does not exist, create it and put it in the cache.
     *
     * @param entityClass   Entity class to be used.
     *
     * @return              Repository instance, or null if something went wrong  (unknown entity or null DAO).
     */
    public static <E extends Entity> Repository<E> getRepository(Class<E> entityClass)
    {
        String className = entityClass.getName();

        if(!_classNameToRepository.containsKey(className))
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

    /**
     * Look for a configuration for the current device.
     *
     * @return Instance of DeviceEntity class.
     */
    public static DeviceEntity getDevEntity()
    {
        if(_deviceEntity != null)
        {
            return _deviceEntity;
        }

        DeviceEntity result = getRepository(DeviceEntity.class).getEntityBy(
                DeviceEntity.SERIAL_NUMBER,
                DeviceHandler.getDevice().getSerialNumber());
        _deviceEntity = result == null ? null : result;

        return _deviceEntity;
    }

    /**
     * Create a DeviceEntity for the given RFID serial number, if none is found.
     *
     * @param devSerialNumber RFID serial number (Serial number of the device).
     *
     * @return True if the configuration exists, or if it has been created and loaded. False otherwise.
     */
    public static boolean createDeviceIfNotExists(String devSerialNumber)
    {
        // either a DeviceEntity is available => return true, otherwise try to create and load it => return result
        return  getDevEntity() != null ||
                getRepository(DeviceEntity.class).insert(new DeviceEntity(devSerialNumber)) && getDevEntity() != null;

    }
}
