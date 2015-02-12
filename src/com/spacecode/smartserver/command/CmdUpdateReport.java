package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import io.netty.channel.ChannelHandlerContext;

/**
 * Command UpdateReport.
 */
public class CmdUpdateReport extends ClientCommand
{
    private static final String EVENT_CODE_STARTED  = "event_update_started";
    private static final String EVENT_CODE_ENDED    = "event_update_ended";

    /**
     * Send the appropriate event code to notify the listeners what is the auto-update status.
     * If the parameter received is equal to 0, it means the updated start.
     * Else, it means that it ended. For "1", we got a successful update, for "-1" a failure.
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        if(parameters.length == 0 || parameters[0] == null)
        {
            return;
        }

        String parameter = parameters[0].trim();

        if(!"0".equals(parameter) && !"-1".equals(parameter) && !"1".equals(parameter))
        {
            return;
        }

        if(parameter.equals("0"))
        {
            SmartServer.sendAllClients(EVENT_CODE_STARTED);
        }

        else
        {
            SmartServer.sendAllClients(EVENT_CODE_ENDED, parameter.equals("1") ? TRUE : FALSE);
        }
    }
}
