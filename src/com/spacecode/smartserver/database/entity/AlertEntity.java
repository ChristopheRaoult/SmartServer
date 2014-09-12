package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Alert Entity
 */
@DatabaseTable(tableName = AlertEntity.TABLE_NAME)
public final class AlertEntity extends Entity
{
    public static final String TABLE_NAME = "sc_alert";

    public static final String ALERT_TYPE_ID = "alert_type_id";
    public static final String DEVICE_ID = "device_id";
    public static final String ENABLED = "enabled";
    public static final String TO_LIST = "to_list";
    public static final String CC_LIST = "cc_list";
    public static final String BCC_LIST = "bcc_list";
    public static final String EMAIL_SUBJECT = "email_subject";
    public static final String EMAIL_CONTENT = "email_content";

    @DatabaseField(foreign = true, columnName = ALERT_TYPE_ID, canBeNull = false)
    private AlertTypeEntity _alertType;

    @DatabaseField(foreign = true, columnName = DEVICE_ID, canBeNull = false)
    private DeviceEntity _device;

    @DatabaseField(columnName = ENABLED, canBeNull = false)
    private boolean _enabled;

    @DatabaseField(columnName = TO_LIST, canBeNull = false)
    private String _toList;

    @DatabaseField(columnName = CC_LIST)
    private String _ccList;

    @DatabaseField(columnName = BCC_LIST)
    private String _bccList;

    @DatabaseField(columnName = EMAIL_SUBJECT, canBeNull = false)
    private String _emailSubject;

    @DatabaseField(columnName = EMAIL_CONTENT, canBeNull = false)
    private String _emailContent;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    AlertEntity()
    {
    }
}
