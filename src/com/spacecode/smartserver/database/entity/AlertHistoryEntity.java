package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * AlertHistory Entity
 */
@DatabaseTable(tableName = AlertHistoryEntity.TABLE_NAME)
public final class AlertHistoryEntity extends Entity
{
    public static final String TABLE_NAME = "sc_alert_history";

    public static final String ALERT_ID = "alert_id";
    public static final String DEVICE_ID = "device_id";
    public static final String CREATED_AT = "created_at";

    @DatabaseField(foreign = true, columnName = ALERT_ID, canBeNull = false)
    private AlertEntity _alert;

    @DatabaseField(foreign = true, columnName = DEVICE_ID, canBeNull = false)
    private DeviceEntity _device;

    @DatabaseField(columnName = CREATED_AT, canBeNull = false)
    private Date _createdAt;
    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    AlertHistoryEntity()
    {
    }
}
