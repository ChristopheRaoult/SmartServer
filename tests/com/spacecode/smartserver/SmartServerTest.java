package com.spacecode.smartserver;

import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.network.communication.EventCode;
import com.spacecode.sdk.network.communication.MessageHandler;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.helper.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * JUnit "SmartServer" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SmartServer.class, DbManager.class, DeviceHandler.class, SmartLogger.class,
        ConfManager.class, AlertCenter.class, TemperatureCenter.class })
public class SmartServerTest
{
    @Before
    public void setUp()
    {
        mockStatic(SmartServer.class);
        mockStatic(SmartLogger.class);
        mockStatic(DbManager.class);
        mockStatic(DeviceHandler.class);
        mockStatic(ConfManager.class);
        mockStatic(AlertCenter.class);
        mockStatic(TemperatureCenter.class);
    }

    @Test
    public void testMainFailInitializingDb() throws Exception
    {
        when(SmartServer.class, "main", (Object) null).thenCallRealMethod();
        doReturn(false).when(DbManager.class, "initializeDatabase");

        SmartServer.main(null);

        verifyStatic();
        DbManager.initializeDatabase();
        // if the db couldn't be initialized, then SmartServer did not try to connect the device
        verifyPrivate(DeviceHandler.class, never()).invoke("connectDevice");
        // do not start SmartServer if the DB couldn't be initialized / connected
        verifyPrivate(SmartServer.class, never()).invoke("startListening");

        /*
        verifyStatic();
        DeviceHandler.connectDevice();*/
    }

    @Test
    public void testMainFailConnectingDevice() throws Exception
    {
        when(SmartServer.class, "main", (Object) null).thenCallRealMethod();
        doReturn(true).when(DbManager.class, "initializeDatabase");
        doReturn(false).when(DeviceHandler.class, "connectDevice");

        SmartServer.main(null);

        verifyStatic();
        DbManager.initializeDatabase();
        verifyStatic();
        DeviceHandler.connectDevice();

        // the next step was calling init() (if the device had been connected)
        verifyPrivate(SmartServer.class, never()).invoke("init");
        // If no device was found / connected, do not start SmartServer
        verifyPrivate(SmartServer.class, never()).invoke("startListening");
    }

    @Test
    public void testMainFailInit() throws Exception
    {
        when(SmartServer.class, "main", (Object) null).thenCallRealMethod();
        doReturn(true).when(DbManager.class, "initializeDatabase");
        doReturn(true).when(DeviceHandler.class, "connectDevice");
        doReturn(false).when(SmartServer.class, "init");

        SmartServer.main(null);

        verifyStatic();
        DbManager.initializeDatabase();
        verifyStatic();
        DeviceHandler.connectDevice();

        verifyPrivate(SmartServer.class).invoke("init");

        // If the device could be connected but initialization failed: do not start SmartServer
        verifyPrivate(SmartServer.class, never()).invoke("startListening");
    }

    @Test
    public void testMain() throws Exception
    {
        when(SmartServer.class, "main", (Object) null).thenCallRealMethod();
        doReturn(true).when(DbManager.class, "initializeDatabase");
        doReturn(true).when(DeviceHandler.class, "connectDevice");
        doReturn(true).when(SmartServer.class, "init");

        SmartServer.main(null);

        verifyStatic();
        DbManager.initializeDatabase();
        verifyStatic();
        DeviceHandler.connectDevice();
        verifyPrivate(SmartServer.class).invoke("init");

        // SmartServer successfully initialized: start listening
        verifyPrivate(SmartServer.class).invoke("startListening");
    }

    @Test
    public void testInitNullDevice() throws Exception
    {
        when(SmartServer.class, "init").thenCallRealMethod();

        doReturn(null).when(DeviceHandler.class, "getDevice");
        assertFalse((boolean) Whitebox.invokeMethod(SmartServer.class, "init"));
    }

    @Test
    public void testInitDbFailsCreatingDevice() throws Exception
    {
        when(SmartServer.class, "init").thenCallRealMethod();

        String serialNumber = "AA777031";
        Device device = PowerMockito.mock(Device.class);
        doReturn(serialNumber).when(device).getSerialNumber();
        doReturn(device).when(DeviceHandler.class, "getDevice");
        doReturn(false).when(DbManager.class, "createDeviceIfNotExists", serialNumber);

        assertFalse((boolean) Whitebox.invokeMethod(SmartServer.class, "init"));

        verify(device).getSerialNumber();
        verifyStatic();
        DbManager.createDeviceIfNotExists(serialNumber);
    }

    @Test
    public void testInitFailLoadingUsers() throws Exception
    {
        when(SmartServer.class, "init").thenCallRealMethod();

        String serialNumber = "AA777031";
        Device device = PowerMockito.mock(Device.class);
        doReturn(serialNumber).when(device).getSerialNumber();
        doReturn(device).when(DeviceHandler.class, "getDevice");
        doReturn(true).when(DbManager.class, "createDeviceIfNotExists", serialNumber);
        doReturn(false).when(DeviceHandler.class, "loadAuthorizedUsers");

        assertFalse((boolean) Whitebox.invokeMethod(SmartServer.class, "init"));

        verify(device).getSerialNumber();
        verifyStatic();
        DbManager.createDeviceIfNotExists(serialNumber);
        verifyStatic();
        DeviceHandler.connectModules();
    }

    @Test
    public void testInit() throws Exception
    {
        when(SmartServer.class, "init").thenCallRealMethod();

        String serialNumber = "AA777031";
        Device device = PowerMockito.mock(Device.class);
        doReturn(serialNumber).when(device).getSerialNumber();
        doReturn(device).when(DeviceHandler.class, "getDevice");
        doReturn(true).when(DbManager.class, "createDeviceIfNotExists", serialNumber);
        doReturn(true).when(DeviceHandler.class, "loadAuthorizedUsers");
        doReturn(true).when(AlertCenter.class, "initialize");
        doReturn(true).when(ConfManager.class, "isDevTemperature");
        doReturn(true).when(TemperatureCenter.class, "initialize");

        assertTrue((boolean) Whitebox.invokeMethod(SmartServer.class, "init"));

        verify(device).getSerialNumber();
        verifyStatic();
        DbManager.createDeviceIfNotExists(serialNumber);
        verifyStatic();
        DeviceHandler.connectModules();
        verifyStatic();
        DeviceHandler.loadAuthorizedUsers();
        verifyStatic();
        DeviceHandler.loadLastInventory();
        verifyStatic();
        AlertCenter.initialize();
        verifyStatic();
        ConfManager.isDevTemperature();
        verifyStatic();
        TemperatureCenter.initialize();
    }

    @Test
    public void testAddClientChannelTcpIp() throws Exception
    {
        Channel ctx = PowerMockito.mock(Channel.class);
        ChannelHandler tcpIpHandler = PowerMockito.mock(ChannelHandler.class);
        ChannelGroup webSocketGroup = PowerMockito.mock(ChannelGroup.class);
        ChannelGroup tcpIpGroup = PowerMockito.mock(ChannelGroup.class);

        Whitebox.setInternalState(SmartServer.class, "TCP_IP_HANDLER", tcpIpHandler);
        Whitebox.setInternalState(SmartServer.class, "WS_CHAN_GROUP", webSocketGroup);
        Whitebox.setInternalState(SmartServer.class, "TCP_IP_CHAN_GROUP", tcpIpGroup);

        when(SmartServer.class, "addClientChannel", any(Channel.class), any(ChannelHandler.class)).thenCallRealMethod();

        SmartServer.addClientChannel(ctx, tcpIpHandler);
        verify(webSocketGroup, never()).add(ctx);
        verify(tcpIpGroup).add(ctx);
    }

    @Test
    public void testAddClientChannelWebSocket() throws Exception
    {
        Channel ctx = PowerMockito.mock(Channel.class);
        ChannelHandler webSocketHandler = PowerMockito.mock(ChannelHandler.class);
        ChannelGroup webSocketGroup = PowerMockito.mock(ChannelGroup.class);
        ChannelGroup tcpIpGroup = PowerMockito.mock(ChannelGroup.class);

        Whitebox.setInternalState(SmartServer.class, "WS_HANDLER", webSocketHandler);
        Whitebox.setInternalState(SmartServer.class, "WS_CHAN_GROUP", webSocketGroup);
        Whitebox.setInternalState(SmartServer.class, "TCP_IP_CHAN_GROUP", tcpIpGroup);

        when(SmartServer.class, "addClientChannel", any(Channel.class), any(ChannelHandler.class)).thenCallRealMethod();

        SmartServer.addClientChannel(ctx, webSocketHandler);
        verify(tcpIpGroup, never()).add(ctx);
        verify(webSocketGroup).add(ctx);
    }

    @Test
    public void testAddClientChannelUnknownHandler() throws Exception
    {
        Channel ctx = PowerMockito.mock(Channel.class);
        ChannelHandler unknownHandler = PowerMockito.mock(ChannelHandler.class);
        ChannelGroup webSocketGroup = PowerMockito.mock(ChannelGroup.class);
        ChannelGroup tcpIpGroup = PowerMockito.mock(ChannelGroup.class);

        Whitebox.setInternalState(SmartServer.class, "WS_CHAN_GROUP", webSocketGroup);
        Whitebox.setInternalState(SmartServer.class, "TCP_IP_CHAN_GROUP", tcpIpGroup);

        when(SmartServer.class, "addClientChannel", any(Channel.class), any(ChannelHandler.class)).thenCallRealMethod();

        SmartServer.addClientChannel(ctx, unknownHandler);
        verify(tcpIpGroup, never()).add(ctx);
        verify(webSocketGroup, never()).add(ctx);
    }

    @Test
    public void testSendMessageNullParams() throws Exception
    {
        assertNull(SmartServer.sendMessage(null, (String) null));
        assertNull(SmartServer.sendMessage(null, RequestCode.ADD_ALERT, null));

        ChannelHandlerContext ctx = PowerMockito.mock(ChannelHandlerContext.class);
        ChannelFuture cf = PowerMockito.mock(ChannelFuture.class);
        doReturn(cf).when(ctx).writeAndFlush(anyString());

        assertNull(SmartServer.sendMessage(ctx, (String) null));
        verify(ctx, never()).writeAndFlush(anyString());
    }

    @Test
    public void testSendMessage() throws Exception
    {
        ChannelHandlerContext ctx = PowerMockito.mock(ChannelHandlerContext.class);
        ChannelFuture cf = PowerMockito.mock(ChannelFuture.class);
        doReturn(cf).when(ctx).writeAndFlush(anyString());

        when(SmartServer.class, "sendMessage", eq(ctx), anyString(), anyString()).thenCallRealMethod();

        assertNotNull(SmartServer.sendMessage(ctx, RequestCode.ADD_ALERT, String.valueOf(true)));
        verify(ctx).writeAndFlush(RequestCode.ADD_ALERT +
                MessageHandler.DELIMITER +
                "true" +
                MessageHandler.END_OF_MESSAGE);
    }

    @Test
    public void testSendAllClientsNullMessage() throws Exception
    {
        ChannelGroup webSocketGroup = PowerMockito.mock(ChannelGroup.class);
        ChannelGroup tcpIpGroup = PowerMockito.mock(ChannelGroup.class);
        Whitebox.setInternalState(SmartServer.class, "WS_CHAN_GROUP", webSocketGroup);
        Whitebox.setInternalState(SmartServer.class, "TCP_IP_CHAN_GROUP", tcpIpGroup);

        ChannelGroupFuture cgf = PowerMockito.mock(ChannelGroupFuture.class);
        doReturn(cgf).when(tcpIpGroup).write(anyString());

        when(SmartServer.class, "sendAllClients", anyString(), anyString()).thenCallRealMethod();

        assertNull(SmartServer.sendAllClients((String) null));
        assertNull(SmartServer.sendAllClients(EventCode.ENROLLMENT_SAMPLE, null));
        verify(tcpIpGroup, never()).write(anyString());
        verify(webSocketGroup, never()).write(anyString());
    }

    @Test
    public void testSendAllClients() throws Exception
    {
        ChannelGroup webSocketGroup = PowerMockito.mock(ChannelGroup.class);
        ChannelGroup tcpIpGroup = PowerMockito.mock(ChannelGroup.class);
        Whitebox.setInternalState(SmartServer.class, "WS_CHAN_GROUP", webSocketGroup);
        Whitebox.setInternalState(SmartServer.class, "TCP_IP_CHAN_GROUP", tcpIpGroup);

        ChannelGroupFuture cgf = PowerMockito.mock(ChannelGroupFuture.class);
        doReturn(cgf).when(tcpIpGroup).write(anyString());

        when(SmartServer.class, "sendAllClients", anyString(), anyString()).thenCallRealMethod();

        assertNotNull(SmartServer.sendAllClients(EventCode.TEMPERATURE_MEASURE, "4.5"));
        verify(tcpIpGroup).write(EventCode.TEMPERATURE_MEASURE +
                MessageHandler.DELIMITER +
                "4.5" +
                MessageHandler.END_OF_MESSAGE);
        verify(webSocketGroup).write(any(TextWebSocketFrame.class));

        verify(tcpIpGroup).flush();
        verify(webSocketGroup).flush();
    }
}