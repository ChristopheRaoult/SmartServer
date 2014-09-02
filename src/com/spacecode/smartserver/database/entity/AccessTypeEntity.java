package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * AccessType Entity
 */
@DatabaseTable(tableName = AccessTypeEntity.TABLE_NAME)
public class AccessTypeEntity
{
    public static final String TABLE_NAME =  "sc_access_type";

    public static final String ID = "id";
    public static final String TYPE = "type";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(columnName = TYPE, canBeNull = false)
    private String _type;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    AccessTypeEntity()
    {
    }

    /**
     * Default constructor. Should only be used during database initialization to create constant values.
     * @param accessType    Value from SDK enumeration.
     */
    public AccessTypeEntity(String accessType)
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
