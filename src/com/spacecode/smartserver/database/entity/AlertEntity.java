package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.sdk.network.alert.Alert;
import com.spacecode.sdk.network.alert.AlertTemperature;
import com.spacecode.smartserver.database.repository.AlertTypeRepository;

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
     * Build an alert without defining cc/bcc recipients.
     * @param ate           AlertTypeEntity instance to be used as Alert Type.
     * @param de            DeviceEntity instance to be used as Device owning the alert.
     * @param to            List of email addresses (split with commas) to send the alert to.
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
     * Default constructor, full set of parameters.
     * @param ate           AlertTypeEntity instance to be used as Alert Type.
     * @param de            DeviceEntity instance to be used as Device owning the alert.
     * @param to            List of email addresses (split with commas) to send the alert to.
     * @param cc            List of "Cc" recipients (split with commas).
     * @param bcc           List of "Bcc" recipients (split with commas).
     * @param emailSubject  Subject of the email to be sent.
     * @param emailContent  Content of the email to be sent.
     * @param enabled       If false, the alert will not be used by SmartServer (AlertCenter).
     */
    public AlertEntity(AlertTypeEntity ate, DeviceEntity de, String to, String cc, String bcc,
                       String emailSubject, String emailContent, boolean enabled)
    {
        _alertType = ate;
        _device = de;
        _toList = to == null ? "" : to;
        _ccList = cc == null ? "" : cc;
        _bccList = bcc == null ? "" : bcc;
        _emailSubject = emailSubject == null ? "" : emailSubject;
        _emailContent = emailContent == null ? "" : emailContent;
        _enabled = enabled;
    }

    /**
     * Build an AlertEntity from Alert [SDK] values.
     * Also copy Id value, which is useful for methods like "createOrUpdate" (ORMLite dao).
     *
     * @param ate   AlertTypeEntity instance to be used as Alert Type.
     * @param de    DeviceEntity instance to be used as Device owning the alert.
     * @param alert Alert [SDK] instance to be used as source (recipients list, is enabled, etc).
     */
    public AlertEntity(AlertTypeEntity ate, DeviceEntity de, Alert alert)
    {
        this(ate, de, alert.getToList(), alert.getCcList(), alert.getBccList(),
                alert.getEmailSubject(), alert.getEmailContent(), alert.isEnabled());

        _id = alert.getId();
    }

    /**
     * @return AlertTypeEntity instance.
     */
    public AlertTypeEntity getAlertType()
    {
        return _alertType;
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

    /**
     * @param ae    AlertEntity to be "converted".
     * @return      An equivalent "Alert" [SDK]
     */
    public static Alert toAlert(AlertEntity ae)
    {
        return new Alert(ae.getId(),
                AlertTypeRepository.toAlertType(ae.getAlertType()),
                ae._toList,
                ae._ccList,
                ae._bccList,
                ae._emailSubject, ae._emailContent, ae._enabled);
    }

    /**
     * @param ate   AlertTemperatureEntity to be "converted".
     * @return      An equivalent "AlertTemperature" [SDK]
     */
    public static Alert toAlert(AlertTemperatureEntity ate)
    {
        AlertEntity ae = ate.getAlert();

        return new AlertTemperature(ae.getId(),
                ae._toList,
                ae._ccList,
                ae._bccList,
                ae._emailSubject, ae._emailContent, ae._enabled,
                ate.getTemperatureMin(), ate.getTemperatureMax());
    }
}
