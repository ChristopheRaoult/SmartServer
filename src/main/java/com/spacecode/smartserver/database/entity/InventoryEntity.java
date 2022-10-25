package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoAccessType;
import com.spacecode.smartserver.database.dao.DaoInventory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Inventory Entity
 */
@DatabaseTable(tableName = InventoryEntity.TABLE_NAME, daoClass = DaoInventory.class)
public final class InventoryEntity extends Entity
{
    public static final String TABLE_NAME = "sc_inventory";

    public static final String DEVICE_ID = "device_id";
    public static final String USER_ID = "user_id";
    public static final String ACCESS_TYPE_ID = "access_type_id";
    public static final String DOOR_NUMBER = "door_number";
    public static final String TOTAL_ADDED = "total_added";
    public static final String TOTAL_PRESENT = "total_present";
    public static final String TOTAL_REMOVED = "total_removed";
    public static final String CREATED_AT = "created_at";

    @DatabaseField(foreign = true, columnName = DEVICE_ID, canBeNull = false)
    private DeviceEntity _device;

    @DatabaseField(foreign = true, columnName = USER_ID, foreignAutoRefresh = true)
    private UserEntity _user;

    @DatabaseField(foreign = true, columnName = ACCESS_TYPE_ID, canBeNull = false, foreignAutoRefresh = true)
    private AccessTypeEntity _accessType;

    @DatabaseField(columnName = DOOR_NUMBER, canBeNull = false, defaultValue = "-1")
    private byte _doorNumber;

    @DatabaseField(columnName = TOTAL_ADDED, canBeNull = false)
    private int _totalAdded;

    @DatabaseField(columnName = TOTAL_PRESENT, canBeNull = false)
    private int _totalPresent;

    @DatabaseField(columnName = TOTAL_REMOVED, canBeNull = false)
    private int _totalRemoved;

    @DatabaseField(columnName = CREATED_AT, index = true)
    private Date _createdAt;

    @ForeignCollectionField(eager = true)
    private ForeignCollection<InventoryRfidTag> _rfidTags;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    InventoryEntity()
    {
    }

    /**
     * Default constructor.
     *
     * @param inventory     Inventory [SDK] instance to take values from.
     * @param gue           UserEntity (if any) attached to this inventory.
     * @param ate           AccessTypeEntity (Manual, Fingerprint, Badge...) attached to this inventory.
     * @param doorNumber    Number of the door which made the scan start when closed (or -1, default).
     */
    public InventoryEntity(Inventory inventory, UserEntity gue, AccessTypeEntity ate, byte doorNumber)
    {
        _device = DbManager.getDevEntity();
        _user = gue;
        _accessType = ate;
        _doorNumber = doorNumber;

        _totalAdded = inventory.getNumberAdded();
        _totalPresent = inventory.getNumberPresent();
        _totalRemoved = inventory.getNumberRemoved();

        _createdAt = inventory.getCreationDate();
    }

    /** @return Device which performed this inventory. */
    public DeviceEntity getDevice()
    {
        return _device;
    }

    /** @return User which started this inventory by authenticating (or null if manual scan). */
    public UserEntity getUser()
    {
        return _user;
    }

    /** @return Access type (if started by a user) or Undefined (value of AccessType enumeration). */
    public AccessTypeEntity getAccessType()
    {
        return _accessType;
    }

    /** @return Door number (door which was closed, starting the scan) or -1 (default value, manual scan). */
    public byte getDoorNumber()
    {
        return _doorNumber;
    }

    /** @return Number of tags "Added" in the device, since the last inventory. */
    public int getTotalAdded()
    {
        return _totalAdded;
    }

    /** @return Number of tags still "Present" in the device, since the last inventory. */
    public int getTotalPresent()
    {
        return _totalPresent;
    }

    /** @return Number of tags "Removed" from the device, since the last inventory. */
    public int getTotalRemoved()
    {
        return _totalRemoved;
    }

    /** @return Completion date of the inventory. */
    public Date getCreatedAt()
    {
        return new Date(_createdAt.getTime());
    }

    /** @return List of InventoryRfidTag [many-to-many relationship table]. */
    public ForeignCollection<InventoryRfidTag> getRfidTags()
    {
        return _rfidTags;
    }

    /**
     * Build an Inventory instance from the Entity information.
     *
     * @return Instance of (SDK) Inventory.
     */
    public Inventory asInventory()
    {
        List<String> tagsAdded = new ArrayList<>();
        List<String> tagsPresent = new ArrayList<>();
        List<String> tagsRemoved = new ArrayList<>();

        for(InventoryRfidTag irtEntity : _rfidTags)
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

       return new Inventory(_id,
               tagsAdded,
               tagsPresent,
               tagsRemoved,
               _user != null ? _user.getUsername() : "",
               DaoAccessType.asAccessType(_accessType),
               _doorNumber,
               _createdAt);
    }
}
