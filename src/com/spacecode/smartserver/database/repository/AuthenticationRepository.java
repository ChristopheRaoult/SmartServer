package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.AuthenticationEntity;

/**
 * Authentication Repository
 */
public class AuthenticationRepository extends Repository<AuthenticationEntity>
{
    protected AuthenticationRepository(Dao<AuthenticationEntity, Integer> dao)
    {
        super(dao);
    }
}
