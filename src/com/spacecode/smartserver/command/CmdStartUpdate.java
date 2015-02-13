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
            try
            {
                _pythonProcess.waitFor();
            } catch (InterruptedException ie)
            {
                SmartLogger.getLogger().log(Level.SEVERE, "Interrupted while waiting for AutoUpdate to complete.", ie);
            }
        }

        try
        {
            // execute the auto-update script
            _pythonProcess = new ProcessBuilder("/bin/sh", "-c", "python /usr/local/bin/Spacecode/update.py").start();
            SmartLogger.getLogger().info("Running the auto-update process...");
        } catch (IOException ioe)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to execute the auto-update script.", ioe);
        }
    }
}
