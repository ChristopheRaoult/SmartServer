package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.DeviceConfigurationEntity;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.GrantedUserEntity;

/**
 * Manage instantiation of Repositories according to provided class name.
 * See Factory (design) pattern for more information.
 */
public class RepositoryFactory
{
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

        if(className.equals(DeviceConfigurationEntity.class.getName()))
        {
            return new DeviceConfigurationRepository(dao);
        }

        else if(className.equals(FingerprintEntity.class.getName()))
        {
            return new FingerprintRepository(dao);
        }

        else if(className.equals(GrantedUserEntity.class.getName()))
        {
            return new GrantedUserRepository(dao);
        }

        return null;
    }
}
