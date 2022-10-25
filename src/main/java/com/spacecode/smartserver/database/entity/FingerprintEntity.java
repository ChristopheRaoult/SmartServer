package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.smartserver.database.dao.DaoFingerprint;

/**
 * Fingerprint Entity
 */
@DatabaseTable(tableName = FingerprintEntity.TABLE_NAME, daoClass = DaoFingerprint.class)
public final class FingerprintEntity extends Entity
{
    public static final String TABLE_NAME = "sc_fingerprint";

    public static final String USER_ID = "user_id";
    public static final String FINGER_INDEX = "finger_index";
    public static final String TEMPLATE = "template";

    @DatabaseField(foreign = true, columnName = USER_ID, canBeNull = false)
    private UserEntity _user;

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
    public FingerprintEntity(UserEntity user, int index, String template)
    {
        _user = user;
        _fingerIndex = index;
        _template = template;
    }

    /** @return UserEntity attached to this fingerprint. */
    public UserEntity getUser()
    {
        return _user;
    }

    /** @return FingerIndex's index value (0 to 9, see FingerIndex enum). */
    public int getFingerIndex()
    {
        return _fingerIndex;
    }

    /** @return Base64 encoded fingerprint template. */
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
