package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoGrantedAccess;

/**
 * GrantedAccess Entity
 */
@DatabaseTable(tableName = GrantedAccessEntity.TABLE_NAME, daoClass = DaoGrantedAccess.class)
public final class GrantedAccessEntity extends Entity
{
    public static final String TABLE_NAME =  "sc_granted_access";

    public static final String DEVICE_ID = "device_id";
    public static final String USER_ID = "user_id";
    public static final String GRANT_TYPE_ID = "grant_type_id";

    @DatabaseField(foreign = true, columnName = DEVICE_ID, canBeNull = false)
    private DeviceEntity _device;

    @DatabaseField(foreign = true, columnName = USER_ID, canBeNull = false, foreignAutoRefresh = true)
    private UserEntity _user;

    @DatabaseField(foreign = true, columnName = GRANT_TYPE_ID, canBeNull = false, foreignAutoRefresh = true)
    private GrantTypeEntity _grantType;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    GrantedAccessEntity()
    {
    }

    /**
     * Default constructor.
     * @param user      UserEntity instance to be set as user.
     * @param grantType GrantTypeEntity to be set as grant type.
     */
    public GrantedAccessEntity(UserEntity user,
                               GrantTypeEntity grantType)
    {
        _user = user;
        _device = DbManager.getDevEntity();
        _grantType = grantType;
    }

    /** @return UserEntity instance (attached user). */
    public UserEntity getUser()
    {
        return _user;
    }

    /** @return DeviceEntity instance (attached device). */
    public DeviceEntity getDevice()
    {
        return _device;
    }

    /** @return GrantTypeEntity instance (attached GrantTypeEntity). */
    public GrantTypeEntity getGrantType()
    {
        return _grantType;
    }
}
