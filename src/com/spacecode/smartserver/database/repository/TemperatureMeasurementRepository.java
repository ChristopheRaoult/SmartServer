package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.TemperatureMeasurementEntity;

/**
 * TemperatureMeasurement Repository
 */
public class TemperatureMeasurementRepository extends Repository<TemperatureMeasurementEntity>
{
    protected TemperatureMeasurementRepository(Dao<TemperatureMeasurementEntity, Integer> dao)
    {
        super(dao);
    }
}
