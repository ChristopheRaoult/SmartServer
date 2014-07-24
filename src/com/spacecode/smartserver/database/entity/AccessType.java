package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * AccessType Entity
 */
@DatabaseTable(tableName = "sc_access_type")
public class AccessType
{
    public static final String ID = "id";
    public static final String TYPE = "type";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(columnName = TYPE, canBeNull = false)
    private String _type;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    AccessType()
    {
    }

    /**
     * Default constructor. Should only be used during database initialization to create constant values.
     * @param accessType    Value from SDK enumeration.
     */
    public AccessType(String accessType)
    {
        _type = accessType;
    }

    /**
     * Get value of the access type.
     * @return String value of the access type.
     */
    public String getType()
    {
        return _type;
    }
}
