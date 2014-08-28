package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.sdk.user.FingerIndex;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.GrantedUserEntity;

import java.sql.SQLException;

/**
 * FingerprintEntity Repository
 */
public class FingerprintRepository extends Repository<FingerprintEntity>
{
    public FingerprintRepository(Dao<FingerprintEntity, Integer> dao)
    {
        super(dao);
    }

    /**
     * Insert a new fingerprint template in the database.
     * @param gue                   Attached user.
     * @param index                 FingerprintIndex's index value
     * @param fingerprintTemplate   Base64 encoded fingerprint template.
     * @return                      True if success, false otherwise (sql exception).
     */
    public boolean insertNewFingerprint(GrantedUserEntity gue, FingerIndex index, String fingerprintTemplate)
    {
        try
        {
            _dao.create(new FingerprintEntity(gue, index.getIndex(), fingerprintTemplate));
        } catch (SQLException e)
        {
            return false;
        }

        return true;
    }
}
