package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.ConfManager;
import io.netty.channel.ChannelHandlerContext;

/**
 * Command FprSerial
 */
public class CmdFprSerial extends ClientCommand
{
    /**
     * Request to get the serial number of the Fingerprint reader, master or slave, according to the given parameter.
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 1 parameter: true (master reader) or false (slave reader).
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.FPR_SERIAL);
            throw new ClientCommandException("Invalid number of parameters [FprSerial].");
        }

        boolean isMaster = Boolean.parseBoolean(parameters[0]);
        String serialPortNumber = isMaster ? ConfManager.getDevFprMaster() : ConfManager.getDevFprSlave();

        SmartServer.sendMessage(ctx, ClientCommandRegister.FPR_SERIAL, serialPortNumber);
    }
}
