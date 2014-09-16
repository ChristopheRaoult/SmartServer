package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.AlertTemperatureEntity;

/**
 * AlertTemperature Repository
 */
public class AlertTemperatureRepository extends Repository<AlertTemperatureEntity>
{
    protected AlertTemperatureRepository(Dao<AlertTemperatureEntity, Integer> dao)
    {
        super(dao);
    }
}
