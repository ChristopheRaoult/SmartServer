package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * AlertTemperature Entity
 */
@DatabaseTable(tableName = AlertTemperatureEntity.TABLE_NAME)
public final class AlertTemperatureEntity extends Entity
{
    public static final String TABLE_NAME = "sc_alert_temperature";

    public static final String TEMPERATURE_MIN = "temperature_min";
    public static final String TEMPERATURE_MAX = "temperature_max";

    @DatabaseField(columnName = TEMPERATURE_MIN, canBeNull = false)
    private double _temperatureMin;

    @DatabaseField(columnName = TEMPERATURE_MAX, canBeNull = false)
    private double _temperatureMax;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    AlertTemperatureEntity()
    {
    }
}
