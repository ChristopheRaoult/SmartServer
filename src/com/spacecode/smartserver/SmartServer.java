package com.spacecode.smartserver;

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.spacecode.sdk.network.communication.MessageHandler;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.DeviceEntity;
import com.spacecode.smartserver.helper.*;
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
import java.sql.SQLException;
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

    private static Channel _channel;
    private static Channel _wsChannel;

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
     *     <li>Start the Temperature center</li>
     * </ul>
     * *: Critical operations. SmartServer won't start if one fails.</p>
     * <p>3 - Start the asynchronous Server.</p>
     */
    public static void main(String[] args) throws IOException, SQLException
    {
        // SDK use Global logger. Only display its SEVERE logs.
        Logger.getGlobal().setLevel(Level.SEVERE);

        SmartLogger.initialize();
        initializeShutdownHook();

        // Initialize database connection and (if required) model
        JdbcPooledConnectionSource connectionSource = DbManager.initializeDatabase();

        if(connectionSource == null)
        {
            SmartLogger.getLogger().severe("Database couldn't be initialized. SmartServer won't start.");
            return;
        }

        // TODO: execute "update.sql" if any is found (in SmartServer.getWorkingDirectory()), then REMOVE IT.

        if(DeviceHandler.connectDevice())
        {
            String devSerialNumber = DeviceHandler.getDevice().getSerialNumber();

            SmartLogger.getLogger().info(DeviceHandler.getDevice().getDeviceType() + ": " + devSerialNumber);

            // Get device configuration from database (see DeviceEntity class)
            DeviceEntity deviceEntity = DbManager.getDevEntity();

            // No configuration: stop SmartServer.
            if(deviceEntity == null)
            {
                SmartLogger.getLogger().warning("Device not configured. Creating a new entry in Database...");

                if(!DbManager.getRepository(DeviceEntity.class).insert(new DeviceEntity(devSerialNumber)))
                {
                    SmartLogger.getLogger().severe("Could not create a new entry in Database, SmartServer will stop.");
                    return;
                }

                if(DbManager.getDevEntity() == null)
                {
                    SmartLogger.getLogger().severe("New device entry created, but an error occurred while loading it.");
                    return;
                }
            }

            // Use the configuration to connect/load modules.
            // TODO: do something if any failure
            DeviceHandler.connectModules();

            // Load users from DB into Device's UsersService.
            if(!DeviceHandler.loadAuthorizedUsers())
            {
                SmartLogger.getLogger().severe("Users couldn't be loaded from database. SmartServer won't start.");
                return;
            }

            // Load last inventory from DB and load it into device.
            if(!DeviceHandler.loadLastInventory())
            {
                SmartLogger.getLogger().info("No \"previous\" Inventory was loaded because none was found.");
            }

            if(!AlertCenter.initialize())
            {
                SmartLogger.getLogger().severe("Couldn't start AlertCenter.");
            }

            if(ConfManager.isDevTemperature())
            {
                if(!TemperatureCenter.initialize())
                {
                    SmartLogger.getLogger().severe("Couldn't start TemperatureCenter.");
                }
            }

            SmartLogger.getLogger().info("SmartServer is Ready");
        }

        else
        {
            SmartLogger.getLogger().warning("Unable to connect to a SpaceCode RFID device...");
        }

        start();
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
                DeviceHandler.disconnectDevice();
                DbManager.close();
                stop();
            }
        }));
    }

    /**
     * Entry point of SmartServer.
     * Instantiate ServerBootstrap, SmartServerHandler, and configure the server channel.
     */
    private static void start()
    {
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
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(32768,
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
            _channel = tcpIpBootStrap.bind(TCP_IP_PORT).sync().channel();

            ServerBootstrap wsBootStrap = new ServerBootstrap();
            wsBootStrap.group(BOSS_GROUP, WORKER_GROUP)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>()
                    {
                        @Override
                        public void initChannel(SocketChannel ch)
                        {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            ch.pipeline().addLast(WS_HANDLER);
                        }
                    });

             _wsChannel = wsBootStrap.bind(WS_PORT).sync().channel();

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
        String message = MessageHandler.packetsToFullMessage(packets);

        if(message == null)
        {
            return null;
        }

        if(ctx.handler() == WS_HANDLER)
        {
            ctx.writeAndFlush(new TextWebSocketFrame(message));
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
}