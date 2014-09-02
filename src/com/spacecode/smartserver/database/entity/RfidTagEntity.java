package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * RfidTag Entity
 */
@DatabaseTable(tableName = RfidTagEntity.TABLE_NAME)
public class RfidTagEntity
{
    public static final String TABLE_NAME = "sc_rfid_tag";

    public static final String ID = "id";
    public static final String UID = "uid";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(columnName = UID, canBeNull = false)
    private String _uid;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    RfidTagEntity()
    {
    }
}
