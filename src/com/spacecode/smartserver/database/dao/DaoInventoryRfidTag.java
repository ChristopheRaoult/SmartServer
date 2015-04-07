package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.support.ConnectionSource;
import com.spacecode.smartserver.database.entity.InventoryRfidTag;

import java.sql.SQLException;

/**
 * InventoryRfidTag Repository
 */
public class DaoInventoryRfidTag extends DaoEntity<InventoryRfidTag, Integer>
{
    public DaoInventoryRfidTag(ConnectionSource connectionSource) throws SQLException
    {
        super(connectionSource, InventoryRfidTag.class);
    }
}
