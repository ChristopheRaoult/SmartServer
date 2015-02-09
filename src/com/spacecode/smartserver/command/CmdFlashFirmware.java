package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.Arrays;
import java.util.List;

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

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendAllClients(RequestCode.FLASH_FIRMWARE, FALSE);
            return;
        }

        List<String> firmwareLines = Arrays.asList(parameters[0].split("[\\r\\n]+"));
        boolean result = DeviceHandler.getDevice().flashFirmware(firmwareLines);

        SmartServer.sendMessage(ctx, RequestCode.FLASH_FIRMWARE, result ? TRUE : FALSE);
    }
}
