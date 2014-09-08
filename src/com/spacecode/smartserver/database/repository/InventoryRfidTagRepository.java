package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.InventoryRfidTag;

/**
 * InventoryRfidTag Repository
 */
public class InventoryRfidTagRepository extends Repository<InventoryRfidTag>
{
    protected InventoryRfidTagRepository(Dao<InventoryRfidTag, Integer> dao)
    {
        super(dao);
    }
}
