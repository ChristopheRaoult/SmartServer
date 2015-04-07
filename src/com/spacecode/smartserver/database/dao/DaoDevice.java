package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.support.ConnectionSource;
import com.spacecode.smartserver.database.entity.DeviceEntity;

import java.sql.SQLException;

/**
 * DeviceConfiguration Repository
 */
public class DaoDevice extends DaoEntity<DeviceEntity, Integer>
{
    public DaoDevice(ConnectionSource connectionSource) throws SQLException
    {
        super(connectionSource, DeviceEntity.class);
    }
}
