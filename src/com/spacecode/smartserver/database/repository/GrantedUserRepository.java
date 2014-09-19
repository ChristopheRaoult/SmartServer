package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.GrantedAccessEntity;
import com.spacecode.smartserver.database.entity.GrantedUserEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;

/**
 * GrantedUserEntity Repository
 */
public class GrantedUserRepository extends Repository<GrantedUserEntity>
{
    /**
     * Default constructor.
     * @param dao   Dao (GrantedUserEntity, Integer) to be used by the Repository.
     */
    public GrantedUserRepository(Dao<GrantedUserEntity, Integer> dao)
    {
        super(dao);
    }

    /**
     * Allow looking for a GrantedUserEntity by its username.
     * @param username  Desired user's name.
     * @return          GrantedUserEntity instance, or null if any error occurs (SQLException, user not found...).
     */
    public GrantedUserEntity getByUsername(String username)
    {
        try
        {
            return _dao.queryForFirst(
                    _dao.queryBuilder().where()
                            .eq(GrantedUserEntity.USERNAME, username)
                            .prepare());
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Error occurred while getting user by name.", sqle);
            return null;
        }
    }

    /**
     * Remove an user from the database (including all his fingerprints/accesses).
     * @param entity    User to be removed from the table.
     * @return          True if successful, false otherwise (SQLException).
     */
    @Override
    public boolean delete(GrantedUserEntity entity)
    {
        try
        {
            Repository<FingerprintEntity> fpRepo = DatabaseHandler.getRepository(FingerprintEntity.class);
            Repository<GrantedAccessEntity> gaRepo = DatabaseHandler.getRepository(GrantedAccessEntity.class);

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
    public boolean delete(Collection<GrantedUserEntity> entities)
    {
        for(GrantedUserEntity user : entities)
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
    public boolean updateBadge(String username, String badgeNumber)
    {
        GrantedUserEntity gue = getByUsername(username);

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
        GrantedUserEntity gue = getByUsername(username);

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
}
