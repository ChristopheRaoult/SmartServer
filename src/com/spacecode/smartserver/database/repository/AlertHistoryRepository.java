package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.AlertHistoryEntity;

/**
 * AlertHistory Repository
 */
public class AlertHistoryRepository extends Repository<AlertHistoryEntity>
{
    protected AlertHistoryRepository(Dao<AlertHistoryEntity, Integer> dao)
    {
        super(dao);
    }
}
