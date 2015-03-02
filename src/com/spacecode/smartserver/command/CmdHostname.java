package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

/**
 * Command Hostname
 */
public class CmdHostname extends ClientCommand
{
    /**
     * Get the device's (linux) hostname.
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException   Invalid number of parameters received.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/hostname");
        processBuilder.redirectErrorStream(true);

        try
        {
            Process process = processBuilder.start();
            InputStream is = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

            String hostname = bufferedReader.readLine();

            if(hostname != null)
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.HOSTNAME, hostname);
                return;
            }
        } catch (IOException ioe)
        {
            SmartLogger.getLogger().log(Level.SEVERE ,"An I/O error occurred when getting Hostname.");
        }

        SmartServer.sendMessage(ctx, ClientCommandRegister.HOSTNAME, "[Null]");
    }
}
