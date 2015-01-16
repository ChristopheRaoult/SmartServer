package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * FingerprintEntity Repository
 */
public class FingerprintRepository extends Repository<FingerprintEntity>
{
    protected FingerprintRepository(Dao<FingerprintEntity, Integer> dao)
    {
        super(dao);
    }

    /**
     * Allow getting a fingerprint from db with GrantedUser entity and finger index.
     * @param gue   User attached to the fingerprint.
     * @param index Finger index of the fingerprint.
     * @return      FingerprintEntity instance (if any), null otherwise (none or SQLException).
     */
    public FingerprintEntity getFingerprint(UserEntity gue, int index)
    {
        try
        {
            return _dao.queryForFirst(
                    _dao.queryBuilder().where()
                            .eq(FingerprintEntity.GRANTED_USER_ID, gue.getId())
                            .and()
                            .eq(FingerprintEntity.FINGER_INDEX, index)
                            .prepare());
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting Fingerprint.", sqle);
            return null;
        }
    }

    /**
     * (Create or) Update a fingerprint template according to granted_user_id and finger_index.
     * @param fpEntity  Entity instance containing user's Id and finger index values.
     * @return          True if success, false otherwise (unknown fingerprint
     */
    @Override
    public boolean update(FingerprintEntity fpEntity)
    {
        try
        {
            FingerprintEntity fpEnt = _dao.queryForFirst(
                    _dao.queryBuilder().where()
                            .eq(FingerprintEntity.GRANTED_USER_ID, fpEntity.getGrantedUser().getId())
                            .and()
                            .eq(FingerprintEntity.FINGER_INDEX, fpEntity.getFingerIndex())
                            .prepare());

            if(fpEnt == null)
            {
                _dao.create(fpEntity);
            }

            else
            {
                fpEnt.setTemplate(fpEntity.getTemplate());
                _dao.update(fpEnt);
            }
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while updating Fingerprint.", sqle);
            return false;
        }

        return true;
    }

    /**
     * Delete a given fingerprint (username + finger index) from database.
     *
     * @param username  User attached to the fingerprint.
     * @param index     FingerIndex's index of the fingerprint.
     *
     * @return          True if successful, false otherwise.
     */
    public boolean delete(String username, int index)
    {
        Repository<UserEntity> userRepo = DbManager.getRepository(UserEntity.class);

        UserEntity gue = userRepo.getEntityBy(UserEntity.USERNAME, username);

        if(gue == null)
        {
            return false;
        }

        FingerprintEntity fpe = getFingerprint(gue, index);
        return fpe != null && delete(fpe);
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
    public boolean persist(String username, int fingerIndex, String fpTpl)
    {
        Repository<UserEntity> userRepo = DbManager.getRepository(UserEntity.class);

        UserEntity gue = userRepo.getEntityBy(UserEntity.USERNAME, username);

        return gue != null && update(new FingerprintEntity(gue, fingerIndex, fpTpl));
    }
}
