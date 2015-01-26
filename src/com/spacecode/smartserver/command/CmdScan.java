package com.spacecode.smartserver.command;

import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Scan command.
 */
public class CmdScan extends ClientCommand
{
    /**
     * Request a scan on current device. No data is sent/returned. Device events are handled by events handler.
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        if(DeviceHandler.getDevice() == null)
        {
            return;
        }

        DeviceHandler.getDevice().requestScan();
    }
}
