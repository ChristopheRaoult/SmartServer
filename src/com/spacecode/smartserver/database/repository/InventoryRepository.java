package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.misc.TransactionManager;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.*;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
     * Perform RAW SQL query to get the last inventory and create an Inventory (SDK) instance from its data.
     * TODO: Try to get rid of RAW SQL
     *
     * @return An Inventory (SDK) instance or null if anything went wrong.
     */
    public Inventory getLastInventory()
    {
        // 0: GrantedUser id, 1: AccessType id, 2: total tags added, 3: total tags present,
        // 4: total tags removed, 5: tag uid, 6: tag movement, 7: inventory creation date
        String columns = "inv."+InventoryEntity.GRANTED_USER_ID+"," +
                " inv."+InventoryEntity.ACCESS_TYPE_ID+"," +
                " inv."+InventoryEntity.TOTAL_ADDED+"," +
                " inv."+InventoryEntity.TOTAL_PRESENT+"," +
                " inv."+InventoryEntity.TOTAL_REMOVED+"," +
                " rt."+ RfidTagEntity.UID+"," +
                " irt."+ InventoryRfidTag.MOVEMENT+"," +
                " inv."+InventoryEntity.CREATED_AT;

        // raw query to get all columns for the last inventory
        StringBuilder sb = new StringBuilder("SELECT ").append(columns).append(" ");
        sb.append("FROM ").append(InventoryEntity.TABLE_NAME).append(" inv ");
        // join through the many-to-many relationship
        sb.append("LEFT JOIN ").append(InventoryRfidTag.TABLE_NAME).append(" irt ");
        sb.append("ON inv.").append(InventoryEntity.ID).append(" = ");
        sb.append("irt.").append(InventoryRfidTag.INVENTORY_ID).append(" ");
        // join all tags
        sb.append("LEFT JOIN ").append(RfidTagEntity.TABLE_NAME).append(" rt ");
        sb.append("ON rt.").append(RfidTagEntity.ID).append(" = ");
        sb.append("irt.").append(InventoryRfidTag.RFID_TAG_ID).append(" ");
        // for the current device only
        sb.append("WHERE inv.").append(InventoryEntity.CREATED_AT)
                .append(" = (SELECT MAX(").append(InventoryEntity.CREATED_AT).append(")")
                .append(" FROM ").append(InventoryEntity.TABLE_NAME).append(") ");
        sb.append("AND inv.").append(InventoryEntity.DEVICE_ID)
                .append(" = ").append(DbManager.getDevEntity().getId());

        Inventory lastInventoryFromDb = new Inventory();

        try
        {
            // one line per tag movement in the inventory (if any)
            GenericRawResults results = _dao.queryRaw(sb.toString());

            List<String> tagsAdded = new ArrayList<>();
            List<String> tagsPresent = new ArrayList<>();
            List<String> tagsRemoved = new ArrayList<>();

            String[] lastRow = null;

            // fill the inventory instance with results from Raw SQL query
            for (String[] result : (Iterable<String[]>) results)
            {
                // if it's a no-tag scan, there will be no line with a tag-movement
                if(result[6] == null)
                {
                    continue;
                }

                switch(result[6])
                {
                    case "1":
                        tagsAdded.add(result[5]);
                        break;

                    case "0":
                        tagsPresent.add(result[5]);
                        break;

                    case "-1":
                        tagsRemoved.add(result[5]);
                        break;

                    default:
                        // invalid row or value. Should not happen.
                        continue;
                }

                // store the last line in order to initialize (once) repeated data (creation date, username...).
                lastRow = result;
            }

            results.close();

            if(lastRow == null)
            {
                // there were no result: inventory table is empty.
                return lastInventoryFromDb;
            }

            /*
            Parse GrantedUser id as int to get its name from db
            Get AccessType as a value from enum
            Parse Creation Date as a Date
             */
            String username = null;
            AccessType accessType = null;
            Date creationDate;

            // user
            if(lastRow[0] != null)
            {
                int userId = Integer.parseInt(lastRow[0]);
                UserEntity gue = DbManager.getRepository(UserEntity.class).getEntityById(userId);

                if(gue != null)
                {
                    username = gue.getUsername();
                }
            }

            // access type
            int accessTypeId = Integer.parseInt(lastRow[1]);
            AccessTypeEntity ate = DbManager.getRepository(AccessTypeEntity.class).getEntityById(accessTypeId);

            if(ate != null)
            {
                accessType = AccessTypeRepository.asAccessType(ate);
            }

            creationDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(lastRow[7]);

            lastInventoryFromDb = new Inventory(tagsAdded, tagsPresent, tagsRemoved, username, accessType, creationDate);
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to load last inventory from database.", sqle);
            return null;
        } catch(IllegalArgumentException | ParseException e)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Invalid data provided for last inventory loading.", e);
        }

        return lastInventoryFromDb;
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
            List<String> tagsAdded = new ArrayList<>();
            List<String> tagsPresent = new ArrayList<>();
            List<String> tagsRemoved = new ArrayList<>();
            String username = invEntity.getGrantedUser() != null ? invEntity.getGrantedUser().getUsername() : "";
            AccessType accessType = AccessTypeRepository.asAccessType(invEntity.getAccessType());
            Date creationDate = invEntity.getCreatedAt();

            for(InventoryRfidTag irtEntity : invEntity.getRfidTags())
            {
                switch(irtEntity.getMovement())
                {
                    case 1:
                        tagsAdded.add(irtEntity.getRfidTag().getUid());
                        break;

                    case 0:
                        tagsPresent.add(irtEntity.getRfidTag().getUid());
                        break;

                    case -1:
                        tagsRemoved.add(irtEntity.getRfidTag().getUid());
                        break;

                    default:
                        break;
                }
            }

            result.add(new Inventory(tagsAdded, tagsPresent, tagsRemoved, username, accessType, creationDate));
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
