package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.DeviceEntity;

/**
 * DeviceConfiguration Repository
 */
public class DeviceRepository extends Repository<DeviceEntity>
{
    protected DeviceRepository(Dao<DeviceEntity, Integer> dao)
    {
        super(dao);
    }
}
