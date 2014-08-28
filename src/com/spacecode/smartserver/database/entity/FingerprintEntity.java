package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Fingerprint Entity
 */
@DatabaseTable(tableName = "sc_fingerprint")
public class FingerprintEntity
{
    public static final String ID = "id";
    public static final String GRANTED_USER_ID = "granted_user_id";
    public static final String FINGER_INDEX = "finger_index";
    public static final String TEMPLATE = "template";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(foreign = true, columnName = GRANTED_USER_ID, canBeNull = false)
    private GrantedUserEntity _grantedUser;

    @DatabaseField(columnName = FINGER_INDEX, canBeNull = false)
    private int _fingerIndex;

    @DatabaseField(columnName = TEMPLATE, canBeNull = false)
    private String _template;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    FingerprintEntity()
    {
    }

    /**
     * Default constructor.
     * @param user      User entity to be attached to the Fingerprint entity.
     * @param index     FingerIndex's index value.
     * @param template  Base64 encoded fingerprint template.
     */
    public FingerprintEntity(GrantedUserEntity user, int index, String template)
    {
        _grantedUser = user;
        _fingerIndex = index;
        _template = template;
    }
}
