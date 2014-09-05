package com.spacecode.smartserver.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.spacecode.sdk.user.FingerIndex;
import com.spacecode.sdk.user.GrantType;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartLogger;
import com.spacecode.smartserver.database.entity.*;
import com.spacecode.smartserver.database.repository.DeviceRepository;
import com.spacecode.smartserver.database.repository.Repository;
import com.spacecode.smartserver.database.repository.RepositoryFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

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

    private static DeviceEntity _deviceConfiguration;

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
            TableUtils.createTableIfNotExists(_connectionSource, DeviceEntity.class);
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
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to connect to the database, or initialize ORM.", sqle);
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
                _connectionSource = null;
            } catch (SQLException sqle)
            {
                SmartLogger.getLogger().log(Level.WARNING, "Unable to close connection pool.", sqle);
            }
        }
    }

    /**
     * Look for a configuration for the current device.
     * @return Instance of DeviceEntity class.
     */
    public static DeviceEntity getDeviceConfiguration()
    {
        if(_deviceConfiguration != null)
        {
            return _deviceConfiguration;
        }

        Repository deviceRepository = getRepository(DeviceEntity.class);

        if(!(deviceRepository instanceof DeviceRepository))
        {
            return null;
        }

        Object result = deviceRepository.getEntityBy(DeviceEntity.SERIAL_NUMBER, DeviceHandler.getDevice().getSerialNumber());
        _deviceConfiguration = result == null ? null : (DeviceEntity) result;

        return _deviceConfiguration;
    }

    /**
     * Provide an easy access to DAO's.
     * @param entityClass   Class instance of the Entity class to be used.
     * @return              Dao instance, or null if something went wrong (unknown entity class, SQLException...).
     */
    public static Dao getDao(Class entityClass)
    {
        if(!_classNameToDao.containsKey(entityClass.getName()))
        {
            try
            {
                _classNameToDao.put(entityClass.getName(), DaoManager.createDao(_connectionSource, entityClass));
            } catch (SQLException sqle)
            {
                SmartLogger.getLogger().log(Level.WARNING, "Unable to get requested DAO instance.", sqle);
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
     * Load known granted users (from database) in the UsersService users cache.
     * @param deviceConfig  DeviceEntity instance to be used to look for data.
     */
    public static boolean loadGrantedUsers(DeviceEntity deviceConfig)
    {
        Dao daoUser = getDao(GrantedUserEntity.class);

        // 0: username, 1: badge number, 2: grant type, 3: finger index, 4: finger template
        String columns = "gue.username, gue.badge_number, gte.type, fpe.finger_index, fpe.template";

        // raw query to get all users with their access type (on this device) and their fingerprints
        StringBuilder sb = new StringBuilder("SELECT ").append(columns).append(" ");
        sb.append("FROM ").append(GrantedUserEntity.TABLE_NAME).append(" gue ");
        // join all fingerprints
        sb.append("LEFT JOIN ").append(FingerprintEntity.TABLE_NAME).append(" fpe ");
        sb.append("ON gue.").append(GrantedUserEntity.ID).append(" = ");
        sb.append("fpe.").append(FingerprintEntity.GRANTED_USER_ID).append(" ");
        // join all granted accesses
        sb.append("LEFT JOIN ").append(GrantedAccessEntity.TABLE_NAME).append(" gae ");
        sb.append("ON gue.").append(GrantedUserEntity.ID).append(" = ");
        sb.append("gae.").append(GrantedAccessEntity.GRANTED_USER_ID).append(" ");
        // join grant types to granted accesses
        sb.append("LEFT JOIN ").append(GrantTypeEntity.TABLE_NAME).append(" gte ");
        sb.append("ON gae.").append(GrantedAccessEntity.GRANT_TYPE_ID).append(" = ");
        sb.append("gte.").append(GrantTypeEntity.ID).append(" ");
        // for the current device only
        sb.append("WHERE gae.").append(GrantedAccessEntity.DEVICE_ID).append(" = ")
                .append(deviceConfig.getId());

        Map<String, GrantedUser> usernameToUser = new HashMap<>();

        try
        {
            // get one line per user having a granted access on this device
            // one more line (with repeated user information) for each user's fingerprint.
            GenericRawResults results = daoUser.queryRaw(sb.toString());

            Iterator<String[]> iterator = results.iterator();

            // fill the users hashmap with results from Raw SQL query
            while(iterator.hasNext())
            {
                String[] result = iterator.next();

                GrantedUser user = usernameToUser.get(result[0]);

                // first, add the user if it's not known yet
                if(user == null)
                {
                    user = new GrantedUser(result[0], GrantType.valueOf(result[2]), result[1]);
                    usernameToUser.put(result[0], user);
                }

                // if there isn't any fingerprint [finger_index is null], go on
                if(result[3] == null || user == null)
                {
                    // user should not be null at this stage, but test anyway
                    continue;
                }

                // parse string value (from db) to int. Exception is caught as IllegalArgumentException.
                int fingerIndexVal = Integer.parseInt(result[3]);
                FingerIndex fingerIndex = FingerIndex.getValueByIndex(fingerIndexVal);

                // if finger index from db is valid and in the expected range
                if(fingerIndex != null)
                {
                    // add this new fingerprint template to the user
                    user.setFingerprintTemplate(fingerIndex, result[4]);
                }
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

        DeviceHandler.getDevice().getUsersService().addUsers(usernameToUser.values());
        return true;
    }
}
