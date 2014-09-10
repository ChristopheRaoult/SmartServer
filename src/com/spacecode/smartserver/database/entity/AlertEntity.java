package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.table.DatabaseTable;

/**
 * AlertHistory Entity
 */
@DatabaseTable(tableName = AlertEntity.TABLE_NAME)
public final class AlertEntity extends Entity
{
    public static final String TABLE_NAME = "sc_alert";

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    AlertEntity()
    {
    }
}
