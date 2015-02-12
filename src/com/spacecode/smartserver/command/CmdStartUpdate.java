package com.spacecode.smartserver.command;

import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Command StartUpdate
 */
public class CmdStartUpdate extends ClientCommand
{
    private static Process _pythonProcess = null;

    /**
     * Launch the auto-update script (update.py) with the python runtime.
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // do not execute anything if the process exists (= not terminated)
        if(_pythonProcess != null)
        {
            return;
        }

        try
        {
            // execute the auto-update script
            _pythonProcess = Runtime.getRuntime()
                    .exec("/usr/bin/nohup /usr/local/bin/Spacecode/update_runner.sh 2>&1 > /dev/null &");
            SmartLogger.getLogger().info("Running the auto-update script...");
        } catch (IOException ioe)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to execute the auto-update script.", ioe);
        }
    }
}
