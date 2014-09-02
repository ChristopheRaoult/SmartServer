package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.GrantedAccessEntity;

/**
 * GrantedAccess Repository
 */
public class GrantedAccessRepository extends Repository<GrantedAccessEntity>
{
    protected GrantedAccessRepository(Dao<GrantedAccessEntity, Integer> dao)
    {
        super(dao);
    }
}
