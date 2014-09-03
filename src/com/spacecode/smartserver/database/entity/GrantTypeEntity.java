package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * GrantType Entity
 */
@DatabaseTable(tableName = GrantTypeEntity.TABLE_NAME)
public class GrantTypeEntity
{
    public static final String TABLE_NAME = "sc_grant_type";

    public static final String ID = "id";
    public static final String TYPE = "type";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(columnName = TYPE, canBeNull = false)
    private String _type;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    GrantTypeEntity()
    {
    }

    /**
     * Default constructor. Should only be used during database initialization to create constant values.
     * @param userGrant     Value from SDK enumeration.
     */
    public GrantTypeEntity(String userGrant)
    {
        _type = userGrant;
    }

    public int getId()
    {
        return _id;
    }

    /**
     * @return String value of the grant type.
     */
    public String getType()
    {
        return _type;
    }
}
