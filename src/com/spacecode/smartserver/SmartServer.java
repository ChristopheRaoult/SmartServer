package com.spacecode.smartserver;

import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.network.communication.MessageHandler;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.helper.ConfManager;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import com.spacecode.smartserver.helper.TemperatureCenter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SmartServer "Main" class.
 * Relies on Netty to start:
 * <ul>
 *     <li>Asynchronous TCP/IP server</li>
 *     <li>WebSocket server</li>
 * </ul>
 */
public final class SmartServer
{
    private static final EventLoopGroup BOSS_GROUP = new NioEventLoopGroup();
    private static final EventLoopGroup WORKER_GROUP = new NioEventLoopGroup();

    private static final ChannelGroup TCP_IP_CHAN_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final ChannelGroup WS_CHAN_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final int TCP_IP_PORT = 8080;
    private static final ChannelHandler TCP_IP_HANDLER = new SmartServerHandler();
    private static final int WS_PORT = 8081;
    private static final ChannelHandler WS_HANDLER = new WebSocketHandler();
    
    private static final List<SocketAddress> ADMINISTRATORS = new ArrayList<>();

    private static Channel _channel;
    private static Channel _wsChannel;

    // default is 65536 (2^16), most of the time. Increase this value to 2^22. Max supported is Integer limit: 2^31-1
    public static final int MAX_FRAME_LENGTH = 4194304;

    /** Must not be instantiated. */
    private SmartServer()
    {
    }

    /**
     * Allow getting the working directory to work with local files (logging, properties...).
     *
     * @return The current JAR directory, or "." if an error occurred.
     */
    public static String getWorkingDirectory()
    {
        try
        {
            String jarPath = SmartServer.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            int lastSeparatorIndex = jarPath.lastIndexOf(File.separator);

            if(jarPath.endsWith(".jar") &&  lastSeparatorIndex != -1)
            {
                return jarPath.substring(0, lastSeparatorIndex+1);
            }
        } catch(SecurityException se)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Permission to get SmartServer Directory not allowed.", se);
        }

        return "."+File.separator;
    }

    /**
     * Entry point:
     * <p>1 - Initialize a shutdown hook to free Device (including modules) and Database.</p>
     * <p>2 - Following operations:
     * <ul>
     *     <li>Try to initialize/connect to Database*</li>
     *     <li>Try to initialize/connect to Device*</li>
     *     <li>Load granted users*</li>
     *     <li>Load last inventory</li>
     *     <li>Start the Alert center</li>
     *     <li>Start the Temperature center (if a probe is used)</li>
     *     <li>Load the "G-Serial" module ["Serial Bridge" mode]</li>
     * </ul>
     * *: Critical operations. SmartServer won't start if one fails.</p>
     * <p>3 - Start the asynchronous Server.</p>
     */
    public static void main(String[] args) throws IOException, SQLException
    {
        // SDK: Only display its SEVERE logs.
        com.spacecode.sdk.SmartLogger.getLogger().setLevel(Level.ALL);
        // initialize the Server's logger.
        SmartLogger.initialize();

        initializeShutdownHook();

        // Initialize database connection and (if required) model
        if(!DbManager.initializeDatabase())
        {
            SmartLogger.getLogger().severe("Database could not be initialized. SmartServer will not start.");
            return;
        }

        if(!DeviceHandler.connectDevice())
        {
            SmartLogger.getLogger().severe("Unable to connect a device. SmartServer will not start");
            return;
        }

        if(!init())
        {
            SmartLogger.getLogger().severe("Unable to initialize SmartServer [init].");
            return;
        }

        SmartLogger.getLogger().info("SmartServer is Ready");

        startListening();
    }

    /**
     * Check if the g_serial module is loaded, otherwise load it.
     * 
     * This method should be called once SmartServer has completely started.
     * If the device is plugged (via USB) to a computer, the "Serial Bridge" mode should be triggered automatically.
     */
    private static void loadModuleGSerial()
    {
        try
        {
            new ProcessBuilder("/bin/sh", "-c", "if ! lsmod | grep g_serial; then modprobe g_serial; fi;").start();
        } catch (IOException ioe)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to load module g_serial: I/O error.", ioe);
        }
    }

    /**
     * Add an hook on shutdown operation in order to:
     * <ul>
     *     <li>Release RfidDevice</li>
     *     <li>Close the DB connection pool</li>
     *     <li>Stop the asynchronous TCP server</li>
     * </ul>
     */
    private static void initializeShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                TemperatureCenter.stop();
                DeviceHandler.disconnectDevice();
                DbManager.close();
                stop();
            }
        }));
    }

    /**
     * <ul>
     *  <li>* Create the device in the DB, if it does not exist</li>
     *  <li>Done by {@link DeviceHandler}:</li>
     *  <ul>      
     *    <li>* Connect modules if any (fingerprint/badge readers, temperature probe)</li>
     *    <li>* Load the authorized users in the UsersService of the Device instance</li>
     *    <li>Load the last inventory (if any) in the Device instance</li>
     *    <li>Start the Alert Center</li>
     *    <li>Start the Temperature Center (if required)</li>
     *  </ul>
     * </ul>
     *
     * @return True if the whole initialization succeeded (* mandatory steps), false otherwise.
     */
    private static boolean init()
    {
        Device currentDevice = DeviceHandler.getDevice();

        String devSerialNumber = currentDevice.getSerialNumber();
        SmartLogger.getLogger().info(currentDevice.getDeviceType() + ": " + devSerialNumber);

        if(!DbManager.createDeviceIfNotExists(devSerialNumber))
        {
            SmartLogger.getLogger().severe("Unknown device could not be create in DB. SmartServer won't start.");
            return false;
        }

        return DeviceHandler.onConnected();
    }

    /**
     * Entry point of SmartServer.
     * Instantiate ServerBootstrap, SmartServerHandler, and configure the server channel.
     */
    private static void startListening()
    {
        // Try to either read the ports number from the conf or use the default values
        int portTcp, portWs;
        
        try
        {                        
            portTcp = Integer.parseInt(ConfManager.getAppPortTcp());
        } catch(NumberFormatException nfe)
        {
            portTcp = TCP_IP_PORT;            
            SmartLogger.getLogger().info("Using default TCP port for TCP/IP channel");
        }
        
        try
        {                        
            portWs = Integer.parseInt(ConfManager.getAppPortWs());
        } catch(NumberFormatException nfe)
        {
            portWs = WS_PORT;

            SmartLogger.getLogger().info("Using default TCP port for WebSocket channel");
        }


        SmartLogger.getLogger().info(String.format("TCP Ports - TCP/IP: %d, WebSocket: %d", portTcp, portWs));
        
        // start the netty communication layer
        try
        {
            ServerBootstrap tcpIpBootStrap = new ServerBootstrap();
            tcpIpBootStrap.group(BOSS_GROUP, WORKER_GROUP)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>()
                    {
                        @Override
                        public void initChannel(SocketChannel ch)
                        {
                            // Define character EOT (0x04) as an end-of-frame character.
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(MAX_FRAME_LENGTH,
                                    Unpooled.wrappedBuffer(new byte[] {MessageHandler.END_OF_MESSAGE})));

                            // Allow sending/receiving string instead of byte buffers.
                            ch.pipeline().addLast(new StringDecoder(), new StringEncoder());

                            // Add a SmartServerHandler instance to the channel pipeline.
                            ch.pipeline().addLast(TCP_IP_HANDLER);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            _channel = tcpIpBootStrap.bind(portTcp).sync().channel();

            ServerBootstrap wsBootStrap = new ServerBootstrap();
            wsBootStrap.group(BOSS_GROUP, WORKER_GROUP)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>()
                    {
                        @Override
                        public void initChannel(SocketChannel ch)
                        {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(MAX_FRAME_LENGTH));
                            ch.pipeline().addLast(WS_HANDLER);
                        }
                    });

             _wsChannel = wsBootStrap.bind(portWs).sync().channel();

            // Before the main thread got stuck waiting, load the module g_serial, if required
            SmartLogger.getLogger().info("Loading module g_serial (if necessary)...");
            loadModuleGSerial();

            // wait until the main channel (TCP/IP) is closed
            _channel.closeFuture().sync();
        } catch (InterruptedException ie)
        {
            Logger.getLogger(SmartServer.class.getName()).log(Level.SEVERE, "InterruptedException during execution of sync().", ie);
        } finally
        {
            WORKER_GROUP.shutdownGracefully();
            BOSS_GROUP.shutdownGracefully();
        }
    }

    /**
     * Close the connection with all connected clients.
     */
    private static void stop()
    {
        if(_channel != null)
        {
            _channel.close();
        }

        if(_wsChannel != null)
        {
            _wsChannel.close();
        }

        WORKER_GROUP.shutdownGracefully();
        BOSS_GROUP.shutdownGracefully();
    }

    /**
     * Called by ServerHandler/WebSocketHandler when a new connection is made with a client.
     *
     * @param newChannel Channel instance linking the new client to the server.
     */
    public static void addClientChannel(Channel newChannel, ChannelHandler handler)
    {
        if(handler == WS_HANDLER)
        {
            WS_CHAN_GROUP.add(newChannel);
        }

        else if (handler == TCP_IP_HANDLER)
        {
            TCP_IP_CHAN_GROUP.add(newChannel);
        }
    }

    /**
     * Send the given message using the given channel context. Add the END_OF_MESSAGE character at the end of the message.
     *
     * @param ctx       ChannelHandlerContext instance corresponding to the channel existing between
     *                  SmartServer and the new Client.
     *
     * @param packets   Message to be sent to the client.
     *
     * @return          A ChannelFuture instance, given by writeAndFlush method of ctx.
     */
    public static ChannelFuture sendMessage(ChannelHandlerContext ctx, String... packets)
    {
        if(ctx == null)
        {
            return null;
        }

        String message = MessageHandler.packetsToFullMessage(packets);

        if(message == null)
        {
            return null;
        }

        if(ctx.handler() == WS_HANDLER)
        {
            return ctx.writeAndFlush(new TextWebSocketFrame(message));
        }

        return ctx.writeAndFlush(message);
    }


    /**
     * Send the given message to all connected clients.
     * Also used to notify Device events.
     *
     * @param packets Message to be delivered to all clients.
     *
     * @return ChannelGroupFuture instance provided by ChannelGroup write() method.
     */
    public static ChannelGroupFuture sendAllClients(String... packets)
    {
        String message = MessageHandler.packetsToFullMessage(packets);

        if(message == null)
        {
            return null;
        }

        ChannelGroupFuture result = TCP_IP_CHAN_GROUP.write(message);
        WS_CHAN_GROUP.write(new TextWebSocketFrame(message));

        TCP_IP_CHAN_GROUP.flush();
        WS_CHAN_GROUP.flush();

        return result;
    }

    /**
     * Register a socket address as an administrator (a user authenticated with "SignInAdmin" command).
     * 
     * @param socketAddress Channel's socket address to be registered as administrator.
     *
     * @return True if the socket address has been added to the list of administrators.
     */
    public static boolean addAdministrator(SocketAddress socketAddress)
    {
        return socketAddress != null && ADMINISTRATORS.add(socketAddress);

    }

    /**
     * Remove a socket address from the administrators (users authenticated with "SignInAdmin" command).
     *
     * @param socketAddress Channel's socket address to be removed from the administrators.
     *
     * @return True if the socket address has been removed from the list of administrators.
     */
    public static boolean removeAdministrator(SocketAddress socketAddress)
    {
        return socketAddress != null && ADMINISTRATORS.remove(socketAddress);

    }

    /**
     * Allows knowing, for a given Socket Address (ip address + TCP port), if the channel is own by an Administrator
     * (a user authenticated with "SignInAdmin" command). 
     * 
     * @param socketAddress Channel's socket address to be checked.
     * 
     * @return True if the remote socket address is known in the administrator group. False otherwise.
     */
    public static boolean isAdministrator(SocketAddress socketAddress)
    {
        return socketAddress != null && ADMINISTRATORS.contains(socketAddress);
    }
}