package com.spacecode.smartserver.helper;

import com.spacecode.sdk.device.event.TemperatureEventHandler;
import com.spacecode.sdk.device.module.data.ProbeSettings;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoTemperatureMeasurement;
import com.spacecode.smartserver.database.entity.TemperatureMeasurementEntity;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Handle persistence of temperature measures in database.
 * New value will be inserted in database only if it's different from the previous one.
 *
 * Has to be initialized to subscribe to temperature event.
 */
public class TemperatureCenter
{
    private static Date _lastMeasureTime;
    
    // timer doing a periodic measure, regardless the settings (delay/delta) of the Probe 
    private static Timer _measurementTimer;
    // delay of this periodic measure, in milliseconds
    private static final long DELAY_MS_FORCE_MEASURE = 10 * 60 * 1000;

    /** Must not be instantiated */
    private TemperatureCenter()
    {
    }

    /**
     * <ul>
     *     <li>Add an events listener for Temperature events.</li>
     *     <li>Start a timer which periodically records a temperature measure.</li>
     * </ul>
     */
    public static void initialize()
    {
        if(!ConfManager.isDevTemperature())
        {
            return;
        }
        
        // listen for temperature events
        DeviceHandler.getDevice().addListener(new TemperatureMeasureHandler());
        
        // set up a timer to force recording at least one measure every DELAY_MS_FORCE_MEASURE milliseconds
        _measurementTimer = new Timer();
        _measurementTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                if(!DeviceHandler.isAvailable())
                {
                    return;
                }
                
                double currentTemp = DeviceHandler.getDevice().getCurrentTemperature();
                
                if(currentTemp == ProbeSettings.ERROR_VALUE)
                {
                    return;
                }
                
                Date dateNow = new Date();
                
                if(_lastMeasureTime == null || (dateNow.getTime() - _lastMeasureTime.getTime()) < DELAY_MS_FORCE_MEASURE)
                {
                    recordNewMeasure(currentTemp);
                }
                
            }
        }, 0, DELAY_MS_FORCE_MEASURE);
    }

    /**
     * Cancel the timer responsible for periodically recording a temperature measure.
     */
    public static void stop()
    {
        if(!ConfManager.isDevTemperature())
        {
            return;
        }
        
        if(_measurementTimer != null)
        {
            _measurementTimer.cancel();
        }
    }

    /**
     * Round the given measure to one decimal digit, update the "last measure value", insert it in DB.
     * If DB insertion succeeds, update the "last measure time".
     * 
     * @param valueFromProbe Measure given by the TemperatureProbe.
     */
    private static void recordNewMeasure(double valueFromProbe)
    {
        // keep only one decimal place (ie 4.57 => 4.6 // 4.22 => 4.2)
        double roundedValue = (double) Math.round(valueFromProbe * 10) / 10;

        DaoTemperatureMeasurement daoTempMeasurement =
                (DaoTemperatureMeasurement) DbManager.getDao(TemperatureMeasurementEntity.class);

        if(!daoTempMeasurement.insert(new TemperatureMeasurementEntity(roundedValue)))
        {
            SmartLogger.getLogger().severe("Unable to insert new temperature measure ("+ roundedValue +").");
            return;
        }

        _lastMeasureTime = new Date();
    }

    private static class TemperatureMeasureHandler implements TemperatureEventHandler
    {
        @Override
        public void temperatureMeasure(double value)
        {
            if(value == ProbeSettings.ERROR_VALUE)
            {
                SmartLogger.getLogger().warning("ERROR_VALUE sent with the Temperature Probe event!");
                return;
            }
            
            recordNewMeasure(value);
        }
    }
}
