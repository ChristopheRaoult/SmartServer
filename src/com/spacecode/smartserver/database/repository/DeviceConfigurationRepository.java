package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.DeviceConfigurationEntity;

/**
 * DeviceConfiguration Repository
 */
public class DeviceConfigurationRepository extends Repository<DeviceConfigurationEntity>
{
    public DeviceConfigurationRepository(Dao<DeviceConfigurationEntity, Integer> dao)
    {
        super(dao);
    }
}
