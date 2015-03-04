package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.data.FingerIndex;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * JUnit "UserRepository" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SmartServer.class, DbManager.class })
public class UserRepositoryTest
{
    private UserEntity _userEntity;
    private DeviceEntity _devEntity;

    @Before
    public void setUp() throws Exception
    {
        // required to avoid an initialization exception, as SmartServer has some static initialization
        mockStatic(SmartServer.class);

        _devEntity = new DeviceEntity("AA7770201");

        mockStatic(DbManager.class, Mockito.CALLS_REAL_METHODS);
        doReturn(_devEntity).when(DbManager.class, "getDevEntity");

        String username = "Vincent";
        String badgeNumber = "BCDE05551";
        _userEntity = new UserEntity(username, badgeNumber);
    }

    @After
    public void tearDown() throws Exception
    {
        DbManager.close();
        _userEntity = null;
        _devEntity = null;
    }

    @Test
    public void testGetByUsername() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:getByUsername").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the repository
        UserRepository userRepo = (UserRepository) DbManager.getRepository(UserEntity.class);

        assertNull(userRepo.getByUsername(null));
        assertNull(userRepo.getByUsername(""));
        assertNull(userRepo.getByUsername("   "));

        assertNull(userRepo.getByUsername("Unknown User"));

        // get the DAO and create the fixture
        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        daoUser.create(_userEntity);

        UserEntity lastUser = userRepo.getByUsername(_userEntity.getUsername());
        assertNotNull(lastUser);
    }

    @Test
    public void testDeleteByName() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:deleteByUsername").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the repository
        UserRepository userRepo = (UserRepository) DbManager.getRepository(UserEntity.class);

        assertFalse(userRepo.deleteByName(null));
        assertFalse(userRepo.deleteByName(""));
        assertFalse(userRepo.deleteByName("  "));
        assertFalse(userRepo.deleteByName("Unknown User"));

        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);

        assertEquals(daoUser.countOf(), 0);
        daoUser.create(_userEntity);
        assertEquals(daoUser.countOf(), 1);
        assertTrue(userRepo.deleteByName(_userEntity.getUsername()));
        assertEquals(daoUser.countOf(), 0);
    }

    @Test
    public void testDelete() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:deleteUser").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the repository
        UserRepository userRepo = (UserRepository) DbManager.getRepository(UserEntity.class);
        GrantTypeRepository gtRepo = (GrantTypeRepository) DbManager.getRepository(GrantTypeEntity.class);

        assertFalse(userRepo.delete((UserEntity) null));

        // get the DAO's and create the fixtures
        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        Dao<FingerprintEntity, Integer> daoFp = DbManager.getDao(FingerprintEntity.class);
        Dao<GrantedAccessEntity, Integer> daoGa = DbManager.getDao(GrantedAccessEntity.class);
        daoUser.create(_userEntity);
        daoFp.create(new FingerprintEntity(_userEntity, 4, "faketpl"));
        daoGa.create(new GrantedAccessEntity(_userEntity, gtRepo.fromGrantType(GrantType.MASTER)));

        assertEquals(daoUser.countOf(), 1);
        assertEquals(daoFp.countOf(), 1);
        assertEquals(daoGa.countOf(), 1);

        UserEntity userFromDb = userRepo.getByUsername(_userEntity.getUsername());
        assertNotNull(userFromDb);
        assertTrue(userRepo.delete(userFromDb));

        assertEquals(daoUser.countOf(), 0);
        assertEquals(daoFp.countOf(), 0);
        assertEquals(daoGa.countOf(), 0);
    }

    @Test
    public void testUpdateBadgeNumber() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:updateBadgeNumber").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        String newBadgeNumber = "BD12345678";

        // get the repository
        UserRepository userRepo = (UserRepository) DbManager.getRepository(UserEntity.class);

        // unknown or invalid user: check the operation fails
        assertFalse(userRepo.updateBadgeNumber(null, newBadgeNumber));
        assertFalse(userRepo.updateBadgeNumber("", newBadgeNumber));
        assertFalse(userRepo.updateBadgeNumber("Unknown User", newBadgeNumber));

        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        daoUser.create(_userEntity);

        assertNotEquals(_userEntity.getBadgeNumber(), newBadgeNumber);
        assertTrue(userRepo.updateBadgeNumber(_userEntity.getUsername(), newBadgeNumber));

        UserEntity userFromDb = userRepo.getByUsername(_userEntity.getUsername());
        assertNotNull(userFromDb);
        assertEquals(userFromDb.getBadgeNumber(), newBadgeNumber);
    }

    @Test
    public void testUpdateThiefFingerIndex() throws Exception
    {
        _userEntity.setThiefFingerIndex(5);

        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:updateThiefFinger").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        Integer newFingerIndex = 4;

        // get the repository
        UserRepository userRepo = (UserRepository) DbManager.getRepository(UserEntity.class);

        // unknown or invalid user: check the operation fails
        assertFalse(userRepo.updateThiefFingerIndex(null, newFingerIndex));
        assertFalse(userRepo.updateThiefFingerIndex("", newFingerIndex));
        assertFalse(userRepo.updateThiefFingerIndex("Unknown User", newFingerIndex));

        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        daoUser.create(_userEntity);

        assertNotEquals(_userEntity.getThiefFingerIndex(), newFingerIndex);
        assertTrue(userRepo.updateThiefFingerIndex(_userEntity.getUsername(), newFingerIndex));

        UserEntity userFromDb = userRepo.getByUsername(_userEntity.getUsername());
        assertNotNull(userFromDb);
        assertEquals(userFromDb.getThiefFingerIndex(), newFingerIndex);

        // now repeat the operation but nullify the thief finger
        assertTrue(userRepo.updateThiefFingerIndex(_userEntity.getUsername(), null));
        userFromDb = userRepo.getByUsername(_userEntity.getUsername());
        assertNotNull(userFromDb);
        assertEquals(userFromDb.getThiefFingerIndex(), null);
    }

    @Test
    public void testPersistNotExisting() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:persistUserNotExisting").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the repositories
        UserRepository userRepo = (UserRepository) DbManager.getRepository(UserEntity.class);
        FingerprintRepository fpRepo = (FingerprintRepository) DbManager.getRepository(FingerprintEntity.class);

        // create a new (SDK) User instance
        String username = "Mike";
        GrantType userPermission = GrantType.SLAVE;
        String userBadge = "BCD7778123";
        Map<FingerIndex, String> fingersMap = new HashMap<>();
        fingersMap.put(FingerIndex.LEFT_INDEX, "leftindextpl");
        fingersMap.put(FingerIndex.RIGHT_MIDDLE, "rightmiddletpl");

        User newUser = new User(username, userPermission, userBadge, fingersMap);

        // get the DAO
        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        Dao<FingerprintEntity, Integer> daoFp = DbManager.getDao(FingerprintEntity.class);
        Dao<GrantedAccessEntity, Integer> daoGa = DbManager.getDao(GrantedAccessEntity.class);
        assertEquals(daoUser.countOf(), 0);
        assertEquals(daoFp.countOf(), 0);
        assertEquals(daoGa.countOf(), 0);

        assertTrue(userRepo.persist(newUser));

        assertEquals(daoUser.countOf(), 1);
        assertEquals(daoFp.countOf(), 2);
        assertEquals(daoGa.countOf(), 1);

        UserEntity userFromDb = userRepo.getByUsername(username);

        // check the granted access
        Iterator<GrantedAccessEntity> gaIterator = userFromDb.getGrantedAccesses().iterator();
        assertTrue(gaIterator.hasNext());
        GrantedAccessEntity gae = gaIterator.next();
        assertEquals(GrantTypeRepository.asGrantType(gae.getGrantType()), userPermission);

        // check the fingerprints
        FingerprintEntity fpe1 = fpRepo.getFingerprint(userFromDb, FingerIndex.LEFT_INDEX.getIndex());
        FingerprintEntity fpe2 = fpRepo.getFingerprint(userFromDb, FingerIndex.RIGHT_MIDDLE.getIndex());
        assertNotNull(fpe1);
        assertNotNull(fpe2);
        assertEquals(fpe1.getTemplate(), "leftindextpl");
        assertEquals(fpe2.getTemplate(), "rightmiddletpl");
    }

    @Test
    public void testRemovePermission() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:removePermission").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the repositories
        UserRepository userRepo = (UserRepository) DbManager.getRepository(UserEntity.class);
        GrantTypeRepository gtRepo = (GrantTypeRepository) DbManager.getRepository(GrantTypeEntity.class);

        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        Dao<GrantedAccessEntity, Integer> daoGa = DbManager.getDao(GrantedAccessEntity.class);

        daoUser.create(_userEntity);

        assertEquals(daoGa.countOf(), 0);
        daoGa.create(new GrantedAccessEntity(_userEntity, gtRepo.fromGrantType(GrantType.MASTER)));
        assertEquals(daoGa.countOf(), 1);

        assertTrue(userRepo.removePermission(_userEntity.getUsername()));
        assertEquals(daoGa.countOf(), 0);
    }

    @Test
    public void testGetAuthorizedUsers() throws Exception
    {
    }
}