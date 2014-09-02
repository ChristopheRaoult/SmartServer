package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * GrantedAccess Entity
 */
@DatabaseTable(tableName = GrantedAccessEntity.TABLE_NAME)
public class GrantedAccessEntity
{
    public static final String TABLE_NAME =  "sc_granted_access";

    public static final String ID = "id";
    public static final String DEVICE_ID = "device_id";
    public static final String GRANTED_USER_ID = "granted_user_id";
    public static final String GRANT_TYPE_ID = "grant_type_id";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(foreign = true, columnName = DEVICE_ID, canBeNull = false)
    private DeviceEntity _device;

    @DatabaseField(foreign = true, columnName = GRANTED_USER_ID, canBeNull = false)
    private GrantedUserEntity _grantedUser;

    @DatabaseField(foreign = true, columnName = GRANT_TYPE_ID, canBeNull = false)
    private GrantTypeEntity _grantType;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    GrantedAccessEntity()
    {
    }

    /**
     * Default constructor.
     * @param grantedUser   GrantedUserEntity instance to be set as user.
     * @param device        DeviceEntity to be set as device.
     * @param grantType     GrantTypeEntity to be set as grant type.
     */
    public GrantedAccessEntity(GrantedUserEntity grantedUser,
                               DeviceEntity device,
                               GrantTypeEntity grantType)
    {
        _grantedUser = grantedUser;
        _device = device;
        _grantType = grantType;
    }

    /**
     * @return GrantedUserEntity instance.
     */
    public GrantedUserEntity getGrantedUser()
    {
        return _grantedUser;
    }
}
