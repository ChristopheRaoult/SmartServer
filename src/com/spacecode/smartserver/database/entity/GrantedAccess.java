package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * GrantedAccess Entity
 */
@DatabaseTable(tableName = "sc_granted_access")
public class GrantedAccess
{
    public static final String ID = "id";
    public static final String GRANTED_USER_ID = "granted_user_id";
    public static final String GRANT_TYPE_ID = "grant_type_id";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(foreign = true, columnName = GRANTED_USER_ID, canBeNull = false)
    private GrantedUser _grantedUser;

    @DatabaseField(foreign = true, columnName = GRANT_TYPE_ID, canBeNull = false)
    private GrantType _grantType;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    GrantedAccess()
    {
    }
}
