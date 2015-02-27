package com.spacecode.smartserver.database.entity;

import com.digitalpersona.uareu.Reader;
import com.j256.ormlite.dao.ForeignCollection;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.user.data.AccessType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Date;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * JUnit "InventoryEntity" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ InventoryEntity.class, DeviceEntity.class, UserEntity.class,
        AccessTypeEntity.class, InventoryRfidTag.class })
public class InventoryEntityTest
{
    @Test
    public void testAsInventory() throws Exception
    {
        InventoryEntity invEntity = PowerMockito.mock(InventoryEntity.class);
        DeviceEntity devEntity = PowerMockito.mock(DeviceEntity.class);
        UserEntity userEntity = PowerMockito.mock(UserEntity.class);
        AccessTypeEntity accessTypeEntity = PowerMockito.mock(AccessTypeEntity.class);
        ForeignCollection<InventoryRfidTag> rfidTags = PowerMockito.mock(ForeignCollection.class);
        // iterator for the InventoryRfidTag collection
        Iterator<Reader> iterator = PowerMockito.mock(Iterator.class);

        Date createdAt = new Date();

        doReturn(AccessType.FINGERPRINT.name()).when(accessTypeEntity).getType();

        // 5 tags in the inventory: 2 added, 1 present, 2 removed
        int totalAdded = 2, totalPresent = 1, totalRemoved = 2;

        RfidTagEntity tagEntity1 = new RfidTagEntity("12345");
        RfidTagEntity tagEntity2 = new RfidTagEntity("23456");
        RfidTagEntity tagEntity3 = new RfidTagEntity("34567");
        RfidTagEntity tagEntity4 = new RfidTagEntity("45678");
        RfidTagEntity tagEntity5 = new RfidTagEntity("56789");

        // 5 tags in the inventory
        doReturn(true)
        .doReturn(true)
        .doReturn(true)
        .doReturn(true)
        .doReturn(true)
        .doReturn(false)
                .when(iterator).hasNext();

        // 2 added, 1 present, 2 removed
        doReturn(new InventoryRfidTag(invEntity, tagEntity1, 1))
        .doReturn(new InventoryRfidTag(invEntity, tagEntity2, 1))
        .doReturn(new InventoryRfidTag(invEntity, tagEntity3, 0))
        .doReturn(new InventoryRfidTag(invEntity, tagEntity4, -1))
        .doReturn(new InventoryRfidTag(invEntity, tagEntity5, -1))
            .when(iterator).next();

        doReturn(iterator).when(rfidTags).iterator();

        Whitebox.setInternalState(invEntity, "_device", devEntity);
        Whitebox.setInternalState(invEntity, "_grantedUser", userEntity);
        Whitebox.setInternalState(invEntity, "_accessType", accessTypeEntity);
        Whitebox.setInternalState(invEntity, "_totalAdded", totalAdded);
        Whitebox.setInternalState(invEntity, "_totalPresent", totalPresent);
        Whitebox.setInternalState(invEntity, "_totalRemoved", totalRemoved);
        Whitebox.setInternalState(invEntity, "_createdAt", createdAt);
        Whitebox.setInternalState(invEntity, "_rfidTags", rfidTags);

        when(invEntity.asInventory()).thenCallRealMethod();

        Inventory inventory = invEntity.asInventory();

        assertEquals(inventory.getNumberTotal(), totalAdded+totalPresent);
        assertEquals(inventory.getAccessType(), AccessType.FINGERPRINT);
        assertEquals(inventory.getTagsAdded().size(), totalAdded);
        assertEquals(inventory.getTagsPresent().size(), totalPresent);
        assertEquals(inventory.getTagsRemoved().size(), totalRemoved);
        assertEquals(inventory.getCreationDate().getTime(), createdAt.getTime());
        assertEquals(inventory.getTagsAdded().get(0), "12345");
    }
}