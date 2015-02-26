package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.*;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * Inventory Repository
 */
public class InventoryRepository extends Repository<InventoryEntity>
{
    protected InventoryRepository(Dao<InventoryEntity, Integer> dao)
    {
        super(dao);
    }

    /**
     * Get last inventory recorded in the Database and return it as an Inventory (sdk) instance.
     *
     * @return An Inventory (SDK) instance or null if: any error occurred, or no inventory was found.
     */
    public Inventory getLastInventory()
    {
        if(DbManager.getDevEntity() == null)
        {
            return null;
        }

        try
        {
            InventoryEntity lastEntity = _dao.queryForFirst(
                    _dao.queryBuilder()
                            .orderBy(InventoryEntity.CREATED_AT, false)
                            .limit(1L)
                            .where()
                            .eq(InventoryEntity.DEVICE_ID, DbManager.getDevEntity().getId())
                            .prepare());

            return lastEntity != null ? lastEntity.asInventory() : null;
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting entity where field not equal.", sqle);
            return null;
        }
    }

    /**
     * Get the list of InventoryEntity created during a certain period and convert it to a list of Inventory.
     *
     * @param from  Period start date.
     * @param to    Period end date.
     *
     * @return List of Inventory made during the given period (empty if no result or error).
     */
    public List<Inventory> getInventories(Date from, Date to)
    {
        List<InventoryEntity> queryResult;
        List<Inventory> result = new ArrayList<>();

        try
        {
            queryResult = _dao.query(
                    _dao.queryBuilder()
                            .orderBy(InventoryEntity.CREATED_AT, true)
                            .where()
                            .eq(InventoryEntity.DEVICE_ID, DbManager.getDevEntity().getId())
                            .and()
                            .between(InventoryEntity.CREATED_AT, from, to)
                            .prepare());
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "SQL Exception occurred while getting inventories.", sqle);
            return new ArrayList<>();
        }

        for(InventoryEntity invEntity : queryResult)
        {
            result.add(invEntity.asInventory());
        }
        
        return result;
    }

    /**
     * Persist new inventory in the database, including related RfidTagEntities
     * (many-to-many relationship through InventoryRfidTag).
     *
     * @param lastInventory Provided by RfidDevice instance. Inventory made when last scan completed.
     *
     * @return  True if operation succeeded, false otherwise.
     */
    public boolean persist(Inventory lastInventory)
    {
        try
        {
            TransactionManager.callInTransaction(DbManager.getConnectionSource(),
                    new PersistInventoryCallable(lastInventory));
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Error while persisting new inventory.", sqle);
            return false;
        }

        return true;
    }



    /**
     * Callable subclass called when persisting a new inventory (SQL transaction).
     * Doing all the operations in a transaction allow cancelling everything if anything goes wrong.
     */
    private class PersistInventoryCallable implements Callable<Void>
    {
        private final Inventory _inventory;

        private PersistInventoryCallable(Inventory inventory)
        {
            _inventory = inventory;
        }

        @Override
        public Void call() throws Exception
        {
            Repository<UserEntity> userRepo = DbManager.getRepository(UserEntity.class);
            Repository accessTypeRepo = DbManager.getRepository(AccessTypeEntity.class);
            Repository rfidTagRepo = DbManager.getRepository(RfidTagEntity.class);
            Repository<InventoryRfidTag> inventTagRepo = DbManager.getRepository(InventoryRfidTag.class);

            UserEntity gue = null;

            if(_inventory.getUsername() != null && !"".equals(_inventory.getUsername().trim()))
            {
                // the scan was not manual, there is a user corresponding to this inventory
                // we don't care if "gue" is null, as if it is, it means that the username is invalid
                gue = userRepo.getEntityBy(UserEntity.USERNAME, _inventory.getUsername());
            }

            AccessTypeEntity ate = ((AccessTypeRepository) accessTypeRepo).fromAccessType(_inventory.getAccessType());

            if(ate == null)
            {
                // GrantedUser can be null for an inventory (if manually started) but AccessType can't
                throw new SQLException("Invalid access type. Unable to insert Inventory in database.");
            }

            // create the entity to be inserted in the DB
            InventoryEntity ie = new InventoryEntity(_inventory, gue, ate);

            if(!insert(ie))
            {
                // INSERT query failed
                throw new SQLException("Failed when inserting new Inventory.");
            }

            Map<String, RfidTagEntity> uidToEntity = new HashMap<>();
            List<String> allUids = new ArrayList<>(_inventory.getTagsAll());
            allUids.addAll(_inventory.getTagsRemoved());

            // browse all UID's (tags added, present, removed) to fill the map with entities
            for(String tagUid : allUids)
            {
                RfidTagEntity rte = ((RfidTagRepository) rfidTagRepo).createIfNotExists(tagUid);

                if(rte == null)
                {
                    throw new SQLException("Unable to createIfNotExists a tag in database.");
                }

                uidToEntity.put(tagUid, rte);
            }

            // create the many-to-many relationship between the Inventory table and the RfidTag table
            List<InventoryRfidTag> inventoryRfidTags = new ArrayList<>();

            // get the matrix containing the axis number where each tag has been detected for the last time
            Map<String, Byte> tagToAxis = DeviceHandler.getDevice().getTagToAxis();

            int shelveNbr;

            for(String tagUid : _inventory.getTagsAdded())
            {
                shelveNbr = tagToAxis.get(tagUid) == null ? 0 : tagToAxis.get(tagUid);
                inventoryRfidTags.add(new InventoryRfidTag(ie, uidToEntity.get(tagUid), 1, shelveNbr));
            }

            for(String tagUid : _inventory.getTagsPresent())
            {
                shelveNbr = tagToAxis.get(tagUid) == null ? 0 : tagToAxis.get(tagUid);
                inventoryRfidTags.add(new InventoryRfidTag(ie, uidToEntity.get(tagUid), 0, shelveNbr));
            }

            for(String tagUid : _inventory.getTagsRemoved())
            {
                shelveNbr = tagToAxis.get(tagUid) == null ? 0 : tagToAxis.get(tagUid);
                inventoryRfidTags.add(new InventoryRfidTag(ie, uidToEntity.get(tagUid), -1, shelveNbr));
            }

            if(!inventTagRepo.insert(inventoryRfidTags))
            {
                throw new SQLException("Unable to insert all tags and movements from new inventory in database.");
            }

            // this Callable doesn't need a return value
            return null;
        }
    }
}
