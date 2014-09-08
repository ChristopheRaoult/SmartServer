package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.InventoryEntity;

/**
 * Inventory Repository
 */
public class InventoryRepository extends Repository<InventoryEntity>
{
    protected InventoryRepository(Dao<InventoryEntity, Integer> dao)
    {
        super(dao);
    }
}
