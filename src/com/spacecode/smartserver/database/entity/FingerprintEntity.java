package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Fingerprint Entity
 */
@DatabaseTable(tableName = FingerprintEntity.TABLE_NAME)
public final class FingerprintEntity extends Entity
{
    public static final String TABLE_NAME = "sc_fingerprint";

    public static final String GRANTED_USER_ID = "granted_user_id";
    public static final String FINGER_INDEX = "finger_index";
    public static final String TEMPLATE = "template";

    @DatabaseField(foreign = true, columnName = GRANTED_USER_ID, canBeNull = false)
    private GrantedUserEntity _grantedUser;

    @DatabaseField(columnName = FINGER_INDEX, canBeNull = false)
    private int _fingerIndex;

    @DatabaseField(columnName = TEMPLATE, dataType = DataType.LONG_STRING, canBeNull = false)
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

    /**
     * @return GrantedUserEntity attached to this fingerprint.
     */
    public GrantedUserEntity getGrantedUser()
    {
        return _grantedUser;
    }

    /**
     * @return FingerIndex's index value (0 to 9, see FingerIndex enum).
     */
    public int getFingerIndex()
    {
        return _fingerIndex;
    }

    /**
     * @return Base64 encoded fingerprint template.
     */
    public String getTemplate()
    {
        return _template;
    }

    /**
     * Update base64 encoded template value.
     * Used when updating an already existing fingerprint.
     * @param template Base64 encoded fingerprint template.
     */
    public void setTemplate(String template)
    {
        _template = template;
    }
}
