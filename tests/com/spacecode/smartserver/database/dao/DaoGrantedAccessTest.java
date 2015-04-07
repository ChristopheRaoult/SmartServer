package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.dao.Dao;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.DeviceEntity;
import com.spacecode.smartserver.database.entity.GrantedAccessEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * JUnit "GrantedAccessRepository" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SmartServer.class, DbManager.class })
public class DaoGrantedAccessTest
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
    }

    @Test
    public void testPersist() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:persistPermission").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        DaoGrantedAccess gaRepo = (DaoGrantedAccess) DbManager.getDao(GrantedAccessEntity.class);

        assertFalse(gaRepo.persist("unknown user", GrantType.ALL));

        // get the dao of UserEntity to create fixtures
        Dao<GrantedAccessEntity, Integer> daoGa = DbManager.getDao(GrantedAccessEntity.class);
        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        daoUser.create(_userEntity);

        assertEquals(daoGa.countOf(), 0);
        assertTrue(gaRepo.persist(_userEntity.getUsername(), GrantType.MASTER));
        assertEquals(daoGa.countOf(), 1);

        // make sure that the old GA is removed and the new one is created
        assertTrue(gaRepo.persist(_userEntity.getUsername(), GrantType.ALL));
        assertEquals(daoGa.countOf(), 1);
        List<GrantedAccessEntity> gaEntites = daoGa.queryForAll();
        assertFalse(gaEntites.isEmpty());
        assertEquals(DaoGrantType.asGrantType(gaEntites.get(0).getGrantType()), GrantType.ALL);
    }
}