package com.spacecode.smartserver.command.commands;

import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.command.ClientCommand;
import com.spacecode.smartserver.command.ClientCommandException;
import io.netty.channel.ChannelHandlerContext;

/**
 * Scan command.
 */
public class CommandScan implements ClientCommand
{
    /**
     * Request a scan on current device. No data is sent/returned. Device events are handled by events handler.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        DeviceHandler.getDevice().requestScan();
    }
}
