package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.support.ConnectionSource;
import com.spacecode.smartserver.database.entity.AlertTemperatureEntity;

import java.sql.SQLException;

/**
 * AlertTemperature Repository
 */
public class DaoAlertTemperature extends DaoEntity<AlertTemperatureEntity, Integer>
{
    public DaoAlertTemperature(ConnectionSource connectionSource) throws SQLException
    {
        super(connectionSource, AlertTemperatureEntity.class);
    }
}
