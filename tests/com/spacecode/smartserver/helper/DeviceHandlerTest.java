package com.spacecode.smartserver.helper;

import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.device.DeviceCreationException;
import com.spacecode.sdk.device.data.DeviceType;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.device.data.PluggedDeviceInformation;
import com.spacecode.sdk.device.event.DeviceEventHandler;
import com.spacecode.sdk.network.communication.EventCode;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.UsersService;
import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.AuthenticationEntity;
import com.spacecode.smartserver.database.entity.InventoryEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.database.repository.AuthenticationRepository;
import com.spacecode.smartserver.database.repository.InventoryRepository;
import com.spacecode.smartserver.database.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * JUnit "DeviceHandler" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DeviceHandler.class, Device.class, SmartLogger.class, SmartServer.class,
        DbManager.class, Inventory.class })
public class DeviceHandlerTest
{
    private DeviceHandler.SmartEventHandler _eventHandler;
    private Device _device;
    private SmartLogger _smartLogger;

    private String _devSerial = "AA770201";
    private String _devPort = "COM6";
    private String _swVersion = "3.58";
    private String _hwVersion = "1";
    private DeviceType _devType = DeviceType.SMARTBOARD;

    @Before
    public void setUp() throws Exception
    {
        mockStatic(DbManager.class);
        mockStatic(SmartServer.class);
        mockStatic(SmartLogger.class);
        _device = PowerMockito.mock(Device.class);
        _smartLogger = PowerMockito.mock(SmartLogger.class);

        Whitebox.setInternalState(SmartLogger.class, "LOGGER", _smartLogger);
        doReturn(_smartLogger).when(SmartLogger.class, "getLogger");
        doNothing().when(_smartLogger).warning(anyString());
        whenNew(SmartLogger.class).withArguments(eq("SmartLogger"), anyString()).thenReturn(_smartLogger);

        Whitebox.setInternalState(DeviceHandler.class, _device);

        doReturn(_devSerial).when(_device).getSerialNumber();
        doReturn(_swVersion).when(_device).getSoftwareVersion();
        doReturn(_hwVersion).when(_device).getHardwareVersion();
        doReturn(_devType).when(_device).getDeviceType();

        _eventHandler = PowerMockito.mock(DeviceHandler.SmartEventHandler.class, CALLS_REAL_METHODS);
    }

    @Test
    public void testConnectDeviceAlreadyCreated() throws Exception
    {
        assertTrue(DeviceHandler.connectDevice());
    }

    @Test
    public void testConnectDeviceNoDevice() throws Exception
    {
        // _device must be null if we want to try to connect it
        Whitebox.setInternalState(DeviceHandler.class, "_device", (Object) null);

        mockStatic(Device.class);
        doReturn(new TreeMap<String, PluggedDeviceInformation>())
                .when(Device.class, "getPluggedDevicesInformation");

        assertFalse(DeviceHandler.connectDevice());
        verify(_smartLogger).warning(anyString());
    }

    @Test
    public void testConnectDeviceFailInstantiate() throws Exception
    {
        // _device must be null if we want to try to connect it
        Whitebox.setInternalState(DeviceHandler.class, "_device", (Object) null);

        // information about the mocked connected device
        Map<String, PluggedDeviceInformation> pluggedDevices = new TreeMap<>();
        pluggedDevices.put(_devSerial, new PluggedDeviceInformation(_devSerial, _devPort,
                _swVersion, _hwVersion, _devType));

        mockStatic(Device.class);
        doReturn(pluggedDevices).when(Device.class, "getPluggedDevicesInformation");

        whenNew(Device.class).withArguments(any(), eq(_devPort)).thenThrow(new DeviceCreationException(""));
        assertFalse(DeviceHandler.connectDevice());
        verify(_device, never()).addListener(any(DeviceEventHandler.class));
    }

    @Test
    public void testConnectDevice() throws Exception
    {
        // _device must be null if we want to try to connect it
        Whitebox.setInternalState(DeviceHandler.class, "_device", (Object) null);

        // information about the mocked connected device
        Map<String, PluggedDeviceInformation> pluggedDevices = new TreeMap<>();
        pluggedDevices.put(_devSerial, new PluggedDeviceInformation(_devSerial, _devPort,
                _swVersion, _hwVersion, _devType));

        mockStatic(Device.class);
        doReturn(pluggedDevices).when(Device.class, "getPluggedDevicesInformation");

        whenNew(Device.class).withArguments(any(), eq(_devPort)).thenReturn(_device);
        assertTrue(DeviceHandler.connectDevice());
        verify(_device).addListener(any(DeviceEventHandler.class));
    }

    @Test
    public void testDisconnectDevice() throws Exception
    {
        DeviceHandler.disconnectDevice();
        verify(_device).release();
    }

    @Test
    public void testReconnectDevice() throws Exception
    {

    }

    @Test
    public void testGetDevice() throws Exception
    {

    }

    @Test
    public void testConnectModules() throws Exception
    {

    }

    @Test
    public void testReloadTemperatureProbe() throws Exception
    {
        DeviceHandler.reloadTemperatureProbe();
        verify(_device).disconnectTemperatureProbe();
    }

    @Test
    public void testSetForwardingSerialPort() throws Exception
    {
        DeviceHandler.setForwardingSerialPort(true);
        assertTrue((boolean) Whitebox.getInternalState(DeviceHandler.class, "SERIAL_PORT_FORWARDING"));

        DeviceHandler.setForwardingSerialPort(false);
        assertFalse((boolean) Whitebox.getInternalState(DeviceHandler.class, "SERIAL_PORT_FORWARDING"));
    }

    @Test
    public void testIsForwardingSerialPort() throws Exception
    {
        Whitebox.setInternalState(DeviceHandler.class, "SERIAL_PORT_FORWARDING", true);
        assertTrue(DeviceHandler.isForwardingSerialPort());

        Whitebox.setInternalState(DeviceHandler.class, "SERIAL_PORT_FORWARDING", false);
        assertFalse(DeviceHandler.isForwardingSerialPort());
    }

    @Test
    public void testLoadAuthorizedUsersDeviceNull() throws Exception
    {
        Whitebox.setInternalState(DeviceHandler.class, "_device", (Object) null);
        assertFalse(DeviceHandler.loadAuthorizedUsers());
    }

    @Test
    public void testLoadAuthorizedUsers() throws Exception
    {
        User user1 =  new User("Vincent", GrantType.MASTER);

        List<User> authorizedUsers = Arrays.asList(
                new User("Jean", GrantType.ALL),
                new User("Mike", GrantType.SLAVE)
                );

        List<User> notAddedUsers = Arrays.asList(user1);

        UsersService usersService = PowerMockito.mock(UsersService.class);
        UserRepository userRepo = PowerMockito.mock(UserRepository.class);
        // get a fake list of users from DB
        doReturn(authorizedUsers).when(userRepo).getAuthorizedUsers();
        // assume that addUsers() returns a list (not empty) of "not added" users
        doReturn(notAddedUsers).when(usersService).addUsers(authorizedUsers);
        doReturn(usersService).when(_device).getUsersService();
        doReturn(userRepo).when(DbManager.class, "getRepository", UserEntity.class);

        assertTrue(DeviceHandler.loadAuthorizedUsers());
        verify(usersService).addUsers(authorizedUsers);
        // verify that smartlogger logged a warning because some users couldn't be loaded
        verify(_smartLogger).warning(anyString());
    }

    @Test
    public void testLoadLastInventoryNull() throws Exception
    {
        InventoryRepository inventoryRepo = PowerMockito.mock(InventoryRepository.class);
        doReturn(null).when(inventoryRepo).getLastInventory();
        doReturn(inventoryRepo).when(DbManager.class, "getRepository", InventoryEntity.class);

        assertFalse(DeviceHandler.loadLastInventory());
    }

    @Test
    public void testLoadLastInventory() throws Exception
    {
        Inventory lastInv = PowerMockito.mock(Inventory.class);
        InventoryRepository inventoryRepo = PowerMockito.mock(InventoryRepository.class);
        doReturn(lastInv).when(inventoryRepo).getLastInventory();
        doReturn(inventoryRepo).when(DbManager.class, "getRepository", InventoryEntity.class);

        assertTrue(DeviceHandler.loadLastInventory());
        verify(_device).setLastInventory(lastInv);
    }

    @Test
    public void testEventHandlerDeviceDisconnected() throws Exception
    {
        PowerMockito.mockStatic(DeviceHandler.class);

        _eventHandler.deviceDisconnected();

        PowerMockito.verifyStatic();
        DeviceHandler.reconnectDevice();
        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.DEVICE_DISCONNECTED);
    }

    @Test
    public void testEventHandlerDoorOpened() throws Exception
    {
        _eventHandler.doorOpened();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.DOOR_OPENED);
    }

    @Test
    public void testEventHandlerDoorClosed() throws Exception
    {
        _eventHandler.doorClosed();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.DOOR_CLOSED);
    }

    @Test
    public void testEventHandlerDoorOpenDelay() throws Exception
    {
        _eventHandler.doorOpenDelay();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.DOOR_OPEN_DELAY);
    }

    @Test
    public void testEventHandlerScanStarted() throws Exception
    {
        _eventHandler.scanStarted();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.SCAN_STARTED);
    }

    @Test
    public void testEventHandlerScanCancelledByHost() throws Exception
    {
        _eventHandler.scanCancelledByHost();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.SCAN_CANCELLED_BY_HOST);
    }

    @Test
    public void testEventHandlerScanCompleted() throws Exception
    {
        InventoryRepository inventoryRepo = PowerMockito.mock(InventoryRepository.class);
        doReturn(inventoryRepo).when(DbManager.class, "getRepository", InventoryEntity.class);

        _eventHandler.scanCompleted();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.SCAN_COMPLETED);

        verify(inventoryRepo).persist(any(Inventory.class));
    }

    @Test
    public void testEventHandlerScanFailed() throws Exception
    {
        _eventHandler.scanFailed();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.SCAN_FAILED);
    }

    @Test
    public void testEventHandlerTagAdded() throws Exception
    {
        String tagUid = "3001234567";
        _eventHandler.tagAdded(tagUid);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.TAG_ADDED, tagUid);
    }

    @Test
    public void testEventHandlerAuthenticationSuccess() throws Exception
    {
        AuthenticationRepository authenticationRepo = PowerMockito.mock(AuthenticationRepository.class);
        doReturn(authenticationRepo).when(DbManager.class, "getRepository", AuthenticationEntity.class);

        AccessType accessType = AccessType.BADGE;
        User user = new User("Vincent", GrantType.MASTER);

        _eventHandler.authenticationSuccess(user, accessType, true);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.AUTHENTICATION_SUCCESS, user.serialize(), accessType.name(),
                String.valueOf(true));

        verify(authenticationRepo).persist(user, accessType);
    }
}