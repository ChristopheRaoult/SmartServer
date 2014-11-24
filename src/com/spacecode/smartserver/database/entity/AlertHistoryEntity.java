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
    public static final String CREATED_AT = "created_at";

    @DatabaseField(foreign = true, columnName = ALERT_ID, canBeNull = false, foreignAutoRefresh = true)
    private AlertEntity _alert;

    @DatabaseField(columnName = CREATED_AT, canBeNull = false)
    private Date _createdAt;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    AlertHistoryEntity()
    {
    }

    /**
     * Default constructor.
     * @param alert Alert just raised.
     */
    public AlertHistoryEntity(AlertEntity alert)
    {
        _alert = alert;
        _createdAt = new Date();
    }

    /** @return Attached entity. */
    public AlertEntity getAlert()
    {
        return _alert;
    }
}
