package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.EventCode;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Scan command.
 */
@CommandContract(deviceRequired = true, responseWhenInvalid = EventCode.SCAN_FAILED, responseSentToAllWhenInvalid = true)
public class CmdScan extends ClientCommand
{
    /**
     * Request a scan on current device. No data is sent/returned. Device events are handled by events handler.
     * 
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    None expected.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        DeviceHandler.getDevice().requestScan();
    }
}
