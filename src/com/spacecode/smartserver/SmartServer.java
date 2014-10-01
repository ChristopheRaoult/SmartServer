package com.spacecode.smartserver;

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.network.communication.MessageHandler;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.DeviceEntity;
import com.spacecode.smartserver.helper.AlertCenter;
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
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SmartServer "Main" class.
 * Relies on Netty to start an Asynchronous TCP server.
 */
public final class SmartServer
{
    private static final int TCP_PORT = 8080;

    private static final EventLoopGroup BOSS_GROUP = new NioEventLoopGroup();
    private static final EventLoopGroup WORKER_GROUP = new NioEventLoopGroup();

    /** List of active channels (between SmartServer and clients). */
    private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private static Channel _channel;

    /** Must not be instantiated. */
    private SmartServer()
    {
    }

    /**
     * Entry point:
     * 1 - Initialize a shutdown hook to free Device and Database.
     * 2 - Following critical operations: (application stop if one operation of the list fails)
     * <ul>
     *     <li>Try to initialize/connect to Database</li>
     *     <li>Try to initialize/connect to Device/li>
     *     <li>Load granted users/li>
     *     <li>Load last inventory*</li>
     *     <li>Start the Alert center*</li>
     *     <li>Start the Temperature center*</li>
     * </ul>
     * *: Not a critical operation.
     * 3 - Start the asynchronous Server.
     */
    public static void main(String[] args) throws IOException, SQLException
    {
        SmartLogger.initialize();
        initializeShutdownHook();

        SmartLogger.getLogger().info("Initializing database...");
        JdbcPooledConnectionSource connectionSource = DatabaseHandler.initializeDatabase();

        if(connectionSource == null)
        {
            SmartLogger.getLogger().severe("Database couldn't be initialized. SmartServer won't start.");
            return;
        }

        SmartLogger.getLogger().info("Database initialized.");

        SmartLogger.getLogger().info("Connecting to local device...");

        if(DeviceHandler.connectDevice())
        {
            SmartLogger.getLogger().info("Connected to " + DeviceHandler.getDevice().getDeviceType() + " (" + DeviceHandler.getDevice().getSerialNumber() + ")");

            // Get device configuration from database (see DeviceEntity class)
            DeviceEntity deviceConfig = DatabaseHandler.getDeviceConfiguration();

            // No configuration: stop SmartServer.
            if(deviceConfig == null)
            {
                SmartLogger.getLogger().severe("Device not configured. SmartServer won't start. Please create a Device Configuration.");
                return;
            }

            // Use the configuration to connect/load modules.
            // TODO: do something if any failure (see directly in method)
            DeviceHandler.connectModules(deviceConfig);

            // Load users from DB into UsersService.
            SmartLogger.getLogger().info("Loading users...");

            if(!DatabaseHandler.loadGrantedUsers())
            {
                SmartLogger.getLogger().severe("Users couldn't be loaded from database. SmartServer won't start.");
                return;
            }

            SmartLogger.getLogger().info("Users loaded.");

            // Load last inventory from DB and load it into device.
            Inventory lastInventoryFromDb = DatabaseHandler.getLastStoredInventory();

            if(lastInventoryFromDb != null)
            {
                DeviceHandler.getDevice().setLastInventory(lastInventoryFromDb);
                SmartLogger.getLogger().info("Last inventory loaded in RfidDevice memory.");
            }

            SmartLogger.getLogger().info("Start AlertCenter...");

            if(!AlertCenter.initialize())
            {
                SmartLogger.getLogger().severe("Couldn't start AlertCenter.");
            }

            SmartLogger.getLogger().info("Start TemperatureCenter...");

            if(deviceConfig.isTemperatureEnabled() && !TemperatureCenter.initialize())
            {
                SmartLogger.getLogger().severe("Couldn't start TemperatureCenter.");
            }
        }

        else
        {
            SmartLogger.getLogger().warning("Unable to connect to a SpaceCode RFID device...");
        }

        start(TCP_PORT);
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
                DatabaseHandler.close();
                stop();
            }
        }));
    }

    /**
     * Entry point of SmartServer.
     * Instantiate the (netty) ServerBootstrap, the SmartServerHandler, and configure the server channel.
     */
    private static void start(int port)
    {
        try
        {
            ServerBootstrap b = new ServerBootstrap();
            b.group(BOSS_GROUP, WORKER_GROUP)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>()
                    {
                        @Override
                        public void initChannel(SocketChannel ch)
                        {
                            // Define character EOT (0x04) as an end-of-frame character.
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(8192, Unpooled.wrappedBuffer(new byte[] {MessageHandler.END_OF_MESSAGE})));
                            // Allow sending/receiving string instead of byte buffers.
                            ch.pipeline().addLast(new StringDecoder(), new StringEncoder());
                            // Add a SmartServerHandler instance to the channel pipeline.
                            ch.pipeline().addLast(new SmartServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            _channel = b.bind(port).sync().channel();
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

        WORKER_GROUP.shutdownGracefully();
        BOSS_GROUP.shutdownGracefully();
    }

    /**
     * Called by ServerHandler when a new connection is made with a client.
     *
     * @param newChannel Channel instance linking the new client to the server.
     */
    public static void addChannel(Channel newChannel)
    {
        CHANNEL_GROUP.add(newChannel);
    }

    /**
     * Send the given message using the given channel context. Add the END_OF_MESSAGE character at the end of the message.
     *
     * @param ctx       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the new Client.
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

        ChannelGroupFuture result = CHANNEL_GROUP.write(message);
        CHANNEL_GROUP.flush();
        return result;
    }
}