package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.sdk.device.data.Inventory;

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

    public InventoryEntity(Inventory inventory, DeviceEntity device, GrantedUserEntity gue, AccessTypeEntity ate)
    {
        _device = device;
        _grantedUser = gue;
        _accessType = ate;

        _totalAdded = inventory.getNumberAdded();
        _totalPresent = inventory.getNumberPresent();
        _totalRemoved = inventory.getNumberRemoved();

        _createdAt = inventory.getCreationDate();
    }

    /**
     * @return Unique entity identifier.
     */
    public int getId()
    {
        return _id;
    }

    /**
     * @return Device which performed this inventory.
     */
    public DeviceEntity getDevice()
    {
        return _device;
    }

    /**
     * @return User which started this inventory by authenticating (or null if manual scan).
     */
    public GrantedUserEntity getGrantedUser()
    {
        return _grantedUser;
    }

    /**
     * @return Access type (if started by a user) or Undefined (value of AccessType enumeration).
     */
    public AccessTypeEntity getAccessType()
    {
        return _accessType;
    }

    /**
     * @return Number of tags "Added" in the device, since the last inventory.
     */
    public int getTotalAdded()
    {
        return _totalAdded;
    }

    /**
     * @return Number of tags still "Present" in the device, since the last inventory.
     */
    public int getTotalPresent()
    {
        return _totalPresent;
    }

    /**
     * @return Number of tags "Removed" from the device, since the last inventory.
     */
    public int getTotalRemoved()
    {
        return _totalRemoved;
    }

    /**
     * @return Completion date of the inventory.
     */
    public Date getCreatedAt()
    {
        return new Date(_createdAt.getTime());
    }
}
