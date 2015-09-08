package com.spacecode.smartserver.command;


import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * InventoryById command.
 */
@CommandContract(paramCount = 1, strictCount = true, deviceRequired = true, responseIfInvalid = ClientCommand.FALSE)
public class CmdSetLightIntensity extends ClientCommand
{
    /**
     * The value is expected to >= than 0 (light off) and <= than 300 (maximal intensity)
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    Value of the light intensity to be set (expected: [0;300]).
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        String paramValue = parameters[0];

        try
        {
            int value = Integer.parseInt(paramValue);

            SmartServer.sendMessage(ctx,
                    RequestCode.SET_LIGHT_INTENSITY,
                    DeviceHandler.getDevice().setLightIntensity(value) ? TRUE : FALSE
            );
        } catch(NumberFormatException nfe)
        {
            SmartServer.sendMessage(ctx, RequestCode.SET_LIGHT_INTENSITY, FALSE);
        }
    }
}
