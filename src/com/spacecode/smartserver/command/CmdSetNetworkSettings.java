package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Command SetNetworkSettings.
 */
public class CmdSetNetworkSettings extends ClientCommand
{
    private static final String VALID_IPV4_REGEX = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern VALID_IPV4_PATTERN = Pattern.compile(VALID_IPV4_REGEX);
    
    /**
     * Request to set/update the network settings (IP address, subnet mask, gateway) of the current device.
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException   Invalid number of parameters received.
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // Three options: 1 parameter ("dhcp"), OR 2 OR 3 parameters: device ip, subnet mask, (optional) gateway 
        if( (parameters.length < 1 || parameters.length > 3) || 
            (parameters.length == 1 && !parameters[0].toLowerCase().equals("dhcp")) )
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_NETWORK, FALSE);
            throw new ClientCommandException("Invalid number of parameters [SetNetworkSettings].");
        }

        // get the os name
        String osName = System.getProperty("os.name");
        
        if(osName == null)
        {
            SmartLogger.getLogger().severe("Property os.name is null. Is the JVM fine?");
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_NETWORK, FALSE);
            return;
        }

        osName = osName.toLowerCase();
                
        // prepare the network settings, if static mode (=> more than 1 parameter)
        String ipDevice = "", ipSubnet = "", ipGateway = "";
        
        if(parameters.length > 1)
        {
            ipDevice = parameters[0].trim();
            ipSubnet = parameters[1].trim();
            
            if(!VALID_IPV4_PATTERN.matcher(ipDevice).matches())
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_NETWORK, FALSE);
                throw new ClientCommandException("Invalid IP Address provided [SetNetworkSettings].");
            }
            
            if(!VALID_IPV4_PATTERN.matcher(ipSubnet).matches())
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_NETWORK, FALSE);
                throw new ClientCommandException("Invalid Subnet Mask provided [SetNetworkSettings].");
            }
            
            // a gateway was provided
            if(parameters.length == 3)
            {
                ipGateway = parameters[2].trim();
                
                if(!ipGateway.isEmpty() && !VALID_IPV4_PATTERN.matcher(ipGateway).matches())
                {
                    SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_NETWORK, FALSE);
                    throw new ClientCommandException("Invalid Gateway provided [SetNetworkSettings].");
                }
            }
        }
        
        try
        {
            // Windows
            if (osName.contains("windows"))
            {
                List<String> netshPackets = new ArrayList<>(Arrays.asList("netsh.exe", "-c", "interface", "ip", "set", 
                        "address", "name=\"Local Area Connection\""));
                
                // Static
                if (parameters.length > 1)
                {
                    netshPackets.add("source=static");
                    netshPackets.add("addr=");
                    netshPackets.add(ipDevice);
                    netshPackets.add("mask=");
                    netshPackets.add(ipSubnet);

                    if (!ipGateway.isEmpty())
                    {
                        netshPackets.add("gateway=");
                        netshPackets.add(ipGateway);
                        netshPackets.add("gwmetric=1");
                    }
                }

                // DHCP
                else
                {
                    netshPackets.add("source=dhcp");
                }
                
                String[] command = netshPackets.toArray(new String[netshPackets.size()]);                
                new ProcessBuilder(command).start();
            }

            // Linux
            else if (osName.contains("linux"))
            {
                StringBuilder sb = new StringBuilder("auto lo");
                sb.append(System.lineSeparator());
                sb.append("iface lo inet loopback");
                sb.append(System.lineSeparator());
                sb.append(System.lineSeparator());
                sb.append("auto eth0");
                sb.append(System.lineSeparator());
                sb.append("iface eth0 inet ");
                                
                // Static
                if (parameters.length > 1)
                {
                    sb.append("static");
                    sb.append(System.lineSeparator());
                    sb.append("address ");
                    sb.append(ipDevice);
                    sb.append(System.lineSeparator());
                    sb.append("netmask ");
                    sb.append(ipSubnet);
                                        
                    if(!ipGateway.isEmpty())
                    {
                        sb.append(System.lineSeparator());
                        sb.append("gateway ");
                        sb.append(ipGateway);
                    }

                    sb.append(System.lineSeparator());
                }

                // DHCP
                else
                {
                    sb.append("dhcp");
                    sb.append(System.lineSeparator());
                }

                // write the new interfaces settings
                PrintWriter out = new PrintWriter("/etc/network/interfaces");
                out.println(sb.toString());
                out.close();
                
                // reload the eth0 interface
                Process ifdown = new ProcessBuilder("/sbin/ifdown", "eth0").start();
                SmartLogger.getLogger().info("Ifdown result: "+ifdown.waitFor());
                Process ifup = new ProcessBuilder("/sbin/ifup", "eth0").start();
                SmartLogger.getLogger().info("Ifup result: "+ifup.waitFor());
            } 
            
            else
            {
                SmartLogger.getLogger().severe("Property os.name contains an unhandled value. IP cannot be changed.");
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_NETWORK, FALSE);
            }
        } catch(IOException | InterruptedException e)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to apply the provided network settings.", e);
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_NETWORK, FALSE);
        }
    }
}

