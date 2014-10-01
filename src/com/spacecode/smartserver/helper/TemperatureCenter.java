package com.spacecode.smartserver.helper;

import com.spacecode.sdk.device.event.DeviceEventHandler;
import com.spacecode.sdk.device.event.TemperatureEventHandler;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.TemperatureMeasurementEntity;
import com.spacecode.smartserver.database.repository.Repository;

/**
 * Handle persistence of temperature measures in database.
 * New value will be inserted in database only if it's different from the previous one.
 *
 * Has to be initialized to subscribe to temperature event.
 */
public class TemperatureCenter
{
    private static double _lastValue = Double.MIN_VALUE;

    /** Must not be instantiated */
    private TemperatureCenter()
    {
    }

    /**
     * Check that device has been initialized. If it has, add a new listener for Temperature event.
     * @return True if subscribing to the temperature event succeeded. False otherwise.
     */
    public static boolean initialize()
    {
        if(DeviceHandler.getDevice() == null)
        {
            return false;
        }

        DeviceHandler.getDevice().addListener(new TemperatureMeasureHandler());
        return true;
    }

    private static class TemperatureMeasureHandler implements DeviceEventHandler, TemperatureEventHandler
    {
        @Override
        public void temperatureMeasure(double value)
        {
            // keep only one decimal place (ie 4.57 => 4.6 // 4.22 => 4.2)
            double roundedValue = (double) Math.round(value * 10) / 10;

            if(_lastValue == roundedValue)
            {
                return;
            }

            Repository<TemperatureMeasurementEntity> repo = DatabaseHandler
                    .getRepository(TemperatureMeasurementEntity.class);

            if(!repo.insert(
                    new TemperatureMeasurementEntity(DatabaseHandler.getDeviceConfiguration(), roundedValue)))
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
    }
}