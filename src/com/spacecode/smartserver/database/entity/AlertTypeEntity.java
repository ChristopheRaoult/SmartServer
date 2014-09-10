package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * AlertType Entity
 */
@DatabaseTable(tableName = AlertTypeEntity.TABLE_NAME)
public final class AlertTypeEntity extends Entity
{
    public static final String TABLE_NAME = "sc_alert_type";

    public static final String TYPE = "type";

    // alert types
    public static final String DEVICE_DISCONNECTED = "DEVICE DISCONNECTED";
    public static final String DOOR_DELAY = "DOOR DELAY";
    public static final String TEMPERATURE = "TEMPERATURE";
    public static final String THIEF_FINGER = "THIEF FINGER";

    @DatabaseField(columnName = TYPE, canBeNull = false)
    private String _type;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    AlertTypeEntity()
    {
    }

    /**
     * Default constructor. Should only be used during database initialization to create constant values.
     * @param alertType     Value (constant) describing Alert Type.
     */
    public AlertTypeEntity(String alertType)
    {
        _type = alertType;
    }

    /**
     * @return String value of the alert type.
     */
    public String getType()
    {
        return _type;
    }
}
