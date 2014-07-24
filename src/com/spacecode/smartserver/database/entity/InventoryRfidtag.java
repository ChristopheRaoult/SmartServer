package com.spacecode.smartserver.database.entity;


import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * InventoryRfidtag Entity
 */
@DatabaseTable(tableName = "sc_inventory_rfid_tag")
public class InventoryRfidtag
{
    public static final String ID = "id";
    public static final String INVENTORY_ID = "inventory_id";
    public static final String RFID_TAG_ID = "rfidtag_id";
    public static final String MOVEMENT = "movement";
    public static final String SHELVE_NUMBER = "shelve_number";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(foreign = true, columnName = INVENTORY_ID, canBeNull = false)
    private Inventory _inventory;

    @DatabaseField(foreign = true, columnName = RFID_TAG_ID, canBeNull = false)
    private RfidTag _rfidTag;

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
    public InventoryRfidtag(Inventory inventory, RfidTag rfidtag)
    {
        _inventory = inventory;
        _rfidTag = rfidtag;
    }
}
