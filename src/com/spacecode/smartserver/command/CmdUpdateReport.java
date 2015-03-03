package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.logging.Level;

/**
 * Command UpdateReport.
 */
public class CmdUpdateReport extends ClientCommand
{
    // if true, the cmd sends a Progress Report, otherwise, it sends a "start" notification
    private static boolean UPDATE_IN_PROGRESS       = false;

    // number of patches to be applied for this update process, sent with each Progress Report
    private static String PATCHES_COUNT             = "0";

    static final String EVENT_CODE_STARTED  = "event_update_started";
    static final String EVENT_CODE_PROGRESS = "event_update_progress";
    static final String EVENT_CODE_ENDED    = "event_update_ended";

    /**
     * Send the appropriate event code to notify the listeners what is the auto-update status.
     * The update-script sends "0" for a successful update, "-1" for a failure, and the number of patches to be applied
     * when the update has just started.
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

        try
        {
            int integerValue = Integer.parseInt(parameter);
        } catch (NumberFormatException nfe)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Invalid parameter provided to UpdateReport command", nfe);
            return;
        }

        switch (parameter)
        {
            case "0":
                // update successful
                SmartLogger.getLogger().info("[Update] Success.");
                SmartServer.sendAllClients(EVENT_CODE_ENDED, TRUE);

                UPDATE_IN_PROGRESS = false;
                break;

            case "-1":
                // update failure
                SmartLogger.getLogger().info("[Update] Failure.");
                SmartServer.sendAllClients(EVENT_CODE_ENDED, FALSE);

                UPDATE_IN_PROGRESS = false;
                break;

            default:
                if(!UPDATE_IN_PROGRESS)
                {
                    // update started
                    SmartLogger.getLogger().info("[Update] Started. "+parameter+" new patches.");
                    UPDATE_IN_PROGRESS = true;
                    SmartServer.sendAllClients(EVENT_CODE_STARTED);
                    PATCHES_COUNT = parameter;
                }

                else
                {
                    // a new patch has been applied
                    SmartLogger.getLogger().info("[Update] Progress: "+parameter+" patches left.");
                    SmartServer.sendAllClients(EVENT_CODE_PROGRESS, parameter, PATCHES_COUNT);
                }
                break;
        }
    }
}
