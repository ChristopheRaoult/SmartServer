package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.logging.Level;

/**
 * [ADMIN] Command SetThreshold (correlation threshold of the RFID board).
 */
public class CmdRfidSetThreshold extends ClientCommand
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
        // waiting for 1 parameter: the new threshold
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_THRESHOLD, FALSE);
            throw new ClientCommandException("Invalid number of parameters [SetThreshold].");
        }
        
        if(!SmartServer.isAdministrator(ctx.channel().remoteAddress()))
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_THRESHOLD, FALSE);
            return;
        }
        
        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_THRESHOLD, FALSE);
            return;
        }
        
        short threshold;
        
        try
        {
            threshold = Short.parseShort(parameters[0]);
            
            if(threshold < 3 || threshold > 250)
            {
                throw new NumberFormatException("Threshold value out of allowed range [3;250]");
            }
        } catch(NumberFormatException nfe)
        {
            SmartLogger.getLogger().log(Level.WARNING, "Invalid value provided for the threshold", nfe);
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_THRESHOLD, FALSE);
            return;
        }        

        Object queryThreshold = DeviceHandler.getDevice().adminQuery("set_threshold", threshold);

        if(!(queryThreshold instanceof Boolean))
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_THRESHOLD, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx,
                ClientCommandRegister.AppCode.RFID_SET_THRESHOLD,
                (boolean) queryThreshold ? TRUE : FALSE);
    }
}
