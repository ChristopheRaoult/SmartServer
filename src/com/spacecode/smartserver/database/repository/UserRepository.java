package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.data.FingerIndex;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.GrantTypeEntity;
import com.spacecode.smartserver.database.entity.GrantedAccessEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * UserEntity Repository
 */
public class UserRepository extends Repository<UserEntity>
{
    /**
     * Default constructor.
     * @param dao   Dao (UserEntity, Integer) to be used by the Repository.
     */
    public UserRepository(Dao<UserEntity, Integer> dao)
    {
        super(dao);
    }

    /**
     * Allow looking for a UserEntity by its username.
     * @param username  Desired user's name.
     * @return          UserEntity instance, or null if any error occurs (SQLException, user not found...).
     */
    public UserEntity getByUsername(String username)
    {
        try
        {
            return _dao.queryForFirst(
                    _dao.queryBuilder().where()
                            .eq(UserEntity.USERNAME, username)
                            .prepare());
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Error occurred while getting user by name.", sqle);
            return null;
        }
    }

    /**
     * Start the user deletion process (user + fingerprints).
     *
     * @param username  Name of to-be-deleted user.
     *
     * @return          True if successful, false otherwise (unknown user, SQLException...).
     */
    public boolean deleteByName(String username)
    {
        UserEntity gue = getEntityBy(UserEntity.USERNAME, username);
        return gue != null && delete(gue);
    }

    /**
     * Remove an user from the database (including all his fingerprints/accesses).
     * @param entity    User to be removed from the table.
     * @return          True if successful, false otherwise (SQLException).
     */
    @Override
    public boolean delete(UserEntity entity)
    {
        try
        {
            Repository<FingerprintEntity> fpRepo = DbManager.getRepository(FingerprintEntity.class);
            Repository<GrantedAccessEntity> gaRepo = DbManager.getRepository(GrantedAccessEntity.class);

            // first, remove the foreign dependencies
            fpRepo.delete(entity.getFingerprints());
            gaRepo.delete(entity.getGrantedAccesses());

            // then, remove the user
            _dao.delete(entity);
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while deleting GrantedUser.", sqle);
            return false;
        }

        return true;
    }

    /**
     * Remove a collection of users from the database (including all their fingerprints/accesses).
     * @param entities  Collection of users to be removed from the table.
     * @return          True if successfully removed all users, false otherwise (SQLException).
     */
    @Override
    public boolean delete(Collection<UserEntity> entities)
    {
        for(UserEntity user : entities)
        {
            if(!delete(user))
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

        try
        {
            gue.setBadgeNumber(badgeNumber);
            return _dao.update(gue) == 1;
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to update badge number.", sqle);
            return false;
        }
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

        try
        {
            gue.setThiefFingerIndex(fingerIndex);
            return _dao.update(gue) == 1;
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to update thief finger index.", sqle);
            return false;
        }
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
            // try to get the user if he already exists (by username)
            UserEntity gue = getEntityBy(UserEntity.USERNAME, _newUser.getUsername());

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

                Repository<FingerprintEntity> fpRepo = DbManager.getRepository(FingerprintEntity.class);

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
            Repository grantTypeRepo = DbManager.getRepository(GrantTypeEntity.class);
            GrantTypeEntity gte = ((GrantTypeRepository) grantTypeRepo).fromGrantType(_newUser.getPermission());

            if(gte == null)
            {
                throw new SQLException("Persisting user: unknown grant type "+ _newUser.getPermission() +".");
            }

            // create & persist fingerprints and access
            Repository<GrantedAccessEntity> gaRepo = DbManager.getRepository(GrantedAccessEntity.class);

            GrantedAccessEntity gae = new GrantedAccessEntity(gue, DbManager.getDeviceConfiguration(), gte);

            // add the access to current device (if any)
            if(!gaRepo.insert(gae))
            {
                throw new SQLException("Failed when inserting new granted access.");
            }

            return null;
        }
    }
}
