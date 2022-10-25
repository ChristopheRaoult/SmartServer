package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.smartserver.database.dao.DaoRfidTag;

/**
 * RfidTag Entity
 */
@DatabaseTable(tableName = RfidTagEntity.TABLE_NAME, daoClass = DaoRfidTag.class)
public final class RfidTagEntity extends Entity
{
    public static final String TABLE_NAME = "sc_rfid_tag";

    public static final String UID = "uid";

    @DatabaseField(columnName = UID, canBeNull = false, unique = true, width = 120)
    private String _uid;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    RfidTagEntity()
    {
    }

    /**
     * Default constructor.
     * @param uid   RFID Tag unique identifier.
     */
    public RfidTagEntity(String uid)
    {
        _uid = uid;
    }

    /** @return RFID Tag Unique Identifier. */
    public String getUid()
    {
        return _uid;
    }
}
