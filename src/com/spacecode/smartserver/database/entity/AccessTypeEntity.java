package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.smartserver.database.dao.DaoAccessType;

/**
 * AccessType Entity
 */
@DatabaseTable(tableName = AccessTypeEntity.TABLE_NAME, daoClass = DaoAccessType.class)
public final class AccessTypeEntity extends Entity
{
    public static final String TABLE_NAME =  "sc_access_type";

    public static final String TYPE = "type";

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
