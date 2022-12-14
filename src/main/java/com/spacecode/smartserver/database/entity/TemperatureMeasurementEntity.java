package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoTemperatureMeasurement;

import java.util.Date;

/**
 * TemperatureMeasurement Entity
 */
@DatabaseTable(tableName = TemperatureMeasurementEntity.TABLE_NAME, daoClass = DaoTemperatureMeasurement.class)
public final class TemperatureMeasurementEntity extends Entity
{
    public static final String TABLE_NAME = "sc_temperature";

    public static final String DEVICE_ID = "device_id";
    public static final String VALUE = "value";
    public static final String CREATED_AT = "created_at";

    @DatabaseField(foreign = true, columnName = DEVICE_ID, canBeNull = false)
    private DeviceEntity _device;

    @DatabaseField(columnName = VALUE, canBeNull = false)
    private double _value;

    @DatabaseField(columnName = CREATED_AT, index = true)
    private Date _createdAt;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    TemperatureMeasurementEntity()
    {
    }

    /**
     * Default constructor.
     * @param value     Measure in °C.
     */
    public TemperatureMeasurementEntity(double value)
    {
        _device = DbManager.getDevEntity();
        _value = value;
        _createdAt = new Date();
    }

    /** @return Device who took the measure. */
    public DeviceEntity getDevice()
    {
        return _device;
    }

    /** @return Temperature measure value. */
    public double getValue()
    {
        return _value;
    }

    /** @return Creation date of the measurement. */
    public Date getCreatedAt()
    {
        return new Date(_createdAt.getTime());
    }
}
