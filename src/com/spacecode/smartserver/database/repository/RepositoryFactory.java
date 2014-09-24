package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.*;

/**
 * Manage instantiation of Repositories according to provided class name.
 * See Factory (design) pattern for more information.
 */
public class RepositoryFactory
{
    /**
     * Must not be instantiated.
     */
    private RepositoryFactory()
    {
    }

    /**
     * Instantiate a repository according to parameters (or none if given values were not expected).
     * @param className Name of the Entity class to be used.
     * @param dao       Dao to be attached to the Repository instance.
     * @return          Repository instance, or null if unknown values (parameters).
     */
    public static Repository getRepository(String className, Dao dao)
    {
        if(className == null || dao == null || "".equals(className.trim()))
        {
            return null;
        }

        if(className.equals(AccessTypeEntity.class.getName()))
        {
            return new AccessTypeRepository(dao);
        }

        else if(className.equals(AlertEntity.class.getName()))
        {
            return new AlertRepository(dao);
        }

        else if(className.equals(AlertHistoryEntity.class.getName()))
        {
            return new AlertHistoryRepository(dao);
        }

        else if(className.equals(AlertTemperatureEntity.class.getName()))
        {
            return new AlertTemperatureRepository(dao);
        }

        else if(className.equals(AlertTypeEntity.class.getName()))
        {
            return new AlertTypeRepository(dao);
        }

        else if(className.equals(AuthenticationEntity.class.getName()))
        {
            return new AuthenticationRepository(dao);
        }

        else if(className.equals(DeviceEntity.class.getName()))
        {
            return new DeviceRepository(dao);
        }

        else if(className.equals(FingerprintEntity.class.getName()))
        {
            return new FingerprintRepository(dao);
        }

        else if(className.equals(GrantedAccessEntity.class.getName()))
        {
            return new GrantedAccessRepository(dao);
        }

        else if(className.equals(GrantedUserEntity.class.getName()))
        {
            return new GrantedUserRepository(dao);
        }

        else if(className.equals(GrantTypeEntity.class.getName()))
        {
            return new GrantTypeRepository(dao);
        }

        else if(className.equals(InventoryEntity.class.getName()))
        {
            return new InventoryRepository(dao);
        }

        else if(className.equals(InventoryRfidTag.class.getName()))
        {
            return new InventoryRfidTagRepository(dao);
        }

        else if(className.equals(RfidTagEntity.class.getName()))
        {
            return new RfidTagRepository(dao);
        }

        else if(className.equals(SmtpServerEntity.class.getName()))
        {
            return new SmtpServerRepository(dao);
        }

        else if(className.equals(TemperatureMeasurementEntity.class.getName()))
        {
            return new TemperatureMeasurementRepository(dao);
        }

        return null;
    }
}
