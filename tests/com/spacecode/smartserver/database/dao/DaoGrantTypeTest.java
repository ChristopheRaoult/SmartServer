package com.spacecode.smartserver.database.dao;

import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.GrantTypeEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * JUnit "GrantTypeRepository" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SmartServer.class, DbManager.class })
public class DaoGrantTypeTest
{
    @Before
    public void setUp() throws Exception
    {
        // required to avoid an initialization exception, as SmartServer has some static initialization
        mockStatic(SmartServer.class);

        mockStatic(DbManager.class, Mockito.CALLS_REAL_METHODS);
    }

    @After
    public void tearDown() throws Exception
    {
        DbManager.close();
    }

    @Test
    public void testAsGrantType() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        DaoGrantType gtRepo = (DaoGrantType) DbManager.getDao(GrantTypeEntity.class);

        assertEquals(
                DaoGrantType.asGrantType(gtRepo.fromGrantType(GrantType.ALL)), GrantType.ALL);
        assertEquals(
                DaoGrantType.asGrantType(gtRepo.fromGrantType(GrantType.MASTER)), GrantType.MASTER);
        assertEquals(
                DaoGrantType.asGrantType(gtRepo.fromGrantType(GrantType.SLAVE)), GrantType.SLAVE);
        assertEquals(
                DaoGrantType.asGrantType(gtRepo.fromGrantType(GrantType.UNDEFINED)), GrantType.UNDEFINED);

        assertNull(DaoGrantType.asGrantType(null));
    }

    @Test
    public void testFromGrantType() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:fromGrantType").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        DaoGrantType gtRepo = (DaoGrantType) DbManager.getDao(GrantTypeEntity.class);

        assertNotNull(gtRepo.fromGrantType(GrantType.ALL));
        assertNotNull(gtRepo.fromGrantType(GrantType.MASTER));
        assertNotNull(gtRepo.fromGrantType(GrantType.SLAVE));
        assertNotNull(gtRepo.fromGrantType(GrantType.UNDEFINED));

        assertNull(gtRepo.fromGrantType(null));
    }
}