package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.logging.Level;

/**
 * [ADMIN] Command SetDutyCycle ("bridge type" and duty cycle values, in the RFID board memory).
 */
public class CmdRfidSetDutyCycle extends ClientCommand
{
    /**
     * Send true if the duty cycle was updated (values and bridge type), false otherwise.
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 3 parameters: the bridge type (full / half), and a numeric value for both types.
        if(parameters.length != 3)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_DUTY_CYCLE, FALSE);
            throw new ClientCommandException("Invalid number of parameters [SetDutyCycle].");
        }

        if(!SmartServer.isAdministrator(ctx.channel().remoteAddress()))
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_DUTY_CYCLE, FALSE);
            return;
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_DUTY_CYCLE, FALSE);
            return;
        }

        short bridgeType, dcuFull, dcuHalf;

        try
        {
            bridgeType = Short.parseShort(parameters[0]);
            dcuFull = Short.parseShort(parameters[1]);
            dcuHalf = Short.parseShort(parameters[2]);

            if(bridgeType < 0 || bridgeType > 1)
            {
                throw new NumberFormatException("Invalid bridge type");
            }
            
            if(dcuFull < 0 || dcuFull > 167)
            {
                throw new NumberFormatException("Duty Cycle for Full Bridge out of range [0;167]");
            }
            if(dcuHalf < 0 || dcuHalf> 167)
            {
                throw new NumberFormatException("Duty Cycle for Full Bridge out of range [0;167]");
            }
        } catch(NumberFormatException nfe)
        {
            SmartLogger.getLogger().log(Level.WARNING, "Invalid value provided for the duty cycle...", nfe);
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_DUTY_CYCLE, FALSE);
            return;
        }

        Object querySetDcu = DeviceHandler.getDevice().adminQuery("set_duty_cycle", bridgeType, dcuFull, dcuHalf);

        if(!(querySetDcu instanceof Boolean))
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_DUTY_CYCLE, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx,
                ClientCommandRegister.AppCode.RFID_SET_DUTY_CYCLE,
                (boolean) querySetDcu ? TRUE : FALSE);
    }
}
