package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.data.ScanOption;
import com.spacecode.sdk.network.communication.EventCode;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Scan command.
 */
@CommandContract(deviceRequired = true, responseIfInvalid = EventCode.SCAN_FAILED, respondToAllIfInvalid = true)
public class CmdScan extends ClientCommand
{
    /**
     * Request a scan on current device. No data is sent/returned. Device events are handled by events handler.
     * 
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    {@link ScanOption}s could be provided by the client.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
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
                    SmartLogger.getLogger().warning("Invalid ScanOption provided: "+option);
                }
            }
        }
        
        DeviceHandler.getDevice().requestScan(scanOptions.toArray(new ScanOption[scanOptions.size()]));
    }
}
