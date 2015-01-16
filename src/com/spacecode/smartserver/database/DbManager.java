 package com.spacecode.smartserver.database;

 import com.j256.ormlite.dao.Dao;
 import com.j256.ormlite.dao.DaoManager;
 import com.j256.ormlite.dao.GenericRawResults;
 import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
 import com.j256.ormlite.table.TableUtils;
 import com.spacecode.sdk.network.alert.AlertType;
 import com.spacecode.sdk.user.User;
 import com.spacecode.sdk.user.UsersService;
 import com.spacecode.sdk.user.data.AccessType;
 import com.spacecode.sdk.user.data.FingerIndex;
 import com.spacecode.sdk.user.data.GrantType;
 import com.spacecode.smartserver.database.entity.*;
 import com.spacecode.smartserver.database.repository.Repository;
 import com.spacecode.smartserver.database.repository.RepositoryFactory;
 import com.spacecode.smartserver.helper.ConfManager;
 import com.spacecode.smartserver.helper.DeviceHandler;
 import com.spacecode.smartserver.helper.SmartLogger;

 import java.sql.SQLException;
 import java.util.*;
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

    // Instance of ConfManager, handling the DB settings
    private static final ConfManager _confManager = ConfManager.getInstance();

    // DAO's cache
    private static final Map<String, Dao> _classNameToDao = new HashMap<>();

    // Repositories cache
    private static final Map<String, Repository> _classNameToRepository = new HashMap<>();

    // DeviceEntity instance corresponding (by serial number) to the plugged device
    private static DeviceEntity _deviceConfiguration;

    /** Must not be instantiated. */
    private DbManager()
    {
    }

    /** @return A connection string built with settings from smartserver.properties (or default configuration). */
    private static String getConnectionString()
    {
        String confDbHost = _confManager.getProperty(ConfManager.DB_HOST);
        String confDbPort = _confManager.getProperty(ConfManager.DB_PORT);
        String confDbDbms = _confManager.getProperty(ConfManager.DB_DBMS);
        String confDbName = _confManager.getProperty(ConfManager.DB_NAME);
        String confDbUser = _confManager.getProperty(ConfManager.DB_USER);
        String confDbPassword = _confManager.getProperty(ConfManager.DB_PASSWORD);

        // all parameters are mandatory [except Port], otherwise the conf file is not valid
        if( confDbHost == null || confDbDbms == null || confDbName == null ||
            confDbUser == null || confDbPassword == null)
        {
            // default config [embedded mysql]
            return CONNECTION_STRING;
        }

        // used to trigger the DBMS default port if no port is set
        confDbPort = confDbPort == null ? "" : confDbPort;

        switch(confDbDbms)
        {
            case "sqlserver":
                confDbPort = "".equals(confDbPort) ? "1433" : confDbPort;
                return String.format("jdbc:%s://%s:%s;databaseName=%s;", confDbDbms, confDbHost, confDbPort, confDbName);

            case "postgresql":
                confDbPort = "".equals(confDbPort) ? "5432" : confDbPort;
                return String.format("jdbc:%s://%s:%s/%s", confDbDbms, confDbHost, confDbPort, confDbName);

            case "mysql":
                confDbPort = "".equals(confDbPort) ? "3306" : confDbPort;
                return String.format("jdbc:%s://%s:%s/%s", confDbDbms, confDbHost, confDbPort, confDbName);

            default:
                return CONNECTION_STRING;
        }
    }

    /** @return Empty string if the setting is not set, or the value of the setting User. */
    private static String getConnectionUser()
    {
        String confDbUser = _confManager.getProperty(ConfManager.DB_USER);
        return confDbUser == null ? "" : confDbUser;
    }

    /** @return Empty string if the setting is not set, or the value of the setting Password. */
    private static String getConnectionPassword()
    {
        String confDbPassword = _confManager.getProperty(ConfManager.DB_PASSWORD);
        return confDbPassword == null ? "" : confDbPassword;
    }

    /**
     * Initialize Connection Pool and create Schema (if not created).
     *
     * @return JdbcPooledConnectionSource instance if succeeds, null otherwise.
     */
    public static JdbcPooledConnectionSource initializeDatabase()
    {
        try
        {
            // get connection string, from settings in smartserver.properties OR default conf
            String connectionString = getConnectionString();

            if(CONNECTION_STRING.equals(connectionString))
            {
                // if the default conf is used, do not provide user/password as it already contains them
                _pooledConnectionSrc = new JdbcPooledConnectionSource(CONNECTION_STRING);
                SmartLogger.getLogger().warning("Using embedded MySQL database.");
            }

            else
            {
                _pooledConnectionSrc =
                        new JdbcPooledConnectionSource(connectionString, getConnectionUser(), getConnectionPassword());
                SmartLogger.getLogger().warning("Connecting to database: " + connectionString);
            }

            // a connection should not stay opened more than 10 minutes
            _pooledConnectionSrc.setMaxConnectionAgeMillis(10 * 60 * 1000);

            createModelIfNotExists();
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to connect to the database, or initialize ORM.", sqle);
            return null;
        }

        return _pooledConnectionSrc;
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
    public static DeviceEntity getDeviceConfiguration()
    {
        if(_deviceConfiguration != null)
        {
            return _deviceConfiguration;
        }

        Repository deviceRepository = getRepository(DeviceEntity.class);

        Object result = deviceRepository.getEntityBy(DeviceEntity.SERIAL_NUMBER, DeviceHandler.getDevice().getSerialNumber());
        _deviceConfiguration = result == null ? null : (DeviceEntity) result;

        return _deviceConfiguration;
    }

    /**
     * Load known granted users (from database) in the UsersService users cache.
     * TODO: Try to get rid of RAW SQL
     *
     * @return True if operation succeeded, false otherwise.
     */
    public static boolean loadGrantedUsers()
    {
        DeviceEntity deviceConfig = getDeviceConfiguration();

        if(deviceConfig == null)
        {
            return false;
        }

        Dao daoUser = getDao(UserEntity.class);

        // 0: username, 1: badge number, 2: grant type, 3: finger index, 4: finger template
        String columns = "gue.username, gue.badge_number, gte.type, fpe.finger_index, fpe.template";

        // raw query to get all users with their fingerprints and their access type (on this device)
        StringBuilder sb = new StringBuilder("SELECT ").append(columns).append(" ");
        sb.append("FROM ").append(UserEntity.TABLE_NAME).append(" gue ");
        // join all fingerprints
        sb.append("LEFT JOIN ").append(FingerprintEntity.TABLE_NAME).append(" fpe ");
        sb.append("ON gue.").append(UserEntity.ID).append(" = ");
        sb.append("fpe.").append(FingerprintEntity.GRANTED_USER_ID).append(" ");
        // join all granted accesses
        sb.append("LEFT JOIN ").append(GrantedAccessEntity.TABLE_NAME).append(" gae ");
        sb.append("ON gue.").append(UserEntity.ID).append(" = ");
        sb.append("gae.").append(GrantedAccessEntity.GRANTED_USER_ID).append(" ");
        // join grant types to granted accesses
        sb.append("LEFT JOIN ").append(GrantTypeEntity.TABLE_NAME).append(" gte ");
        sb.append("ON gae.").append(GrantedAccessEntity.GRANT_TYPE_ID).append(" = ");
        sb.append("gte.").append(GrantTypeEntity.ID).append(" ");
        // for the current device only
        sb.append("WHERE gae.").append(GrantedAccessEntity.DEVICE_ID).append(" = ")
                .append(deviceConfig.getId());

        // username to temporary User instance
        Map<String, User> usernameToUser = new HashMap<>();

        // username to map of fingerprints
        Map<String, Map<FingerIndex, String>> usernameToFingersMap = new HashMap<>();

        UsersService usersService = DeviceHandler.getDevice().getUsersService();

        try
        {
            // get one line per user having a granted access on this device
            // one more line (with repeated user information) for each user's fingerprint.
            GenericRawResults results = daoUser.queryRaw(sb.toString());

            // fill the Maps with results from Raw SQL query
            for (String[] result : (Iterable<String[]>) results)
            {
                User user = usernameToUser.get(result[0]);

                // first, create the user if he's not known yet
                if (user == null)
                {
                    user = new User(result[0], GrantType.valueOf(result[2]), result[1]);
                    usernameToUser.put(result[0], user);
                }

                // if there isn't any fingerprint [finger_index is null], go on
                if (result[3] == null)
                {
                    continue;
                }

                Map<FingerIndex, String> fingersMap = usernameToFingersMap.get(user.getUsername());

                if(fingersMap == null)
                {
                    fingersMap = new EnumMap<>(FingerIndex.class);
                    usernameToFingersMap.put(user.getUsername(), fingersMap);
                }

                // parse string value (from db) to int. Exception is caught as IllegalArgumentException.
                int fingerIndexVal = Integer.parseInt(result[3]);
                FingerIndex fingerIndex = FingerIndex.getValueByIndex(fingerIndexVal);

                // if finger index from db is valid and in the expected range
                if (fingerIndex != null)
                {
                    // add this new fingerprint template to the user
                    fingersMap.put(fingerIndex, result[4]);
                }
            }

            // Maps are filled, now, create the users and register them
            for(Map.Entry<String, User> e : usernameToUser.entrySet())
            {
                String username = e.getKey();
                Map<FingerIndex, String> fingersMap = usernameToFingersMap.get(username);
                User tmpUser = e.getValue();
                User newUser = new User(username, tmpUser.getPermission(), tmpUser.getBadgeNumber(), fingersMap);

                usersService.addUser(newUser);
            }

            results.close();
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to load users from database.", sqle);
            return false;
        } catch(IllegalArgumentException iae)
        {
            // invalid fingerIndex or grantType from database
            SmartLogger.getLogger().log(Level.SEVERE, "Loading users process failed because of corrupted data.", iae);
            return false;
        }

        return true;
    }
}
