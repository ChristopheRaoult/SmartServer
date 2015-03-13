package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        
        // if the OS is Linux, read the "interfaces" file to get the network settings
        if(osName.contains("linux"))
        {
            try
            {
                List<String> networkInterfaces = 
                        Files.readAllLines(Paths.get("/etc/network/interfaces"), Charset.defaultCharset());

                Map<String, String> netConf = new HashMap<>();
                netConf.put("address", "");
                netConf.put("netmask", "");
                netConf.put("gateway", "");
                
                for(String line : networkInterfaces)
                {
                    line = line.toLowerCase();
                    
                    if(line.contains("inet dhcp"))
                    {
                        SmartServer.sendMessage(ctx, ClientCommandRegister.NETWORK_SETTINGS, "dhcp");
                        return;
                    }
                    
                    for(String confKey : netConf.keySet())
                    {
                        if(line.contains(confKey))
                        {
                            String[] fragments = line.split(" ");
                            
                            if(fragments.length >= 2)
                            {
                                netConf.put(confKey, fragments[1]);
                            }
                            
                            break;
                        }
                    }
                }

                SmartServer.sendMessage(ctx, ClientCommandRegister.NETWORK_SETTINGS, 
                        netConf.get("address"), netConf.get("netmask"), netConf.get("gateway"));
                return;
            } catch (IOException ioe)
            {
                SmartLogger.getLogger().log(Level.SEVERE, "An I/O error occurred when getting Network Settings", ioe);
            }
        }
        
        else if(osName.contains("windows"))
        {
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "ipconfig");
            processBuilder.redirectErrorStream(true);

            try
            {
                Process process = processBuilder.start();
                InputStream is = process.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

                Map<String, String> netConf = new HashMap<>();
                netConf.put("ip address", "");
                netConf.put("subnet", "");
                netConf.put("gateway", "");

                String line = bufferedReader.readLine();
                boolean isEth0 = false;

                while (line != null)
                {
                    line = line.toLowerCase();

                    if(line.contains("adapter"))
                    {
                        isEth0 = line.contains("local area connection");
                    }

                    if(isEth0)
                    {
                        for (String keyConf : netConf.keySet())
                        {
                            if (line.contains(keyConf))
                            {
                                String[] fragments = line.split(": ");

                                if (fragments.length >= 2)
                                {
                                    netConf.put(keyConf, fragments[1].trim());
                                }
                            }
                        }
                    }

                    line = bufferedReader.readLine();
                }

                SmartServer.sendMessage(ctx, ClientCommandRegister.NETWORK_SETTINGS,
                        netConf.get("ip address"), netConf.get("subnet"), netConf.get("gateway"));
                return;
            } catch (IOException ioe)
            {
                SmartLogger.getLogger().log(Level.SEVERE, "An I/O error occurred when getting Network Settings", ioe);
            }
        }
        
        else
        {
            SmartLogger.getLogger().severe("Unknown OS: Unable to get the network settings.");
        }

        SmartServer.sendMessage(ctx, ClientCommandRegister.NETWORK_SETTINGS, "null");
    }
}
