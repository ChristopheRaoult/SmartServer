package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.smartserver.database.dao.DaoAuthentication;

import java.util.Date;

/**
 * Authentication Entity
 */
@DatabaseTable(tableName = AuthenticationEntity.TABLE_NAME, daoClass = DaoAuthentication.class)
public final class AuthenticationEntity extends Entity
{
    public static final String TABLE_NAME = "sc_authentication";

    public static final String DEVICE_ID = "device_id";
    public static final String USER_ID = "user_id";
    public static final String ACCESS_TYPE_ID = "access_type_id";
    public static final String CREATED_AT = "created_at";

    @DatabaseField(foreign = true, columnName = DEVICE_ID, canBeNull = false)
    private DeviceEntity _device;

    @DatabaseField(foreign = true, columnName = USER_ID, canBeNull = false, foreignAutoRefresh = true)
    private UserEntity _user;

    @DatabaseField(foreign = true, columnName = ACCESS_TYPE_ID, canBeNull = false, foreignAutoRefresh = true)
    private AccessTypeEntity _accessType;

    @DatabaseField(columnName = CREATED_AT, index = true)
    private Date _createdAt;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    AuthenticationEntity()
    {
    }

    /**
     * Default constructor.
     * @param device    Device which has been opened by User.
     * @param gte       User who opened the device.
     * @param ate       Access type value (fingerprint reader, badge reader...).
     */
    public AuthenticationEntity(DeviceEntity device, UserEntity gte, AccessTypeEntity ate)
    {
        _device = device;
        _user = gte;
        _accessType = ate;
        _createdAt = new Date();
    }

    /** @return DeviceEntity instance. */
    public DeviceEntity getDevice()
    {
        return _device;
    }

    /** @return UserEntity instance. */
    public UserEntity getUser()
    {
        return _user;
    }

    /** @return AccessTypeEntity instance. */
    public AccessTypeEntity getAccessType()
    {
        return _accessType;
    }

    /** @return Date the authentication was persisted. */
    public Date getCreatedAt()
    {
        return new Date(_createdAt.getTime());
    }
}
