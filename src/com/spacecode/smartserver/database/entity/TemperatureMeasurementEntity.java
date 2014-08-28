package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * TemperatureMeasurement Entity
 */
@DatabaseTable(tableName = "sc_temperature")
public class TemperatureMeasurementEntity
{
    public static final String ID = "id";
    public static final String VALUE = "value";
    public static final String CREATED_AT = "created_at";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

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
