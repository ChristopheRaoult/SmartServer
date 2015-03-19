package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.ConfManager;
import io.netty.channel.ChannelHandlerContext;

/**
 * Command SetBrSerial
 */
public class CmdSetBrSerial extends ClientCommand
{
    /**
     * Request to set/update the serial port name of the desired badge reader.
     * Return true (if operation succeeded) or false (if failure).
     *
     * @param ctx        Channel between SmartServer and the client.
     * @param parameters String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException Invalid number of parameters received.
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 2 parameters: serial and isMaster (true/false)
        if (parameters.length != 2)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_BR_SERIAL, FALSE);
            throw new ClientCommandException("Invalid number of parameters [SetBrSerial].");
        }

        String serial = parameters[0] == null ? "" : parameters[0].trim();
        boolean isMaster = Boolean.parseBoolean(parameters[1]);
        boolean result = isMaster ? ConfManager.setDevBrMaster(serial) : ConfManager.setDevBrSlave(serial);

        SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_BR_SERIAL, result ? TRUE : FALSE);
    }
}