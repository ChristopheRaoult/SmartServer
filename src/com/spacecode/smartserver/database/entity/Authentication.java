package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * Authentication Entity
 */
@DatabaseTable(tableName = "sc_authentication")
public class Authentication
{
    public static final String ID = "id";
    public static final String GRANTED_USER_ID = "granted_user_id";
    public static final String ACCESS_TYPE_ID = "access_type_id";
    public static final String CREATED_AT = "created_at";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(foreign = true, columnName = GRANTED_USER_ID, canBeNull = false)
    private GrantedUser _grantedUser;

    @DatabaseField(foreign = true, columnName = ACCESS_TYPE_ID, canBeNull = false)
    private AccessType _accessType;

    @DatabaseField(columnName = CREATED_AT)
    private Date _createdAt;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    Authentication()
    {
    }
}
