package com.spacecode.smartserver.database.entity;


import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.smartserver.database.dao.DaoInventoryRfidTag;

/**
 * InventoryRfidTag Entity (table linking InventoryEntity and RfidTagEntity (many-to-many relationship)).
 */
@DatabaseTable(tableName = InventoryRfidTag.TABLE_NAME, daoClass = DaoInventoryRfidTag.class)
public final class InventoryRfidTag extends Entity
{
    public static final String TABLE_NAME = "sc_inventory_rfid_tag";

    public static final String INVENTORY_ID = "inventory_id";
    public static final String RFID_TAG_ID = "rfid_tag_id";
    public static final String MOVEMENT = "movement";
    public static final String SHELVE_NUMBER = "shelve_number";

    @DatabaseField(foreign = true, columnName = INVENTORY_ID, canBeNull = false)
    private InventoryEntity _inventory;

    @DatabaseField(foreign = true, columnName = RFID_TAG_ID, canBeNull = false, foreignAutoRefresh = true)
    private RfidTagEntity _rfidTag;

    @DatabaseField(columnName = MOVEMENT, canBeNull = false)
    private int _movement;

    @DatabaseField(columnName = SHELVE_NUMBER, canBeNull = false)
    private int _shelveNumber;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    InventoryRfidTag()
    {
    }

    /**
     * Default constructor
     *
     * @param inventory     Inventory Entity instance to be attached to RfidTag Entity.
     * @param rfidTag       RfidTag Entity Instance to be attached to Inventory Entity.
     * @param movementType  Tag status in inventory: Added (1), Present (0) or Removed (-1).
     */
    public InventoryRfidTag(InventoryEntity inventory, RfidTagEntity rfidTag, int movementType)
    {
        _inventory = inventory;
        _rfidTag = rfidTag;
        _movement = movementType;
        _shelveNumber = 0;
    }

    /**
     * Constructor chaining, adds shelve number.
     *
     * @param inventory     Inventory Entity instance to be attached to RfidTag Entity.
     * @param rfidTag       RfidTag Entity Instance to be attached to Inventory Entity.
     * @param movementType  Tag status in inventory: Added (1), Present (0) or Removed (-1).
     * @param shelveNumber  Shelve number for the tag (tag location).
     */
    public InventoryRfidTag(InventoryEntity inventory, RfidTagEntity rfidTag, int movementType, int shelveNumber)
    {
        this(inventory, rfidTag, movementType);
        _shelveNumber = shelveNumber;
    }

    /** @return Related Inventory. */
    public InventoryEntity getInventory()
    {
        return _inventory;
    }

    /** @return Related Tag. */
    public RfidTagEntity getRfidTag()
    {
        return _rfidTag;
    }

    /** @return Return tag status in the inventory. Added (1), Present (0) or Removed (-1). */
    public int getMovement()
    {
        return _movement;
    }

    /** @return Number of the shelve where the tag was scanned (if the device has many compartments). */
    public int getShelveNumber()
    {
        return _shelveNumber;
    }
}
