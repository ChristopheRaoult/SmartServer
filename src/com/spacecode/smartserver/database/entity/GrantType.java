package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * GrantType Entity
 */
@DatabaseTable(tableName = "sc_granttype")
public class GrantType
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
    GrantType()
    {
    }

    /**
     * Default constructor. Should only be used during database initialization to create constant values.
     * @param userGrant     Value from SDK enumeration.
     */
    public GrantType(String userGrant)
    {
        _type = userGrant;
    }

    /**
     * Get value of the grant type.
     * @return String value of the grant type.
     */
    public String getType()
    {
        return _type;
    }
}
