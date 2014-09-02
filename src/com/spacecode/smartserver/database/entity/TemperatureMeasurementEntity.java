package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * TemperatureMeasurement Entity
 */
@DatabaseTable(tableName = TemperatureMeasurementEntity.TABLE_NAME)
public class TemperatureMeasurementEntity
{
    public static final String TABLE_NAME = "sc_temperature";

    public static final String ID = "id";
    public static final String DEVICE_ID = "device_id";
    public static final String VALUE = "value";
    public static final String CREATED_AT = "created_at";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(foreign = true, columnName = DEVICE_ID, canBeNull = false)
    private DeviceEntity _device;

    @DatabaseField(columnName = VALUE, canBeNull = false)
    private double _uid;

    @DatabaseField(columnName = CREATED_AT)
    private Date _createdAt;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    TemperatureMeasurementEntity()
    {
    }
}
