package com.spacecode.smartserver.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.table.TableUtils;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.network.alert.Alert;
import com.spacecode.sdk.network.alert.AlertTemperature;
import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.sdk.network.alert.SmtpServer;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.UsersService;
import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.sdk.user.data.FingerIndex;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.database.entity.*;
import com.spacecode.smartserver.database.repository.*;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * ORMLite DB Wrapper. Handle common interactions with Database.
 */
public class DatabaseHandler
{
    // TODO: values to be read from a local .conf file (just like a real *nix system service)
    private static final String DB_HOST             = "localhost:3306";
    private static final String DB_NAME             = "smartserver";
    private static final String DB_USER             = "root";
    private static final String DB_PASSWORD         = "";
    private static final String CONNECTION_STRING   = "jdbc:mysql://"+DB_HOST+"/"+DB_NAME+"?user="+DB_USER+"&password="+DB_PASSWORD;

    private static JdbcPooledConnectionSource       _connectionSource;

    private static Map<String, Dao> _classNameToDao = new HashMap<>();
    private static Map<String, Repository> _classNameToRepository = new HashMap<>();

    private static DeviceEntity _deviceConfiguration;

    /** Must not be instantiated. */
    private DatabaseHandler()
    {
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
            _connectionSource =
                    new JdbcPooledConnectionSource(CONNECTION_STRING);

            // a connection should not stay opened more than 10 minutes
            _connectionSource.setMaxConnectionAgeMillis(10 * 60 * 1000);

            // get DAO's of following entities in order to initialize constant values if necessary
            Dao<AccessTypeEntity, Integer> daoAccessType = DaoManager.createDao(_connectionSource, AccessTypeEntity.class);
            Dao<GrantTypeEntity, Integer> daoGrantType = DaoManager.createDao(_connectionSource, GrantTypeEntity.class);
            Dao<AlertTypeEntity, Integer> daoAlertType = DaoManager.createDao(_connectionSource, AlertTypeEntity.class);

            // create model (if necessary)
            TableUtils.createTableIfNotExists(_connectionSource, AlertEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, AlertHistoryEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, AlertTemperatureEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, AuthenticationEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, DeviceEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, FingerprintEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, GrantedAccessEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, UserEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, InventoryEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, InventoryRfidTag.class);
            TableUtils.createTableIfNotExists(_connectionSource, RfidTagEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, SmtpServerEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, TemperatureMeasurementEntity.class);

            if(!daoAccessType.isTableExists())
            {
                // create table and fill with constants
                TableUtils.createTable(_connectionSource, AccessTypeEntity.class);
                daoAccessType.create(new AccessTypeEntity(AccessType.UNDEFINED.name()));
                daoAccessType.create(new AccessTypeEntity(AccessType.BADGE.name()));
                daoAccessType.create(new AccessTypeEntity(AccessType.FINGERPRINT.name()));
            }

            if(!daoGrantType.isTableExists())
            {
                // create table and fill with constants
                TableUtils.createTable(_connectionSource, GrantTypeEntity.class);
                daoGrantType.create(new GrantTypeEntity(GrantType.UNDEFINED.name()));
                daoGrantType.create(new GrantTypeEntity(GrantType.SLAVE.name()));
                daoGrantType.create(new GrantTypeEntity(GrantType.MASTER.name()));
                daoGrantType.create(new GrantTypeEntity(GrantType.ALL.name()));
            }

            if(!daoAlertType.isTableExists())
            {
                // create table and fill with constants
                TableUtils.createTable(_connectionSource, AlertTypeEntity.class);
                daoAlertType.create(new AlertTypeEntity(AlertType.DEVICE_DISCONNECTED.name()));
                daoAlertType.create(new AlertTypeEntity(AlertType.DOOR_OPEN_DELAY.name()));
                daoAlertType.create(new AlertTypeEntity(AlertType.TEMPERATURE.name()));
                daoAlertType.create(new AlertTypeEntity(AlertType.THIEF_FINGER.name()));
            }
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to connect to the database, or initialize ORM.", sqle);
            return null;
        }

        return _connectionSource;
    }

    /** @return A JdbcPooledConnectionSource (ORMLite) instance for DAO's. */
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
     * Provide an easy access to DAO's.
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

    /** @return SmtpServer set for the current device, if any, null otherwise. */
    public static SmtpServerEntity getSmtpServerConfiguration()
    {
        return getRepository(SmtpServerEntity.class)
                .getEntityBy(SmtpServerEntity.DEVICE_ID, getDeviceConfiguration().getId());
    }

    /**
     * Load known granted users (from database) in the UsersService users cache.
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

        // raw query to get all users with their access type (on this device) and their fingerprints
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

        Map<String, User> usernameToUser = new HashMap<>();
        UsersService usersService = DeviceHandler.getDevice().getUsersService();

        try
        {
            // get one line per user having a granted access on this device
            // one more line (with repeated user information) for each user's fingerprint.
            GenericRawResults results = daoUser.queryRaw(sb.toString());

            // fill the users hashmap with results from Raw SQL query
            for (String[] result : (Iterable<String[]>) results)
            {
                User user = usernameToUser.get(result[0]);

                // first, add the user if it's not known yet
                if (user == null)
                {
                    user = new User(result[0], GrantType.valueOf(result[2]), result[1]);
                    usernameToUser.put(result[0], user);

                    usersService.addUser(user);
                }

                // if there isn't any fingerprint [finger_index is null], go on
                if (result[3] == null)
                {
                    continue;
                }

                // parse string value (from db) to int. Exception is caught as IllegalArgumentException.
                int fingerIndexVal = Integer.parseInt(result[3]);
                FingerIndex fingerIndex = FingerIndex.getValueByIndex(fingerIndexVal);

                // if finger index from db is valid and in the expected range
                if (fingerIndex != null)
                {
                    // add this new fingerprint template to the user
                    usersService.updateFingerprintTemplate(user.getUsername(), fingerIndex, result[4]);
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

        return true;
    }

    /**
     * Load information from the last inventory stored in database.
     *
     * Allow getting information of the last inventory in order to be able to:
     * <ul>
     *     <li>Query for last inventory even when device has just started.</li>
     *     <li>Define which tags should be considered as added, present or removed for the next inventory.</li>
     * </ul>
     *
     * @return Inventory instance, or null if something went wrong (SQL Exception).
     */
    public static Inventory getLastStoredInventory()
    {
        return ((InventoryRepository) getRepository(InventoryEntity.class)).getLastInventory(_deviceConfiguration);
    }

    /**
     * Process the data persistence:
     * UserEntity, Fingerprint(s), GrantedAccessEntity.
     *
     * @param newUser   Instance of GrantedUser (SDK) to be added to database.
     *
     * @return          True if success, false otherwise (username already used, SQLException, etc).
     */
    public static boolean persistUser(final User newUser)
    {
        try
        {
            TransactionManager.callInTransaction(getConnectionSource(), new PersistUserCallable(newUser));
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Error while persisting new user.", sqle);
            return false;
        }

        return true;
    }

    /**
     * Start the user deletion process (user + fingerprints).
     *
     * @param username  Name of to-be-deleted user.
     *
     * @return          True if successful, false otherwise (unknown user, SQLException...).
     */
    public static boolean deleteUser(String username)
    {
        Repository<UserEntity> userRepo = getRepository(UserEntity.class);

        UserEntity gue = userRepo.getEntityBy(UserEntity.USERNAME, username);

        return gue != null && userRepo.delete(gue);
    }

    /**
     * Get FingerprintRepository and start data persistence process.
     *
     * @param username      User to be attached to the fingerprint entity.
     * @param fingerIndex   Finger index (int) to be written in new row.
     * @param fpTpl         Base64 encoded fingerprint template.
     *
     * @return              True if success, false otherwise (user unknown in DB, SQLException...).
     */
    public static boolean persistFingerprint(String username, int fingerIndex, String fpTpl)
    {
        Repository<UserEntity> userRepo = getRepository(UserEntity.class);
        Repository<FingerprintEntity> fpRepo   = getRepository(FingerprintEntity.class);

        UserEntity gue = userRepo.getEntityBy(UserEntity.USERNAME, username);

        return gue != null && fpRepo.update(new FingerprintEntity(gue, fingerIndex, fpTpl));
    }

    /**
     * Delete a given fingerprint (username + finger index) from database.
     *
     * @param username  User attached to the fingerprint.
     * @param index     FingerIndex's index of the fingerprint.
     *
     * @return          True if successful, false otherwise.
     */
    public static boolean deleteFingerprint(String username, int index)
    {
        Repository<FingerprintEntity> fpRepo = getRepository(FingerprintEntity.class);
        Repository<UserEntity> userRepo = getRepository(UserEntity.class);

        UserEntity gue = userRepo.getEntityBy(UserEntity.USERNAME, username);

        if(gue == null)
        {
            return false;
        }

        FingerprintEntity fpe = ((FingerprintRepository)fpRepo).getFingerprint(gue, index);

        return fpe != null && fpRepo.delete(fpe);
    }

    /**
     * Persist new badge number in database.
     *
     * @param username      User to be updated.
     * @param badgeNumber   New badge number.
     *
     * @return              True if success, false otherwise (user not known, SQLException, etc).
     */
    public static boolean persistBadgeNumber(String username, String badgeNumber)
    {
        Repository userRepo = getRepository(UserEntity.class);

        return ((GrantedUserRepository)userRepo).updateBadge(username, badgeNumber);
    }

    /**
     * Persist new "thief finger" index in database.
     *
     * @param username      User to be updated.
     * @param fingerIndex   New "thief finger" index.
     *
     * @return              True if success, false otherwise (user not known, SQLException, etc).
     */
    public static boolean persistThiefFingerIndex(String username, Integer fingerIndex)
    {
        Repository userRepo = getRepository(UserEntity.class);

        return ((GrantedUserRepository)userRepo).updateThiefFingerIndex(username, fingerIndex);
    }

    /**
     * Persist new permission in database.
     *
     * @param username      User to be updated.
     * @param grantType     New permission.
     *
     * @return              True if success, false otherwise (user not known, SQLException, etc).
     */
    public static boolean persistPermission(String username, GrantType grantType)
    {
        Repository<UserEntity> userRepo = getRepository(UserEntity.class);
        Repository grantTypeRepo = getRepository(GrantTypeEntity.class);

        UserEntity gue = userRepo.getEntityBy(UserEntity.USERNAME, username);

        if(gue == null)
        {
            return false;
        }

        GrantTypeEntity gte = ((GrantTypeRepository) grantTypeRepo).fromGrantType(grantType);

        if(gte == null)
        {
            SmartLogger.getLogger().severe("Persisting permission: unknown grant type " + grantType + ".");
            return false;
        }

        GrantedAccessEntity gae = new GrantedAccessEntity(gue,
                getDeviceConfiguration(),
                gte
        );

        Iterator<GrantedAccessEntity> it = gue.getGrantedAccesses().iterator();

        // remove any previous permission on this device
        while(it.hasNext())
        {
            if(it.next().getDevice().getId() == getDeviceConfiguration().getId())
            {
                it.remove();
                break;
            }
        }

        // add the new permission
        return gue.getGrantedAccesses().add(gae);
    }

    /**
     * Start the alert deletion process (AlertEntity, plus AlertTemperatureEntity if required).
     *
     * @param alert Alert [SDK] instance to be deleted from Database.
     *
     * @return      True if successful, false otherwise (unknown alert, SQLException...).
     */
    public static boolean deleteAlert(Alert alert)
    {
        Repository<AlertEntity> aRepo = getRepository(AlertEntity.class);

        AlertEntity gue = aRepo.getEntityById(alert.getId());

        return gue != null && aRepo.delete(gue);
    }

    /**
     * On successful authentication (event raised by Device), persist (log) information in database.
     *
     * @param grantedUser   GrantedUser instance who successfully authenticated.
     * @param accessType    AccessType enum value (fingerprint, badge...).
     *
     * @return True if operation was successful, false otherwise.
     */
    public static boolean persistAuthentication(User grantedUser, AccessType accessType)
    {
        Repository<UserEntity> userRepo = getRepository(UserEntity.class);
        Repository accessTypeRepo = getRepository(AccessTypeEntity.class);
        Repository<AuthenticationEntity> authenticationRepo = getRepository(AuthenticationEntity.class);

        UserEntity gue = userRepo.getEntityBy(UserEntity.USERNAME, grantedUser.getUsername());

        if(gue == null)
        {
            // user does not exist in database
            return false;
        }

        AccessTypeEntity ate = ((AccessTypeRepository)accessTypeRepo).fromAccessType(accessType);

        if(ate == null)
        {
            SmartLogger.getLogger().severe("Persisting authentication: unknown access type " + accessType + ".");
            return false;
        }

        return authenticationRepo.insert(new AuthenticationEntity(getDeviceConfiguration(), gue, ate));
    }

    /**
     * Persist new inventory in the database, including related RfidTagEntities
     * (many-to-many relationship through InventoryRfidTag).
     *
     * @param lastInventory Provided by RfidDevice instance. Inventory made when last scan completed.
     *
     * @return  True if operation succeeded, false otherwise.
     */
    public static boolean persistInventory(Inventory lastInventory)
    {
        try
        {
            TransactionManager.callInTransaction(getConnectionSource(), new PersistInventoryCallable(lastInventory));
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Error while persisting new inventory.", sqle);
            return false;
        }

        return true;
    }

    /**
     * Insert or Update AlertEntity (from Alert instance [SDK]) in database.
     *
     * @param alert Alert instance coming from SDK client.
     *
     * @return      True if successful, false otherwise.
     */
    public static boolean persistAlert(Alert alert)
    {
        Repository<AlertEntity> aRepo = getRepository(AlertEntity.class);
        AlertTypeRepository aTypeRepo = (AlertTypeRepository) getRepository(AlertTypeEntity.class);

        AlertTypeEntity ate = aTypeRepo.fromAlertType(alert.getType());

        if(ate == null)
        {
            return false;
        }

        // create an AlertEntity from the given Alert. All data is copied (including Id).
        AlertEntity newAlertEntity = new AlertEntity(ate, getDeviceConfiguration(), alert);

        // TODO: check that To/Cc/Bcc, Subject/Content can't allow an SQL injection

        if(alert.getId() == 0)
        {
            if(!aRepo.insert(newAlertEntity))
            {
                // if we fail inserting the new alert
                return false;
            }
        }

        else
        {
            if(!aRepo.update(newAlertEntity))
            {
                // if we fail updating the alert
                return false;
            }
        }

        if(alert.getType() != AlertType.TEMPERATURE)
        {
            // successfully inserted/updated the alert and we don't need to insert/update an AlertTemperature...
            return true;
        }

        // get the AlertTemperature [SDK] instance
        if(!(alert instanceof AlertTemperature))
        {
            SmartLogger.getLogger().severe("Trying to persist an Alert as an AlertTemperature whereas it is not.");
            return false;
        }

        AlertTemperature alertTemperature = (AlertTemperature) alert;

        AlertTemperatureRepository aTempRepo =
                (AlertTemperatureRepository) DatabaseHandler.getRepository(AlertTemperatureEntity.class);

        // if the Alert is already known: update the attached AlertTemperature
        if(alert.getId() != 0)
        {
            AlertTemperatureEntity atEntity = aTempRepo.getEntityBy(AlertTemperatureEntity.ALERT_ID, alert.getId());
            atEntity.setTemperatureMin(alertTemperature.getTemperatureMin());
            atEntity.setTemperatureMax(alertTemperature.getTemperatureMax());
            return aTempRepo.update(atEntity);
        }

        // else create a new AlertTemperature
        else
        {
            return aTempRepo.insert(new AlertTemperatureEntity(newAlertEntity, alertTemperature));
        }
    }

    /**
     * Persist new SMTP server information for the current device.
     *
     * @param address       Server address.
     * @param port          Server TCP port number.
     * @param username      Username to connect to the SMTP server.
     * @param password      Password to connect to the SMTP server.
     * @param sslEnabled    If true, will use SSL for authentication.
     *
     * @return              True if operation succeeded, false otherwise.
     */
    public static boolean persistSmtpServer(String address, int port, String username,
                                            String password, boolean sslEnabled)
    {
        Repository<SmtpServerEntity> ssRepo = getRepository(SmtpServerEntity.class);

        SmtpServerEntity currentSse = getSmtpServerConfiguration();

        // TODO: make sure the given data doesn't allow a SQL injection

        if(currentSse == null)
        {
            return ssRepo.insert(new SmtpServerEntity(address, port, username,
                    password, sslEnabled));
        }

        else
        {
            currentSse.updateFrom(new SmtpServer(address, port, username,
                    password, sslEnabled));
            return ssRepo.update(currentSse);
        }
    }

    /**
     * Callable subclass called when persisting a new user (SQL transaction)
     */
    private static class PersistUserCallable implements Callable<Void>
    {
        private final User _newUser;

        private PersistUserCallable(User newUser)
        {
            _newUser = newUser;
        }

        @Override
        public Void call() throws Exception
        {
            // First, get & create the user
            Repository<UserEntity> userRepo = getRepository(UserEntity.class);

            // try to get the user if he already exists (by username)
            UserEntity gue = userRepo.getEntityBy(UserEntity.USERNAME, _newUser.getUsername());

            // if he doesn't exist
            if(gue == null)
            {
                // create it
                gue = new UserEntity(_newUser);

                // try to insert him in DB
                if (!userRepo.insert(gue))
                {
                    throw new SQLException("Failed when inserting new user.");
                }

                Repository<FingerprintEntity> fpRepo = getRepository(FingerprintEntity.class);

                // add his fingerprints
                for(FingerIndex index : _newUser.getEnrolledFingersIndexes())
                {
                    if(!fpRepo.insert(
                            new FingerprintEntity(gue, index.getIndex(), _newUser.getFingerprintTemplate(index))
                    ))
                    {
                        throw new SQLException("Failed when inserting new user's fingerprints.");
                    }
                }
            }

            // get GrantTypeEntity instance corresponding to newUser grant type
            Repository grantTypeRepo = getRepository(GrantTypeEntity.class);
            GrantTypeEntity gte = ((GrantTypeRepository) grantTypeRepo).fromGrantType(_newUser.getPermission());

            if(gte == null)
            {
                throw new SQLException("Persisting user: unknown grant type "+ _newUser.getPermission() +".");
            }

            // create & persist fingerprints and access
            Repository<GrantedAccessEntity> gaRepo = getRepository(GrantedAccessEntity.class);

            GrantedAccessEntity gae = new GrantedAccessEntity(gue, getDeviceConfiguration(), gte);

            // add the access to current device (if any)
            if(!gaRepo.insert(gae))
            {
                throw new SQLException("Failed when inserting new granted access.");
            }

            return null;
        }
    }

    /**
     * Callable subclass called when persisting a new inventory (SQL transaction)
     */
    private static class PersistInventoryCallable implements Callable<Void>
    {
        private final Inventory _inventory;

        private PersistInventoryCallable(Inventory inventory)
        {
            _inventory = inventory;
        }

        @Override
        public Void call() throws Exception
        {
            // First, get & create the inventory
            Repository<InventoryEntity> inventoryRepo = getRepository(InventoryEntity.class);
            Repository<UserEntity> userRepo = getRepository(UserEntity.class);
            Repository accessTypeRepo = getRepository(AccessTypeEntity.class);
            Repository rfidTagRepo = getRepository(RfidTagEntity.class);
            Repository<InventoryRfidTag> inventTagRepo = getRepository(InventoryRfidTag.class);

            UserEntity gue = null;

            if(_inventory.getUsername() != null && !"".equals(_inventory.getUsername().trim()))
            {
                gue = userRepo.getEntityBy(UserEntity.USERNAME, _inventory.getUsername());
            }

            AccessTypeEntity ate = ((AccessTypeRepository) accessTypeRepo).fromAccessType(_inventory.getAccessType());

            if(ate == null)
            {
                // GrantedUser can be null for an inventory (if manually started) but AccessType can't.
                throw new SQLException("Invalid access type. Unable to insert Inventory in database.");
            }

            InventoryEntity ie = new InventoryEntity(_inventory, getDeviceConfiguration(), gue, ate);

            if(!inventoryRepo.insert(ie))
            {
                throw new SQLException("Failed when inserting new Inventory.");
            }

            Map<String, RfidTagEntity> uidToEntity = new HashMap<>();
            List<String> allUids = new ArrayList<>(_inventory.getTagsAll());
            allUids.addAll(_inventory.getTagsRemoved());

            // browse all UID's (tags added, present, removed) to fill the map with entities
            for(String tagUid : allUids)
            {
                RfidTagEntity rte = ((RfidTagRepository) rfidTagRepo).createIfNotExists(tagUid);

                if(rte == null)
                {
                    throw new SQLException("Unable to createIfNotExists a tag in database.");
                }

                uidToEntity.put(tagUid, rte);
            }

            // create the many-to-many relationship between the Inventory table and the RfidTag table
            List<InventoryRfidTag> inventoryRfidTags = new ArrayList<>();

            // get the matrix containing the axis number where each tag has been detected for the last time
            Map<String, Byte> tagToAxis = DeviceHandler.getDevice().getTagToAxis();

            int shelveNbr;

            for(String tagUid : _inventory.getTagsAdded())
            {
                shelveNbr = tagToAxis.get(tagUid) == null ? 0 : tagToAxis.get(tagUid);
                inventoryRfidTags.add(new InventoryRfidTag(ie, uidToEntity.get(tagUid), 1, shelveNbr));
            }

            for(String tagUid : _inventory.getTagsPresent())
            {
                shelveNbr = tagToAxis.get(tagUid) == null ? 0 : tagToAxis.get(tagUid);
                inventoryRfidTags.add(new InventoryRfidTag(ie, uidToEntity.get(tagUid), 0, shelveNbr));
            }

            for(String tagUid : _inventory.getTagsRemoved())
            {
                shelveNbr = tagToAxis.get(tagUid) == null ? 0 : tagToAxis.get(tagUid);
                inventoryRfidTags.add(new InventoryRfidTag(ie, uidToEntity.get(tagUid), -1, shelveNbr));
            }

            if(!inventTagRepo.insert(inventoryRfidTags))
            {
                throw new SQLException("Unable to insert all tags and movements from new inventory in database.");
            }

            return null;
        }
    }
}
