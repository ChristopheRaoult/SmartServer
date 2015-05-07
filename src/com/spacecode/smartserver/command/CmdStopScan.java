package com.spacecode.smartserver.command;

import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * StopScan command.
 */
@CommandContract(deviceRequired = true, noResponseWhenInvalid = true)
public class CmdStopScan extends ClientCommand
{
    /**
     * Request the device to stop its scan. No data is sent/returned. Device events are handled by events handler.
     * 
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    None expected.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        DeviceHandler.getDevice().stopScan();
    }
}
