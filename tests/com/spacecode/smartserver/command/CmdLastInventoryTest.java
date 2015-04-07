package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoInventory;
import com.spacecode.smartserver.database.entity.InventoryEntity;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Date;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * JUnit "CmdLastInventory" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ CmdLastInventory.class, SmartServer.class, DbManager.class, DaoInventory.class,
        DeviceHandler.class, Device.class, Inventory.class })
public class CmdLastInventoryTest
{
    private ChannelHandlerContext _ctx;
    private CmdLastInventory _command;
    private DaoInventory _daoInventory;

    private Device _device;
    private Inventory _inventory1;
    private Inventory _inventory2;

    @Before
    public void setUp() throws Exception
    {
        _ctx = PowerMockito.mock(ChannelHandlerContext.class);
        _command = PowerMockito.mock(CmdLastInventory.class, Mockito.CALLS_REAL_METHODS);
        _daoInventory = PowerMockito.mock(DaoInventory.class);

        _device = PowerMockito.mock(Device.class);
        _inventory1 = PowerMockito.mock(Inventory.class);
        _inventory2 = PowerMockito.mock(Inventory.class);

        PowerMockito.mockStatic(SmartServer.class);
        PowerMockito.mockStatic(DeviceHandler.class);
        PowerMockito.mockStatic(DbManager.class);
    }

    @After
    public void tearDown()
    {
        _ctx = null;
        _command = null;
        _device = null;
        _daoInventory = null;
        _inventory1 = null;
        _inventory2 = null;
    }

    @Test
    public void testExecuteNullDevice() throws Exception
    {
        doReturn(null).when(DeviceHandler.class, "getDevice");

        _command.execute(_ctx, null);

        verifyStatic(never());
        SmartServer.sendMessage(eq(_ctx), eq(RequestCode.LAST_INVENTORY), anyString());
    }

    @Test
    public void testExecuteNoCache() throws Exception
    {
        doReturn(_device).when(DeviceHandler.class, "getDevice");
        Whitebox.setInternalState(_command, "_lastInventory", (Object) null);

        String serializedInventory = "serialized_inventory";
        doReturn(serializedInventory).when(_inventory1).serialize();
        doReturn(_inventory1).when(_daoInventory).getLastInventory();
        doReturn(_daoInventory).when(DbManager.class, "getDao", InventoryEntity.class);

        _command.execute(_ctx, null);

        verifyPrivate(_command).invoke("getAndSendLastInventory", _ctx);
        verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.LAST_INVENTORY, serializedInventory);
    }

    @Test
    public void testExecuteWithCacheUpToDate() throws Exception
    {
        // assume that the last inventory in cache has the same "creation date" that the last inventory of Device
        Date lastInvDate = new Date(123456789);
        String serializedInventory = "serialized_cache_inventory";
        doReturn(serializedInventory).when(_inventory1).serialize();
        doReturn(lastInvDate).when(_inventory1).getCreationDate();
        doReturn(lastInvDate).when(_inventory2).getCreationDate();

        // inventory1 is the cache, inventory2 the last inventory known by the Device
        Whitebox.setInternalState(_command, "_lastInventory", _inventory1);
        doReturn(_inventory2).when(_device).getLastInventory();
        doReturn(_device).when(DeviceHandler.class, "getDevice");

        _command.execute(_ctx, null);

        // make sure we're sending the inventory in cache and not getting one from the DB
        verifyPrivate(_command, never()).invoke("getAndSendLastInventory", _ctx);
        verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.LAST_INVENTORY, serializedInventory);
    }

    @Test
    public void testExecuteWithCacheOutOfDate() throws Exception
    {
        // assume that the last inventory in cache is outdated
        Date cacheInvDate = new Date(123456);
        Date lastInvDate = new Date(123457);
        String serializedOldInv = "old_inventory";
        String serializedNewInv = "new_inventory";
        doReturn(serializedOldInv).when(_inventory2).serialize();
        doReturn(serializedNewInv).when(_inventory2).serialize();
        doReturn(cacheInvDate).when(_inventory1).getCreationDate();
        doReturn(lastInvDate).when(_inventory2).getCreationDate();

        // inventory1 is the cache, inventory2 the last inventory known by the Device
        Whitebox.setInternalState(_command, "_lastInventory", _inventory1);
        doReturn(_inventory2).when(_device).getLastInventory();
        doReturn(_device).when(DeviceHandler.class, "getDevice");
        doReturn(_inventory2).when(_daoInventory).getLastInventory();
        doReturn(_daoInventory).when(DbManager.class, "getDao", InventoryEntity.class);

        _command.execute(_ctx, null);

        // make sure we're sending an inventory from the Db, as we want the last one
        verifyPrivate(_command).invoke("getAndSendLastInventory", _ctx);
        verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.LAST_INVENTORY, serializedNewInv);
        verifyStatic(never());
        SmartServer.sendMessage(_ctx, RequestCode.LAST_INVENTORY, serializedOldInv);
    }
}