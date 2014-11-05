package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.user.AccessType;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.*;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
        sb.append("AND inv.").append(InventoryEntity.DEVICE_ID).append(" = ").append(DatabaseHandler.getDeviceConfiguration().getId());

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
                GrantedUserEntity gue = DatabaseHandler.getRepository(GrantedUserEntity.class).getEntityById(userId);

                if(gue != null)
                {
                    username = gue.getUsername();
                }
            }

            // access type
            int accessTypeId = Integer.parseInt(lastRow[1]);
            AccessTypeEntity ate = DatabaseHandler.getRepository(AccessTypeEntity.class).getEntityById(accessTypeId);

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
     * Perform RAW SQL query to get the last inventory and create an Inventory (SDK) instance from its data.
     *
     * @return An Inventory (SDK) instance or null if anything went wrong.
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
                            .eq(InventoryEntity.DEVICE_ID, DatabaseHandler.getDeviceConfiguration().getId())
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
}
