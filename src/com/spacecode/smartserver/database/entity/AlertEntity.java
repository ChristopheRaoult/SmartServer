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

    @DatabaseField(foreign = true, columnName = ALERT_TYPE_ID, canBeNull = false, foreignAutoRefresh = true)
    private AlertTypeEntity _alertType;

    @DatabaseField(foreign = true, columnName = DEVICE_ID, canBeNull = false)
    private DeviceEntity _device;

    @DatabaseField(columnName = TO_LIST, canBeNull = false)
    private String _toList;

    @DatabaseField(columnName = CC_LIST, canBeNull = false)
    private String _ccList;

    @DatabaseField(columnName = BCC_LIST, canBeNull = false)
    private String _bccList;

    @DatabaseField(columnName = EMAIL_SUBJECT, canBeNull = false)
    private String _emailSubject;

    @DatabaseField(columnName = EMAIL_CONTENT, canBeNull = false)
    private String _emailContent;

    @DatabaseField(columnName = ENABLED, canBeNull = false)
    private boolean _enabled;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    AlertEntity()
    {
    }

    /**
     * Build an alert without cc/bcc recipients.
     * @param ate           AlertTypeEntity instance to be used as Alert Type.
     * @param de            DeviceEntity instance to be used as Device owning the alert.
     * @param to            List of email addresses (splitted with commas) to send the alert to.
     * @param emailSubject  Subject of the email to be sent.
     * @param emailContent  Content of the email to be sent.
     * @param enabled       If false, the alert will not be used by SmartServer (AlertCenter).
     */
    public AlertEntity(AlertTypeEntity ate, DeviceEntity de, String to,
                       String emailSubject, String emailContent, boolean enabled)
    {
        this(ate, de, to, "", "", emailSubject, emailContent, enabled);
    }

    /**
     *
     * @param ate           AlertTypeEntity instance to be used as Alert Type.
     * @param de            DeviceEntity instance to be used as Device owning the alert.
     * @param to            List of email addresses (splitted with commas) to send the alert to.
     * @param cc            List of "Cc" recipients (splitted with commas).
     * @param bcc           List of "Bcc" recipients (splitted with commas).
     * @param emailSubject  Subject of the email to be sent.
     * @param emailContent  Content of the email to be sent.
     * @param enabled       If false, the alert will not be used by SmartServer (AlertCenter).
     */
    public AlertEntity(AlertTypeEntity ate, DeviceEntity de, String to, String cc, String bcc,
                       String emailSubject, String emailContent, boolean enabled)
    {
        _alertType = ate;
        _device = de;
        _toList = to;
        _ccList = cc;
        _bccList = bcc;
        _emailSubject = emailSubject;
        _emailContent = emailContent;
        _enabled = enabled;
    }

    /**
     * @return Email (to be sent) subject.
     */
    public String getEmailSubject()
    {
        return _emailSubject;
    }

    /**
     * @return Email (to be sent) content.
     */
    public String getEmailContent()
    {
        return _emailContent;
    }

    /**
     * @return List of "TO" recipients, splitted by commas.
     */
    public String getToList()
    {
        return _toList;
    }

    /**
     * @return List of "CC" recipients, splitted by commas (warning: may be null).
     */
    public String getCcList()
    {
        return _ccList;
    }

    /**
     * @return List of "BCC" recipients, splitted by commas (warning: may be null).
     */
    public String getBccList()
    {
        return _bccList;
    }

    /**
     * @return True if alert is set as "Enabled", false otherwise.
     */
    public boolean isEnabled()
    {
        return _enabled;
    }
}
