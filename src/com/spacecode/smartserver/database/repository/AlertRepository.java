package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.AlertEntity;

/**
 * Alert Repository
 */
public class AlertRepository extends Repository<AlertEntity>
{
    protected AlertRepository(Dao<AlertEntity, Integer> dao)
    {
        super(dao);
    }
}
