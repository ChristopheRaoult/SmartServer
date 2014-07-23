package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Fingerprint Entity
 */
@DatabaseTable(tableName = "sc_fingerprint")
public class Fingerprint
{
    public static final String ID = "id";
    public static final String GRANTED_USER_ID = "granted_user_id";
    public static final String FINGER_INDEX = "finger_index";
    public static final String TEMPLATE = "template";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(foreign = true, columnName = GRANTED_USER_ID, canBeNull = false)
    private GrantedUser _grantedUser;

    @DatabaseField(columnName = FINGER_INDEX, canBeNull = false)
    private int _fingerIndex;

    @DatabaseField(columnName = TEMPLATE, canBeNull = false)
    private String _template;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    Fingerprint()
    {
    }
}
