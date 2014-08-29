package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.GrantedUserEntity;

import java.sql.SQLException;
import java.util.Collection;

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
     * Also handle Fingerprints insert() calls.
     * @param newUser   GrantedUserEntity to be inserted in the database.
     * @return          True if success, false otherwise (SQL error, one fingerprint could not be inserted, etc).
     */
    @Override
    public boolean insert(GrantedUserEntity newUser)
    {
        GrantedUserEntity gu = getByUsername(newUser.getUsername());

        if(gu != null)
        {
            return false;
        }

        try
        {
            Repository fpRepo = DatabaseHandler.getRepository(FingerprintEntity.class);

            // create user. Result is supposed to be "1" if row inserted
            if(_dao.create(newUser) != 1 || !(fpRepo instanceof  FingerprintRepository))
            {
                return false;
            }

            if(newUser.getFingerprints() != null)
            {
                for(FingerprintEntity fpEntity : newUser.getFingerprints())
                {
                    if(!fpRepo.insert(fpEntity))
                    {
                        return false;
                    }
                }
            }

            return true;
        } catch (SQLException e)
        {
            return false;
        }
    }

    /**
     * Remove an user from the database (including all his fingerprints).
     * @param entity    User to be removed from the table.
     * @return          True if successful, false otherwise (SQLException).
     */
    @Override
    public boolean delete(GrantedUserEntity entity)
    {
        try
        {
            Repository fpRepo = DatabaseHandler.getRepository(FingerprintEntity.class);

            // create user. Result is supposed to be "1" if row inserted
            if(!(fpRepo instanceof  FingerprintRepository))
            {
                return false;
            }

            // first, remove the fingerprints (foreign dependency)
            fpRepo.delete(entity.getFingerprints());

            // then remove the user
            _dao.delete(entity);
        } catch (SQLException sqle)
        {
            return false;
        }

        return true;
    }

    /**
     * Remove a collection of users from the database (including all their fingerprints).
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
}
