package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.support.ConnectionSource;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.DeviceEntity;
import com.spacecode.smartserver.database.entity.GrantTypeEntity;
import com.spacecode.smartserver.database.entity.GrantedAccessEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

/**
 * GrantedAccess Repository
 */
public class DaoGrantedAccess extends DaoEntity<GrantedAccessEntity, Integer>
{
    public DaoGrantedAccess(ConnectionSource connectionSource) throws SQLException
    {
        super(connectionSource, GrantedAccessEntity.class);
    }

    /**
     * Persist new permission in database.
     *
     * @param username  User to be updated.
     * @param grantType New permission.
     *
     * @return  True if success, false otherwise (user not known, SQLException, etc).
     */
    public boolean persist(String username, GrantType grantType)
    {
        DaoUser userRepo = (DaoUser) DbManager.getDao(UserEntity.class);
        return userRepo != null && persist(userRepo.getEntityBy(UserEntity.USERNAME, username), grantType);
    }

    /**
     * Overloaded method. See {@link #persist(String, GrantType)}.
     * 
     * @param gue       UserEntity to be granted with a new access.
     * @param grantType Permission type (of the access).
     *
     * @return  True if success, false otherwise (user not known, SQLException, etc).
     */
    public boolean persist(UserEntity gue, GrantType grantType)
    {   
        DaoGrantType daoGrantType = (DaoGrantType) DbManager.getDao(GrantTypeEntity.class);
        DeviceEntity devEntity = DbManager.getDevEntity();
        
        if(gue == null || daoGrantType == null || devEntity == null)
        {
            return false;
        }

        GrantTypeEntity gte = daoGrantType.fromGrantType(grantType);

        if(gte == null)
        {
            SmartLogger.getLogger().severe("Persisting permission: unknown grant type " + grantType + ".");
            return false;
        }

        GrantedAccessEntity gae = new GrantedAccessEntity(gue, gte);
        Collection<GrantedAccessEntity> gaesList = gue.getGrantedAccesses();
        
        if(gaesList == null)
        {
            SmartLogger.getLogger().severe("UserEntity with null collection of GrantedAccess. Not hydrated?");
            return false;
        }

        Iterator<GrantedAccessEntity> it = gaesList.iterator();

        // remove any previous permission on this device
        while(it.hasNext())
        {
            if(it.next().getDevice().getId() == devEntity.getId())
            {
                it.remove();
                break;
            }
        }

        DbManager.forceUpdate(gue);
        
        // add the new permission
        return gaesList.add(gae);
    }
}
