package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.support.ConnectionSource;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * FingerprintEntity Repository
 */
public class DaoFingerprint extends DaoEntity<FingerprintEntity, Integer>
{
    public DaoFingerprint(ConnectionSource connectionSource) throws SQLException
    {
        super(connectionSource, FingerprintEntity.class);
    }

    /**
     * Allow getting a fingerprint from db with a UserEntity and a finger index.
     * 
     * @param gue   User attached to the fingerprint.
     * @param index Finger index of the fingerprint.
     *              
     * @return      FingerprintEntity instance (if any), null otherwise (none or SQLException).
     */
    public FingerprintEntity getFingerprint(UserEntity gue, int index)
    {
        if(gue == null)
        {
            return null;
        }
        
        try
        {
            return queryForFirst(
                    queryBuilder().where()
                            .eq(FingerprintEntity.USER_ID, gue.getId())
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
     * (Create or) Update a fingerprint template according to user_id and finger_index.
     * 
     * @param fpEntity  Entity instance containing user's Id and finger index values.
     *                  
     * @return          True if success, false otherwise (unknown fingerprint
     */
    @Override
    public boolean updateEntity(FingerprintEntity fpEntity)
    {
        UserEntity ue = fpEntity.getUser();        
        FingerprintEntity fpEnt = getFingerprint(ue, fpEntity.getFingerIndex());

        DbManager.forceUpdate(ue);
        
        if(fpEnt == null)
        {
            return insert(fpEntity);
        }
        
        fpEnt.setTemplate(fpEntity.getTemplate());
        
        return super.updateEntity(fpEnt);
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
        DaoUser daoUser = (DaoUser) DbManager.getDao(UserEntity.class);
        UserEntity gue = daoUser.getEntityBy(UserEntity.USERNAME, username);

        if(gue == null)
        {
            return false;
        }

        DbManager.forceUpdate(gue);

        FingerprintEntity fpe = getFingerprint(gue, index);
        return fpe != null && deleteEntity(fpe);
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
        DaoUser daoUser = (DaoUser) DbManager.getDao(UserEntity.class);        
        UserEntity gue = daoUser.getEntityBy(UserEntity.USERNAME, username);

        return gue != null && updateEntity(new FingerprintEntity(gue, fingerIndex, fpTpl));
    }
}
