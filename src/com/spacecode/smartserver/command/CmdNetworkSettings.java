package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;

/**
 * Command NetworkSettings.
 */
public class CmdNetworkSettings extends ClientCommand
{
    /**
     * Send Network settings for the current device.
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // get the os name
        String osName = System.getProperty("os.name");

        if(osName == null)
        {
            SmartLogger.getLogger().severe("Property os.name is null. Is the JVM fine?");
            SmartServer.sendMessage(ctx, ClientCommandRegister.NETWORK_SETTINGS, "null");
            return;
        }

        osName = osName.toLowerCase();
        
        if(osName.contains("linux"))
        {
            try
            {
                List<String> networkInterfaces = 
                        Files.readAllLines(Paths.get("/etc/network/interfaces"), Charset.defaultCharset());
                
                String ipDevice = "", netmask = "", gateway = "";
                
                for(String line : networkInterfaces)
                {
                    line = line.toLowerCase();
                    
                    if(line.contains("inet dhcp"))
                    {
                        SmartServer.sendMessage(ctx, ClientCommandRegister.NETWORK_SETTINGS, "dhcp");
                        return;
                    }
                    
                    if(line.contains("address"))
                    {
                        ipDevice = line.split(" ")[1];
                    }
                    
                    else if(line.contains("netmask"))
                    {
                        netmask = line.split(" ")[1];
                    }
                    
                    else if(line.contains("gateway"))
                    {
                        gateway = line.split(" ")[1];
                    }
                }

                SmartServer.sendMessage(ctx, ClientCommandRegister.NETWORK_SETTINGS, ipDevice, netmask, gateway);
                return;
            } catch (IOException ioe)
            {
                SmartLogger.getLogger().log(Level.SEVERE, "An I/O error occurred when getting Network Settings", ioe);
            }

            SmartServer.sendMessage(ctx, ClientCommandRegister.NETWORK_SETTINGS, "null");
        }
        
        else if(osName.contains("windows"))
        {
            
        }
        
        else
        {
            SmartLogger.getLogger().severe("Unknown OS: Unable to get the network settings.");
        }
    }
}
