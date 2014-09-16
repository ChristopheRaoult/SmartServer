package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.AlertTypeEntity;

/**
 * AlertType Repository
 */
public class AlertTypeRepository extends Repository<AlertTypeEntity>
{
    protected AlertTypeRepository(Dao<AlertTypeEntity, Integer> dao)
    {
        super(dao);
    }
}
