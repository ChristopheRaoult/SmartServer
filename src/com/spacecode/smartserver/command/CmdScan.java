package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.data.DeviceStatus;
import com.spacecode.sdk.device.data.ScanOption;
import com.spacecode.sdk.network.communication.EventCode;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Scan command.
 */
@CommandContract(deviceRequired = true, responseIfInvalid = EventCode.SCAN_FAILED, respondToAllIfInvalid = true)
public class CmdScan extends ClientCommand
{
    /**
     * Request a scan on current device. No data is sent/returned by this command. Scan events are handled by {@link DeviceHandler}
     * 
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    {@link ScanOption}s could be provided by the client.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        if(DeviceHandler.getDevice().getStatus() == DeviceStatus.SCANNING)
        {
            SmartLogger.getLogger().warning("Trying to start a scan whereas the Device is already 'SCANNING'!");
            return;
        }
        
        List<ScanOption> scanOptions = new ArrayList<>();
        
        if(parameters.length > 0)
        {
            for(String option : parameters)
            {
                try
                {
                    scanOptions.add(ScanOption.valueOf(option));
                } catch(IllegalArgumentException iae)
                {
                    SmartLogger.getLogger().log(Level.WARNING, "Invalid ScanOption provided: "+option, iae);
                }
            }
        }

        // if the user asked for a scan which result should not be recorded in the database
        DeviceHandler.setRecordInventory(!scanOptions.contains(ScanOption.NO_RECORD));
        
        // start the scan process with the given options (if any)
        DeviceHandler.getDevice().requestScan(scanOptions.toArray(new ScanOption[scanOptions.size()]));
    }
}
