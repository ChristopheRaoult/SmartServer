package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * [ADMIN] Command Threshold (correlation threshold of the RFID board).
 */
public class CmdRfidThreshold extends ClientCommand
{
    /**
     * Send the device's board's correlation threshold (or "-1" in case of error).
     * 
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        if(!SmartServer.isAdministrator(ctx.channel().remoteAddress()))
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_THRESHOLD, "-1");
            return;
        }
        
        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_THRESHOLD, "-1");
            return;
        }

        Object queryThreshold = DeviceHandler.getDevice().adminQuery("threshold");
        
        if(!(queryThreshold instanceof Integer) && !(queryThreshold instanceof Short))
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_THRESHOLD, "-1");
            return;
        }
        
        SmartServer.sendMessage(ctx, 
                ClientCommandRegister.AppCode.RFID_THRESHOLD, 
                String.valueOf((short) queryThreshold));
    }
}
