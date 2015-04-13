package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.data.FingerIndex;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.DeviceEntity;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
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
     * @param newUser   Instance of User (SDK) to be added to database.
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
        DeviceEntity devEntity = DbManager.getDevEntity();        
        DaoGrantedAccess gaRepo = (DaoGrantedAccess) DbManager.getDao(GrantedAccessEntity.class);

        if(gue == null || devEntity == null || gaRepo == null)
        {
            return false;
        }

        Collection<GrantedAccessEntity> gaesList = gue.getGrantedAccesses();
        Collection<GrantedAccessEntity> gaesOndevice = new ArrayList<>();

        for(GrantedAccessEntity gae : gaesList)
        {
            if(gae.getDevice().getId() == devEntity.getId())
            {
                gaesOndevice.add(gae);
            }
        }
        
        DbManager.forceUpdate(gue);

        return gaRepo.deleteEntity(gaesOndevice);
    }

    /**
     * Fill the two lists given in parameter with the users authorized (who have a permission on the device) and
     * unregistered (who have not).
     *
     * @param authorizedUsers   Users with a permission on the device.
     * @param unregisteredUsers Users without a permission.
     *
     * @return True if the operation succeeded, false otherwise.
     */
    public boolean sortUsersFromDb(List<User> authorizedUsers, List<User> unregisteredUsers)
    {
        if(authorizedUsers == null || unregisteredUsers == null)
        {
            return false;
        }

        authorizedUsers.clear();
        unregisteredUsers.clear();

        // use getDevEntity to initialize devEntity, in case it was null. If still null, stop: no device available.
        if (DbManager.getDevEntity() == null)
        {
            return false;
        }

        DaoGrantedAccess daoAccess = (DaoGrantedAccess) DbManager.getDao(GrantedAccessEntity.class);

        if(daoAccess == null)
        {
            return false;
        }

        // IDs of authorized users (used to get, at the opposite, the unregistered users)
        List<Integer> authorizedIds = new ArrayList<>();

        // get all accesses created on this device
        List<GrantedAccessEntity> grantedAccesses = daoAccess
                .getEntitiesBy(GrantedAccessEntity.DEVICE_ID, DbManager.getDevEntity().getId());

        // for each, get: the user, the grant type, create a map for the fingerprints, then add the user to the list
        for(GrantedAccessEntity gae : grantedAccesses)
        {
            Map<FingerIndex, String> fingers = new EnumMap<>(FingerIndex.class);
            UserEntity userEntity = gae.getUser();
            GrantType gt = DaoGrantType.asGrantType(gae.getGrantType());

            if(userEntity == null || gt == null)
            {
                // null user or grant type: skip
                SmartLogger.getLogger().warning("Null value on User or GrantType while loading Authorized Users...");
                continue;
            }

            authorizedIds.add(userEntity.getId());

            for(FingerprintEntity fpe : userEntity.getFingerprints())
            {
                FingerIndex fIndex = FingerIndex.getValueByIndex(fpe.getFingerIndex());

                if(fIndex == null)
                {
                    // invalid finger index: skip
                    SmartLogger.getLogger().warning("Null value on Finger Index while loading Authorized Users...");
                    continue;
                }

                fingers.put(fIndex, fpe.getTemplate());
            }

            authorizedUsers.add(new User(userEntity.getUsername(), gt, userEntity.getBadgeNumber(), fingers));
        }

        // Now get all the unregistered users: Those whom ID does not appear in the "GrantedAccesses for this device".
        List<UserEntity> unregEntities = getEntitiesWhereNotIn(UserEntity.ID, authorizedIds);

        for(UserEntity unregEntity : unregEntities)
        {
            Map<FingerIndex, String> fingers = new EnumMap<>(FingerIndex.class);

            for(FingerprintEntity fpe : unregEntity.getFingerprints())
            {
                FingerIndex fIndex = FingerIndex.getValueByIndex(fpe.getFingerIndex());

                if(fIndex == null)
                {
                    // invalid finger index: skip
                    SmartLogger.getLogger().warning("Null value on Finger Index while loading Unregistered Users...");
                    continue;
                }

                fingers.put(fIndex, fpe.getTemplate());
            }

            unregisteredUsers.add(new User(unregEntity.getUsername(),
                    GrantType.UNDEFINED,
                    unregEntity.getBadgeNumber(),
                    fingers));
        }

        return true;
    }

    /**
     * Callable subclass called when persisting a new user (SQL transaction). 
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
                
                if(fpRepo != null)
                {
                    // insert or update his fingerprints
                    for (FingerIndex index : _newUser.getEnrolledFingersIndexes())
                    {
                        if (!fpRepo.updateEntity(
                                new FingerprintEntity(gue, index.getIndex(), _newUser.getFingerprintTemplate(index))
                        ))
                        {
                            throw new SQLException("Failed when inserting new user's fingerprints.");
                        }
                    }
                }
            }

            // create & persist access
            DaoGrantedAccess gaRepo = (DaoGrantedAccess) DbManager.getDao(GrantedAccessEntity.class);            
            if(gaRepo == null || !gaRepo.persist(gue, _newUser.getPermission()))
            {
                throw new SQLException("Failed when inserting new permission.");
            }

            return null;
        }
    }
}
