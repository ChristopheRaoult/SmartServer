package com.spacecode.smartserver.database.entity;


import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * InventoryRfidtag Entity
 */
@DatabaseTable(tableName = InventoryRfidtag.TABLE_NAME)
public class InventoryRfidtag
{
    public static final String TABLE_NAME = "sc_inventory_rfid_tag";

    public static final String ID = "id";
    public static final String INVENTORY_ID = "inventory_id";
    public static final String RFID_TAG_ID = "rfidtag_id";
    public static final String MOVEMENT = "movement";
    public static final String SHELVE_NUMBER = "shelve_number";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(foreign = true, columnName = INVENTORY_ID, canBeNull = false)
    private InventoryEntity _inventory;

    @DatabaseField(foreign = true, columnName = RFID_TAG_ID, canBeNull = false)
    private RfidTagEntity _rfidTag;

    @DatabaseField(columnName = MOVEMENT, canBeNull = false)
    private int _movement;

    @DatabaseField(columnName = SHELVE_NUMBER, canBeNull = false)
    private int _shelveNumber;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    InventoryRfidtag()
    {
    }

    /**
     * Default constructor
     * @param inventory Inventory Entity instance to be attached to RfidTag Entity.
     * @param rfidtag   RfidTag Entity Instance to be attached to Inventory Entity.
     */
    public InventoryRfidtag(InventoryEntity inventory, RfidTagEntity rfidtag)
    {
        _inventory = inventory;
        _rfidTag = rfidtag;
    }

    public int getId()
    {
        return _id;
    }

    public InventoryEntity getInventory()
    {
        return _inventory;
    }

    public RfidTagEntity getRfidTag()
    {
        return _rfidTag;
    }

    public int getMovement()
    {
        return _movement;
    }

    public int getShelveNumber()
    {
        return _shelveNumber;
    }
}
