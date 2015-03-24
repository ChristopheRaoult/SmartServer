package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.data.DeviceStatus;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.ConfManager;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.io.*;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

/** Class "container" for all ClientCommands dedicated to the (sys.) "Administration" part of the SmartApp. */
class ScAdmin
{
    /** Command BrSerial */
    static class CmdBrSerial extends ClientCommand
    {
        /**
         * Request to get the serial port name of the Badge reader, master or slave, according to the given parameter.
         * 
         * @param ctx                       Channel between SmartServer and the client.
         * @param parameters                String array containing parameters (if any) provided by the client.
         *                                  
         * @throws ClientCommandException
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
        {
            // waiting for 1 parameter: true (master reader) or false (slave reader).
            if(parameters.length != 1)
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.BR_SERIAL);
                throw new ClientCommandException("Invalid number of parameters [BrSerial].");
            }

            //SmartLogger.getLogger().info(parameters[0]);
            boolean isMaster = Boolean.parseBoolean(parameters[0]);
            String serialPortName = isMaster ? ConfManager.getDevBrMaster() : ConfManager.getDevBrSlave();

            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.BR_SERIAL, serialPortName);
        }
    }

    /** Command FlashFirmware */
    static class CmdFlashFirmware extends ClientCommand
    {
        /**
         * Start a Firmware Flashing operation with the given firmware (provided as String).
         * Send "True" if the operation did started. False otherwise. The progress of the operation is given by events.
         *
         * @param ctx                       Channel between SmartServer and the client.
         * @param parameters                String array containing parameters (if any) provided by the client.
         *
         * @throws ClientCommandException   Invalid number of parameters received.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
        {
            if(parameters.length != 1)
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.FLASH_FIRMWARE, FALSE);
                throw new ClientCommandException("Invalid number of parameters [FlashFirmware].");
            }

            if( DeviceHandler.getDevice() == null || 
                DeviceHandler.getDevice().getStatus() == DeviceStatus.FLASHING_FIRMWARE)
            {
                SmartServer.sendAllClients(ClientCommandRegister.AppCode.FLASH_FIRMWARE, FALSE);
                return;
            }

            List<String> firmwareLines = Arrays.asList(parameters[0].split("[\\r\\n]+"));
            boolean result = DeviceHandler.getDevice().flashFirmware(firmwareLines);

            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.FLASH_FIRMWARE, result ? TRUE : FALSE);
        }
    }

    /** Command Hostname */
    static class CmdHostname extends ClientCommand
    {
        /**
         * Get the device's hostname.
         *
         * @param ctx                       Channel between SmartServer and the client.
         * @param parameters                String array containing parameters (if any) provided by the client.
         *
         * @throws ClientCommandException   Invalid number of parameters received.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
        {
            // get the os name
            String osName = System.getProperty("os.name");

            if(osName == null)
            {
                SmartLogger.getLogger().severe("Property os.name is null. Is the JVM fine?");
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.HOSTNAME, "[Null]");
                return;
            }

            osName = osName.toLowerCase();

            if(osName.contains("linux"))
            {
                ProcessBuilder processBuilder = new ProcessBuilder("/bin/hostname");
                processBuilder.redirectErrorStream(true);

                try
                {
                    Process process = processBuilder.start();
                    InputStream is = process.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

                    String hostname = bufferedReader.readLine();

                    if (hostname != null)
                    {
                        SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.HOSTNAME, hostname);
                        return;
                    }
                } catch (IOException ioe)
                {
                    SmartLogger.getLogger().log(Level.SEVERE, "An I/O error occurred when getting Hostname", ioe);
                }
            }

            else if (osName.contains("windows"))
            {
                try
                {
                    SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.HOSTNAME,
                            java.net.InetAddress.getLocalHost().getHostName());
                    return;
                } catch (UnknownHostException uhe)
                {
                    SmartLogger.getLogger().log(Level.SEVERE, "Unable to get Hostname", uhe);
                }
            }

            else
            {
                SmartLogger.getLogger().severe("Unknown OS: could not get Hostname");
            }

            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.HOSTNAME, "[Null]");
        }
    }

    /** Command FprSerial */
    static class CmdFprSerial extends ClientCommand
    {
        /**
         * Request to get the serial number of the Fingerprint reader, master or slave, according to the given parameter.
         * 
         * @param ctx                       Channel between SmartServer and the client.
         * @param parameters                String array containing parameters (if any) provided by the client.
         *                                  
         * @throws ClientCommandException
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
        {
            // waiting for 1 parameter: true (master reader) or false (slave reader).
            if(parameters.length != 1)
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.FPR_SERIAL);
                throw new ClientCommandException("Invalid number of parameters [FprSerial].");
            }

            boolean isMaster = Boolean.parseBoolean(parameters[0]);
            String serialPortNumber = isMaster ? ConfManager.getDevFprMaster() : ConfManager.getDevFprSlave();

            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.FPR_SERIAL, serialPortNumber);
        }
    }

    /** Command NetworkSettings */
    static class CmdNetworkSettings extends ClientCommand
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
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.NETWORK_SETTINGS, "null");
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
                            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.NETWORK_SETTINGS, "dhcp");
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

                    SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.NETWORK_SETTINGS,
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

                    SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.NETWORK_SETTINGS,
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

            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.NETWORK_SETTINGS, "null");
        }
    }

    /** Command SignInAdmin */
    static class CmdSignInAdmin extends ClientCommand
    {
        private static final String ADMIN_USERNAME = "testrfid";
        private static final String ADMIN_PASSWORD = "testrfid123456";

        /**
         * Add the current ChannelHandlerContext to the list of authenticated (administrator) contexts.
         *
         * @param ctx                       Channel between SmartServer and the client.
         * @param parameters                String array containing parameters (if any) provided by the client.
         *
         * @throws ClientCommandException   Invalid number of parameters received.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
        {
            // waiting for 2 parameters: username, password
            if (parameters.length != 2)
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SIGN_IN_ADMIN, FALSE);
                throw new ClientCommandException("Invalid number of parameters [SignInAdmin].");
            }

            String username = parameters[0];
            String password = parameters[1];

            if(!ADMIN_USERNAME.equals(username) || !ADMIN_PASSWORD.equals(password))
            {
                SmartLogger.getLogger().info(String.format("Authentication Failure! (%s/%s)", username, password));
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SIGN_IN_ADMIN, FALSE);
                return;
            }

            SmartServer.addAdministrator(ctx.channel().remoteAddress());
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SIGN_IN_ADMIN, TRUE);
        }
    }

    /**
     * SerialBridge command.
     *
     * /drivers/usb/gadget/composite.c has been modified to raise an uevent when the USB OTG of ARM Board is plugged/unplugged.
     * udev rules have been created to run a shell script which send this SerialBridge command.
     * The intent is to get the server to sleep while it's running the serial port forwarding.
     * Forwarding is made with "socat". From port ttyGS0 (g_serial emulated port) to ttyUSB0 (FTDI_IO USB to Serial port).
     * If the command is sent again, then stop the port forwarding and wake the server up.
     */
    static class CmdSerialBridge extends ClientCommand
    {
        private static Process _portForwardingProcess = null;

        /**
         * According to parameter ("ON"/"OFF"), enable or disable Serial Port forwarding ("Serial Bridge").
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    String array containing parameters (if any) provided by the client.
         *
         * @throws ClientCommandException
         */
        @Override
        public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
        {
            if(parameters.length == 0)
            {
                return;
            }

            if(!"ON".equals(parameters[0]) && !"OFF".equals(parameters[0]))
            {
                return;
            }

            boolean openBridge = "ON".equals(parameters[0]);

            if(openBridge)
            {
                if(_portForwardingProcess != null)
                {
                    return;
                }

                try
                {
                    // tell the device handler that serial port is hooked, then it doesn't try to reconnect device
                    DeviceHandler.setForwardingSerialPort(true);

                    // disconnect the device, release the serial port
                    DeviceHandler.disconnectDevice();

                    // execute command for port forwarding
                    String socatCmd = "socat /dev/ttyGS0,raw,echo=0,crnl /dev/ttyUSB0,raw,echo=0,crnl";
                    _portForwardingProcess = new ProcessBuilder("/bin/sh", "-c", socatCmd).start();

                    SmartLogger.getLogger().severe("Running Port Forwarding command.");
                } catch (IOException ioe)
                {
                    SmartLogger.getLogger().log(Level.SEVERE, "Unable to run Port Forwarding command.", ioe);

                    // reconnect to local Device
                    DeviceHandler.reconnectDevice();
                }
            }

            else
            {
                if(_portForwardingProcess == null)
                {
                    return;
                }

                try
                {
                    // stop the process (port forwarding)
                    // NOTE: calling destroy() on Process instance does not stop "socat"...
                    Process killingSocatProcess = new ProcessBuilder("/bin/sh", "-c", "pkill -f socat").start();
                    killingSocatProcess.waitFor();

                    _portForwardingProcess = null;

                    SmartLogger.getLogger().severe("Stopped Port Forwarding command. Reconnecting Device...");

                    // reconnect to local Device
                    DeviceHandler.setForwardingSerialPort(false);
                    DeviceHandler.reconnectDevice();
                } catch (IOException | InterruptedException e)
                {
                    SmartLogger.getLogger().log(Level.SEVERE, "Unable to stop Port Forwarding command.", e);
                }
            }
        }
    }

    /** Command SetBrSerial */
    static class CmdSetBrSerial extends ClientCommand
    {
        /**
         * Request to set/update the serial port name of the desired badge reader.
         * Return true (if operation succeeded) or false (if failure).
         *
         * @param ctx        Channel between SmartServer and the client.
         * @param parameters String array containing parameters (if any) provided by the client.
         *
         * @throws ClientCommandException Invalid number of parameters received.
         */
        @Override
        public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
        {
            // waiting for 2 parameters: serial and isMaster (true/false)
            if (parameters.length != 2)
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_BR_SERIAL, FALSE);
                throw new ClientCommandException("Invalid number of parameters [SetBrSerial].");
            }

            String serial = parameters[0] == null ? "" : parameters[0].trim();
            boolean isMaster = Boolean.parseBoolean(parameters[1]);
            boolean result = isMaster ? ConfManager.setDevBrMaster(serial) : ConfManager.setDevBrSlave(serial);

            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_BR_SERIAL, result ? TRUE : FALSE);
        }
    }

    /** Command SetFprSerial */
    static class CmdSetFprSerial extends ClientCommand
    {
        /**
         * Request to set/update the serial number of the desired fingerprint reader.
         * Return true (if operation succeeded) or false (if failure).
         *
         * @param ctx        Channel between SmartServer and the client.
         * @param parameters String array containing parameters (if any) provided by the client.
         *
         * @throws ClientCommandException Invalid number of parameters received.
         */
        @Override
        public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
        {
            // waiting for 2 parameters: serial and isMaster (true/false)
            if (parameters.length != 2)
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_FPR_SERIAL, FALSE);
                throw new ClientCommandException("Invalid number of parameters [SetFprSerial].");
            }

            String serial = parameters[0] == null ? "" : parameters[0].trim();
            boolean isMaster = Boolean.parseBoolean(parameters[1]);
            boolean result = isMaster ? ConfManager.setDevFprMaster(serial) : ConfManager.setDevFprSlave(serial);

            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SET_FPR_SERIAL, result ? TRUE : FALSE);
        }
    }
    
    /** Command SetNetworkSettings */
    static class CmdSetNetworkSettings extends ClientCommand
    {
        private static final String VALID_IPV4_REGEX = 
                "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
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

    /** Command StartUpdate */
    static class CmdStartUpdate extends ClientCommand
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

    /** Command UpdateReport */
    static class CmdUpdateReport extends ClientCommand
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
}
