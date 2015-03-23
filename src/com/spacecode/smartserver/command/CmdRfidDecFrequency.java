package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * [ADMIN] Command DecFrequency (allow decreasing the period of the carrier signal).
 */
public class CmdRfidDecFrequency extends ClientCommand
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
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_DEC_FREQUENCY, FALSE);
            return;
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_DEC_FREQUENCY, FALSE);
            return;
        }

        Object queryFrequency = DeviceHandler.getDevice().adminQuery("decrease_frequency");

        if(!(queryFrequency instanceof Boolean))
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_DEC_FREQUENCY, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx,
                ClientCommandRegister.AppCode.RFID_DEC_FREQUENCY, (boolean) queryFrequency ? TRUE : FALSE);
    }
}