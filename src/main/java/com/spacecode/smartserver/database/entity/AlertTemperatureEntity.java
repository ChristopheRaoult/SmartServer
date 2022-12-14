package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.sdk.network.alert.AlertTemperature;
import com.spacecode.smartserver.database.dao.DaoAlertTemperature;

/**
 * AlertTemperature Entity
 */
@DatabaseTable(tableName = AlertTemperatureEntity.TABLE_NAME, daoClass = DaoAlertTemperature.class)
public final class AlertTemperatureEntity extends Entity
{
    public static final String TABLE_NAME = "sc_alert_temperature";

    public static final String ALERT_ID = "alert_id";
    public static final String TEMPERATURE_MIN = "temperature_min";
    public static final String TEMPERATURE_MAX = "temperature_max";

    @DatabaseField(foreign = true, columnName = ALERT_ID, canBeNull = false, foreignAutoRefresh = true)
    private AlertEntity _alert;

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

    /**
     * Default constructor.
     *
     * @param newAlertEntity    AlertEntity (contains general information) attached to this
     *                          temperature alert.
     * @param at                AlertTemperature [SDK] instance to take values from.
     */
    public AlertTemperatureEntity(AlertEntity newAlertEntity, AlertTemperature at)
    {
        _temperatureMin = at.getTemperatureMin();
        _temperatureMax = at.getTemperatureMax();
        _alert = newAlertEntity;
    }

    /** @return Attached Alert (entity). */
    public AlertEntity getAlert()
    {
        return _alert;
    }

    /** @return Min. temperature threshold. */
    public double getTemperatureMin()
    {
        return _temperatureMin;
    }

    /** @return Max. temperature threshold. */
    public double getTemperatureMax()
    {
        return _temperatureMax;
    }

    /**
     * Allow updating the min. temperature threshold.
     * @param temperatureMin New min. value.
     */
    public void setTemperatureMin(double temperatureMin)
    {
        _temperatureMin = temperatureMin;
    }

    /**
     * Allow updating the max. temperature threshold.
     * @param temperatureMax New max. value.
     */
    public void setTemperatureMax(double temperatureMax)
    {
        _temperatureMax = temperatureMax;
    }
}
