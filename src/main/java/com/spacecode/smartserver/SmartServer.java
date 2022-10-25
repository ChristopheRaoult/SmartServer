package com.spacecode.smartserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.device.data.DeviceStatus;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.network.communication.MessageHandler;
import com.spacecode.smartserver.React.*;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoInventory;
import com.spacecode.smartserver.database.entity.InventoryEntity;
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
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import javafx.util.Pair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.SocketAddress;
import java.net.URI;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
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

    private static Timer _otgTimer;
    private static final long DELAY_MS_DETECT_OTG = 15 * 1000;
    private static double OTGPlugTime = 0.0;
    private static double OTGUnplugTime = 0.0;

    // default is 65536 (2^16), most of the time. Increase this value to 2^22. Max supported is Integer limit: 2^31-1
    public static final int MAX_FRAME_LENGTH = 4194304;

    // SocketIO
    private static  io.socket.client.Socket socket = null;
    //public static RegisterDeviceData _lastRegistrationDeviceData = null;
    //public static UserConnectData _lastUserConnectData = null;
    public static boolean IsLicenseValidated = false;
    public static boolean IsContinuousMode = false;
    private static boolean scanFlag = false;
    private static boolean forLed = false;
    private static boolean ledLighting = false;
    private static boolean bFirstConnection = true;

    //private static boolean InOneTImeSessionScanned = false;
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
        if (!DbManager.initializeDatabase()) {
            SmartLogger.getLogger().severe("Database could not be initialized. SmartServer will not start.");
            return;
        }

        if (!DeviceHandler.connectDevice()) {
            SmartLogger.getLogger().severe("Unable to connect a device. SmartServer will not start");
            return;
        }

        if (!init()) {
            SmartLogger.getLogger().severe("Unable to initialize SmartServer [init].");
            return;
        }

       /* SmartLogger.getLogger().info("Detect OTG ");
        OTGPlugTime  =  detectOtgPlugged();
        OTGUnplugTime = detectOtgUnPlugged();

        if (OTGPlugTime != 0.0 ) {
            if (OTGPlugTime > OTGUnplugTime) {
                SmartLogger.getLogger().log(Level.INFO, "OTG Plugged - redirect ttyUSB0 to ttyGS0");
                if (DoRedirection())
                    SmartLogger.getLogger().info("SmartServer is Ready in USB");
                else {
                    SmartLogger.getLogger().info("Error in redirection to USB");
                    SmartLogger.getLogger().info("SmartServer should be  Ready in Ethernet");
                }
            }
            else
            {
                SmartLogger.getLogger().info("Unplug OTG detected");
                SmartLogger.getLogger().info("SmartServer should be  Ready in Ethernet");
            }
        }
        else {
            SmartLogger.getLogger().info("OTG Not Found : Start SmartServer");
            SmartLogger.getLogger().info("SmartServer is Ready in Ethernet");
        }
        */
        //startTimer to test OTG
        _otgTimer = new Timer();
        _otgTimer.schedule(new RemindTask(), 0, DELAY_MS_DETECT_OTG);

        startListening();
    }


    /**
     * Change for OTG detecton issue
     * Function to read if otg connected a t start and run the socat redirection if nededed
     *
     */
    private static Double detectOtgPlugged()
    {
        Double LastPluggedTime = -0.0;
        Process process =  null;
        List<String> result  = new ArrayList<>();
        String pattern = "CDC ACM config";
        //String pattern = "usb";
        try
        {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "-c", "dmesg | grep \"" + pattern + "\"");

        builder.directory(new File(System.getProperty("user.home")));
        process = builder.start();

            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
               result.add(line);
            }
            process.waitFor();
            if (result.stream().count() > 0) {
                String LastPlugged  = result.get(result.size() - 1);
                String requiredString = LastPlugged .substring(LastPlugged .indexOf("[") + 1, LastPlugged .indexOf("]"));
                LastPluggedTime = Double.parseDouble(requiredString.trim());
                SmartLogger.getLogger().log(Level.INFO, "plug time: " + LastPluggedTime);
                return  LastPluggedTime;
            }
            else {
                SmartLogger.getLogger().log(Level.INFO, "Last Plug Time: " + LastPluggedTime);
                return LastPluggedTime;
            }
        }
        catch(Exception e)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Detect OTG error : ", e);
        }
        finally
        {
            process.destroy();
        }
        return   LastPluggedTime;
    }

    private static Double detectOtgUnPlugged()
    {
        Double LastUnPluggedTime = -0.0;
        Process process =  null;
        List<String> result  = new ArrayList<>();
        String pattern = "usb device plug out, stop pcd!!!";
        //String pattern = "usb";
        try
        {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("sh", "-c", "dmesg | grep \"" + pattern + "\"");

            builder.directory(new File(System.getProperty("user.home")));
            process = builder.start();

            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.add(line);
            }
            process.waitFor();
            if (result.stream().count() > 0) {
                String LastUnPlugged  = result.get(result.size() - 1);
                String requiredString = LastUnPlugged .substring(LastUnPlugged .indexOf("[") + 1, LastUnPlugged .indexOf("]"));
                LastUnPluggedTime = Double.parseDouble(requiredString.trim());
                SmartLogger.getLogger().log(Level.SEVERE, "Last Unplug Time: " + LastUnPluggedTime);
                return  LastUnPluggedTime;
            }
            else
            {
                SmartLogger.getLogger().log(Level.SEVERE, "Last Unplug time: " + LastUnPluggedTime);
                return  LastUnPluggedTime;
            }
        }
        catch(Exception e)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Detect OTG error : ", e);
        }
        finally
        {
            process.destroy();
        }
        return   LastUnPluggedTime;
    }

    private static boolean DoRedirection()
    {
        try
        {
            // tell the device handler that serial port is hooked, then it doesn't try to reconnect device
            DeviceHandler.setForwardingSerialPort(true);

            // disconnect the device, release the serial port
            DeviceHandler.disconnectDevice();

            // execute socat and proceed to "serial port forwarding"
            String devSerialPort = DeviceHandler.getSerialPortName();
            String socatCmd = "socat /dev/ttyGS0,raw,echo=0,crnl "+ devSerialPort +",raw,echo=0,crnl";
            new ProcessBuilder("/bin/sh", "-c", socatCmd).start();
            SmartLogger.getLogger().severe("Running Port Forwarding command.");
            return true;
        } catch (IOException ioe)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to run Port Forwarding command.", ioe);

            // reconnect to local Device
            DeviceHandler.setForwardingSerialPort(false);
            DeviceHandler.reconnectDevice();
            return false;
        }
    }
    private static boolean StopRedirection()
    {
        try
        {
            // stop the process (port forwarding)
            // NOTE: calling destroy() on Process instance does not stop "socat"...
            Process killingSocatProcess = new ProcessBuilder("/bin/sh", "-c", "pkill -f socat").start();
            killingSocatProcess.waitFor();

            SmartLogger.getLogger().severe("Stopped Port Forwarding command. Reconnecting Device...");

            // reconnect to local Device
            DeviceHandler.setForwardingSerialPort(false);
            DeviceHandler.reconnectDevice();
            return true;
        } catch (IOException | InterruptedException e)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to stop Port Forwarding command.", e);
            return false;
        }
    }
    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
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



            /******************************/
            try
            {
                //SelfSignedCertificate ssc = new SelfSignedCertificate("localhost");
                //final SslContext sslCtx =  SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
                // uncomment for SSL
                //ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));

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
            }
            catch(Exception exp)
            {
                Logger.getLogger(SmartServer.class.getName()).log(Level.SEVERE, "Exception in WebSocket Init", exp);
            }
            /*catch (CertificateException ce){
                Logger.getLogger(SmartServer.class.getName()).log(Level.SEVERE, "ICertificateException Exception raised.", ce);
            }
            catch(SSLException ssle){
                Logger.getLogger(SmartServer.class.getName()).log(Level.SEVERE, "SSLException Exception raised.", ssle);
            }*/
            /******************************/

            // Before the main thread got stuck waiting, load the module g_serial, if required
            SmartLogger.getLogger().info("Loading module g_serial (if necessary)...");
            loadModuleGSerial();
            //SOCKET IO Middleware
            //String SocketIOurl = "http://192.168.1.144:3000";
            String SocketIOurl = ConfManager.getAppUrlSocketIO();

            if (!SocketIOurl.isEmpty()) {
                SmartLogger.getLogger().info("Try Initialize socketIO on  : " + SocketIOurl);

                SmartLogger.getLogger().info("Version 1.0.1");
                SmartLogger.getLogger().info("Try Initialize socketIO on  : " + SocketIOurl);

                IO.Options options = IO.Options.builder()
                        .setQuery("token : v3")
                        .setQuery("EIO=4")
                        .build();
                socket = IO.socket((URI.create(SocketIOurl)), options);


                socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("Socket  Event CONNECT");
                        if (!bFirstConnection) {
                            connectionString connectionString = new connectionString() {{
                                deviceType = "service";
                            }};
                            ObjectMapper om = new ObjectMapper();
                            String dataJson = null;
                            try {
                                dataJson = om.writeValueAsString(connectionString);
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }
                            JSONObject jsonObj = null;
                            try {
                                jsonObj = new JSONObject(dataJson);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            SmartLogger.getLogger().info("send  : " + dataJson);
                            socket.emit("connection", jsonObj, new Ack() {
                                @Override
                                public void call(Object... objects) {
                                    SmartLogger.getLogger().info("Socket connected");
                                }
                            });
                        } else bFirstConnection = false;
                    }
                });

                socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("Socket  Event ERROR");
                    }
                });

                socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("Socket  Event DISCONNECT");
                        IsLicenseValidated = false;
                    }
                });

                socket.on("connectToRoom", new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("Connect to Room");
                    }
                });
                socket.on("getConnectedDevices", new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("getConnectedDevices: " + objects[0]);
                    }
                });
                socket.on("getDevices", new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("payload: " + objects[0]);
                        SmartLogger.getLogger().info("Fetching devices : ");
                        ObjectMapper om = new ObjectMapper();
                        String socketId = objects[0].toString();

                        List<DeviceModel> deviceList = new ArrayList<>();
                        DeviceModel dm = new DeviceModel() {{
                            socketId = socket.id().toString();
                            deviceId = DeviceHandler.getDevice().getSerialNumber();
                        }};
                        deviceList.add(dm);
                        ackGetDevices output = new ackGetDevices() {{
                            status = true;
                            message = "Device found";
                            devices = deviceList.toArray();
                        }};


                        ObjectMapper om2 = new ObjectMapper();

                        String dataJson = null;
                        try {
                            dataJson = om2.writeValueAsString(output);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        try {
                            JSONObject jsonObj = new JSONObject(dataJson);
                            SmartLogger.getLogger().info("send  : " + dataJson);
                            Ack fn = (Ack) objects[1];
                            fn.call(jsonObj);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                socket.on("connectDevice", new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("connectDevice payload: " + objects[0]);
                        ObjectMapper om = new ObjectMapper();
                        IsLicenseValidated = false;
                        try {
                            connectDeviceData cdd = om.readValue(objects[0].toString(), connectDeviceData.class);
                            if (!cdd.deviceId.contains(":"))  //not an IP
                            {
                                SmartLogger.getLogger().info("Checking for serial number");
                                if (Arrays.stream(cdd.deviceLisences).count() > 0) {
                                    boolean bFoundLic = false;
                                    for (Devicelisence licInfo : cdd.deviceLisences) {
                                        if (cdd.deviceId.equals(licInfo.serialNumber)) {
                                            bFoundLic = true;
                                            SimpleDateFormat parser = new SimpleDateFormat("MM/dd/yyyy");
                                            try {
                                                Date start = parser.parse(licInfo.startLicenseDate);
                                                Date end = parser.parse(licInfo.endLicenseDate);
                                                Date today = new Date();
                                                if (today.after(start) && today.before(end)) {
                                                    IsLicenseValidated = true;
                                                    ackDeviceConnected output1 = new ackDeviceConnected() {{
                                                        status = true;
                                                        message = "Device Connected";
                                                        deviceSerialNumber = cdd.deviceId;
                                                    }};
                                                    ackConnection ack = new ackConnection() {{
                                                        output = output1;
                                                        payload = cdd.payload;
                                                    }};
                                                    ObjectMapper om2 = new ObjectMapper();

                                                    String dataJson = null;
                                                    try {
                                                        dataJson = om2.writeValueAsString(ack);
                                                    } catch (JsonProcessingException e) {
                                                        e.printStackTrace();
                                                    }
                                                    try {
                                                        JSONObject jsonObj = new JSONObject(dataJson);
                                                        SmartLogger.getLogger().info("send  : " + dataJson);
                                                        Ack fn = (Ack) objects[1];
                                                        fn.call(jsonObj);
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                } else {
                                                    ackDeviceConnected output1 = new ackDeviceConnected() {{
                                                        status = false;
                                                        message = "Selected device is not having license or may be expired";

                                                    }};
                                                    ackConnection ack = new ackConnection() {{
                                                        output = output1;
                                                        payload = cdd.payload;
                                                    }};
                                                    ObjectMapper om2 = new ObjectMapper();

                                                    String dataJson = null;
                                                    try {
                                                        dataJson = om2.writeValueAsString(ack);
                                                    } catch (JsonProcessingException e) {
                                                        e.printStackTrace();
                                                    }
                                                    try {
                                                        JSONObject jsonObj = new JSONObject(dataJson);
                                                        SmartLogger.getLogger().info("send  : " + dataJson);
                                                        Ack fn = (Ack) objects[1];
                                                        fn.call(jsonObj);
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            } catch (ParseException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                    if (!bFoundLic) {
                                        ackDeviceConnected output1 = new ackDeviceConnected() {{
                                            status = false;
                                            message = "Licence not found";

                                        }};
                                        ackConnection ack = new ackConnection() {{
                                            output = output1;
                                            payload = cdd.payload;
                                        }};
                                        ObjectMapper om2 = new ObjectMapper();

                                        String dataJson = null;
                                        try {
                                            dataJson = om2.writeValueAsString(ack);
                                        } catch (JsonProcessingException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            JSONObject jsonObj = new JSONObject(dataJson);
                                            SmartLogger.getLogger().info("send  : " + dataJson);
                                            Ack fn = (Ack) objects[1];
                                            fn.call(jsonObj);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } else {
                                    ackDeviceConnected output1 = new ackDeviceConnected() {{
                                        status = false;
                                        message = "License not found.";

                                    }};
                                    ackConnection ack = new ackConnection() {{
                                        output = output1;
                                        payload = cdd.payload;
                                    }};
                                    ObjectMapper om2 = new ObjectMapper();

                                    String dataJson = null;
                                    try {
                                        dataJson = om2.writeValueAsString(ack);
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                    try {
                                        JSONObject jsonObj = new JSONObject(dataJson);
                                        SmartLogger.getLogger().info("send  : " + dataJson);
                                        Ack fn = (Ack) objects[1];
                                        fn.call(jsonObj);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                            } else {
                                ackDeviceConnected output1 = new ackDeviceConnected() {{
                                    status = false;
                                    message = "Device cannot be Ethernet";
                                }};
                                ackConnection ack = new ackConnection() {{
                                    output = output1;
                                    payload = cdd.payload;
                                }};
                                ObjectMapper om2 = new ObjectMapper();

                                String dataJson = null;
                                try {
                                    dataJson = om2.writeValueAsString(ack);
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    JSONObject jsonObj = new JSONObject(dataJson);
                                    SmartLogger.getLogger().info("send  : " + dataJson);
                                    Ack fn = (Ack) objects[1];
                                    fn.call(jsonObj);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                    }
                });
                socket.on("disconnectDevice", new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("Disconnect payload: " + objects[0]);
                        IsLicenseValidated = false;

                        ackDeviceDisconnected adc = new ackDeviceDisconnected() {{
                            status = true;
                            socketId = socket.id().toString();
                            deviceId = objects[0].toString();
                            message = "Device disconnected";
                        }};
                        ObjectMapper om2 = new ObjectMapper();

                        String dataJson = null;
                        try {
                            dataJson = om2.writeValueAsString(adc);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        try {
                            JSONObject jsonObj = new JSONObject(dataJson);
                            SmartLogger.getLogger().info("send  : " + dataJson);
                            Ack fn = (Ack) objects[1];
                            fn.call(jsonObj);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });
                socket.on("startScan", new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("StartScan payload: " + objects[0]);
                        ackStartScan output1 = null;
                        ObjectMapper om = new ObjectMapper();
                        try {
                            StartScanData ssd = om.readValue(objects[0].toString(), StartScanData.class);
                            if (ssd.scanMode.equals("continuous"))
                                IsContinuousMode = true;
                            else
                                IsContinuousMode = false;

                            if (ssd.deviceId.equals(DeviceHandler.getDevice().getSerialNumber())) {
                                if (IsLicenseValidated) {
                                    if (DeviceHandler.getDevice().getStatus() == DeviceStatus.READY || DeviceHandler.getDevice().getStatus() == DeviceStatus.LED_ON) {
                                        DeviceHandler.getDevice().requestScan();
                                        output1 = new ackStartScan() {{
                                            status = true;
                                            message = "Success";
                                        }};
                                    } else {
                                        output1 = new ackStartScan() {{
                                            status = false;
                                            message = "Device not ready";
                                        }};
                                    }
                                } else {
                                    output1 = new ackStartScan() {{
                                        status = false;
                                        message = "Invalid License";
                                    }};
                                }
                            } else {
                                output1 = new ackStartScan() {{
                                    status = false;
                                    message = "Invalid Device Serial";
                                }};
                            }
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                        ObjectMapper om2 = new ObjectMapper();
                        String dataJson = null;
                        try {
                            dataJson = om2.writeValueAsString(output1);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        try {
                            JSONObject jsonObj = new JSONObject(dataJson);
                            SmartLogger.getLogger().info("send  : " + dataJson);
                            Ack fn = (Ack) objects[1];
                            fn.call(jsonObj);
                        } catch (JSONException e) {
                            e.printStackTrace();

                        }
                    }
                });
                socket.on("stopScan", new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("StopScan payload: " + objects[0]);
                        ackStopScan output1 = null;
                        ObjectMapper om = new ObjectMapper();

                        try {
                            StopScanData ssd = om.readValue(objects[0].toString(), StopScanData.class);
                            if (ssd.deviceId.equals(DeviceHandler.getDevice().getSerialNumber())) {
                                if (IsLicenseValidated) {
                                    if (DeviceHandler.getDevice().getStatus() == DeviceStatus.SCANNING) {
                                        DeviceHandler.getDevice().stopScan();
                                        Thread.sleep(1000);
                                    }
                                    Inventory _lastInventory = ((DaoInventory) DbManager.getDao(InventoryEntity.class)).getLastInventory();
                                    output1 = new ackStopScan() {{
                                        status = true;
                                        message = "Scan stopped";
                                        deviceSerialNumber = DeviceHandler.getDevice().getSerialNumber();
                                        tags = _lastInventory.getTagsAll().toArray();
                                    }};

                                } else {
                                    output1 = new ackStopScan() {{
                                        status = false;
                                        message = "Invalid License";
                                        deviceSerialNumber = DeviceHandler.getDevice().getSerialNumber();
                                    }};
                                }
                            } else {
                                output1 = new ackStopScan() {{
                                    status = false;
                                    message = "Invalid Device Serial";
                                    deviceSerialNumber = DeviceHandler.getDevice().getSerialNumber();
                                }};
                            }

                        } catch (JsonProcessingException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        ackStopScan finalOutput = output1;
                        ackConnection2 ack = new ackConnection2() {{
                            output = finalOutput;
                            payload = objects[0].toString();
                        }};
                        ObjectMapper om2 = new ObjectMapper();
                        String dataJson = null;
                        try {
                            dataJson = om2.writeValueAsString(ack);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        try {
                            JSONObject jsonObj = new JSONObject(dataJson);
                            SmartLogger.getLogger().info("send  : " + dataJson);
                            Ack fn = (Ack) objects[1];
                            fn.call(jsonObj);
                        } catch (JSONException e) {
                            e.printStackTrace();

                        }
                    }
                });
                socket.on("refreshTags", new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("refreshTags payload: " + objects[0]);
                    }
                });
                socket.on("connectionStatus", new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("connectionStatus payload: " + objects[0]);
                    }
                });
                socket.on("ledOn", new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("Led on payload: " + objects[0]);
                        ObjectMapper om = new ObjectMapper();
                        try {
                            Pair<Boolean, String> resp = null;
                            ledOnData lod = om.readValue(objects[0].toString(), ledOnData.class);
                            if (IsLicenseValidated) {
                                if (lod.deviceId.equals(DeviceHandler.getDevice().getSerialNumber())) {

                                    if (!ledLighting) {
                                        ledLighting = true;
                                        boolean ret = DeviceHandler.getDevice().startLightingTagsLed(new ArrayList<>(Arrays.asList(lod.list)));
                                        if (ret)
                                            resp = new Pair<>(ret, "Led turned On");
                                        else
                                            resp = new Pair<>(ret, "Unable to turn on LED");
                                    } else
                                        resp = new Pair<>(false, "LED Already lighting");
                                } else {
                                    resp = new Pair<>(false, "Invalid Serial Number");
                                }
                            } else {
                                resp = new Pair<>(false, "Invalid License");
                            }
                            Pair<Boolean, String> finalResp = resp;
                            ackLedOn ack = new ackLedOn() {{
                                status = finalResp.getKey();
                                message = finalResp.getValue();
                            }};

                            ObjectMapper om2 = new ObjectMapper();
                            String dataJson = null;
                            try {
                                dataJson = om2.writeValueAsString(ack);
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }
                            try {
                                JSONObject jsonObj = new JSONObject(dataJson);
                                SmartLogger.getLogger().info("send  : " + dataJson);
                                Ack fn = (Ack) objects[1];
                                fn.call(jsonObj);
                            } catch (JSONException e) {
                                e.printStackTrace();

                            }


                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                });
                socket.on("ledOff", new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("led off payload: " + objects[0]);
                        Pair<Boolean, String> resp = null;
                        if (IsLicenseValidated) {
                            if (ledLighting) {
                                ledLighting = false;
                                boolean ret = DeviceHandler.getDevice().stopLightingTagsLed();
                                if (ret)
                                    resp = new Pair<>(ret, "Led turned Off");
                                else
                                    resp = new Pair<>(ret, "Unable to turn off LED");
                            } else
                                resp = new Pair<>(false, "LED Already stopped");
                        } else {
                            resp = new Pair<>(false, "Invalid License");
                        }

                        Pair<Boolean, String> finalResp = resp;
                        ackLedOn ack = new ackLedOn() {{
                            status = finalResp.getKey();
                            message = finalResp.getValue();
                        }};

                        ObjectMapper om2 = new ObjectMapper();
                        String dataJson = null;
                        try {
                            dataJson = om2.writeValueAsString(ack);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        try {
                            JSONObject jsonObj = new JSONObject(dataJson);
                            SmartLogger.getLogger().info("send  : " + dataJson);
                            Ack fn = (Ack) objects[1];
                            fn.call(jsonObj);
                        } catch (JSONException e) {
                            e.printStackTrace();

                        }

                    }
                });
                socket.on("oneTagScan", new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("one tag scan payload: " + objects[0]);
                    }
                });

                socket.connect();

                Thread.sleep(1000);
                connectionString connectionString = new connectionString() {{
                    deviceType = "service";
                }};
                ObjectMapper om = new ObjectMapper();
                String dataJson = om.writeValueAsString(connectionString);
                JSONObject jsonObj = new JSONObject(dataJson);
                SmartLogger.getLogger().info("send  : " + dataJson);
                socket.emit("connection", jsonObj, new Ack() {
                    @Override
                    public void call(Object... objects) {
                        SmartLogger.getLogger().info("Socket connected");
                    }
                });

                socket.emit("getConnectedDevices", "sdsd");

            }

        ///Socket IO BS
/*
            String SocketIOurl = ConfManager.getAppUrlSocketIO();

            SmartLogger.getLogger().info("Try Initialize socketIO on  : " +  SocketIOurl);
            //String url2 = "https://dev.bs.spacecode.in";
            IO.Options options = IO.Options.builder()
                    .setQuery("token : v3")
                    .setQuery("EIO=4")
                    .build();
            socket = IO.socket((URI.create(SocketIOurl)) , options);
            socket.on("Welcome", new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    SmartLogger.getLogger().info("received Welcome : " +  objects[0]);
                }
            });
            socket.on ("registerDevice" , new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    SmartLogger.getLogger().info("registerDevice Received : " + objects[0]);
                    ObjectMapper om = new ObjectMapper();
                    try {
                        _lastRegistrationDeviceData   = om.readValue(objects[0].toString(), RegisterDeviceData.class);
                        SmartLogger.getLogger().info("received token : " + _lastRegistrationDeviceData.body.token);
                    } catch (JsonProcessingException e) {
                        Logger.getLogger(SmartServer.class.getName()).log(Level.SEVERE, "registerDevice  Exception raised.", e);
                    }
                }
            });
            socket.on("userConnect", new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    Logger.getLogger(SmartServer.class.getName()).log(Level.INFO, "userConnect Received : " + objects[0]);
                    ObjectMapper om = new ObjectMapper();
                    try {
                        _lastUserConnectData   = om.readValue(objects[0].toString(), UserConnectData.class);
                        SmartLogger.getLogger().info("received user : " + _lastUserConnectData.userName);

                        final Payload pd = new Payload(){{
                            status = true;
                            message = "device is connected";
                            errorCode = null;
                        }};

                        userConnectEmit uce = new userConnectEmit(){
                            {
                                event_id  = 1 ;
                                token =  _lastRegistrationDeviceData.body.token;
                                userName = _lastUserConnectData.userName;
                                method = "E110";
                                payload = pd;
                            }};

                        String dataJson = om.writeValueAsString(uce);
                        SmartLogger.getLogger().info("send  : " + dataJson);
                        JSONObject jsonObj = new JSONObject(dataJson);
                        socket.emit("generic" , jsonObj);

                    } catch (JsonProcessingException | JSONException e) {
                        Logger.getLogger(SmartServer.class.getName()).log(Level.SEVERE, "userConnectEmit  Exception raised.", e);
                    }
                }
            });
            socket.on("oneTimeSessionScan", new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    SmartLogger.getLogger().info("received oneTimeSessionScan : " +objects[0]);
                    InOneTImeSessionScanned = true;
                    DeviceHandler.getDevice().requestScan();
                }
            });

            socket.connect();

            //register Device
            RegisterDeviceInfo rdi = new RegisterDeviceInfo() {{
                event_id = 1;
                serialNumber =  DeviceHandler.getDevice().getSerialNumber();
            }};
            ObjectMapper om = new ObjectMapper();
            String dataJson = null;
            try {
                dataJson = om.writeValueAsString(rdi);
            } catch (JsonProcessingException e) {
                Logger.getLogger(SmartServer.class.getName()).log(Level.SEVERE, "RegisterDeviceInfo Exception raised.", e);
            }
            SmartLogger.getLogger().info("send  RegisterDevice : " + dataJson);
            try {
                JSONObject jsonObj = new JSONObject(dataJson);
                socket.emit("registerDevice" , jsonObj);
            } catch (JSONException e) {
                Logger.getLogger(SmartServer.class.getName()).log(Level.SEVERE, "RegisterDevice Exception raised.", e);
            }
*/
                    // wait until the main channel (TCP/IP) is closed
                    _channel.closeFuture().sync();
        } catch (InterruptedException ie)
        {
            Logger.getLogger(SmartServer.class.getName()).log(Level.SEVERE, "InterruptedException during execution of sync().", ie);
        } catch (JsonProcessingException | JSONException e) {
            e.printStackTrace();
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

        if (IsLicenseValidated)
        {
            if (message.contains("event_scan_started"))
            {
                SmartLogger.getLogger().info("Scan started " );
                scanFlag = true;
                ackScanStarted ass = new ackScanStarted(){{
                    msg = "Scan Started";
                    sockectId = socket.id().toString();
                }};
                ObjectMapper om = new ObjectMapper();
                String dataJson = null;
                try {
                    dataJson = om.writeValueAsString(ass);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                JSONObject jsonObj = null;
                try {
                    jsonObj = new JSONObject(dataJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                SmartLogger.getLogger().info("send  : " + dataJson);
                socket.emit("scanStarted",jsonObj);

            }
            else if (message.contains("event_tag_added"))
            {
                int start = message.lastIndexOf('\u001C');
                int end = message.lastIndexOf('\u0004');
                String tag_Added  = message.substring(start + 1 ,end);
                SmartLogger.getLogger().info("Tag Added  " + tag_Added);
                ackTagAdded ata = new ackTagAdded(){{
                    str = "SERIAL-" + DeviceHandler.getDevice().getSerialNumber()+", TAG-" + tag_Added;
                    socketId = socket.id().toString();
                }};
                ObjectMapper om = new ObjectMapper();
                String dataJson = null;
                try {
                    dataJson = om.writeValueAsString(ata);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                JSONObject jsonObj = null;
                try {
                    jsonObj = new JSONObject(dataJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                SmartLogger.getLogger().info("send  : " + dataJson);
                socket.emit("addTag",jsonObj);

            }
            else if (message.contains("event_status_changed"))
            {
                int start = message.lastIndexOf('\u001C');
                int end = message.lastIndexOf('\u0004');
                String status = message.substring(start + 1 ,end);
                SmartLogger.getLogger().info("Rfid Status  " + status);
            }
            else if (message.contains("event_scan_completed")) {
                SmartLogger.getLogger().info("Scan completed");

                scanFlag = false;
                Inventory _lastInventory = ((DaoInventory) DbManager.getDao(InventoryEntity.class)).getLastInventory();
                ackScanCompleted asc = new ackScanCompleted(){{
                   status = true;
                   message = "Scan Completed";
                   tags = _lastInventory.getTagsAll().toArray();
                   socketId = socket.id().toString();
                }};
                ObjectMapper om = new ObjectMapper();
                String dataJson = null;
                try {
                    dataJson = om.writeValueAsString(asc);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                JSONObject jsonObj = null;
                try {
                    jsonObj = new JSONObject(dataJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                SmartLogger.getLogger().info("send  : " + dataJson);
                socket.emit("scanCompleted",jsonObj);

            }
            else
            {
                SmartLogger.getLogger().info("unknown msg " + message);
            }
        }

        /*if (InOneTImeSessionScanned )
        {
            if (message.contains("event_scan_completed"))
            {
                List<TagData> tags = new ArrayList<>();
                Inventory inv = DeviceHandler.getDevice().getLastInventory();

                PayloadAck  payload = new PayloadAck()
                {{
                    status = true;
                    errorCode = null;
                    message = "tags on the device";
                    data = new TagData[inv.getTagsAll().size()];
                }};
                Integer index = 0;
                for (String uid : inv.getTagsAll()) {
                    TagData td = new TagData(){{
                        tagId = uid;
                    }};
                    payload.data[index++]= td;
                }

                AcknowledgeObj ao = new AcknowledgeObj(){{
                    //event_id = "???",
                    userName = _lastUserConnectData != null ? _lastUserConnectData.userName : null;
                    token = _lastRegistrationDeviceData.body.token;
                    method = "E113";

                }};
                ao.payload = payload;
                ObjectMapper om = new ObjectMapper();
                String dataJson = null;
                try {
                    dataJson = om.writeValueAsString(ao);
                    SmartLogger.getLogger().info("Send onTag: " + dataJson);
                } catch (JsonProcessingException e) {
                    Logger.getLogger(SmartServer.class.getName()).log(Level.SEVERE, "OneTimeSession scan Exception raised.", e);
                }
                Logger.getLogger(SmartServer.class.getName()).log(Level.INFO, "send  OneTimeSession : " + dataJson);
                try {
                    JSONObject jsonObj = new JSONObject(dataJson);
                    SmartLogger.getLogger().info("Emit generic " + jsonObj.toString());
                    socket.emit("generic" , jsonObj);
                } catch (JSONException e) {
                    Logger.getLogger(SmartServer.class.getName()).log(Level.SEVERE, "Socket Ack Exception raised.", e);
                }
            }
        }*/
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

    public static class RemindTask extends TimerTask {
        public void run() {
            SmartLogger.getLogger().info("Detect OTG ");
            double newOTGplugTime = detectOtgPlugged();
            double newOTGUnplugTime = detectOtgUnPlugged();

            if (newOTGplugTime != 0.0 ) {   // there is a plug after start
                SmartLogger.getLogger().info("OTG was plugged ");
                if (OTGPlugTime > OTGUnplugTime) { // we are in USB , test unplug
                    SmartLogger.getLogger().info("test for new unplug");
                    if (newOTGUnplugTime > OTGUnplugTime) // new plug
                    {
                        SmartLogger.getLogger().log(Level.INFO, "OTG UnPlugged -stop redirection");
                        if (StopRedirection()){
                            SmartLogger.getLogger().info("SmartServer is Ready in Ethernet");
                            OTGUnplugTime = newOTGUnplugTime;
                        }
                        else {
                            SmartLogger.getLogger().info("Error in Stop redirection ");
                        }
                    }
                }
                else  // were ar in ethernet test plug
                {
                    SmartLogger.getLogger().info("test for new plug");
                    if (newOTGplugTime > OTGPlugTime) // new plug
                    {
                        SmartLogger.getLogger().log(Level.INFO, "OTG Plugged - redirect ttyUSB0 to ttyGS0");
                        if (DoRedirection()) {
                            OTGPlugTime = newOTGplugTime;
                            SmartLogger.getLogger().info("SmartServer is Ready in USB");
                        }
                        else {
                            SmartLogger.getLogger().info("Error in redirection to USB");
                            SmartLogger.getLogger().info("SmartServer should be  Ready in Ethernet");
                        }
                    }
                }
            }
        }
    }
}