package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * Inventory Entity
 */
@DatabaseTable(tableName = InventoryEntity.TABLE_NAME)
public class InventoryEntity
{
    public static final String TABLE_NAME = "sc_inventory";

    public static final String ID = "id";
    public static final String DEVICE_ID = "device_id";
    public static final String GRANTED_USER_ID = "granteduser_id";
    public static final String ACCESS_TYPE_ID = "accesstype_id";
    public static final String TOTAL_ADDED = "total_added";
    public static final String TOTAL_PRESENT = "total_present";
    public static final String TOTAL_REMOVED = "total_removed";
    public static final String CREATED_AT = "created_at";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(foreign = true, columnName = DEVICE_ID, canBeNull = false)
    private DeviceEntity _device;

    @DatabaseField(foreign = true, columnName = GRANTED_USER_ID)
    private GrantedUserEntity _grantedUser;

    @DatabaseField(foreign = true, columnName = ACCESS_TYPE_ID, canBeNull = false)
    private AccessTypeEntity _accessType;

    @DatabaseField(columnName = TOTAL_ADDED, canBeNull = false)
    private int _totalAdded;

    @DatabaseField(columnName = TOTAL_PRESENT, canBeNull = false)
    private int _totalPresent;

    @DatabaseField(columnName = TOTAL_REMOVED, canBeNull = false)
    private int _totalRemoved;

    @DatabaseField(columnName = CREATED_AT)
    private Date _createdAt;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    InventoryEntity()
    {
    }
}
