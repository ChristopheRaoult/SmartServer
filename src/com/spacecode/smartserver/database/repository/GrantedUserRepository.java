package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.sdk.user.FingerIndex;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.database.entity.GrantedUserEntity;

import java.sql.SQLException;

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
        } catch (SQLException e)
        {
            return null;
        }
    }

    /**
     * Persist new user (from GrantedUser instance) in database.
     * Also handle Fingerprints inserts (but core code of each template insertion is left to FingerprintRepository).
     * @param newUser
     * @param fpRepo
     * @return
     */
    public boolean insertNewUser(GrantedUser newUser, FingerprintRepository fpRepo)
    {
        GrantedUserEntity gu = getByUsername(newUser.getUsername());

        if(gu != null)
        {
            return false;
        }

        try
        {
            GrantedUserEntity gue = new GrantedUserEntity(newUser.getUsername(), newUser.getBadgeId());

            // create user. Result is supposed to be "1" if row inserted
            if(_dao.create(gue) != 1)
            {
                return false;
            }

            for(FingerIndex index : newUser.getEnrolledFingersIndexes())
            {
                if(!fpRepo.insertNewFingerprint(gue, index, newUser.getFingerprintTemplate(index)))
                {
                    return false;
                }
            }

            return true;
        } catch (SQLException e)
        {
            return false;
        }
    }
}
