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

    @DatabaseField(unique = true, columnName = SERIAL_NUMBER, canBeNull = false)
    private String _serialNumber;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    DeviceEntity()
    {
    }

    public DeviceEntity(String serialNumber)
    {
        _serialNumber = serialNumber;
    }

    /** @return Device's serial number. */
    public String getSerialNumber()
    {
        return _serialNumber;
    }
}
