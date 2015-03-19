package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.ConfManager;
import io.netty.channel.ChannelHandlerContext;

/**
 * Command SetFprSerial
 */
public class CmdSetFprSerial extends ClientCommand
{
    /**
     * Request to set/update the serial number of the desired fingerprint reader.
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
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_FPR_SERIAL, FALSE);
            throw new ClientCommandException("Invalid number of parameters [SetFprSerial].");
        }
        
        String serial = parameters[0] == null ? "" : parameters[0].trim();
        boolean isMaster = Boolean.parseBoolean(parameters[1]);
        boolean result = isMaster ? ConfManager.setDevFprMaster(serial) : ConfManager.setDevFprSlave(serial);

        SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_FPR_SERIAL, result ? TRUE : FALSE);
    }
}