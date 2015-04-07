package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.dao.Dao;
import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.AccessTypeEntity;
import com.spacecode.smartserver.database.entity.DeviceEntity;
import com.spacecode.smartserver.database.entity.InventoryEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.DeviceHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * JUnit "InventoryRepository" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SmartServer.class, DbManager.class, DeviceEntity.class, Inventory.class, DeviceHandler.class})
public class DaoInventoryTest
{
    private UserEntity _userEntity;
    private DeviceEntity _devEntity;
    private Device _device;

    @Before
    public void setUp() throws Exception
    {
        // required to avoid an initialization exception, as SmartServer has some static initialization
        mockStatic(SmartServer.class);

        _devEntity = new DeviceEntity("AA7770201");
        String username = "Vincent";
        String badgeNumber = "BCDE05551";
        _userEntity = new UserEntity(username, badgeNumber);

        mockStatic(DbManager.class, Mockito.CALLS_REAL_METHODS);
        doReturn(_devEntity).when(DbManager.class, "getDevEntity");

        _device = PowerMockito.mock(Device.class);
        mockStatic(DeviceHandler.class);
        doReturn(_device).when(DeviceHandler.class, "getDevice");
    }

    @After
    public void tearDown() throws Exception
    {
        DbManager.close();
        _devEntity = null;
        _userEntity = null;
        _device = null;
    }

    @Test
    public void testGetLastInventory() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:lastInventory").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the dao
        DaoInventory invRepo = (DaoInventory) DbManager.getDao(InventoryEntity.class);

        // get the DAO's and create the fixtures
        Dao<DeviceEntity, Integer> daoDev = DbManager.getDao(DeviceEntity.class);
        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        DaoAccessType ateRepo = (DaoAccessType) DbManager.getDao(AccessTypeEntity.class);
        Dao<InventoryEntity, Integer> daoInv = DbManager.getDao(InventoryEntity.class);

        // create the fixtures
        daoDev.create(_devEntity);
        daoUser.create(_userEntity);

        // create a first inventory in the DB
        daoInv.create(new InventoryEntity(new Inventory(), _userEntity, ateRepo.fromAccessType(AccessType.BADGE)));

        Inventory lastInventory = invRepo.getLastInventory();
        assertNotNull(lastInventory);
        assertEquals(lastInventory.getUsername(), "Vincent");
        assertEquals(lastInventory.getAccessType(), AccessType.BADGE);

        // create a NEWER "last inventory" to make sure we always get the last one
        daoInv.create(new InventoryEntity(new Inventory(), null, ateRepo.fromAccessType(AccessType.UNDEFINED)));

        lastInventory = invRepo.getLastInventory();
        assertNotNull(lastInventory);
        assertEquals(lastInventory.getUsername(), "");
        assertEquals(lastInventory.getAccessType(), AccessType.UNDEFINED);
    }

    /**
     * In case of "One database for Many Devices", the Inventory Table can contain many inventories from different
     * devices. Let's make sure that we always get the "last inventory" of the *current* device.
     *
     * @throws Exception If mocking the getConnectionString method failed.
     */
    @Test
    public void testGetLastInventoryMultiDevices() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:lastInventorymultiDevices").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        DeviceEntity otherDevice = new DeviceEntity("SecondDevice");

        // get the repositories
        DaoInventory invRepo = (DaoInventory) DbManager.getDao(InventoryEntity.class);
        DaoAccessType ateRepo = (DaoAccessType) DbManager.getDao(AccessTypeEntity.class);

        // get the DAO's and create the fixtures
        Dao<DeviceEntity, Integer> daoDev = DbManager.getDao(DeviceEntity.class);
        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        Dao<InventoryEntity, Integer> daoInv = DbManager.getDao(InventoryEntity.class);

        // create the fixtures
        daoDev.create(_devEntity);
        daoDev.create(otherDevice);
        daoUser.create(_userEntity);

        // create a first inventory in the DB
        daoInv.create(new InventoryEntity(new Inventory(), _userEntity, ateRepo.fromAccessType(AccessType.BADGE)));

        // VIRTUALLY change the Device which write in the database
        doReturn(otherDevice).when(DbManager.class, "getDevEntity");
        // and Add a new inventory
        daoInv.create(new InventoryEntity(new Inventory(), null, ateRepo.fromAccessType(AccessType.UNDEFINED)));

        // VIRTUALLY get back to our true device
        doReturn(_devEntity).when(DbManager.class, "getDevEntity");
        // and check that we're still getting the correct "last inventory"
        Inventory lastInventory = invRepo.getLastInventory();
        assertNotNull(lastInventory);
        assertEquals(lastInventory.getUsername(), _userEntity.getUsername());
        assertEquals(lastInventory.getAccessType(), AccessType.BADGE);
    }

    @Test
    public void testGetInventories() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:inventories").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the repositories
        DaoInventory invRepo = (DaoInventory) DbManager.getDao(InventoryEntity.class);
        DaoAccessType ateRepo = (DaoAccessType) DbManager.getDao(AccessTypeEntity.class);

        // get the DAO and create fixtures
        Dao<InventoryEntity, Integer> daoInv = DbManager.getDao(InventoryEntity.class);
        // create inventories
        Inventory inv1 = PowerMockito.mock(Inventory.class);
        Inventory inv2 = PowerMockito.mock(Inventory.class);
        Inventory inv3 = PowerMockito.mock(Inventory.class);
        // 2 very close (100ms separe them)
        doReturn(new Date(123001)).when(inv1).getCreationDate();
        doReturn(new Date(123101)).when(inv2).getCreationDate();
        // one last later
        doReturn(new Date(955123)).when(inv3).getCreationDate();
        // insert them in DB
        daoInv.create(new InventoryEntity(inv1, null, ateRepo.fromAccessType(AccessType.UNDEFINED)));
        daoInv.create(new InventoryEntity(inv2, null, ateRepo.fromAccessType(AccessType.UNDEFINED)));
        daoInv.create(new InventoryEntity(inv3, null, ateRepo.fromAccessType(AccessType.UNDEFINED)));

        // select inv1 & inv2
        List<Inventory> inventories = invRepo.getInventories(new Date(123001), new Date(123101));
        assertNotNull(inventories);
        assertEquals(inventories.size(), 2);
        // select ALL inventories
        inventories = invRepo.getInventories(new Date(123000), new Date(955999));
        assertNotNull(inventories);
        assertEquals(inventories.size(), 3);
        // select only inv3
        inventories = invRepo.getInventories(new Date(955000), new Date(955999));
        assertNotNull(inventories);
        assertEquals(inventories.size(), 1);
    }

    @Test
    public void testGetInventoriesMultiDevices() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:inventoriesMultiDevices").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the repositories
        DaoInventory invRepo = (DaoInventory) DbManager.getDao(InventoryEntity.class);
        DaoAccessType ateRepo = (DaoAccessType) DbManager.getDao(AccessTypeEntity.class);

        // get the DAO and create fixtures
        Dao<InventoryEntity, Integer> daoInv = DbManager.getDao(InventoryEntity.class);
        Dao<DeviceEntity, Integer> daoDev = DbManager.getDao(DeviceEntity.class);
        // create inventories
        Inventory inv1 = PowerMockito.mock(Inventory.class);
        Inventory inv2 = PowerMockito.mock(Inventory.class);
        Inventory inv3 = PowerMockito.mock(Inventory.class);
        // 2 very close (100ms separe them)
        doReturn(new Date(123001)).when(inv1).getCreationDate();
        doReturn(new Date(123101)).when(inv2).getCreationDate();
        // one last later
        doReturn(new Date(955123)).when(inv3).getCreationDate();
        // insert inv1 & inv2 as inventories of our "current" device
        daoInv.create(new InventoryEntity(inv1, null, ateRepo.fromAccessType(AccessType.UNDEFINED)));
        daoInv.create(new InventoryEntity(inv2, null, ateRepo.fromAccessType(AccessType.UNDEFINED)));
        // insert the second device in the database
        DeviceEntity otherDevice = new DeviceEntity("SecondDevice");
        daoDev.create(otherDevice);

        // VIRTUALLY change the Device which write in the database
        doReturn(otherDevice).when(DbManager.class, "getDevEntity");
        // and Add a third inventory as made by the "second device"
        daoInv.create(new InventoryEntity(inv3, null, ateRepo.fromAccessType(AccessType.UNDEFINED)));
        // VIRTUALLY get back to our true device
        doReturn(_devEntity).when(DbManager.class, "getDevEntity");

        // select inv1 & inv2
        List<Inventory> inventories = invRepo.getInventories(new Date(123001), new Date(123101));
        assertNotNull(inventories);
        assertEquals(inventories.size(), 2);
        // select ALL inventories: Should only return 2 inventories, as "inv3" is not made by the "current" device
        inventories = invRepo.getInventories(new Date(123000), new Date(955999));
        assertNotNull(inventories);
        assertEquals(inventories.size(), 2);
        // select only inv3: Fails, as inv3 has not been made by the "current" device
        inventories = invRepo.getInventories(new Date(955000), new Date(955999));
        assertNotNull(inventories);
        assertTrue(inventories.isEmpty());
    }

    @Test
    public void testPersist() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:persistInventory").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the dao and make sure the table is empty
        DaoInventory invRepo = (DaoInventory) DbManager.getDao(InventoryEntity.class);
        assertNull(invRepo.getLastInventory());

        // create the user associated to the inventory persisted
        Dao<InventoryEntity, Integer> daoInv = DbManager.getDao(InventoryEntity.class);
        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        daoUser.create(_userEntity);

        // create a new inventory (to be persisted)
        List<String> tagsAdded = Arrays.asList("1234", "2345", "3456");
        List<String> tagsPresent = Arrays.asList("4567", "5678");
        List<String> tagsRemoved = Arrays.asList("9876");

        Inventory newInventory = new Inventory(2, tagsAdded, tagsPresent, tagsRemoved,
                _userEntity.getUsername(), AccessType.FINGERPRINT, new Date());

        // mock the "Tag to Axis Number" Map in the Device instance
        // tags added
        Map<String, Byte> tagToAxis = new HashMap<>();
        tagToAxis.put("1234", (byte) 3);
        tagToAxis.put("2345", (byte) 3);
        tagToAxis.put("3456", (byte) 2);
        // tags present
        tagToAxis.put("4567", (byte) 1);
        tagToAxis.put("5678", (byte) 1);
        doReturn(tagToAxis).when(_device).getTagToAxis();

        long inventoriesCount = daoInv.countOf();

        // persist the new inventory
        assertTrue(invRepo.persist(newInventory));
        assertEquals(inventoriesCount + 1, daoInv.countOf());

        // check it has successfully been persisted and check its data
        Inventory lastInventory = invRepo.getLastInventory();
        assertNotNull(lastInventory);
        // 3 added + 2 present
        assertEquals(lastInventory.getNumberTotal(), 5);
        // 1 removed
        assertEquals(lastInventory.getNumberRemoved(), 1);
        assertTrue(lastInventory.getTagsRemoved().contains("9876"));
        assertEquals(lastInventory.getUsername(), _userEntity.getUsername());
        assertEquals(lastInventory.getAccessType(), AccessType.FINGERPRINT);
    }
}