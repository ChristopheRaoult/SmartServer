package com.spacecode.smartserver;

/**
 * Handle Alerts raising/reporting and Emails sending (if any SMTP server is set).
 *
 * Has to be initialized to subscribe to "alert-compliant" events
 */
public class AlertHandler
{
    private static boolean _isInitialized = false;

    public static boolean initialize()
    {
        if(DeviceHandler.getDevice() == null)
        {
            return false;
        }

        // TODO: subscribe to appropriate event by implementing the required DeviceEventHandler interfaces
        // and then implement and call the appropriate method to raise the appropriate alert :-).

        _isInitialized = true;
        return true;
    }
}
