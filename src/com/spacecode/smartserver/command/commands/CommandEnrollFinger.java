package com.spacecode.smartserver.command.commands;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.FingerIndex;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.command.ClientCommand;
import com.spacecode.smartserver.command.ClientCommandException;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.TimeoutException;

/**
 * EnrollFinger command.
 */
public class CommandEnrollFinger implements ClientCommand
{
    /**
     * Request to start enrollment process for a given user and finger index. Send (string) "true" if succeed, "false" otherwise.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 3 parameters: username, finger index, "is Master reader?" (boolean)
        if(parameters.length != 3)
        {
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, "false");
            return;
        }

        String username = parameters[0];
        FingerIndex fingerIndex;
        boolean masterReader = Boolean.parseBoolean(parameters[2]);

        try
        {
            int fingerIndexVal = Integer.parseInt(parameters[1]);
            fingerIndex = FingerIndex.getValueByIndex(fingerIndexVal);
        } catch(NumberFormatException nfe)
        {
            // integer for the finger index is not valid
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, "false");
            return;
        }

        // no finger index matching with the given integer value
        if(fingerIndex == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, "false");
            return;
        }

        try
        {
            boolean result = DeviceHandler.getDevice().getUsersService().enrollFinger(username, fingerIndex, masterReader);
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, result ? "true" : "false");

            // TODO : persist new template in DB
        } catch (TimeoutException e)
        {
            // enrollment process timeout expired
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, "false");
        }
    }
}
