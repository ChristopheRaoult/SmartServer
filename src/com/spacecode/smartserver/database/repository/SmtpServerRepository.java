package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.SmtpServerEntity;

/**
 * SmtpServer Repository
 */
public class SmtpServerRepository extends Repository<SmtpServerEntity>
{
    protected SmtpServerRepository(Dao<SmtpServerEntity, Integer> dao)
    {
        super(dao);
    }
}
