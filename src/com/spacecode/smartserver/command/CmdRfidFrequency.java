package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * [ADMIN] Command Frequency (Carrier Period and Antenna Voltage).
 */
public class CmdRfidFrequency extends ClientCommand
{
    /**
     * Send the device's board's Carrier Period and Antenna Voltage
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
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_FREQUENCY);
            return;
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_FREQUENCY);
            return;
        }

        Object queryCarrier = DeviceHandler.getDevice().adminQuery("carrier_period");

        if(!(queryCarrier instanceof int[]))
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_FREQUENCY);
            return;
        }

        int[] carrierInfo = (int[]) queryCarrier;
        
        if(carrierInfo.length < 2)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_FREQUENCY);
            return;
        }

        SmartServer.sendMessage(ctx,
                ClientCommandRegister.AppCode.RFID_FREQUENCY, String.valueOf(carrierInfo[0]),
                String.valueOf(carrierInfo[1]));
    }
}
