package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.FingerprintEntity;

/**
 * FingerprintEntity Repository
 */
public class FingerprintRepository extends Repository<FingerprintEntity>
{
    public FingerprintRepository(Dao<FingerprintEntity, Integer> dao)
    {
        super(dao);
    }
}
