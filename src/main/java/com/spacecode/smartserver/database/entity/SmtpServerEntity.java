package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.sdk.network.alert.SmtpServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoSmtpServer;

/**
 * SmtpServer Entity
 */
@DatabaseTable(tableName = SmtpServerEntity.TABLE_NAME, daoClass = DaoSmtpServer.class)
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
     * Default constructor.
     *
     * @param address       SMTP server address.
     * @param port          SMTP server TCP port.
     * @param username      Username to access the SMTP server.
     * @param password      Password to access the SMTP server.
     * @param sslEnabled    True if SSL protocol should be used for authentication.
     */
    public SmtpServerEntity(String address, int port, String username, String password, boolean sslEnabled)
    {
        _device = DbManager.getDevEntity();
        _address = address;
        _port = port;
        _username = username;
        _password = password;
        _sslEnabled = sslEnabled;
    }

    /** @return DeviceEntity attached to this SmtpServerEntity. */
    public DeviceEntity getDevice()
    {
        return _device;
    }

    /** @return SMTP Server address. */
    public String getAddress()
    {
        return _address;
    }

    /** @return SMTP Server portnumber. */
    public int getPort()
    {
        return _port;
    }

    /** @return SMTP Server username credential. */
    public String getUsername()
    {
        return _username;
    }

    /** @return SMTP Server password credential. */
    public String getPassword()
    {
        return _password;
    }

    /** @return True if SSL shall be enabled, false otherwise. */
    public boolean isSslEnabled()
    {
        return _sslEnabled;
    }

    /**
     * Copy all values from an SmtpServer [SDK] instance.
     * @param smtpServer SmtpServer to take values from.
     */
    public void updateFrom(SmtpServer smtpServer)
    {
        _address = smtpServer.getAddress();
        _port = smtpServer.getPort();
        _username = smtpServer.getUsername();
        _password = smtpServer.getPassword();
        _sslEnabled = smtpServer.isSslEnabled();
    }
}
