package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.data.FingerIndex;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.GrantTypeEntity;
import com.spacecode.smartserver.database.entity.GrantedAccessEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * UserEntity Repository
 */
public class DaoUser extends DaoEntity<UserEntity, Integer>
{
    public DaoUser(ConnectionSource connectionSource) throws SQLException
    {
        super(connectionSource, UserEntity.class);
    }

    /**
     * Allow looking for a UserEntity by its username.
     *
     * @param username  Desired user's name.
     *
     * @return          UserEntity instance, or null if any error occurs (SQLException, user not found...).
     */
    public UserEntity getByUsername(String username)
    {
        if(username == null || username.trim().isEmpty())
        {
            return null;
        }

        return getEntityBy(UserEntity.USERNAME, username);
    }

    /**
     * Start the user deletion process (user + permissions + fingerprints).
     *
     * @param username  Name of to-be-deleted user.
     *
     * @return          True if successful, false otherwise (unknown user, SQLException...).
     */
    public boolean deleteByName(String username)
    {
        if(username == null || username.trim().isEmpty())
        {
            return false;
        }

        UserEntity gue = getByUsername(username);

        return gue != null && deleteEntity(gue);
    }

    /**
     * Remove an user from the database (including all his fingerprints/accesses).
     * @param entity    User to be removed from the table.
     * @return          True if successful, false otherwise (SQLException).
     */
    @Override
    public boolean deleteEntity(UserEntity entity)
    {
        if(entity == null)
        {
            return false;
        }
        
        DaoFingerprint fpRepo = (DaoFingerprint) DbManager.getDao(FingerprintEntity.class);
        DaoGrantedAccess gaRepo = (DaoGrantedAccess) DbManager.getDao(GrantedAccessEntity.class);

        // first, remove the foreign dependencies
        fpRepo.deleteEntity(entity.getFingerprints());
        gaRepo.deleteEntity(entity.getGrantedAccesses());

        // then, remove the user
        return super.deleteEntity(entity);
    }

    /**
     * Remove a collection of users from the database (including all their fingerprints/accesses).
     * @param entities  Collection of users to be removed from the table.
     * @return          True if successfully removed all users, false otherwise (SQLException).
     */
    @Override
    public boolean deleteEntity(Collection<UserEntity> entities)
    {
        for(UserEntity user : entities)
        {
            if(!deleteEntity(user))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Update an user's badge number according to client's request.
     *
     * @param username      User to be updated.
     * @param badgeNumber   New badge number.
     *
     * @return              True if success, false otherwise.
     */
    public boolean updateBadgeNumber(String username, String badgeNumber)
    {
        UserEntity gue = getByUsername(username);

        if(gue == null)
        {
            return false;
        }

        gue.setBadgeNumber(badgeNumber);
        return updateEntity(gue);
    }

    /**
     * Update an user's "thief finger" according to client's request.
     *
     * @param username      User to be updated.
     * @param fingerIndex   New "thief finger" index.
     *
     * @return              True if success, false otherwise.
     */
    public boolean updateThiefFingerIndex(String username, Integer fingerIndex)
    {
        UserEntity gue = getByUsername(username);

        if(gue == null)
        {
            return false;
        }
    
        gue.setThiefFingerIndex(fingerIndex);
        return updateEntity(gue);
    }

    /**
     * Process the data persistence:
     * UserEntity, Fingerprint(s), GrantedAccessEntity.
     *
     * @param newUser   Instance of GrantedUser (SDK) to be added to database.
     *
     * @return          True if success, false otherwise (username already used, SQLException, etc).
     */
    public boolean persist(final User newUser)
    {
        try
        {
            TransactionManager.callInTransaction(DbManager.getConnectionSource(), new PersistUserCallable(newUser));
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Error while persisting new user.", sqle);
            return false;
        }

        return true;
    }

    /**
     * Delete all permissions (if any) of a given user (by name) on the current device.
     *
     * @param username User's name.
     *
     * @return True if the operation succeded, false otherwise (unknown user, SQL exception).
     */
    public boolean removePermission(String username)
    {
        UserEntity gue = getByUsername(username);

        if(gue == null)
        {
            return false;
        }

        DaoGrantedAccess gaRepo = (DaoGrantedAccess) DbManager.getDao(GrantedAccessEntity.class);
        Collection<GrantedAccessEntity> gaesList = gue.getGrantedAccesses();
        Collection<GrantedAccessEntity> gaesOndevice = new ArrayList<>();

        for(GrantedAccessEntity gae : gaesList)
        {
            if(gae.getDevice().getId() == DbManager.getDevEntity().getId())
            {
                gaesOndevice.add(gae);
            }
        }

        return gaRepo.deleteEntity(gaesOndevice);
    }

    /**
     * Callable subclass called when persisting a new user (SQL transaction)
     */
    private class PersistUserCallable implements Callable<Void>
    {
        private final User _newUser;

        private PersistUserCallable(User newUser)
        {
            _newUser = newUser;
        }

        @Override
        public Void call() throws Exception
        {
            // try to get the user if he already exists
            UserEntity gue = getByUsername(_newUser.getUsername());

            // if he doesn't exist
            if(gue == null)
            {
                // create it
                gue = new UserEntity(_newUser);

                // try to insert him in DB
                if (!insert(gue))
                {
                    throw new SQLException("Failed when inserting new user.");
                }

                DaoFingerprint fpRepo = (DaoFingerprint) DbManager.getDao(FingerprintEntity.class);

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
            DaoGrantType grantTypeRepo =(DaoGrantType) DbManager.getDao(GrantTypeEntity.class);
            GrantTypeEntity gte = grantTypeRepo.fromGrantType(_newUser.getPermission());

            if(gte == null)
            {
                throw new SQLException("Persisting user: unknown grant type "+ _newUser.getPermission() +".");
            }

            // create & persist access
            DaoGrantedAccess gaRepo = (DaoGrantedAccess) DbManager.getDao(GrantedAccessEntity.class);

            GrantedAccessEntity gae = new GrantedAccessEntity(gue, gte);

            // add the access to current device (if any)
            if(!gaRepo.insert(gae))
            {
                throw new SQLException("Failed when inserting new granted access.");
            }

            return null;
        }
    }

    /**
     * Get all users having a permission of the current device, create corresponding User instances and fill a list.
     * TODO: Try to get rid of RAW SQL
     *
     * @return A list of User instances, or null if any error occurred.
     */
    public List<User> getAuthorizedUsers()
    {
        // use getDevEntity to initialize devEntity, in case it was null. If still null, stop: no device available.
        if(DbManager.getDevEntity() == null)
        {
            return null;
        }

        List<User> users = new ArrayList<>();

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
                .append(DbManager.getDevEntity().getId());

        // username to temporary User instance
        Map<String, User> usernameToUser = new HashMap<>();

        // username to map of fingerprints
        Map<String, Map<FingerIndex, String>> usernameToFingersMap = new HashMap<>();

        try
        {
            // get one line per user having a granted access on this device
            // one more line (with repeated user information) for each user's fingerprint.
            GenericRawResults results = queryRaw(sb.toString());

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
                    // if there was no map for current user's fingers, create one
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

            // Maps are filled, now, create the users and fill the results list
            for(Map.Entry<String, User> e : usernameToUser.entrySet())
            {
                String username = e.getKey();
                Map<FingerIndex, String> fingersMap = usernameToFingersMap.get(username);
                User tmpUser = e.getValue();
                User newUser = new User(username, tmpUser.getPermission(), tmpUser.getBadgeNumber(), fingersMap);

                users.add(newUser);
            }

            results.close();

            return users;
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to load users from database.", sqle);
        } catch(IllegalArgumentException iae)
        {
            // invalid fingerIndex or grantType from database
            SmartLogger.getLogger().log(Level.SEVERE, "Loading users process failed because of corrupted data.", iae);
        }

        return null;
    }
}
