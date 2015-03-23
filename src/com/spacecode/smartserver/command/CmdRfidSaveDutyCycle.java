package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * [ADMIN] Command SaveDutyCycle ("bridge type" and duty cycle values, in the RFID board memory).
 */
public class CmdRfidSaveDutyCycle extends ClientCommand
{
    /**
     * Send TRUE if the current settings could be applied, FALSE otherwise.
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
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SAVE_DUTY_CYCLE, FALSE);
            return;
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SAVE_DUTY_CYCLE, FALSE);
            return;
        }

        Object querySaveDcu = DeviceHandler.getDevice().adminQuery("save_duty_cycle");

        if(!(querySaveDcu instanceof Boolean))
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SAVE_DUTY_CYCLE, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx,
                ClientCommandRegister.AppCode.RFID_SAVE_DUTY_CYCLE, (boolean) querySaveDcu ? TRUE : FALSE);
    }
}
