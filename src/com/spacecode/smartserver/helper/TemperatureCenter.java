package com.spacecode.smartserver.helper;

import com.spacecode.sdk.device.data.DeviceStatus;
import com.spacecode.sdk.device.event.DeviceEventHandler;
import com.spacecode.sdk.device.event.TemperatureEventHandler;
import com.spacecode.sdk.device.module.TemperatureProbe;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoTemperatureMeasurement;
import com.spacecode.smartserver.database.entity.TemperatureMeasurementEntity;

/**
 * Handle persistence of temperature measures in database.
 * New value will be inserted in database only if it's different from the previous one.
 *
 * Has to be initialized to subscribe to temperature event.
 */
public class TemperatureCenter
{
    private static double _lastValue = TemperatureProbe.ERROR_VALUE;

    /** Must not be instantiated */
    private TemperatureCenter()
    {
    }

    /**
     * Add an events listener for Temperature events.
     */
    public static void initialize()
    {
        if(ConfManager.isDevTemperature())
        {
            DeviceHandler.getDevice().addListener(new TemperatureMeasureHandler());
        }
    }

    private static class TemperatureMeasureHandler implements DeviceEventHandler, TemperatureEventHandler
    {
        @Override
        public void temperatureMeasure(double value)
        {
            if(value == TemperatureProbe.ERROR_VALUE)
            {
                return;
            }
            
            // keep only one decimal place (ie 4.57 => 4.6 // 4.22 => 4.2)
            double roundedValue = (double) Math.round(value * 10) / 10;

            if(Math.abs(_lastValue - roundedValue) < 0.01)
            {
                return;
            }

            DaoTemperatureMeasurement daoTempMeasurement = 
                    (DaoTemperatureMeasurement) DbManager.getDao(TemperatureMeasurementEntity.class);

            if(!daoTempMeasurement.insert(new TemperatureMeasurementEntity(roundedValue)))
            {
                SmartLogger.getLogger().severe("Unable to insert new temperature measure.");
                return;
            }

            _lastValue = roundedValue;
        }

        @Override
        public void deviceDisconnected()
        {
            // not needed
        }

        @Override
        public void deviceStatusChanged(DeviceStatus status)
        {
            // not needed
        }
    }
}
