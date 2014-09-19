package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * DeviceConfiguration Entity
 */
@DatabaseTable(tableName = DeviceEntity.TABLE_NAME)
public final class DeviceEntity extends Entity
{
    public static final String TABLE_NAME = "sc_device";

    public static final String SERIAL_NUMBER = "serial_number";
    public static final String NB_BADGE_READER = "nb_badge_reader";
    public static final String FP_READER_MASTER = "fp_reader_master";
    public static final String FP_READER_SLAVE = "fp_reader_slave";
    public static final String TEMPERATURE_ENABLED = "temperature_enabled";

    @DatabaseField(unique = true, columnName = SERIAL_NUMBER, canBeNull = false)
    private String _serialNumber;

    @DatabaseField(columnName = NB_BADGE_READER, canBeNull = false)
    private int _nbOfBadgeReader;

    @DatabaseField(columnName = FP_READER_MASTER, canBeNull = false)
    private String _fpReaderMasterSerial;

    @DatabaseField(columnName = FP_READER_SLAVE, canBeNull = false)
    private String _fpReaderSlaveSerial;

    @DatabaseField(columnName = TEMPERATURE_ENABLED, canBeNull = false)
    private boolean _temperatureEnabled;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    DeviceEntity()
    {
    }

    /**
     * @return Device's serial number.
     */
    public String getSerialNumber()
    {
        return _serialNumber;
    }

    /**
     * @return Number of Badge Readers to be connected.
     */
    public int getNbOfBadgeReader()
    {
        return _nbOfBadgeReader;
    }

    /**
     * @return Serial Number of the Master Fingerprint Reader.
     */
    public String getFpReaderMasterSerial()
    {
        return _fpReaderMasterSerial;
    }

    /**
     * @return Serial Number of the Slave Fingerprint Reader.
     */
    public String getFpReaderSlaveSerial()
    {
        return _fpReaderSlaveSerial;
    }

    /**
     * @return True if temperature probe is enabled, false otherwise.
     */
    public boolean isTemperatureEnabled()
    {
        return _temperatureEnabled;
    }
}
