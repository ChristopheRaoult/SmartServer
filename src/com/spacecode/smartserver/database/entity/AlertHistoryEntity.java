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
    public static final String EXTRA_DATA = "extra_data";
    public static final String CREATED_AT = "created_at";

    @DatabaseField(foreign = true, columnName = ALERT_ID, canBeNull = false, foreignAutoRefresh = true)
    private AlertEntity _alert;

    @DatabaseField(columnName = EXTRA_DATA)
    private String _extraData;

    @DatabaseField(columnName = CREATED_AT, canBeNull = false, index = true)
    private Date _createdAt;

    /** No-Arg constructor (with package visibility) for ORMLite. */
    AlertHistoryEntity()
    {
    }

    /**
     * Default constructor.
     * @param alert     Alert just raised.
     * @param extraData Additional data provided with the alert report (Username, Temperature...).
     */
    public AlertHistoryEntity(AlertEntity alert, String extraData)
    {
        _alert = alert;
        _extraData = extraData;
        _createdAt = new Date();
    }

    /** @return Attached entity. */
    public AlertEntity getAlert()
    {
        return _alert;
    }

    /** @return Creation date. */
    public Date getCreatedAt()
    {
        return new Date(_createdAt.getTime());
    }

    /** @return Additional data added to the alert report as a valuable info. */
    public String getExtraData()
    {
        return _extraData != null ? _extraData : "";
    }
}
