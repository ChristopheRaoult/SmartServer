package com.spacecode.smartserver.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.table.TableUtils;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.user.AccessType;
import com.spacecode.sdk.user.FingerIndex;
import com.spacecode.sdk.user.GrantType;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartLogger;
import com.spacecode.smartserver.database.entity.*;
import com.spacecode.smartserver.database.repository.*;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * ORMLite DB Wrapper
 */
public class DatabaseHandler
{
    // TODO: values to be read from a local .conf file (just like a real system service)
    private static final String DB_HOST             = "localhost:3306";
    private static final String DB_NAME             = "test";
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

            Dao<AccessTypeEntity, Integer> daoAccessType = DaoManager.createDao(_connectionSource, AccessTypeEntity.class);
            Dao<GrantTypeEntity, Integer> daoGrantType = DaoManager.createDao(_connectionSource, GrantTypeEntity.class);

            // create model (if necessary)
            TableUtils.createTableIfNotExists(_connectionSource, AuthenticationEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, DeviceEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, FingerprintEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, GrantedAccessEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, GrantedUserEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, InventoryEntity.class);
            TableUtils.createTableIfNotExists(_connectionSource, InventoryRfidTag.class);
            TableUtils.createTableIfNotExists(_connectionSource, RfidTagEntity.class);
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
     *
     * @param entityClass   Class instance of the Entity class to be used.
     *
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
     *
     * @param entityClass   Entity class to be used.
     *
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

    public static Inventory getLastStoredInventory()
    {
        Dao daoUser = getDao(InventoryEntity.class);

        // 0: username, 1: badge number, 2: grant type, 3: finger index, 4: finger template
        String columns = "inv.device_id, inv.granteduser_id, inv.accesstype_id, inv.total_added, inv.total_present, inv.total_removed, rt.uid, irt.movement, inv.created_at";

        // raw query to get all columns for the last inventory
        StringBuilder sb = new StringBuilder("SELECT ").append(columns).append(" ");
        sb.append("FROM ").append(InventoryEntity.TABLE_NAME).append(" inv ");
        // join through the many-to-many relationship
        sb.append("LEFT JOIN ").append(InventoryRfidTag.TABLE_NAME).append(" irt ");
        sb.append("ON inv.").append(InventoryEntity.ID).append(" = ");
        sb.append("irt.").append(InventoryRfidTag.INVENTORY_ID).append(" ");
        // join all tags
        sb.append("LEFT JOIN ").append(RfidTagEntity.TABLE_NAME).append(" rt ");
        sb.append("ON rt.").append(RfidTagEntity.ID).append(" = ");
        sb.append("irt.").append(InventoryRfidTag.RFID_TAG_ID).append(" ");
        // for the current device only
        sb.append("WHERE inv.").append(InventoryEntity.CREATED_AT)
                .append(" =(SELECT MAX(").append(InventoryEntity.CREATED_AT).append(")")
                .append(" FROM ").append(InventoryEntity.TABLE_NAME).append(")");

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
                // TODO : parse the result to build a proper Inventory instance, and give it to the Device as a "current inventory" on loading.
            }

            results.close();
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to load users from database.", sqle);
            return null;
        }

        return null;
    }

    /**
     * Process the data persistence:
     * GrantedUserEntity, Fingerprint(s), GrantedAccessEntity.
     *
     * @param newUser   Instance of GrantedUser (SDK) to be added to database.
     *
     * @return          True if success, false otherwise (username already used, SQLException, etc).
     */
    public static boolean persistUser(final GrantedUser newUser)
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
        Repository userRepo = getRepository(GrantedUserEntity.class);

        if(!(userRepo instanceof GrantedUserRepository))
        {
            // not supposed to happen as the repositories map is filled automatically
            return false;
        }

        GrantedUserEntity gue = ((GrantedUserRepository) userRepo).getByUsername(username);

        if(gue == null)
        {
            // user unknown
            return false;
        }

        return userRepo.delete(gue);
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
        Repository userRepo = getRepository(GrantedUserEntity.class);
        Repository fpRepo   = getRepository(FingerprintEntity.class);

        if(!(userRepo instanceof GrantedUserRepository))
        {
            return false;
        }

        GrantedUserEntity gue = ((GrantedUserRepository) userRepo).getByUsername(username);

        if(gue == null)
        {
            return false;
        }

        if(!(fpRepo instanceof FingerprintRepository))
        {
            return false;
        }

        return fpRepo.update(new FingerprintEntity(gue, fingerIndex, fpTpl));
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
        Repository fpRepo = getRepository(FingerprintEntity.class);

        if(!(fpRepo instanceof FingerprintRepository))
        {
            // not supposed to happen as the repositories map is filled automatically
            return false;
        }

        Repository userRepo = getRepository(GrantedUserEntity.class);

        if(!(userRepo instanceof GrantedUserRepository))
        {
            // not supposed to happen as the repositories map is filled automatically
            return false;
        }

        GrantedUserEntity gue = ((GrantedUserRepository) userRepo).getByUsername(username);

        if(gue == null)
        {
            return false;
        }

        FingerprintEntity fpe = ((FingerprintRepository)fpRepo).getFingerprint(gue, index);

        if(fpe == null)
        {
            return false;
        }

        return fpRepo.delete(fpe);
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
        Repository userRepo = getRepository(GrantedUserEntity.class);

        if(!(userRepo instanceof GrantedUserRepository))
        {
            // not supposed to happen as the repositories map is filled automatically
            return false;
        }

        return ((GrantedUserRepository)userRepo).updateBadge(username, badgeNumber);
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
        Repository userRepo = getRepository(GrantedUserEntity.class);
        Repository accessRepo = getRepository(GrantedAccessEntity.class);
        Repository grantTypeRepo = getRepository(GrantTypeEntity.class);

        if( !(userRepo instanceof GrantedUserRepository) ||
            !(grantTypeRepo instanceof GrantTypeRepository) ||
            !(accessRepo instanceof GrantedAccessRepository))
        {
            // not supposed to happen as the repositories map is filled automatically
            return false;
        }

        GrantedUserEntity gue = ((GrantedUserRepository)userRepo).getByUsername(username);

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

        while(it.hasNext())
        {
            if(it.next().getDevice().getId() == getDeviceConfiguration().getId())
            {
                it.remove();
                break;
            }
        }

        return gue.getGrantedAccesses().add(gae);
    }

    /**
     * On successful authentication (event raised by Device), persist (log) information in database.
     *
     * @param grantedUser   GrantedUser instance who successfully authenticated.
     * @param accessType    AccessType enum value (fingerprint, badge...).
     *
     * @return True if operation was successful, false otherwise.
     */
    public static boolean persistAuthentication(GrantedUser grantedUser, AccessType accessType)
    {
        Repository userRepo = getRepository(GrantedUserEntity.class);
        Repository accessTypeRepo = getRepository(AccessTypeEntity.class);
        Repository authenticationRepo = getRepository(AuthenticationEntity.class);

        if( !(userRepo instanceof GrantedUserRepository) ||
            !(accessTypeRepo instanceof AccessTypeRepository))
        {
            // not supposed to happen as the repositories map is filled automatically
            return false;
        }

        GrantedUserEntity gue = ((GrantedUserRepository)userRepo).getByUsername(grantedUser.getUsername());

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
     * Callable subclass called when persisting a new user (SQL transaction)
     */
    private static class PersistUserCallable implements Callable<Void>
    {
        private final GrantedUser _newUser;

        private PersistUserCallable(GrantedUser newUser)
        {
            _newUser = newUser;
        }

        @Override
        public Void call() throws Exception
        {
            // First, get & create the user
            Repository userRepo = getRepository(GrantedUserEntity.class);

            GrantedUserEntity gue = new GrantedUserEntity(_newUser);

            if(!userRepo.insert(gue))
            {
                throw new SQLException("Failed when inserting new user.");
            }

            // get GrantTypeEntity instance corresponding to newUser grant type
            Repository grantTypeRepo = getRepository(GrantTypeEntity.class);
            GrantTypeEntity gte = ((GrantTypeRepository) grantTypeRepo)
                    .fromGrantType(_newUser.getPermission());

            if(gte == null)
            {
                throw new SQLException("Persisting user: unknown grant type "+ _newUser.getPermission() +".");
            }

            // create & persist fingerprints and access
            Repository fpRepo = getRepository(FingerprintEntity.class);
            Repository gaRepo = getRepository(GrantedAccessEntity.class);

            GrantedAccessEntity gae = new GrantedAccessEntity(gue, getDeviceConfiguration(), gte);

            // add the fingerprints
            for(FingerIndex index : _newUser.getEnrolledFingersIndexes())
            {
                if(!fpRepo.insert(
                        new FingerprintEntity(gue, index.getIndex(),
                                _newUser.getFingerprintTemplate(index))
                ))
                {
                    throw new SQLException("Failed when inserting new fingerprint.");
                }
            }

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
            Repository inventoryRepo = getRepository(InventoryEntity.class);
            Repository userRepo = getRepository(GrantedUserEntity.class);
            Repository accessTypeRepo = getRepository(AccessTypeEntity.class);
            Repository rfidTagRepo = getRepository(RfidTagEntity.class);
            Repository inventTagRepo = getRepository(InventoryRfidTag.class);

            if( !(userRepo instanceof GrantedUserRepository) ||
                !(accessTypeRepo instanceof AccessTypeRepository) ||
                !(rfidTagRepo instanceof RfidTagRepository) ||
                !(inventTagRepo instanceof InventoryRfidTagRepository))
            {
                // not supposed to happen as the repositories map is filled automatically
                throw new SQLException("Invalid repositories. Unable to insert Inventory in database.");
            }

            GrantedUserEntity gue = null;

            if(_inventory.getInitiatingUserName() != null && !"".equals(_inventory.getInitiatingUserName().trim()))
            {
                gue = ((GrantedUserRepository) userRepo).getByUsername(_inventory.getInitiatingUserName());
            }

            AccessTypeEntity ate = ((AccessTypeRepository) accessTypeRepo).fromAccessType(_inventory.getAccessType());

            if(ate == null)
            {
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
