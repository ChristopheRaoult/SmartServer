package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * [ADMIN] Command IncFrequency (allow increasing the period of the carrier signal).
 */
public class CmdRfidIncFrequency extends ClientCommand
{
    /**
     * Send true if the correlation threshold was updated, false otherwise.
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
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_INC_FREQUENCY, FALSE);
            return;
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_INC_FREQUENCY, FALSE);
            return;
        }

        Object queryFrequency = DeviceHandler.getDevice().adminQuery("increase_frequency");

        if(!(queryFrequency instanceof Boolean))
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_INC_FREQUENCY, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx,
                ClientCommandRegister.AppCode.RFID_INC_FREQUENCY, (boolean) queryFrequency ? TRUE : FALSE);
    }
}