package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import io.netty.channel.ChannelHandlerContext;

/**
 * Command FlashFirmware
 */
public class CmdFlashFirmware extends ClientCommand
{
    /**
     * Start a Firmware Flashing operation with the given firmware (provided as String).
     * Send back "True" if the operation did started. False otherwise. The progress of the operation is given by events.
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException   Invalid number of parameters received.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.FLASH_FIRMWARE, FALSE);
            throw new ClientCommandException("Invalid number of parameters [FlashFirmware].");
        }

        SmartServer.sendMessage(ctx, RequestCode.FLASH_FIRMWARE, TRUE);
    }
}
