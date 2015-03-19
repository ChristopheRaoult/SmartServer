package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.ConfManager;
import io.netty.channel.ChannelHandlerContext;

/**
 * Command BrSerial
 */
public class CmdBrSerial extends ClientCommand
{
    /**
     * Request to get the serial port name of the Badge reader, master or slave, according to the given parameter.
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
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.BR_SERIAL);
            throw new ClientCommandException("Invalid number of parameters [BrSerial].");
        }

        //SmartLogger.getLogger().info(parameters[0]);
        boolean isMaster = Boolean.parseBoolean(parameters[0]);
        String serialPortName = isMaster ? ConfManager.getDevBrMaster() : ConfManager.getDevBrSlave();

        SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.BR_SERIAL, serialPortName);
    }
}
