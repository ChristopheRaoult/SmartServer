package com.spacecode.smartserver.database.dao;

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

import java.util.*;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * JUnit "UserRepository" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SmartServer.class, DbManager.class })
public class DaoUserTest
{
    private UserEntity _userEntity;
    private DeviceEntity _devEntity;
    private String _badgeNumber;

    @Before
    public void setUp() throws Exception
    {
        // required to avoid an initialization exception, as SmartServer has some static initialization
        mockStatic(SmartServer.class);

        _devEntity = new DeviceEntity("AA7770201");

        mockStatic(DbManager.class, Mockito.CALLS_REAL_METHODS);
        doReturn(_devEntity).when(DbManager.class, "getDevEntity");

        String username = "Vincent";
        _badgeNumber = "BCDE05551";
        _userEntity = new UserEntity(username, _badgeNumber);
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

        // get the dao
        DaoUser daoUser = (DaoUser) DbManager.getDao(UserEntity.class);

        assertNull(daoUser.getByUsername(null));
        assertNull(daoUser.getByUsername(""));
        assertNull(daoUser.getByUsername("   "));

        assertNull(daoUser.getByUsername("Unknown User"));

        // create the fixture
        daoUser.create(_userEntity);

        UserEntity lastUser = daoUser.getByUsername(_userEntity.getUsername());
        assertNotNull(lastUser);
        assertEquals(lastUser.getBadgeNumber(), _badgeNumber);
    }

    @Test
    public void testUpdateBadgeNumber() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:updateBadgeNumber").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        String newBadgeNumber = "BD12345678";

        // get the dao
        DaoUser daoUser = (DaoUser) DbManager.getDao(UserEntity.class);

        // unknown or invalid user: check the operation fails
        assertFalse(daoUser.updateBadgeNumber(null, newBadgeNumber));
        assertFalse(daoUser.updateBadgeNumber("", newBadgeNumber));
        assertFalse(daoUser.updateBadgeNumber("Unknown User", newBadgeNumber));

        daoUser.create(_userEntity);

        assertNotEquals(_userEntity.getBadgeNumber(), newBadgeNumber);
        assertTrue(daoUser.updateBadgeNumber(_userEntity.getUsername(), newBadgeNumber));

        UserEntity userFromDb = daoUser.getByUsername(_userEntity.getUsername());
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

        // get the dao
        DaoUser userRepo = (DaoUser) DbManager.getDao(UserEntity.class);

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
    public void testPersist() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:persistUserNotExisting").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the repositories
        DaoUser daoUser = (DaoUser) DbManager.getDao(UserEntity.class);
        DaoFingerprint daoFp = (DaoFingerprint) DbManager.getDao(FingerprintEntity.class);
        DaoGrantedAccess daoGa = (DaoGrantedAccess) DbManager.getDao(GrantedAccessEntity.class);

        // create a new (SDK) User instance
        String username = "Mike";
        GrantType userPermission = GrantType.SLAVE;
        String userBadge = "BCD7778123";
        Map<FingerIndex, String> fingersMap = new HashMap<>();
        fingersMap.put(FingerIndex.LEFT_INDEX, "leftindextpl");
        fingersMap.put(FingerIndex.RIGHT_MIDDLE, "rightmiddletpl");

        User newUser = new User(username, userPermission, userBadge, fingersMap);

        assertEquals(daoUser.countOf(), 0);
        assertEquals(daoFp.countOf(), 0);
        assertEquals(daoGa.countOf(), 0);

        assertTrue(daoUser.persist(newUser));

        assertEquals(daoUser.countOf(), 1);
        assertEquals(daoFp.countOf(), 2);
        assertEquals(daoGa.countOf(), 1);

        UserEntity userFromDb = daoUser.getByUsername(username);

        // check the granted access
        Iterator<GrantedAccessEntity> gaIterator = userFromDb.getGrantedAccesses().iterator();
        assertTrue(gaIterator.hasNext());
        GrantedAccessEntity gae = gaIterator.next();
        assertEquals(DaoGrantType.asGrantType(gae.getGrantType()), userPermission);

        // check the fingerprints
        FingerprintEntity fpe1 = daoFp.getFingerprint(userFromDb, FingerIndex.LEFT_INDEX.getIndex());
        FingerprintEntity fpe2 = daoFp.getFingerprint(userFromDb, FingerIndex.RIGHT_MIDDLE.getIndex());
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
        DaoUser daoUser = (DaoUser) DbManager.getDao(UserEntity.class);
        DaoGrantType daoGt = (DaoGrantType) DbManager.getDao(GrantTypeEntity.class);
        DaoGrantedAccess daoGa = (DaoGrantedAccess) DbManager.getDao(GrantedAccessEntity.class);

        daoUser.create(_userEntity);

        assertEquals(daoGa.countOf(), 0);
        daoGa.create(new GrantedAccessEntity(_userEntity, daoGt.fromGrantType(GrantType.MASTER)));
        assertEquals(daoGa.countOf(), 1);

        assertTrue(daoUser.removePermission(_userEntity.getUsername()));
        assertEquals(daoGa.countOf(), 0);
    }

    @Test
    public void sortUsersFromDb() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:sortUsers").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the repositories
        DaoUser daoUser = (DaoUser) DbManager.getDao(UserEntity.class);
        DaoGrantType gtRepo = (DaoGrantType) DbManager.getDao(GrantTypeEntity.class);
        DaoGrantedAccess daoGa = (DaoGrantedAccess) DbManager.getDao(GrantedAccessEntity.class);
        
        // create the fixtures
        UserEntity authUser1, authUser2, authUser3;
        UserEntity unregUser1, unregUser2, unregUser3;
        
        authUser1 = new UserEntity("auth1", "abadge1");
        authUser2 = new UserEntity("auth2", "abadge2");
        authUser3 = new UserEntity("auth3", "abadge3");
        unregUser1 = new UserEntity("unreg1", "ubadge1");
        unregUser2 = new UserEntity("unreg2", "ubadge2");
        
        daoUser.create(authUser1);
        daoUser.create(authUser2);
        daoUser.create(authUser3);        
        daoUser.create(unregUser1);
        daoUser.create(unregUser2);

        // authorized users have a permission
        daoGa.create(new GrantedAccessEntity(authUser1, gtRepo.fromGrantType(GrantType.MASTER)));
        daoGa.create(new GrantedAccessEntity(authUser2, gtRepo.fromGrantType(GrantType.ALL)));
        daoGa.create(new GrantedAccessEntity(authUser3, gtRepo.fromGrantType(GrantType.UNDEFINED)));
        
        List<User> authorizedUsers = new ArrayList<>();
        List<User> unregisteredUsers = new ArrayList<>();
        
        assertTrue(daoUser.sortUsersFromDb(authorizedUsers, unregisteredUsers));
        
        assertFalse(authorizedUsers.isEmpty());
        assertFalse(unregisteredUsers.isEmpty());
        assertEquals(authorizedUsers.size(), 3);
        assertEquals(unregisteredUsers.size(), 2);
    }
}