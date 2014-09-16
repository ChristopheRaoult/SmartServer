package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * SmtpServer Entity
 */
@DatabaseTable(tableName = SmtpServerEntity.TABLE_NAME)
public final class SmtpServerEntity extends Entity
{
    public static final String TABLE_NAME = "sc_smtp_server";

    public static final String DEVICE_ID = "device_id";
    public static final String ADDRESS = "address";
    public static final String PORT = "port";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String SSL_ENABLED = "ssl_enabled";

    @DatabaseField(foreign = true, columnName = DEVICE_ID, canBeNull = false)
    private DeviceEntity _device;

    @DatabaseField(columnName = ADDRESS, canBeNull = false)
    private String _address;

    @DatabaseField(columnName = PORT, canBeNull = false)
    private int _port;

    @DatabaseField(columnName = USERNAME, canBeNull = false)
    private String _username;

    @DatabaseField(columnName = PASSWORD, canBeNull = false)
    private String _password;

    @DatabaseField(columnName = SSL_ENABLED, canBeNull = false)
    private boolean _sslEnabled;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    SmtpServerEntity()
    {
    }

    /**
     * @return SMTP Server address.
     */
    public String getAddress()
    {
        return _address;
    }

    /**
     * @return SMTP Server portnumber.
     */
    public int getPort()
    {
        return _port;
    }

    /**
     * @return SMTP Server username credential.
     */
    public String getUsername()
    {
        return _username;
    }

    /**
     * @return SMTP Server password credential.
     */
    public String getPassword()
    {
        return _password;
    }

    /**
     * @return True if SSL shall be enabled, false otherwise.
     */
    public boolean isSslEnabled()
    {
        return _sslEnabled;
    }
}
