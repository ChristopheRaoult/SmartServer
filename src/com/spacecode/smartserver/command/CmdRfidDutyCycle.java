package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * [ADMIN] Command DutyCycle ("bridge type" and duty cycle values, in the RFID board memory).
 */
public class CmdRfidDutyCycle extends ClientCommand
{
    /**
     * Send the device's board's duty cycle "bridge type" (half/full) and values (for both types).
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
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_DUTY_CYCLE);
            return;
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_DUTY_CYCLE);
            return;
        }

        Object queryDcu = DeviceHandler.getDevice().adminQuery("duty_cycle");

        if(!(queryDcu instanceof short[]))
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_DUTY_CYCLE);
            return;
        }
        
        short[] dcuInfo = (short[]) queryDcu;

        if(dcuInfo.length < 3)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_DUTY_CYCLE);
            return;
        }
        
        SmartServer.sendMessage(ctx,
                ClientCommandRegister.AppCode.RFID_DUTY_CYCLE, String.valueOf(dcuInfo[0]), String.valueOf(dcuInfo[1]),
                String.valueOf(dcuInfo[2]));
    }
}
